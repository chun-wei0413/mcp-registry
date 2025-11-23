# ezapp-starter 類別索引 (v1.0.0)

這是 ezapp-starter 框架的核心類別索引，用於快速查找和確認 import 路徑。

## Domain Layer 核心類別

```
tw.teddysoft.ezddd.entity.AggregateRoot
tw.teddysoft.ezddd.entity.DomainEvent
tw.teddysoft.ezddd.entity.InternalDomainEvent
tw.teddysoft.ezddd.entity.ValueObject
tw.teddysoft.ezddd.entity.Entity
tw.teddysoft.ezddd.entity.EsAggregateRoot
tw.teddysoft.ezddd.entity.DomainEventTypeMapper
```

## Use Case Layer 核心類別

```
tw.teddysoft.ezddd.usecase.port.in.interactor.Input
tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode
tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException
tw.teddysoft.ezddd.cqrs.usecase.command.Command
tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput
```

## Repository Pattern 類別

```
tw.teddysoft.ezddd.usecase.port.out.repository.Repository
tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository
tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxData
tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxMapper
```

## Query Pattern 類別

```
tw.teddysoft.ezddd.cqrs.usecase.query.Query
tw.teddysoft.ezddd.cqrs.usecase.query.Projection
tw.teddysoft.ezddd.cqrs.usecase.query.ProjectionInput
tw.teddysoft.ezddd.cqrs.usecase.query.Archive
```

## Domain Event Data 類別

```
tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData
tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper
```

## Messaging 類別

```
tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus
tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer
tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus
tw.teddysoft.ezddd.message.broker.adapter.InMemoryMessageBroker
tw.teddysoft.ezddd.message.broker.adapter.out.producer.InMemoryMessageProducer
tw.teddysoft.ezddd.message.broker.adapter.PostEventFailureException
```

## Reactor Pattern 類別

```
tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor
```

## Outbox Infrastructure 類別

```
tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxRepositoryPeerAdapter
tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxStore
tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxClient
tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxStoreAdapter
tw.teddysoft.ezddd.data.io.ezoutbox.SpringJpaClient
```

## Event Sourcing Infrastructure 類別

```
tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient
tw.teddysoft.ezddd.data.io.ezes.store.MessageDbClient
tw.teddysoft.ezddd.data.io.ezes.store.MessageData
tw.teddysoft.ezddd.data.io.ezes.relay.EzesCatchUpRelay
tw.teddysoft.ezddd.data.io.ezes.relay.MessageDbToDomainEventDataConverter
```

## Testing Support 類別 (ezspec)

```
tw.teddysoft.ezspec.extension.junit5.EzScenario
tw.teddysoft.ezspec.EzFeature
tw.teddysoft.ezspec.EzFeatureReport
tw.teddysoft.ezspec.keyword.Feature
tw.teddysoft.ezspec.visitor.PlainTextReport
```

## Design by Contract 類別 (ucontract)

```
tw.teddysoft.ucontract.Contract
tw.teddysoft.ucontract.PreconditionViolationException
tw.teddysoft.ucontract.PostconditionViolationException
```

## Utility 類別

```
tw.teddysoft.ezddd.common.Converter
```

## 使用說明

1. **查找類別**: 使用 Ctrl+F 搜尋類別名稱
2. **確認 import**: 複製完整的類別路徑作為 import
3. **避免猜測**: 如果類別不在此索引中，請參考實際專案中的 import 或搜尋 ezapp-starter 原始碼

## 重要注意事項

### 依賴配置
- ✅ 所有這些類別都包含在 `ezapp-starter:1.0.0` 中
- ✅ 不需要單獨引入 ezddd-core、ezcqrs 等依賴
- ✅ 使用 jakarta.persistence 而非 javax.persistence

### 常見錯誤路徑 ⚠️
- ❌ `tw.teddysoft.ezddd.domain.model.*` → ✅ `tw.teddysoft.ezddd.entity.*`
- ❌ `tw.teddysoft.ezddd.reactor.*` → ✅ `tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor`
- ❌ `tw.teddysoft.ezcqrs.eventbus.*` → ✅ `tw.teddysoft.ezddd.usecase.port.inout.messaging.*`
- ❌ `EventSourcingAggregate` → ✅ `EsAggregateRoot`

### Reactor 介面使用 (ADR-031)
```java
// 正確：Reactor 必須繼承 Reactor<DomainEventData>
public interface MyReactor extends Reactor<DomainEventData> {
    // ...
}
```