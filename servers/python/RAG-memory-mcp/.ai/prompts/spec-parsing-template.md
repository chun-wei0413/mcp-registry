# Spec Parsing Template for AI Assistant

## Instructions for AI
When given a spec file, ALWAYS follow this parsing procedure:

### Step 1: Extract ALL Components
Parse the spec and create a comprehensive list:

```markdown
## Components to Implement from Spec

### 1. Use Cases
- [ ] List all use cases mentioned

### 2. Services  
- [ ] List all services needed

### 3. DTOs (dataTransferObjects)
- [ ] List each DTO from spec

### 4. Projections
- [ ] List projection interfaces
- [ ] List projection implementations  

### 5. Mappers (🚨 CRITICAL - OFTEN MISSED!)
- [ ] **MUST CHECK**: Does spec have a "mappers" section?
- [ ] List all mappers from spec
- [ ] Note package location: `[aggregate].usecase.port` (NOT adapter!)
- [ ] Note: Mappers convert between entities and DTOs
- [ ] **WARNING**: If spec says "必須要產生" or "critical": true, this is MANDATORY!

### 6. Repositories
- [ ] List any custom repositories

### 7. Tests
- [ ] List required test files
```

### Step 2: Create Implementation Order
1. Domain entities/value objects (if needed)
2. DTOs
3. **Mappers** (do NOT skip!)
4. Use Case interfaces
5. Projections (interface then implementation)
6. Services
7. Tests

### Step 3: Validation Checklist
Before marking task complete:
- [ ] All DTOs from spec created?
- [ ] All Mappers from spec created?
- [ ] All Projections created (both interface AND implementation)?
- [ ] Mapper is injected and used in Projection?
- [ ] All tests passing?

## Common Mistakes to Avoid
1. ❌ Implementing DTO conversion directly in Projection instead of using Mapper
2. ❌ Forgetting to create Mapper when spec mentions it
3. ❌ Creating only Projection interface without implementation
4. ❌ Missing DTOs mentioned in spec

## Example Spec Analysis
Given a spec with:
```json
{
  "mappers": [
    {"name": "ProductMapper", "description": "..."}
  ],
  "projections": [...],
  "dataTransferObjects": [...]
}
```

MUST create:
1. ✅ ProductMapper class
2. ✅ Use ProductMapper in Projection implementation
3. ✅ All DTOs listed
4. ✅ Both Projection interface AND implementation
