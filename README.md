# PickDict

[![Clojars Project](https://img.shields.io/badge/Clojars%20Project-0.1.0-blue.svg)](https://clojars.org/hector/pickdict)
[![License](https://img.shields.io/badge/license-EPL%202.0-blue.svg)](https://www.eclipse.org/legal/epl-2.0/)

A professional Clojure library that implements authentic Pick/D3-style multivalue database functionality on top of SQL databases. PickDict brings the power of multivalue databases to modern Clojure applications while maintaining full compatibility with traditional SQL databases.

## Installation

### From Clojars (Recommended)

PickDict is available on [Clojars](https://clojars.org/hector/pickdict). Add this to your `project.clj`:

```clojure
[hector/pickdict "0.1.0"]
```

### Manual Installation

For local development or if you need the latest changes:

```bash
git clone https://github.com/hectorqlucero/pickdict.git
cd pickdict
lein install
```

### Manual Deployment (Alternative)

If GitHub Actions fails, deploy manually:

```bash
# 1. Get your Clojars deploy token from: https://clojars.org/tokens

# 2. Set credentials
export CLOJARS_USERNAME=hector
export CLOJARS_PASSWORD=your_deploy_token

# 3. Run deployment
./deploy.sh
```

### Publishing New Versions

This project uses GitHub Actions to automatically publish to Clojars when a new version is tagged.

#### Quick Release (Recommended)

Use the provided release script:

```bash
# Update version and create release
./release.sh 0.1.1

# Or for patch releases
./release.sh 0.1.0.1
```

The script will:
- Update `project.clj` version
- Update `CHANGELOG.md`
- Commit changes
- Create and push Git tag
- Trigger GitHub Actions for publishing

#### Manual Release Process

1. **Update version** in `project.clj`:
   ```clojure
   (defproject hector/pickdict "0.1.1"  ;; Increment version
   ```

2. **Update CHANGELOG.md** with the new changes

3. **Commit and push** your changes:
   ```bash
   git add .
   git commit -m "Bump version to 0.1.1"
   git push origin main
   ```

4. **Create a Git tag**:
   ```bash
   git tag v0.1.1
   git push origin v0.1.1
   ```

5. **GitHub Actions** will automatically:
   - Run tests
   - Publish to Clojars
   - Create a GitHub release

### Required Secrets

Set these in your GitHub repository settings under "Secrets and variables" → "Actions":

- `CLOJARS_USERNAME`: Your Clojars username
- `CLOJARS_PASSWORD`: Your Clojars deploy token (not password)

[![License](https://img.shields.io/badge/license-EPL%202.0-blue.svg)](https://www.eclipse.org/legal/epl-2.0/)

A professional Clojure library that implements authentic Pick/D3-style multivalue database functionality on top of SQL databases. PickDict brings the power of multivalue databases to modern Clojure applications while maintaining full compatibility with traditional SQL databases.

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
🔢 **Multivalue Fields** - Store arrays of related data in single fields using configurable delimiters
🧮 **Advanced Expression Engine** - Full Clojure expressions with automatic variable binding
🔗 **Relationship Support** - Built-in translation fields for cross-table lookups
⚡ **High Performance** - Optimized SQL backend with connection pooling
🔄 **Legacy Compatibility** - Supports traditional MULTIPLY/SUM operations
🛡️ **Type Safety** - Comprehensive error handling and validation
📊 **SQLite Backend** - Persistent storage with `clojure.java.jdbc`
🧪 **Comprehensive Testing** - Full test suite with 14 assertions

## Installation

### From Clojars (Recommended)

PickDict is available on [Clojars](https://clojars.org/hector/pickdict). Add this to your `project.clj`:

```clojure
[hector/pickdict "0.1.0"]
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

;; 2. Create table with multivalue fields
(pick/create-file! db "PRODUCT"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :name "TEXT NOT NULL"
                    :price "REAL"
                    :categories "TEXT"      ;; Multivalue: "electronics]popular]new"
                    :stock_levels "TEXT"})  ;; Multivalue: "10]5]20"

;; 3. Create dictionary for data interpretation
(pick/create-dictionary! db "PRODUCT_DICT")

;; 4. Define dictionary fields
(pick/define-dictionary-field! db "PRODUCT_DICT" "PRODUCT_NAME" "A" "1" "" "Product Name")
(pick/define-dictionary-field! db "PRODUCT_DICT" "PRICE" "A" "2" "" "Unit Price")
(pick/define-dictionary-field! db "PRODUCT_DICT" "CATEGORIES" "A" "3" "" "Categories")
(pick/define-dictionary-field! db "PRODUCT_DICT" "TOTAL_STOCK" "C" "" "SUM:STOCK_LEVELS" "Total Stock")

;; 5. Create and query data
(pick/create-record! db "PRODUCT"
                     {:name "Wireless Headphones"
                      :price 99.99
                      :categories "electronics]audio]wireless"
                      :stock_levels "50]30]20"})

;; 6. Query with automatic interpretation
(pick/find-all db "PRODUCT")
;; Returns: {:PRODUCT_NAME "Wireless Headphones"
;;           :PRICE 99.99
;;           :CATEGORIES ["electronics" "audio" "wireless"]
;;           :TOTAL_STOCK 100}
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
Creates a new table with the specified schema.

**Parameters:**
- `db`: Database connection map
- `table-name`: String name of the table to create
- `schema`: Map of column names to SQL types

**Example:**
```clojure
(pick/create-file! db "CUSTOMER"
                   {:id "INTEGER PRIMARY KEY AUTOINCREMENT"
                    :name "TEXT NOT NULL"
                    :email "TEXT"})
```

#### `(drop-table! db table-name)`
Drops a table and its associated dictionary.

**Parameters:**
- `db`: Database connection map
- `table-name`: String name of the table to drop

### Dictionary Operations

#### `(create-dictionary! db dict-name)`
Creates a new dictionary for a table.

**Parameters:**
- `db`: Database connection map
- `dict-name`: String name of the dictionary (typically `TABLE_DICT`)

#### `(define-dictionary-field! db dict-name field-name field-type position expression description)`
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
(pick/define-dictionary-field! db "PRODUCT_DICT" "NAME" "A" "1" "" "Product Name")

;; Translate field
(pick/define-dictionary-field! db "PRODUCT_DICT" "SUPPLIER_NAME" "T" "1" "TSUPPLIER;name" "Supplier Name")

;; Computed field
(pick/define-dictionary-field! db "PRODUCT_DICT" "TOTAL_VALUE" "C" "" "(* PRICE QUANTITY)" "Total Value")
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

**Returns:** Sequence of maps with interpreted field values

#### `(find-by-id db table-name id)`
Retrieves a single record by ID with dictionary interpretation.

**Returns:** Map with interpreted field values or `nil` if not found

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
(define-dictionary-field! db "CUSTOMER_DICT" "CUSTOMER_NAME" "A" "1" "" "Customer Name")

;; Map to second column (email)
(define-dictionary-field! db "CUSTOMER_DICT" "EMAIL" "A" "2" "" "Email Address")

;; Map to third column (phone)
(define-dictionary-field! db "CUSTOMER_DICT" "PHONE" "A" "3" "" "Phone Number")
```

### Translate Fields (`T`)

Lookup values from related tables using the format `T{table};{field}`:

```clojure
;; Lookup customer name from CUSTOMER table
(define-dictionary-field! db "ORDER_DICT" "CUSTOMER_NAME" "T" "1" "TCUSTOMER;name" "Customer Name")

;; Lookup product names from PRODUCT table
(define-dictionary-field! db "ORDER_DICT" "PRODUCT_NAMES" "T" "2" "TPRODUCT;name" "Product Names")

;; Lookup supplier contact from SUPPLIER table
(define-dictionary-field! db "PRODUCT_DICT" "SUPPLIER_EMAIL" "T" "1" "TSUPPLIER;email" "Supplier Email")
```

### Computed Fields (`C`)

Perform calculations using Clojure expressions or legacy operations:

```clojure
;; Full Clojure expressions
(define-dictionary-field! db "INVOICE_DICT" "SUBTOTAL" "C" "" "(+ 99.99 49.99 29.99)" "Subtotal")
(define-dictionary-field! db "INVOICE_DICT" "TAX" "C" "" "(* SUBTOTAL 0.08)" "Tax (8%)")
(define-dictionary-field! db "INVOICE_DICT" "TOTAL" "C" "" "(+ SUBTOTAL TAX)" "Total Amount")

;; Legacy operations (still supported)
(define-dictionary-field! db "PRODUCT_DICT" "TOTAL_STOCK" "C" "" "SUM:STOCK_LEVELS" "Total Stock")
(define-dictionary-field! db "PRODUCT_DICT" "LINE_TOTALS" "C" "" "MULTIPLY:QUANTITIES,PRICES" "Line Totals")
```

## Expression Engine

### Variable Binding

Expressions automatically bind variables from other dictionary fields:

```clojure
;; Define base fields first
(define-dictionary-field! db "INVOICE_DICT" "QUANTITY" "A" "1" "" "Quantity")
(define-dictionary-field! db "INVOICE_DICT" "PRICE" "A" "2" "" "Unit Price")

;; Computed field can reference previous fields
(define-dictionary-field! db "INVOICE_DICT" "LINE_TOTAL" "C" "" "(* QUANTITY PRICE)" "Line Total")
(define-dictionary-field! db "INVOICE_DICT" "DISCOUNTED_TOTAL" "C" "" "(* LINE_TOTAL 0.9)" "90% Discount")
```

### Multivalue Expressions

For multivalue fields, expressions are evaluated element-wise:

```clojure
;; If quantities = "2]1]5" and prices = "10]20]30"
(define-dictionary-field! db "INVOICE_DICT" "LINE_TOTALS" "C" "" "MULTIPLY:QUANTITIES,PRICES" "Line Totals")
;; Result: "20]20]150"

(define-dictionary-field! db "INVOICE_DICT" "SUBTOTAL" "C" "" "SUM:LINE_TOTALS" "Subtotal")
;; Result: "190"
```

### Advanced Expressions

```clojure
;; Conditional logic
(define-dictionary-field! db "PRODUCT_DICT" "STOCK_STATUS" "C" ""
  "(if (> TOTAL_STOCK 100) \"In Stock\" \"Low Stock\")" "Stock Status")

;; String manipulation
(define-dictionary-field! db "CUSTOMER_DICT" "INITIALS" "C" ""
  "(str (first (clojure.string/upper-case (subs FIRST_NAME 0 1)))
        (first (clojure.string/upper-case (subs LAST_NAME 0 1))))" "Initials")

;; Date calculations
(define-dictionary-field! db "ORDER_DICT" "DAYS_AGO" "C" ""
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
- Verify dictionary exists with `create-dictionary!`
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

;; Dictionary with business logic
(pick/create-dictionary! db "PRODUCT_DICT")
(pick/define-dictionary-field! db "PRODUCT_DICT" "PRODUCT_NAME" "A" "1" "" "Product Name")
(pick/define-dictionary-field! db "PRODUCT_DICT" "CATEGORIES" "A" "2" "" "Categories")
(pick/define-dictionary-field! db "PRODUCT_DICT" "TAGS" "A" "3" "" "Tags")
(pick/define-dictionary-field! db "PRODUCT_DICT" "IS_FEATURED" "C" "" "(some #(= % \"featured\") TAGS)" "Is Featured")
(pick/define-dictionary-field! db "PRODUCT_DICT" "CATEGORY_COUNT" "C" "" "(count CATEGORIES)" "Category Count")
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

;; Advanced financial calculations
(pick/create-dictionary! db "INVOICE_DICT")
(pick/define-dictionary-field! db "INVOICE_DICT" "CUSTOMER_NAME" "T" "1" "TCUSTOMER;name" "Customer Name")
(pick/define-dictionary-field! db "INVOICE_DICT" "SUBTOTAL" "C" "" "(reduce + LINE_ITEM_TOTALS)" "Subtotal")
(pick/define-dictionary-field! db "INVOICE_DICT" "TAX_AMOUNT" "C" "" "(* SUBTOTAL (/ TAX_RATE 100))" "Tax Amount")
(pick/define-dictionary-field! db "INVOICE_DICT" "DISCOUNT_AMOUNT" "C" "" "(* SUBTOTAL (/ DISCOUNT_PERCENT 100))" "Discount Amount")
(pick/define-dictionary-field! db "INVOICE_DICT" "TOTAL" "C" "" "(- (+ SUBTOTAL TAX_AMOUNT) DISCOUNT_AMOUNT)" "Final Total")
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