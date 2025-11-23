# Prompt Template: Add New Feature

## Input Format
```yaml
feature_name: Task Priority
feature_description: Add priority levels (High, Medium, Low) to tasks
affected_aggregates:
  - Plan (add priority field to tasks)
  - Task (new priority property)
required_use_cases:
  - SetTaskPriority
  - GetTasksByPriority
ui_changes:
  - Add priority selector in task creation
  - Show priority badge on task items
  - Add filter by priority
```

## Generation Steps

### Step 1: Update Domain Model
1. Add new value object if needed (e.g., `TaskPriority`)
2. Update aggregate with new field
3. Add domain methods for the feature
4. Update domain events

### Step 2: Create Use Cases
For each required use case:
1. Create use case interface
2. Implement service
3. Write tests

### Step 3: Update Persistence
1. Add database migration if needed
2. Update JPA entities
3. Update mappers

### Step 4: Create/Update API Endpoints
1. Create new controllers or update existing
2. Add DTOs if needed
3. Update OpenAPI documentation

### Step 5: Update Frontend (if required)
1. Update API client
2. Add/modify React components
3. Update state management

## Example Code Structure

### Value Object
```java
public record TaskPriority(String value) {
    public static final TaskPriority HIGH = new TaskPriority("HIGH");
    public static final TaskPriority MEDIUM = new TaskPriority("MEDIUM");
    public static final TaskPriority LOW = new TaskPriority("LOW");
    
    public TaskPriority {
        requireNotNull("Value", value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid priority: " + value);
        }
    }
    
    private static boolean isValid(String value) {
        return "HIGH".equals(value) || "MEDIUM".equals(value) || "LOW".equals(value);
    }
}
```

### Domain Method
```java
public void setPriority(TaskId taskId, TaskPriority priority) {
    Contract.requireNotNull("TaskId", taskId);
    Contract.requireNotNull("Priority", priority);
    Contract.require("Task exists", tasks.containsKey(taskId));
    
    apply(new PlanEvents.TaskPrioritySet(
        id.value(),
        taskId.value(),
        priority.value(),
        UUID.randomUUID(),
        Instant.now()
    ));
}
```

## Validation Points
- [ ] Domain model updated correctly
- [ ] All use cases implemented and tested
- [ ] Database migrations created
- [ ] API endpoints working
- [ ] Frontend integrated (if applicable)
- [ ] All tests passing