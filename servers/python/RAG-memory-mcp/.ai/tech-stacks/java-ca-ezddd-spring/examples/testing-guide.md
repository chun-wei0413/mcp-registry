# 測試指南總覽

## 📚 測試文件架構

我們的測試文件分為兩個層次：

### 1. 🎯 實務導向（Production-Ready）
**檔案**: [use-case-test-example.md](./use-case-test-example.md)
- 現代化 Spring Boot 測試模式
- Profile-based testing (test-inmemory / test-outbox)
- 實際專案使用的模式
- 完整的 Command/Query Use Case 範例

**何時使用**：
- ✅ 開發新功能時
- ✅ 撰寫 Use Case 測試
- ✅ 需要 Spring Boot 整合測試

### 2. 📖 概念導向（Conceptual）
**檔案**: [test-example.md](./test-example.md)
- ezSpec BDD 框架基礎概念
- Domain Entity 單元測試
- Test Data Builder 模式
- 手動管理 Repository 和 MessageBus

**何時使用**：
- ✅ 學習 ezSpec 框架
- ✅ 測試 Domain Entity
- ✅ 非 Spring Boot 專案
- ✅ 需要完全控制測試環境

## 🤔 該保留兩份文件嗎？

### 保留的理由：

1. **不同用途**
   - `use-case-test-example.md`: 實務參考手冊
   - `test-example.md`: 學習教材

2. **漸進式學習**
   - 新人可以從基礎概念開始
   - 理解原理後再學習進階實作

3. **完整性**
   - Domain Entity 測試在舊文件中有更詳細說明
   - Test Data Builder 模式仍然有用

4. **向後相容**
   - 某些舊測試可能還在使用手動管理方式
   - 提供遷移參考

## 📋 建議的文件組織

```
.ai/tech-stacks/java-ca-ezddd-spring/examples/
├── README.md                    # 導覽首頁（建議新增）
├── use-case-test-example.md    # 主要參考 ⭐
├── test-example.md              # 基礎概念
├── outbox/                      # Outbox 測試範例
└── inquiry-archive/             # Inquiry/Archive 範例
```

## 🎯 結論

**建議保留兩份文件**，但要：
1. 清楚標示各自定位
2. 在舊文件加入導引到新文件
3. 建立 README 作為導覽首頁
4. 定期檢視是否需要合併或重構