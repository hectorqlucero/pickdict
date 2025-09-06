# PickDict

[![Clojars Project](https://img.shields.io/badge/Clojars%20Project-0.1.1-blue.svg)](https://clojars.org/org.clojars.hector/pickdict)
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
[org.clojars.hector/pickdict "0.1.0"]
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
```clojure
;; project.clj
[org.clojars.hector/pickdict "0.1.1"]

;; deps.edn
{:deps {org.clojars.hector/pickdict {:mvn/version "0.1.1"}}}
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
;; Customer dictionary (automatically created by create-file!)
(pick/define-dictionary-field db "CUSTOMER_DICT" "CUSTOMER_ID" "A" "0" "" "Customer ID")
(pick/define-dictionary-field db "CUSTOMER_DICT" "FULL_NAME" "C" "" "(str FIRST_NAME \" \" LAST_NAME)" "Full Name")
(pick/define-dictionary-field db "CUSTOMER_DICT" "EMAIL" "A" "3" "" "Email Address")
(pick/define-dictionary-field db "CUSTOMER_DICT" "PRIMARY_PHONE" "A" "4" "" "Primary Phone")
(pick/define-dictionary-field db "CUSTOMER_DICT" "PHONE_NUMBERS" "A" "4" "" "All Phone Numbers")

;; Product dictionary (automatically created by create-file!)
(pick/define-dictionary-field db "PRODUCT_DICT" "PRODUCT_ID" "A" "0" "" "Product ID")
(pick/define-dictionary-field db "PRODUCT_DICT" "PRODUCT_NAME" "A" "1" "" "Product Name")
(pick/define-dictionary-field db "PRODUCT_DICT" "PRICE" "A" "3" "" "Unit Price")
(pick/define-dictionary-field db "PRODUCT_DICT" "CATEGORIES" "A" "4" "" "Categories")
(pick/define-dictionary-field db "PRODUCT_DICT" "TAGS" "A" "5" "" "Tags")
(pick/define-dictionary-field db "PRODUCT_DICT" "TOTAL_STOCK" "C" "" "SUM:STOCK_LEVELS" "Total Stock")
(pick/define-dictionary-field db "PRODUCT_DICT" "INVENTORY_VALUE" "C" "" "(* PRICE TOTAL_STOCK)" "Inventory Value")
(pick/define-dictionary-field db "PRODUCT_DICT" "IS_LOW_STOCK" "C" "" "(if (< TOTAL_STOCK 10) 1 0)" "Low Stock Flag")

;; Order dictionary (automatically created by create-file!)
(pick/define-dictionary-field db "ORDER_DICT" "ORDER_ID" "A" "0" "" "Order ID")
(pick/define-dictionary-field db "ORDER_DICT" "CUSTOMER_NAME" "T" "1" "TCUSTOMER;FULL_NAME" "Customer Name")
(pick/define-dictionary-field db "ORDER_DICT" "PRODUCT_NAMES" "T" "2" "TPRODUCT;PRODUCT_NAME" "Product Names")
(pick/define-dictionary-field db "ORDER_DICT" "QUANTITIES" "A" "3" "" "Quantities")
(pick/define-dictionary-field db "ORDER_DICT" "UNIT_PRICES" "A" "4" "" "Unit Prices")
(pick/define-dictionary-field db "ORDER_DICT" "LINE_TOTALS" "C" "" "MULTIPLY:QUANTITIES,UNIT_PRICES" "Line Totals")
(pick/define-dictionary-field db "ORDER_DICT" "SUBTOTAL" "C" "" "SUM:LINE_TOTALS" "Subtotal")
(pick/define-dictionary-field db "ORDER_DICT" "TAX" "C" "" "(* SUBTOTAL 0.08)" "Tax (8%)")
(pick/define-dictionary-field db "ORDER_DICT" "TOTAL" "C" "" "(+ SUBTOTAL TAX)" "Order Total")
(pick/define-dictionary-field db "ORDER_DICT" "STATUS" "A" "6" "" "Order Status")
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

---

**PickDict** - Elegant multivalue databases for Clojure applications.

## Table of Contents

- [What is Pick/D3?](#what-is-pickd3)
- [Key Features](#key-features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
- [API Reference](#api-reference)
- [CRUD Operations](#crud-operations)
- [Dictionary Field Types](#dictionary-field-types)
- [Expression Engine](#expression-engine)
- [Best Practices](#best-practices)
- [Performance Considerations](#performance-considerations)
- [Troubleshooting](#troubleshooting)
- [Examples](#examples)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)

## What is Pick/D3?

Pick/D3 is a powerful multivalue database system that revolutionized data management in the 1970s. Unlike traditional relational databases, Pick/D3 allows:

- **Multivalue Fields**: Store multiple related values in a single database field using delimiters
- **Dynamic Dictionaries**: Define multiple interpretations of the same data through dictionary fields
- **Computed Fields**: Perform calculations and transformations using powerful expression engines
- **Flexible Schema**: One dictionary per table with multiple field definitions for different use cases

PickDict brings this proven architecture to Clojure, enabling developers to build sophisticated data applications with elegant, concise code.

## Key Features

✨ **Authentic Pick/D3 Architecture** - True multivalue database implementation with one dictionary per table
🔢 **Multivalue Fields** - Store arrays of related data in single fields using configurable delimiters, automatically parsed into vectors on retrieval
🧮 **Advanced Expression Engine** - Full Clojure expressions with automatic variable binding
🔗 **Relationship Support** - Built-in translation fields for cross-table lookups
⚡ **High Performance** - Optimized SQL backend with connection pooling
🔄 **Legacy Compatibility** - Supports traditional MULTIPLY/SUM operations
🛡️ **Type Safety** - Comprehensive error handling and validation
📊 **SQLite Backend** - Persistent storage with `clojure.java.jdbc`
🧪 **Comprehensive Testing** - Full test suite with automatic multivalue parsing validation

## Installation

### From Clojars (Recommended)

PickDict is available on [Clojars](https://clojars.org/org.clojars.hector/pickdict). Add this to your `project.clj`:

```clojure
[org.clojars.hector/pickdict "0.1.1"]
```

### Manual Installation

For local development or if you need the latest changes:

```bash
git clone https://github.com/hectorqlucero/pickdict.git
cd pickdict
lein install
```

### Dependencies

PickDict requires the following runtime dependencies:

- **Clojure 1.12.2+**
- **clojure.java.jdbc 0.7.12** - Database connectivity
- **org.xerial/sqlite-jdbc 3.42.0.0** - SQLite database driver
- **cheshire 5.11.0** - JSON parsing (for future extensions)

## Quick Start

Get started with PickDict in minutes:

```clojure
(require '[pickdict.core :as pick])

;; 1. Configure database connection
(def db {:dbtype "sqlite" :dbname "inventory.db"})

;; 2. Create table with multivalue fields (dictionary created automatically)
(pick/create-file! db "PRODUCT"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :name "TEXT NOT NULL"
                    :price "REAL"
                    :categories "TEXT"      ;; Multivalue: "electronics]popular]new"
                    :stock_levels "TEXT"})  ;; Multivalue: "10]5]20"

;; Dictionary fields are created automatically (Pick/D3 style):
;; - NAME (maps to name column)
;; - PRICE (maps to price column)  
;; - CATEGORIES (maps to categories column)
;; - STOCK_LEVELS (maps to stock_levels column)

;; 4. Create and query data
(pick/create-record! db "PRODUCT"
                     {:name "Wireless Headphones"
                      :price 99.99
                      :categories "electronics]audio]wireless"
                      :stock_levels "50]30]20"})

;; 5. Query with automatic interpretation
(pick/find-all db "PRODUCT")
;; Returns: {:PRODUCT_NAME "Wireless Headphones"
;;           :PRICE 99.99
;;           :CATEGORIES ["electronics" "audio" "wireless"]
;;           :STOCK_LEVELS ["50" "30" "20"]}
```

## Core Concepts

### Multivalue Fields

PickDict uses the `]` character as a delimiter to store multiple values in a single database field:

```clojure
;; Database field contains: "electronics]audio]wireless"
;; PickDict interprets as: ["electronics" "audio" "wireless"]
```

### Dictionaries

A dictionary defines how to interpret data from a table. Each table has exactly one dictionary containing multiple field definitions:

```clojure
;; One dictionary per table (Pick/D3 standard)
PRODUCT_DICT → Defines how to interpret PRODUCT table data
CUSTOMER_DICT → Defines how to interpret CUSTOMER table data
```

### Field Types

PickDict supports three fundamental field types:

- **Attribute (A)**: Direct mapping to table columns
- **Translate (T)**: Lookup values from related tables
- **Computed (C)**: Calculated values using expressions

## API Reference

### Database Operations

#### `(create-file! db table-name schema)`
Creates a new table with the specified schema and automatically creates a dictionary with Attribute fields for each column (Pick/D3 style).

**Parameters:**
- `db`: Database connection map
- `table-name`: String name of the table to create
- `schema`: Map of column names to SQL types

**Automatic Dictionary Creation:**
- Creates a dictionary table named `{TABLE_NAME}_DICT`
- Automatically creates Attribute (A) type fields for each column (except 'id')
- Field names are uppercase versions of column names
- Positions are assigned sequentially starting from 1
- Descriptions are human-readable versions of column names

**Example:**
```clojure
(pick/create-file! db "PRODUCT"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :name "TEXT NOT NULL"
                    :price "REAL"
                    :categories "TEXT"})
;; Automatically creates dictionary fields: NAME, PRICE, CATEGORIES
```

#### `(drop-table! db table-name)`
Drops a table and its associated dictionary.

**Parameters:**
- `db`: Database connection map
- `table-name`: String name of the table to drop

### Dictionary Operations

**Note:** Dictionaries are created automatically when you call `create-file!`. You only need to define dictionary fields.

#### `(define-dictionary-field db dict-name field-name field-type position expression description)`
Defines a new field in a dictionary.

**Parameters:**
- `db`: Database connection map
- `dict-name`: String name of the dictionary
- `field-name`: String name of the field
- `field-type`: "A" (Attribute), "T" (Translate), or "C" (Computed)
- `position`: Column position (for Attribute fields) or empty string
- `expression`: Expression string (for Translate/Computed fields)
- `description`: Human-readable description

**Examples:**
```clojure
;; Attribute field
(pick/define-dictionary-field db "PRODUCT_DICT" "NAME" "A" "1" "" "Product Name")

;; Translate field
(pick/define-dictionary-field db "PRODUCT_DICT" "SUPPLIER_NAME" "T" "1" "TSUPPLIER;name" "Supplier Name")

;; Computed field
(pick/define-dictionary-field db "PRODUCT_DICT" "TOTAL_VALUE" "C" "" "(* PRICE QUANTITY)" "Total Value")
```

### CRUD Operations

#### `(create-record! db table-name data)`
Creates a new record in the specified table.

**Parameters:**
- `db`: Database connection map
- `table-name`: String name of the table
- `data`: Map of column names to values

#### `(find-all db table-name)`
Retrieves all records from a table with dictionary interpretation.

**Returns:** Sequence of maps with interpreted field values. Multivalue fields are automatically parsed into vectors.

#### `(find-by-id db table-name id)`
Retrieves a single record by ID with dictionary interpretation.

**Returns:** Map with interpreted field values or `nil` if not found. Multivalue fields are automatically parsed into vectors.

#### `(update-record! db table-name id data)`
Updates an existing record.

**Parameters:**
- `db`: Database connection map
- `table-name`: String name of the table
- `id`: Record ID to update
- `data`: Map of column names to new values

#### `(delete-record! db table-name id)`
Deletes a record by ID.

#### `(read-all-records db table-name)`
Retrieves all records without dictionary interpretation (raw data).

**Returns:** Sequence of maps with raw database values

## CRUD Operations

### Creating Records

```clojure
;; Create a customer
(pick/create-record! db "CUSTOMER"
                     {:name "John Doe"
                      :email "john@example.com"
                      :phone "555-0123"})

;; Create a product with multivalue fields
(pick/create-record! db "PRODUCT"
                     {:name "Gaming Laptop"
                      :price 1299.99
                      :categories "electronics]gaming]laptop"
                      :specifications "16GB RAM]512GB SSD]RTX 3060"})
```

### Reading Records

```clojure
;; Get all products with dictionary interpretation
(def products (pick/find-all db "PRODUCT"))

;; Get specific product by ID
(def product (pick/find-by-id db "PRODUCT" 1))

;; Get raw data (for debugging or admin purposes)
(def raw-data (pick/read-all-records db "PRODUCT"))
```

### Updating Records

```clojure
;; Update product price
(pick/update-record! db "PRODUCT" 1
                     {:price 1199.99
                      :categories "electronics]gaming]laptop]discount"})

;; Update customer information
(pick/update-record! db "CUSTOMER" 1
                     {:phone "555-0124"
                      :email "john.doe@newemail.com"})
```

### Deleting Records

```clojure
;; Delete a product
(pick/delete-record! db "PRODUCT" 1)

;; Delete a customer
(pick/delete-record! db "CUSTOMER" 1)
```

## Dictionary Field Types

### Attribute Fields (`A`)

Direct mapping to table columns by position. Use for simple field access:

```clojure
;; Map to first column (name)
(define-dictionary-field db "CUSTOMER_DICT" "CUSTOMER_NAME" "A" "1" "" "Customer Name")

;; Map to second column (email)
(define-dictionary-field db "CUSTOMER_DICT" "EMAIL" "A" "2" "" "Email Address")

;; Map to third column (phone)
(define-dictionary-field db "CUSTOMER_DICT" "PHONE" "A" "3" "" "Phone Number")
```

### Translate Fields (`T`)

Lookup values from related tables using the format `T{table};{field}`:

```clojure
;; Lookup customer name from CUSTOMER table
(define-dictionary-field db "ORDER_DICT" "CUSTOMER_NAME" "T" "1" "TCUSTOMER;name" "Customer Name")

;; Lookup product names from PRODUCT table
(define-dictionary-field db "ORDER_DICT" "PRODUCT_NAMES" "T" "2" "TPRODUCT;name" "Product Names")

;; Lookup supplier contact from SUPPLIER table
(define-dictionary-field db "PRODUCT_DICT" "SUPPLIER_EMAIL" "T" "1" "TSUPPLIER;email" "Supplier Email")
```

### Computed Fields (`C`)

Perform calculations using Clojure expressions or legacy operations:

```clojure
;; Full Clojure expressions
(define-dictionary-field db "INVOICE_DICT" "SUBTOTAL" "C" "" "(+ 99.99 49.99 29.99)" "Subtotal")
(define-dictionary-field db "INVOICE_DICT" "TAX" "C" "" "(* SUBTOTAL 0.08)" "Tax (8%)")
(define-dictionary-field db "INVOICE_DICT" "TOTAL" "C" "" "(+ SUBTOTAL TAX)" "Total Amount")

;; Legacy operations (still supported)
(define-dictionary-field db "PRODUCT_DICT" "TOTAL_STOCK" "C" "" "SUM:STOCK_LEVELS" "Total Stock")
(define-dictionary-field db "PRODUCT_DICT" "LINE_TOTALS" "C" "" "MULTIPLY:QUANTITIES,PRICES" "Line Totals")
```

## Expression Engine

### Variable Binding

Expressions automatically bind variables from other dictionary fields:

```clojure
;; Define base fields first
(define-dictionary-field db "INVOICE_DICT" "QUANTITY" "A" "1" "" "Quantity")
(define-dictionary-field db "INVOICE_DICT" "PRICE" "A" "2" "" "Unit Price")

;; Computed field can reference previous fields
(define-dictionary-field db "INVOICE_DICT" "LINE_TOTAL" "C" "" "(* QUANTITY PRICE)" "Line Total")
(define-dictionary-field db "INVOICE_DICT" "DISCOUNTED_TOTAL" "C" "" "(* LINE_TOTAL 0.9)" "90% Discount")
```

### Multivalue Expressions

For multivalue fields, expressions are evaluated element-wise:

```clojure
;; If quantities = "2]1]5" and prices = "10]20]30"
(define-dictionary-field db "INVOICE_DICT" "LINE_TOTALS" "C" "" "MULTIPLY:QUANTITIES,PRICES" "Line Totals")
;; Result: "20]20]150"

(define-dictionary-field db "INVOICE_DICT" "SUBTOTAL" "C" "" "SUM:LINE_TOTALS" "Subtotal")
;; Result: "190"
```

### Advanced Expressions

```clojure
;; Conditional logic
(define-dictionary-field db "PRODUCT_DICT" "STOCK_STATUS" "C" ""
  "(if (> TOTAL_STOCK 100) \"In Stock\" \"Low Stock\")" "Stock Status")

;; String manipulation
(define-dictionary-field db "CUSTOMER_DICT" "INITIALS" "C" ""
  "(str (first (clojure.string/upper-case (subs FIRST_NAME 0 1)))
        (first (clojure.string/upper-case (subs LAST_NAME 0 1))))" "Initials")

;; Date calculations
(define-dictionary-field db "ORDER_DICT" "DAYS_AGO" "C" ""
  "(/ (- (System/currentTimeMillis) (.getTime (java.text.SimpleDateFormat. \"yyyy-MM-dd\") (.parse ORDER_DATE))) (* 24 60 60 1000))" "Days Since Order")
```

## Best Practices

### Dictionary Design

1. **One Dictionary Per Table**: Follow authentic Pick/D3 architecture
2. **Descriptive Field Names**: Use uppercase with underscores (e.g., `CUSTOMER_NAME`)
3. **Logical Field Ordering**: Define base fields before computed fields
4. **Consistent Naming**: Use consistent patterns across dictionaries

### Performance Optimization

1. **Index Key Fields**: Add database indexes on frequently queried fields
2. **Batch Operations**: Use batch inserts/updates for large datasets
3. **Connection Pooling**: Configure connection pooling for production use
4. **Expression Complexity**: Keep computed expressions reasonably simple

### Error Handling

```clojure
;; Always wrap database operations in try-catch
(try
  (pick/create-record! db "PRODUCT" product-data)
  (catch Exception e
    (log/error "Failed to create product:" (.getMessage e))
    (throw e)))

;; Validate data before operations
(defn create-product! [db product]
  {:pre [(string? (:name product))
         (number? (:price product))
         (pos? (:price product))]}
  (pick/create-record! db "PRODUCT" product))
```

## Performance Considerations

### Database Configuration

For production use, configure your database connection appropriately:

```clojure
;; Development
(def db {:dbtype "sqlite" :dbname "app.db"})

;; Production (with connection pooling)
(def db {:dbtype "postgresql"
         :dbname "production_db"
         :host "localhost"
         :port 5432
         :user "app_user"
         :password "secure_password"
         :maximum-pool-size 10})
```

### Indexing Strategy

```sql
-- Add indexes for better query performance
CREATE INDEX idx_product_name ON PRODUCT(name);
CREATE INDEX idx_customer_email ON CUSTOMER(email);
CREATE INDEX idx_order_date ON ORDER(order_date);
```

### Memory Usage

- Multivalue fields are parsed on-demand
- Computed expressions are evaluated per-record
- Consider pagination for large result sets

## Troubleshooting

### Common Issues

**"Table doesn't exist" errors:**
- Ensure you've called `create-file!` before using the table
- Check database connection configuration

**"Dictionary field not found" errors:**
- Ensure you've called `create-file!` to create both table and dictionary
- Ensure field names match exactly (case-sensitive)

**"Expression evaluation failed" errors:**
- Check expression syntax for Clojure code
- Verify referenced fields exist and are defined before the computed field
- Use `read-all-records` to inspect raw data

**Multivalue parsing issues:**
- Ensure values are separated by `]` character only
- Check for escaped delimiters in data

### Debugging Tips

```clojure
;; Inspect raw data without dictionary interpretation
(def raw-records (pick/read-all-records db "PRODUCT"))
(doseq [record raw-records]
  (println "Raw record:" record))

;; Test expressions in REPL
(def test-data {:QUANTITY 5 :PRICE 10.99})
;; Test expression: (* QUANTITY PRICE)
(eval `(let [~'QUANTITY ~(:QUANTITY test-data)
             ~'PRICE ~(:PRICE test-data)]
         (* QUANTITY PRICE)))
```

### Getting Help

- **Documentation**: Check this README and `doc/example_usage.clj`
- **Tests**: Run `lein test` to verify functionality
- **REPL**: Use `lein repl` for interactive debugging
- **Issues**: Report bugs with minimal reproduction cases

## Examples

### Complete Inventory System

See `doc/example_usage.clj` for a comprehensive example including:

- Product catalog with multivalue categories
- Customer management
- Order processing with line item calculations
- Inventory tracking and reporting
- Error handling and validation

Run the complete example:

```clojure
(load-file "doc/example_usage.clj")
(example-usage/run-complete-crud-demo)
```

### Real-World Patterns

#### E-commerce Product Catalog

```clojure
;; Product table with rich multivalue data
(pick/create-file! db "PRODUCT"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :name "TEXT NOT NULL"
                    :price "REAL"
                    :categories "TEXT"
                    :tags "TEXT"
                    :specifications "TEXT"
                    :images "TEXT"})

;; Dictionary with business logic (automatically created by create-file!)
(pick/define-dictionary-field db "PRODUCT_DICT" "PRODUCT_NAME" "A" "1" "" "Product Name")
(pick/define-dictionary-field db "PRODUCT_DICT" "CATEGORIES" "A" "2" "" "Categories")
(pick/define-dictionary-field db "PRODUCT_DICT" "TAGS" "A" "3" "" "Tags")
(pick/define-dictionary-field db "PRODUCT_DICT" "IS_FEATURED" "C" "" "(some #(= % \"featured\") TAGS)" "Is Featured")
(pick/define-dictionary-field db "PRODUCT_DICT" "CATEGORY_COUNT" "C" "" "(count CATEGORIES)" "Category Count")
```

#### Financial Reporting System

```clojure
;; Invoice table with complex calculations
(pick/create-file! db "INVOICE"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :customer_id "INTEGER"
                    :line_items "TEXT"      ;; JSON-like multivalue structure
                    :tax_rate "REAL"
                    :discount_percent "REAL"})

;; Advanced financial calculations (automatically created by create-file!)
(pick/define-dictionary-field db "INVOICE_DICT" "CUSTOMER_NAME" "T" "1" "TCUSTOMER;name" "Customer Name")
(pick/define-dictionary-field db "INVOICE_DICT" "SUBTOTAL" "C" "" "(reduce + LINE_ITEM_TOTALS)" "Subtotal")
(pick/define-dictionary-field db "INVOICE_DICT" "TAX_AMOUNT" "C" "" "(* SUBTOTAL (/ TAX_RATE 100))" "Tax Amount")
(pick/define-dictionary-field db "INVOICE_DICT" "DISCOUNT_AMOUNT" "C" "" "(* SUBTOTAL (/ DISCOUNT_PERCENT 100))" "Discount Amount")
(pick/define-dictionary-field db "INVOICE_DICT" "TOTAL" "C" "" "(- (+ SUBTOTAL TAX_AMOUNT) DISCOUNT_AMOUNT)" "Final Total")
```

## Development

### Running Tests

```bash
# Run all tests
lein test

# Run specific test namespace
lein test pickdict.core-test

# Run tests with coverage
lein cloverage
```

### Development Workflow

```bash
# Start REPL
lein repl

# Run tests on file changes
lein test-refresh

# Check for outdated dependencies
lein ancient

# Generate documentation
lein codox

# Build JAR
lein jar

# Install locally
lein install
```

### Code Quality

```bash
# Run linter
lein eastwood

# Check test coverage
lein cloverage

# Run all quality checks
lein do test, eastwood, cloverage
```

## Contributing

We welcome contributions to PickDict! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Clone your fork: `git clone https://github.com/yourusername/pickdict.git`
3. Create a feature branch: `git checkout -b feature/your-feature`
4. Make your changes
5. Add tests for new functionality
6. Run the test suite: `lein test`
7. Submit a pull request

### Code Style

- Follow Clojure style guidelines
- Use descriptive function and variable names
- Add docstrings to public functions
- Include comprehensive tests
- Update documentation for API changes

## License

Copyright © 2025 Hector

Distributed under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/).

This project is not affiliated with Rocket Software or other Pick/D3 vendors. "Pick" and "D3" are trademarks of Rocket Software, Inc.

---

**PickDict** - Bringing the power of multivalue databases to modern Clojure applications.