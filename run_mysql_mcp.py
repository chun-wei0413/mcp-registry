#!/usr/bin/env python3
"""簡化的 MySQL MCP Server 啟動腳本"""

import os
import sys
import asyncio
import uvicorn
from pathlib import Path

# 添加 src 目錄到 Python 路徑
project_root = Path(__file__).parent
src_path = project_root / "src"
sys.path.insert(0, str(src_path))

from src.mysql_mcp.mysql_server import MySQLMCPServer

def main():
    """主要啟動函數"""
    # 配置環境變數
    os.environ.setdefault("MCP_LOG_LEVEL", "INFO")

    try:
        # 創建 MCP Server
        server = MySQLMCPServer()

        # 使用同步 stdio 模式運行 (Claude Code 期望的模式)
        server.run_sync()

    except KeyboardInterrupt:
        print("伺服器被中斷", file=sys.stderr)
    except Exception as e:
        print(f"伺服器錯誤: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()