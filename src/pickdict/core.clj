
(ns pickdict.core
  (:require [pickdict.database :as db]
            [pickdict.dictionary :as dict]
            [pickdict.crud :as crud]
            [clojure.string :as str]))

;; Re-export database functions
(def execute-query db/execute-query)
(def execute-command db/execute-command)
(def table-exists? db/table-exists?)
(def create-table db/create-table)
(def drop-table db/drop-table)

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
(defn create-file!
  "Create a table and its associated dictionary table (Pick/D3 style)"
  ([table-name schema] (create-file! db/default-db table-name schema))
  ([db table-name schema]
   ;; Create the main table
   (create-table db table-name schema)
   ;; Create the dictionary table
   (let [dict-name (str table-name "_DICT")]
     (create-dictionary-table db dict-name)
     ;; Automatically create Attribute fields for each column (Pick/D3 style)
     (let [columns (keys schema)
           ;; Skip 'id' column as it's typically the primary key
           data-columns (remove #(= % :id) columns)]
       (doseq [[index column] (map-indexed vector data-columns)]
         (let [field-name (str/upper-case (name column))
               position (str (inc index))  ;; 1-based position
               description (str/replace
                            (str/capitalize (name column))
                            #"_" " ")]
           (define-dictionary-field db dict-name field-name "A" position "" description)))))
   true))
