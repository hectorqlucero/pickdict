(ns example-usage
  "Complete CRUD examples for the PickDict library"
  (:require [pickdict.core :as pick]))

;; Complete CRUD Examples for PickDict Library

(defn setup-database []
  "Set up database connection and create tables"
  (let [db {:dbtype "sqlite" :dbname "example.db"}]

    ;; Clean up any existing tables
    (try (pick/drop-table! db "ORDER") (catch Exception _))
    (try (pick/drop-table! db "CUSTOMER") (catch Exception _))
    (try (pick/drop-table! db "PRODUCT") (catch Exception _))

    ;; Create CUSTOMER table
    (pick/create-file! db "CUSTOMER"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :name "TEXT NOT NULL"
                        :email "TEXT"
                        :phone "TEXT"
                        :address "TEXT"})

    ;; Create PRODUCT table with multivalue fields
    (pick/create-file! db "PRODUCT"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :name "TEXT NOT NULL"
                        :price "REAL"
                        :categories "TEXT"      ;; Multivalue: "electronics]popular]new"
                        :stock_levels "TEXT"    ;; Multivalue: "10]5]20"
                        :tags "TEXT"})          ;; Multivalue: "bestseller]clearance]featured"

    ;; Create ORDER table with multivalue relationships
    (pick/create-file! db "ORDER"
                       {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                        :customer_id "INTEGER"
                        :product_ids "TEXT"     ;; Multivalue: "1]2]3"
                        :quantities "TEXT"      ;; Multivalue: "2]1]5"
                        :unit_prices "TEXT"     ;; Multivalue: "9.95]19.95]29.95"
                        :order_date "TEXT"
                        :status "TEXT"})

    ;; Create dictionaries
    (pick/create-dictionary! db "CUSTOMER_DICT")
    (pick/create-dictionary! db "PRODUCT_DICT")
    (pick/create-dictionary! db "ORDER_DICT")

    ;; Define CUSTOMER dictionary fields
    (pick/define-dictionary-field! db "CUSTOMER_DICT" "CUSTOMER_NAME" "A" "1" "" "Customer Name")
    (pick/define-dictionary-field! db "CUSTOMER_DICT" "EMAIL" "A" "2" "" "Email Address")
    (pick/define-dictionary-field! db "CUSTOMER_DICT" "PHONE" "A" "3" "" "Phone Number")
    (pick/define-dictionary-field! db "CUSTOMER_DICT" "FULL_ADDRESS" "A" "4" "" "Full Address")

    ;; Define PRODUCT dictionary fields
    (pick/define-dictionary-field! db "PRODUCT_DICT" "PRODUCT_NAME" "A" "1" "" "Product Name")
    (pick/define-dictionary-field! db "PRODUCT_DICT" "PRICE" "A" "2" "" "Unit Price")
    (pick/define-dictionary-field! db "PRODUCT_DICT" "CATEGORIES" "A" "3" "" "Categories")
    (pick/define-dictionary-field! db "PRODUCT_DICT" "STOCK_LEVELS" "A" "4" "" "Stock Levels")
    (pick/define-dictionary-field! db "PRODUCT_DICT" "TAGS" "A" "5" "" "Tags")

    ;; Computed fields for PRODUCT
    (pick/define-dictionary-field! db "PRODUCT_DICT" "TOTAL_STOCK" "C" "" "SUM:STOCK_LEVELS" "Total Stock")
    (pick/define-dictionary-field! db "PRODUCT_DICT" "INVENTORY_VALUE" "C" "" "(* PRICE TOTAL_STOCK)" "Inventory Value")
    (pick/define-dictionary-field! db "PRODUCT_DICT" "IS_LOW_STOCK" "C" "" "(< TOTAL_STOCK 10)" "Low Stock Alert")

    ;; Define ORDER dictionary fields
    (pick/define-dictionary-field! db "ORDER_DICT" "CUSTOMER_NAME" "T" "1" "TCUSTOMER;name" "Customer Name")
    (pick/define-dictionary-field! db "ORDER_DICT" "PRODUCT_NAMES" "T" "2" "TPRODUCT;name" "Product Names")
    (pick/define-dictionary-field! db "ORDER_DICT" "QUANTITIES" "A" "3" "" "Quantities")
    (pick/define-dictionary-field! db "ORDER_DICT" "UNIT_PRICES" "A" "4" "" "Unit Prices")
    (pick/define-dictionary-field! db "ORDER_DICT" "ORDER_DATE" "A" "5" "" "Order Date")
    (pick/define-dictionary-field! db "ORDER_DICT" "STATUS" "A" "6" "" "Order Status")

    ;; Computed fields for ORDER
    (pick/define-dictionary-field! db "ORDER_DICT" "LINE_TOTALS" "C" "" "MULTIPLY:QUANTITIES,UNIT_PRICES" "Line Totals")
    (pick/define-dictionary-field! db "ORDER_DICT" "SUBTOTAL" "C" "" "SUM:LINE_TOTALS" "Subtotal")
    (pick/define-dictionary-field! db "ORDER_DICT" "TAX" "C" "" "(* SUBTOTAL 0.08)" "Tax (8%)")
    (pick/define-dictionary-field! db "ORDER_DICT" "TOTAL" "C" "" "(+ SUBTOTAL TAX)" "Total Amount")

    db))

(defn create-sample-data [db]
  "Create sample data for all tables"
  (println "Creating sample data...")

  ;; Create customers
  (pick/create-record! db "CUSTOMER"
                       {:name "John Doe"
                        :email "john.doe@email.com"
                        :phone "555-0101"
                        :address "123 Main St, Anytown, USA"})

  (pick/create-record! db "CUSTOMER"
                       {:name "Jane Smith"
                        :email "jane.smith@email.com"
                        :phone "555-0102"
                        :address "456 Oak Ave, Somewhere, USA"})

  ;; Create products
  (pick/create-record! db "PRODUCT"
                       {:name "Wireless Headphones"
                        :price 99.99
                        :categories "electronics]audio]wireless"
                        :stock_levels "50]30]20"
                        :tags "bestseller]featured]premium"})

  (pick/create-record! db "PRODUCT"
                       {:name "Gaming Mouse"
                        :price 49.99
                        :categories "electronics]gaming]peripheral"
                        :stock_levels "100]75]25"
                        :tags "bestseller]clearance"})

  (pick/create-record! db "PRODUCT"
                       {:name "USB Cable"
                        :price 9.99
                        :categories "electronics]accessory]basic"
                        :stock_levels "200]150]50"
                        :tags "essentials]budget"})

  ;; Create orders
  (pick/create-record! db "ORDER"
                       {:customer_id 1
                        :product_ids "1]2"
                        :quantities "2]1"
                        :unit_prices "99.99]49.99"
                        :order_date "2025-01-15"
                        :status "completed"})

  (pick/create-record! db "ORDER"
                       {:customer_id 2
                        :product_ids "2]3"
                        :quantities "3]5"
                        :unit_prices "49.99]9.99"
                        :order_date "2025-01-16"
                        :status "pending"})

  (println "Sample data created successfully!"))

(defn demonstrate-read-operations [db]
  "Demonstrate various read operations"
  (println "\n=== READ OPERATIONS ===")

  ;; Read all customers
  (println "\nAll Customers:")
  (doseq [customer (pick/find-all db "CUSTOMER")]
    (println (str "  " (:CUSTOMER_NAME customer) " - " (:EMAIL customer))))

  ;; Read all products with computed fields
  (println "\nAll Products with computed fields:")
  (doseq [product (pick/find-all db "PRODUCT")]
    (println (str "  " (:PRODUCT_NAME product)
                  " - Price: $" (:PRICE product)
                  " - Total Stock: " (:TOTAL_STOCK product)
                  " - Inventory Value: $" (:INVENTORY_VALUE product)
                  " - Low Stock: " (:IS_LOW_STOCK product))))

  ;; Read all orders with translations and computations
  (println "\nAll Orders with full interpretation:")
  (doseq [order (pick/find-all db "ORDER")]
    (println (str "  Customer: " (:CUSTOMER_NAME order)))
    (println (str "  Products: " (:PRODUCT_NAMES order)))
    (println (str "  Quantities: " (:QUANTITIES order)))
    (println (str "  Line Totals: " (:LINE_TOTALS order)))
    (println (str "  Subtotal: $" (:SUBTOTAL order)))
    (println (str "  Tax: $" (:TAX order)))
    (println (str "  Total: $" (:TOTAL order)))
    (println (str "  Status: " (:STATUS order)))
    (println))

  ;; Find specific records
  (println "\nSpecific Customer (ID=1):")
  (let [customer (pick/find-by-id db "CUSTOMER" 1)]
    (println (str "  Name: " (:CUSTOMER_NAME customer)))
    (println (str "  Email: " (:EMAIL customer))))

  ;; Raw data without dictionary interpretation
  (println "\nRaw Product Data (no dictionary):")
  (doseq [product (pick/read-all-records db "PRODUCT")]
    (println (str "  Raw: " product))))

(defn demonstrate-update-operations [db]
  "Demonstrate update operations"
  (println "\n=== UPDATE OPERATIONS ===")

  ;; Update customer information
  (println "Updating customer information...")
  (pick/update-record! db "CUSTOMER" 1
                       {:phone "555-0199"
                        :address "789 New Address, Updated City, USA"})

  ;; Update product price and stock
  (println "Updating product price and stock...")
  (pick/update-record! db "PRODUCT" 1
                       {:price 89.99
                        :stock_levels "45]25]15"
                        :tags "bestseller]featured>premium]clearance"})

  ;; Update order status
  (println "Updating order status...")
  (pick/update-record! db "ORDER" 2
                       {:status "shipped"
                        :order_date "2025-01-17"})

  (println "Updates completed. Verifying changes...")
  (let [updated-customer (pick/find-by-id db "CUSTOMER" 1)
        updated-product (pick/find-by-id db "PRODUCT" 1)
        updated-order (pick/find-by-id db "ORDER" 2)]
    (println (str "Customer phone: " (:PHONE updated-customer)))
    (println (str "Product price: $" (:PRICE updated-product)))
    (println (str "Product total stock: " (:TOTAL_STOCK updated-product)))
    (println (str "Order status: " (:STATUS updated-order)))))

(defn demonstrate-delete-operations [db]
  "Demonstrate delete operations"
  (println "\n=== DELETE OPERATIONS ===")

  ;; Create a temporary record for deletion demo
  (println "Creating temporary record for deletion demo...")
  (let [temp-id (pick/create-record! db "CUSTOMER"
                                     {:name "Temp Customer"
                                      :email "temp@delete.me"
                                      :phone "555-9999"
                                      :address "Temp Address"})]
    (println (str "Created temporary customer with ID: " temp-id))

    ;; Verify it exists
    (println "Verifying temporary customer exists...")
    (let [temp-customer (pick/find-by-id db "CUSTOMER" temp-id)]
      (println (str "Found: " (:CUSTOMER_NAME temp-customer))))

    ;; Delete the record
    (println "Deleting temporary customer...")
    (pick/delete-record! db "CUSTOMER" temp-id)

    ;; Verify it's gone
    (println "Verifying deletion...")
    (let [deleted-customer (pick/find-by-id db "CUSTOMER" temp-id)]
      (if deleted-customer
        (println "ERROR: Customer still exists!")
        (println "SUCCESS: Customer successfully deleted")))))

(defn demonstrate-advanced-patterns [db]
  "Demonstrate advanced CRUD patterns"
  (println "\n=== ADVANCED CRUD PATTERNS ===")

  ;; Batch create products
  (println "Batch creating products...")
  (let [new-products [{:name "Bluetooth Speaker" :price 79.99 :categories "electronics]audio]portable" :stock_levels "30]20]10" :tags "new]featured"}
                      {:name "Webcam" :price 59.99 :categories "electronics]video]peripheral" :stock_levels "40]25]15" :tags "essentials"}]]
    (doseq [product new-products]
      (pick/create-record! db "PRODUCT" product))
    (println (str "Created " (count new-products) " new products")))

  ;; Conditional updates based on computed fields
  (println "Finding products with low stock...")
  (let [low-stock-products (filter #(:IS_LOW_STOCK %)
                                   (pick/find-all db "PRODUCT"))]
    (println (str "Found " (count low-stock-products) " products with low stock"))
    (doseq [product low-stock-products]
      (println (str "  " (:PRODUCT_NAME product) " - Stock: " (:TOTAL_STOCK product)))))

  ;; Complex order with multiple products
  (println "Creating complex order...")
  (pick/create-record! db "ORDER"
                       {:customer_id 1
                        :product_ids "1]2]3]4]5"
                        :quantities "1]2]3]1]2"
                        :unit_prices "89.99]49.99]9.99]79.99]59.99"
                        :order_date "2025-01-18"
                        :status "processing"})

  ;; Show the complex order details
  (println "Complex order details:")
  (let [orders (pick/find-all db "ORDER")
        complex-order (last orders)]  ;; Get the last (newest) order
    (println (str "  Products: " (:PRODUCT_NAMES complex-order)))
    (println (str "  Quantities: " (:QUANTITIES complex-order)))
    (println (str "  Line Totals: " (:LINE_TOTALS complex-order)))
    (println (str "  Total: $" (:TOTAL complex-order)))))

(defn demonstrate-error-handling [db]
  "Demonstrate error handling in CRUD operations"
  (println "\n=== ERROR HANDLING ===")

  ;; Try to create invalid data
  (println "Testing error handling...")
  (try
    (pick/create-record! db "PRODUCT" {:name "" :price -10})
    (catch Exception e
      (println (str "Expected error creating invalid product: " (.getMessage e)))))

  ;; Try to update non-existent record
  (try
    (pick/update-record! db "CUSTOMER" 999 {:name "Non-existent"})
    (catch Exception e
      (println (str "Expected error updating non-existent record: " (.getMessage e)))))

  ;; Try to find non-existent record
  (let [missing-record (pick/find-by-id db "CUSTOMER" 999)]
    (if missing-record
      (println "Unexpected: Found record that shouldn't exist")
      (println "Correctly returned nil for non-existent record"))))

(defn run-complete-crud-demo []
  "Run the complete CRUD demonstration"
  (println "=== PICKDICT CRUD DEMONSTRATION ===")
  (println "Setting up database and tables...")

  (let [db (setup-database)]
    (create-sample-data db)
    (demonstrate-read-operations db)
    (demonstrate-update-operations db)
    (demonstrate-delete-operations db)
    (demonstrate-advanced-patterns db)
    (demonstrate-error-handling db)

    (println "\n=== DEMO COMPLETE ===")
    (println "Database file: example.db")
    (println "You can inspect the database with any SQLite browser")))

;; Run the complete demo
(comment
  (run-complete-crud-demo))

;; Individual function calls for testing
(comment
  (def db (setup-database))
  (create-sample-data db)
  (demonstrate-read-operations db)
  (demonstrate-update-operations db)
  (demonstrate-delete-operations db)
  (demonstrate-advanced-patterns db)
  (demonstrate-error-handling db))
