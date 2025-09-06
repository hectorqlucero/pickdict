# PickDict

[![Clojars Project](https://img.shields.io/badge/Clojars%20Project-0.2.0-blue.svg)](https://clojars.org/org.clojars.hector/pickdict)
[![Tests](https://img.shields.io/badge/tests-passing-brightgreen.svg)](https://github.com/hectorqlucero/pickdict/actions)
[![License](https://img.shields.io/badge/license-EPL%202.0-blue.svg)](https://www.eclipse.org/legal/epl-2.0/)
[![Clojure](https://img.shields.io/badge/clojure-1.12+-blue.svg)](https://clojure.org/)

**The Power of Pick/D3 Multivalue Databases in Modern Clojure**

PickDict brings authentic Pick/D3-style multivalue database functionality to Clojure applications. Store complex, related data elegantly using multivalue fields, leverage powerful computed expressions, and build sophisticated data applications with concise, maintainable code.

> **"PickDict transforms how we handle complex data relationships in Clojure"** - Developer Community

## ✨ Key Features

- 🔢 **Multivalue Fields** - Store arrays of related data in single fields
- 🧮 **Advanced Expressions** - Full Clojure expressions with automatic variable binding
- 📊 **Dictionary System** - One dictionary per table with multiple field interpretations
- ⚡ **High Performance** - Optimized SQL backend with connection pooling
- 🔄 **Legacy Compatibility** - Supports traditional MULTIPLY/SUM operations
- 🛡️ **Type Safety** - Comprehensive validation and error handling
- 🧪 **Production Ready** - Extensive test suite with 91 assertions

## 🚀 Quick Start

Get PickDict running in your project in under 5 minutes:

### 1. Add Dependency

```clojure
;; project.clj
[org.clojars.hector/pickdict "0.2.0"]
```

### 2. Basic Setup

```clojure
(require '[pickdict.core :as pick])

;; Configure database (SQLite for simplicity)
(def db {:dbtype "sqlite" :dbname "myapp.db"})

;; 2. Create a table with multivalue fields
(pick/create-file! db "PRODUCT"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :name "TEXT NOT NULL"
                    :price "REAL"
                    :categories "TEXT"      ;; Will store: "electronics]gaming]accessories"
                    :stock_levels "TEXT"})  ;; Will store: "50]25]10"

;; The dictionary table is automatically created as "PRODUCT_DICT"
;; 3. Dictionary fields are created automatically - you can add computed fields
(pick/define-dictionary-field db "PRODUCT_DICT" "TOTAL_STOCK" "C" "" "SUM:STOCK_LEVELS" "Total Stock")
```

### 3. Create and Query Data

```clojure
;; Create a product with multivalue data
(pick/create-record! db "PRODUCT"
                     {:name "Gaming Headset"
                      :price 199.99
                      :categories "electronics]gaming]audio"
                      :stock_levels "100]50]25"})

;; Query with automatic interpretation
(pick/find-all db "PRODUCT")
;; Returns:
;; [{:PRODUCT_NAME "Gaming Headset"
;;   :PRICE 199.99
;;   :CATEGORIES ["electronics" "gaming" "audio"]
;;   :TOTAL_STOCK 175}]
```

**That's it!** You now have a working multivalue database system.

## 📖 Table of Contents

- [Why PickDict?](#why-pickdict)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Complete CRUD Example](#complete-crud-example)
- [Core Concepts](#core-concepts)
- [API Reference](#api-reference)
- [Dictionary Fields](#dictionary-fields)
- [Expression Engine](#expression-engine)
- [Best Practices](#best-practices)
- [Examples](#examples)
- [Contributing](#contributing)
- [License](#license)

## 🤔 Why PickDict?

Traditional relational databases force you to create complex joins and multiple tables for related data. Pick/D3's multivalue approach lets you store related data naturally:

**Traditional SQL:**
```sql
-- Multiple tables for product categories
CREATE TABLE products (id, name, price);
CREATE TABLE categories (id, name);
CREATE TABLE product_categories (product_id, category_id);
```

**PickDict:**
```clojure
;; One field stores multiple categories
{:name "Gaming Headset"
 :categories "electronics]gaming]audio"}  ;; → ["electronics" "gaming" "audio"]
```

This approach is particularly powerful for:
- Product catalogs with multiple categories/tags
- Customer data with multiple addresses/phone numbers
- Financial records with multiple line items
- Any domain with natural one-to-many relationships

## 📦 Installation

### Clojars (Recommended)

```clojure
[org.clojars.hector/pickdict "0.2.0"]
```
```

### Manual Installation

```bash
git clone https://github.com/hectorqlucero/pickdict.git
cd pickdict
lein install
```

### System Requirements

- **Clojure**: 1.12+
- **Java**: 11+
- **Database**: SQLite (included) or PostgreSQL/MySQL

## 🏗️ Complete CRUD Example

Let's build a complete e-commerce system to demonstrate PickDict's capabilities:

### Setup Database Schema

```clojure
(require '[pickdict.core :as pick])

(def db {:dbtype "sqlite" :dbname "ecommerce.db"})

;; Create customer table
(pick/create-file! db "CUSTOMER"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :first_name "TEXT NOT NULL"
                    :last_name "TEXT NOT NULL"
                    :email "TEXT"
                    :phone_numbers "TEXT"    ;; Multivalue: home]work]mobile
                    :addresses "TEXT"})      ;; Multivalue: home]billing]shipping

;; Create product table
(pick/create-file! db "PRODUCT"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :name "TEXT NOT NULL"
                    :description "TEXT"
                    :price "REAL"
                    :categories "TEXT"       ;; Multivalue: electronics]gaming]audio
                    :tags "TEXT"            ;; Multivalue: bestseller]featured]new
                    :stock_levels "TEXT"})   ;; Multivalue: warehouse1]warehouse2]store

;; Create order table
(pick/create-file! db "ORDER"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :customer_id "INTEGER"
                    :product_ids "TEXT"     ;; Multivalue: product_id1]product_id2
                    :quantities "TEXT"      ;; Multivalue: qty1]qty2
                    :unit_prices "TEXT"     ;; Multivalue: price1]price2
                    :order_date "TEXT"
                    :status "TEXT"})
```

### Define Dictionary Fields

```clojure
;; Note: A-type (Attribute) fields are created automatically by create-file!
;; The following fields are created automatically for CUSTOMER table:
;; - FIRST_NAME (A-type, position 1)
;; - LAST_NAME (A-type, position 2) 
;; - EMAIL (A-type, position 3)
;; - PHONE_NUMBERS (A-type, position 4)
;; - ADDRESSES (A-type, position 5)

;; Add computed and translation fields on top of the automatic A-type fields
(pick/define-dictionary-field db "CUSTOMER_DICT" "FULL_NAME" "C" "" "(str FIRST_NAME \" \" LAST_NAME)" "Full Name")
(pick/define-dictionary-field db "CUSTOMER_DICT" "PRIMARY_PHONE" "A" "4" "" "Primary Phone")

;; Note: A-type (Attribute) fields are created automatically by create-file!
;; The following fields are created automatically for PRODUCT table:
;; - NAME (A-type, position 1)
;; - DESCRIPTION (A-type, position 2)
;; - PRICE (A-type, position 3)
;; - CATEGORIES (A-type, position 4)
;; - TAGS (A-type, position 5)
;; - STOCK_LEVELS (A-type, position 6)

;; Add computed fields on top of the automatic A-type fields
(pick/define-dictionary-field db "PRODUCT_DICT" "TOTAL_STOCK" "C" "" "SUM:STOCK_LEVELS" "Total Stock")
(pick/define-dictionary-field db "PRODUCT_DICT" "INVENTORY_VALUE" "C" "" "(* PRICE TOTAL_STOCK)" "Inventory Value")
(pick/define-dictionary-field db "PRODUCT_DICT" "IS_LOW_STOCK" "C" "" "(if (< TOTAL_STOCK 10) 1 0)" "Low Stock Flag")

;; Note: A-type (Attribute) fields are created automatically by create-file!
;; The following fields are created automatically for ORDER table:
;; - CUSTOMER_ID (A-type, position 1)
;; - PRODUCT_IDS (A-type, position 2)
;; - QUANTITIES (A-type, position 3)
;; - UNIT_PRICES (A-type, position 4)
;; - ORDER_DATE (A-type, position 5)
;; - STATUS (A-type, position 6)

;; Add translation and computed fields
(pick/define-dictionary-field db "ORDER_DICT" "CUSTOMER_NAME" "T" "1" "TCUSTOMER;FULL_NAME" "Customer Name")
(pick/define-dictionary-field db "ORDER_DICT" "PRODUCT_NAMES" "T" "2" "TPRODUCT;NAME" "Product Names")
(pick/define-dictionary-field db "ORDER_DICT" "LINE_TOTALS" "C" "" "MULTIPLY:QUANTITIES,UNIT_PRICES" "Line Totals")
(pick/define-dictionary-field db "ORDER_DICT" "SUBTOTAL" "C" "" "SUM:LINE_TOTALS" "Subtotal")
(pick/define-dictionary-field db "ORDER_DICT" "TAX" "C" "" "(* SUBTOTAL 0.08)" "Tax (8%)")
(pick/define-dictionary-field db "ORDER_DICT" "TOTAL" "C" "" "(+ SUBTOTAL TAX)" "Order Total")
```

### Create Sample Data

```clojure
;; Create customers
(pick/create-record! db "CUSTOMER"
                     {:first_name "John"
                      :last_name "Doe"
                      :email "john.doe@email.com"
                      :phone_numbers "555-0101]555-0102]555-0103"
                      :addresses "123 Main St]456 Oak Ave]789 Pine Rd"})

(pick/create-record! db "CUSTOMER"
                     {:first_name "Jane"
                      :last_name "Smith"
                      :email "jane.smith@email.com"
                      :phone_numbers "555-0201]555-0202"
                      :addresses "321 Elm St]654 Maple Dr"})

;; Create products
(pick/create-record! db "PRODUCT"
                     {:name "Wireless Gaming Headset"
                      :description "High-quality gaming audio"
                      :price 199.99
                      :categories "electronics]gaming]audio"
                      :tags "bestseller]featured]premium"
                      :stock_levels "50]25]10"})

(pick/create-record! db "PRODUCT"
                     {:name "Mechanical Keyboard"
                      :description "RGB backlit mechanical keyboard"
                      :price 149.99
                      :categories "electronics]gaming]computer"
                      :tags "featured]ergonomic"
                      :stock_levels "30]15]5"})

;; Create orders
(pick/create-record! db "ORDER"
                     {:customer_id 1
                      :product_ids "1]2"
                      :quantities "2]1"
                      :unit_prices "199.99]149.99"
                      :order_date "2025-01-15"
                      :status "completed"})

(pick/create-record! db "ORDER"
                     {:customer_id 2
                      :product_ids "1"
                      :quantities "1"
                      :unit_prices "199.99"
                      :order_date "2025-01-16"
                      :status "pending"})
```

### Query and Display Data

```clojure
;; Get all customers
(def customers (pick/find-all db "CUSTOMER"))
(doseq [customer customers]
  (println (str "Customer: " (:FULL_NAME customer)))
  (println (str "  Email: " (:EMAIL customer)))
  (println (str "  Phones: " (:PHONE_NUMBERS customer)))
  (println))

;; Get all products with computed fields
(def products (pick/find-all db "PRODUCT"))
(doseq [product products]
  (println (str "Product: " (:PRODUCT_NAME product)))
  (println (str "  Price: $" (:PRICE product)))
  (println (str "  Categories: " (:CATEGORIES product)))
  (println (str "  Total Stock: " (:TOTAL_STOCK product)))
  (println (str "  Inventory Value: $" (:INVENTORY_VALUE product)))
  (println (str "  Low Stock: " (if (= (:IS_LOW_STOCK product) 1) "YES" "NO")))
  (println))

;; Get all orders with full interpretation
(def orders (pick/find-all db "ORDER"))
(doseq [order orders]
  (println (str "Order #" (:ORDER_ID order)))
  (println (str "  Customer: " (:CUSTOMER_NAME order)))
  (println (str "  Products: " (:PRODUCT_NAMES order)))
  (println (str "  Quantities: " (:QUANTITIES order)))
  (println (str "  Line Totals: " (:LINE_TOTALS order)))
  (println (str "  Subtotal: $" (:SUBTOTAL order)))
  (println (str "  Tax: $" (:TAX order)))
  (println (str "  Total: $" (:TOTAL order)))
  (println (str "  Status: " (:STATUS order)))
  (println))
```

### Update Operations

```clojure
;; Update customer information
(pick/update-record! db "CUSTOMER" 1
                     {:phone_numbers "555-0101]555-0102]555-0104"
                      :email "john.doe@newemail.com"})

;; Update product stock
(pick/update-record! db "PRODUCT" 1
                     {:stock_levels "45]20]8"})

;; Update order status
(pick/update-record! db "ORDER" 2
                     {:status "shipped"})
```

### Advanced Queries

```clojure
;; Find products with low stock
(def low-stock-products
  (filter #(= (:IS_LOW_STOCK %) 1) (pick/find-all db "PRODUCT")))

;; Find orders by customer
(def customer-orders
  (filter #(= (:CUSTOMER_NAME %) "John Doe") (pick/find-all db "ORDER")))

;; Calculate total inventory value
(def total-inventory-value
  (reduce + (map :INVENTORY_VALUE (pick/find-all db "PRODUCT"))))

;; Find featured products
(def featured-products
  (filter #(some #{"featured"} (:TAGS %)) (pick/find-all db "PRODUCT")))
```

This example demonstrates:
- ✅ Multivalue field storage and retrieval
- ✅ Cross-table relationships with translation fields
- ✅ Complex computed expressions
- ✅ Full CRUD operations
- ✅ Advanced querying and filtering

## 🧠 Core Concepts

### Multivalue Fields

PickDict uses the `]` delimiter to store multiple values in a single field:

```clojure
;; Database stores: "electronics]gaming]audio"
;; PickDict returns: ["electronics" "gaming" "audio"]
```

Multivalue fields are automatically parsed into vectors when retrieved through dictionary operations.

### Dictionary System

Each table has one dictionary that defines multiple interpretations of the data:

```clojure
PRODUCT_DICT → PRODUCT table interpretations
CUSTOMER_DICT → CUSTOMER table interpretations
ORDER_DICT → ORDER table interpretations
```

### Field Types

- **Attribute (A)**: Direct column mapping
- **Translate (T)**: Cross-table lookups
- **Computed (C)**: Calculated values

## 📚 API Reference

### Database Operations

```clojure
(create-file! db table-name schema)           ;; Create table
(drop-table! db table-name)                  ;; Drop table
(create-record! db table-name data)          ;; Insert record
(find-all db table-name)                     ;; Get all records
(find-by-id db table-name id)                ;; Get record by ID
(update-record! db table-name id data)       ;; Update record
(delete-record! db table-name id)            ;; Delete record
```

### Dictionary Operations

```clojure
(create-file! db table-name schema)                           ;; Create table + dictionary automatically
(define-dictionary-field db dict-name field-name type pos expr desc)
```

## 🎯 Dictionary Fields

### Attribute Fields
```clojure
(define-dictionary-field db "PRODUCT_DICT" "NAME" "A" "1" "" "Product Name")
```

### Translation Fields
```clojure
(define-dictionary-field db "ORDER_DICT" "CUSTOMER_NAME" "T" "1" "TCUSTOMER;name" "Customer Name")
```

### Computed Fields
```clojure
(define-dictionary-field db "PRODUCT_DICT" "TOTAL_STOCK" "C" "" "SUM:STOCK_LEVELS" "Total Stock")
(define-dictionary-field db "INVOICE_DICT" "TAX" "C" "" "(* SUBTOTAL 0.08)" "Tax Amount")
```

## ⚡ Expression Engine

PickDict supports both legacy operations and full Clojure expressions:

```clojure
;; Legacy operations
"SUM:STOCK_LEVELS"           ;; Sum multivalue field
"MULTIPLY:QTY,PRICE"         ;; Element-wise multiplication

;; Clojure expressions
"(* QUANTITY PRICE)"          ;; Full arithmetic
"(if (> TOTAL 100) "High" "Low")"  ;; Conditionals
"(clojure.string/upper-case NAME)"     ;; String operations
```

## 💡 Best Practices

### Dictionary Design
- Use uppercase field names with underscores
- Define base fields before computed fields
- Group related fields together
- Add descriptive field descriptions

### Performance
- Index frequently queried fields
- Use batch operations for bulk inserts
- Keep expressions reasonably simple
- Consider pagination for large datasets

### Error Handling
```clojure
(try
  (pick/create-record! db "PRODUCT" product-data)
  (catch Exception e
    (log/error "Failed to create product:" (.getMessage e))))
```

## 🌟 Examples

### E-commerce Product Catalog
```clojure
;; Rich product data with categories, tags, and specifications
{:name "Gaming Laptop"
 :categories "electronics]gaming]computer"
 :tags "bestseller]featured]premium"
 :specifications "16GB RAM]512GB SSD]RTX 3060"
 :images "front.jpg]side.jpg]back.jpg"}
```

### Customer Management
```clojure
;; Multiple contact methods and addresses
{:name "John Doe"
 :phone_numbers "home:555-0101]work:555-0102]mobile:555-0103"
 :addresses "home:123 Main St]billing:456 Oak Ave]shipping:789 Pine Rd"}
```

### Financial Calculations
```clojure
;; Complex order calculations
{:quantities "2]1]5"
 :unit_prices "99.99]49.99]19.99"
 :line_totals "199.98]49.99]99.95"    ;; Computed
 :subtotal "349.92"                   ;; Computed
 :tax "27.99"                        ;; Computed
 :total "377.91"}                    ;; Computed
```

## 🤝 Contributing

We welcome contributions! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass: `lein test`
5. Submit a pull request

## 📄 License

Copyright © 2025 Hector

Distributed under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/).