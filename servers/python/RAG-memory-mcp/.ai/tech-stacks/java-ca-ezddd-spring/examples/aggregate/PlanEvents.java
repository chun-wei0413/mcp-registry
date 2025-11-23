package tw.teddysoft.example.plan.entity;

import tw.teddysoft.example.tag.entity.TagId;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.entity.DomainEventTypeMapper;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public sealed interface PlanEvents extends InternalDomainEvent permits
        PlanEvents.PlanCreated,
        PlanEvents.PlanRenamed,
        PlanEvents.PlanDeleted,
        PlanEvents.ProjectCreated,
        PlanEvents.ProjectDeleted,
        PlanEvents.TaskCreated,
        PlanEvents.TaskChecked,
        PlanEvents.TaskUnchecked,
        PlanEvents.TaskDeleted,
        PlanEvents.TaskDeadlineSet,
        PlanEvents.TaskRenamed,
        PlanEvents.TagAssigned,
        PlanEvents.TagUnassigned {

    PlanId planId();

    @Override
    default String source() {
        return planId().value();  // 新版 API: source() 回傳聚合的 ID
    }

    record PlanCreated(
            PlanId planId,
            String name,
            String userId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents, ConstructionEvent {
        public PlanCreated {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(name);
            Objects.requireNonNull(userId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record PlanRenamed(
            PlanId planId,
            String newName,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public PlanRenamed {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(newName);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record PlanDeleted(
            PlanId planId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public PlanDeleted {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record ProjectCreated(
            PlanId planId,
            ProjectId projectId,
            ProjectName projectName,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public ProjectCreated {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(projectName);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record ProjectDeleted(
            PlanId planId,
            ProjectId projectId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public ProjectDeleted {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record TaskCreated(
            PlanId planId,
            ProjectId projectId,
            TaskId taskId,
            String taskName,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public TaskCreated {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(taskName);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record TaskChecked(
            PlanId planId,
            ProjectId projectId,
            TaskId taskId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public TaskChecked {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record TaskUnchecked(
            PlanId planId,
            ProjectId projectId,
            TaskId taskId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public TaskUnchecked {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record TaskDeleted(
            PlanId planId,
            ProjectId projectId,
            TaskId taskId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public TaskDeleted {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record TaskDeadlineSet(
            PlanId planId,
            ProjectId projectId,
            TaskId taskId,
            String deadline,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public TaskDeadlineSet {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(taskId);
            // deadline can be null (to remove deadline)
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record TaskRenamed(
            PlanId planId,
            ProjectId projectId,
            TaskId taskId,
            String newName,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public TaskRenamed {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(newName);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    class TypeMapper extends DomainEventTypeMapper.DefaultMapper {
        public static final String MAPPING_TYPE_PREFIX = "PlanEvents$";

        public static final String PLAN_CREATED = MAPPING_TYPE_PREFIX + "PlanCreated";
        public static final String PLAN_RENAMED = MAPPING_TYPE_PREFIX + "PlanRenamed";
        public static final String PLAN_DELETED = MAPPING_TYPE_PREFIX + "PlanDeleted";
        public static final String PROJECT_CREATED = MAPPING_TYPE_PREFIX + "ProjectCreated";
        public static final String PROJECT_DELETED = MAPPING_TYPE_PREFIX + "ProjectDeleted";
        public static final String TASK_CREATED = MAPPING_TYPE_PREFIX + "TaskCreated";
        public static final String TASK_CHECKED = MAPPING_TYPE_PREFIX + "TaskChecked";
        public static final String TASK_UNCHECKED = MAPPING_TYPE_PREFIX + "TaskUnchecked";
        public static final String TASK_DELETED = MAPPING_TYPE_PREFIX + "TaskDeleted";
        public static final String TASK_DEADLINE_SET = MAPPING_TYPE_PREFIX + "TaskDeadlineSet";
        public static final String TASK_RENAMED = MAPPING_TYPE_PREFIX + "TaskRenamed";
        public static final String TAG_ASSIGNED = MAPPING_TYPE_PREFIX + "TagAssigned";
        public static final String TAG_UNASSIGNED = MAPPING_TYPE_PREFIX + "TagUnassigned";

        private static final DomainEventTypeMapper mapper;

        static {
            mapper = DomainEventTypeMapper.create();
            mapper.put(PLAN_CREATED, PlanEvents.PlanCreated.class);
            mapper.put(PLAN_RENAMED, PlanEvents.PlanRenamed.class);
            mapper.put(PLAN_DELETED, PlanEvents.PlanDeleted.class);
            mapper.put(PROJECT_CREATED, PlanEvents.ProjectCreated.class);
            mapper.put(PROJECT_DELETED, PlanEvents.ProjectDeleted.class);
            mapper.put(TASK_CREATED, PlanEvents.TaskCreated.class);
            mapper.put(TASK_CHECKED, PlanEvents.TaskChecked.class);
            mapper.put(TASK_UNCHECKED, PlanEvents.TaskUnchecked.class);
            mapper.put(TASK_DELETED, PlanEvents.TaskDeleted.class);
            mapper.put(TASK_DEADLINE_SET, PlanEvents.TaskDeadlineSet.class);
            mapper.put(TASK_RENAMED, PlanEvents.TaskRenamed.class);
            mapper.put(TAG_ASSIGNED, PlanEvents.TagAssigned.class);
            mapper.put(TAG_UNASSIGNED, PlanEvents.TagUnassigned.class);
        }

        public static DomainEventTypeMapper getInstance() {
            return mapper;
        }
    }

    record TagAssigned(
            PlanId planId,
            ProjectId projectId,
            TaskId taskId,
            TagId tagId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public TagAssigned {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(tagId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    record TagUnassigned(
            PlanId planId,
            ProjectId projectId,
            TaskId taskId,
            TagId tagId,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements PlanEvents {
        public TagUnassigned {
            Objects.requireNonNull(planId);
            Objects.requireNonNull(projectId);
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(tagId);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    static DomainEventTypeMapper mapper() {
        return TypeMapper.getInstance();
    }
}