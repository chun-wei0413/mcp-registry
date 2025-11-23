# Template-Based Code Generation Workflow

## Overview
Generate code using predefined templates and patterns specific to your project's technology stack.
This workflow integrates with the **Sub-agent System** for high-quality code generation.

## 🤖 Sub-agent Integration
- Uses **Code Generation Agent** for production code
- Uses **Test Generation Agent** for test code
- Uses **Code Review Agent** for quality assurance

## When to Use
- Creating new components that follow established patterns
- Ensuring consistency across codebase
- Speeding up repetitive code creation
- Onboarding new team members with standard patterns

## Prerequisites
- Technology stack identified (check `tech-stacks/` directory)
- Templates available for your tech stack
- Understanding of your project's architecture

## Workflow

### 1. Identify Code Type
Determine what type of code you need:
- API endpoints
- Database models
- Service classes
- Test files
- Configuration files
- UI components

### 2. Locate Appropriate Template
Check if templates exist in your tech stack:
```
tech-stacks/
  your-tech-stack/
    codegen/
      templates/
    examples/
```

### 3. Gather Requirements
Collect necessary information:
- Component name
- Input/output data structures
- Business logic requirements
- Validation rules
- Error handling needs

### 4. Generate Code

#### Option A: Use Code Generation Sub-agent (👍 Recommended)
**Command**: 
```
"請啟動 Code Generation Sub-agent 根據 [template-name] 產生 [component-name]"
```

**Sub-agent Process**:
1. Load template and coding standards
2. Generate production-ready code
3. Ensure all conventions are followed
4. Apply domain-specific patterns

#### Option B: Manual Template Usage
1. Copy the template structure
2. Replace placeholders with actual values
3. Adapt logic to specific requirements
4. Ensure naming conventions are followed

### 5. Customize and Extend
- Add specific business logic
- Include additional validations
- Integrate with existing code
- Add appropriate error handling

### 5.1 Generate Tests with Test Generation Sub-agent
**Command**:
```
"請啟動 Test Generation Sub-agent 為產生的程式碼建立測試"
```

**Sub-agent will**:
- Generate comprehensive test cases
- Use appropriate testing frameworks (ezSpec for Use Cases)
- Ensure test coverage requirements
- Verify all tests pass

### 6. Verify Generated Code

#### Use Code Review Sub-agent
**Command**:
```
"請啟動 Code Review Sub-agent 審查產生的程式碼"
```

**Review Process**:
- Check syntax and compilation
- Ensure it follows project standards
- Verify integration points
- Validate imports and dependencies
- Identify potential issues
- Suggest improvements

## Template Variables
Common placeholders in templates:
- `{ComponentName}` - Name of the component
- `{EntityName}` - Domain entity name
- `{Package}` - Package/namespace
- `{Properties}` - List of properties/fields
- `{Methods}` - Component methods

## Best Practices

### DO:
- ✅ Always review generated code
- ✅ Customize for specific needs
- ✅ Follow project naming conventions
- ✅ Add appropriate documentation
- ✅ Include error handling
- ✅ Write tests for generated code

### DON'T:
- ❌ Use templates blindly without understanding
- ❌ Generate code that violates project standards
- ❌ Skip code review for generated code
- ❌ Forget to update imports and dependencies

## Technology-Specific Templates

### REST API
```
- Controller/Route template
- Service/Handler template
- DTO/Model template
- Validation template
```

### Database
```
- Entity/Model template
- Repository template
- Migration template
- Query builder template
```

### Frontend
```
- Component template
- Service template
- State management template
- Form template
```

### Testing
```
- Unit test template
- Integration test template
- Mock/Stub template
- Test data builder template
```

## Example Workflow

### Creating a New REST Endpoint:
1. **Identify**: Need a new user registration endpoint
2. **Template**: Use REST controller template from tech stack
3. **Gather**: 
   - Endpoint: POST /api/users/register
   - Input: email, password, name
   - Output: user ID, token
4. **Generate**:
   ```
   - UserController with register method
   - RegisterUserDTO for input
   - UserResponseDTO for output
   - UserService for business logic
   ```
5. **Customize**: Add email validation, password hashing
6. **Verify**: Test endpoint, check error handling

## Integration with Sub-agent System

### Recommended Workflow
1. **Code Generation**: Use Code Generation Sub-agent
2. **Test Creation**: Use Test Generation Sub-agent
3. **Quality Check**: Use Code Review Sub-agent

### Example Commands

#### For Complete Feature:
```
"請使用 sub-agent workflow 根據 user-management 模板產生完整功能"
```

#### For Specific Component:
```
"請啟動 Code Generation Sub-agent 使用 REST controller 模板產生 UserController"
```

#### Traditional AI Prompt (without sub-agents):
```
"Generate a REST controller for user management using our standard template. 
Include endpoints for create, read, update, delete. 
Follow our project's naming conventions and include proper error handling."
```

## Quality Checklist

Before considering generated code complete:
- [ ] Follows project coding standards
- [ ] Includes appropriate error handling
- [ ] Has proper logging
- [ ] Includes input validation
- [ ] Has unit tests
- [ ] Documentation is complete
- [ ] Security considerations addressed
- [ ] Performance implications considered

## Common Pitfalls

1. **Over-reliance on templates**
   - Templates are starting points, not final solutions
   - Always customize for specific needs

2. **Ignoring project context**
   - Ensure generated code fits with existing architecture
   - Check for duplicate functionality

3. **Missing edge cases**
   - Templates often cover happy paths only
   - Add handling for errors and edge cases

4. **Forgetting dependencies**
   - Update package files
   - Add necessary imports
   - Configure dependency injection

## Related Resources

### Sub-agent Prompts
- `.ai/prompts/code-generation-prompt.md` - Code Generation Agent
- `.ai/prompts/test-generation-prompt.md` - Test Generation Agent
- `.ai/prompts/code-review-prompt.md` - Code Review Agent

### Related Workflows
- `api-endpoint-generation-workflow.md` - For REST API specific generation
- `database-schema-generation-workflow.md` - For database-related code
- `test-generation-workflow.md` - For test code generation
- `documentation-generation-workflow.md` - For generating documentation

### Technical Resources
- `.ai/SUB-AGENT-SYSTEM.md` - Complete sub-agent system documentation
- `.ai/tech-stacks/` - Technology-specific templates and standards