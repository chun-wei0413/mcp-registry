# Workflow: 功能實現

**標籤**: `#sub-agent-integrated` `#complete-integration` `#code-generation` `#testing` `#review`  
**整合狀態**: 🤖 完全整合 (3 Sub-agents)

## 概述

此工作流程指導 AI 和人類協作實現新功能，確保遵循既定的架構模式和編碼標準。
本流程與 **Sub-agent System** 深度整合，在關鍵步驟使用專門的 sub-agents 來提高程式碼品質。

### 🚨 強制要求
**Use Case 測試必須使用 ezSpec BDD 風格**：
- 使用 `@EzFeature` 和 `@EzScenario` 註解
- 使用 Given-When-Then 格式
- 不得使用純 JUnit 風格的 Use Case 測試

### 🤖 Sub-agent 整合點
- **階段 3**: 使用 Command/Query Sub-agent 產生 Use Case
- **階段 4**: 使用 Controller Sub-agent 產生 API
- **測試階段**: 使用 Test Generation Agent 產生測試
- **審查階段**: 使用 Code Review Agent 進行品質檢查

## 如何啟動此 Workflow

使用以下 AI 指令啟動：

```
"使用 feature-implementation workflow 實現 [功能名稱]"
```

或更詳細的指令：

```
"我需要實現用戶註冊功能，請使用 feature-implementation workflow
需求：
- 用戶提供 email 和密碼
- email 必須唯一
- 密碼需要加密儲存"
```

## 目標

1. 快速且正確地實現新功能
2. 保持架構一致性
3. 確保代碼質量
4. 完整的測試覆蓋

## 工作流程

### 階段 1：需求理解

**AI 行動**：
1. 分析功能需求
2. 識別受影響的組件
3. 評估技術可行性
4. 提出實現方案

**人類輸入**：
- 澄清業務需求
- 確認驗收標準
- 設定優先級和時限
- 提供用戶故事

**產出**：
- 功能規格文檔
- 技術設計方案
- 工作分解結構

### 階段 2：領域建模

**AI 行動**：
1. 設計領域模型
2. 定義 Aggregate 邊界
3. 識別 Domain Events
4. 創建 Value Objects

**建模原則**（基於 `.ai/tech-stacks/java-ca-ezddd-spring/examples/`）：
```java
// Aggregate Root
public class Plan extends EsAggregateRoot<PlanId, PlanEvent> {
    private PlanId id;
    private PlanName name;
    private List<Project> projects;
    
    // 業務方法觸發事件
    public void rename(String newName) {
        Contract.requireNotNull(newName, "Plan name");
        apply(new PlanRenamed(id, newName));
    }
}

// Value Object
@Value
public class PlanName {
    String value;
    
    public PlanName(String value) {
        Contract.require(value != null && !value.isBlank(), 
            "Plan name cannot be empty");
        this.value = value;
    }
}
```

**人類輸入**：
- 驗證業務規則
- 確認領域邊界
- 審查事件設計

**產出**：
- 領域模型圖
- 實體規格文檔
- 事件流程圖

### 階段 3：Use Case 實現

#### 🤖 使用 Code Generation Sub-agent

**啟動方式**：
```
請啟動 Code Generation Sub-agent 根據 spec 產生 Use Case 程式碼
```

**Sub-agent 執行**：
1. 載入 spec 檔案和 coding-standards.md
2. 產生 Use Case Interface (with Inner Classes)
3. 產生 Service Implementation
4. 產生 DTOs 和 Mappers
5. 確保符合所有編碼規範

**傳統 AI 行動**（若不使用 sub-agent）：
1. 實現 Use Case 介面
2. 創建 Input/Output DTOs
3. 實現 Service 邏輯
4. 處理異常情況

**Use Case 模式**（基於 `.ai/tech-stacks/java-ca-ezddd-spring/examples/`）：
```java
// Use Case Interface
public interface CreatePlanUseCase extends Command<CreatePlanInput, PlanDto> {
}

// Service Implementation
@Service
@AllArgsConstructor
public class CreatePlanService implements CreatePlanUseCase {
    private final PlanRepository repository;
    private final MessageBus messageBus;
    
    @Override
    public CqrsOutput<PlanDto> execute(CreatePlanInput input) {
        // 1. 創建聚合根
        var plan = new Plan(PlanId.create(), input.getName(), input.getUserId());
        
        // 2. 保存到資料庫
        repository.save(plan);
        
        // 3. 發布事件
        messageBus.publish(plan.getEvents());
        
        // 4. 返回結果
        return CqrsOutput.of(PlanMapper.toDto(plan));
    }
}
```

**人類輸入**：
- 確認業務邏輯
- 審查錯誤處理
- 驗證性能考量

**產出**：
- Use Case 實現
- 單元測試
- 集成測試

### 階段 4：API 層實現

#### 🤖 使用 Controller Code Generation Sub-agent

**啟動方式**：
```
請啟動 Controller Code Generation Sub-agent 產生 REST Controller
```

**Sub-agent 執行**：
1. 載入 controller spec 和相關規範
2. 產生 Controller class with Spring annotations
3. 產生 ApiError 錯誤處理類別
4. 實作 HTTP status codes 處理
5. 確保 REST API 合規性

**傳統 AI 行動**（若不使用 sub-agent）：
1. 創建 REST Controller
2. 定義 API 端點
3. 實現請求/響應映射
4. 添加驗證和錯誤處理

**Controller 模式**（基於 `.ai/tech-stacks/java-ca-ezddd-spring/examples/`）：
```java
@RestController
@RequestMapping("/api/v1/plans")
@AllArgsConstructor
public class CreatePlanController {
    private final CreatePlanUseCase useCase;
    
    @PostMapping
    public ResponseEntity<PlanDto> createPlan(
            @Valid @RequestBody CreatePlanRequest request) {
        var input = toInput(request);
        var output = useCase.execute(input);
        return ResponseEntity.created(locationOf(output))
                           .body(output.getData());
    }
}
```

**人類輸入**：
- 審查 API 設計
- 確認安全需求
- 驗證響應格式

**產出**：
- REST API 實現
- API 文檔
- Postman 集合

### 階段 5：前端集成

**AI 行動**：
1. 創建 React 組件
2. 實現 API 調用
3. 管理狀態
4. 處理用戶交互

**前端模式**（基於專案標準）：
```typescript
// API Service
export const planApi = {
  create: async (data: CreatePlanData): Promise<Plan> => {
    const response = await api.post('/plans', data);
    return response.data;
  }
};

// React Component
const CreatePlanForm: React.FC = () => {
  const queryClient = useQueryClient();
  
  const mutation = useMutation({
    mutationFn: planApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plans'] });
      toast.success('Plan created successfully');
    }
  });
  
  return (
    <form onSubmit={handleSubmit}>
      {/* Form fields */}
    </form>
  );
};
```

**人類輸入**：
- 審查 UI/UX 設計
- 確認交互流程
- 測試邊緣情況

**產出**：
- React 組件
- 樣式實現
- E2E 測試

## 測試策略

### 🤖 使用 Test Generation Sub-agents

#### Use Case 測試
**啟動方式**：
```
請啟動 Test Generation Sub-agent 為 Use Case 產生 ezSpec 測試
```

**Sub-agent 執行**：
1. 分析 production code
2. 載入 ezspec-test-template.md
3. 產生完整的 BDD 測試場景
4. 準備測試資料
5. 執行測試確保通過

#### Controller 測試
**啟動方式**：
```
請啟動 Controller Test Generation Sub-agent 產生 Controller 測試
```

**Sub-agent 執行**：
1. 產生 MockMvc 測試檔案
2. 產生 REST Assured 整合測試檔案
3. 涵蓋所有測試場景
4. 執行測試確保全部通過

### 測試金字塔
1. **單元測試**（最多）
   - Domain 邏輯
   - Use Case 邏輯
   - 工具類

2. **集成測試**（適中）
   - API 端點
   - 資料庫操作
   - 事件發布

3. **E2E 測試**（最少）
   - 關鍵用戶流程
   - 跨系統集成

### 測試模式

#### 🚨 強制要求：Use Case 測試必須使用 ezSpec
**絕對不可使用純 JUnit 風格撰寫 Use Case 測試**

使用 ezSpec BDD 風格：
```java
@EzFeature
public class CreatePlanUseCaseTest {
    static Feature feature = Feature.New("Create Plan Use Case");
    
    @EzScenario
    public void should_create_plan_with_valid_name() {
        feature.newScenario("Successfully create a plan with valid name")
            .Given("valid plan creation input", env -> {
                var input = new CreatePlanInput("My Plan");
                env.put("input", input);
            })
            .When("the use case is executed", env -> {
                var input = env.get("input", CreatePlanInput.class);
                var output = useCase.execute(input);
                env.put("output", output);
            })
            .Then("the plan should be created successfully", env -> {
                var output = env.get("output", CqrsOutput.class);
                assertThat(output.getData().getName()).isEqualTo("My Plan");
                assertThat(publishedEvents).hasSize(1);
                assertThat(publishedEvents.get(0)).isInstanceOf(PlanCreated.class);
            })
            .Execute();
    }
}
```

## 部署考量

1. **資料庫遷移**
   - 使用 Flyway 腳本
   - 向後兼容
   - 回滾策略

2. **配置管理**
   - 環境變量
   - Feature flags
   - 秘密管理

3. **監控**
   - 日誌記錄
   - 性能指標
   - 錯誤追踪

## 品質審查

### 🤖 使用 Code Review Sub-agent

**啟動方式**：
```
請啟動 Code Review Sub-agent 審查所有產生的程式碼
```

**Sub-agent 執行**：
1. 載入 CODE-REVIEW-CHECKLIST.md
2. 檢查規範遵守情況
3. 識別品質問題
4. 提供改進建議
5. 產生審查報告

## 成功標準

- [ ] 功能符合所有驗收標準
- [ ] 測試覆蓋率 > 80%
- [ ] 無關鍵錯誤
- [ ] 性能符合要求
- [ ] 文檔完整
- [ ] **Sub-agent 審查通過**

## 時間估算

- 簡單功能：1-2 天
- 中等功能：3-5 天
- 複雜功能：1-2 週

## 注意事項

1. **遵循既定模式**：使用 `.ai/tech-stacks/` 中的模式
2. **小步前進**：頻繁提交和測試
3. **及時溝通**：遇到阻礙立即討論
4. **文檔同步**：代碼和文檔一起更新

## 相關資源

### Sub-agent Prompts
- `.ai/prompts/command-sub-agent-prompt.md` - Command Use Case Generation
- `.ai/prompts/query-sub-agent-prompt.md` - Query Use Case Generation
- `.ai/prompts/aggregate-sub-agent-prompt.md` - Aggregate Generation
- `.ai/prompts/test-generation-prompt.md` - Test Generation Agent
- `.ai/prompts/controller-code-generation-prompt.md` - Controller Generation
- `.ai/prompts/controller-test-generation-prompt.md` - Controller Test Generation
- `.ai/prompts/code-review-prompt.md` - Code Review Agent

### 技術資源
- `.ai/SUB-AGENT-SYSTEM.md` - Sub-agent 系統說明
- `.ai/tech-stacks/java-ca-ezddd-spring/codegen/` - 代碼生成模板
- `.ai/tech-stacks/` - 編碼標準
- `.dev/specs/` - 領域規格
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/` - 實現範例