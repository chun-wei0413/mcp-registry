from datetime import datetime
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class Connection(BaseModel):
    """Database connection entity."""
    connection_id: str
    host: str
    port: int
    database: str
    user: str
    encrypted_password: str
    pool_size: int = 10
    is_active: bool = True
    created_at: datetime = Field(default_factory=datetime.utcnow)


class Query(BaseModel):
    """SQL query value object."""
    sql: str
    params: List[Any] = Field(default_factory=list)
    query_id: Optional[str] = None
    connection_id: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)


class QueryResult(BaseModel):
    """Query execution result."""
    success: bool
    query_id: Optional[str] = None
    columns: List[str] = Field(default_factory=list)
    rows: List[Dict[str, Any]] = Field(default_factory=list)
    row_count: int = 0
    execution_time_ms: int = 0
    message: Optional[str] = None
    executed_at: datetime = Field(default_factory=datetime.utcnow)


class ColumnInfo(BaseModel):
    """Database column information."""
    name: str
    data_type: str
    is_nullable: bool
    default_value: Optional[str] = None
    max_length: Optional[int] = None
    precision: Optional[int] = None
    scale: Optional[int] = None
    comment: Optional[str] = None


class IndexInfo(BaseModel):
    """Database index information."""
    name: str
    definition: str
    is_unique: bool
    is_primary: bool


class ConstraintInfo(BaseModel):
    """Database constraint information."""
    name: str
    type: str
    column_name: Optional[str] = None
    foreign_table: Optional[str] = None
    foreign_column: Optional[str] = None


class TableSchema(BaseModel):
    """Database table schema information."""
    table_name: str
    schema_name: str = "public"
    columns: List[ColumnInfo] = Field(default_factory=list)
    indexes: List[IndexInfo] = Field(default_factory=list)
    constraints: List[ConstraintInfo] = Field(default_factory=list)
    row_count: int = 0
    table_size_bytes: int = 0


class HealthStatus(BaseModel):
    """System health status."""
    is_healthy: bool
    response_time_ms: int
    last_check: int
    details: Dict[str, Any] = Field(default_factory=dict)


class MetricsData(BaseModel):
    """Connection metrics data."""
    connection_id: str
    total_queries: int = 0
    successful_queries: int = 0
    failed_queries: int = 0
    average_execution_time_ms: int = 0
    last_query_time: Optional[int] = None
    query_history: List[Dict[str, Any]] = Field(default_factory=list)