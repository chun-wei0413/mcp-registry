package tw.teddysoft.example.tag.entity;

import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.entity.DomainEventTypeMapper;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public sealed interface TagEvents extends InternalDomainEvent permits
        TagEvents.TagCreated,
        TagEvents.TagRenamed,
        TagEvents.TagColorChanged,
        TagEvents.TagDeleted {

    TagId tagId();

    @Override
    default String source() {
        return tagId().value();  // 新版 API: source() 回傳聚合的 ID
    }

    record TagCreated(
            TagId tagId,
            PlanId planId,
            String name,
            String color,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements TagEvents, ConstructionEvent {
        public TagCreated {
            Objects.requireNonNull(tagId);
            Objects.requireNonNull(planId);
            Objects.requireNonNull(name);
            Objects.requireNonNull(color);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }

    record TagRenamed(
            TagId tagId,
            String newName,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements TagEvents {
        public TagRenamed {
            Objects.requireNonNull(tagId);
            Objects.requireNonNull(newName);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }

    record TagColorChanged(
            TagId tagId,
            String newColor,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements TagEvents {
        public TagColorChanged {
            Objects.requireNonNull(tagId);
            Objects.requireNonNull(newColor);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }

    record TagDeleted(
            TagId tagId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements TagEvents, DestructionEvent {
        public TagDeleted {
            Objects.requireNonNull(tagId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
        
        // 不需要覆寫 source()，已在介面層級定義
    }

    class TypeMapper extends DomainEventTypeMapper.DefaultMapper {
        public static final String MAPPING_TYPE_PREFIX = "TagEvents$";

        public static final String TAG_CREATED = MAPPING_TYPE_PREFIX + "TagCreated";
        public static final String TAG_RENAMED = MAPPING_TYPE_PREFIX + "TagRenamed";
        public static final String TAG_COLOR_CHANGED = MAPPING_TYPE_PREFIX + "TagColorChanged";
        public static final String TAG_DELETED = MAPPING_TYPE_PREFIX + "TagDeleted";

        private static final DomainEventTypeMapper mapper;

        static {
            mapper = DomainEventTypeMapper.create();
            mapper.put(TAG_CREATED, TagEvents.TagCreated.class);
            mapper.put(TAG_RENAMED, TagEvents.TagRenamed.class);
            mapper.put(TAG_COLOR_CHANGED, TagEvents.TagColorChanged.class);
            mapper.put(TAG_DELETED, TagEvents.TagDeleted.class);
        }

        public static DomainEventTypeMapper getInstance() {
            return mapper;
        }
    }

    static DomainEventTypeMapper mapper() {
        return TypeMapper.getInstance();
    }
}