# Aggregate 實作檢查清單

本文件提供 Aggregate 實作的完整檢查清單，確保避免常見錯誤。

## ⚠️ 關鍵檢查項目

### 1. 集合欄位初始化 ⚠️ CRITICAL
- [ ] **所有集合欄位必須在宣告時初始化**
  ```java
  // ✅ 正確
  private final List<Member> members = new ArrayList<>();
  
  // ❌ 錯誤：會導致事件重播失敗
  private final List<Member> members;
  // 然後在建構子中 this.members = new ArrayList<>();
  ```

- [ ] **建構子中不能在 super() 之後重新初始化集合**
  ```java
  public MyAggregate(List<Events> events) {
      super(events);  // 事件重播
      // ❌ 不要在這裡初始化集合！
  }
  ```

### 2. 事件重播建構子
- [ ] 提供接受事件列表的建構子
- [ ] 呼叫 `super(domainEvents)`
- [ ] 不重新初始化任何集合欄位

### 3. 事件處理器 (when 方法)
- [ ] 處理所有定義的 Domain Events
- [ ] 正確更新 aggregate 狀態
- [ ] 集合操作使用 add/remove，不要重新賦值

### 4. Command Methods
- [ ] 前置條件檢查 (require, requireNotNull)
- [ ] 產生對應的 Domain Event
- [ ] 後置條件檢查 (ensure)
- [ ] 驗證事件正確產生

### 5. Mapper 實作
- [ ] `toDomain()` 從事件重建時使用事件建構子
- [ ] `toDomain()` 無事件時要恢復所有狀態（特別是集合）
  ```java
  if (data.getMembers() != null) {
      for (MemberData member : data.getMembers()) {
          aggregate.addMember(...);  // 恢復成員
      }
  }
  ```
- [ ] `toData()` 完整轉換所有欄位

## 常見錯誤案例

### ❌ 錯誤 1：集合在 super() 後初始化
```java
public class Sprint extends EsAggregateRoot<SprintId, SprintEvents> {
    private final List<String> selectedPbiIds;
    
    public Sprint(List<SprintEvents> events) {
        super(events);  // 事件重播，填充 selectedPbiIds
        this.selectedPbiIds = new ArrayList<>();  // 錯誤！清空了資料
    }
}
```
**症狀**：集合元素無法累積，每次只保留最後一個

### ❌ 錯誤 2：Mapper 未恢復集合狀態
```java
public static Sprint toDomain(SprintData data) {
    Sprint sprint = new Sprint(id, name);
    // 忘記恢復 selectedPbiIds！
    return sprint;
}
```
**症狀**：從資料庫載入後集合為空

### ❌ 錯誤 3：事件處理器覆蓋集合
```java
protected void when(SprintEvents event) {
    switch (event) {
        case BacklogItemSelected e -> {
            // 錯誤：重新賦值會丟失其他 PBI
            this.selectedPbiIds = List.of(e.pbiId());
            
            // 正確：加入到現有集合
            this.selectedPbiIds.add(e.pbiId());
        }
    }
}
```
**症狀**：集合只有最新的元素

## 測試驗證要點

### 必要測試案例
1. **集合累積測試**
   - 連續新增多個元素
   - 驗證所有元素都存在
   
2. **事件重播測試**
   - 儲存 aggregate
   - 重新載入
   - 驗證狀態完整恢復
   
3. **併發測試**
   - 多個操作同時執行
   - 驗證樂觀鎖正常運作

4. **Domain Event 序列化測試** ⚠️ CRITICAL
   - 每個 Aggregate 必須有對應的 EventSerializationTest
   - 測試所有 Domain Events 的序列化和反序列化
   - 確保 JSON 不包含不需要的欄位（如 "empty"）
   - 驗證所有欄位正確保留

### 測試範例
```java
@Test
void should_accumulate_selected_pbis() {
    // Given
    Sprint sprint = new Sprint(sprintId, "Sprint 1", productId);
    
    // When
    sprint.selectBacklogItem("pbi-001");
    sprint.selectBacklogItem("pbi-002");
    sprint.selectBacklogItem("pbi-003");
    
    // Then
    assertEquals(3, sprint.getSelectedPbiIds().size());
    assertTrue(sprint.hasSelectedPbi("pbi-001"));
    assertTrue(sprint.hasSelectedPbi("pbi-002"));
    assertTrue(sprint.hasSelectedPbi("pbi-003"));
}

@Test
void should_restore_selected_pbis_after_save_and_reload() {
    // Given
    Sprint sprint = new Sprint(sprintId, "Sprint 1", productId);
    sprint.selectBacklogItem("pbi-001");
    sprint.selectBacklogItem("pbi-002");
    
    // When
    repository.save(sprint);
    Sprint reloaded = repository.findById(sprintId).get();
    
    // Then
    assertEquals(2, reloaded.getSelectedPbiIds().size());
}
```

### Domain Event 序列化測試範例
```java
@Test
void should_serialize_and_deserialize_domain_events() throws Exception {
    // Given
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    
    SprintEvents.BacklogItemSelected event = SprintEvents.BacklogItemSelected.create(
        sprintId, "pbi-001", "user-admin"
    );
    
    // When: Serialize to JSON
    String json = objectMapper.writeValueAsString(event);
    
    // Then: No unwanted fields
    assertFalse(json.contains("\"empty\""));
    
    // When: Deserialize back
    SprintEvents.BacklogItemSelected deserialized = objectMapper.readValue(
        json, SprintEvents.BacklogItemSelected.class
    );
    
    // Then: All fields preserved
    assertEquals(event.sprintId(), deserialized.sprintId());
    assertEquals(event.pbiId(), deserialized.pbiId());
    assertEquals(event.userId(), deserialized.userId());
    assertEquals(event.occurredOn(), deserialized.occurredOn());
    assertEquals(event.eventId(), deserialized.eventId());
}
```

## 相關文件
- [Aggregate 編碼規範](.ai/tech-stacks/java-ca-ezddd-spring/coding-standards/aggregate-standards.md)
- [Mapper 編碼規範](.ai/tech-stacks/java-ca-ezddd-spring/coding-standards/mapper-standards.md)
- [測試驗證指南](.ai/checklists/TEST-VERIFICATION-GUIDE.md)