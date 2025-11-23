# Common Rules for All Sub-agents

## 🔴 Critical Rules (MUST FOLLOW)

### ❌ ABSOLUTELY FORBIDDEN
1. **NEVER add comments** in code (unless explicitly requested by user)
2. **NEVER add System.out.println or debug logging** - no debug output allowed
3. **NEVER use @Component or @Service** annotations on Service classes - use @Bean in Configuration instead
4. **NEVER hardcode Spring profiles** - profiles must be dynamically configurable
5. **NEVER use javax.persistence** - always use jakarta.persistence
6. **NEVER add @ActiveProfiles to test classes** - let application-test.yml or environment decide

### ✅ ALWAYS REQUIRED
1. **ALWAYS use requireNotNull** for contract checks (from tw.teddysoft.ucontract.Contract)
2. **ALWAYS use proper Value Objects** (e.g., ProductId.valueOf(), not new ProductId())
3. **ALWAYS return proper response types** (CqrsOutput for commands, DTOs for queries)
4. **ALWAYS wrap exceptions** in appropriate exception types (UseCaseFailureException for use cases)
5. **ALWAYS check if entity exists** before operations
6. **ALWAYS register Service as @Bean** in Configuration classes (not @Component)

### 📦 Framework Import Rules
- **ALWAYS use ezapp-starter imports** - All EZDDD framework classes are provided through ezapp-starter
- **NO separate ezddd-core, ezcqrs dependencies** - ezapp-starter includes everything
- **Reference**: `.ai/guides/EZAPP-STARTER-API-REFERENCE.md` for correct import paths

### 🧪 Testing Rules
- **ALWAYS use ezSpec BDD framework** for tests - Plain @Test is FORBIDDEN
- **ALWAYS extend BaseUseCaseTest** without @ActiveProfiles annotation
- **ALWAYS support dual-profile testing** (test-inmemory and test-outbox)
- **NEVER create TestContext manually** - use @SpringBootTest with @Autowired
- **NEVER call super.setUpXXX() in @BeforeEach** - JUnit handles parent methods automatically
- **Reference test patterns**: `.ai/prompts/shared/test-base-class-patterns.md`

### 📝 Code Style Rules
- **NO unnecessary comments** - code should be self-documenting
- **NO debug output** - use proper logging if needed
- **NO static factory methods for Aggregates** - use public constructors
- **USE record for Value Objects** - with proper validation in compact constructor
## 🌐 CORS Configuration (Frontend-Backend Integration)

### When Frontend Calls Backend API

**Problem**: Browser blocks requests with CORS error:
```
Access to fetch at 'http://localhost:9090/v1/api/...' from origin 'http://localhost:5173' 
has been blocked by CORS policy
```

**Solution**: Backend needs `CorsConfig.java`

### Required Backend Configuration

**File**: `src/main/java/{package}/config/CorsConfig.java`

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedHeaders(Arrays.asList(
            "Origin", "Content-Type", "Accept", "Authorization", 
            "Idempotency-Key", "X-Requested-With"
        ));
        config.setExposedHeaders(Arrays.asList(
            "Location", "Operation-Id", "traceId"
        ));
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/v1/api/**", config);
        return new CorsFilter(source);
    }
}
```

### Sub-Agent Responsibilities

**Controller Sub-Agent**: When generating first controller, check if CorsConfig exists:
- ✅ If exists: No action needed
- ❌ If missing: Inform user to create CorsConfig

**Frontend Sub-Agent**: When generating frontend API calls:
- ✅ Always check backend has CorsConfig
- ❌ If missing: Provide template and remind user

**Reference**: `.ai/guides/CORS-SETUP.md` (通用 CORS 設定指南)

### Quick Verification

```bash
# Test CORS is working
curl -X OPTIONS http://localhost:9090/v1/api/products/test/pbis \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -v | grep "Access-Control-Allow-Origin"
```

Expected: `Access-Control-Allow-Origin: http://localhost:5173`
