# uContract 詳細範例集

本文件提供 uContract 各個進階方法的詳細使用範例，展示在不同場景下的最佳實踐。

## 1. old() 詳細範例

### 基本使用：捕獲單一屬性

```java
public void updatePrice(Money newPrice) {
    requireNotNull("New price", newPrice);
    require("Price is positive", () -> newPrice.isPositive());
    
    // 捕獲單一屬性
    Money oldPrice = old(() -> this.price);
    
    this.price = newPrice;
    
    // 比較新舊值
    ensure("Price changed", () -> !price.equals(oldPrice));
    ensure("Price update is tracked", () -> 
        priceHistory.getLast().oldPrice().equals(oldPrice) &&
        priceHistory.getLast().newPrice().equals(newPrice)
    );
}
```

### 進階使用：捕獲複雜物件

```java
public void updateCustomerInfo(CustomerInfo newInfo) {
    requireNotNull("Customer info", newInfo);
    
    // 捕獲整個複雜物件
    CustomerInfo oldInfo = old(() -> this.customerInfo);
    
    // 捕獲特定的嵌套屬性
    String oldEmail = old(() -> this.customerInfo.getEmail());
    Address oldAddress = old(() -> this.customerInfo.getAddress());
    
    this.customerInfo = newInfo;
    
    // 詳細的變更追蹤
    if (!oldEmail.equals(newInfo.getEmail())) {
        apply(new EmailChanged(customerId, oldEmail, newInfo.getEmail()));
    }
    
    if (!oldAddress.equals(newInfo.getAddress())) {
        apply(new AddressChanged(customerId, oldAddress, newInfo.getAddress()));
    }
    
    ensure("Customer info updated", () -> !customerInfo.equals(oldInfo));
}
```

### 集合狀態的捕獲

```java
public void addOrderItem(OrderItem item) {
    requireNotNull("Order item", item);
    
    // 捕獲集合的大小和內容
    int oldSize = old(() -> orderItems.size());
    List<OrderItem> oldItems = old(() -> new ArrayList<>(orderItems));
    Money oldTotal = old(() -> calculateTotal());
    
    orderItems.add(item);
    
    // 驗證集合變更
    ensure("Item added", () -> orderItems.size() == oldSize + 1);
    ensure("New item is in collection", () -> orderItems.contains(item));
    ensure("Old items unchanged", () -> 
        oldItems.stream().allMatch(orderItems::contains)
    );
    ensure("Total updated", () -> calculateTotal().equals(oldTotal.add(item.getPrice())));
}
```

## 2. ensureAssignable() 詳細範例

### 基本欄位驗證

```java
public void updateEmail(String newEmail) {
    var oldUser = old(() -> this.clone());
    
    this.email = newEmail;
    this.lastModified = DateProvider.now();
    this.version++;
    
    // 只允許 email、lastModified 和 version 改變
    ensureAssignable(this, oldUser, "email", "lastModified", "version");
}
```

### 使用正則表達式模式

```java
public void updateProfile(ProfileData profileData) {
    var oldState = old(() -> this.clone());
    
    // 更新多個 profile 相關欄位
    this.profileName = profileData.getName();
    this.profileBio = profileData.getBio();
    this.profileAvatar = profileData.getAvatar();
    this.profileUpdatedAt = DateProvider.now();
    
    // 使用正則表達式允許所有 profile 開頭的欄位改變
    ensureAssignable(this, oldState, "profile.*", "version", "lastModified");
}
```

### 嵌套物件的驗證

```java
public void updateShippingInfo(ShippingInfo newInfo) {
    var oldOrder = old(() -> this.deepClone());
    
    // 更新嵌套物件
    this.shipping.address = newInfo.getAddress();
    this.shipping.carrier = newInfo.getCarrier();
    this.shipping.trackingNumber = newInfo.getTrackingNumber();
    this.updatedAt = DateProvider.now();
    
    // 允許 shipping 底下的所有欄位改變
    ensureAssignable(this, oldOrder, 
        "shipping\\..*",      // 注意要跳脫點號
        "updatedAt",
        "version"
    );
}
```

## 3. ensureResult() 詳細範例

### 驗證單一返回值

```java
public User findActiveUser(String userId) {
    User user = userRepository.findById(userId);
    
    // 確保返回的用戶符合條件
    return ensureResult("User must be active and verified", user, u ->
        u != null &&
        u.isActive() &&
        u.isEmailVerified() &&
        !u.isDeleted() &&
        u.getUserId().equals(userId)
    );
}
```

### 驗證集合返回值

```java
public List<Task> getTasksDueToday() {
    LocalDate today = LocalDate.now();
    List<Task> tasks = taskRepository.findByDueDate(today);
    
    // 驗證所有返回的任務都符合條件
    return ensureResult("All tasks must be due today and not completed", tasks, 
        taskList -> taskList.stream().allMatch(task ->
            task.getDueDate().equals(today) &&
            !task.isCompleted() &&
            task.isActive()
        )
    );
}
```

### 驗證計算結果

```java
public OrderSummary calculateOrderSummary(Order order) {
    Money subtotal = order.calculateSubtotal();
    Money tax = order.calculateTax();
    Money shipping = order.calculateShipping();
    Money total = subtotal.add(tax).add(shipping);
    
    OrderSummary summary = new OrderSummary(
        order.getId(),
        subtotal,
        tax,
        shipping,
        total
    );
    
    // 驗證計算結果的正確性
    return ensureResult("Order summary calculations are correct", summary, s ->
        s.getTotal().equals(s.getSubtotal().add(s.getTax()).add(s.getShipping())) &&
        s.getSubtotal().isPositiveOrZero() &&
        s.getTax().isPositiveOrZero() &&
        s.getShipping().isPositiveOrZero()
    );
}
```

## 4. ensureImmutableCollection() 詳細範例

### List 的不可變保護

```java
public List<Permission> getUserPermissions() {
    // 建立防禦性複製並返回不可變版本
    List<Permission> permissions = new ArrayList<>(userPermissions);
    
    // 可以在返回前進行排序或過濾
    permissions.sort(Comparator.comparing(Permission::getName));
    
    return ensureImmutableCollection(Collections.unmodifiableList(permissions));
}
```

### Set 的不可變保護

```java
public Set<Tag> getAssignedTags() {
    // 對於 Set，確保唯一性和不可變性
    Set<Tag> tagsCopy = new HashSet<>(assignedTags);
    
    return ensureImmutableCollection(Collections.unmodifiableSet(tagsCopy));
}
```

### Map 的不可變保護

```java
public Map<String, ProjectStatistics> getProjectStats() {
    // 建立新的 Map 並計算統計資料
    Map<String, ProjectStatistics> stats = new HashMap<>();
    
    for (Project project : projects) {
        stats.put(project.getId(), calculateStats(project));
    }
    
    return ensureImmutableCollection(Collections.unmodifiableMap(stats));
}
```

### 嵌套集合的處理

```java
public Map<Category, List<Product>> getProductsByCategory() {
    Map<Category, List<Product>> result = new HashMap<>();
    
    for (Map.Entry<Category, List<Product>> entry : productMap.entrySet()) {
        // 對內層的 List 也要做不可變處理
        List<Product> immutableProducts = Collections.unmodifiableList(
            new ArrayList<>(entry.getValue())
        );
        result.put(entry.getKey(), immutableProducts);
    }
    
    return ensureImmutableCollection(Collections.unmodifiableMap(result));
}
```

## 5. reject() 詳細範例

### 避免重複操作

```java
public void assignToProject(ProjectId projectId) {
    requireNotNull("Project ID", projectId);
    
    // 如果已經指派到該專案，提前返回
    if (reject("Already assigned to project", () -> 
        this.projectId != null && this.projectId.equals(projectId))) {
        return;
    }
    
    // 只在真正需要時才產生事件
    apply(new TaskAssignedToProject(taskId, projectId, DateProvider.now()));
}
```

### 條件性更新

```java
public void updateStatus(OrderStatus newStatus) {
    requireNotNull("New status", newStatus);
    
    // 檢查多個條件，任一滿足就提前返回
    if (reject("Invalid status transition", () -> 
        !isValidTransition(currentStatus, newStatus))) {
        return;
    }
    
    if (reject("Status unchanged", () -> currentStatus == newStatus)) {
        return;
    }
    
    if (reject("Order is locked", () -> isLocked)) {
        return;
    }
    
    // 所有檢查通過後才執行
    apply(new OrderStatusChanged(orderId, currentStatus, newStatus, DateProvider.now()));
}
```

### 複雜業務規則的提前退出

```java
public void applyDiscount(DiscountCode code) {
    requireNotNull("Discount code", code);
    
    // 使用 reject 進行一系列業務規則檢查
    if (reject("Code expired", () -> code.isExpired())) {
        return;
    }
    
    if (reject("Code already used", () -> usedDiscountCodes.contains(code))) {
        return;
    }
    
    if (reject("Minimum amount not met", () -> 
        getSubtotal().isLessThan(code.getMinimumAmount()))) {
        return;
    }
    
    if (reject("Maximum discount reached", () -> 
        getCurrentDiscountAmount().isGreaterThanOrEqual(maxDiscountAllowed))) {
        return;
    }
    
    // 通過所有檢查後應用折扣
    apply(new DiscountApplied(orderId, code, calculateDiscountAmount(code)));
}
```

## 6. check() 詳細範例

### 多步驟操作的中間檢查

```java
public void processPayment(PaymentMethod method, Money amount) {
    requireNotNull("Payment method", method);
    requireNotNull("Amount", amount);
    
    // 步驟 1: 驗證支付方式
    boolean methodValid = validatePaymentMethod(method);
    check("Payment method is valid", () -> methodValid);
    
    // 步驟 2: 檢查餘額
    Money availableBalance = getAvailableBalance();
    check("Sufficient balance", () -> availableBalance.isGreaterThanOrEqual(amount));
    
    // 步驟 3: 執行扣款
    PaymentResult result = executePayment(method, amount);
    check("Payment executed successfully", () -> result.isSuccessful());
    
    // 步驟 4: 更新餘額
    updateBalance(amount);
    check("Balance updated correctly", () -> 
        getAvailableBalance().equals(availableBalance.subtract(amount))
    );
    
    // 最終確認
    apply(new PaymentProcessed(accountId, method, amount, result.getTransactionId()));
}
```

### 迴圈中的檢查

```java
public void batchUpdateTasks(List<TaskUpdate> updates) {
    requireNotNull("Updates", updates);
    require("Updates not empty", () -> !updates.isEmpty());
    
    int processedCount = 0;
    List<TaskId> updatedTasks = new ArrayList<>();
    
    for (TaskUpdate update : updates) {
        // 每個更新前的檢查
        check("Update has valid task ID", () -> update.getTaskId() != null);
        check("Update has valid data", () -> update.isValid());
        
        Task task = findTask(update.getTaskId());
        check("Task exists", () -> task != null);
        check("Task can be updated", () -> !task.isLocked() && !task.isDeleted());
        
        // 執行更新
        task.applyUpdate(update);
        updatedTasks.add(task.getId());
        processedCount++;
        
        // 檢查更新是否成功
        check("Task updated successfully", () -> task.getLastModified().isAfter(update.getTimestamp()));
    }
    
    // 最終檢查
    check("All updates processed", () -> processedCount == updates.size());
    check("All tasks tracked", () -> updatedTasks.size() == updates.size());
}
```

### 狀態機轉換的檢查

```java
public void transitionTo(WorkflowState newState) {
    requireNotNull("New state", newState);
    
    WorkflowState oldState = this.currentState;
    
    // 檢查轉換是否有效
    Set<WorkflowState> allowedTransitions = oldState.getAllowedTransitions();
    check("Transition is allowed", () -> allowedTransitions.contains(newState));
    
    // 執行轉換前的動作
    oldState.onExit();
    check("Exit actions completed", () -> oldState.exitActionsCompleted());
    
    // 執行轉換
    this.currentState = newState;
    newState.onEnter();
    check("Enter actions completed", () -> newState.enterActionsCompleted());
    
    // 驗證轉換後的狀態
    check("State transition successful", () -> 
        this.currentState == newState && 
        this.currentState != oldState
    );
    
    // 檢查不變條件
    check("Workflow invariants maintained", () -> maintainsInvariants());
}
```

## 組合使用範例

### 完整的領域方法實現

```java
public class ShoppingCart extends EsAggregateRoot<CartId, CartEvents> {
    
    public void updateItemQuantity(ProductId productId, int newQuantity) {
        // Preconditions
        requireNotNull("Product ID", productId);
        require("Quantity is positive", () -> newQuantity > 0);
        require("Quantity within limit", () -> newQuantity <= MAX_QUANTITY_PER_ITEM);
        
        // 捕獲舊狀態
        var oldCart = old(() -> this.deepClone());
        var oldItem = old(() -> findItem(productId));
        var oldTotal = old(() -> calculateTotal());
        var oldVersion = old(() -> getVersion());
        
        // 檢查商品是否存在
        CartItem item = findItem(productId);
        check("Item exists in cart", () -> item != null);
        
        // 提前退出如果數量沒變
        if (reject("Quantity unchanged", () -> item.getQuantity() == newQuantity)) {
            return;
        }
        
        // 執行更新
        int oldQuantity = item.getQuantity();
        item.setQuantity(newQuantity);
        
        // 中間檢查
        check("Item quantity updated", () -> item.getQuantity() == newQuantity);
        
        // 重新計算總額
        Money newTotal = calculateTotal();
        check("Total recalculated", () -> !newTotal.equals(oldTotal));
        
        // 產生事件
        apply(new ItemQuantityUpdated(
            cartId,
            productId,
            oldQuantity,
            newQuantity,
            oldTotal,
            newTotal,
            UUID.randomUUID(),
            DateProvider.now()
        ));
        
        // Postconditions
        ensure("Item quantity changed", () -> 
            findItem(productId).getQuantity() == newQuantity
        );
        ensure("Total updated correctly", () -> {
            Money priceDiff = item.getPrice()
                .multiply(newQuantity - oldQuantity);
            return calculateTotal().equals(oldTotal.add(priceDiff));
        });
        ensure("Version incremented", () -> getVersion() == oldVersion + 1);
        
        // 確保只有允許的欄位改變
        ensureAssignable(this, oldCart, 
            "items\\..*quantity",
            "total",
            "lastModified",
            "version"
        );
    }
    
    public CartSummary getSummary() {
        List<CartItemDto> items = this.items.stream()
            .map(CartItemDto::from)
            .collect(Collectors.toList());
            
        CartSummary summary = new CartSummary(
            cartId,
            customerId,
            ensureImmutableCollection(Collections.unmodifiableList(items)),
            calculateTotal(),
            calculateTax(),
            calculateShipping(),
            appliedCoupons.isEmpty() ? null : appliedCoupons.get(0)
        );
        
        // 驗證返回的摘要
        return ensureResult("Cart summary is valid", summary, s ->
            s.getCartId().equals(cartId) &&
            s.getItems().size() == this.items.size() &&
            s.getTotal().equals(calculateTotal()) &&
            s.getItems().stream().noneMatch(Objects::isNull)
        );
    }
}
```

## 錯誤處理與回滾

```java
public void complexTransaction(TransactionData data) {
    var oldState = old(() -> this.fullClone());
    var checkpoint = createCheckpoint();
    
    try {
        // 步驟 1
        processStep1(data);
        check("Step 1 completed", () -> step1Completed);
        
        // 步驟 2
        processStep2(data);
        check("Step 2 completed", () -> step2Completed);
        
        // 步驟 3 - 可能失敗
        processStep3(data);
        check("Step 3 completed", () -> step3Completed);
        
        // 全部成功
        apply(new TransactionCompleted(transactionId, data));
        
    } catch (Exception e) {
        // 回滾到檢查點
        restoreFromCheckpoint(checkpoint);
        
        // 確保狀態已回滾
        ensure("State rolled back", () -> this.equals(oldState));
        ensure("No partial changes", () -> 
            !step1Completed && !step2Completed && !step3Completed
        );
        
        // 記錄失敗
        apply(new TransactionFailed(transactionId, data, e.getMessage()));
    }
}
```

這些範例展示了 uContract 進階功能在實際場景中的應用，幫助開發者寫出更健壯、更易維護的程式碼。