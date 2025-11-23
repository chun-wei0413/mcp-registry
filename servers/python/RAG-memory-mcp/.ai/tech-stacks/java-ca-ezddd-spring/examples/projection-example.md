# Projection 實作範例

這個範例展示如何實作一個完整的 Projection，用於優化跨 Aggregate 的查詢。

## 業務需求

查詢某個使用者的所有待辦任務，包含：
- 任務基本資訊
- 所屬計劃和專案名稱
- 標籤列表
- 截止日期

## 1. 定義 Projection Interface

```java
package tw.teddysoft.example.plan.usecase.port.out.projection;

import tw.teddysoft.ezddd.cqrs.usecase.query.Projection;
import tw.teddysoft.ezddd.cqrs.usecase.query.ProjectionInput;
import java.time.LocalDate;
import java.util.List;

// ✅ 正確：使用複數形命名，繼承 Projection 介面
public interface TasksProjection extends Projection<TasksProjection.TasksProjectionInput, List<TaskData>> {
    
    // query 方法由 Projection 介面繼承，不需要重複宣告
    
    class TasksProjectionInput implements ProjectionInput {
        public String userId;
        public LocalDate fromDate;
        public LocalDate toDate;
        public String status;  // PENDING, COMPLETED, ALL
        
        public TasksProjectionInput() {
            // 預設構造子
        }
        
        public TasksProjectionInput(String userId) {
            this.userId = userId;
        }
    }
}
```

## 2. 定義 Data 物件 (Persistence Object)

```java
package tw.teddysoft.example.plan.usecase.port.out;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "task")
public class TaskData {
    @Id
    @Column(name = "task_id")
    private String taskId;
    
    @Column(name = "task_name")
    private String taskName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "deadline")
    private LocalDate deadline;
    
    @Column(name = "completed")
    private boolean completed;
    
    @Column(name = "plan_id")
    private String planId;
    
    @Column(name = "plan_name")
    private String planName;
    
    @Column(name = "project_id")
    private String projectId;
    
    @Column(name = "project_name")
    private String projectName;
    
    // Getters and setters...
}
```

## 3. 實作 JPA Projection

```java
package tw.teddysoft.example.plan.adapter.out.database.springboot.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// ⚠️ 重要：不要加 @Repository 註解，Spring Data JPA 會自動產生 bean
public interface JpaTasksProjection 
    extends TasksProjection, JpaRepository<TaskData, String> {
    
    @Override
    default List<TaskData> query(TasksProjectionInput input) {
        // 根據條件調用不同的查詢方法
        if ("PENDING".equals(input.status)) {
            return findPendingTasksByUser(
                input.userId, 
                input.fromDate, 
                input.toDate
            );
        } else if ("COMPLETED".equals(input.status)) {
            return findCompletedTasksByUser(
                input.userId, 
                input.fromDate, 
                input.toDate
            );
        } else {
            return findAllTasksByUser(
                input.userId, 
                input.fromDate, 
                input.toDate
            );
        }
    }
    
    @Query("""
        SELECT t
        FROM TaskData t
        WHERE t.userId = :userId
        AND t.completed = false
        AND (:fromDate IS NULL OR t.deadline >= :fromDate)
        AND (:toDate IS NULL OR t.deadline <= :toDate)
        ORDER BY t.deadline ASC
        """)
    List<TaskData> findPendingTasksByUser(
        @Param("userId") String userId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );
    
    @Query("""
        SELECT t
        FROM TaskData t
        WHERE t.userId = :userId
        AND t.completed = true
        AND (:fromDate IS NULL OR t.deadline >= :fromDate)
        AND (:toDate IS NULL OR t.deadline <= :toDate)
        ORDER BY t.deadline ASC
        """)
    List<TaskData> findCompletedTasksByUser(
        @Param("userId") String userId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );
    
    @Query("""
        SELECT t
        FROM TaskData t
        WHERE t.userId = :userId
        AND (:fromDate IS NULL OR t.deadline >= :fromDate)
        AND (:toDate IS NULL OR t.deadline <= :toDate)
        ORDER BY t.deadline ASC
        """)
    List<TaskData> findAllTasksByUser(
        @Param("userId") String userId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );
}
```

## 4. DTO 定義 (用於 Use Case 輸出)

```java
package tw.teddysoft.example.plan.dto;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class TaskDto {
    String taskId;
    String taskName;
    String description;
    LocalDate deadline;
    boolean completed;
    String planId;
    String planName;
    String projectId;
    String projectName;
    List<String> tagIds;
    List<String> tagNames;
}
```

## 5. Mapper (Data to DTO 轉換)

```java
package tw.teddysoft.example.plan.mapper;

import tw.teddysoft.example.plan.usecase.port.out.TaskData;
import tw.teddysoft.example.plan.dto.TaskDto;
import java.util.List;
import java.util.stream.Collectors;

public class TaskMapper {
    private TaskMapper() {} // Prevent instantiation
    
    public static TaskDto toDto(TaskData data) {
        if (data == null) {
            return null;
        }
        
        return TaskDto.builder()
            .taskId(data.getTaskId())
            .taskName(data.getTaskName())
            .description(data.getDescription())
            .deadline(data.getDeadline())
            .completed(data.isCompleted())
            .planId(data.getPlanId())
            .planName(data.getPlanName())
            .projectId(data.getProjectId())
            .projectName(data.getProjectName())
            .build();
    }
    
    public static List<TaskDto> toDtoList(List<TaskData> dataList) {
        if (dataList == null) {
            return Collections.emptyList();
        }
        
        return dataList.stream()
            .map(TaskMapper::toDto)
            .collect(Collectors.toList());
    }
}
```

## 6. InMemory 實作版本（用於測試）

```java
package tw.teddysoft.example.plan.adapter.out.database.springboot.projection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTasksProjection implements TasksProjection {
    
    private final Map<String, TaskData> store;
    
    public InMemoryTasksProjection(Map<String, TaskData> store) {
        this.store = store;
    }
    
    @Override
    public List<TaskData> query(TasksProjectionInput input) {
        return store.values().stream()
            .filter(task -> matchesUserId(task, input.userId))
            .filter(task -> matchesStatus(task, input.status))
            .filter(task -> matchesDateRange(task, input.fromDate, input.toDate))
            .sorted(Comparator.comparing(TaskData::getDeadline))
            .collect(Collectors.toList());
    }
    
    private boolean matchesUserId(TaskData task, String userId) {
        return userId == null || userId.equals(task.getUserId());
    }
    
    private boolean matchesStatus(TaskData task, String status) {
        if (status == null || "ALL".equals(status)) {
            return true;
        }
        if ("PENDING".equals(status)) {
            return !task.isCompleted();
        }
        if ("COMPLETED".equals(status)) {
            return task.isCompleted();
        }
        return true;
    }
    
    private boolean matchesDateRange(TaskData task, LocalDate fromDate, LocalDate toDate) {
        if (task.getDeadline() == null) {
            return false;
        }
        if (fromDate != null && task.getDeadline().isBefore(fromDate)) {
            return false;
        }
        if (toDate != null && task.getDeadline().isAfter(toDate)) {
            return false;
        }
        return true;
    }
}
```

## 7. 在 Use Case 中使用

```java
// 注意：不要加 @Service 或 @Component 註解
public class GetTasksDueTodayService implements GetTasksDueTodayUseCase {
    
    private final TasksProjection projection;
    
    public GetTasksDueTodayService(TasksProjection projection) {
        this.projection = projection;
    }
    
    @Override
    public GetTasksDueTodayOutput execute(GetTasksDueTodayInput input) {
        // 準備 Projection 輸入
        TasksProjectionInput projectionInput = 
            new TasksProjectionInput(input.getUserId());
        projectionInput.fromDate = LocalDate.now();
        projectionInput.toDate = LocalDate.now();
        projectionInput.status = "PENDING";
        
        // 執行查詢 (返回 Data 物件)
        List<TaskData> taskDataList = projection.query(projectionInput);
        
        // 轉換 Data 到 DTO
        List<TaskDto> taskDtos = TaskMapper.toDtoList(taskDataList);
        
        // 轉換輸出
        return GetTasksDueTodayOutput.builder()
            .tasks(taskDtos)
            .count(taskDtos.size())
            .build();
    }
}
```

## 8. Spring 配置

```java
@Configuration
@EnableJpaRepositories(basePackages = {
    // ... 其他套件 ...
    "tw.teddysoft.example.plan.adapter.out.database.springboot.projection",
    // ... 其他套件 ...
})
public class JpaConfiguration {
    // Spring Data JPA 會自動為該套件下的 JpaRepository 介面產生實作
}

@Configuration
public class UseCaseConfiguration {
    
    @Bean
    public GetTasksDueTodayUseCase getTasksDueTodayUseCase(TasksProjection projection) {
        return new GetTasksDueTodayService(projection);
    }
    
    // 注意：JPA Projection 不需要手動註冊 Bean
    // Spring Data JPA 會透過 @EnableJpaRepositories 自動產生
    
    @Bean
    @Profile("test")
    public TasksProjection inMemoryTasksProjection() {
        // 測試環境使用 InMemory 實作
        Map<String, TaskData> store = new ConcurrentHashMap<>();
        return new InMemoryTasksProjection(store);
    }
}
```

## 9. 測試 Projection

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TasksProjectionTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private JpaTasksProjection projection;
    
    @Test
    void query_ReturnsPendingTasks() {
        // Given
        String userId = "user-123";
        TaskData task1 = createTaskData(userId, "任務 1", LocalDate.now(), false);
        TaskData task2 = createTaskData(userId, "任務 2", LocalDate.now().plusDays(1), false);
        TaskData task3 = createTaskData(userId, "已完成", LocalDate.now(), true);
        
        entityManager.persistAndFlush(task1);
        entityManager.persistAndFlush(task2);
        entityManager.persistAndFlush(task3);
        
        // When
        TasksProjectionInput input = new TasksProjectionInput(userId);
        input.status = "PENDING";
        
        List<TaskData> results = projection.query(input);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(TaskData::getTaskName)
            .containsExactly("任務 1", "任務 2");
    }
}
```

## 效能優化建議

### 1. 資料庫索引
```sql
-- 使用者查詢索引
CREATE INDEX idx_plan_user_id ON plan(user_id);
CREATE INDEX idx_task_deadline ON task(deadline);
CREATE INDEX idx_task_completed ON task(completed);

-- 複合索引
CREATE INDEX idx_task_deadline_completed ON task(deadline, completed);
```

### 2. 查詢優化
- 使用分頁避免載入過多資料
- 考慮使用快取（Spring Cache）
- 對於複雜統計，考慮使用 Materialized View

### 3. 注意事項
- Projection 是唯讀的，不應該修改資料
- 避免 N+1 查詢問題
- 定期分析查詢效能

## 重點總結

### Projection 核心概念
1. **命名規範**：使用複數形 (TasksProjection, ProductsProjection)
2. **返回類型**：返回 Data 物件，不是 DTO
3. **介面繼承**：必須繼承 `Projection<Input, Output>`
4. **Input 實作**：Input 必須實作 `ProjectionInput` 介面
5. **Bean 管理**：JPA Projection 不需要加 `@Repository` 註解

### Data vs DTO
- **Data Objects**: 持久層物件（對應資料表結構）
- **DTO Objects**: 傳輸層物件（用於 Use Case 輸出）
- **Projection 返回 Data**，Use Case 負責轉換成 DTO

### Spring 配置要點
- JPA Projection 透過 `@EnableJpaRepositories` 自動掃描產生 bean
- Use Case Service 不要加 `@Service` 或 `@Component` 註解
- 在 `UseCaseConfiguration` 中明確註冊 Use Case bean

## 相關資源

- [Projection Standards](../coding-standards/projection-standards.md)
- [Query Sub-Agent Prompt](../../prompts/query-sub-agent-prompt.md)
- [Repository Pattern](./repository/README.md)
- [UseCase Pattern](./usecase/README.md)