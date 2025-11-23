# Reference 參考文檔

## 📋 概述

本目錄包含開發時需要查閱的參考文檔，不是程式碼範例或生成模板，而是純粹的技術參考資料。

## 📁 文檔清單

### ezddd-import-mapping.md
EZDDD 框架的 import 路徑對照表，包含：
- 正確的 import 語句
- 常見的 import 錯誤
- 使用範例
- 框架更新注意事項

使用時機：
- 遇到 import 錯誤時查閱
- 初次使用 ezddd 框架時參考
- 確認正確的套件路徑

### reactor-pattern-guide.md
Reactor 模式的完整指南，包含：
- Reactor 概念和用途說明
- 實作要點和注意事項
- 多種使用模式（跨聚合一致性、讀模型投影等）
- 完整的測試策略
- 常見錯誤和最佳實踐

使用時機：
- 學習 Reactor 模式時參考
- 實作事件處理器時查閱
- 解決 Reactor 相關問題時使用

### ezspec-test-template.md
ezSpec BDD 測試框架的完整模板和指南，包含：
- 測試類別結構模板
- Rule-based 測試組織方法
- TestContext 模式實作
- Given-When-Then 步驟說明
- 無 Rule 和有 Rule 的完整範例

使用時機：
- 開始寫新的測試類別時參考
- 學習 ezSpec Rule 功能時查閱
- 實作 TestContext 模式時使用

## 🎯 與其他目錄的區別

- **examples/[pattern]/** - 展示設計模式的實作範例
- **generation-templates/** - 用於生成程式碼的完整模板
- **reference/** - 純粹的技術參考文檔，如 API 對照表、框架說明等

## 💡 使用建議

1. 將這些文檔當作「字典」使用 - 需要時查閱
2. 不要試圖記憶所有內容，知道在哪裡找即可
3. 發現錯誤或過時資訊請及時更新

## 🔗 相關資源

- [EZDDD 框架官方文檔](https://github.com/teddy-chen-tw/ezddd)
- [專案程式碼範例](../)
- [生成模板](../generation-templates/)