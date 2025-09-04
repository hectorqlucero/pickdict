(ns pickdict.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [pickdict.core :as core]))

(def test-db {:dbtype "sqlite" :dbname "test.db"})

(deftest test-basic-functionality
  (testing "Basic Pick/D3 operations"
    ;; Test table creation
    (is (core/create-file! test-db "TEST_PRODUCT"
                           {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                            :name "TEXT NOT NULL"
                            :price "REAL"}))

    ;; Test record creation
    (is (core/create-record! test-db "TEST_PRODUCT"
                             {:name "Test Widget"
                              :price 9.99}))

    ;; Test record retrieval
    (let [records (core/find-all test-db "TEST_PRODUCT")]
      (is (> (count records) 0))
      (is (= (:name (first records)) "Test Widget")))

    ;; Test cleanup
    (is (core/drop-table! test-db "TEST_PRODUCT"))))

(deftest test-multivalue-parsing
  (testing "Multivalue field parsing"
    (is (core/create-file! test-db "TEST_MULTIVALUE"
                           {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                            :multivalues "TEXT"}))

    ;; Test multivalue creation and parsing
    (is (core/create-record! test-db "TEST_MULTIVALUE"
                             {:multivalues "A]B]C"}))

    (let [record (first (core/find-all test-db "TEST_MULTIVALUE"))]
      (is (= (:multivalues record) ["A" "B" "C"])))

    ;; Test cleanup
    (is (core/drop-table! test-db "TEST_MULTIVALUE"))))

(deftest test-dictionary-operations
  (testing "Dictionary field operations"
    (is (core/create-file! test-db "TEST_DICT"
                           {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                            :field1 "TEXT"
                            :field2 "TEXT"}))

    ;; Create dictionary
    (is (core/define-dictionary-field! test-db "TEST_DICT_DICT"
          "FIELD1"
          "A"
          "field1"
          ""
          "Test Field 1"))

    ;; Test with dictionary
    (is (core/create-record! test-db "TEST_DICT"
                             {:field1 "test value"}))

    (let [records (core/find-all test-db "TEST_DICT")]
      (is (> (count records) 0)))

    ;; Test cleanup
    (is (core/drop-table! test-db "TEST_DICT"))))