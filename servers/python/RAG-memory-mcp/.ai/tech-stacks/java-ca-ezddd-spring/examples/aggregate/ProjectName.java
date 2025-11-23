package tw.teddysoft.example.plan.entity;

import tw.teddysoft.ezddd.entity.ValueObject;

import java.util.Objects;

public record ProjectName(String value) implements ValueObject {

    public ProjectName {
        Objects.requireNonNull(value, "ProjectName value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("ProjectName value cannot be empty");
        }
    }

    public static ProjectName valueOf(String value) {
        return new ProjectName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}