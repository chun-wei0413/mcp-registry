package tw.teddysoft.example.tag.entity;

import tw.teddysoft.ezddd.entity.ValueObject;

import java.util.Objects;
import java.util.UUID;

public record TagId(String value) implements ValueObject {
    public TagId {
        Objects.requireNonNull(value, "TagId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("TagId value cannot be empty");
        }
    }

    public static TagId valueOf(String id) {
        return new TagId(id);
    }

    public static TagId valueOf(UUID id) {
        return new TagId(id.toString());
    }

    public static TagId create() {
        return new TagId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}