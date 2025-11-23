package tw.teddysoft.example.plan.usecase.port.out;

import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxData;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "plan")
public class PlanData implements OutboxData<String> {

    @Transient
    private List<DomainEventData> domainEventDatas;

    @Transient
    private String streamName;

    @Id
    @Column(name = "id")
    private String planId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "next_task_id", nullable = false)
    private int nextTaskId;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "planData")
    private Set<ProjectData> projectDatas;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Version
    @Column(columnDefinition = "bigint DEFAULT 0", nullable = false)
    private long version;

    public PlanData() {
        this(0L);
    }

    public PlanData(long version) {
        projectDatas = new HashSet<>();
        this.version = version;
        this.domainEventDatas = new ArrayList<>();
        this.isDeleted = false;
        this.nextTaskId = 0;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getNextTaskId() {
        return nextTaskId;
    }

    public void setNextTaskId(int nextTaskId) {
        this.nextTaskId = nextTaskId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Set<ProjectData> getProjectDatas() {
        return projectDatas;
    }

    public void setProjectDatas(Set<ProjectData> projectDatas) {
        this.projectDatas = projectDatas;
    }

    public void addProjectData(ProjectData projectData) {
        projectData.setPlanData(this);
        this.projectDatas.add(projectData);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    @Transient
    public String getId() {
        return planId;
    }

    @Override
    @Transient
    public void setId(String id) {
        this.planId = id;
    }

    @Override
    @Transient
    public List<DomainEventData> getDomainEventDatas() {
        return this.domainEventDatas;
    }

    @Override
    @Transient
    public void setDomainEventDatas(List<DomainEventData> domainEventDatas) {
        this.domainEventDatas = domainEventDatas;
    }

    @Override
    @Transient
    public String getStreamName() {
        return streamName;
    }

    @Override
    @Transient
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }
}