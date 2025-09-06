(ns pickdict.dictionary-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [pickdict.core :as pick]))

(def test-db {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname "test_dictionary.db"})

(defn setup-teardown [f]
  (try
    (pick/drop-table test-db "TEST_CUSTOMER")
    (pick/drop-table test-db "TEST_PRODUCT")
    (pick/drop-table test-db "TEST_ORDERS")
    (catch Exception _))
  (f)
  (try
    (pick/drop-table test-db "TEST_CUSTOMER")
    (pick/drop-table test-db "TEST_PRODUCT")
    (pick/drop-table test-db "TEST_ORDERS")
    (catch Exception _)))

(use-fixtures :each setup-teardown)

(deftest test-dictionary-field-types
  (testing "Dictionary field types (A, C, T) work correctly"
    ;; Create test tables
    (pick/create-file! test-db "TEST_CUSTOMER"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :first_name "TEXT NOT NULL"
                        :last_name "TEXT NOT NULL"
                        :email "TEXT"})

    (pick/create-file! test-db "TEST_PRODUCT"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :name "TEXT NOT NULL"
                        :price "REAL"
                        :stock_levels "TEXT"})

    (pick/create-file! test-db "TEST_ORDERS"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :customer_id "INTEGER"
                        :product_ids "TEXT"
                        :quantities "TEXT"})

    ;; Define computed field (C-type)
    (pick/define-dictionary-field test-db "TEST_CUSTOMER_DICT" "FULL_NAME" "C" ""
      "(str FIRST_NAME \" \" LAST_NAME)" "Full Name")

    ;; Define translation field (T-type)
    (pick/define-dictionary-field test-db "TEST_ORDERS_DICT" "CUSTOMER_NAME" "T" "1"
      "TTEST_CUSTOMER;FULL_NAME" "Customer Name")

    ;; Define computed field with SUM operation
    (pick/define-dictionary-field test-db "TEST_PRODUCT_DICT" "TOTAL_STOCK" "C" ""
      "SUM:STOCK_LEVELS" "Total Stock")

    ;; Create test data
    (let [customer-id (pick/create-record test-db "TEST_CUSTOMER"
                                          {:first_name "John"
                                           :last_name "Doe"
                                           :email "john@example.com"})
          product-id (pick/create-record test-db "TEST_PRODUCT"
                                         {:name "Test Product"
                                          :price 29.99
                                          :stock_levels "10]5]3"})
          order-id (pick/create-record test-db "TEST_ORDERS"
                                       {:customer_id customer-id
                                        :product_ids (str product-id)
                                        :quantities "2"})]

      ;; Test computed field
      (let [customer (pick/find-by-id test-db "TEST_CUSTOMER" customer-id)]
        (is (= "John Doe" (:FULL_NAME customer)) "Computed field should work"))

      ;; Test translation field
      (let [order (pick/find-by-id test-db "TEST_ORDERS" order-id)]
        (is (= "John Doe" (:CUSTOMER_NAME order)) "Translation field should work"))

      ;; Test SUM operation
      (let [product (pick/find-by-id test-db "TEST_PRODUCT" product-id)]
        (is (= 18.0 (:TOTAL_STOCK product)) "SUM operation should work")))))

(deftest test-multivalue-operations
  (testing "Multivalue fields and operations work correctly"
    (pick/create-file! test-db "TEST_PRODUCT"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :name "TEXT NOT NULL"
                        :prices "TEXT"
                        :quantities "TEXT"})

    ;; Define MULTIPLY operation
    (pick/define-dictionary-field test-db "TEST_PRODUCT_DICT" "LINE_TOTALS" "C" ""
      "MULTIPLY:PRICES,QUANTITIES" "Line Totals")

    ;; Define SUM operation on multivalue result
    (pick/define-dictionary-field test-db "TEST_PRODUCT_DICT" "SUBTOTAL" "C" ""
      "SUM:LINE_TOTALS" "Subtotal")

    ;; Create test data
    (let [product-id (pick/create-record test-db "TEST_PRODUCT"
                                         {:name "Test Product"
                                          :prices "10.0]20.0]30.0"
                                          :quantities "2]3]1"})]

      ;; Test MULTIPLY operation
      (let [product (pick/find-by-id test-db "TEST_PRODUCT" product-id)]
        (is (= [20.0 60.0 30.0] (:LINE_TOTALS product)) "MULTIPLY operation should work")
        (is (= 110.0 (:SUBTOTAL product)) "SUM on MULTIPLY result should work")))))

(deftest test-case-insensitive-column-matching
  (testing "Case insensitive column matching works for translations"
    (pick/create-file! test-db "TEST_CUSTOMER"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :first_name "TEXT NOT NULL"
                        :last_name "TEXT NOT NULL"})

    (pick/create-file! test-db "TEST_ORDERS"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :customer_id "INTEGER"})

    ;; Define translation with lowercase column name
    (pick/define-dictionary-field test-db "TEST_ORDERS_DICT" "CUSTOMER_FIRST" "T" "1"
      "TTEST_CUSTOMER;first_name" "Customer First Name")

    ;; Create test data
    (let [customer-id (pick/create-record test-db "TEST_CUSTOMER"
                                          {:first_name "Jane"
                                           :last_name "Smith"})
          order-id (pick/create-record test-db "TEST_ORDERS"
                                       {:customer_id customer-id})]

      ;; Test case insensitive matching
      (let [order (pick/find-by-id test-db "TEST_ORDERS" order-id)]
        (is (= "Jane" (:CUSTOMER_FIRST order)) "Case insensitive column matching should work")))))

(deftest test-expression-evaluation
  (testing "Clojure expressions evaluate correctly"
    (pick/create-file! test-db "TEST_PRODUCT"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :name "TEXT NOT NULL"
                        :price "REAL"
                        :stock "INTEGER"})

    ;; Define expression with arithmetic
    (pick/define-dictionary-field test-db "TEST_PRODUCT_DICT" "INVENTORY_VALUE" "C" ""
      "(* PRICE STOCK)" "Inventory Value")

    ;; Define expression with conditionals
    (pick/define-dictionary-field test-db "TEST_PRODUCT_DICT" "IS_EXPENSIVE" "C" ""
      "(if (> PRICE 100) 1 0)" "Is Expensive Flag")

    ;; Create test data
    (let [product-id (pick/create-record test-db "TEST_PRODUCT"
                                         {:name "Expensive Product"
                                          :price 150.0
                                          :stock 10})]

      ;; Test arithmetic expression
      (let [product (pick/find-by-id test-db "TEST_PRODUCT" product-id)]
        (is (= 1500.0 (:INVENTORY_VALUE product)) "Arithmetic expression should work")
        (is (= 1 (:IS_EXPENSIVE product)) "Conditional expression should work")))))
