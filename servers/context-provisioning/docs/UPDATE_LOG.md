# ChromaDB 資料更新記錄

## 2025-11-23 更新：跨平台相容性改進

### 變更內容

✅ **移除絕對路徑欄位** (`full_path`)
- 原因：絕對路徑在不同裝置和 OS 上會不同
- 影響：減少元數據大小，提升跨平台相容性

✅ **統一路徑分隔符**
- 所有 `source_file` 統一使用 `/` (forward slash)
- Windows: `checklists\test.md` → `checklists/test.md`
- Linux/macOS: 保持原樣 `checklists/test.md`

### 驗證結果

- **總 Chunks**: 339
- **使用 `/` 分隔符**: 339 (100%)
- **使用 `\` 分隔符**: 0 (0%)
- **包含 `full_path`**: 0 (0%)

### 跨平台測試

```bash
# ✅ Windows
source_file: "prompts/shared/common-rules.md"

# ✅ Linux
source_file: "prompts/shared/common-rules.md"

# ✅ macOS
source_file: "prompts/shared/common-rules.md"
```

所有路徑格式完全一致，無需任何轉換！

### 遷移到新裝置

現在可以直接複製 `chroma_db/` 目錄到任何裝置：

```bash
# 打包（任何 OS）
tar -czf chroma_db.tar.gz chroma_db/

# 傳輸並解壓（任何 OS）
tar -xzf chroma_db.tar.gz

# 驗證（任何 OS）
python verify_ai_docs.py
```

### 元數據範例

```python
{
    "source_file": "tech-stacks/java-ca-ezddd-spring/coding-standards/aggregate-standards.md",
    "category": "tech-stacks",
    "priority": "low",
    "topics": "aggregate,ddd,testing",
    "chunk_index": 2,
    "section_title": "Soft Delete 規範",
    "file_size": 15234,
    "ingested_at": "2025-11-23T11:12:00Z",
    "doc_type": "ai_documentation"
}
```

**注意**: 無 `full_path` 欄位，所有路徑都是相對於 `.ai/` 目錄的相對路徑。
