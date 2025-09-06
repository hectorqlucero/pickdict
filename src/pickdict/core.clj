
(ns pickdict.core
  (:require [pickdict.database :as db]
            [pickdict.multivalue :as mv]
            [pickdict.dictionary :as dict]
            [pickdict.crud :as crud]))

;; Re-export database functions
(def execute-query db/execute-query)
(def execute-command db/execute-command)
(def table-exists? db/table-exists?)
(def create-table db/create-table)
(def drop-table db/drop-table)

;; Re-export multivalue functions
(def parse-multivalue mv/parse-multivalue)
(def format-multivalue mv/format-multivalue)
(def extract-multivalue-numbers mv/extract-multivalue-numbers)

;; Re-export dictionary functions
(def create-dictionary-table dict/create-dictionary-table)
(def define-dictionary-field dict/define-dictionary-field)
(def translate-field-value dict/translate-field-value)
(def evaluate-clojure-expression dict/evaluate-clojure-expression)


;; Re-export CRUD functions
(def create-record crud/create-record)
(def find-by-id crud/find-by-id)
(def update-record crud/update-record)
(def delete-record crud/delete-record)
(def find-all crud/read-all-records)


;; Utility functions
(defn query-with-dictionary-fields
  "Query records using a Pick-style dictionary to define field mappings and translations"
  ([table-name dict-name] (query-with-dictionary-fields db/default-db table-name dict-name))
  ([db table-name dict-name]
   ;; This is a placeholder - the actual implementation would be complex
   ;; For now, just return the raw records
   (crud/read-all-records db table-name)))

(defn create-file!
  "Create a table and its associated dictionary table (Pick/D3 style)"
  ([table-name schema] (create-file! db/default-db table-name schema))
  ([db table-name schema]
   ;; Create the main table
   (create-table db table-name schema)
   ;; Create the dictionary table
   (let [dict-name (str table-name "_DICT")]
     (create-dictionary-table db dict-name))
   true))
