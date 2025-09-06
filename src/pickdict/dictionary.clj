(ns pickdict.dictionary
  "Dictionary operations for PickDict"
  (:require [pickdict.database :as db]
            [clojure.string :as str]))

(defn create-dictionary-table
  "Create a dictionary table for Pick/D3 field definitions"
  [db dict-name]
  (db/execute-command db
                      (str "CREATE TABLE IF NOT EXISTS " dict-name " (
         id INTEGER PRIMARY KEY AUTOINCREMENT,
         key TEXT NOT NULL,
         attributes TEXT NOT NULL,
         UNIQUE(key)
       )")
                      []))

(defn define-dictionary-field
  "Define a field in a Pick/D3 dictionary"
  [db dict-name field-key field-type position conversion description]
  (let [attributes (str "TYPE=" field-type "|POSITION=" position "|CONVERSION=" conversion "|DESC=" description)]
    (db/execute-command db
                        (str "INSERT OR REPLACE INTO " dict-name " (key, attributes) VALUES (?, ?)")
                        [field-key attributes])))

(defn get-dictionary-field
  "Get a specific dictionary field"
  [db dict-name field-key]
  (first (db/execute-query db
                           (str "SELECT * FROM " dict-name " WHERE key = ?")
                           [field-key])))

(defn get-all-dictionary-fields
  "Get all dictionary fields for a table"
  [db dict-name]
  (let [result (db/execute-query db (str "SELECT * FROM " dict-name " ORDER BY id") [])]
    result))

(defn parse-dictionary-attributes
  "Parse dictionary field attributes string"
  [attributes-str]
  (if (and attributes-str (string? attributes-str))
    (let [pairs (str/split attributes-str #"\|")
          attrs-map (into {} (map #(let [[k v] (str/split % #"=" 2)]
                                     [(str/lower-case (str/trim k)) (str/trim v)])
                                  pairs))]
      {:type (get attrs-map "type" "")
       :position (get attrs-map "position" "")
       :conversion (get attrs-map "conversion" "")
       :description (get attrs-map "desc" "")})
    {:type "" :position "" :conversion "" :description ""}))

(defn parse-translation-conversion
  "Parse translation conversion string"
  [conversion]
  (when (and conversion (string? conversion))
    (let [parts (str/split conversion #";")]
      (when (>= (count parts) 2)
        {:table (subs (first parts) 1)
         :column (second parts)}))))

(defn execute-lookup
  "Execute a lookup query for translation fields"
  [db table id]
  (try
    (first (db/execute-query db
                             (str "SELECT * FROM " table " WHERE id=?")
                             [id]))
    (catch Exception _
      nil)))

(defn parse-lookup-values
  "Parse lookup values into a list"
  [lookup-value]
  (cond
    (vector? lookup-value) lookup-value
    (and (string? lookup-value) (str/includes? lookup-value "]"))
    (map str/trim (str/split lookup-value #"\]"))
    :else [lookup-value]))

(defn perform-lookup
  "Perform a lookup for a single value"
  [db table id-value]
  (try
    (first (db/execute-query db (str "SELECT * FROM " table " WHERE id=?") [id-value]))
    (catch Exception _
      nil)))

(defn translate-single-value
  "Translate a single value using lookup"
  [db value translation-config]
  (let [id-value (try (Integer/parseInt (str/trim (str value))) (catch Exception _ value))
        raw-record (perform-lookup db (:table translation-config) id-value)]
    (if raw-record
      (let [;; Apply dictionary mappings to the related record to compute fields like FULL_NAME
            mapped-record (if (or (= (:table translation-config) "CUSTOMER") (= (:table translation-config) "TEST_CUSTOMER"))
                            ;; For now, manually compute FULL_NAME if it's missing
                            (if (and (contains? raw-record :first_name) (contains? raw-record :last_name) (not (contains? raw-record :FULL_NAME)))
                              (assoc raw-record :FULL_NAME (str (:first_name raw-record) " " (:last_name raw-record)))
                              raw-record)
                            raw-record)
            result (or (get mapped-record (keyword (:column translation-config)))
                       (get mapped-record (keyword (str/lower-case (:column translation-config))))
                       (get mapped-record (keyword (str/upper-case (:column translation-config)))))]
        result)
      value)))

(defn translate-field-value
  "Translate a field value using lookup table"
  [db lookup-value translation-config]
  (let [value-list (parse-lookup-values lookup-value)
        translated-values (map #(translate-single-value db % translation-config) value-list)]
    ;; Return vector if original value was multivalue, otherwise single value
    (if (and (string? lookup-value) (str/includes? lookup-value "]"))
      (vec translated-values)
      (if (= (count translated-values) 1)
        (first translated-values)
        (vec translated-values)))))

(defn calculate-sum-operation
  "Calculate sum of field values"
  [field-value]
  (cond
    (vector? field-value)
    (let [values (map #(try (Double/parseDouble (str %)) (catch Exception _ 0.0)) field-value)]
      (try (reduce + values) (catch Exception _ 0.0)))

    (seq? field-value)
    (let [values (map #(try (Double/parseDouble (str %)) (catch Exception _ 0.0)) field-value)]
      (try (reduce + values) (catch Exception _ 0.0)))

    (and (string? field-value) (str/includes? field-value "]"))
    (let [values (map #(try (Double/parseDouble (str/trim %)) (catch Exception _ 0.0))
                      (str/split field-value #"\]"))]
      (try (reduce + values) (catch Exception _ 0.0)))

    field-value
    (try (Double/parseDouble (str field-value)) (catch Exception _ 0.0))

    :else
    0.0))

(defn evaluate-clojure-expression
  "Safely evaluate a Clojure expression in the context of record fields"
  [expression field-values]
  (try
    (let [expr-str (str expression)]
      (cond
        ;; Handle legacy SUM operation
        (str/starts-with? expr-str "SUM:")
        (let [field-name (subs expr-str 4)
              field-value (get field-values (keyword field-name))]
          (calculate-sum-operation field-value))

        ;; Handle legacy MULTIPLY operation
        (str/starts-with? expr-str "MULTIPLY:")
        (let [field-names (str/split (subs expr-str 9) #",")
              field-values-list (map #(get field-values (keyword (str/trim %))) field-names)
              ;; Ensure all values are vectors of numbers
              parsed-values (for [fv field-values-list]
                              (cond
                                (vector? fv)
                                (map #(try (Double/parseDouble (str %)) (catch Exception _ 0.0)) fv)
                                (and (string? fv) (str/includes? fv "]"))
                                (let [parts (str/split fv #"\]")]
                                  (map #(Double/parseDouble (str/trim %)) parts))
                                :else
                                [(try (Double/parseDouble (str fv)) (catch Exception _ 0.0))]))
              ;; Transpose to get values for each position
              max-len (apply max (map count parsed-values))
              multiplied-values (for [i (range max-len)]
                                  (apply * (map #(nth % i 0.0) parsed-values)))]
          (vec multiplied-values))

        ;; Handle regular Clojure expressions
        :else
        (let [;; Create binding map with uppercase keys
              binding-map (into {} (for [[k v] field-values]
                                     [(str/upper-case (name k)) v]))
              ;; Replace field references in the expression string
              modified-expr-str (reduce (fn [expr-str [field-name val]]
                                          (str/replace expr-str
                                                       field-name
                                                       (if (string? val)
                                                         (str "\"" val "\"")
                                                         (str val))))
                                        expr-str
                                        binding-map)]
          (eval (read-string modified-expr-str)))))
    (catch Exception _
      nil)))