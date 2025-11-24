#!/usr/bin/env python3
"""檢查 ChromaDB 中的路徑格式"""

import sys
from pathlib import Path
script_dir = Path(__file__).parent
project_root = script_dir.parent
sys.path.insert(0, str(project_root))

from services.vector_store_service import VectorStoreService

vs = VectorStoreService(db_path=str(project_root / 'chroma_db'), collection_name='ai_documentation')
print(f'Total Chunks: {vs.collection.count()}')

# 獲取所有資料
results = vs.collection.get()

print('\n=== Path Format Check ===')
print('\nSample paths (first 10):')
for i, meta in enumerate(results['metadatas'][:10], 1):
    path = meta.get('source_file', 'N/A')
    has_backslash = '\\' in path
    status = '[FAIL]' if has_backslash else '[OK]'
    print(f'  {status} {path}')

# 統計
total = len(results['metadatas'])
has_backslash_count = sum(1 for m in results['metadatas'] if '\\' in m.get('source_file', ''))
has_full_path_count = sum(1 for m in results['metadatas'] if 'full_path' in m)

print(f'\n=== Summary ===')
print(f'Total chunks: {total}')
print(f'With backslash (\\): {has_backslash_count}')
print(f'With forward slash (/): {total - has_backslash_count}')
print(f'Has full_path field: {has_full_path_count}')

if has_backslash_count == 0 and has_full_path_count == 0:
    print('\n✓ All checks PASSED!')
else:
    print('\n✗ Some checks FAILED!')
