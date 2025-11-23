package tw.teddysoft.example.plan.entity;

import tw.teddysoft.ezddd.entity.ValueObject;

import java.util.Objects;
import java.util.UUID;

public record PlanId(String value) implements ValueObject {

    public PlanId {
        Objects.requireNonNull(value, "PlanId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("PlanId value cannot be empty");
        }
    }

    public static PlanId create(){
        return new PlanId(UUID.randomUUID().toString());
    }

    public static PlanId valueOf(String value) {
        return new PlanId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}