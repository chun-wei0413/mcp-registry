#!/usr/bin/env python3
"""
驗證 .ai 目錄文檔 Embedding - 智能程式碼分離

驗證項目：
1. ChromaDB 集合包含的文檔數量和分佈
2. 程式碼塊是否正確分離並儲存
3. 元數據是否完整
4. 搜尋功能是否正常工作
5. 性能指標分析
"""

import json
from pathlib import Path
from typing import Dict, List, Any
import sys

sys.path.insert(0, str(Path(__file__).parent.parent))

from services.vector_store_service import VectorStoreService


class AIDocsVerifier:
    """驗證器"""

    def __init__(self, vector_store: VectorStoreService):
        self.vector_store = vector_store
        self.separator_line = "=" * 80

    def run_verification(self) -> Dict[str, Any]:
        """運行所有驗證"""
        results = {
            'timestamp': self._get_timestamp(),
            'sections': {}
        }

        print(f"\n{self.separator_line}")
        print("驗證 .ai 目錄文檔 Embedding - 智能程式碼分離".center(80))
        print(self.separator_line + "\n")

        # 1. 集合統計
        results['sections']['collection_stats'] = self._verify_collection_stats()

        # 2. 元數據驗證
        results['sections']['metadata_validation'] = self._verify_metadata()

        # 3. 程式碼分離驗證
        results['sections']['code_separation'] = self._verify_code_separation()

        # 4. 搜尋功能驗證
        results['sections']['search_validation'] = self._verify_search()

        # 5. 性能指標
        results['sections']['performance_metrics'] = self._analyze_performance()

        # 6. 建議
        results['recommendations'] = self._generate_recommendations(results)

        return results

    def _verify_collection_stats(self) -> Dict[str, Any]:
        """驗證集合統計資訊"""
        print("\n[1] 集合統計資訊")
        print("-" * 80)

        collection = self.vector_store.collection
        count = collection.count()

        stats = {
            'total_documents': count,
            'status': 'OK' if count > 0 else 'ERROR'
        }

        print(f"總文檔數: {count}")

        # 取樣檢查文檔
        if count > 0:
            sample = collection.get(limit=5)
            print(f"\n樣本文檔（前 5 個）:")

            for i, doc_id in enumerate(sample['ids'][:5], 1):
                idx = sample['ids'].index(doc_id)
                metadata = sample['metadatas'][idx]
                content_preview = sample['documents'][idx][:50].replace('\n', ' ')

                print(f"\n  {i}. ID: {doc_id[:8]}...")
                print(f"     Category: {metadata.get('category', 'N/A')}")
                print(f"     Priority: {metadata.get('priority', 'N/A')}")
                print(f"     Topics: {metadata.get('topics', 'N/A')}")
                print(f"     Code Blocks: {metadata.get('code_block_count', 0)}")
                print(f"     Preview: {content_preview}...")

        return stats

    def _verify_metadata(self) -> Dict[str, Any]:
        """驗證元數據完整性"""
        print(f"\n[2] 元數據驗證")
        print("-" * 80)

        collection = self.vector_store.collection
        all_docs = collection.get()

        metadata_stats = {
            'total_documents': len(all_docs['ids']),
            'with_code_blocks': 0,
            'by_priority': {},
            'by_category': {},
            'missing_fields': {
                'source_file': 0,
                'category': 0,
                'priority': 0,
                'topics': 0,
            }
        }

        for i, doc_id in enumerate(all_docs['ids']):
            metadata = all_docs['metadatas'][i]

            # 檢查程式碼塊
            if int(metadata.get('code_block_count', 0)) > 0:
                metadata_stats['with_code_blocks'] += 1

            # 統計優先級
            priority = metadata.get('priority', 'unknown')
            metadata_stats['by_priority'][priority] = metadata_stats['by_priority'].get(priority, 0) + 1

            # 統計分類
            category = metadata.get('category', 'unknown')
            metadata_stats['by_category'][category] = metadata_stats['by_category'].get(category, 0) + 1

            # 檢查必要欄位
            if not metadata.get('source_file'):
                metadata_stats['missing_fields']['source_file'] += 1
            if not metadata.get('category'):
                metadata_stats['missing_fields']['category'] += 1
            if not metadata.get('priority'):
                metadata_stats['missing_fields']['priority'] += 1

        print(f"包含程式碼塊的文檔: {metadata_stats['with_code_blocks']} / {metadata_stats['total_documents']}")
        print(f"程式碼塊覆蓋率: {metadata_stats['with_code_blocks']/metadata_stats['total_documents']*100:.1f}%")

        print(f"\n優先級分佈:")
        for priority in ['critical', 'high', 'medium', 'low']:
            count = metadata_stats['by_priority'].get(priority, 0)
            pct = count / metadata_stats['total_documents'] * 100 if metadata_stats['total_documents'] > 0 else 0
            print(f"  - {priority}: {count} ({pct:.1f}%)")

        print(f"\n主要分類（前 10 個）:")
        sorted_categories = sorted(metadata_stats['by_category'].items(), key=lambda x: x[1], reverse=True)
        for category, count in sorted_categories[:10]:
            pct = count / metadata_stats['total_documents'] * 100
            print(f"  - {category}: {count} ({pct:.1f}%)")

        print(f"\n遺漏的必要欄位:")
        for field, missing_count in metadata_stats['missing_fields'].items():
            if missing_count > 0:
                print(f"  ⚠️  {field}: {missing_count} 個文檔遺漏")

        return metadata_stats

    def _verify_code_separation(self) -> Dict[str, Any]:
        """驗證程式碼分離功能"""
        print(f"\n[3] 程式碼分離驗證")
        print("-" * 80)

        collection = self.vector_store.collection
        all_docs = collection.get()

        separation_stats = {
            'total_documents': len(all_docs['ids']),
            'documents_with_code_blocks': 0,
            'total_code_blocks': 0,
            'avg_code_blocks_per_doc': 0.0,
            'max_code_blocks_in_doc': 0,
            'samples': []
        }

        for i, doc_id in enumerate(all_docs['ids']):
            metadata = all_docs['metadatas'][i]
            code_block_count = int(metadata.get('code_block_count', 0))

            if code_block_count > 0:
                separation_stats['documents_with_code_blocks'] += 1
                separation_stats['total_code_blocks'] += code_block_count

                if code_block_count > separation_stats['max_code_blocks_in_doc']:
                    separation_stats['max_code_blocks_in_doc'] = code_block_count

                # 收集範例
                if len(separation_stats['samples']) < 3:
                    content_preview = all_docs['documents'][i][:80].replace('\n', ' ')
                    separation_stats['samples'].append({
                        'source_file': metadata.get('source_file'),
                        'code_block_count': code_block_count,
                        'content_preview': content_preview
                    })

        if separation_stats['documents_with_code_blocks'] > 0:
            separation_stats['avg_code_blocks_per_doc'] = (
                separation_stats['total_code_blocks'] /
                separation_stats['documents_with_code_blocks']
            )

        print(f"包含程式碼塊的文檔: {separation_stats['documents_with_code_blocks']} / {separation_stats['total_documents']}")
        print(f"總程式碼塊數: {separation_stats['total_code_blocks']}")
        print(f"平均每文檔程式碼塊數: {separation_stats['avg_code_blocks_per_doc']:.2f}")
        print(f"單一文檔最多程式碼塊數: {separation_stats['max_code_blocks_in_doc']}")

        print(f"\n程式碼分離範例:")
        for sample in separation_stats['samples']:
            print(f"\n  📄 {sample['source_file']}")
            print(f"     代碼塊數: {sample['code_block_count']}")
            print(f"     文本預覽: {sample['content_preview']}...")

        return separation_stats

    def _verify_search(self) -> Dict[str, Any]:
        """驗證搜尋功能"""
        print(f"\n[4] 搜尋功能驗證")
        print("-" * 80)

        test_queries = [
            ("程式碼審查標準", 3),
            ("測試編寫", 3),
            ("Spring Boot 配置", 2),
        ]

        search_stats = {
            'total_queries': len(test_queries),
            'successful_queries': 0,
            'results_with_code_blocks': 0,
            'query_results': []
        }

        for query, top_k in test_queries:
            try:
                results = self.vector_store.search_knowledge(query, top_k=top_k)

                if results:
                    search_stats['successful_queries'] += 1

                    query_result = {
                        'query': query,
                        'result_count': len(results),
                        'results': []
                    }

                    for i, result in enumerate(results[:top_k], 1):
                        result_info = {
                            'rank': i,
                            'topic': result.get('topic', 'N/A'),
                            'similarity': result.get('similarity', 0),
                            'has_code_blocks': 'code_blocks' in result and len(result['code_blocks']) > 0,
                            'code_block_count': len(result.get('code_blocks', []))
                        }
                        query_result['results'].append(result_info)

                        if result_info['has_code_blocks']:
                            search_stats['results_with_code_blocks'] += 1

                    search_stats['query_results'].append(query_result)

            except Exception as e:
                print(f"  ❌ 查詢失敗: {query} - {str(e)}")

        print(f"成功的查詢: {search_stats['successful_queries']} / {search_stats['total_queries']}")
        print(f"包含程式碼塊的結果: {search_stats['results_with_code_blocks']}")

        print(f"\n查詢結果詳情:")
        for qr in search_stats['query_results']:
            print(f"\n  🔍 查詢: {qr['query']}")
            print(f"     結果數: {qr['result_count']}")
            for res in qr['results']:
                similarity_indicator = "⭐" * min(5, int(res['similarity'] * 5))
                print(f"     {res['rank']}. {res['topic']} {similarity_indicator}")
                if res['has_code_blocks']:
                    print(f"        [包含 {res['code_block_count']} 個代碼塊]")

        return search_stats

    def _analyze_performance(self) -> Dict[str, Any]:
        """分析性能指標"""
        print(f"\n[5] 性能分析")
        print("-" * 80)

        collection = self.vector_store.collection
        all_docs = collection.get()

        # 計算文本大小統計
        text_sizes = [len(doc) for doc in all_docs['documents']]
        code_blocks = []

        for metadata in all_docs['metadatas']:
            if 'code_blocks' in metadata:
                try:
                    blocks = json.loads(metadata['code_blocks'])
                    code_blocks.extend(blocks)
                except:
                    pass

        # 取得 embedding 維度（不同於 embeddings 列表）
        embedding_dimension = 768  # google/embeddinggemma-300m 的維度

        performance = {
            'total_documents': len(all_docs['ids']),
            'embedding_dimension': embedding_dimension,
            'avg_text_size': sum(text_sizes) / len(text_sizes) if text_sizes else 0,
            'min_text_size': min(text_sizes) if text_sizes else 0,
            'max_text_size': max(text_sizes) if text_sizes else 0,
            'total_code_blocks': len(code_blocks),
        }

        print(f"Embedding 維度: {performance['embedding_dimension']}")
        print(f"平均文本大小: {performance['avg_text_size']:.0f} 字符")
        print(f"文本大小範圍: {performance['min_text_size']} - {performance['max_text_size']} 字符")
        print(f"總程式碼塊數: {performance['total_code_blocks']}")

        # 估算 embedding 大小節省
        if code_blocks:
            total_code_size = sum(len(block.get('code', '')) for block in code_blocks)
            total_embedding_tokens = sum(text_sizes) + total_code_size
            text_only_tokens = sum(text_sizes)
            savings_pct = (1 - text_only_tokens / total_embedding_tokens) * 100 if total_embedding_tokens > 0 else 0

            print(f"\n程式碼分離節省：")
            print(f"  包含程式碼的 tokens: ~{total_embedding_tokens:,}")
            print(f"  實際 embedding 的 tokens: ~{text_only_tokens:,}")
            print(f"  節省: {savings_pct:.1f}%")

        return performance

    def _generate_recommendations(self, results: Dict) -> List[str]:
        """根據驗證結果生成建議"""
        recommendations = []

        # 檢查文檔數量
        collection_stats = results['sections'].get('collection_stats', {})
        if collection_stats.get('total_documents', 0) == 0:
            recommendations.append("⚠️  沒有文檔被索引，請檢查 ingest 腳本")

        # 檢查元數據
        metadata = results['sections'].get('metadata_validation', {})
        missing_fields = metadata.get('missing_fields', {})
        for field, count in missing_fields.items():
            if count > 0:
                recommendations.append(f"⚠️  {count} 個文檔缺少 '{field}' 欄位")

        # 檢查代碼分離
        separation = results['sections'].get('code_separation', {})
        code_coverage = (
            separation.get('documents_with_code_blocks', 0) /
            separation.get('total_documents', 1) * 100
            if separation.get('total_documents', 0) > 0 else 0
        )
        if code_coverage < 10:
            recommendations.append(f"ℹ️  只有 {code_coverage:.1f}% 的文檔包含程式碼塊，這是正常的如果大多數文檔是純文本")

        # 檢查搜尋
        search = results['sections'].get('search_validation', {})
        if search.get('successful_queries', 0) < search.get('total_queries', 1):
            recommendations.append("⚠️  某些搜尋查詢失敗，請檢查錯誤日誌")

        if not recommendations:
            recommendations.append("✅ 所有驗證通過！系統狀態良好。")

        return recommendations

    def _get_timestamp(self) -> str:
        """取得時間戳"""
        from datetime import datetime
        return datetime.now().isoformat()

    def print_summary(self, results: Dict) -> None:
        """列印摘要"""
        print(f"\n{self.separator_line}")
        print("驗證結果摘要".center(80))
        print(self.separator_line)

        print(f"\n建議:")
        for rec in results['recommendations']:
            print(f"  {rec}")

        print(f"\n{self.separator_line}\n")


def main():
    """主函數"""
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    chroma_db_dir = project_root / 'chroma_db'

    print(f"ChromaDB 目錄: {chroma_db_dir}")

    # 初始化向量存儲
    print("\n初始化向量存儲...")
    vector_store = VectorStoreService(
        db_path=str(chroma_db_dir),
        collection_name="ai_documentation"
    )

    # 運行驗證
    verifier = AIDocsVerifier(vector_store)
    results = verifier.run_verification()
    verifier.print_summary(results)


if __name__ == '__main__':
    main()
