"""Security validation for PostgreSQL MCP Server."""

import re
from typing import List, Optional, Set
from dataclasses import dataclass

from .models.types import SecurityConfig


@dataclass
class SecurityValidationResult:
    """安全驗證結果"""
    is_valid: bool
    error_message: Optional[str] = None
    blocked_reason: Optional[str] = None


class SecurityValidator:
    """SQL 安全驗證器"""

    def __init__(self, config: SecurityConfig):
        self.config = config
        self._dangerous_patterns = self._build_dangerous_patterns()

    def _build_dangerous_patterns(self) -> List[re.Pattern]:
        """建立危險操作的正則表達式模式"""
        patterns = []

        # 基本的危險關鍵字
        dangerous_keywords = [
            r'\bDROP\s+',
            r'\bTRUNCATE\s+',
            r'\bALTER\s+',
            r'\bCREATE\s+',
            r'\bDELETE\s+FROM\s+(?!.*WHERE)',  # DELETE without WHERE
            r'\bUPDATE\s+.*SET\s+.*(?<!WHERE)',  # UPDATE without WHERE (簡化檢查)
            r'\bGRANT\s+',
            r'\bREVOKE\s+',
            r'\bCOPY\s+',
            r'\bVACUUM\s+',
            r'\bREINDEX\s+',
            r'\bANALYZE\s+',
            r'\bCLUSTER\s+',
        ]

        # 系統函數和危險操作
        system_functions = [
            r'pg_read_file\s*\(',
            r'pg_write_file\s*\(',
            r'pg_execute\s*\(',
            r'dblink\s*\(',
            r'pg_stat_file\s*\(',
            r'pg_ls_dir\s*\(',
        ]

        # 編譯正則表達式
        all_patterns = dangerous_keywords + system_functions
        for pattern in all_patterns:
            patterns.append(re.compile(pattern, re.IGNORECASE | re.MULTILINE))

        return patterns

    def validate_query(self, query: str) -> SecurityValidationResult:
        """驗證查詢的安全性"""

        # 1. 檢查查詢長度
        if len(query) > self.config.max_query_length:
            return SecurityValidationResult(
                is_valid=False,
                error_message=f"Query length ({len(query)}) exceeds maximum allowed ({self.config.max_query_length})",
                blocked_reason="QUERY_TOO_LONG"
            )

        # 2. 清理查詢字串以便分析
        cleaned_query = self._clean_query(query)

        # 3. 檢查是否為允許的操作類型
        operation_type = self._get_operation_type(cleaned_query)
        if operation_type not in self.config.allowed_operations:
            return SecurityValidationResult(
                is_valid=False,
                error_message=f"Operation '{operation_type}' is not allowed",
                blocked_reason="OPERATION_NOT_ALLOWED"
            )

        # 4. 如果是只讀模式，只允許 SELECT 和 WITH
        if self.config.readonly_mode:
            if operation_type not in ['SELECT', 'WITH', 'EXPLAIN']:
                return SecurityValidationResult(
                    is_valid=False,
                    error_message="Only SELECT, WITH, and EXPLAIN queries are allowed in readonly mode",
                    blocked_reason="READONLY_MODE_VIOLATION"
                )

        # 5. 檢查被阻擋的關鍵字
        for keyword in self.config.blocked_keywords:
            if re.search(rf'\b{re.escape(keyword)}\b', cleaned_query, re.IGNORECASE):
                return SecurityValidationResult(
                    is_valid=False,
                    error_message=f"Blocked keyword '{keyword}' found in query",
                    blocked_reason="BLOCKED_KEYWORD"
                )

        # 6. 檢查危險操作模式
        for pattern in self._dangerous_patterns:
            if pattern.search(cleaned_query):
                return SecurityValidationResult(
                    is_valid=False,
                    error_message=f"Dangerous operation pattern detected: {pattern.pattern}",
                    blocked_reason="DANGEROUS_PATTERN"
                )

        # 7. 檢查特殊安全風險
        security_risks = self._check_security_risks(cleaned_query)
        if security_risks:
            return SecurityValidationResult(
                is_valid=False,
                error_message=f"Security risk detected: {', '.join(security_risks)}",
                blocked_reason="SECURITY_RISK"
            )

        return SecurityValidationResult(is_valid=True)

    def _clean_query(self, query: str) -> str:
        """清理查詢字串，移除註釋和多餘空白"""
        # 移除單行註釋 (-- 註釋)
        query = re.sub(r'--.*$', '', query, flags=re.MULTILINE)

        # 移除多行註釋 (/* 註釋 */)
        query = re.sub(r'/\*.*?\*/', '', query, flags=re.DOTALL)

        # 正規化空白字符
        query = re.sub(r'\s+', ' ', query.strip())

        return query

    def _get_operation_type(self, query: str) -> str:
        """取得查詢的操作類型"""
        query_upper = query.upper().strip()

        operation_patterns = {
            'SELECT': r'^\s*SELECT\b',
            'INSERT': r'^\s*INSERT\b',
            'UPDATE': r'^\s*UPDATE\b',
            'DELETE': r'^\s*DELETE\b',
            'WITH': r'^\s*WITH\b',
            'CREATE': r'^\s*CREATE\b',
            'DROP': r'^\s*DROP\b',
            'ALTER': r'^\s*ALTER\b',
            'TRUNCATE': r'^\s*TRUNCATE\b',
            'EXPLAIN': r'^\s*EXPLAIN\b',
            'COPY': r'^\s*COPY\b',
            'VACUUM': r'^\s*VACUUM\b',
            'REINDEX': r'^\s*REINDEX\b',
            'ANALYZE': r'^\s*ANALYZE\b',
            'GRANT': r'^\s*GRANT\b',
            'REVOKE': r'^\s*REVOKE\b',
        }

        for op_type, pattern in operation_patterns.items():
            if re.match(pattern, query_upper):
                return op_type

        return 'UNKNOWN'

    def _check_security_risks(self, query: str) -> List[str]:
        """檢查其他安全風險"""
        risks = []

        # 檢查是否嘗試存取系統表
        system_tables = [
            'pg_shadow', 'pg_user', 'pg_authid', 'pg_auth_members',
            'information_schema.user_privileges', 'pg_roles'
        ]

        for table in system_tables:
            if re.search(rf'\b{re.escape(table)}\b', query, re.IGNORECASE):
                risks.append(f"Accessing system table: {table}")

        # 檢查是否嘗試執行動態 SQL
        dynamic_sql_patterns = [
            r'EXECUTE\s+',
            r'pg_exec\s*\(',
            r'PREPARE\s+',
        ]

        for pattern in dynamic_sql_patterns:
            if re.search(pattern, query, re.IGNORECASE):
                risks.append(f"Dynamic SQL execution detected: {pattern}")

        # 檢查是否包含檔案操作
        file_operations = [
            r'pg_read_file',
            r'pg_write_file',
            r'COPY.*FROM\s+PROGRAM',
            r'COPY.*TO\s+PROGRAM',
        ]

        for pattern in file_operations:
            if re.search(pattern, query, re.IGNORECASE):
                risks.append(f"File operation detected: {pattern}")

        return risks

    def validate_connection_params(
        self,
        host: str,
        port: int,
        database: str,
        user: str
    ) -> SecurityValidationResult:
        """驗證連線參數的安全性"""

        # 檢查主機名稱
        if host.lower() in ['localhost', '127.0.0.1', '::1']:
            pass  # 本地連線 OK
        elif not re.match(r'^[a-zA-Z0-9.-]+$', host):
            return SecurityValidationResult(
                is_valid=False,
                error_message="Invalid characters in hostname",
                blocked_reason="INVALID_HOSTNAME"
            )

        # 檢查埠號
        if not 1 <= port <= 65535:
            return SecurityValidationResult(
                is_valid=False,
                error_message="Port number must be between 1 and 65535",
                blocked_reason="INVALID_PORT"
            )

        # 檢查資料庫名稱
        if not re.match(r'^[a-zA-Z0-9_-]+$', database):
            return SecurityValidationResult(
                is_valid=False,
                error_message="Invalid characters in database name",
                blocked_reason="INVALID_DATABASE_NAME"
            )

        # 檢查使用者名稱
        if not re.match(r'^[a-zA-Z0-9_-]+$', user):
            return SecurityValidationResult(
                is_valid=False,
                error_message="Invalid characters in username",
                blocked_reason="INVALID_USERNAME"
            )

        return SecurityValidationResult(is_valid=True)