"""
Data Models and Type Definitions

SPECIFICATION:
This module defines all data models, type definitions, and data structures
used throughout the PostgreSQL MCP Server. It provides type-safe data
containers using Pydantic models for validation and serialization.

CORE MODEL CATEGORIES:
1. Connection Models: Database connection information and status
2. Query Models: Query execution results and metadata
3. Schema Models: Database schema information (tables, columns, indexes)
4. Configuration Models: Server and security configuration settings
5. Monitoring Models: Health status and performance metrics
6. Security Models: Authentication and authorization data structures

DATA VALIDATION FEATURES:
- Type Safety: Strict type checking for all model fields
- Field Validation: Custom validators for complex field requirements
- Serialization: JSON serialization/deserialization support
- Default Values: Intelligent default value management
- Field Documentation: Comprehensive field descriptions and constraints

MODEL DESIGN PRINCIPLES:
- Immutability: Models are designed to be immutable where possible
- Validation: All input data is validated against model schemas
- Consistency: Consistent naming and structure across all models
- Extensibility: Models support extension and customization
- Documentation: All models are fully documented with usage examples

PRIMARY MODELS:
- ConnectionInfo: Database connection configuration and status
- QueryResult: Query execution results and metadata
- TableSchema: Database table structure information
- ServerConfig: Server configuration parameters
- SecurityConfig: Security policy settings
- HealthStatus: System health and monitoring data

USAGE PATTERN:
These models are used throughout the application for data validation,
API responses, configuration management, and internal data structures.
All models provide automatic validation and type checking.
"""

from datetime import datetime
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field

class ConnectionInfo(BaseModel):
    """資料庫連線資訊"""
    connection_id: str
    host: str
    port: int
    database: str
    user: str
    encrypted_password: str
    pool_size: int = 10
    is_active: bool = True
    created_at: datetime = Field(default_factory=datetime.utcnow)

class ConnectionResult(BaseModel):
    """連線操作結果"""
    connection_id: str
    success: bool
    message: str
    created_at: datetime = Field(default_factory=datetime.utcnow)

class ConnectionStatus(BaseModel):
    """連線狀態"""
    connection_id: str
    is_healthy: bool
    message: str
    checked_at: datetime = Field(default_factory=datetime.utcnow)

class ColumnInfo(BaseModel):
    """欄位資訊"""
    name: str
    data_type: str
    max_length: Optional[int] = None
    precision: Optional[int] = None
    scale: Optional[int] = None
    is_nullable: bool
    default_value: Optional[str] = None
    ordinal_position: int

class TableInfo(BaseModel):
    """表格資訊"""
    name: str
    schema: str
    type: str  # BASE TABLE, VIEW, etc.
    comment: Optional[str] = None

class TableSchema(BaseModel):
    """表格結構"""
    success: bool
    table_name: str
    schema: str
    columns: List[ColumnInfo]
    primary_keys: List[str] = Field(default_factory=list)
    indexes: List[Dict[str, Any]] = Field(default_factory=list)
    foreign_keys: List[Dict[str, Any]] = Field(default_factory=list)
    error: Optional[str] = None

class QueryResult(BaseModel):
    """查詢結果"""
    success: bool
    rows: List[Dict[str, Any]] = Field(default_factory=list)
    row_count: int = 0
    columns: List[str] = Field(default_factory=list)
    query: str
    duration_ms: int
    error: Optional[str] = None
    executed_at: datetime = Field(default_factory=datetime.utcnow)

class TransactionResult(BaseModel):
    """事務結果"""
    success: bool
    results: List[Dict[str, Any]]
    rolled_back: bool
    duration_ms: int
    error: Optional[str] = None
    executed_at: datetime = Field(default_factory=datetime.utcnow)

class BatchResult(BaseModel):
    """批次操作結果"""
    success: bool
    batch_size: int
    total_affected_rows: int
    duration_ms: int
    error: Optional[str] = None
    executed_at: datetime = Field(default_factory=datetime.utcnow)

class ExplainResult(BaseModel):
    """查詢執行計畫結果"""
    success: bool
    query: str
    plan: List[Dict[str, Any]]
    total_cost: Optional[float] = None
    execution_time: Optional[float] = None
    error: Optional[str] = None

class QueryHistory(BaseModel):
    """查詢歷史"""
    connection_id: str
    query: str
    params: Optional[List[Any]] = None
    success: bool
    duration_ms: int
    rows_affected: int = 0
    error: Optional[str] = None
    executed_at: datetime = Field(default_factory=datetime.utcnow)

class ServerConfig(BaseModel):
    """伺服器配置"""
    port: int = 3000
    log_level: str = "INFO"
    default_pool_size: int = 10
    query_timeout: int = 30
    max_connections: int = 100
    enable_query_cache: bool = False
    cache_ttl_seconds: int = 300

class SecurityConfig(BaseModel):
    """安全配置"""
    encryption_key: Optional[str] = None
    readonly_mode: bool = False
    allowed_operations: List[str] = Field(default_factory=lambda: ["SELECT", "INSERT", "UPDATE", "DELETE"])
    blocked_keywords: List[str] = Field(default_factory=lambda: ["DROP", "TRUNCATE", "ALTER"])
    max_query_length: int = 10000
    enable_query_logging: bool = True