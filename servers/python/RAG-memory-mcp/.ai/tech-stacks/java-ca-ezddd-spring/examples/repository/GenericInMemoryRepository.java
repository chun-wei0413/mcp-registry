package tw.teddysoft.example.adapter.out.repository;

import tw.teddysoft.ezddd.entity.AggregateRoot;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;

public class GenericInMemoryRepository<T extends AggregateRoot, ID> implements Repository<T, ID> {

    private final Set<T> aggregates;
    private final MessageBus messageBus;

    public GenericInMemoryRepository(MessageBus messageBus) {
        Objects.requireNonNull(messageBus, "Message bus cannot be null");

        this.aggregates = new HashSet<>();
        this.messageBus = messageBus;
    }

    @Override
    public void save(T aggregate) {
        Objects.requireNonNull(aggregate, "Aggregate cannot be null");

        // Remove existing aggregate if present
        aggregates.removeIf(a -> a.getId().equals(aggregate.getId()));

        // Publish all domain events
        aggregate.getDomainEvents().forEach(messageBus::post);

        // Clear domain events from aggregate
        aggregate.clearDomainEvents();

        // Save aggregate
        aggregates.add(aggregate);
    }

    @Override
    public Optional<T> findById(ID id) {
        Objects.requireNonNull(id, "ID cannot be null");

        return aggregates.stream()
                .filter(aggregate -> aggregate.getId().equals(id))
                .findFirst();
    }

    @Override
    public void delete(T aggregate) {
        Objects.requireNonNull(aggregate, "Aggregate cannot be null");

        // Publish all domain events before deletion
        aggregate.getDomainEvents().forEach(messageBus::post);

        // Clear domain events from aggregate
        aggregate.clearDomainEvents();

        // Remove aggregate
        aggregates.remove(aggregate);
    }

    public void clear() {
        aggregates.clear();
    }

    public int size() {
        return aggregates.size();
    }
}