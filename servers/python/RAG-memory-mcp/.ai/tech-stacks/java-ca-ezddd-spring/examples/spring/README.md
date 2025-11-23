# Spring Boot Configuration Examples

This directory contains Spring Boot configuration templates for new projects using the ezapp-starter framework.

## Files Overview

### 1. **AiScrumApp.java**
Main Spring Boot application class template with:
- Message broker and producer beans for domain events
- PgMessageDbClient configuration for Outbox pattern
- EzesCatchUpRelay for event synchronization
- Profile-based configuration support

### 2. **InMemoryRepositoryConfig.java**
Configuration for InMemory repositories:
- Activated with `inmemory` or `test-inmemory` profiles
- Uses `GenericInMemoryRepository` for all entities
- No database dependencies
- Ideal for unit testing and development

### 3. **OutboxRepositoryConfig.java** 🆕
Configuration for Outbox pattern repositories:
- Activated with `outbox` or `test-outbox` profiles
- Integrates with PostgreSQL via PgMessageDbClient
- Ensures reliable event publishing with transactional outbox
- Includes implementation checklist for each aggregate

### 4. **UseCaseConfiguration.java**
Central configuration for all use case beans:
- Profile-aware repository injection
- Constructor-based dependency injection
- Supports both InMemory and Outbox profiles
- Single source of truth for use case wiring

### 5. **application.properties**
Base Spring Boot configuration:
- Common settings across all profiles
- JPA and Hibernate configuration
- Logging settings

### 6. **application-inmemory.properties**
InMemory profile specific settings:
- No database configuration
- Repository type set to `inmemory`
- Lightweight configuration for testing

### 7. **application-outbox.properties**
Outbox profile specific settings:
- PostgreSQL database configuration
- Connection pooling settings
- JPA/Hibernate for production use

## Usage Guide

### For New Projects

1. **Copy all files** to your project's appropriate locations:
   - Java files → `src/main/java/{your.package}/io/springboot/config/`
   - Properties files → `src/main/resources/`

2. **Replace placeholders**:
   - `{rootPackage}` → Your base package (e.g., `tw.teddysoft.myapp`)
   - Example entities → Your domain entities

3. **Choose your profile**:
   - Development/Testing: Use `inmemory` profile
   - Production: Use `outbox` profile

### Profile Selection

```bash
# Run with InMemory profile
mvn spring-boot:run -Dspring.profiles.active=inmemory

# Run with Outbox profile
mvn spring-boot:run -Dspring.profiles.active=outbox

# Run tests with specific profile
mvn test -Dspring.profiles.active=test-inmemory
mvn test -Dspring.profiles.active=test-outbox
```

## Implementation Flow

### For InMemory Profile
1. Copy `InMemoryRepositoryConfig.java`
2. Add repository beans for your entities
3. Use `GenericInMemoryRepository` implementation
4. No additional data classes needed

### For Outbox Profile
1. Copy `OutboxRepositoryConfig.java`
2. For each aggregate:
   - Create `{Entity}OrmClient` class
   - Create `{Entity}Data` JPA entity
   - Create `{Entity}Mapper` with inner `OutboxMapper`
   - Add repository bean definition
3. Configure PostgreSQL connection in properties
4. Test with PostgreSQL on port 5432 (prod) or 5800 (test)

## Important Notes

- **ezapp-starter** already includes all EZDDD framework components
- No need to add individual ezddd dependencies
- Import paths remain the same (`tw.teddysoft.ezddd.*`)
- Always use `@Primary` annotation on Outbox repository beans
- Follow ADR-019 for OutboxMapper implementation (must be inner class)

## See Also

- [Outbox Pattern Examples](../outbox/)
- [Use Case Injection Examples](../use-case-injection/)
- [Local Utilities](../generation-templates/local-utils.md)