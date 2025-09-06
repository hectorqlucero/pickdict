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
                                     [(str/lower-case k) v])
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
  [db table column id]
  (try
    (first (db/execute-query db
                             (str "SELECT * FROM " table " WHERE id=?")
                             [id]))
    (catch Exception e
      (println (str "Lookup error for " table "." column " id=" id ": " (.getMessage e)))
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
  [db table column id-value]
  (try
    (first (db/execute-query db (str "SELECT * FROM " table " WHERE id=?") [id-value]))
    (catch Exception e
      (println (str "Lookup error for " table "." column " id=" id-value ": " (.getMessage e)))
      nil)))

(defn translate-single-value
  "Translate a single value using lookup"
  [db value translation-config]
  (let [id-value (try (Integer/parseInt (str/trim (str value))) (catch Exception _ value))
        related-record (perform-lookup db (:table translation-config) (:column translation-config) id-value)]
    (if related-record
      (get related-record (keyword (:column translation-config)))
      value)))

(defn translate-field-value
  "Translate a field value using lookup table"
  [db lookup-value translation-config]
  (let [value-list (parse-lookup-values lookup-value)
        translated-values (map #(translate-single-value db % translation-config) value-list)]
    ;; For PRODUCT_NAMES (table=PRODUCT), always return a vector. For others, return single value if only one
    (if (= (:table translation-config) "PRODUCT")
      (vec translated-values)
      (if (= (count translated-values) 1)
        (first translated-values)
        (vec translated-values)))))

(defn evaluate-clojure-expression
  "Safely evaluate a Clojure expression in the context of record fields"
  [expression field-values]
  (try
    (let [expr-str (str expression)
          field-symbols (set (keys field-values))
          has-variables (some #(str/includes? expr-str (name %)) field-symbols)]
      (if has-variables
        (let [bindings (vec (apply concat (for [[k v] field-values] [k v])))]
          (eval `(let ~bindings ~expression)))
        (eval expression)))
    (catch Exception e
      (println (str "Error evaluating Clojure expression: " expression " - " (.getMessage e)))
      nil)))

(defn calculate-multiply-operation
  "Calculate multiplication of multiple operands"
  [operands]
  (try
    (apply * operands)
    (catch Exception e
      (println (str "Error in MULTIPLY operation: " (.getMessage e)))
      0.0)))

(defn calculate-sum-operation
  "Calculate sum of field values"
  [field-value]
  (cond
    (vector? field-value)
    (try (reduce + field-value) (catch Exception _ 0.0))

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
