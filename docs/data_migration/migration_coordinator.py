#!/usr/bin/env python3
"""
Kanban Data Migration Coordinator
智能資料遷移協調器，使用 LLM 執行 old_kanban_data 到 PostgreSQL 的遷移
"""

import asyncio
import json
import time
from datetime import datetime
from typing import Dict, List, Any, Optional
import structlog

logger = structlog.get_logger()


class KanbanMigrationCoordinator:
    """Kanban 資料遷移協調器"""

    def __init__(self, mysql_mcp_client, postgresql_mcp_client):
        """
        初始化遷移協調器

        Args:
            mysql_mcp_client: MySQL MCP Server 客戶端
            postgresql_mcp_client: PostgreSQL MCP Server 客戶端
        """
        self.mysql_client = mysql_mcp_client
        self.pg_client = postgresql_mcp_client
        self.migration_log = []
        self.migration_start_time = None
        self.migration_config = {
            "batch_size": 200,
            "state_sourcing_priority": True,
            "verification_enabled": True,
            "error_handling": "skip_and_log",
            "rollback_support": True
        }

    async def execute_migration(
        self,
        mysql_connection_id: str = "old_kanban",
        pg_connection_id: str = "target_pg",
        target_schema: str = "public"
    ) -> Dict[str, Any]:
        """
        執行完整的 Kanban 資料遷移流程

        Returns:
            遷移結果報告
        """
        self.migration_start_time = datetime.utcnow()

        try:
            logger.info("kanban_migration_started",
                       mysql_conn=mysql_connection_id,
                       pg_conn=pg_connection_id)

            # Phase 1: 環境準備和驗證
            await self._phase1_environment_setup(mysql_connection_id, pg_connection_id)

            # Phase 2: 架構分析和映射
            schema_mapping = await self._phase2_schema_analysis(
                mysql_connection_id, pg_connection_id, target_schema
            )

            # Phase 3: State Sourcing 優先遷移
            migration_results = await self._phase3_migration_execution(
                mysql_connection_id, pg_connection_id, schema_mapping
            )

            # Phase 4: 驗證和報告
            verification_results = await self._phase4_verification(
                mysql_connection_id, pg_connection_id, schema_mapping
            )

            # 生成最終報告
            final_report = self._generate_migration_report(
                migration_results, verification_results, schema_mapping
            )

            logger.info("kanban_migration_completed",
                       duration_seconds=(datetime.utcnow() - self.migration_start_time).total_seconds(),
                       success_rate=final_report["success_rate_percent"])

            return final_report

        except Exception as e:
            logger.error("kanban_migration_failed", error=str(e))
            return {
                "success": False,
                "error": str(e),
                "phase": "unknown",
                "migration_log": self.migration_log
            }

    async def _phase1_environment_setup(self, mysql_conn_id: str, pg_conn_id: str):
        """Phase 1: 環境準備和連線驗證"""

        self._log_migration_step("Phase 1: Environment Setup", "started")

        # 測試 MySQL 連線
        mysql_health = await self.mysql_client.call_tool("health_check", {
            "connection_id": mysql_conn_id
        })

        if not mysql_health.get("is_healthy"):
            raise Exception(f"MySQL connection unhealthy: {mysql_health.get('error')}")

        # 測試 PostgreSQL 連線
        pg_health = await self.pg_client.call_tool("health_check", {
            "connection_id": pg_conn_id
        })

        if not pg_health.get("is_healthy"):
            raise Exception(f"PostgreSQL connection unhealthy: {pg_health.get('error')}")

        self._log_migration_step("Phase 1: Environment Setup", "completed")

    async def _phase2_schema_analysis(
        self, mysql_conn_id: str, pg_conn_id: str, target_schema: str
    ) -> Dict[str, Any]:
        """Phase 2: 架構分析和映射生成"""

        self._log_migration_step("Phase 2: Schema Analysis", "started")

        # 分析 MySQL 舊架構
        mysql_tables = await self.mysql_client.call_tool("list_tables", {
            "connection_id": mysql_conn_id,
            "schema_name": "old_kanban_data"
        })

        mysql_schemas = {}
        for table in mysql_tables.get("tables", []):
            schema = await self.mysql_client.call_tool("get_table_schema", {
                "connection_id": mysql_conn_id,
                "table_name": table,
                "schema_name": "old_kanban_data"
            })
            mysql_schemas[table] = schema

        # 分析 PostgreSQL 新架構
        pg_tables = await self.pg_client.call_tool("list_tables", {
            "connection_id": pg_conn_id,
            "schema_name": target_schema
        })

        pg_schemas = {}
        for table in pg_tables.get("tables", []):
            schema = await self.pg_client.call_tool("get_table_schema", {
                "connection_id": pg_conn_id,
                "table_name": table,
                "schema_name": target_schema
            })
            pg_schemas[table] = schema

        # 生成智能映射
        schema_mapping = self._generate_intelligent_mapping(mysql_schemas, pg_schemas)

        self._log_migration_step("Phase 2: Schema Analysis", "completed", {
            "mysql_tables": len(mysql_schemas),
            "postgresql_tables": len(pg_schemas),
            "mapped_tables": len(schema_mapping["table_mappings"])
        })

        return schema_mapping

    def _generate_intelligent_mapping(
        self, mysql_schemas: Dict, pg_schemas: Dict
    ) -> Dict[str, Any]:
        """
        生成智能架構映射

        這裡實現基本的映射邏輯，實際使用時會由 LLM 分析並生成
        """

        mapping = {
            "table_mappings": {},
            "column_mappings": {},
            "data_transformations": {},
            "migration_order": []
        }

        # 基本表映射（實際會由 LLM 智能分析）
        common_tables = set(mysql_schemas.keys()) & set(pg_schemas.keys())

        for table in common_tables:
            mysql_schema = mysql_schemas[table]
            pg_schema = pg_schemas[table]

            # 映射表
            mapping["table_mappings"][table] = table

            # 映射欄位
            mysql_columns = {col["name"]: col for col in mysql_schema.get("columns", [])}
            pg_columns = {col["name"]: col for col in pg_schema.get("columns", [])}

            column_mapping = {}
            for mysql_col, mysql_def in mysql_columns.items():
                if mysql_col in pg_columns:
                    column_mapping[mysql_col] = mysql_col
                # 處理欄位名稱變更（實際會由 LLM 智能推導）
                elif mysql_col == "id" and "id" in pg_columns:
                    column_mapping[mysql_col] = "id"

            mapping["column_mappings"][table] = column_mapping

            # 資料轉換規則
            mapping["data_transformations"][table] = {
                "timestamp_format": "ISO8601",
                "boolean_mapping": {"0": False, "1": True},
                "null_handling": "preserve"
            }

        # State Sourcing 優先遷移順序
        mapping["migration_order"] = [
            "users",       # 基礎用戶資料
            "projects",    # 專案資料
            "boards",      # 看板資料
            "lists",       # 列表資料
            "cards",       # 卡片資料
            "comments",    # 評論資料
            "attachments"  # 附件資料
        ]

        return mapping

    async def _phase3_migration_execution(
        self, mysql_conn_id: str, pg_conn_id: str, schema_mapping: Dict
    ) -> Dict[str, Any]:
        """Phase 3: 執行 State Sourcing 優先遷移"""

        self._log_migration_step("Phase 3: Migration Execution", "started")

        migration_results = {
            "tables_migrated": {},
            "total_records": 0,
            "successful_records": 0,
            "failed_records": 0,
            "errors": []
        }

        # 按照 State Sourcing 優先順序遷移
        for table_name in schema_mapping["migration_order"]:
            if table_name not in schema_mapping["table_mappings"]:
                continue

            try:
                table_result = await self._migrate_table(
                    mysql_conn_id, pg_conn_id, table_name, schema_mapping
                )
                migration_results["tables_migrated"][table_name] = table_result
                migration_results["total_records"] += table_result["total_records"]
                migration_results["successful_records"] += table_result["successful_records"]
                migration_results["failed_records"] += table_result["failed_records"]

            except Exception as e:
                error_info = {
                    "table": table_name,
                    "error": str(e),
                    "timestamp": datetime.utcnow().isoformat()
                }
                migration_results["errors"].append(error_info)
                logger.error("table_migration_failed", table=table_name, error=str(e))

        self._log_migration_step("Phase 3: Migration Execution", "completed", {
            "total_records": migration_results["total_records"],
            "successful_records": migration_results["successful_records"],
            "failed_records": migration_results["failed_records"]
        })

        return migration_results

    async def _migrate_table(
        self, mysql_conn_id: str, pg_conn_id: str, table_name: str, schema_mapping: Dict
    ) -> Dict[str, Any]:
        """遷移單個表的資料"""

        target_table = schema_mapping["table_mappings"][table_name]
        column_mapping = schema_mapping["column_mappings"].get(table_name, {})
        transformations = schema_mapping["data_transformations"].get(table_name, {})

        # 獲取總記錄數
        count_result = await self.mysql_client.call_tool("execute_query", {
            "connection_id": mysql_conn_id,
            "query": f"SELECT COUNT(*) as total FROM {table_name}"
        })
        total_records = count_result["rows"][0]["total"] if count_result.get("rows") else 0

        result = {
            "table_name": table_name,
            "target_table": target_table,
            "total_records": total_records,
            "successful_records": 0,
            "failed_records": 0,
            "batches_processed": 0,
            "errors": []
        }

        # 批次處理
        batch_size = self.migration_config["batch_size"]
        offset = 0

        while offset < total_records:
            try:
                # 讀取批次資料
                mysql_data = await self.mysql_client.call_tool("execute_query", {
                    "connection_id": mysql_conn_id,
                    "query": f"SELECT * FROM {table_name} LIMIT {batch_size} OFFSET {offset}"
                })

                if not mysql_data.get("rows"):
                    break

                # 轉換資料格式
                transformed_data = self._transform_batch_data(
                    mysql_data["rows"], column_mapping, transformations
                )

                # 寫入 PostgreSQL
                if transformed_data:
                    insert_result = await self._insert_batch_to_postgresql(
                        pg_conn_id, target_table, transformed_data, column_mapping
                    )

                    if insert_result.get("success"):
                        result["successful_records"] += len(transformed_data)
                    else:
                        result["failed_records"] += len(transformed_data)
                        result["errors"].append({
                            "batch_offset": offset,
                            "error": insert_result.get("error")
                        })

                result["batches_processed"] += 1
                offset += batch_size

                # 記錄進度
                if result["batches_processed"] % 10 == 0:
                    logger.info("table_migration_progress",
                               table=table_name,
                               progress=f"{offset}/{total_records}",
                               success_rate=f"{result['successful_records']}/{offset}")

            except Exception as e:
                result["failed_records"] += batch_size
                result["errors"].append({
                    "batch_offset": offset,
                    "error": str(e)
                })
                offset += batch_size

        return result

    def _transform_batch_data(
        self, mysql_rows: List[Dict], column_mapping: Dict, transformations: Dict
    ) -> List[Dict]:
        """轉換批次資料格式"""

        transformed_rows = []

        for row in mysql_rows:
            transformed_row = {}

            for mysql_col, value in row.items():
                # 映射欄位名稱
                pg_col = column_mapping.get(mysql_col, mysql_col)

                # 應用資料轉換
                if pg_col:
                    transformed_value = self._transform_value(value, transformations)
                    transformed_row[pg_col] = transformed_value

            if transformed_row:
                transformed_rows.append(transformed_row)

        return transformed_rows

    def _transform_value(self, value: Any, transformations: Dict) -> Any:
        """轉換單個值"""

        if value is None:
            return None

        # Boolean 轉換
        if "boolean_mapping" in transformations and str(value) in transformations["boolean_mapping"]:
            return transformations["boolean_mapping"][str(value)]

        # 時間戳轉換
        if "timestamp_format" in transformations and isinstance(value, str):
            # 實際實現會更複雜，這裡簡化處理
            return value

        return value

    async def _insert_batch_to_postgresql(
        self, pg_conn_id: str, table_name: str, data: List[Dict], column_mapping: Dict
    ) -> Dict[str, Any]:
        """批次插入資料到 PostgreSQL"""

        if not data:
            return {"success": True, "rows_inserted": 0}

        # 生成插入語句
        columns = list(data[0].keys())
        placeholders = ", ".join([f"${i+1}" for i in range(len(columns))])
        query = f"INSERT INTO {table_name} ({', '.join(columns)}) VALUES ({placeholders})"

        # 準備參數
        params_list = []
        for row in data:
            params = [row.get(col) for col in columns]
            params_list.append(params)

        # 執行批次插入
        try:
            result = await self.pg_client.call_tool("execute_batch", {
                "connection_id": pg_conn_id,
                "query": query,
                "params_list": params_list
            })

            return result

        except Exception as e:
            return {
                "success": False,
                "error": str(e)
            }

    async def _phase4_verification(
        self, mysql_conn_id: str, pg_conn_id: str, schema_mapping: Dict
    ) -> Dict[str, Any]:
        """Phase 4: 驗證遷移結果"""

        self._log_migration_step("Phase 4: Verification", "started")

        verification_results = {
            "table_verifications": {},
            "overall_success": True,
            "issues_found": []
        }

        for table_name in schema_mapping["table_mappings"]:
            try:
                table_verification = await self._verify_table_migration(
                    mysql_conn_id, pg_conn_id, table_name, schema_mapping
                )
                verification_results["table_verifications"][table_name] = table_verification

                if not table_verification["success"]:
                    verification_results["overall_success"] = False
                    verification_results["issues_found"].extend(table_verification["issues"])

            except Exception as e:
                verification_results["overall_success"] = False
                verification_results["issues_found"].append({
                    "table": table_name,
                    "issue": f"Verification failed: {str(e)}"
                })

        self._log_migration_step("Phase 4: Verification", "completed", {
            "overall_success": verification_results["overall_success"],
            "issues_count": len(verification_results["issues_found"])
        })

        return verification_results

    async def _verify_table_migration(
        self, mysql_conn_id: str, pg_conn_id: str, table_name: str, schema_mapping: Dict
    ) -> Dict[str, Any]:
        """驗證單個表的遷移結果"""

        target_table = schema_mapping["table_mappings"][table_name]

        # 比較記錄數量
        mysql_count = await self.mysql_client.call_tool("execute_query", {
            "connection_id": mysql_conn_id,
            "query": f"SELECT COUNT(*) as count FROM {table_name}"
        })

        pg_count = await self.pg_client.call_tool("execute_query", {
            "connection_id": pg_conn_id,
            "query": f"SELECT COUNT(*) as count FROM {target_table}"
        })

        mysql_total = mysql_count["rows"][0]["count"] if mysql_count.get("rows") else 0
        pg_total = pg_count["rows"][0]["count"] if pg_count.get("rows") else 0

        verification = {
            "table_name": table_name,
            "target_table": target_table,
            "mysql_records": mysql_total,
            "postgresql_records": pg_total,
            "success": mysql_total == pg_total,
            "issues": []
        }

        if mysql_total != pg_total:
            verification["issues"].append({
                "type": "record_count_mismatch",
                "message": f"Record count mismatch: MySQL {mysql_total} vs PostgreSQL {pg_total}"
            })

        return verification

    def _generate_migration_report(
        self, migration_results: Dict, verification_results: Dict, schema_mapping: Dict
    ) -> Dict[str, Any]:
        """生成最終遷移報告"""

        duration = (datetime.utcnow() - self.migration_start_time).total_seconds()

        total_records = migration_results["total_records"]
        successful_records = migration_results["successful_records"]
        success_rate = (successful_records / total_records * 100) if total_records > 0 else 0

        report = {
            "migration_summary": {
                "start_time": self.migration_start_time.isoformat(),
                "end_time": datetime.utcnow().isoformat(),
                "duration_seconds": duration,
                "success": verification_results["overall_success"] and migration_results["failed_records"] == 0
            },
            "data_summary": {
                "total_tables": len(schema_mapping["table_mappings"]),
                "total_records": total_records,
                "successful_records": successful_records,
                "failed_records": migration_results["failed_records"],
                "success_rate_percent": round(success_rate, 2)
            },
            "table_details": migration_results["tables_migrated"],
            "verification_results": verification_results,
            "migration_config": self.migration_config,
            "migration_log": self.migration_log,
            "recommendations": self._generate_recommendations(migration_results, verification_results)
        }

        return report

    def _generate_recommendations(
        self, migration_results: Dict, verification_results: Dict
    ) -> List[str]:
        """生成遷移建議"""

        recommendations = []

        if migration_results["failed_records"] > 0:
            recommendations.append(
                f"There were {migration_results['failed_records']} failed records. "
                "Review the error logs and consider manual data correction."
            )

        if not verification_results["overall_success"]:
            recommendations.append(
                "Data verification found issues. "
                "Check the verification results and address any inconsistencies."
            )

        if migration_results["successful_records"] == migration_results["total_records"]:
            recommendations.append(
                "Migration completed successfully! "
                "Consider implementing Event Sourcing data migration for historical records if needed."
            )

        return recommendations

    def _log_migration_step(self, step: str, status: str, details: Optional[Dict] = None):
        """記錄遷移步驟"""

        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "step": step,
            "status": status,
            "details": details or {}
        }

        self.migration_log.append(log_entry)
        logger.info("migration_step", step=step, status=status, details=details)


# 使用範例
async def main():
    """執行 Kanban 資料遷移的主函數"""

    # 初始化 MCP 客戶端（實際使用時需要真實的客戶端）
    mysql_client = None  # MySQLMCPClient()
    pg_client = None     # PostgreSQLMCPClient()

    # 建立遷移協調器
    coordinator = KanbanMigrationCoordinator(mysql_client, pg_client)

    # 執行遷移
    migration_report = await coordinator.execute_migration(
        mysql_connection_id="old_kanban",
        pg_connection_id="target_pg",
        target_schema="public"
    )

    # 輸出報告
    print(json.dumps(migration_report, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    asyncio.run(main())