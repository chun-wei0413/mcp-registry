# Architecture-Aware Configuration

## 🏗️ Reading Architecture Configuration
All sub-agents should read `.dev/project-config.json` to determine architecture patterns:

### Configuration Structure
```javascript
{
  "architecture": {
    "defaultPattern": "inmemory" | "outbox" | "eventsourcing",
    "aggregates": {
      "Product": {
        "pattern": "outbox"  // Override default for specific aggregate
      }
    },
    "commandDefaults": {
      "dualProfileSupport": true,     // Generate both inmemory and primary pattern
      "generateOutboxPattern": true    // Auto-generate Data/Mapper for outbox
    }
  }
}
```

### How to Read Configuration
```javascript
// Step 1: Read project configuration
const config = readProjectConfig();

// Step 2: Determine pattern for specific aggregate
const aggregateName = extractAggregateFromUseCase(); // e.g., "Product" from "CreateProductUseCase"
const pattern = config.architecture.aggregates[aggregateName]?.pattern
                || config.architecture.defaultPattern;

// Step 3: Check dual profile support
const dualProfileSupport = config.architecture.commandDefaults?.dualProfileSupport || false;
```

## 🎯 Pattern-Based Code Generation

### Supported Patterns
1. **inmemory**: Use GenericInMemoryRepository only
2. **outbox**: Generate Data/Mapper classes, use OutboxRepository
3. **eventsourcing**: Use EventSourcingRepository (no Data/Mapper needed)

### Generation Logic
```javascript
if (dualProfileSupport) {
  // Always generate BOTH inmemory AND primary pattern
  generateInMemoryConfiguration();

  switch (pattern) {
    case "outbox":
      generateOutboxConfiguration();
      generateDataAndMapperClasses();
      break;
    case "eventsourcing":
      generateEventSourcingConfiguration();
      break;
  }
} else {
  // Single pattern mode
  switch (pattern) {
    case "inmemory":
      generateInMemoryConfiguration();
      break;
    case "outbox":
      generateOutboxConfiguration();
      generateDataAndMapperClasses();
      break;
    case "eventsourcing":
      generateEventSourcingConfiguration();
      break;
  }
}
```

## 📋 What to Generate Based on Pattern

### InMemory Pattern
- ✅ GenericInMemoryRepository beans
- ✅ MessageBus<DomainEvent> bean
- ❌ NO Data/Mapper classes
- ❌ NO JPA configuration

### Outbox Pattern
- ✅ OutboxRepository beans
- ✅ Data/Mapper classes (inner class pattern)
- ✅ PgMessageDbClient via JpaRepositoryFactory
- ✅ MessageBroker and MessageProducer beans
- ✅ JPA configuration with @EnableJpaRepositories

### EventSourcing Pattern
- ✅ EventSourcingRepository beans
- ✅ Event store configuration
- ❌ NO Data/Mapper classes
- ✅ Event replay mechanisms

## 🔧 Bean Registration with Dual Profile Support

### When dualProfileSupport = true
```java
// UseCaseConfiguration.java - Smart detection
@Bean
public CreateXxxUseCase createXxxUseCase(
        @Autowired(required = false) @Qualifier("xxxOutboxRepository") Repository<Xxx, XxxId> outboxRepo,
        @Autowired(required = false) @Qualifier("xxxInMemoryRepository") Repository<Xxx, XxxId> inMemoryRepo) {

    // Priority: Primary pattern > InMemory
    Repository<Xxx, XxxId> repository = outboxRepo != null ? outboxRepo : inMemoryRepo;

    if (repository == null) {
        throw new IllegalStateException("No repository bean found for Xxx");
    }

    return new CreateXxxService(repository);
}
```

### Repository Bean Naming Convention
- InMemory: `{aggregate}InMemoryRepository`
- Outbox: `{aggregate}OutboxRepository`
- EventSourcing: `{aggregate}EventSourcingRepository`

## 🎯 Configuration Files to Check
1. `.dev/project-config.json` - Architecture settings
2. `.dev/ARCHITECTURE-CONFIG-USAGE.md` - Usage guide
3. `.ai/guides/DUAL-PROFILE-CONFIGURATION-GUIDE.md` - Profile configuration

## ⚠️ Important Notes
- Always check if Data/Mapper already exists before generating
- Respect the configured pattern preferences
- When dualProfileSupport is true, ALWAYS generate both configurations
- Use @Qualifier to distinguish between different repository implementations