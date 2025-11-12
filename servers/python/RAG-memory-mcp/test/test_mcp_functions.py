#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RAG Memory MCP Server 功能測試
測試三個核心功能：learn_knowledge, search_knowledge, retrieve_all_by_topic
"""
import subprocess
import json
import time

def send_mcp_requests(requests):
    """發送 MCP 請求到 Docker 容器"""
    input_data = '\n'.join([json.dumps(req) for req in requests]) + '\n'

    proc = subprocess.Popen(
        ['docker', 'exec', '-i', 'memory-mcp-server', 'python', 'mcp_server.py'],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )

    try:
        stdout, stderr = proc.communicate(input=input_data, timeout=10)

        # 解析響應
        responses = []
        for line in stdout.strip().split('\n'):
            if line.strip():
                try:
                    responses.append(json.loads(line))
                except:
                    pass
        return responses
    except subprocess.TimeoutExpired:
        proc.kill()
        return []

def test_learn_knowledge():
    """測試 1: learn_knowledge - 新增知識點"""
    print("\n" + "="*70)
    print("測試 1: learn_knowledge - 新增知識點")
    print("="*70)

    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")

    requests = [
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "test", "version": "1.0"}
            }
        },
        {
            "jsonrpc": "2.0",
            "method": "notifications/initialized"
        },
        {
            "jsonrpc": "2.0",
            "id": 2,
            "method": "tools/call",
            "params": {
                "name": "learn_knowledge",
                "arguments": {
                    "topic": "FastMCP",
                    "content": f"FastMCP 是 Anthropic 提供的 Python SDK，用於快速建立 MCP servers。它支援 stdio 傳輸協議。測試時間: {timestamp}"
                }
            }
        }
    ]

    print("\n[執行] 呼叫 learn_knowledge...")
    print(f"  主題: FastMCP")
    print(f"  內容: FastMCP 是 Anthropic 提供的...")

    responses = send_mcp_requests(requests)

    # 找到對應的響應
    for resp in responses:
        if resp.get('id') == 2:
            if 'result' in resp:
                result_text = resp['result']['content'][0]['text']
                print(f"\n[成功] 知識點已新增")
                print(f"  響應: {result_text[:100]}...")
                return True
            else:
                print(f"\n[失敗] {resp.get('error', '未知錯誤')}")
                return False

    print("\n[失敗] 未收到響應")
    return False

def test_search_knowledge():
    """測試 2: search_knowledge - 搜尋知識"""
    print("\n" + "="*70)
    print("測試 2: search_knowledge - 搜尋知識")
    print("="*70)

    requests = [
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "test", "version": "1.0"}
            }
        },
        {
            "jsonrpc": "2.0",
            "method": "notifications/initialized"
        },
        {
            "jsonrpc": "2.0",
            "id": 3,
            "method": "tools/call",
            "params": {
                "name": "search_knowledge",
                "arguments": {
                    "query": "FastMCP Python SDK",
                    "top_k": 3
                }
            }
        }
    ]

    print("\n[執行] 呼叫 search_knowledge...")
    print(f"  查詢: FastMCP Python SDK")
    print(f"  返回數量: 3")

    responses = send_mcp_requests(requests)

    for resp in responses:
        if resp.get('id') == 3:
            if 'result' in resp:
                result_text = resp['result']['content'][0]['text']
                print(f"\n[成功] 搜尋完成")
                print(f"  結果預覽:")
                # 嘗試解析 JSON 結果
                try:
                    result_data = json.loads(result_text)
                    if 'results' in result_data:
                        print(f"  找到 {len(result_data['results'])} 個結果")
                        for i, item in enumerate(result_data['results'][:3], 1):
                            print(f"    {i}. 主題: {item.get('topic', 'N/A')}")
                            print(f"       相似度: {item.get('similarity', 0):.4f}")
                            print(f"       內容: {item.get('content', '')[:80]}...")
                except:
                    print(f"  {result_text[:200]}...")
                return True
            else:
                print(f"\n[失敗] {resp.get('error', '未知錯誤')}")
                return False

    print("\n[失敗] 未收到響應")
    return False

def test_retrieve_all_by_topic():
    """測試 3: retrieve_all_by_topic - 按主題檢索"""
    print("\n" + "="*70)
    print("測試 3: retrieve_all_by_topic - 按主題檢索")
    print("="*70)

    # 先新增幾個同主題的知識點
    print("\n[準備] 先新增幾個測試知識點...")

    topics_to_add = [
        ("DDD", "Aggregate Root 是 Domain-Driven Design 的核心概念"),
        ("DDD", "Bounded Context 定義了模型的適用範圍"),
        ("DDD", "Entity 和 Value Object 是 DDD 的基本構建塊")
    ]

    for topic, content in topics_to_add:
        requests = [
            {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "test", "version": "1.0"}}},
            {"jsonrpc": "2.0", "method": "notifications/initialized"},
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/call",
                "params": {
                    "name": "learn_knowledge",
                    "arguments": {"topic": topic, "content": content}
                }
            }
        ]
        send_mcp_requests(requests)
        print(f"  已新增: {topic} - {content[:30]}...")

    # 現在測試檢索
    print("\n[執行] 呼叫 resources/read (retrieve_all_by_topic)...")
    print(f"  主題: DDD")

    requests = [
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "test", "version": "1.0"}
            }
        },
        {
            "jsonrpc": "2.0",
            "method": "notifications/initialized"
        },
        {
            "jsonrpc": "2.0",
            "id": 4,
            "method": "resources/read",
            "params": {
                "uri": "knowledge://DDD"
            }
        }
    ]

    responses = send_mcp_requests(requests)

    for resp in responses:
        if resp.get('id') == 4:
            if 'result' in resp:
                contents = resp['result']['contents']
                print(f"\n[成功] 檢索完成")
                print(f"  找到 {len(contents)} 個資源")
                for i, content in enumerate(contents, 1):
                    print(f"    {i}. URI: {content.get('uri', 'N/A')}")
                    text = content.get('text', '')
                    print(f"       內容預覽: {text[:80]}...")
                return True
            else:
                print(f"\n[失敗] {resp.get('error', '未知錯誤')}")
                return False

    print("\n[失敗] 未收到響應")
    return False

if __name__ == "__main__":
    print("="*70)
    print(" RAG Memory MCP Server - 功能測試")
    print("="*70)
    print("\n測試三個核心功能:")
    print("  1. learn_knowledge - 新增知識點")
    print("  2. search_knowledge - 搜尋知識")
    print("  3. retrieve_all_by_topic - 按主題檢索")

    results = []

    # 執行測試
    results.append(("learn_knowledge", test_learn_knowledge()))
    time.sleep(1)

    results.append(("search_knowledge", test_search_knowledge()))
    time.sleep(1)

    results.append(("retrieve_all_by_topic", test_retrieve_all_by_topic()))

    # 總結
    print("\n" + "="*70)
    print(" 測試結果總結")
    print("="*70)

    for name, success in results:
        status = "✓ 通過" if success else "✗ 失敗"
        print(f"  {name:30s} {status}")

    passed = sum(1 for _, s in results if s)
    total = len(results)

    print(f"\n  總計: {passed}/{total} 個測試通過")
    print("="*70)
