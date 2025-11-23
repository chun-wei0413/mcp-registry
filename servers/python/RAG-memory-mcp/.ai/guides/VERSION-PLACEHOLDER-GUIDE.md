# Version Placeholder Guide

This guide documents all placeholders that should be automatically replaced when generating code from templates.

## Configuration Source
All placeholder values come from `.dev/project-config.json`

## Available Placeholders

### Basic Project Information
- `{groupId}` → tw.teddysoft.aiscrum
- `{artifactId}` → tw.teddysoft.aiscrum  
- `{rootPackage}` → tw.teddysoft.aiscrum
- `{projectName}` → AiScrum
- `{projectVersion}` → 0.1.0-SNAPSHOT

### Java & Spring Versions
- `{javaVersion}` → 21
- `{springBootVersion}` → 3.5.3

### Dependencies
- `{ezappStarterVersion}` → 1.0.0
- `{ezdocVersion}` → 1.0.0
- `{junitVersion}` → 5.10.2
- `{junitPlatformVersion}` → 1.10.2
- `{mockitoVersion}` → 5.11.0
- `{orgJsonVersion}` → 20211205
- `{awaitilityVersion}` → 4.2.0
- `{byteBuddyVersion}` → 1.15.10

### Backend Configuration
- `{backendPort}` → 9090
- `{contextPath}` → /
- `{apiPrefix}` → /v1/api

### Database Configuration (Common)
- `{dbDriver}` → org.postgresql.Driver
- `{dbDialect}` → org.hibernate.dialect.PostgreSQLDialect

### Test Environment Database
- `{dbTestHost}` → localhost
- `{dbTestPort}` → 5800
- `{dbTestName}` → board_test
- `{dbTestUsername}` → postgres
- `{dbTestPassword}` → root
- `{dbTestUrl}` → jdbc:postgresql://localhost:5800/board_test?currentSchema=message_store
- `{dbTestSchema}` → message_store
- `{dbTestDdlAuto}` → create-drop
- `{dbTestShowSql}` → true

### Production Environment Database
- `{dbProductionHost}` → localhost
- `{dbProductionPort}` → 5500
- `{dbProductionName}` → board
- `{dbProductionUsername}` → postgres
- `{dbProductionPassword}` → root
- `{dbProductionUrl}` → jdbc:postgresql://localhost:5500/board?currentSchema=message_store
- `{dbProductionSchema}` → message_store
- `{dbProductionDdlAuto}` → update
- `{dbProductionShowSql}` → false

### AI Environment Database
- `{dbAiHost}` → localhost
- `{dbAiPort}` → 6600
- `{dbAiName}` → board_ai
- `{dbAiUsername}` → postgres
- `{dbAiPassword}` → root
- `{dbAiUrl}` → jdbc:postgresql://localhost:6600/board_ai?currentSchema=message_store
- `{dbAiSchema}` → message_store
- `{dbAiDdlAuto}` → update
- `{dbAiShowSql}` → true

### Frontend Configuration
- `{frontendFramework}` → React
- `{frontendLanguage}` → TypeScript
- `{frontendPort}` → 3000
- `{frontendBuildTool}` → Vite
- `{reactVersion}` → 18.3.1
- `{typescriptVersion}` → 5.5.3
- `{viteVersion}` → 5.4.1

### Dynamic Placeholders (Context-Dependent)
- `{fileName}` → Extracted from file name (e.g., task-delete-task → delete-task)
- `{useCaseName}` → Converted to PascalCase (e.g., delete-task → DeleteTask)
- `{aggregateName}` → Extracted from context (e.g., from .dev/tasks/feature/pbi/ → pbi)
- `{entityName}` → Extracted from context

## Placeholder Processing Rules

### When to Process Placeholders
1. **Always check for placeholders** when reading spec files or templates
2. **Read `.dev/project-config.json`** to get latest values
3. **Replace all placeholders** before generating code

### Processing Steps
1. Identify all placeholders in the template/spec
2. Load configuration from `.dev/project-config.json`
3. Replace placeholders with actual values
4. Validate all placeholders have been replaced

### Common Issues
- Missing placeholder values → Check project-config.json
- Duplicate placeholders (e.g., multiple {ezappStarterVersion}) → This is a known issue in the guide
- Context-dependent placeholders → Extract from file path or naming convention

## Usage Notes

1. **Automatic Replacement**: AI will automatically replace these placeholders when generating code
2. **Single Source of Truth**: All values come from `.dev/project-config.json`
3. **Environment-Specific**: Database configurations are environment-specific (test/production/ai)
4. **Schema Note**: All databases use `message_store` schema only (no public schema)

## Important Reminders

- Database password is `root` (not password1)
- All PostgreSQL connections use `message_store` schema
- Spring Boot application port is always 8080
- PostgreSQL container ports are:
  - Test: 5800
  - Production: 5500
  - AI: 6600