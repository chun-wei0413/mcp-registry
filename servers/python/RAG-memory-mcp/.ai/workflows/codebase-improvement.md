# Workflow: 代碼庫改進

## 概述

此工作流程指導 AI 和人類協作改進現有代碼庫，包括重構、性能優化、錯誤修復和技術債務清理。
本流程與 **Sub-agent System** 整合，在關鍵階段使用 Code Review Agent 進行品質審查。

## 🤖 Sub-agent 整合
- **階段 1**：使用 Code Review Agent 進行全面分析
- **階段 3**：使用 Test Generation Agent 補充測試
- **階段 5**：使用 Code Review Agent 驗證改進效果

## 如何啟動此 Workflow

使用以下 AI 指令啟動：

```
"使用 codebase-improvement workflow 分析並改進 [模組/功能]"
```

具體範例：

```
"請使用 codebase-improvement workflow 分析 User 模組
重點關注：
- 代碼複雜度
- 測試覆蓋率
- 性能瓶頸"
```

## 目標

1. 提高代碼質量和可維護性
2. 優化性能瓶頸
3. 減少技術債務
4. 增強測試覆蓋率

## 工作流程

### 階段 1：代碼分析

#### 🤖 使用 Code Review Sub-agent

**啟動方式**：
```
請啟動 Code Review Sub-agent 分析 [模組名稱] 的程式碼品質
```

**Sub-agent 執行**：
1. 載入 CODE-REVIEW-CHECKLIST.md
2. 執行全面的靜態分析
3. 識別代碼異味（Code Smells）
4. 檢查規範遵守情況
5. 分析依賴健康度
6. 產生詳細的分析報告

**傳統 AI 行動**（若不使用 sub-agent）：
1. 執行靜態代碼分析
2. 識別代碼異味（Code Smells）
3. 檢查測試覆蓋率
4. 分析依賴關係

**分析 Prompt 模板**：
```yaml
分析參數：
  focus_areas:
    - architecture      # 架構模式和結構
    - code_quality     # 代碼異味和複雜度
    - test_coverage    # 測試完整性
    - dependencies     # 依賴健康度
  exclude_patterns:
    - node_modules/
    - build/
    - .git/
```

**人類輸入**：
- 指出已知的問題區域
- 設定改進優先級
- 解釋歷史決策背景

**產出**：
- 代碼質量報告
- 技術債務清單
- 改進優先級列表

### 階段 2：重構計劃

**AI 行動**：
1. 基於 `.ai/tech-stacks/` 提出重構建議
2. 評估改動風險
3. 制定分階段計劃
4. 準備重構清單

**人類輸入**：
- 批准重構範圍
- 設定時間限制
- 確認業務優先級

**產出**：
- 重構計劃文檔
- 風險評估矩陣
- 實施時間表

### 階段 3：測試強化

#### 🤖 使用 Test Generation Sub-agent

**啟動方式**：
```
請啟動 Test Generation Sub-agent 為缺少測試的區域補充測試
```

**Sub-agent 執行**：
1. 分析現有測試覆蓋率
2. 識別未測試的關鍵邏輯
3. 使用 ezSpec 產生 BDD 測試
4. 產生單元測試和整合測試
5. 執行測試確保全部通過

**傳統 AI 行動**（若不使用 sub-agent）：
1. 識別缺少測試的區域
2. 生成測試案例
3. 實施測試
4. 確保測試通過

**測試策略**（基於 `.ai/tech-stacks/java-ca-ezddd-spring/codegen/ezspec/`）：

🚨 **Use Case 測試必須使用 ezSpec BDD 風格**

```java
// 1. 先寫失敗的測試（使用 ezSpec）
@EzScenario
public void should_handle_edge_case() {
    feature.newScenario("Handle edge case properly")
        .Given("an edge case input", env -> {
            var input = createEdgeCaseInput();
            env.put("input", input);
        })
        .When("the service processes the input", env -> {
            var input = env.get("input", InputType.class);
            env.put("exception", catchThrowable(() -> 
                service.process(input)
            ));
        })
        .Then("it should throw the expected exception", env -> {
            var exception = env.get("exception", Throwable.class);
            assertThat(exception)
                .isInstanceOf(ExpectedException.class)
                .hasMessageContaining("expected error");
        })
        .Execute();
}

// 2. 實現功能使測試通過
// 3. 重構並保持測試綠燈
```

**人類輸入**：
- 確認測試場景完整性
- 提供邊緣案例
- 審查測試質量

**產出**：
- 新增測試案例
- 測試覆蓋率報告
- 測試文檔

### 階段 4：性能優化

**AI 行動**：
1. 識別性能瓶頸
2. 提出優化方案
3. 實施優化
4. 測量改進效果

**優化重點**（基於專案經驗）：
- JPA 查詢優化（使用 Projection）
- 避免 N+1 查詢問題
- 合理使用快取
- 異步處理長時間操作

**人類輸入**：
- 提供性能目標
- 確認可接受的權衡
- 批准架構變更

**產出**：
- 性能分析報告
- 優化實施記錄
- 性能測試結果

### 階段 5：代碼清理

#### 🤖 使用 Code Review Sub-agent 驗證

**啟動方式**：
```
請啟動 Code Review Sub-agent 審查所有改進結果
```

**Sub-agent 執行**：
1. 驗證重構後的程式碼品質
2. 確認規範遵守情況
3. 檢查潛在的新問題
4. 提供最終改進報告

**傳統 AI 行動**（若不使用 sub-agent）：
1. 移除無用代碼
2. 更新過時的依賴
3. 統一編碼風格
4. 改進命名和註釋

**清理原則**：
- 遵循 `.ai/tech-stacks/java-ca-ezddd-spring/anti-patterns.md` 避免反模式
- 保持向後兼容性
- 小步快跑，頻繁提交

**人類輸入**：
- 確認可以安全刪除的代碼
- 批准依賴更新
- 審查重要變更

**產出**：
- 清理記錄
- 更新的依賴清單
- 代碼風格指南

## 執行策略

### 基於參與模式

**High Engagement**：
- 每個改動都需討論
- 共同編程模式
- 詳細的代碼審查

**Medium Engagement**：
- AI 提出改進方案
- 人類審批後執行
- 關鍵決策點暫停

**Auto-pilot**：
- AI 自主執行改進
- 記錄所有變更
- 批量審查結果

### 風險管理

1. **漸進式改進**：
   - 小批次變更
   - 頻繁測試
   - 可回滾策略

2. **安全網**：
   - 完整的測試套件
   - 版本控制
   - 代碼審查

3. **溝通**：
   - 記錄變更原因
   - 更新相關文檔
   - 通知團隊成員

## 具體技術指南

### Clean Architecture 改進
參考 `.ai/tech-stacks/`：
- 確保層次職責清晰
- 依賴方向正確
- 適當使用介面

### DDD 模式應用
基於 `.ai/tech-stacks/java-ca-ezddd-spring/examples/`：
- Aggregate 邊界合理
- Value Object 不可變
- Domain Event 完整

### Spring Boot 最佳實踐
- 構造函數注入
- 適當的異常處理
- RESTful API 設計

## 成功標準

- [ ] 代碼質量指標提升 20%
- [ ] 測試覆蓋率達到 80%
- [ ] 性能提升可測量
- [ ] 技術債務明顯減少
- [ ] **Sub-agent 審查通過**

## 時間估算

- 小型改進：4-8 小時
- 中型重構：2-3 天
- 大型改造：1-2 週

## 注意事項

1. **保持業務運行**：改進不應影響功能
2. **溝通優先**：重大變更需團隊共識
3. **記錄決策**：使用 `AI-ASSUMPTIONS.md`
4. **持續改進**：不追求完美，追求進步

## 相關資源

### Sub-agent Prompts
- `.ai/prompts/code-review-prompt.md` - Code Review Agent
- `.ai/prompts/test-generation-prompt.md` - Test Generation Agent
- `.ai/prompts/code-generation-prompt.md` - Code Generation Agent

### 技術資源
- `.ai/SUB-AGENT-SYSTEM.md` - Sub-agent 系統說明
- `.ai/tech-stacks/` - 編碼標準
- `.ai/tech-stacks/java-ca-ezddd-spring/anti-patterns.md` - 避免的模式
- `.ai/tech-stacks/java-ca-ezddd-spring/best-practices.md` - 性能指南
- `.ai/workflows/` - 從錯誤中學習