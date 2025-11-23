#!/usr/bin/env python3
"""
.ai 目錄文檔智能 Chunking 與 Embedding 腳本

策略說明：
1. 混合式 Chunking：核心文件單獨處理，相關文件按功能域分組
2. 智能切分：按 Markdown 標題（##, ###）進行語義切分
3. 元數據豐富：包含分類、主題、優先級、相關文件等
4. 上下文保留：重疊區域確保語義連貫性
"""

import os
import re
from pathlib import Path
from typing import List, Dict, Optional, Tuple
from datetime import datetime, timezone
import hashlib

import sys
sys.path.insert(0, str(Path(__file__).parent))

from services.vector_store_service import VectorStoreService


class AIDocsChunker:
    """AI 文檔智能分塊器"""

    # Chunking 配置
    SMALL_FILE_THRESHOLD = 800  # 小於 800 tokens 的文件整個作為一個 chunk
    LARGE_FILE_THRESHOLD = 2000  # 大於 2000 tokens 的文件需要切分
    CHUNK_SIZE = 1500  # 目標 chunk 大小（tokens）
    CHUNK_OVERLAP = 200  # 重疊區域（tokens）

    # 文件分類與優先級映射
    CATEGORY_PRIORITY = {
        'core-index': {'priority': 'critical', 'files': ['INDEX.md', 'README.md', 'DIRECTORY-RULES.md', 'SUB-AGENT-SYSTEM.md']},
        'prompts-shared': {'priority': 'critical', 'pattern': 'prompts/shared/'},
        'prompts-subagent': {'priority': 'high', 'pattern': 'prompts/.*-sub-agent-prompt.md'},
        'prompts-review': {'priority': 'high', 'pattern': 'prompts/.*-code-review-prompt.md'},
        'prompts-generation': {'priority': 'high', 'pattern': 'prompts/.*-generation-prompt.md'},
        'coding-standards': {'priority': 'high', 'pattern': 'coding-standards/.*-standards.md'},
        'guides': {'priority': 'medium', 'pattern': 'guides/'},
        'workflows': {'priority': 'medium', 'pattern': 'workflows/'},
        'checklists': {'priority': 'medium', 'pattern': 'checklists/'},
        'examples': {'priority': 'low', 'pattern': 'examples/'},
        'scripts': {'priority': 'low', 'pattern': 'scripts/'},
    }

    # 主題標籤映射（用於語義檢索）
    TOPIC_KEYWORDS = {
        'aggregate': ['aggregate', 'domain model', 'entity', 'value object'],
        'repository': ['repository', 'persistence', 'database'],
        'usecase': ['use case', 'command', 'query', 'cqrs'],
        'testing': ['test', 'junit', 'mockito', 'testcontainers'],
        'reactor': ['reactor', 'event', 'domain event', 'event sourcing'],
        'controller': ['controller', 'api', 'rest', 'endpoint'],
        'spring-boot': ['spring boot', 'spring', 'configuration', 'profile'],
        'ddd': ['ddd', 'domain driven design', 'bounded context'],
        'clean-architecture': ['clean architecture', 'dependency inversion', 'layered'],
    }

    def __init__(self, ai_docs_dir: str, vector_store: VectorStoreService):
        self.ai_docs_dir = Path(ai_docs_dir)
        self.vector_store = vector_store
        self.processed_files = []

    def process_all_docs(self) -> Dict[str, int]:
        """處理所有文檔並返回統計資訊"""
        stats = {
            'total_files': 0,
            'total_chunks': 0,
            'skipped_files': 0,
            'by_category': {},
        }

        print("\n" + "="*80)
        print("開始處理 .ai 目錄文檔".center(80))
        print("="*80 + "\n")

        # 遞迴處理所有 .md 文件
        for md_file in self.ai_docs_dir.rglob('*.md'):
            if self._should_skip_file(md_file):
                stats['skipped_files'] += 1
                continue

            try:
                chunks = self._process_single_file(md_file)
                stats['total_files'] += 1
                stats['total_chunks'] += len(chunks)

                # 更新分類統計
                category = self._get_file_category(md_file)
                stats['by_category'][category] = stats['by_category'].get(category, 0) + len(chunks)

                print(f"[OK] {md_file.relative_to(self.ai_docs_dir)}: {len(chunks)} chunks")

            except Exception as e:
                print(f"[ERROR] 處理失敗 {md_file.name}: {e}")
                import traceback
                traceback.print_exc()
                stats['skipped_files'] += 1

        print("\n" + "="*80)
        print("處理完成統計".center(80))
        print("="*80)
        print(f"總文件數: {stats['total_files']}")
        print(f"總 Chunks: {stats['total_chunks']}")
        print(f"跳過文件: {stats['skipped_files']}")
        print("\n分類統計:")
        for category, count in sorted(stats['by_category'].items()):
            print(f"  - {category}: {count} chunks")
        print("="*80 + "\n")

        return stats

    def _should_skip_file(self, file_path: Path) -> bool:
        """判斷是否應該跳過此文件"""
        # 跳過 generated/ 目錄下的自動生成文件
        if 'generated' in file_path.parts:
            return True
        # 跳過 .sh 腳本文件
        if file_path.suffix == '.sh':
            return True
        return False

    def _process_single_file(self, file_path: Path) -> List[str]:
        """處理單個文件，返回生成的 chunk IDs"""
        # 讀取文件內容
        content = file_path.read_text(encoding='utf-8')

        # 估算 tokens 數量（粗略估算：中文 1字=1token, 英文 1詞=1token）
        estimated_tokens = self._estimate_tokens(content)

        # 獲取文件元數據
        metadata = self._build_metadata(file_path, content)

        # 根據文件大小決定分塊策略
        if estimated_tokens < self.SMALL_FILE_THRESHOLD:
            # 小文件：整個作為一個 chunk
            chunks = [self._create_chunk(content, metadata, chunk_index=0)]
        elif estimated_tokens < self.LARGE_FILE_THRESHOLD:
            # 中等文件：按 H2 標題切分
            chunks = self._split_by_headers(content, metadata, level=2)
        else:
            # 大文件：按 H2 和 H3 標題切分
            chunks = self._split_by_headers(content, metadata, level=3)

        # 存入向量資料庫
        chunk_ids = []
        for chunk_data in chunks:
            # 使用 VectorStoreService 的方法
            chunk_id = self._add_chunk_to_store(chunk_data)
            chunk_ids.append(chunk_id)

        return chunk_ids

    def _add_chunk_to_store(self, chunk_data: Dict) -> str:
        """將單個 chunk 加入向量存儲"""
        import uuid

        doc_id = str(uuid.uuid4())
        embedding = self.vector_store.model.encode(chunk_data['content']).tolist()

        # 合併元數據和 topic
        full_metadata = chunk_data['metadata'].copy()
        full_metadata['topic'] = chunk_data['topic']
        full_metadata['timestamp'] = datetime.now(timezone.utc).isoformat()

        self.vector_store.collection.add(
            ids=[doc_id],
            embeddings=[embedding],
            documents=[chunk_data['content']],
            metadatas=[full_metadata]
        )

        return doc_id

    def _estimate_tokens(self, text: str) -> int:
        """粗略估算文本的 token 數量"""
        # 簡化估算：中文字符 + 英文單詞
        chinese_chars = len(re.findall(r'[\u4e00-\u9fff]', text))
        english_words = len(re.findall(r'\b[a-zA-Z]+\b', text))
        return chinese_chars + english_words

    def _build_metadata(self, file_path: Path, content: str) -> Dict:
        """構建文件元數據"""
        relative_path = file_path.relative_to(self.ai_docs_dir)
        category = self._get_file_category(file_path)
        priority = self._get_file_priority(file_path)
        topics = self._extract_topics(content)

        return {
            'source_file': str(relative_path).replace('\\', '/'),  # 統一使用 / 作為路徑分隔符，跨平台相容
            'category': category,
            'priority': priority,
            'topics': ','.join(topics) if topics else '',  # ChromaDB 不支援 list，轉為逗號分隔字串
            'file_size': file_path.stat().st_size,
            'ingested_at': datetime.now(timezone.utc).isoformat(),
            'doc_type': 'ai_documentation',
        }

    def _get_file_category(self, file_path: Path) -> str:
        """獲取文件分類"""
        relative_path = str(file_path.relative_to(self.ai_docs_dir))

        # 檢查是否為核心索引文件
        if file_path.name in self.CATEGORY_PRIORITY['core-index']['files']:
            return 'core-index'

        # 按路徑模式匹配
        for category, config in self.CATEGORY_PRIORITY.items():
            if 'pattern' in config:
                if re.search(config['pattern'], relative_path):
                    return category

        # 默認分類
        parts = file_path.relative_to(self.ai_docs_dir).parts
        return parts[0] if len(parts) > 1 else 'other'

    def _get_file_priority(self, file_path: Path) -> str:
        """獲取文件優先級"""
        category = self._get_file_category(file_path)

        for cat, config in self.CATEGORY_PRIORITY.items():
            if cat == category:
                return config.get('priority', 'low')

        return 'low'

    def _extract_topics(self, content: str) -> List[str]:
        """從內容中提取主題標籤"""
        content_lower = content.lower()
        topics = []

        for topic, keywords in self.TOPIC_KEYWORDS.items():
            for keyword in keywords:
                if keyword in content_lower:
                    topics.append(topic)
                    break

        return list(set(topics))  # 去重

    def _split_by_headers(self, content: str, base_metadata: Dict, level: int = 2) -> List[Dict]:
        """按 Markdown 標題切分內容"""
        chunks = []

        # 根據 level 決定切分模式
        if level == 2:
            # 只按 H2 切分
            pattern = r'\n##\s+(.+?)(?=\n##\s+|\Z)'
        else:
            # 按 H2 和 H3 切分
            pattern = r'\n###?\s+(.+?)(?=\n###?\s+|\Z)'

        sections = re.split(r'(\n##\s+)', content)

        # 處理文件開頭（沒有標題的部分）
        if sections[0].strip():
            chunks.append(self._create_chunk(
                sections[0].strip(),
                base_metadata,
                chunk_index=0,
                section_title="文件開頭"
            ))

        # 處理各個章節
        current_chunk = ""
        current_title = ""
        chunk_index = 1

        for i in range(1, len(sections), 2):
            if i + 1 < len(sections):
                header = sections[i] + sections[i + 1].split('\n')[0]
                section_content = '\n'.join(sections[i + 1].split('\n')[1:])

                # 提取標題
                title_match = re.search(r'##\s+(.+)', header)
                section_title = title_match.group(1) if title_match else f"Section {chunk_index}"

                # 如果當前 chunk 太大，先保存
                if self._estimate_tokens(current_chunk + section_content) > self.CHUNK_SIZE and current_chunk:
                    chunks.append(self._create_chunk(
                        current_chunk,
                        base_metadata,
                        chunk_index=chunk_index,
                        section_title=current_title
                    ))
                    chunk_index += 1
                    current_chunk = section_content
                    current_title = section_title
                else:
                    current_chunk += f"\n{header}\n{section_content}"
                    current_title = section_title

        # 保存最後一個 chunk
        if current_chunk.strip():
            chunks.append(self._create_chunk(
                current_chunk,
                base_metadata,
                chunk_index=chunk_index,
                section_title=current_title
            ))

        # 如果沒有切分出任何 chunk，整個文件作為一個 chunk
        if not chunks:
            chunks.append(self._create_chunk(content, base_metadata, chunk_index=0))

        return chunks

    def _create_chunk(self, content: str, base_metadata: Dict,
                     chunk_index: int = 0, section_title: Optional[str] = None) -> Dict:
        """創建一個 chunk 數據結構"""
        metadata = base_metadata.copy()
        metadata['chunk_index'] = chunk_index
        metadata['chunk_id'] = self._generate_chunk_id(base_metadata['source_file'], chunk_index)

        if section_title:
            metadata['section_title'] = section_title

        # 生成簡短摘要（取前 200 字符）
        summary = content[:200].replace('\n', ' ').strip()
        if len(content) > 200:
            summary += "..."
        metadata['summary'] = summary

        # 主題使用 category 作為主要分類
        topic = f"{metadata['category']}"
        if section_title:
            topic += f" - {section_title}"

        return {
            'content': content.strip(),
            'metadata': metadata,
            'topic': topic,
        }

    def _generate_chunk_id(self, source_file: str, chunk_index: int) -> str:
        """生成唯一的 chunk ID"""
        unique_string = f"{source_file}#{chunk_index}"
        return hashlib.md5(unique_string.encode()).hexdigest()[:16]


def main():
    """主函數"""
    import sys

    # 設定路徑
    script_dir = Path(__file__).parent
    ai_docs_dir = script_dir / '.ai'
    chroma_db_dir = script_dir / 'chroma_db'

    print(f"AI 文檔目錄: {ai_docs_dir}")
    print(f"ChromaDB 目錄: {chroma_db_dir}")

    # 檢查 .ai 目錄是否存在
    if not ai_docs_dir.exists():
        print(f"[ERROR] .ai 目錄不存在: {ai_docs_dir}")
        sys.exit(1)

    # 初始化向量存儲
    print("\n初始化向量存儲...")
    vector_store = VectorStoreService(
        db_path=str(chroma_db_dir),
        collection_name="ai_documentation"
    )

    # 創建 chunker 並處理所有文檔
    chunker = AIDocsChunker(ai_docs_dir=str(ai_docs_dir), vector_store=vector_store)
    stats = chunker.process_all_docs()

    print("\n[SUCCESS] 所有文檔已成功處理並存入 ChromaDB!")
    print(f"[INFO] ChromaDB 資料目錄: {chroma_db_dir}")
    print("\n[TIP] 提示: 您可以將 chroma_db/ 目錄複製到其他裝置使用")

    # 測試檢索
    print("\n" + "="*80)
    print("測試檢索功能".center(80))
    print("="*80)

    test_queries = [
        "如何實作 Aggregate?",
        "測試要怎麼寫?",
        "Sub-agent 系統架構",
    ]

    for query in test_queries:
        print(f"\n[QUERY] 查詢: {query}")
        results = vector_store.search_knowledge(query, top_k=3)
        for i, result in enumerate(results, 1):
            print(f"  {i}. [{result['topic']}] (相似度: {result['similarity']:.3f})")
            # 移除 emoji 和特殊字符以避免編碼問題
            content_preview = result['content'][:100].encode('ascii', 'ignore').decode('ascii')
            print(f"     {content_preview}...")

    print("\n" + "="*80)


if __name__ == '__main__':
    main()
