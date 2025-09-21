"""
Unit tests for security validation functionality
"""

import pytest
from src.postgresql_mcp.infrastructure.security.validator import PostgreSQLSecurityValidator


class TestSecurityValidator:
    """Test security validation functionality."""

    @pytest.fixture
    def security_config(self):
        """Create a security config for testing."""
        return SecurityConfig(
            readonly_mode=False,
            allowed_operations=["SELECT", "INSERT", "UPDATE", "DELETE"],
            blocked_keywords=["DROP", "TRUNCATE", "ALTER"],
            max_query_length=1000,
            enable_query_logging=True
        )

    @pytest.fixture
    def readonly_config(self):
        """Create a readonly security config."""
        return SecurityConfig(
            readonly_mode=True,
            allowed_operations=["SELECT", "WITH", "EXPLAIN"],
            blocked_keywords=["DROP", "TRUNCATE", "ALTER", "INSERT", "UPDATE", "DELETE"],
            max_query_length=1000,
            enable_query_logging=True
        )

    @pytest.fixture
    def validator(self, security_config):
        """Create a security validator."""
        return SecurityValidator(security_config)

    @pytest.fixture
    def readonly_validator(self, readonly_config):
        """Create a readonly security validator."""
        return SecurityValidator(readonly_config)

    def test_validate_safe_select_query(self, validator):
        """Test validation of safe SELECT query."""
        query = "SELECT * FROM users WHERE id = $1"
        result = validator.validate_query(query)

        assert result.is_valid
        assert result.error_message is None
        assert result.blocked_reason is None

    def test_validate_safe_insert_query(self, validator):
        """Test validation of safe INSERT query."""
        query = "INSERT INTO users (name, email) VALUES ($1, $2)"
        result = validator.validate_query(query)

        assert result.is_valid

    def test_validate_safe_update_query(self, validator):
        """Test validation of safe UPDATE query."""
        query = "UPDATE users SET last_login = NOW() WHERE id = $1"
        result = validator.validate_query(query)

        assert result.is_valid

    def test_validate_safe_delete_query(self, validator):
        """Test validation of safe DELETE query."""
        query = "DELETE FROM temp_data WHERE created_at < NOW() - INTERVAL '1 day'"
        result = validator.validate_query(query)

        assert result.is_valid

    def test_validate_with_query(self, validator):
        """Test validation of WITH query."""
        query = """
        WITH ranked_products AS (
            SELECT *, ROW_NUMBER() OVER (ORDER BY price DESC) as rank
            FROM products
        )
        SELECT * FROM ranked_products WHERE rank <= 10
        """
        result = validator.validate_query(query)

        assert result.is_valid

    def test_block_drop_table(self, validator):
        """Test blocking of DROP TABLE query."""
        query = "DROP TABLE users"
        result = validator.validate_query(query)

        assert not result.is_valid
        assert "DROP" in result.error_message
        assert result.blocked_reason == "BLOCKED_KEYWORD"

    def test_block_truncate_table(self, validator):
        """Test blocking of TRUNCATE query."""
        query = "TRUNCATE TABLE orders"
        result = validator.validate_query(query)

        assert not result.is_valid
        assert "TRUNCATE" in result.error_message
        assert result.blocked_reason == "BLOCKED_KEYWORD"

    def test_block_alter_table(self, validator):
        """Test blocking of ALTER TABLE query."""
        query = "ALTER TABLE products ADD COLUMN secret TEXT"
        result = validator.validate_query(query)

        assert not result.is_valid
        assert "ALTER" in result.error_message
        assert result.blocked_reason == "BLOCKED_KEYWORD"

    def test_block_dangerous_delete(self, validator):
        """Test blocking of DELETE without WHERE clause."""
        query = "DELETE FROM users"
        result = validator.validate_query(query)

        assert not result.is_valid
        assert result.blocked_reason == "DANGEROUS_PATTERN"

    def test_block_system_functions(self, validator):
        """Test blocking of dangerous system functions."""
        queries = [
            "SELECT pg_read_file('/etc/passwd')",
            "SELECT pg_write_file('/tmp/test', 'data')",
            "SELECT dblink('foreign_server', 'SELECT 1')"
        ]

        for query in queries:
            result = validator.validate_query(query)
            assert not result.is_valid
            assert result.blocked_reason == "DANGEROUS_PATTERN"

    def test_block_copy_to_program(self, validator):
        """Test blocking of COPY TO PROGRAM."""
        query = "COPY users TO PROGRAM 'cat > /tmp/dump.sql'"
        result = validator.validate_query(query)

        assert not result.is_valid
        assert result.blocked_reason == "DANGEROUS_PATTERN"

    def test_query_length_limit(self, validator):
        """Test query length limitation."""
        long_query = "SELECT * FROM users WHERE " + " AND ".join([f"id != {i}" for i in range(200)])
        result = validator.validate_query(long_query)

        assert not result.is_valid
        assert "length" in result.error_message.lower()
        assert result.blocked_reason == "QUERY_TOO_LONG"

    def test_readonly_mode_allows_select(self, readonly_validator):
        """Test readonly mode allows SELECT queries."""
        query = "SELECT * FROM users WHERE id = $1"
        result = readonly_validator.validate_query(query)

        assert result.is_valid

    def test_readonly_mode_allows_with(self, readonly_validator):
        """Test readonly mode allows WITH queries."""
        query = "WITH temp AS (SELECT * FROM users) SELECT * FROM temp"
        result = readonly_validator.validate_query(query)

        assert result.is_valid

    def test_readonly_mode_allows_explain(self, readonly_validator):
        """Test readonly mode allows EXPLAIN queries."""
        query = "EXPLAIN SELECT * FROM users"
        result = readonly_validator.validate_query(query)

        assert result.is_valid

    def test_readonly_mode_blocks_insert(self, readonly_validator):
        """Test readonly mode blocks INSERT queries."""
        query = "INSERT INTO users (name) VALUES ('test')"
        result = readonly_validator.validate_query(query)

        assert not result.is_valid
        assert result.blocked_reason == "READONLY_MODE_VIOLATION"

    def test_readonly_mode_blocks_update(self, readonly_validator):
        """Test readonly mode blocks UPDATE queries."""
        query = "UPDATE users SET name = 'updated' WHERE id = 1"
        result = readonly_validator.validate_query(query)

        assert not result.is_valid
        assert result.blocked_reason == "READONLY_MODE_VIOLATION"

    def test_readonly_mode_blocks_delete(self, readonly_validator):
        """Test readonly mode blocks DELETE queries."""
        query = "DELETE FROM users WHERE id = 1"
        result = readonly_validator.validate_query(query)

        assert not result.is_valid
        assert result.blocked_reason == "READONLY_MODE_VIOLATION"

    def test_clean_query_removes_comments(self, validator):
        """Test query cleaning removes comments."""
        query = """
        SELECT * FROM users
        WHERE id = $1 -- This is a comment
        /* This is a
           multi-line comment */
        AND active = true
        """
        cleaned = validator._clean_query(query)

        assert "--" not in cleaned
        assert "/*" not in cleaned
        assert "*/" not in cleaned
        assert "SELECT * FROM users WHERE id = $1 AND active = true" == cleaned.strip()

    def test_get_operation_type_select(self, validator):
        """Test operation type detection for SELECT."""
        queries = [
            "SELECT * FROM users",
            "  select id from orders",
            "\n\tSELECT COUNT(*) FROM products"
        ]

        for query in queries:
            op_type = validator._get_operation_type(query)
            assert op_type == "SELECT"

    def test_get_operation_type_various(self, validator):
        """Test operation type detection for various queries."""
        test_cases = [
            ("INSERT INTO users VALUES (1, 'test')", "INSERT"),
            ("UPDATE users SET name = 'new'", "UPDATE"),
            ("DELETE FROM users", "DELETE"),
            ("WITH temp AS (...) SELECT * FROM temp", "WITH"),
            ("DROP TABLE test", "DROP"),
            ("EXPLAIN SELECT * FROM users", "EXPLAIN"),
        ]

        for query, expected_type in test_cases:
            op_type = validator._get_operation_type(query)
            assert op_type == expected_type

    def test_validate_connection_params_valid(self, validator):
        """Test validation of valid connection parameters."""
        result = validator.validate_connection_params(
            host="localhost",
            port=5432,
            database="test_db",
            user="test_user"
        )

        assert result.is_valid

    def test_validate_connection_params_invalid_port(self, validator):
        """Test validation of invalid port."""
        result = validator.validate_connection_params(
            host="localhost",
            port=99999,
            database="test_db",
            user="test_user"
        )

        assert not result.is_valid
        assert "port" in result.error_message.lower()
        assert result.blocked_reason == "INVALID_PORT"

    def test_validate_connection_params_invalid_hostname(self, validator):
        """Test validation of invalid hostname."""
        result = validator.validate_connection_params(
            host="invalid@hostname!",
            port=5432,
            database="test_db",
            user="test_user"
        )

        assert not result.is_valid
        assert "hostname" in result.error_message.lower()
        assert result.blocked_reason == "INVALID_HOSTNAME"

    def test_validate_connection_params_invalid_database_name(self, validator):
        """Test validation of invalid database name."""
        result = validator.validate_connection_params(
            host="localhost",
            port=5432,
            database="invalid@db!",
            user="test_user"
        )

        assert not result.is_valid
        assert "database" in result.error_message.lower()
        assert result.blocked_reason == "INVALID_DATABASE_NAME"

    def test_validate_connection_params_invalid_username(self, validator):
        """Test validation of invalid username."""
        result = validator.validate_connection_params(
            host="localhost",
            port=5432,
            database="test_db",
            user="invalid@user!"
        )

        assert not result.is_valid
        assert "username" in result.error_message.lower()
        assert result.blocked_reason == "INVALID_USERNAME"

    def test_check_security_risks_system_tables(self, validator):
        """Test detection of system table access."""
        queries = [
            "SELECT * FROM pg_shadow",
            "SELECT * FROM pg_user",
            "SELECT * FROM information_schema.user_privileges"
        ]

        for query in queries:
            risks = validator._check_security_risks(query)
            assert len(risks) > 0
            assert any("system table" in risk.lower() for risk in risks)

    def test_check_security_risks_dynamic_sql(self, validator):
        """Test detection of dynamic SQL execution."""
        queries = [
            "EXECUTE 'SELECT * FROM users'",
            "PREPARE stmt AS SELECT * FROM users",
            "SELECT pg_exec('malicious code')"
        ]

        for query in queries:
            risks = validator._check_security_risks(query)
            assert len(risks) > 0
            assert any("dynamic sql" in risk.lower() or "sql execution" in risk.lower() for risk in risks)

    def test_check_security_risks_file_operations(self, validator):
        """Test detection of file operations."""
        queries = [
            "SELECT pg_read_file('/etc/passwd')",
            "SELECT pg_write_file('/tmp/test', 'data')",
            "COPY data FROM PROGRAM 'cat /etc/hosts'",
            "COPY data TO PROGRAM 'malicious_script.sh'"
        ]

        for query in queries:
            risks = validator._check_security_risks(query)
            assert len(risks) > 0
            assert any("file operation" in risk.lower() for risk in risks)