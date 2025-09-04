# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [0.1.0] - 2025-09-03
### Added
- Initial release of PickDict library
- Complete Pick/D3-style multivalue database functionality
- SQLite backend with clojure.java.jdbc
- Dictionary-based field interpretation (Attribute, Translate, Computed)
- Full Clojure expression support in computed fields
- Legacy MULTIPLY/SUM operations support
- Comprehensive CRUD operations
- Professional documentation and examples
- GitHub Actions workflow for Clojars publishing
- Complete test suite with 14 assertions

### Features
- **Multivalue Fields**: Store multiple values in single database fields using `]` delimiter
- **Dictionary System**: One dictionary per table with multiple field interpretations
- **Expression Engine**: Full Clojure expressions with automatic variable binding
- **Translation Fields**: Cross-table lookups with `T{table};{field}` syntax
- **Computed Fields**: Dynamic calculations using Clojure code or legacy operations
- **Professional API**: Clean, well-documented functions for all operations

### Documentation
- Comprehensive README with API reference and examples
- Complete CRUD examples in `doc/example_usage.clj`
- Performance considerations and best practices
- Troubleshooting guide and debugging tips

[Unreleased]: https://github.com/hector/pickdict/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/hector/pickdict/releases/tag/v0.1.0
