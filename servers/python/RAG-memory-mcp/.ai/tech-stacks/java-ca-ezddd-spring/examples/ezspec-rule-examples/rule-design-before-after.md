# ezSpec Rule 設計：重構前後對比

## 重構前：過細的 Rules（每個 Rule 只有 1-2 個 scenarios）

```java
// 6 個 Rules，大部分只有 1 個 scenario
static final String PLAN_CREATION_RULE = "A plan can be created with valid ID, name, and user";
static final String UNIQUE_ID_RULE = "Plan ID must be unique within the system";
static final String REQUIRED_NAME_RULE = "Plan name is required and cannot be empty";
static final String VALID_ID_FORMAT_RULE = "Plan ID must be a valid non-empty string";
static final String USER_OWNERSHIP_RULE = "A user can own multiple plans with different IDs";
static final String SAME_NAME_ALLOWED_RULE = "Different users can create plans with the same name";
```

統計結果：
- PLAN_CREATION_RULE - 1 個 scenario
- UNIQUE_ID_RULE - 1 個 scenario
- REQUIRED_NAME_RULE - 2 個 scenarios
- VALID_ID_FORMAT_RULE - 1 個 scenario
- USER_OWNERSHIP_RULE - 1 個 scenario
- SAME_NAME_ALLOWED_RULE - 1 個 scenario

## 重構後：適中粒度的 Rules（每個 Rule 有 3-5 個 scenarios）

```java
// 3 個 Rules，每個有多個相關的 scenarios
static final String PLAN_IDENTIFICATION_RULE = "Plan must have a valid and unique identifier";
static final String PLAN_INFORMATION_RULE = "Plan must have valid name and owner information";
static final String USER_PLAN_OWNERSHIP_RULE = "Users can own and manage multiple plans";
```

統計結果：
- PLAN_IDENTIFICATION_RULE - 3 個 scenarios
  - create_plan_fails_with_duplicate_id
  - create_plan_with_invalid_planid_format
  - create_plan_with_null_id
- PLAN_INFORMATION_RULE - 4 個 scenarios
  - create_a_plan
  - create_plan_fails_with_empty_name
  - create_plan_fails_with_null_values
  - create_plan_with_null_user_id
- USER_PLAN_OWNERSHIP_RULE - 3 個 scenarios
  - create_multiple_plans_by_same_user
  - create_plans_with_same_name_but_different_ids
  - user_can_own_plans_with_different_names

## 重構的好處

1. **更好的組織**: 相關的測試場景被組織在同一個業務規則下
2. **符合 Gherkin 最佳實踐**: 每個 Rule 有 3-5 個 scenarios
3. **減少 Rule 數量**: 從 6 個減少到 3 個，更容易理解和維護
4. **完整的驗證**: 每個業務規則都有正面、負面和邊界案例

## 設計原則

### 如何決定 Rule 的粒度？

1. **相關性**: 將驗證相同業務概念的 scenarios 放在一起
   - 例如：所有關於 ID 的驗證（唯一性、格式、null）都在 `PLAN_IDENTIFICATION_RULE` 下

2. **完整性**: 每個 Rule 應該完整地探索一個業務規則
   - 正面案例：成功的情況
   - 負面案例：各種失敗的情況
   - 邊界案例：極端或特殊的情況

3. **業務視角**: Rule 名稱應該從業務角度描述規則，而非技術實現
   - ❌ "ID Validation" （技術視角）
   - ✅ "Plan must have a valid and unique identifier" （業務視角）

## CreateTagUseCaseTest 的例子

重構前：
```java
static final String TAG_CREATION_RULE = "A tag can be created with valid ID, name, color, and plan";
static final String VALID_COLOR_FORMAT_RULE = "Tag color must be in valid hex format (#RRGGBB)";
```

重構後：
```java
static final String TAG_PROPERTIES_RULE = "Tag must have valid properties and belong to a plan";
```

合併後的 scenarios (5 個)：
- create_a_tag_successfully
- create_a_tag_with_invalid_color_format
- create_tag_with_null_name
- create_tag_with_empty_name
- create_multiple_tags_for_same_plan

## 結論

適當的 Rule 設計需要在過細和過粗之間找到平衡。目標是讓每個 Rule：
- 表達一個清晰的業務規則
- 包含足夠的 scenarios 來完整驗證該規則
- 對技術和非技術人員都有意義