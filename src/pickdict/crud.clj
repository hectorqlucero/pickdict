(ns pickdict.crud
  (:require [pickdict.database :as db]
            [pickdict.dictionary :as dict]
            [pickdict.multivalue :as mv]
            [clojure.string :as str]))

;; --- Dictionary Transformation Helpers ---
(defn get-dictionary-table-name [table-name]
  (str table-name "_DICT"))

(defn get-dictionary-fields [db table-name]
  (let [dict-table-name (get-dictionary-table-name table-name)]
    (dict/get-all-dictionary-fields db dict-table-name)))

(defn parse-dictionary-field-attributes [dict-field]
  (let [attributes (dict/parse-dictionary-attributes (:attributes dict-field))]
    {:field-name (keyword (:key dict-field))
     :field-type (:type attributes)
     :position (:position attributes)
     :conversion (:conversion attributes)}))




(defn dict-field->source-key [field-name]
  (-> field-name name clojure.string/lower-case (clojure.string/replace "_" "-") keyword))


(defn apply-direct-field-mapping [_db _table-name record {:keys [field-name]}]
  (let [source-key (dict-field->source-key field-name)
        source-value (get record source-key)]
    (if (some? source-value)
      (assoc record field-name source-value)
      record)))



(defn apply-dictionary-field-mapping [db table-name record field-attrs]
  (let [{:keys [field-type]} field-attrs]
    (if (= field-type "A")
      (apply-direct-field-mapping db table-name record field-attrs)
      record)))




(defn apply-dictionary-mappings [db table-name record]
  (try
    (let [dict-fields (get-dictionary-fields db table-name)]
      (if (map? (reduce (fn [rec dict-field]
                          (let [field-attrs (parse-dictionary-field-attributes dict-field)]
                            (apply-dictionary-field-mapping db table-name rec field-attrs)))
                        record
                        dict-fields))
        (reduce (fn [rec dict-field]
                  (let [field-attrs (parse-dictionary-field-attributes dict-field)]
                    (apply-dictionary-field-mapping db table-name rec field-attrs)))
                record
                dict-fields)
        record))
    (catch Exception _
      record)))

;; --- Utility Functions for Computed Fields ---
(defn sum-multivalue-field [record field-key]
  (let [values (mv/extract-multivalue-numbers (get record field-key))]
    (if (seq values) (reduce + values) 0.0)))

(defn multiply-fields [record qty-key price-key]
  (let [qtys (mv/extract-multivalue-numbers (get record qty-key))
        prices (mv/extract-multivalue-numbers (get record price-key))]
    (if (and (seq qtys) (seq prices) (= (count qtys) (count prices)))
      (vec (map * qtys prices))
      [])))

;; --- Computed Fields Registry (Generic) ---
(def computed-fields-registry (atom {}))

(defn register-computed-field!
  "Register a computed field function for a table."
  [table-name field-name f]
  (swap! computed-fields-registry update table-name assoc field-name f))

(defn unregister-computed-field!
  "Unregister a computed field for a table."
  [table-name field-name]
  (swap! computed-fields-registry update table-name dissoc field-name))

(defn apply-computed-fields [table-name record]
  ;; (when (not (map? record))
  ;;   (println "ERROR: apply-computed-fields received non-map:" (type record) record))
  (let [computed-fns (get @computed-fields-registry table-name)]
    (if computed-fns
      (reduce (fn [rec [field f]]
                (assoc rec field (f rec)))
              record
              computed-fns)
      record)))


;; No hardcoded computed field registrations. Users should register computed fields externally as needed.


;; No hardcoded computed field logic. All computed field logic should be registered externally via the registry.

;; --- Dictionary Translation Registry (Generic) ---
(def translation-registry (atom {}))

(defn register-translation!
  "Register a translation function for a table/field."
  [table-name field-name f]
  (swap! translation-registry update table-name assoc field-name f))

(defn unregister-translation!
  "Unregister a translation function for a table/field."
  [table-name field-name]
  (swap! translation-registry update table-name dissoc field-name))

(defn apply-translations [db table-name record]
  (let [translations (get @translation-registry table-name)]
    (if translations
      (reduce (fn [rec [field f]]
                (assoc rec field (f db rec)))
              record
              translations)
      record)))


;; No hardcoded translation registrations. Users should register translations externally as needed.

;; --- Validation Registry (Generic) ---
(def validation-registry (atom {}))

(defn register-validator!
  "Register a validation function for a table."
  [table-name f]
  (swap! validation-registry assoc table-name f))

(defn unregister-validator!
  "Unregister a validation function for a table."
  [table-name]
  (swap! validation-registry dissoc table-name))

(defn validate-record [table-name record]
  (if-let [validator (get @validation-registry table-name)]
    (validator record)
    record))


;; No hardcoded validation registrations. Users should register validations externally as needed.

;; --- CRUD Operations (with generic translation and validation) ---
(defn create-record
  "Create a new record in a table. Formats record for storage. Returns the generated ID."
  [db table-name record]
  (let [validated-record (validate-record table-name record)
        formatted-record (mv/format-record-for-storage validated-record)]
    (db/insert-record db table-name formatted-record)))




(defn find-by-id
  "Find a record by ID. Parses multivalue fields and applies dictionary, translation, and computed fields."
  [db table-name id]
  (when id
    (let [raw-record (db/find-by-id db table-name id)]
      (when raw-record
        (let [parsed-record (mv/parse-record-from-storage raw-record)
              dict-mapped (apply-dictionary-mappings db table-name parsed-record)
              translated (apply-translations db table-name dict-mapped)
              computed (apply-computed-fields table-name translated)]
          computed)))))


;(defn read-all-records ...)


(defn read-all-records
  "Read all records from a table. Parses multivalue fields and applies dictionary, translation, and computed fields."
  [db table-name]
  (let [raw-records (db/find-all db table-name)]
    (->> (filter map? raw-records)
         (keep (fn [raw-record]
                 (let [parsed-record (mv/parse-record-from-storage raw-record)
                       dict-mapped (apply-dictionary-mappings db table-name parsed-record)
                       translated (apply-translations db table-name dict-mapped)
                       computed (apply-computed-fields table-name translated)]
                   (when (map? computed) computed))))
         (filter map?)
         vec)))

(defn update-record
  "Update a record by ID. Formats record for storage. Returns true if successful."
  [db table-name id updates]
  (let [validated-updates (validate-record table-name updates)
        formatted-updates (mv/format-record-for-storage validated-updates)]
    (db/update-record db table-name id formatted-updates)))

(defn delete-record
  "Delete a record by ID. Returns true if successful."
  [db table-name id]
  (db/delete-record db table-name id))

(defn find-by-criteria
  "Find records by criteria (map of field to value). Parses multivalue fields and applies dictionary, translation, and computed fields."
  [db table-name criteria]
  (let [raw-records (db/find-by-criteria db table-name criteria)]
    (map (fn [raw-record]
           (let [parsed-record (mv/parse-record-from-storage raw-record)
                 dict-mapped (apply-dictionary-mappings db table-name parsed-record)]
             (-> dict-mapped
                 (apply-translations db table-name)
                 (apply-computed-fields table-name))))
         raw-records)))

(defn count-records
  "Count records in a table."
  [db table-name]
  (db/count-records db table-name))
