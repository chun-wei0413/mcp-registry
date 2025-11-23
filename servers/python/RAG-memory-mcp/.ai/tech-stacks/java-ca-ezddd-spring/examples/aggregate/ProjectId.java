package tw.teddysoft.example.plan.entity;

import tw.teddysoft.ezddd.entity.ValueObject;

import java.util.Objects;
import java.util.UUID;

public record ProjectId(String value) implements ValueObject {

    public ProjectId {
        Objects.requireNonNull(value, "ProjectId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("ProjectId value cannot be empty");
        }
    }

    public static ProjectId create() {
        return new ProjectId(UUID.randomUUID().toString());
    }

    public static ProjectId valueOf(String value) {
        return new ProjectId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}