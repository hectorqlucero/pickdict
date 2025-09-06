(ns test-crud
  (:require [pickdict.core :as pick])
  (:gen-class))

(defn -main []
  ;; Test the CRUD example from README
  (let [db {:dbtype "sqlite" :dbname "test_crud.db"}]

    ;; Clean up
    (try (pick/drop-table db "CUSTOMER") (catch Exception _))
    (try (pick/drop-table db "PRODUCT") (catch Exception _))
    (try (pick/drop-table db "ORDERS") (catch Exception _))
    (try (pick/drop-table db "CUSTOMER_DICT") (catch Exception _))
    (try (pick/drop-table db "PRODUCT_DICT") (catch Exception _))
    (try (pick/drop-table db "ORDERS_DICT") (catch Exception _))

    ;; Create tables
    (pick/create-file! db "CUSTOMER"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :first_name "TEXT NOT NULL"
                        :last_name "TEXT NOT NULL"
                        :email "TEXT"
                        :phone_numbers "TEXT"
                        :addresses "TEXT"})

    (pick/create-file! db "PRODUCT"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :name "TEXT NOT NULL"
                        :description "TEXT"
                        :price "REAL"
                        :categories "TEXT"
                        :tags "TEXT"
                        :stock_levels "TEXT"})

    (pick/create-file! db "ORDERS"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :customer_id "INTEGER"
                        :product_ids "TEXT"
                        :quantities "TEXT"
                        :unit_prices "TEXT"
                        :order_date "TEXT"
                        :status "TEXT"})

    ;; Define computed fields
    (pick/define-dictionary-field db "CUSTOMER_DICT" "FULL_NAME" "C" "" "(str FIRST_NAME \" \" LAST_NAME)" "Full Name")
    (pick/define-dictionary-field db "PRODUCT_DICT" "TOTAL_STOCK" "C" "" "SUM:STOCK_LEVELS" "Total Stock")
    (pick/define-dictionary-field db "PRODUCT_DICT" "INVENTORY_VALUE" "C" "" "(* PRICE TOTAL_STOCK)" "Inventory Value")
    (pick/define-dictionary-field db "PRODUCT_DICT" "IS_LOW_STOCK" "C" "" "(if (< TOTAL_STOCK 10) 1 0)" "Low Stock Flag")

    ;; Define translation fields
    (pick/define-dictionary-field db "ORDERS_DICT" "CUSTOMER_NAME" "T" "2" "TCUSTOMER;FULL_NAME" "Customer Name")
    (pick/define-dictionary-field db "ORDERS_DICT" "PRODUCT_NAMES" "T" "3" "TPRODUCT;NAME" "Product Names")
    (pick/define-dictionary-field db "ORDERS_DICT" "LINE_TOTALS" "C" "" "MULTIPLY:QUANTITIES,UNIT_PRICES" "Line Totals")
    (pick/define-dictionary-field db "ORDERS_DICT" "SUBTOTAL" "C" "" "SUM:LINE_TOTALS" "Subtotal")    ;; Create sample data
    (pick/create-record db "CUSTOMER"
                        {:first_name "John"
                         :last_name "Doe"
                         :email "john.doe@email.com"
                         :phone_numbers "555-0101]555-0102"
                         :addresses "123 Main St]456 Oak Ave"})

    (pick/create-record db "PRODUCT"
                        {:name "Gaming Headset"
                         :description "High-quality gaming audio"
                         :price 199.99
                         :categories "electronics]gaming]audio"
                         :tags "bestseller]featured"
                         :stock_levels "50]25]10"})

    (pick/create-record db "PRODUCT"
                        {:name "Mechanical Keyboard"
                         :description "RGB backlit keyboard"
                         :price 149.99
                         :categories "electronics]gaming"
                         :tags "featured"
                         :stock_levels "5]2]1"})  ;; Low stock

    (let [order-id (pick/create-record db "ORDERS"
                                       {:customer_id 1
                                        :product_ids "1]2"
                                        :quantities "2]1"
                                        :unit_prices "199.99]149.99"
                                        :order_date "2025-01-15"
                                        :status "completed"})]
      (println "Created order with ID:" order-id))

    ;; Test queries
    (let [products (pick/find-all db "PRODUCT")]
      (println "All products:")
      (doseq [product products]
        (println (str "  " (:NAME product) " - Stock: " (:TOTAL_STOCK product) " - Value: $" (:INVENTORY_VALUE product) " - Low stock: " (= (:IS_LOW_STOCK product) 1))))

      (let [low-stock-products (filter #(= (:IS_LOW_STOCK %) 1) products)]
        (println "\nLow stock products:" low-stock-products))

      (let [orders (pick/find-all db "ORDERS")]
        (println "\nAll orders:" orders)
        (let [customer-orders (filter #(= (:CUSTOMER_NAME %) "John Doe") orders)]
          (println "\nCustomer orders:" customer-orders)))

      (let [total-inventory-value (reduce + (map :INVENTORY_VALUE products))]
        (println "\nTotal inventory value:" total-inventory-value))

      (let [featured-products (filter #(some #{"featured"} (:TAGS %)) products)]
        (println "\nFeatured products:" featured-products)))))