# Mandatory References for Sub-agents

## 🔥 Core Framework References
These documents MUST be read by all sub-agents before implementation:

1. **🔴 Framework API Integration Guide**
   - Path: `.ai/guides/FRAMEWORK-API-INTEGRATION-GUIDE.md`
   - Content: PgMessageDbClient creation, OutboxMapper rules, Jakarta persistence migration
   - Critical for: Outbox pattern, JPA configuration

2. **🔴 ezapp-starter API Reference**
   - Path: `.ai/guides/EZAPP-STARTER-API-REFERENCE.md`
   - Content: Complete framework API with correct import paths
   - Critical for: All framework class imports

3. **🔧 Dual-Profile Configuration Guide**
   - Path: `.ai/guides/DUAL-PROFILE-CONFIGURATION-GUIDE.md`
   - Content: InMemory vs Outbox profile configuration
   - Critical for: Profile-based bean configuration

4. **📋 Version Placeholder Guide**
   - Path: `.ai/guides/VERSION-PLACEHOLDER-GUIDE.md`
   - Content: Automatic placeholder replacement rules
   - Critical for: pom.xml and template usage

## 📚 Architecture & Standards

5. **Coding Standards**
   - Path: `.ai/tech-stacks/java-ca-ezddd-spring/coding-standards.md`
   - Content: Complete coding standards and conventions
   - Critical for: Code consistency

6. **Spring Boot Configuration Checklist**
   - Path: `.ai/tech-stacks/java-ca-ezddd-spring/SPRING-BOOT-CONFIGURATION-CHECKLIST.md`
   - Content: Common configuration mistakes and solutions
   - Critical for: Avoiding configuration errors

## 🧪 Testing References

7. **ADR-021 Profile-Based Testing**
   - Path: `.dev/adr/ADR-021-profile-based-testing-architecture.md`
   - Content: Why @ActiveProfiles is forbidden
   - Critical for: Test configuration

8. **JUnit Suite Profile Switching**
   - Path: `.dev/lessons/JUNIT-SUITE-PROFILE-SWITCHING.md`
   - Content: ProfileSetter pattern explanation
   - Critical for: Dual-profile test suites

9. **Spring DI Test Guide**
   - Path: `.ai/guides/SPRING-DI-TEST-GUIDE.md`
   - Content: Spring Dependency Injection in tests
   - Critical for: Test implementation

## 📂 Template & Example References

10. **pom.xml Template**
    - Path: `.ai/tech-stacks/java-ca-ezddd-spring/examples/pom/pom.xml`
    - Content: Verified pom.xml configuration
    - Critical for: Project setup

11. **Spring Configuration Templates**
    - Path: `.ai/tech-stacks/java-ca-ezddd-spring/examples/spring/`
    - Content: Spring configuration examples
    - Critical for: Configuration classes

12. **UseCase Injection Template**
    - Path: `.ai/tech-stacks/java-ca-ezddd-spring/examples/use-case-injection/README.md`
    - Content: Profile-based repository switching
    - Critical for: Dependency injection

13. **Local Utils Template**
    - Path: `.ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md`
    - Content: Common utility classes (DateProvider, GenericInMemoryRepository, etc.)
    - Critical for: Fresh project initialization

## 🎯 Specialized References by Sub-agent Type

### For Command Sub-agent:
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/command/`
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/aggregate/`

### For Query Sub-agent:
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/projection/`
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/inquiry-archive/`

### For Reactor Sub-agent:
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/reactor-full.md`
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/reference/reactor-pattern-guide.md`

### For Aggregate Sub-agent:
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/aggregate/`
- `.ai/checklists/AGGREGATE-IDENTIFICATION-CHECKLIST.md`

### For Test Generation:
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/test/`
- `.ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/test-suites.md`

## ⚠️ Important Note
**When in doubt, always check the specific sub-agent prompt for additional specialized references.**