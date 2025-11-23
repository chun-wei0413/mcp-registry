# Value Object Contract 範例

## ⚠️ 重要：Value Object 驗證方式

**Value Objects 必須使用 `Objects.requireNonNull()`，不是 `Contract.requireNotNull()`！**

| 類型 | 正確的驗證方式 | 原因 |
|------|--------------|------|
| **Aggregate Root** | `Contract.requireNotNull()` | 使用 DBC 框架進行契約驗證 |
| **Entity** | `Objects.requireNonNull()` | 使用標準 Java 驗證 |
| **Value Object** | `Objects.requireNonNull()` | 使用標準 Java 驗證 |

## 概述

Value Object 的設計著重於不變性（immutability）和有效性驗證。本文提供各種 Value Object 的正確實作範例。

## 基本 Value Object Contract

### 範例 1：TagId

```java
import java.util.Objects;
import java.util.UUID;

public record TagId(String value) implements ValueObject {
    
    public TagId {
        // Value Object 使用 Objects.requireNonNull (不是 Contract)
        Objects.requireNonNull(value, "TagId value cannot be null");
        
        // 使用標準 Java 驗證
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("TagId value cannot be empty");
        }
        if (value.length() < 1 || value.length() > 50) {
            throw new IllegalArgumentException("TagId value length must be between 1 and 50");
        }
        if (!value.matches("^[a-zA-Z0-9\\-_]+$")) {
            throw new IllegalArgumentException("TagId value contains invalid characters");
        }
    }
    
    public static TagId valueOf(String value) {
        Objects.requireNonNull(value, "Value for TagId cannot be null");
        return new TagId(value);
    }
    
    public static TagId generate() {
        String uuid = UUID.randomUUID().toString();
        return new TagId(uuid);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### 範例 2：Money

```java
import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public final class Money implements ValueObject, Comparable<Money> {
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money(BigDecimal amount, Currency currency) {
        // Value Object 使用 Objects.requireNonNull
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        
        // 使用標準 Java 驗證
        if (amount.scale() > currency.getDefaultFractionDigits()) {
            throw new IllegalArgumentException("Amount scale exceeds currency precision");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        
        
        // 正規化金額（設定正確的小數位數）
        this.amount = amount.setScale(
            currency.getDefaultFractionDigits(), 
            RoundingMode.HALF_UP
        );
    }
    
    public static Money of(double amount, String currencyCode) {
        Objects.requireNonNull(currencyCode, "Currency code cannot be null");
        if (currencyCode.length() != 3) {
            throw new IllegalArgumentException("Currency code must be 3 characters");
        }
        
        try {
            Currency currency = Currency.getInstance(currencyCode);
            BigDecimal bdAmount = BigDecimal.valueOf(amount);
            
            return new Money(bdAmount, currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid currency code: " + currencyCode, e);
        }
    }
    
    public Money add(Money other) {
        // 驗證參數
        Objects.requireNonNull(other, "Other money cannot be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        
        
        // 執行運算
        BigDecimal newAmount = this.amount.add(other.amount);
        
        // 建立新物件
        return new Money(newAmount, this.currency);
    }
    
    public Money subtract(Money other) {
        // 驗證參數
        Objects.requireNonNull(other, "Other money cannot be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }
        if (this.amount.compareTo(other.amount) < 0) {
            throw new IllegalArgumentException("Insufficient amount for subtraction");
        }
        
        
        // 執行運算
        BigDecimal newAmount = this.amount.subtract(other.amount);
        
        // 建立新物件
        return new Money(newAmount, this.currency);
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Other money cannot be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare money with different currencies");
        }
        return this.amount.compareTo(other.amount);
    }
}
```

## 複雜 Value Object Contract

### 範例 3：Email

```java
import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) implements ValueObject {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    public Email {
        // Value Object 使用 Objects.requireNonNull
        Objects.requireNonNull(value, "Email value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Email value cannot be empty");
        }
        
        
        // 正規化：轉換為小寫並去除空白
        value = value.trim().toLowerCase();
        
        // 使用標準 Java 驗證
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Email format is invalid");
        }
        if (value.length() < 5 || value.length() > 254) {
            throw new IllegalArgumentException("Email length must be between 5 and 254 characters");
        }
        
        
        // 進階驗證
        String[] parts = value.split("@");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Email must contain exactly one @ symbol");
        }
        
        String localPart = parts[0];
        String domainPart = parts[1];
        
        if (localPart.length() < 1 || localPart.length() > 64) {
            throw new IllegalArgumentException("Email local part must be between 1 and 64 characters");
        }
        if (domainPart.length() < 3 || !domainPart.contains(".")) {
            throw new IllegalArgumentException("Email domain part is invalid");
        }
        if (domainPart.startsWith(".") || domainPart.endsWith(".")) {
            throw new IllegalArgumentException("Email domain cannot start or end with a dot");
        }
    }
    
    public String getLocalPart() {
        int atIndex = value.indexOf('@');
        if (atIndex <= 0) {
            throw new IllegalStateException("Invalid email format");
        }
        return value.substring(0, atIndex);
    }
    
    public String getDomain() {
        int atIndex = value.indexOf('@');
        if (atIndex <= 0 || atIndex >= value.length() - 1) {
            throw new IllegalStateException("Invalid email format");
        }
        return value.substring(atIndex + 1);
    }
    
    public static Email valueOf(String value) {
        return new Email(value);
    }
}
```

### 範例 4：DateRange

```java
import java.util.Objects;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record DateRange(LocalDate startDate, LocalDate endDate) implements ValueObject {
    
    public DateRange {
        // Value Object 使用 Objects.requireNonNull
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
        
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 365 * 10) {
            throw new IllegalArgumentException("Date range cannot exceed 10 years");
        }
        
        // Postconditions 在 record 中自動處理
    }
    
    public static DateRange of(String startDateStr, String endDateStr) {
        Objects.requireNonNull(startDateStr, "Start date string cannot be null");
        Objects.requireNonNull(endDateStr, "End date string cannot be null");
        
        LocalDate start = LocalDate.parse(startDateStr);
        LocalDate end = LocalDate.parse(endDateStr);
        
        return new DateRange(start, end);
    }
    
    public boolean contains(LocalDate date) {
        Objects.requireNonNull(date, "Date to check cannot be null");
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    public boolean overlaps(DateRange other) {
        Objects.requireNonNull(other, "Other date range cannot be null");
        return !this.endDate.isBefore(other.startDate) && 
               !other.endDate.isBefore(this.startDate);
    }
    
    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    public DateRange extend(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("Days cannot be negative");
        }
        LocalDate newEndDate = endDate.plusDays(days);
        return new DateRange(startDate, newEndDate);
    }
}
```

## 組合 Value Object

### 範例 5：Address

```java
import java.util.Objects;
import java.util.Map;

public record Address(
    String street,
    String city,
    String state,
    String zipCode,
    String country
) implements ValueObject {
    
    public Address {
        // Value Object 使用 Objects.requireNonNull
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(state, "State cannot be null");
        Objects.requireNonNull(zipCode, "Zip code cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");
        
        // 使用標準 Java 驗證
        if (street.trim().isEmpty()) {
            throw new IllegalArgumentException("Street cannot be empty");
        }
        if (city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be empty");
        }
        if (state.trim().isEmpty()) {
            throw new IllegalArgumentException("State cannot be empty");
        }
        if (zipCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Zip code cannot be empty");
        }
        if (country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be empty");
        }
        
        
        // 正規化
        street = street.trim();
        city = city.trim();
        state = state.trim().toUpperCase();
        zipCode = zipCode.trim();
        country = country.trim().toUpperCase();
        
        // 格式驗證
        if (state.length() != 2) {
            throw new IllegalArgumentException("State code must be 2 characters");
        }
        if (country.length() != 2) {
            throw new IllegalArgumentException("Country code must be 2 characters");
        }
        if (country.equals("US") && !zipCode.matches("\\d{5}(-\\d{4})?")) {
            throw new IllegalArgumentException("Invalid US zip code format");
        }
    }
    
    public static Address of(Map<String, String> components) {
        Objects.requireNonNull(components, "Address components cannot be null");
        
        if (!components.containsKey("street") ||
            !components.containsKey("city") ||
            !components.containsKey("state") ||
            !components.containsKey("zipCode") ||
            !components.containsKey("country")) {
            throw new IllegalArgumentException("Missing required address components");
        }
            
        return new Address(
            components.get("street"),
            components.get("city"),
            components.get("state"),
            components.get("zipCode"),
            components.get("country")
        );
    }
    
    public String getFullAddress() {
        return String.format("%s, %s, %s %s, %s",
            street, city, state, zipCode, country);
    }
}
```

## Value Object 設計要點

### 1. 驗證方式（最重要）
- **必須使用** `Objects.requireNonNull()` 進行 null 檢查
- **不要使用** `Contract.requireNotNull()`（那是給 Aggregate Root 用的）
- 使用標準 Java 例外：`IllegalArgumentException`
- 提供清晰的錯誤訊息

### 2. 不變性保證
- 使用 `record` 或確保所有欄位都是 `final`
- 沒有 setter 方法
- 修改操作返回新物件
- 原物件保持不變

### 3. 驗證完整性
- 建構時驗證所有規則
- 拒絕無效狀態
- 提供清晰的錯誤訊息
- 考慮邊界情況

### 4. 正規化
- 統一大小寫
- 去除多餘空白
- 標準化格式
- 確保一致性

### 5. 工廠方法
- 提供 `valueOf()` 或 `of()` 方法
- 封裝複雜的建構邏輯
- 驗證輸入參數
- 返回有效的物件

## 正確的範例模板

```java
import java.util.Objects;  // 重要：使用 Objects，不是 Contract！

public record MyValueObject(String value) implements ValueObject {
    
    public MyValueObject {
        // ✅ 正確：使用 Objects.requireNonNull
        Objects.requireNonNull(value, "Value cannot be null");
        
        // ✅ 正確：使用標準 Java 驗證
        if (value.isBlank()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }
    }
    
    public static MyValueObject valueOf(String value) {
        return new MyValueObject(value);
    }
}
```

## 總結

良好的 Value Object 設計能：
- 保證物件的不變性
- 防止無效狀態
- 提供清晰的 API
- 簡化測試
- 提高程式碼可靠性
- **遵循正確的驗證規範**（使用 `Objects.requireNonNull`）