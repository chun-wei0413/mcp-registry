# Dual Profile Testing Configuration

## 🔴 MANDATORY: Test Generation Requirements

### When dualProfileSupport = true
**You MUST generate these 3 test files:**
1. **{UseCase}ServiceTest.java** - Main test file (using ezSpec BDD)
2. **InMemory{UseCase}TestSuite.java** - InMemory profile test suite
3. **Outbox{UseCase}TestSuite.java** - Outbox profile test suite

### ⚠️ Critical Warning
**If you don't generate all 3 files, the testing is incomplete and will fail in CI/CD!**

## 📋 Pre-Generation Checklist
- [ ] Check `.dev/project-config.json` for `dualProfileSupport` setting
- [ ] If `dualProfileSupport: true`, MUST generate all 3 test files
- [ ] Main test file has NO @ActiveProfiles annotation
- [ ] Test Suites use ProfileSetter pattern
- [ ] ProfileSetter is the FIRST class in @SelectClasses

## 🔥 ProfileSetter Pattern (The Magic Solution)

### Why ProfileSetter?
**CRITICAL DISCOVERY**: JUnit Platform Suite's static blocks DON'T execute!
- ❌ Static blocks in @Suite classes are NEVER executed
- ✅ Static blocks in @SelectClasses[0] ARE executed
- This is why we need the ProfileSetter pattern

### Implementation Template

#### InMemoryProfileSetter.java
```java
package tw.teddysoft.aiscrum.test.suite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class InMemoryProfileSetter {
    static {
        // This static block WILL execute because it's in @SelectClasses[0]
        System.setProperty("spring.profiles.active", "test-inmemory");
        System.out.println("InMemoryProfileSetter: Set profile to test-inmemory");
    }

    @Test
    void setProfile() {
        // Empty test to ensure static block execution
    }
}
```

#### OutboxProfileSetter.java
```java
package tw.teddysoft.aiscrum.test.suite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class OutboxProfileSetter {
    static {
        System.setProperty("spring.profiles.active", "test-outbox");
        System.out.println("OutboxProfileSetter: Set profile to test-outbox");
    }

    @Test
    void setProfile() {
        // Empty test to ensure static block execution
    }
}
```

### Test Suite Configuration

#### InMemoryTestSuite.java
```java
@Suite
@SuiteDisplayName("In-Memory Tests")
@SelectClasses({
    InMemoryProfileSetter.class,    // MUST be first!
    CreateProductServiceTest.class,
    // ... other test classes
})
public class InMemoryTestSuite {
    // NO static block here - it won't execute!
}
```

#### OutboxTestSuite.java
```java
@Suite
@SuiteDisplayName("Outbox Pattern Tests")
@SelectClasses({
    OutboxProfileSetter.class,      // MUST be first!
    CreateProductServiceTest.class,
    // ... same test classes, different profile
})
public class OutboxTestSuite {
    // NO static block here either!
}
```

## 🎯 How It Works
1. JUnit Platform Suite executes @SelectClasses in order
2. ProfileSetter (first class) loads and executes its static block
3. Static block sets `spring.profiles.active` system property
4. Spring Boot Test creates ApplicationContext with correct profile
5. Subsequent tests reuse the cached ApplicationContext
6. All tests run with the correct profile!

## ⚠️ Key Rules
- ✅ ProfileSetter MUST be first in @SelectClasses
- ✅ ProfileSetter MUST have @SpringBootTest annotation
- ✅ ProfileSetter MUST have at least one @Test method
- ❌ DON'T put static blocks in Suite classes (they don't execute)
- ❌ DON'T use @ActiveProfiles on test classes

## 📚 References
- **ADR-021**: `.dev/adr/ADR-021-profile-based-testing-architecture.md`
- **JUnit Suite Guide**: `.dev/lessons/JUNIT-SUITE-PROFILE-SWITCHING.md`
- **Test Templates**: `.ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/test-suites.md`