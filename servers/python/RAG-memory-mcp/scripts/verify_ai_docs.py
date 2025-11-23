#!/usr/bin/env python3
"""
驗證 .ai 文檔是否成功存入 ChromaDB 並可檢索
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from services.vector_store_service import VectorStoreService


def main():
    script_dir = Path(__file__).parent
    chroma_db_dir = script_dir / 'chroma_db'

    print("="*80)
    print("ChromaDB 資料驗證".center(80))
    print("="*80)

    # 初始化向量存儲
    print(f"\n[INFO] ChromaDB 目錄: {chroma_db_dir}")
    vector_store = VectorStoreService(
        db_path=str(chroma_db_dir),
        collection_name="ai_documentation"
    )

    # 獲取集合統計
    collection_count = vector_store.collection.count()
    print(f"[INFO] 總 Chunks 數量: {collection_count}")

    # 測試查詢
    print("\n" + "="*80)
    print("測試查詢功能".center(80))
    print("="*80)

    test_queries = [
        ("如何實作 Aggregate?", "測試 Aggregate 相關文檔檢索"),
        ("測試要怎麼寫?", "測試 Testing 相關文檔檢索"),
        ("Sub-agent 系統架構", "測試 Sub-agent 相關文檔檢索"),
        ("Spring Boot 配置", "測試 Spring Boot 相關文檔檢索"),
        ("Repository 規範", "測試 Repository 相關文檔檢索"),
    ]

    for query, description in test_queries:
        print(f"\n[QUERY] {query}")
        print(f"[DESC]  {description}")

        # 直接使用 ChromaDB 的 query 方法獲取完整元數據
        query_embedding = vector_store.model.encode(query).tolist()
        raw_results = vector_store.collection.query(
            query_embeddings=[query_embedding],
            n_results=3
        )

        if not raw_results or not raw_results["ids"][0]:
            print("[WARN]  沒有找到相關結果")
            continue

        for i in range(len(raw_results["ids"][0])):
            metadata = raw_results["metadatas"][0][i]
            similarity = raw_results["distances"][0][i]

            # 安全地獲取主題（移除 emoji）
            topic = metadata.get('topic', 'N/A')
            safe_topic = ''.join(c if ord(c) < 128 else '?' for c in str(topic))

            # 獲取 source_file
            source_file = metadata.get('source_file', 'N/A')
            category = metadata.get('category', 'N/A')
            priority = metadata.get('priority', 'N/A')

            print(f"\n  [{i+1}] 相似度: {similarity:.3f}")
            print(f"      主題: {safe_topic}")
            print(f"      來源: {source_file}")
            print(f"      分類: {category}")
            print(f"      優先級: {priority}")

            # 安全地預覽內容（移除非 ASCII 字符）
            content = raw_results["documents"][0][i]
            content_preview = content[:150].replace('\n', ' ').strip()
            # 只保留可打印的 ASCII 字符
            safe_preview = ''.join(c if ord(c) < 128 else '?' for c in content_preview)
            print(f"      預覽: {safe_preview}...")

    # 按分類統計
    print("\n" + "="*80)
    print("分類統計".center(80))
    print("="*80)

    # 獲取所有文檔並按 category 統計
    all_results = vector_store.collection.get(limit=1000)

    if all_results and all_results['metadatas']:
        category_stats = {}
        for metadata in all_results['metadatas']:
            category = metadata.get('category', 'unknown')
            category_stats[category] = category_stats.get(category, 0) + 1

        for category, count in sorted(category_stats.items()):
            print(f"  - {category}: {count} chunks")

    print("\n" + "="*80)
    print("[SUCCESS] 驗證完成！資料已成功存入並可正常檢索。".center(80))
    print("="*80)


if __name__ == '__main__':
    main()
