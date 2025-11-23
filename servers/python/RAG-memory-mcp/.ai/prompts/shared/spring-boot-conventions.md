# Spring Boot Conventions for Sub-agents

## Main Application Class Rules

### 1. Location Convention
```
CORRECT:
src/main/java/tw/teddysoft/aiscrum/AiScrumApp.java (根套件)

WRONG:
src/main/java/tw/teddysoft/aiscrum/io/springboot/AiScrumApp.java (子套件)
src/main/java/tw/teddysoft/aiscrum/AiScrumApplication.java (重複類別)
```

### 2. Naming Convention
- **唯一名稱**: 專案中只能有一個 `@SpringBootApplication` 類別
- **標準名稱**: `{ProjectName}App.java` 或 `{ProjectName}Application.java`
- **AI-SCRUM 專案**: 使用 `AiScrumApp.java`

### 3. Package Scanning
```java
@SpringBootApplication(
    scanBasePackages = "tw.teddysoft.aiscrum"  // 明確指定掃描範圍
)
public class AiScrumApp {
    public static void main(String[] args) {
        SpringApplication.run(AiScrumApp.class, args);
    }
}
```

### 4. Test Configuration
- 測試類別會自動找到主類別（如果在正確位置）
- 不需要在 `@SpringBootTest` 明確指定 `classes`
- 使用 `@SpringBootTest` 而非 `@SpringBootTest(classes = ...)`

## Pre-Generation Checklist

Before generating ANY Spring Boot configuration:

1. **Check existing main class**:
   ```bash
   find . -name "*App.java" -o -name "*Application.java" | grep -E "@SpringBootApplication"
   ```

2. **Verify package structure**:
   - Main class should be in root package: `tw.teddysoft.aiscrum`
   - Not in sub-packages like `io.springboot` or `config`

3. **Avoid duplicates**:
   - Never create multiple `@SpringBootApplication` classes
   - If exists, reuse or modify, don't create new

## Profile Configuration

### Test Profile Setup
```properties
# src/test/resources/application.properties
spring.profiles.active=test-inmemory

# src/main/resources/application.properties
# Don't set default profile here for tests to override
```

### Priority Order
1. Test resources (`src/test/resources/application.properties`)
2. Environment variable (`SPRING_PROFILES_ACTIVE`)
3. Main resources (`src/main/resources/application.properties`)

## Common Mistakes to Avoid

### ❌ DON'T: Create multiple main classes
```java
// DON'T DO THIS
tw.teddysoft.aiscrum.AiScrumApp
tw.teddysoft.aiscrum.io.springboot.AiScrumApp
tw.teddysoft.aiscrum.AiScrumApplication
```

### ❌ DON'T: Put main class in sub-package
```java
// WRONG
package tw.teddysoft.aiscrum.io.springboot;
@SpringBootApplication
public class AiScrumApp { }
```

### ✅ DO: Single main class in root package
```java
// CORRECT
package tw.teddysoft.aiscrum;
@SpringBootApplication(scanBasePackages = "tw.teddysoft.aiscrum")
public class AiScrumApp { }
```

## Integration with Sub-agents

All sub-agents should:
1. Check for existing main class before generation
2. Use the standard location and naming
3. Never create duplicates
4. Include this reference in their shared imports