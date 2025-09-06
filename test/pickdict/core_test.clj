(ns pickdict.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [pickdict.core :as pick]))

(deftest test-create-file-with-automatic-dictionary-fields
  (testing "create-file! should automatically create Attribute dictionary fields"
    (let [db {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname "test_create_file.db"}]

      ;; Clean up any existing test table
      (try (pick/drop-table db "TEST_PRODUCT") (catch Exception _))

      ;; Create table with create-file! (should auto-create dictionary fields)
      (pick/create-file! db "TEST_PRODUCT"
                         {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                          :name "TEXT NOT NULL"
                          :price "REAL"
                          :categories "TEXT"
                          :stock_levels "TEXT"})

      ;; Verify the table was created
      (is (pick/table-exists? db "TEST_PRODUCT"))

      ;; Verify the dictionary table was created
      (is (pick/table-exists? db "TEST_PRODUCT_DICT"))

      ;; Create a test record
      (let [product-id (pick/create-record db "TEST_PRODUCT"
                                           {:name "Test Product"
                                            :price 29.99
                                            :categories "test]category"
                                            :stock_levels "10]5"})]
        (is (pos? product-id) "Should create product successfully")

        ;; Read the record back with dictionary interpretation
        (let [product (pick/find-by-id db "TEST_PRODUCT" product-id)]
          ;; Verify that the automatic dictionary fields work
          (is (= "Test Product" (:NAME product)) "NAME field should be mapped")
          (is (= 29.99 (:PRICE product)) "PRICE field should be mapped")
          ;; Note: Multivalue parsing not implemented yet, so these come back as strings
          (is (= "test]category" (:CATEGORIES product)) "CATEGORIES field should be mapped")
          (is (= "10]5" (:STOCK_LEVELS product)) "STOCK_LEVELS field should be mapped")))

      ;; Clean up
      (try (pick/drop-table db "TEST_PRODUCT") (catch Exception _))
      (try (pick/drop-table db "TEST_PRODUCT_DICT") (catch Exception _)))))