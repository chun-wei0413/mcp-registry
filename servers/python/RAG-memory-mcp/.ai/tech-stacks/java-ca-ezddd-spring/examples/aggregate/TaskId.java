    package tw.teddysoft.example.plan.entity;

import tw.teddysoft.ezddd.entity.ValueObject;

import java.util.UUID;

public record TaskId(String value) implements ValueObject {
    
    public TaskId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TaskId cannot be null or empty");
        }
    }
    
    public static TaskId create() {
        return new TaskId(UUID.randomUUID().toString());
    }
    
    public static TaskId valueOf(String value) {
        return new TaskId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}