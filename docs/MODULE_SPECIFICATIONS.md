# PostgreSQL MCP Server - Module Specifications

## 📋 Overview

This document provides detailed specifications for each module in the PostgreSQL MCP Server, separated from the implementation code to improve readability and maintainability.

## 🏗️ Core Layer

### core/interfaces.py

**Purpose**: Defines abstract base classes and protocols for all system components

**Responsibilities**:
- Define database connection management interface
- Define query execution interface
- Define security validation interface
- Define monitoring and health check interfaces
- Define configuration management interface

**Key Interfaces**:
- `IConnectionManager`: Database connection lifecycle management
- `IQueryExecutor`: SQL query execution and transaction management
- `ISecurityValidator`: Query validation and access control
- `IHealthChecker`: System health monitoring
- `IMetricsCollector`: Performance metrics collection
- `IConfigurationManager`: Configuration loading and validation

### core/exceptions.py

**Purpose**: Custom exception hierarchy for better error handling and debugging

**Responsibilities**:
- Define domain-specific exceptions
- Provide structured error information
- Enable proper error categorization and handling

**Exception Categories**:
- `ConnectionError`: Database connection failures
- `SecurityError`: Security validation failures
- `QueryError`: SQL execution errors
- `ConfigurationError`: Configuration validation errors
- `MonitoringError`: Health check and metrics errors

## 🏢 Domain Layer

### domain/models.py

**Purpose**: Core business entities and value objects without infrastructure dependencies

**Responsibilities**:
- Define Connection entity with validation rules
- Define Query value objects with parameter binding
- Define Schema entities for database structure representation
- Implement domain validation logic
- Provide serialization/deserialization capabilities

**Key Models**:
- `Connection`: Database connection configuration and state
- `Query`: SQL query with parameters and metadata
- `QueryResult`: Query execution results with timing information
- `TableSchema`: Database table structure information
- `ColumnInfo`: Database column metadata and constraints

### domain/services.py

**Purpose**: Domain business logic without infrastructure concerns

**Responsibilities**:
- Implement connection validation rules
- Define query parameter binding logic
- Provide schema analysis capabilities
- Handle domain-specific business rules
- Coordinate between domain entities

**Key Services**:
- `ConnectionValidator`: Business rules for connection parameters
- `QueryBuilder`: Safe query construction with parameter binding
- `SchemaAnalyzer`: Database schema analysis and validation

## 🏭 Infrastructure Layer

### infrastructure/database/connection_pool.py

**Purpose**: PostgreSQL-specific connection pool implementation

**Responsibilities**:
- Manage asyncpg connection pools
- Handle connection lifecycle (create, validate, cleanup)
- Implement connection health monitoring
- Provide connection acquisition and release
- Handle connection failures and recovery

**Implementation Details**:
- Uses asyncpg for PostgreSQL connectivity
- Implements connection warming for performance
- Provides configurable pool sizing
- Includes automatic connection validation
- Supports graceful shutdown procedures

### infrastructure/database/query_executor.py

**Purpose**: SQL execution engine with transaction support

**Responsibilities**:
- Execute parameterized queries safely
- Manage database transactions with ACID compliance
- Handle query timeouts and cancellation
- Provide batch execution capabilities
- Implement query performance monitoring

**Implementation Details**:
- Strict parameter binding for SQL injection prevention
- Transaction isolation level management
- Automatic retry for transient errors
- Query execution time tracking
- Support for prepared statements

### infrastructure/security/validator.py

**Purpose**: Security validation implementation for all database operations

**Responsibilities**:
- Validate SQL queries for security threats
- Implement operation filtering (read-only mode, operation whitelist)
- Scan for dangerous keywords and patterns
- Validate connection parameters
- Audit and log security events

**Security Features**:
- Multi-layer SQL injection detection
- Operation-based access control
- Query complexity analysis
- Parameter sanitization
- Security event logging

### infrastructure/monitoring/health_checker.py

**Purpose**: System health monitoring implementation

**Responsibilities**:
- Monitor MCP server health and responsiveness
- Check database connection pool status
- Validate system resource availability
- Detect performance anomalies
- Generate health status reports

**Monitoring Capabilities**:
- Real-time health status assessment
- Connection pool utilization tracking
- Query performance monitoring
- Resource usage analysis
- Automated failure detection

### infrastructure/monitoring/metrics_collector.py

**Purpose**: Performance metrics collection and aggregation

**Responsibilities**:
- Collect query execution metrics
- Track connection pool statistics
- Monitor system resource usage
- Aggregate performance data
- Provide metrics for monitoring systems

**Metrics Categories**:
- Query performance (execution time, throughput)
- Connection statistics (pool utilization, connection health)
- System resources (memory usage, CPU utilization)
- Error rates and failure patterns
- Historical trend analysis

### infrastructure/config/config_manager.py

**Purpose**: Configuration management and environment handling

**Responsibilities**:
- Load configuration from environment variables and files
- Validate configuration parameters
- Provide type-safe configuration access
- Set up structured logging
- Handle configuration updates

**Configuration Sources**:
- Environment variables (highest priority)
- Configuration files (.env, YAML)
- Default values (lowest priority)
- Runtime configuration updates

## 📱 Application Layer

### application/services/connection_service.py

**Purpose**: Connection management orchestration service

**Responsibilities**:
- Orchestrate connection creation and validation
- Manage multiple database connections
- Coordinate between domain and infrastructure layers
- Handle connection lifecycle events
- Provide connection status information

**Service Operations**:
- Create new database connections with validation
- Test existing connection health
- Remove and cleanup connections
- List active connections with metadata
- Handle connection failure recovery

### application/services/query_service.py

**Purpose**: Query execution orchestration service

**Responsibilities**:
- Orchestrate query execution workflow
- Coordinate security validation and execution
- Manage query history and audit trails
- Handle transaction coordination
- Provide query performance analysis

**Service Operations**:
- Execute single queries with parameter binding
- Manage multi-statement transactions
- Perform batch operations efficiently
- Stream large result sets
- Analyze query execution plans

### application/services/schema_service.py

**Purpose**: Database schema inspection orchestration service

**Responsibilities**:
- Orchestrate schema discovery and analysis
- Provide comprehensive database metadata
- Generate schema documentation
- Analyze database relationships
- Support schema comparison operations

**Service Operations**:
- Retrieve detailed table schema information
- List database objects with metadata
- Analyze query execution plans
- Generate schema documentation
- Compare schema versions

## 🎨 Presentation Layer

### presentation/server.py

**Purpose**: MCP server entry point and configuration

**Responsibilities**:
- Initialize MCP server with FastMCP
- Register MCP tools with proper handlers
- Configure server startup and shutdown
- Handle server-level error management
- Provide server status and health endpoints

**MCP Integration**:
- Tool registration for database operations
- Resource provisioning for dynamic data
- Notification handling for server events
- Protocol compliance and validation
- Client session management

### presentation/factory.py

**Purpose**: Dependency injection container and component wiring

**Responsibilities**:
- Create and wire all system components
- Manage component lifecycle
- Provide dependency injection
- Handle configuration-based component creation
- Support testing with mock implementations

**Factory Responsibilities**:
- Infrastructure component instantiation
- Application service composition
- Handler creation with proper dependencies
- Configuration injection
- Component cleanup and disposal

## 📚 Documentation Standards

### Code Documentation
- Remove all SPECIFICATION comments from code files
- Use concise docstrings for public APIs only
- Focus on implementation details rather than specifications
- Reference this specification document for complete information

### Specification Updates
- All architectural changes must update this document
- New components require specification entries
- Responsibility changes must be documented
- Keep specifications synchronized with implementation

### Testing Documentation
- Each component specification includes testing requirements
- Interface contracts define testing boundaries
- Mock implementations follow interface specifications
- Integration test scenarios reference service specifications

This modular specification approach ensures clear separation between documentation and implementation, improving code readability while maintaining comprehensive architectural guidance.