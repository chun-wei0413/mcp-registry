# Sub-agent System Architecture

本文件定義了 Sub-agent 系統架構，說明如何使用專門的 sub-agents 來提高程式碼生成的品質和規範遵守率。

## 🎯 系統目標

透過專業分工，讓每個 sub-agent 專注於特定領域，減少錯誤並提高效率：

1. **降低認知負擔**：每個 agent 只需理解自己領域的規範
2. **提高規範遵守率**：專注的 context 讓規則更容易被遵守
3. **減少錯誤**：避免不同領域規則的互相干擾
4. **提升品質**：專業化帶來更好的輸出品質

## 🤖 Sub-agent 角色定義

### Use Case Sub-agents (區分 Command 與 Query) 🆕

#### 1a. Command Code Generation Agent
**職責**：產生 Command use case 的 production code
**專注領域**：
- Aggregate 狀態變更邏輯
- Domain Events 產生
- Repository 儲存操作
- 事務管理與一致性
- Business rule enforcement
- 🔥 **架構感知**: 根據 `.dev/project-config.json` 決定 outbox/inmemory/eventsourcing 模式
- 🔥 **智慧產生**: Outbox 模式自動產生 Data/Mapper，其他模式不產生

**使用 Prompt**：`.ai/prompts/command-sub-agent-prompt.md`

**適用於**：
- Create/Update/Delete 操作
- 狀態轉換 (如 EstimateTask, SelectPbi)
- 任何修改 domain state 的操作

#### 1b. Query Code Generation Agent
**職責**：產生 Query use case 的 production code
**專注領域**：
- Projection 設計與實作
- DTO mapping 策略
- 查詢優化
- Read model 設計
- 無副作用保證

**使用 Prompt**：`.ai/prompts/query-sub-agent-prompt.md`

**適用於**：
- Get/List/Search 操作
- 報表與統計查詢
- 任何唯讀操作

#### 2. Test Generation Agent
**職責**：產生測試程式碼
**專注領域**：
- ezSpec BDD 測試
- Domain Object 單元測試
- 測試資料準備

**使用 Prompt**：`.ai/prompts/test-generation-prompt.md`

#### 3. Code Review Agent
**職責**：審查程式碼品質
**專注領域**：
- 規範遵守檢查
- 品質問題識別
- 改進建議

**使用 Prompt**：`.ai/prompts/code-review-prompt.md`

### Testing Enhancement Sub-agents 🆕

#### 4. Mutation Testing Enhancement Agent
**職責**：使用 PIT + uContract + assertion-free tests 提升 mutation coverage
**專注領域**：
- PIT mutation testing 配置與執行
- Design by Contract (uContract) 漸進式實施
- Assertion-free test 設計
- Mutation coverage 分析與改善
- 既有測試相容性維護

**使用 Prompt**：`.ai/prompts/mutation-testing-sub-agent-prompt.md`

**適用於**：
- Entity layer mutation coverage 提升
- Aggregate 行為規格強化
- 測試有效性驗證
- Contract-based testing 實施

**成效指標**：
- Mutation Coverage > 80%
- Test Strength > 85%
- 既有測試通過率 100%

**自動化工具**：`.ai/scripts/check-mutation-coverage.sh`

**工作流程**：`.ai/workflows/mutation-testing-workflow.md`

### Controller Sub-agents

#### 5. Controller Code Generation Agent
**職責**：產生 REST Controller 程式碼
**專注領域**：
- REST API endpoints
- Request/Response mapping
- HTTP status codes handling
- Spring Boot annotations
- Error responses (ApiError)

**使用 Prompt**：`.ai/prompts/controller-code-generation-prompt.md`

#### 6. Controller Test Generation Agent
**職責**：產生 Controller 測試程式碼並確保通過
**專注領域**：
- MockMvc tests (必須通過)
- REST Assured integration tests (必須通過)
- HTTP behavior verification
- JSON response validation
- 測試執行驗證

**使用 Prompt**：`.ai/prompts/controller-test-generation-prompt.md`

#### 7. Controller Code Review Agent
**職責**：審查 Controller 程式碼品質
**專注領域**：
- REST compliance
- Spring Boot standards
- Security checks
- API design patterns

**使用 Prompt**：`.ai/prompts/controller-code-review-prompt.md`

### Aggregate Sub-agents 🆕

#### 8. Aggregate Code Generation Agent
**職責**：產生 DDD Aggregate 的領域模型程式碼
**專注領域**：
- 狀態機設計與實作
- Domain Events 產生與處理
- 不變式 (invariants) 維護
- Event Sourcing 實作
- 業務規則封裝
- 邊界條件處理

**使用 Prompt**：`.ai/prompts/aggregate-sub-agent-prompt.md`

**適用於**：
- Aggregate 狀態機實作
- 複雜業務邏輯
- Event Sourcing handlers
- Domain 層核心邏輯

#### 9. Aggregate Test Generation Agent  
**職責**：產生 Aggregate 測試程式碼
**專注領域**：
- 狀態轉換測試
- 邊界條件測試
- 不變式驗證
- Event 序列測試
- Given-When-Then 測試

**使用 Prompt**：`.ai/prompts/aggregate-test-generation-prompt.md`

#### 10. Aggregate Code Review Agent
**職責**：審查 Aggregate 實作品質
**專注領域**：
- DDD 原則遵循
- Event Sourcing 正確性
- 狀態機完整性
- 不變式維護
- 業務規則正確性

**使用 Prompt**：`.ai/prompts/aggregate-code-review-prompt.md`

### Reactor Sub-agents 🆕

#### 11. Reactor Code Generation Agent
**職責**：產生 Reactor 實作程式碼
**專注領域**：
- execute(Object event) 方法實作
- Event type checking (instanceof pattern)
- 跨聚合最終一致性邏輯
- MessageBus 整合
- 錯誤隔離與 graceful degradation

**使用 Prompt**：`.ai/prompts/reactor-sub-agent-prompt.md`

**適用於**：
- 跨聚合事件處理
- Read model projection (CQRS)
- Event forwarding 到外部系統
- Domain event 轉換

#### 12. Reactor Test Generation Agent
**職責**：產生 Reactor 測試程式碼
**專注領域**：
- BlockingMessageBus 設置
- Event posting 與驗證
- 多事件類型測試
- 錯誤處理測試
- 隔離性測試

**使用 Prompt**：`.ai/prompts/reactor-test-generation-prompt.md`

#### 13. Reactor Code Review Agent
**職責**：審查 Reactor 實作品質
**專注領域**：
- execute(Object event) 方法正確性
- Event handling 邏輯
- 跨聚合邊界維護
- 錯誤處理完整性
- MessageBus 整合正確性

**使用 Prompt**：`.ai/prompts/reactor-code-review-prompt.md`

### Infrastructure Sub-agents 🆕

#### 14. Profile Configuration Agent 🔥
**職責**：配置 Spring Profiles 以支援 InMemory 和 Outbox 模式
**專注領域**：
- Spring Profile 隔離策略
- DataSource 自動配置排除
- 條件化 Bean 載入 (@Profile, @ConditionalOn*)
- Properties 檔案配置
- Configuration 類別組織
- Profile 衝突預防

**使用 Prompt**：`.ai/prompts/profile-config-sub-agent-prompt.md`

**適用於**：
- 新專案初始化時的 Profile 設定
- InMemory 到 Outbox 的切換配置
- 解決 Bean not found 錯誤
- 解決 DataSource 配置錯誤
- Profile 隔離和測試配置

**成功標準**：
- Spring Boot 可在所有 Profile 下正常啟動
- 無 Repository Bean 缺失錯誤
- 無 DataSource 配置錯誤
- Profile 之間完全隔離

#### 15. Outbox Pattern Implementation Agent
**職責**：產生 Outbox Pattern 實作程式碼
**專注領域**：
- OutboxData 介面實作 (with @Transient annotations)
- OutboxMapper 內部類別設計
- OrmClient 介面定義
- OutboxRepository 配置
- Jakarta persistence 註解使用
- 版本控制與樂觀鎖

**使用 Prompt**：`.ai/prompts/outbox-sub-agent-prompt.md`

**適用於**：
- Aggregate 的 Outbox Repository 實作
- Event Store 整合
- 可靠的事件發布機制
- 交易一致性保證

## 📋 標準工作流程

### Phase 1: 需求分析
```mermaid
Main Agent
├── 讀取 spec 檔案
├── 解析需求
└── 識別需要的元件
```

### Phase 2: 程式碼生成
```mermaid
Main Agent
└── Code Generation Sub-agent
    ├── Context Loading
    │   ├── spec 內容
    │   ├── coding-standards.md
    │   ├── 相關範例程式碼
    │   └── domain-event-checklist.md
    ├── Code Generation
    │   ├── Domain Layer
    │   ├── Use Case Layer
    │   └── Adapter Layer
    └── Output: Production Code
```

### Phase 3: 測試生成
```mermaid
Main Agent
└── Test Generation Sub-agent
    ├── Context Loading
    │   ├── Production Code
    │   ├── ezspec-test-template.md
    │   ├── test-data-preparation-guide.md
    │   └── spec 的 testScenarios
    ├── Test Generation
    │   ├── Use Case Tests (ezSpec)
    │   ├── Domain Tests
    │   └── TestContext Setup
    └── Output: Test Code
```

### Phase 4: 程式碼審查
```mermaid
Main Agent
└── Code Review Sub-agent
    ├── Context Loading
    │   ├── Generated Code
    │   ├── CODE-REVIEW-CHECKLIST.md
    │   └── VALIDATION-CHECKLIST.md
    ├── Review Process
    │   ├── Structure Check
    │   ├── Compliance Check
    │   └── Quality Check
    └── Output: Review Report
```

### Phase 5: 修正與完成
```mermaid
Main Agent
├── 分析 Review Report
├── 執行必要修正
├── 驗證編譯和測試
├── 更新 task 檔案 (強制)
│   ├── status: "todo" → "done"
│   ├── 新增 results 記錄
│   └── commit 更新
└── 確認完成
```

## 💻 實際執行範例

### 範例 1: 實作 Command Use Case

```python
# Step 1: Main Agent 解析 spec
spec = read_file(".dev/specs/product/usecase/create-product.json")
components = parse_spec(spec)

# Step 2: 啟動 Command Code Generation Sub-agent
code_agent_task = {
    "description": "Generate CreateProduct command use case",
    "prompt": f"""
    請根據以下 spec 產生 Command use case 程式碼。
    
    必須遵守的規範：
    {read_file('.ai/prompts/command-sub-agent-prompt.md')}
    
    Spec 內容：
    {spec}
    
    請產生所有必要的程式碼，包括：
    - Use Case Interface (with Inner Classes)
    - Service Implementation
    - Domain Event handling
    - Repository interaction
    """,
    "subagent_type": "general-purpose"
}

# Step 3: 啟動 Test Generation Sub-agent
test_agent_task = {
    "description": "Generate tests for CreateProduct",
    "prompt": f"""
    請為以下程式碼產生測試。
    
    必須遵守的規範：
    {read_file('.ai/prompts/test-generation-prompt.md')}
    
    Production Code：
    {generated_code}
    
    Test Scenarios from Spec：
    {spec['testScenarios']}
    """,
    "subagent_type": "general-purpose"
}

# Step 4: 啟動 Code Review Sub-agent
review_agent_task = {
    "description": "Review generated code",
    "prompt": f"""
    請審查以下程式碼。
    
    審查標準：
    {read_file('.ai/prompts/code-review-prompt.md')}
    
    程式碼：
    {all_generated_code}
    
    請提供詳細的審查報告。
    """,
    "subagent_type": "general-purpose"
}
```

### 範例 2: 實作 Query Use Case 🆕

```python
# Step 1: Main Agent 解析 spec
spec = read_file(".dev/specs/product/usecase/get-product.json")
components = parse_spec(spec)

# Step 2: 啟動 Query Code Generation Sub-agent
code_agent_task = {
    "description": "Generate GetProduct query use case",
    "prompt": f"""
    請根據以下 spec 產生 Query use case 程式碼。
    
    必須遵守的規範：
    {read_file('.ai/prompts/query-sub-agent-prompt.md')}
    
    Spec 內容：
    {spec}
    
    請產生所有必要的程式碼，包括：
    - Use Case Interface (with Inner Classes)
    - Service Implementation
    - DTOs and Mappers (static utilities)
    - Projection interface and implementation
    """,
    "subagent_type": "general-purpose"
}

# Step 3: 啟動 Test Generation Sub-agent
# (相同流程，略)

# Step 4: 啟動 Code Review Sub-agent
# (相同流程，略)
```

### 範例 3: 實作 REST Controller

```python
# Step 1: Main Agent 解析 controller spec
controller_spec = read_file(".dev/specs/product/adapter/get-product-controller.json")

# Step 2: 啟動 Controller Code Generation Sub-agent
controller_code_task = {
    "description": "Generate GetProductController",
    "prompt": f"""
    請根據以下 spec 產生 REST Controller。
    
    必須遵守的規範：
    {read_file('.ai/prompts/controller-code-generation-prompt.md')}
    
    Controller Spec：
    {controller_spec}
    
    請產生：
    - Controller class with proper annotations
    - ApiError class
    - All necessary error handling
    """,
    "subagent_type": "general-purpose"
}

# Step 3: 啟動 Controller Test Generation Sub-agent
controller_test_task = {
    "description": "Generate Controller tests",
    "prompt": f"""
    請為 Controller 產生測試。
    
    必須遵守的規範：
    {read_file('.ai/prompts/controller-test-generation-prompt.md')}
    
    Controller Code：
    {generated_controller}
    
    🔴 重要：必須同時產生兩種測試且都要通過：
    1. MockMvc 測試檔案 (GetProductControllerTest.java) - 必須全部通過
    2. REST Assured 測試檔案 (GetProductControllerIntegrationTest.java) - 必須全部通過
    
    兩種測試都要涵蓋所有測試場景。
    執行測試確保全部通過後才算完成。
    """,
    "subagent_type": "general-purpose"
}

# Step 4: 啟動 Controller Code Review Sub-agent
controller_review_task = {
    "description": "Review Controller implementation",
    "prompt": f"""
    請審查 Controller 實作。
    
    審查標準：
    {read_file('.ai/prompts/controller-code-review-prompt.md')}
    
    Controller 程式碼：
    {controller_code}
    
    MockMvc 測試程式碼：
    {mockmvc_test_code}
    
    REST Assured 測試程式碼：
    {restassured_test_code}
    
    🔴 必須確認：
    1. 兩種測試檔案都存在
    2. 兩種測試涵蓋相同場景
    3. MockMvc 測試全部通過
    4. REST Assured 測試全部通過
    5. REST API 合規性
    
    請提供完整審查報告。
    """,
    "subagent_type": "general-purpose"
}
```

### 範例 4: 實作 Reactor 🆕

```python
# Step 1: Main Agent 解析 reactor spec
reactor_spec = read_file(".dev/specs/sprint/reactor/notify-sprint-to-select-backlog-item.json")

# Step 2: 啟動 Reactor Code Generation Sub-agent
reactor_code_task = {
    "description": "Generate NotifySprintToSelectBacklogItem reactor",
    "prompt": f"""
    請根據以下 spec 產生 Reactor 實作。
    
    必須遵守的規範：
    {read_file('.ai/prompts/reactor-sub-agent-prompt.md')}
    
    Reactor Spec：
    {reactor_spec}
    
    請產生：
    - Reactor interface
    - Service implementation with execute(Object event)
    - Event type checking logic
    - Cross-aggregate consistency logic
    """,
    "subagent_type": "general-purpose"
}

# Step 3: 啟動 Reactor Test Generation Sub-agent
reactor_test_task = {
    "description": "Generate Reactor tests",
    "prompt": f"""
    請為 Reactor 產生測試。
    
    必須遵守的規範：
    {read_file('.ai/prompts/reactor-test-generation-prompt.md')}
    
    Reactor Code：
    {generated_reactor}
    
    必須包含：
    1. Happy path test - 處理預期事件
    2. Ignore irrelevant events test
    3. Error handling test
    4. 使用 BlockingMessageBus
    """,
    "subagent_type": "general-purpose"
}

# Step 4: 啟動 Reactor Code Review Sub-agent
reactor_review_task = {
    "description": "Review Reactor implementation",
    "prompt": f"""
    請審查 Reactor 實作。
    
    審查標準：
    {read_file('.ai/prompts/reactor-code-review-prompt.md')}
    
    Reactor 程式碼：
    {reactor_code}
    
    測試程式碼：
    {reactor_test_code}
    
    必須確認：
    1. 使用 execute(Object event) 方法
    2. 正確的 event type checking
    3. 跨聚合邊界維護
    4. 錯誤不會擴散
    """,
    "subagent_type": "general-purpose"
}
```

### 範例 5: Outbox Pattern 實作 🆕

```python
# Step 1: Main Agent 分析需求
aggregate_spec = read_file(".dev/specs/task/entity/task.json")
needs_outbox = determine_if_needs_outbox(aggregate_spec)

# Step 2: 啟動 Outbox Pattern Implementation Sub-agent
outbox_task = {
    "description": "Implement Outbox Pattern for Task aggregate",
    "prompt": f"""
    請為 Task aggregate 實作 Outbox Pattern。
    
    必須遵守的規範：
    {read_file('.ai/prompts/outbox-sub-agent-prompt.md')}
    
    Aggregate Spec：
    {aggregate_spec}
    
    請產生：
    1. TaskData.java - 實作 OutboxData<String> 介面
    2. TaskMapper.java - 包含 OutboxMapper 內部類別
    3. TaskOrmClient.java - 繼承 SpringJpaClient
    4. 更新 OutboxRepositoryConfig - 新增 Task 相關 beans
    
    關鍵要求：
    - OutboxMapper 必須是 TaskMapper 的內部類別
    - domainEventDatas 和 streamName 必須標記 @Transient
    - 使用 jakarta.persistence 而非 javax.persistence
    - 版本號從 0 開始是正常的
    """,
    "subagent_type": "general-purpose"
}

# Step 3: 驗證實作正確性
validation_task = {
    "description": "Validate Outbox implementation",
    "prompt": f"""
    請驗證 Outbox Pattern 實作是否符合 ADR-019 規範。
    
    檢查項目：
    1. OutboxMapper 是內部類別（不是獨立類別）
    2. @Transient 標註正確
    3. 套件位置正確
    4. 使用 jakarta.persistence
    5. Repository 配置正確
    
    Generated Code：
    {generated_outbox_code}
    
    請提供驗證報告。
    """,
    "subagent_type": "general-purpose"
}
```

## 🔧 使用指南

### 對使用者的建議

當你需要實作新功能時，可以這樣要求：

**後端 Use Cases:**
```
"請使用 command-sub-agent workflow 實作 create-product use case"
"請使用 query-sub-agent workflow 實作 get-product use case"
"請使用 reactor-sub-agent workflow 實作 notify-sprint-to-select-backlog-item"
"使用 sub-agent workflow 實作 [任何 use case]"
```

**Reactor 實作:**
```
"請使用 reactor-sub-agent workflow 實作跨聚合一致性"
"請使用 reactor-sub-agent 產生 read model projection"
"使用 reactor workflow 處理 ProductBacklogItemSelected 事件"
```

**Outbox Pattern 實作:** 🆕
```
"請使用 outbox-sub-agent workflow 為 Task aggregate 實作 Outbox Pattern"
"使用 outbox workflow 將 InMemoryRepository 升級為 OutboxRepository"
"請用 outbox-sub-agent 實作可靠的事件發布機制"
```

### 對 Main Agent 的指引

#### 識別 Command vs Query vs Reactor
**Command 特徵:**
- 動詞開頭：Create, Update, Delete, Estimate, Select, Define
- 產生 Domain Events
- 修改 Aggregate 狀態
- 需要 Repository.save()

**Query 特徵:**
- Get/List/Search 開頭
- 返回 DTO 或 List<DTO>
- 使用 Projection
- 無副作用

**Reactor 特徵:**
- Notify/Update/Project 開頭
- 實作 execute(Object event) 方法
- 處理跨聚合事件
- 使用 MessageBus
- 不直接被 REST API 呼叫

#### 執行原則
1. **明確分工**：不要混合不同 agent 的職責
2. **完整 Context**：確保每個 sub-agent 有足夠的 context
3. **串接結果**：正確傳遞前一個 agent 的輸出給下一個
4. **處理失敗**：如果某個 agent 失敗，要有適當的處理

## 📊 效益評估

### 預期改進

| 指標 | 傳統方式 | Sub-agent 方式 | 改進幅度 |
|------|---------|---------------|---------|
| 規範違規率 | 30-40% | 5-10% | ↓ 75% |
| 首次編譯成功率 | 60% | 90% | ↑ 50% |
| 測試完整性 | 70% | 95% | ↑ 36% |
| 測試通過率 | 50% | 85% | ↑ 70% |
| 開發時間 | 基準 | -20% | ↓ 20% |

### 關鍵成功因素

1. **Prompt 品質**：專門的 prompt 檔案必須完整且明確
2. **Context 大小**：確保不超過 sub-agent 的 context 限制
3. **清晰的介面**：agent 之間的輸入輸出要明確定義
4. **持續優化**：根據實際使用經驗更新 prompt

## 🔴 重要：Framework API Integration 指南

### 所有 Infrastructure Sub-agents 的共同要求

**自 2025-09-05 起，所有相關 sub-agent 都必須遵守 Framework API Integration Guide：**
- 📖 **位置**：`.ai/guides/FRAMEWORK-API-INTEGRATION-GUIDE.md`
- 🔴 **狀態**：MANDATORY REFERENCE - 已加入所有相關 sub-agent 的必讀清單

### 影響的 Sub-agents：
- **Command Sub-agent**: PgMessageDbClient 創建規範
- **Query Sub-agent**: JPA Configuration 規範  
- **Aggregate Sub-agent**: Entity 註解使用規範
- **Reactor Sub-agent**: Domain Event 處理規範
- **Outbox Sub-agent**: OutboxMapper 內部類別規範（最關鍵）
- **Profile Config Sub-agent**: Spring Profile 配置規範

### 關鍵防護：
1. **PgMessageDbClient** - 必須使用 JpaRepositoryFactory，絕不直接 new
2. **OutboxMapper** - 必須是內部類別，不可獨立存在
3. **@Transient 註解** - domainEventDatas 和 streamName 強制要求
4. **Jakarta Persistence** - 全面取代 javax.persistence
5. **自動檢查腳本** - `.ai/scripts/check-framework-api-compliance.sh`

## 🚀 進階使用

### 平行執行

某些情況下可以平行執行多個 sub-agents：

```python
# 平行產生多個 Use Cases
tasks = [
    create_code_generation_task("create-product"),
    create_code_generation_task("update-product"),
    create_code_generation_task("delete-product")
]

# 同時執行
results = parallel_execute(tasks)
```

### 條件執行

根據審查結果決定是否需要修正：

```python
review_result = review_agent.execute()
if review_result.has_must_fix_issues():
    fix_agent.execute(review_result.must_fix_issues)
```

## 📝 維護指南

### 更新 Prompts

當發現新的規範或常見錯誤時：

1. 更新對應的 prompt 檔案
2. 在 lessons/FAILURE-CASES.md 記錄新發現
3. 更新 CODE-REVIEW-CHECKLIST.md

### 監控效果

定期檢查：
- Sub-agent 的成功率
- 產生程式碼的品質
- 規範遵守情況

### 持續改進

- 收集使用回饋
- 分析失敗案例
- 優化 prompt 內容
- 調整工作流程

## 🔗 相關文件

### Sub-agent Prompts
- [Command Sub-agent Prompt](.ai/prompts/command-sub-agent-prompt.md)
- [Query Sub-agent Prompt](.ai/prompts/query-sub-agent-prompt.md)
- [Mutation Testing Enhancement Prompt](.ai/prompts/mutation-testing-sub-agent-prompt.md) 🆕
- [Reactor Sub-agent Prompt](.ai/prompts/reactor-sub-agent-prompt.md)
- [Reactor Test Generation Prompt](.ai/prompts/reactor-test-generation-prompt.md)
- [Reactor Code Review Prompt](.ai/prompts/reactor-code-review-prompt.md)
- [Outbox Sub-agent Prompt](.ai/prompts/outbox-sub-agent-prompt.md) 🆕
- [Test Generation Prompt](.ai/prompts/test-generation-prompt.md)
- [Code Review Prompt](.ai/prompts/code-review-prompt.md)
- [Controller Code Generation Prompt](.ai/prompts/controller-code-generation-prompt.md)
- [Controller Test Generation Prompt](.ai/prompts/controller-test-generation-prompt.md)
- [Controller Code Review Prompt](.ai/prompts/controller-code-review-prompt.md)

### Standards & Guides
- [Coding Standards](.ai/tech-stacks/java-ca-ezddd-spring/coding-standards.md)
- [Code Review Checklist](.ai/tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md)
- [Test Data Preparation Guide](.ai/TEST-DATA-PREPARATION-GUIDE.md)

## 📌 快速參考

### 啟動 Command Code Generation
```
Task: Generate [command-name] use case
Prompt: .ai/prompts/command-sub-agent-prompt.md
Input: spec file
Output: command implementation with events
```

### 啟動 Query Code Generation
```
Task: Generate [query-name] use case
Prompt: .ai/prompts/query-sub-agent-prompt.md
Input: spec file
Output: query implementation with projections
```

### 啟動 Reactor Code Generation
```
Task: Generate [reactor-name] reactor
Prompt: .ai/prompts/reactor-sub-agent-prompt.md
Input: spec file + event definitions
Output: reactor implementation with event handling
```

### 啟動 Test Generation
```
Task: Generate tests for [use-case-name]
Prompt: .ai/prompts/test-generation-prompt.md
Input: production code + spec
Output: test code
```

### 啟動 Code Review
```
Task: Review [use-case-name] implementation
Prompt: .ai/prompts/code-review-prompt.md
Input: all generated code
Output: review report
```