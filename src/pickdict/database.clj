(ns pickdict.database
  "Database connection and basic operations"
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]))

;; Configuration
(def default-db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "example.db"})

;; SQL Construction Functions
(defn build-select-sql
  "Build a SELECT SQL statement"
  [table-name & {:keys [where limit offset]}]
  (cond-> (str "SELECT * FROM " table-name)
    where (str " WHERE " where)
    limit (str " LIMIT " limit)
    offset (str " OFFSET " offset)))

(defn build-insert-sql
  "Build an INSERT SQL statement"
  [table-name columns]
  (let [column-names (str/join ", " (map name columns))
        placeholders (str/join ", " (repeat (count columns) "?"))]
    (str "INSERT INTO " table-name " (" column-names ") VALUES (" placeholders ")")))

(defn build-update-sql
  "Build an UPDATE SQL statement"
  [table-name columns]
  (let [set-clause (str/join ", " (map #(str (name %) " = ?") columns))]
    (str "UPDATE " table-name " SET " set-clause " WHERE id = ?")))

(defn build-delete-sql
  "Build a DELETE SQL statement"
  [table-name]
  (str "DELETE FROM " table-name " WHERE id = ?"))

(defn build-count-sql
  "Build a COUNT SQL statement"
  [table-name]
  (str "SELECT COUNT(*) as count FROM " table-name))

(defn build-table-exists-sql
  "Build SQL to check if table exists"
  []
  "SELECT name FROM sqlite_master WHERE type='table' AND name=?")

(defn build-table-info-sql
  "Build SQL to get table information"
  [table-name]
  (str "PRAGMA table_info(" table-name ")"))

(defn build-create-table-sql
  "Build CREATE TABLE SQL statement"
  [table-name columns]
  (let [column-defs (str/join ", " (map #(str (name (first %)) " " (second %)) columns))]
    (str "CREATE TABLE IF NOT EXISTS " table-name " (" column-defs ")")))

(defn build-drop-table-sql
  "Build DROP TABLE SQL statement"
  [table-name]
  (str "DROP TABLE IF EXISTS " table-name))

;; Database Execution Functions
(defn execute-query
  "Execute a SQL query and return results"
  [db sql params]
  (jdbc/query db (into [sql] params)))

(defn execute-command
  "Execute a SQL command and return success status"
  [db sql params]
  (jdbc/execute! db (into [sql] params))
  true)

;; Error Handling
(defn safe-db-operation
  "Execute database operation with proper error handling"
  [operation]
  (try
    (operation)
    (catch Exception e
      (throw e))))

(defn try-db-operation
  "Execute database operation, returning nil on error"
  [operation]
  (try
    (operation)
    (catch Exception _
      nil)))

;; Table Operations
(defn table-exists?
  "Check if a table exists in the database"
  [db table-name]
  (try-db-operation
   #(let [result (execute-query db (build-table-exists-sql) [table-name])]
      (boolean (seq result)))))

(defn get-table-columns
  "Get column names for a table"
  [db table-name]
  (try-db-operation
   #(let [result (execute-query db (build-table-info-sql table-name) [])]
      (map :name result))))

(defn create-table
  "Create a table with given columns"
  [db table-name columns]
  (try-db-operation
   #(execute-command db (build-create-table-sql table-name columns) [])))

(defn drop-table
  "Drop a table from the database"
  [db table-name]
  (try-db-operation
   #(execute-command db (build-drop-table-sql table-name) [])))

;; Record Operations
;; Record Operations
(defn extract-generated-id
  "Extract the generated ID from insert result"
  [insert-result]
  (if (seq insert-result)
    (let [result (first insert-result)]
      (or (:last_insert_rowid result)
          (:id result)
          (:generated_key result)
          (first (vals result))
          0))
    0))

(defn insert-record
  "Insert a record and return the generated ID"
  [db table-name record]
  (safe-db-operation
   #(let [insert-result (jdbc/insert! db table-name record)]
      (extract-generated-id insert-result))))

;; Query Operations
(defn find-by-id
  "Find a record by ID"
  [db table-name id]
  (try-db-operation
   #(let [sql (build-select-sql table-name :where "id = ?")
          result (execute-query db sql [id])]
      (first result))))

(defn find-by-criteria
  "Find records by criteria"
  [db table-name criteria]
  (let [where-clause (str/join " AND " (map #(str (name %) " = ?") (keys criteria)))
        values (vals criteria)
        sql (build-select-sql table-name :where where-clause)]
    (try-db-operation
     #(execute-query db sql values))))

(defn find-all
  "Find all records in a table"
  [db table-name]
  (try-db-operation
   #(execute-query db (build-select-sql table-name) [])))

(defn update-record
  "Update a record by ID"
  [db table-name id updates]
  (try-db-operation
   #(let [columns (keys updates)
          values (vals updates)
          sql (build-update-sql table-name columns)
          params (conj (vec values) id)]
      (execute-command db sql params))))

(defn delete-record
  "Delete a record by ID"
  [db table-name id]
  (try-db-operation
   #(execute-command db (build-delete-sql table-name) [id])))

(defn count-records
  "Count records in a table"
  [db table-name]
  (try-db-operation
   #(let [result (execute-query db (build-count-sql table-name) [])]
      (:count (first result) 0))));; Utility Functions
;; Utility Functions
(defn transaction
  "Execute operations within a database transaction"
  [db operations]
  (safe-db-operation
   #(jdbc/with-db-transaction [tx db]
      (doseq [op operations]
        (op tx)))))

(defn batch-insert
  "Insert multiple records in a batch"
  [db table-name records]
  (safe-db-operation
   #(jdbc/insert-multi! db table-name records)))
