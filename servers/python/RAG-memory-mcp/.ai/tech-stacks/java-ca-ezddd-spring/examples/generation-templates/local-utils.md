# 🚨 共用類別（必須產生）🚨

# [rootPackage] 內容參考 .dev/project-config.json#rootPackage
#
# 🚨 Code for DateProvider in the [rootPackage].common.entity package 🚨
# ⚠️ 重要：必須放在 src/main/java 目錄，不是 test 目錄
# 完整路徑：src/main/java/[rootPackage]/common/entity/DateProvider.java
```java
package [rootPackage].common.entity;

import java.time.Instant;

public class DateProvider {
    
    private static Instant fixedInstant = null;
    
    public static Instant now() {
        if (fixedInstant != null) {
            return fixedInstant;
        }
        return Instant.now();
    }
    
    public static void useFixedInstant(Instant instant) {
        fixedInstant = instant;
    }
    
    public static void useSystemTime() {
        fixedInstant = null;
    }
}
```

# 🚨 Code for GenericInMemoryRepository in the [rootPackage].common.adapter.out.repository package 🚨
# ⚠️ 重要：必須放在 src/main/java 目錄，不是 test 目錄
# 完整路徑：src/main/java/[rootPackage]/common/adapter/out/repository/GenericInMemoryRepository.java

# 🚨🚨🚨 IMPORT 路徑必須完全照抄，不得修改 🚨🚨🚨
# ✅ 正確的 import（必須照抄）：
```java
package [rootPackage].common.adapter.out.repository;

import tw.teddysoft.ezddd.entity.AggregateRoot;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class GenericInMemoryRepository<T extends AggregateRoot, ID> 
        implements Repository<T, ID> {
    
    private final Map<ID, T> store = new ConcurrentHashMap<>();
    private final MessageBus<DomainEvent> messageBus;
    
    // Outbox implementation
    private final Map<String, List<OutboxEntry>> outbox = new ConcurrentHashMap<>();
    private final AtomicLong globalIndexCounter = new AtomicLong(0);
    
    /**
     * Internal class to store domain events with global index
     */
    private static class OutboxEntry {
        final InternalDomainEvent event;
        final long globalIndex;
        final String streamName;
        
        OutboxEntry(InternalDomainEvent event, long globalIndex, String streamName) {
            this.event = event;
            this.globalIndex = globalIndex;
            this.streamName = streamName;
        }
    }
    
    public GenericInMemoryRepository(MessageBus<DomainEvent> messageBus) {
        this.messageBus = messageBus;
    }
    
    public GenericInMemoryRepository() {
        this.messageBus = null;
    }
    
    @Override
    public Optional<T> findById(ID id) {
        T aggregate = store.get(id);
        if (aggregate == null) {
            return Optional.empty();
        }
        
        // Check for soft delete - if aggregate has isDeleted method and returns true, return empty
        try {
            var method = aggregate.getClass().getMethod("isDeleted");
            boolean isDeleted = (Boolean) method.invoke(aggregate);
            if (isDeleted) {
                return Optional.empty();
            }
        } catch (Exception e) {
            // If no isDeleted method or error occurred, continue normally
        }
        
        return Optional.of(aggregate);
    }
    
    @Override
    public void save(T aggregate) {
        store.put((ID) aggregate.getId(), aggregate);
        
        // Store events in outbox before publishing
        List<DomainEvent> events = aggregate.getDomainEvents();
        if (!events.isEmpty()) {
            // Generate stream name from aggregate type and id
            String streamName = generateStreamName(aggregate);
            List<OutboxEntry> streamEntries = outbox.computeIfAbsent(streamName, k -> new ArrayList<>());
            
            // Add each event to outbox with global index
            for (DomainEvent event : events) {
                if (event instanceof InternalDomainEvent internalEvent) {
                    long globalIndex = globalIndexCounter.incrementAndGet();
                    streamEntries.add(new OutboxEntry(internalEvent, globalIndex, streamName));
                }
            }
        }
        
        // Publish domain events if message bus is available
        if (messageBus != null) {
            for (DomainEvent event : events) {
                messageBus.post(event);
            }
        }
        
        // Clear events after publishing
        aggregate.clearDomainEvents();
    }
    
    @Override
    public void delete(T aggregate) {
        // Get domain events before deletion
        List<DomainEvent> events = aggregate.getDomainEvents();
        
        // Store events in outbox if available
        String streamName = aggregate.getClass().getSimpleName() + "-" + aggregate.getId().toString();
        if (!events.isEmpty()) {
            List<OutboxEntry> streamEntries = outbox.computeIfAbsent(streamName, k -> new ArrayList<>());
            for (DomainEvent event : events) {
                if (event instanceof InternalDomainEvent internalEvent) {
                    long globalIndex = globalIndexCounter.incrementAndGet();
                    streamEntries.add(new OutboxEntry(internalEvent, globalIndex, streamName));
                }
            }
        }
        
        // Publish domain events if message bus is available
        if (messageBus != null) {
            for (DomainEvent event : events) {
                messageBus.post(event);
            }
        }
        
        // Clear events after publishing
        aggregate.clearDomainEvents();
        
        // Remove from store
        store.remove(aggregate.getId());
    }
    
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }
    
    public void clear() {
        store.clear();
    }
    
    public int size() {
        return store.size();
    }
    
    // ======================== Outbox Pattern Support ========================
    
    /**
     * Generate stream name from aggregate type and id
     * Format: {AggregateType}:{AggregateId}
     */
    private String generateStreamName(T aggregate) {
        String aggregateType = aggregate.getClass().getSimpleName();
        String aggregateId = aggregate.getId().toString();
        return aggregateType + ":" + aggregateId;
    }
    
    /**
     * Find all domain events by aggregate stream name
     * @param aggregateStreamName the stream name of the aggregate
     * @return list of InternalDomainEvents for the specified stream
     */
    public List<InternalDomainEvent> findByStreamName(String aggregateStreamName) {
        List<OutboxEntry> entries = outbox.get(aggregateStreamName);
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }
        return entries.stream()
                .map(entry -> entry.event)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all domain events sorted by global index (ascending)
     * @return list of all InternalDomainEvents sorted by global index
     */
    public List<InternalDomainEvent> getAllDomainEvents() {
        return outbox.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparingLong(entry -> entry.globalIndex))
                .map(entry -> entry.event)
                .collect(Collectors.toList());
    }
    
    /**
     * Clear the outbox (useful for testing)
     */
    public void clearOutbox() {
        outbox.clear();
        globalIndexCounter.set(0);
    }
    
    /**
     * Get the current global index counter value
     * @return current global index
     */
    public long getCurrentGlobalIndex() {
        return globalIndexCounter.get();
    }
    
    /**
     * Get outbox size for a specific stream
     * @param streamName the aggregate stream name
     * @return number of events in the stream
     */
    public int getOutboxSize(String streamName) {
        List<OutboxEntry> entries = outbox.get(streamName);
        return entries != null ? entries.size() : 0;
    }
    
    /**
     * Get total outbox size (all streams)
     * @return total number of events in outbox
     */
    public int getTotalOutboxSize() {
        return outbox.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
```

# 🚨 Code for MyInMemoryMessageBroker in the [rootPackage].common package 🚨
# ⚠️ 重要：必須放在 src/main/java 目錄，不是 test 目錄
# 完整路徑：src/main/java/[rootPackage]/common/MyInMemoryMessageBroker.java
# 用途：提供異步的訊息傳遞機制，用於處理領域事件
```java
package [rootPackage].common;

import com.google.common.eventbus.EventBus;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MyInMemoryMessageBroker extends EventBus implements Runnable {
    private final BlockingQueue<Object> queue = new ArrayBlockingQueue(1024);

    public synchronized void post(Object message) {
        try {
            this.queue.put(message);
        } catch (InterruptedException var3) {
            Thread.currentThread().interrupt();
        }

    }

    public void postAll(List<Object> messages) {
        messages.forEach(this::post);
    }

    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                Object message = this.queue.take();
                System.out.println("MyInMemoryMessageBroker post message => " + message);
                super.post(message);
            } catch (InterruptedException var2) {
                Thread.currentThread().interrupt();
            }
        }

    }
}
```

# 🚨 Code for MyInMemoryMessageProducer in the [rootPackage].common package 🚨
# ⚠️ 重要：必須放在 src/main/java 目錄，不是 test 目錄
# 完整路徑：src/main/java/[rootPackage]/common/MyInMemoryMessageProducer.java
# 用途：實作 MessageProducer 介面，用於 Outbox Pattern 中發送領域事件
# 🚨🚨🚨 IMPORT 路徑必須完全照抄，不得修改 🚨🚨🚨
```java
package [rootPackage].common;

import tw.teddysoft.ezddd.message.broker.adapter.InMemoryMessageBroker;
import tw.teddysoft.ezddd.message.broker.adapter.PostEventFailureException;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;

import java.io.IOException;

public class MyInMemoryMessageProducer implements MessageProducer<DomainEventData> {

    private final MyInMemoryMessageBroker messageBroker;

    public MyInMemoryMessageProducer(MyInMemoryMessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    public void post(DomainEventData domainEventData) throws PostEventFailureException {
//        System.out.println("BBB => " + domainEventData.toString());
        this.messageBroker.post(domainEventData);
    }

    public void close() throws IOException {
    }
}
```
