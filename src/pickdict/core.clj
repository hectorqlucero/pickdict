(ns pickdict.core
  (:require [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [clojure.string :as str]))

;; Default database connection - can be overridden by users
(def default-db
  "Default SQLite database connection. Users can override this or pass their own db spec to functions."
  {:dbtype "sqlite" :dbname "pickdict.db"})

;; Basic JSON helpers
(defn parse-json [v]
  (json/parse-string v))

(defn generate-json [v]
  (json/generate-string v))

;; Basic file operations
(defn drop-table!
  "Drop a table and its corresponding dictionary table.
   Every table has exactly one dictionary table named {table_name}_DICT.
   Example: (drop-table! \"PRODUCT\")"
  ([table] (drop-table! default-db table))
  ([db table]
   ;; Drop the main table
   (jdbc/execute! db [(str "DROP TABLE IF EXISTS " table)])
   (let [dict-name (str table "_DICT")]
     (jdbc/execute! db [(str "DROP TABLE IF EXISTS " dict-name)]))
   ;; Return success
   true))

(defn write-record!
  "Write a record to a table with meaningful column names.
   Example: (write-record! \"PRODUCT\" {:name \"Widget A\" :price 9.95})"
  ([file-name record] (write-record! default-db file-name record))
  ([db file-name record]
   (jdbc/insert! db file-name record)))

;; Dictionary functions - TRUE Pick/D3 style dictionary
;; Each table has ONE dictionary table that defines how to interpret its data
;; The dictionary is itself a Pick-style multi-value table

(defn create-dictionary!
  "Create a Pick-style dictionary table for a data table.
   Dictionary entries are stored as multi-value records.
   Example: (create-dictionary! 'INVOICE_DICT')"
  ([dict-name] (create-dictionary! default-db dict-name))
  ([db dict-name]
   ;; Create the dictionary table directly (avoid circular dependency with create-file!)
   (try
     (jdbc/execute! db
                    [(str "CREATE TABLE IF NOT EXISTS " dict-name " ("
                          "key TEXT PRIMARY KEY, "
                          "attributes TEXT)")])
     true  ;; Return success
     (catch Exception e
       (println (str "Error creating dictionary table " dict-name ": " (.getMessage e)))
       false))))

(defn create-file!
  "Create a table with meaningful column names and its corresponding dictionary.
   Every table has exactly one dictionary table named {table_name}_DICT.
   Example: (create-file! \"PRODUCT\" {:id \"INTEGER PRIMARY KEY AUTOINCREMENT\"
                                       :name \"TEXT NOT NULL\"
                                       :price \"REAL\"})"
  ([file-name columns] (create-file! default-db file-name columns))
  ([db file-name columns]
   (when-not (map? columns)
     (throw (IllegalArgumentException. "Columns must be a map of column definitions")))
   (let [column-defs (map #(str "\"" (name (first %)) "\" " (second %)) columns)]
     ;; Create the main table
     (jdbc/execute! db
                    [(str "CREATE TABLE IF NOT EXISTS " file-name " ("
                          (str/join ", " column-defs)
                          ")")])
     ;; Create the dictionary table for this table
     (let [dict-name (str file-name "_DICT")
           dict-created (create-dictionary! db dict-name)]
       (when-not dict-created
         (throw (Exception. (str "Failed to create dictionary table: " dict-name)))))
     ;; Return success
     true)))

(defn define-dictionary-field!
  "Define a field in a Pick-style dictionary using multi-value format.
   Example: (define-dictionary-field! 'INVOICE_DICT' 'DATE' 'A' '1' 'D' 'Invoice Date')
   This creates: key='DATE', attributes='A]1]D]Invoice Date'"
  ([dict-name field-name type position conversion description]
   (define-dictionary-field! default-db dict-name field-name type position conversion description))
  ([db dict-name field-name type position conversion description]
   (let [attributes (str/join "]" [type position conversion description])]
     (write-record! db dict-name {:key field-name
                                  :attributes attributes}))))

(defn get-dictionary-field
  "Get dictionary field definition from Pick-style dictionary"
  ([dict-name field-name] (get-dictionary-field default-db dict-name field-name))
  ([db dict-name field-name]
   (first (jdbc/query db [(str "SELECT * FROM " dict-name " WHERE key=?") field-name]))))

(defn get-all-dictionary-fields
  "Get all fields in a Pick-style dictionary"
  ([dict-name] (get-all-dictionary-fields default-db dict-name))
  ([db dict-name]
   (jdbc/query db [(str "SELECT * FROM " dict-name)])))

(defn parse-dictionary-attributes
  "Parse multi-value attributes from dictionary entry
   Returns: {:type 'A', :position '1', :conversion 'D', :description 'Invoice Date'}"
  [attributes]
  (when (and attributes (string? attributes))
    (let [parts (str/split attributes #"\]")]
      {:type (get parts 0)
       :position (get parts 1)
       :conversion (get parts 2)
       :description (get parts 3)})))

(defn define-dictionary-fields!
  "Define multiple dictionary fields at once using Pick multi-value format.
   attrs should be a vector of vectors: [[field-name type position conversion description] ...]"
  ([dict-name fields] (define-dictionary-fields! default-db dict-name fields))
  ([db dict-name fields]
   (doseq [[field-name type position conversion description] fields]
     (define-dictionary-field! db dict-name field-name type position conversion description))))

(defn parse-multivalue
  "Parse a multivalue string into a vector of values.
   Handles both numeric and string values appropriately."
  [value]
  (cond
    (nil? value) nil
    (and (string? value) (str/includes? value "]"))
    (let [parts (map str/trim (str/split value #"\]"))]
      (if (= (count parts) 1)
        (first parts)  ; Single value, don't return vector
        (mapv #(try (Double/parseDouble %) (catch Exception _ %)) parts)))
    :else value))

(defn format-multivalue
  "Format a multivalue vector back to string for storage"
  [value]
  (cond
    (nil? value) nil
    (vector? value) (str/join "]" (map str value))
    :else (str value)))

(defn table-exists?
  "Check if a table exists in the database"
  ([table-name] (table-exists? default-db table-name))
  ([db table-name]
   (try
     ;; Try a simple query first to see if table exists
     (let [test-query (str "SELECT 1 FROM " table-name " LIMIT 1")]
       (try
         (jdbc/query db [test-query])
         true
         (catch Exception _ false)))
     (catch Exception _ false))))

(defn read-all-records
  "Read all records from a table and parse multivalue fields into vectors"
  ([file-name] (read-all-records default-db file-name))
  ([db file-name]
   (let [raw-records (jdbc/query db [(str "SELECT * FROM " file-name)])]
     (mapv (fn [record]
             (into {} (map (fn [[k v]] [k (parse-multivalue v)]) record)))
           raw-records))))

(defn get-table-columns
  "Get column names from a table schema"
  ([table-name] (get-table-columns default-db table-name))
  ([db table-name]
   (try
     ;; Try to get column names from a sample query
     (let [sample-result (jdbc/query db [(str "SELECT * FROM " table-name " LIMIT 1")])]
       (if (seq sample-result)
         (keys (first sample-result))
         ;; Fallback: try PRAGMA if sample query returns no results
         (let [pragma-query (str "PRAGMA table_info(" table-name ")")
               pragma-result (jdbc/query db [pragma-query])]
           (map :name pragma-result))))
     (catch Exception e
       (println (str "Error getting table columns for " table-name ": " (.getMessage e)))
       []))))

(defn query-with-dictionary-fields
  "Query records using a Pick-style dictionary to define field mappings and translations.
   This is the TRUE Pick/D3 functionality - dictionary entries are multi-value records."
  ([table-name dict-name] (query-with-dictionary-fields default-db table-name dict-name))
  ([db table-name dict-name]
   (let [raw-records (read-all-records db table-name)
         dict-entries (get-all-dictionary-fields db dict-name)
         table-columns (get-table-columns db table-name)
         ;; Create dynamic position-to-column mapping
         position-mapping (into {}
                                (for [i (range 1 (inc (count table-columns)))]
                                  [i (nth table-columns (dec i) nil)]))]
     (map (fn [record]
            (reduce
             (fn [result dict-entry]
               (let [field-name (:key dict-entry)
                     attrs (parse-dictionary-attributes (:attributes dict-entry))
                     field-type (:type attrs)
                     position (let [pos (:position attrs)]
                                (if (and pos (not= pos ""))
                                  (try (Integer/parseInt pos) (catch Exception _ pos))
                                  0))
                     conversion (:conversion attrs)
                     column-name (cond
                                   ;; If position is a string, use it directly as column name
                                   (string? position) position
                                   ;; If position is numeric, map to table column
                                   (number? position) (get position-mapping position)
                                   ;; If conversion is specified for attribute fields, use it as column name
                                   (and (= field-type "A") conversion (not= conversion "")) conversion
                                   ;; Default fallback
                                   :else nil)]
                 (cond
                   ;; Attribute field - direct column mapping by position or conversion
                   (= field-type "A")
                   (if column-name
                     (let [raw-value (get record (keyword column-name))]
                       (assoc result (keyword field-name) (parse-multivalue raw-value)))
                     result)

                   ;; Translate field - lookup related table
                   (= field-type "T")
                   (if (and conversion (string? conversion) column-name)
                     (let [translate-parts (str/split conversion #";")
                           related-table (if (>= (count translate-parts) 1)
                                           (subs (first translate-parts) 1)
                                           nil)
                           related-column (if (>= (count translate-parts) 2)
                                            (second translate-parts)
                                            nil)]
                       (if (and related-table related-column)
                         (let [lookup-value (get record (keyword column-name))
                               value-list (cond
                                            ;; If it's already a vector (parsed by parse-multivalue), use it directly
                                            (vector? lookup-value) lookup-value
                                            ;; If it's a string with separators, split it
                                            (and (string? lookup-value) (str/includes? lookup-value "]"))
                                            (map str/trim (str/split lookup-value #"\]"))
                                            ;; Single value
                                            :else [lookup-value])
                               translated-values (map #(let [id-value (try (Integer/parseInt (str/trim %)) (catch Exception _ %))
                                                             related-record (try
                                                                              (first (jdbc/query db [(str "SELECT * FROM " related-table " WHERE id=?") id-value]))
                                                                              (catch Exception _ nil))]
                                                         (if related-record
                                                           (get related-record (keyword related-column))
                                                           %))
                                                      value-list)]
                           (assoc result (keyword field-name)
                                  (if (= (count translated-values) 1)
                                    (first translated-values)
                                    (vec translated-values))))
                         result))
                     result)

                   ;; Computed field - perform calculations on multi-value fields
                   (= field-type "C")
                   (if conversion
                     (cond
                       ;; Check if it's Clojure code (starts with parenthesis)
                       (and (string? conversion) (str/starts-with? (str/trim conversion) "("))
                       (let [clojure-expr (read-string (str/trim conversion))]
                         (try
                           ;; Check if the expression contains variable references
                           (let [expr-str (str clojure-expr)
                                 field-symbols (set (keys (into {} (for [[k v] result]
                                                                     [(symbol (name k)) v]))))
                                 has-variables (some #(str/includes? expr-str (name %)) field-symbols)]
                             (if has-variables
                               ;; Expression has variables - create bindings
                               (let [field-values (into {} (for [[k v] result]
                                                             [(symbol (name k))
                                                              (cond
                                                                ;; If it's a string with separators, split into sequence of numbers
                                                                (and (string? v) (str/includes? v "]"))
                                                                (map #(try (Double/parseDouble (str/trim %)) (catch Exception _ %))
                                                                     (str/split v #"\]"))
                                                                ;; If it's already a sequence, use as is
                                                                (seq? v) v
                                                                ;; If it's a number/string, try to parse as number, otherwise keep as is
                                                                :else (try (Double/parseDouble (str v)) (catch Exception _ v)))]))
                                     ;; Check if we have multi-value fields (sequences with multiple values)
                                     has-multi-values (some #(and (seq? %) (> (count %) 1)) (vals field-values))]
                                 (if has-multi-values
                                   ;; Multi-value case: evaluate expression for each index
                                   (let [max-length (try (apply max (map #(if (seq? %) (count %) 1) (vals field-values)))
                                                         (catch Exception _ 1))
                                         result-values (for [i (range max-length)]
                                                         (let [bindings (vec (try (apply concat (for [[k v] field-values]
                                                                                                  [k (if (and (seq? v) (< i (count v)))
                                                                                                       (nth v i)
                                                                                                       (if (seq? v) (first v) v))]))
                                                                                  (catch Exception _ [])))]
                                                           (eval `(let ~bindings ~clojure-expr))))]
                                     (assoc result (keyword field-name)
                                            (if (= (count result-values) 1)
                                              (first result-values)
                                              (vec result-values))))
                                   ;; Single value case: evaluate once with first values
                                   (let [bindings (vec (try (apply concat (for [[k v] field-values]
                                                                            [k (if (seq? v) (first v) v)]))
                                                            (catch Exception _ [])))
                                         result-value (eval `(let ~bindings ~clojure-expr))]
                                     (assoc result (keyword field-name) result-value))))
                               ;; Expression has no variables - evaluate directly
                               (let [result-value (eval clojure-expr)]
                                 (assoc result (keyword field-name) result-value))))
                           (catch Exception e
                             (println (str "Error evaluating Clojure expression: " conversion " - " (.getMessage e)))
                             result)))

                       ;; Legacy MULTIPLY operation
                       (and (string? conversion) (str/starts-with? conversion "MULTIPLY:"))
                       (let [operands (str/split (subs conversion 9) #",")
                             operand-values (vec (for [operand operands]
                                                   (let [field-value (get result (keyword operand))]
                                                     (cond
                                                       (and field-value (string? field-value) (str/includes? field-value "]"))
                                                       (vec (map #(try (Double/parseDouble (str/trim %)) (catch Exception _ 0.0))
                                                                 (str/split field-value #"\]")))

                                                       (and field-value (string? field-value))
                                                       [(try (Double/parseDouble field-value) (catch Exception _ 0.0))]

                                                       (and field-value (or (seq? field-value) (vector? field-value)))
                                                       (if (vector? field-value)
                                                         field-value
                                                         (vec field-value))

                                                       field-value
                                                       [field-value]

                                                       :else
                                                       [0.0]))))]
                         (if (seq operand-values)
                           (let [max-len (try (apply max (map count operand-values)) (catch Exception _ 1))
                                 padded-values (mapv #(if (< (count %) max-len)
                                                        (vec (concat % (repeat (- max-len (count %)) 0.0)))
                                                        (vec %))
                                                     operand-values)]
                             (try
                               (let [multiplied-values (apply map * padded-values)]
                                 (assoc result (keyword field-name)
                                        (if (= (count multiplied-values) 1)
                                          (first multiplied-values)
                                          (vec multiplied-values))))
                               (catch Exception e
                                 (println (str "Error in MULTIPLY operation: " (.getMessage e) " - operands: " operands " - values: " operand-values))
                                 result)))
                           result))

                       ;; Legacy SUM operation
                       (and (string? conversion) (str/starts-with? conversion "SUM:"))
                       (let [sum-field (subs conversion 4)
                             field-value (get result (keyword sum-field))]
                         (cond
                           ;; Handle vector of numbers directly
                           (and field-value (vector? field-value))
                           (let [total (try (reduce + field-value) (catch Exception _ 0.0))]
                             (assoc result (keyword field-name) total))

                           ;; Handle sequence of numbers
                           (and field-value (seq? field-value))
                           (let [values (map #(try (Double/parseDouble (str %)) (catch Exception _ 0.0)) field-value)
                                 total (try (reduce + values) (catch Exception _ 0.0))]
                             (assoc result (keyword field-name) total))

                           ;; Handle string with separators
                           (and field-value (string? field-value) (str/includes? field-value "]"))
                           (let [values (map (fn [v] (try (Double/parseDouble (str/trim v)) (catch Exception _ 0.0)))
                                             (str/split field-value #"\]"))
                                 total (try (reduce + values) (catch Exception _ 0.0))]
                             (assoc result (keyword field-name) total))

                           ;; Handle single value
                           field-value
                           (assoc result (keyword field-name) (try (Double/parseDouble (str field-value)) (catch Exception _ 0.0)))

                           :else
                           (assoc result (keyword field-name) 0.0)))

                       ;; Default - keep as is
                       :else result)
                     result)

                   ;; Default - keep as is
                   :else result)))
             {}
             dict-entries))
          raw-records))))

(defn read-all-records-with-dict
  "Read all records from a table with automatic dictionary transformations if dictionary exists and has entries"
  ([file-name] (read-all-records-with-dict default-db file-name))
  ([db file-name]
   (let [raw-records (read-all-records db file-name)
         dict-name (str file-name "_DICT")]
     (if (and (table-exists? db dict-name)
              (seq (get-all-dictionary-fields db dict-name)))  ;; Check if dictionary has entries
       (query-with-dictionary-fields db file-name dict-name)
       raw-records))))

(defn calculate-line-total
  "Calculate line total: quantity * unit_price"
  [quantity unit-price]
  (* quantity unit-price))

(defn calculate-invoice-total
  "Calculate total for an invoice from its detail lines"
  ([invoice-id] (calculate-invoice-total default-db invoice-id))
  ([db invoice-id]
   (let [details (jdbc/query db ["SELECT line_total FROM INVOICE_DETAIL WHERE invoice_id = ?" invoice-id])
         total (reduce + (map :line_total details))]
     total)))

(defn update-invoice-total!
  "Update the total amount for an invoice"
  ([invoice-id] (update-invoice-total! default-db invoice-id))
  ([db invoice-id]
   (let [total (calculate-invoice-total db invoice-id)]
     (jdbc/execute! db ["UPDATE INVOICE_HEADER SET total_amount = ? WHERE id = ?" total invoice-id])
     total)))

(defn create-invoice-line!
  "Create an invoice detail line with automatic line total calculation"
  ([invoice-id product-id quantity unit-price] (create-invoice-line! default-db invoice-id product-id quantity unit-price))
  ([db invoice-id product-id quantity unit-price]
   (let [line-total (calculate-line-total quantity unit-price)]
     (write-record! db "INVOICE_DETAIL" {:invoice_id invoice-id
                                         :product_id product-id
                                         :quantity quantity
                                         :unit_price unit-price
                                         :line_total line-total})
     ;; Update invoice total
     (update-invoice-total! db invoice-id)
     line-total)))

(defn get-complete-invoice
  "Get complete invoice with header and all detail lines"
  ([invoice-id] (get-complete-invoice default-db invoice-id))
  ([db invoice-id]
   (let [header (first (query-with-dictionary-fields db "INVOICE_HEADER" "INVOICE_HEADER_DICT"))
         details (query-with-dictionary-fields db "INVOICE_DETAIL" "INVOICE_DETAIL_DICT")]
     {:header header
      :details (filter #(= (:invoice_id %) invoice-id) details)
      :total (:total_amount header)})))

(defn query-records
  "Query records from a table. Returns data with meaningful column names.
   Supports translate fields using column names directly.
   Example: (query-records \"INVOICE\" {:customer_name \"TCUSTOMER;name\"})"
  ([file-name] (query-records default-db file-name {}))
  ([file-name translate-fields] (query-records default-db file-name translate-fields))
  ([db file-name translate-fields]
   (let [rows (read-all-records db file-name)]
     (map (fn [row]
            ;; Process translate fields
            (reduce (fn [result [field-key translate-spec]]
                      (if (and translate-spec (string? translate-spec))
                        (let [translate-parts (str/split translate-spec #";")
                              related-table (if (>= (count translate-parts) 1)
                                              (subs (first translate-parts) 1)
                                              nil)
                              related-column (if (>= (count translate-parts) 2)
                                               (second translate-parts)
                                               nil)]
                          (if (and related-table related-column)
                            (let [lookup-value (get row field-key)
                                  value-list (cond
                                               ;; If it's already a vector (parsed by parse-multivalue), use it directly
                                               (vector? lookup-value) lookup-value
                                               ;; If it's a string with separators, split it
                                               (and (string? lookup-value) (str/includes? lookup-value "]"))
                                               (map str/trim (str/split lookup-value #"\]"))
                                               ;; Single value
                                               :else [lookup-value])
                                  translated-values (map #(let [id-value (try (Integer/parseInt (str/trim %)) (catch Exception _ %))
                                                                related-record (try
                                                                                 (first (jdbc/query db [(str "SELECT * FROM " related-table " WHERE id=?") id-value]))
                                                                                 (catch Exception _ nil))]
                                                            (if related-record
                                                              (get related-record (keyword related-column))
                                                              %))
                                                         value-list)]
                              (assoc result field-key
                                     (if (= (count translated-values) 1)
                                       (first translated-values)
                                       (vec translated-values))))
                            result))
                        result))
                    row
                    translate-fields))
          rows))))

;; Generic CRUD Functions

(defn find-by-id
  "Find a record by ID from a table"
  ([table-name id] (find-by-id default-db table-name id))
  ([db table-name id]
   (when id
     (let [raw-record (first (jdbc/query db [(str "SELECT * FROM " table-name " WHERE id = ?") id]))]
       (if raw-record
         (into {} (map (fn [[k v]] [k (parse-multivalue v)]) raw-record))
         nil)))))

(defn find-by
  "Find records by criteria from a table
   Example: (find-by \"PRODUCT\" {:name \"Widget A\"})"
  ([table-name criteria] (find-by default-db table-name criteria))
  ([db table-name criteria]
   (if (empty? criteria)
     (read-all-records-with-dict db table-name)  ;; Use dict-aware version when no criteria
     (let [where-clause (str/join " AND " (map #(str (name %) " = ?") (keys criteria)))
           values (vals criteria)
           raw-records (jdbc/query db (into [(str "SELECT * FROM " table-name " WHERE " where-clause)] values))]
       (mapv (fn [record]
               (into {} (map (fn [[k v]] [k (parse-multivalue v)]) record)))
             raw-records)))))

(defn update-record!
  "Update a record in a table by ID
   Example: (update-record! \"PRODUCT\" 1 {:price 12.99})"
  ([table-name id updates] (update-record! default-db table-name id updates))
  ([db table-name id updates]
   (when (and id (seq updates))
     (let [set-clause (str/join ", " (map #(str (name %) " = ?") (keys updates)))
           values (conj (vec (vals updates)) id)]
       (jdbc/execute! db (into [(str "UPDATE " table-name " SET " set-clause " WHERE id = ?")] values))
       id))))

(defn delete-record!
  "Delete a record from a table by ID
   Example: (delete-record! \"PRODUCT\" 1)"
  ([table-name id] (delete-record! default-db table-name id))
  ([db table-name id]
   (jdbc/execute! db [(str "DELETE FROM " table-name " WHERE id = ?") id])
   id))

(defn exists?
  "Check if a record exists in a table by ID or criteria
   Example: (exists? \"PRODUCT\" 1) or (exists? \"PRODUCT\" {:name \"Widget A\"})"
  ([table-name id-or-criteria] (exists? default-db table-name id-or-criteria))
  ([db table-name id-or-criteria]
   (cond
     (number? id-or-criteria)
     (let [result (jdbc/query db [(str "SELECT 1 FROM " table-name " WHERE id = ? LIMIT 1") id-or-criteria])]
       (seq result))

     (map? id-or-criteria)
     (let [where-clause (str/join " AND " (map #(str (name %) " = ?") (keys id-or-criteria)))
           values (vals id-or-criteria)
           result (jdbc/query db (into [(str "SELECT 1 FROM " table-name " WHERE " where-clause " LIMIT 1")] values))]
       (seq result))

     :else false)))

(defn count-records
  "Count records in a table
   Example: (count-records \"PRODUCT\")"
  ([table-name] (count-records default-db table-name))
  ([db table-name]
   (let [result (jdbc/query db [(str "SELECT COUNT(*) as count FROM " table-name)])]
     (:count (first result)))))

(defn save-record!
  "Save a record - if it has an ID and exists, update it; otherwise create new
   Example: (save-record! \"PRODUCT\" {:name \"New Widget\" :price 15.99})
            (save-record! \"PRODUCT\" {:id 1 :price 12.99})"
  ([table-name record] (save-record! default-db table-name record))
  ([db table-name record]
   (if-let [id (:id record)]
     (if (exists? db table-name id)
       (do
         (update-record! db table-name id (dissoc record :id))
         id)  ;; Return the existing ID
       (do
         ;; ID provided but doesn't exist, create new record
         (write-record! db table-name (dissoc record :id))
         ;; Get the last inserted ID
         (let [result (jdbc/query db ["SELECT last_insert_rowid() as id"])]
           (:id (first result)))))
     (do
       ;; No ID provided, create new record
       (write-record! db table-name record)
       ;; Get the last inserted ID
       (let [result (jdbc/query db ["SELECT last_insert_rowid() as id"])]
         (:id (first result)))))))

(defn find-first
  "Find the first record matching criteria
   Example: (find-first \"PRODUCT\" {:name \"Widget A\"})"
  ([table-name criteria] (find-first default-db table-name criteria))
  ([db table-name criteria]
   (first (find-by db table-name criteria))))

(defn find-all
  "Alias for read-all-records-with-dict for consistency - automatically applies dictionary transformations"
  ([table-name] (read-all-records-with-dict table-name))
  ([db table-name] (read-all-records-with-dict db table-name)))

(defn create-record!
  "Alias for write-record! for consistency"
  ([table-name record] (write-record! table-name record))
  ([db table-name record] (write-record! db table-name record)))

(comment
  ;; ==========================================
  ;; PICK/D3 QUICK START GUIDE
  ;; ==========================================
  ;;
  ;; Welcome to Pick/D3! This guide will get you up and running quickly.
  ;;
  ;; 1. SETUP YOUR FIRST TABLES
  ;; --------------------------
  ;;
  ;; Create tables with meaningful column names:
  ;;
  ;; (create-file! "PRODUCT"
  ;;               {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
  ;;                :name "TEXT NOT NULL"
  ;;                :price "REAL"
  ;;                :category "TEXT"})
  ;;
  ;; (create-file! "CUSTOMER"
  ;;               {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
  ;;                :name "TEXT NOT NULL"
  ;;                :email "TEXT"
  ;;                :phone "TEXT"})
  ;;
  ;; (create-file! "INVOICE"
  ;;               {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
  ;;                :customer_id "INTEGER"
  ;;                :date "DATE"
  ;;                :product_ids "TEXT"    ;; Multi-value: "1]2]3"
  ;;                :quantities "TEXT"     ;; Multi-value: "2]1]5"
  ;;                :unit_prices "TEXT"})  ;; Multi-value: "10.99]19.95]5.99"
  ;;
  ;;
  ;; 2. DEFINE DICTIONARY FIELDS
  ;; ---------------------------
  ;;
  ;; Define how to interpret your data using Pick-style dictionaries.
  ;; The system automatically detects table schemas and supports flexible field mappings:
  ;;
  ;; ;; Numeric positions (1, 2, 3...) map to table columns by position
  ;; (define-dictionary-field! "INVOICE_DICT" "CUSTOMER_ID" "A" "1" "" "Customer ID")
  ;; (define-dictionary-field! "INVOICE_DICT" "PRODUCT_IDS" "A" "4" "" "Product IDs")
  ;;
  ;; ;; String positions map directly to column names
  ;; (define-dictionary-field! "PRODUCT_DICT" "NAME" "A" "name" "" "Product Name")
  ;; (define-dictionary-field! "PRODUCT_DICT" "PRICE" "A" "price" "" "Unit Price")
  ;;
  ;; ;; Translate fields lookup related tables
  ;; (define-dictionary-field! "INVOICE_DICT" "CUSTOMER_NAME" "T" "1" "TCUSTOMER;name" "Customer Name")
  ;; (define-dictionary-field! "INVOICE_DICT" "PRODUCT_NAMES" "T" "4" "TPRODUCT;name" "Product Names")
  ;;
  ;; ;; Computed fields perform calculations
  ;; (define-dictionary-field! "INVOICE_DICT" "LINE_TOTALS" "C" "" "MULTIPLY:QUANTITIES,UNIT_PRICES" "Line Totals")
  ;; (define-dictionary-field! "INVOICE_DICT" "INVOICE_TOTAL" "C" "" "SUM:LINE_TOTALS" "Invoice Total")
  ;;
  ;; ;; Works with ANY table schema - no hardcoded mappings required!
  ;;
  ;;
  ;; 3. CRUD OPERATIONS
  ;; ------------------
  ;;
  ;; The system provides multiple ways to access your data:
  ;;
  ;; ;; AUTOMATIC DICTIONARY APPLICATION (Recommended - True Pick/D3 behavior)
  ;; (find-all "PRODUCT")                    ;; Uses PRODUCT_DICT automatically
  ;; (find-all "INVOICE")                    ;; Uses INVOICE_DICT automatically
  ;; (find-by "PRODUCT" {})                  ;; Uses PRODUCT_DICT automatically
  ;; (find-by "CUSTOMER" {})                 ;; Uses CUSTOMER_DICT automatically
  ;;
  ;; ;; PARSED DATA WITHOUT DICTIONARY (Multivalue fields parsed to vectors)
  ;; (find-by-id "PRODUCT" 1)               ;; Returns {:id 1, :name "Widget A", :price 9.95}
  ;; (find-by "PRODUCT" {:category "Electronics"}) ;; Parsed multivalue, no dict transform
  ;; (find-first "PRODUCT" {:name "Widget A"})     ;; Parsed multivalue, no dict transform
  ;;
  ;; ;; RAW DATA ACCESS (No transformations)
  ;; (read-all-records "PRODUCT")            ;; Raw database data with multivalue parsing only
  ;;
  ;; ;; MANUAL DICTIONARY APPLICATION (For custom dictionaries or special cases)
  ;; (query-with-dictionary-fields "INVOICE" "INVOICE_DICT") ;; Explicit dictionary application
  ;;
  ;; ;; CREATE
  ;; (create-record! "PRODUCT" {:name "Widget A" :price 10.99 :category "Electronics"})
  ;; (create-record! "CUSTOMER" {:name "John Doe" :email "john@example.com"})
  ;;
  ;; ;; UPDATE
  ;; (update-record! "PRODUCT" 1 {:price 12.99})  ;; Update by ID
  ;;
  ;; ;; DELETE
  ;; (delete-record! "PRODUCT" 1)           ;; Delete by ID
  ;;
  ;; ;; SMART SAVE (create or update automatically)
  ;; (save-record! "PRODUCT" {:name "New Widget" :price 15.99})  ;; Creates new
  ;; (save-record! "PRODUCT" {:id 1 :price 14.99})              ;; Updates existing
  ;;
  ;; ;; UTILITY FUNCTIONS
  ;; (count-records "PRODUCT")              ;; Count records
  ;; (exists? "PRODUCT" 1)                  ;; Check if exists
  ;; (exists? "PRODUCT" {:name "Widget A"}) ;; Check by criteria
  ;;
  ;;
  ;; 4. WORKING WITH MULTIVALUE FIELDS
  ;; ----------------------------------
  ;;
  ;; Pick/D3 handles multivalue fields automatically in ANY table schema:
  ;;
  ;; ;; Create invoice with multivalue fields
  ;; (create-record! "INVOICE"
  ;;                 {:customer_id 1
  ;;                  :date "2025-09-02"
  ;;                  :product_ids "1]2]3"      ;; Multiple product IDs
  ;;                  :quantities "2]1]5"       ;; Corresponding quantities
  ;;                  :unit_prices "10.99]19.95]5.99"}) ;; Unit prices
  ;;
  ;; ;; Multivalue fields are automatically parsed into vectors
  ;; ;; product_ids becomes [1 2 3]
  ;; ;; quantities becomes [2.0 1.0 5.0]
  ;; ;; unit_prices becomes [10.99 19.95 5.99]
  ;;
  ;; ;; The system works with ANY table - no hardcoded mappings required
  ;; (create-record! "SOME_TABLE" {:field1 "A]B]C" :field2 "1]2]3"})
  ;; ;; Automatically becomes: {:field1 ["A" "B" "C"] :field2 [1 2 3]}
  ;;
  ;;
  ;; 5. AUTOMATIC DICTIONARY APPLICATION
  ;; -----------------------------------
  ;;
  ;; The system now automatically applies dictionary transformations:
  ;;
  ;; ;; Automatic dictionary application (Recommended - True Pick/D3 behavior)
  ;; (find-all "INVOICE")  ;; Automatically uses INVOICE_DICT if it exists
  ;; (find-all "PRODUCT")  ;; Automatically uses PRODUCT_DICT if it exists
  ;; (find-all "CUSTOMER") ;; Automatically uses CUSTOMER_DICT if it exists
  ;;
  ;; ;; Returns records like:
  ;; ;; {:CUSTOMER_NAME "John Doe"
  ;; ;;  :PRODUCT_NAMES ["Widget A" "Widget B" "Widget C"]
  ;; ;;  :QUANTITIES [2.0 1.0 5.0]
  ;; ;;  :UNIT_PRICES [10.99 19.95 5.99]
  ;; ;;  :LINE_TOTALS [21.98 19.95 29.95]    ;; Computed: qty * price
  ;; ;;  :INVOICE_TOTAL 71.88}               ;; Computed: sum of line totals
  ;;
  ;; ;; Manual dictionary application (for special cases only)
  ;; (query-with-dictionary-fields "INVOICE" "INVOICE_DICT") ;; Explicit control
  ;;
  ;;
  ;; 6. ADVANCED FEATURES
  ;; --------------------
  ;;
  ;; ;; Custom Clojure expressions in computed fields
  ;; (define-dictionary-field! "INVOICE_DICT" "TAX" "C" "" "(* INVOICE_TOTAL 0.08)" "Tax Amount")
  ;;
  ;; ;; Complex queries with multiple criteria
  ;; (find-by "PRODUCT" {:category "Electronics" :price 10.99})
  ;;
  ;; ;; Batch operations
  ;; (doseq [product [{:name "A" :price 1.99} {:name "B" :price 2.99}]]
  ;;   (create-record! "PRODUCT" product))
  ;;
  ;; ;; Works with ANY table schema - completely generic!
  ;; (find-all "ANY_TABLE")  ;; Automatically applies ANY_TABLE_DICT if it exists
  ;;
  ;;
  ;; 7. CLEANUP
  ;; ----------
  ;;
  ;; ;; Drop tables (also drops associated dictionaries)
  ;; (drop-table! "PRODUCT")
  ;; (drop-table! "CUSTOMER")
  ;; (drop-table! "INVOICE")
  ;;
  ;;
  ;; 🎉 You're now ready to build powerful Pick/D3 applications with ANY table schema!
  ;; The system automatically detects table structures and applies dictionary transformations.
  ;; For more advanced features, check the function documentation above.
  ;;
  ;; ==========================================
  ;; END OF QUICK START GUIDE
  ;; ==========================================
  )
