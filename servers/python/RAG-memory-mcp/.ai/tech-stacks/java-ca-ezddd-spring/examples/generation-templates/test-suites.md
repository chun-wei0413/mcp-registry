# Test Suite Templates with ProfileSetter

這些 Test Suite 模板包含 ProfileSetter inner class，用於在測試執行前強制設定正確的 Spring profile。

## 🚨 重要：ProfileSetter 的作用
ProfileSetter 是一個內部類別，透過 static block 在類別載入時設定 profile，確保整個 Test Suite 使用正確的 profile 執行。

## 1. InMemoryTestSuite - 記憶體測試套件
# 完整路徑：src/test/java/[rootPackage]/test/suite/inmemory/InMemoryTestSuite.java

```java
package [rootPackage].test.suite.inmemory;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.*;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test Suite for running all tests with in-memory repositories.
 * 
 * This suite runs all UseCase tests with in-memory implementations.
 * These tests are:
 * - Fast execution (no database required)
 * - Good for rapid development and CI/CD
 * - Isolated from external dependencies
 * 
 * Usage:
 * - Run directly from IDE: Right-click and "Run InMemoryTestSuite"
 * - Maven: mvn test -Dtest=InMemoryTestSuite
 * - Maven with profile: mvn test -Ptest-inmemory
 */
@Suite
@SuiteDisplayName("InMemory Tests")
@SelectClasses({
    InMemoryTestSuite.ProfileSetter.class     // MUST be first to set profile!
})
@SelectPackages({
    "[rootPackage]"  // Select all packages under root
})
@IncludeClassNamePatterns({
    ".*UseCaseTest",
    ".*ControllerTest",
    ".*IntegrationTest"
})
@ExcludeTags({"outbox", "slow", "integration"})
public class InMemoryTestSuite {
    
    /**
     * Inner class that sets the profile for this test suite.
     * Must be the first class in @SelectClasses to ensure profile is set before any tests run.
     * 
     * This is a critical component for profile-based testing architecture.
     * The static block runs when the class is loaded, before any Spring context initialization.
     */
    @SpringBootTest
    public static class ProfileSetter {
        static {
            System.setProperty("spring.profiles.active", "test-inmemory");
            System.out.println("========================================");
            System.out.println("InMemoryTestSuite.ProfileSetter: Set spring.profiles.active=test-inmemory");
            System.out.println("========================================");
        }
        
        @Test
        void setProfile() {
            // Empty test to ensure static block runs
            System.out.println("InMemoryTestSuite profile is set to test-inmemory");
        }
    }
}
```

## 2. OutboxTestSuite - Outbox Pattern 測試套件
# 完整路徑：src/test/java/[rootPackage]/test/suite/outbox/OutboxTestSuite.java

```java
package [rootPackage].test.suite.outbox;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.*;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test Suite for running all tests with Outbox pattern and PostgreSQL.
 * 
 * This suite runs all UseCase tests with real PostgreSQL and Outbox pattern.
 * These tests are:
 * - Use real database (PostgreSQL on port 5800)
 * - Test event sourcing and transactional consistency
 * - Ensure reliable event publishing
 * - Good for integration testing
 * 
 * Requirements:
 * - PostgreSQL must be running on port 5800
 * - Database: [projectName]_test (根據 project-config.json)
 * - User/Password: postgres/mysecretpassword
 * 
 * Usage:
 * - Run directly from IDE: Right-click and "Run OutboxTestSuite"
 * - Maven: mvn test -Dtest=OutboxTestSuite
 * - Maven with profile: mvn test -Ptest-outbox
 * - With Spring profile: mvn test -Dtest=OutboxTestSuite -Dspring.profiles.active=test-outbox
 * 
 * IMPORTANT: These tests run the SAME test classes as InMemoryTestSuite,
 * but with different profile to use PostgreSQL instead of in-memory repositories.
 * This demonstrates the power of profile-based testing:
 * Write once, test with different implementations!
 */
@Suite
@SuiteDisplayName("Outbox Pattern Tests")
@SelectClasses({
    OutboxTestSuite.ProfileSetter.class     // MUST be first to set profile!
})
@SelectPackages({
    "[rootPackage]"  // Select all packages under root
})
@IncludeClassNamePatterns({
    ".*UseCaseTest",
    ".*ControllerTest",
    ".*IntegrationTest",
    ".*OutboxRepositoryTest"
})
@ExcludeTags({"inmemory", "unit"})
public class OutboxTestSuite {
    
    /**
     * Inner class that sets the profile for this test suite.
     * Must be the first class in @SelectClasses to ensure profile is set before any tests run.
     * 
     * This is a critical component for profile-based testing architecture.
     * The static block runs when the class is loaded, before any Spring context initialization.
     */
    @SpringBootTest
    public static class ProfileSetter {
        static {
            System.setProperty("spring.profiles.active", "test-outbox");
            System.out.println("========================================");
            System.out.println("OutboxTestSuite.ProfileSetter: Set spring.profiles.active=test-outbox");
            System.out.println("========================================");
        }
        
        @Test
        void setProfile() {
            // Empty test to ensure static block runs
            System.out.println("OutboxTestSuite profile is set to test-outbox");
        }
    }
}
```

## 3. UseCaseTestSuite - 只執行 UseCase 測試
# 完整路徑：src/test/java/[rootPackage]/test/suite/UseCaseTestSuite.java

```java
package [rootPackage].test.suite;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.*;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test Suite for running only UseCase tests.
 * 
 * This suite specifically targets UseCase tests only, excluding controllers and integration tests.
 * Profile can be controlled via environment variable or Maven profile.
 * 
 * Usage:
 * - Default (inmemory): mvn test -Dtest=UseCaseTestSuite
 * - With outbox: SPRING_PROFILES_ACTIVE=test-outbox mvn test -Dtest=UseCaseTestSuite
 */
@Suite
@SuiteDisplayName("UseCase Tests Only")
@SelectPackages({
    "[rootPackage]"
})
@IncludeClassNamePatterns(".*UseCaseTest")
@ExcludeClassNamePatterns({
    ".*ControllerTest",
    ".*IntegrationTest",
    ".*RepositoryTest"
})
public class UseCaseTestSuite {
    // No ProfileSetter - uses default profile from environment
    // This allows flexible profile switching without code changes
}
```

## 使用說明

### 1. ProfileSetter Inner Class 的重要性
- **必須是 @SelectClasses 中的第一個類別**
- 透過 static block 在類別載入時設定 profile
- 確保在 Spring context 初始化前設定正確的 profile
- 包含空的 @Test 方法確保 static block 執行

### 2. Test Suite 的好處
- 可以選擇性執行特定 profile 的測試
- 相同的測試可以在不同 profile 下執行
- 方便 CI/CD 整合
- 提供清晰的測試組織結構

### 3. Profile 切換機制優先順序
1. Test Suite 的 ProfileSetter（最高優先）
2. 環境變數 SPRING_PROFILES_ACTIVE
3. Maven profile 設定
4. application-test.yml 預設值

### 4. 與 BaseUseCaseTest 的關係
- Test Suite 設定 profile
- BaseUseCaseTest 偵測 profile 並調整行為
- 測試類別不需要知道 profile 細節

## 重要提醒

### ⚠️ 絕對不要：
- 在測試類別上使用 @ActiveProfiles
- 在 BaseUseCaseTest 上使用 @ActiveProfiles
- 硬編碼 profile 在測試邏輯中

### ✅ 應該要：
- 使用 Test Suite 控制 profile
- 讓測試支援多個 profiles
- 透過 ProfileSetter inner class 設定 profile

## 佔位符說明
- `[rootPackage]`: 從 .dev/project-config.json 取得
- `[projectName]`: 從 .dev/project-config.json 取得

這些模板確保 AI 能產生正確的 Test Suite，包含關鍵的 ProfileSetter inner class。