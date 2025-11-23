# EZDDD Framework Reference

## 重要：使用 ezapp-starter

從現在開始，所有 EZDDD 框架功能都由 `ezapp-starter` 統一提供：

```xml
<dependency>
    <groupId>tw.teddysoft.ezapp</groupId>
    <artifactId>ezapp-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Import 路徑參考

雖然依賴由 ezapp-starter 提供，但 import 路徑保持不變：

### 核心類別
- `tw.teddysoft.ezddd.entity.DomainEvent`
- `tw.teddysoft.ezddd.entity.AggregateRoot`
- `tw.teddysoft.ezddd.entity.Entity`
- `tw.teddysoft.ezddd.entity.ValueObject`

### UseCase 相關
- `tw.teddysoft.ezddd.usecase.UseCase`
- `tw.teddysoft.ezddd.usecase.port.in.*`
- `tw.teddysoft.ezddd.usecase.port.out.*`

### Repository
- `tw.teddysoft.ezddd.usecase.port.out.repository.Repository`

### Messaging
- `tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus`
- `tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer`

### Contract
- `tw.teddysoft.ucontract.Contract`

### 測試
- `tw.teddysoft.ezspec.ezspec`

## 框架功能說明

**ezapp-starter 包含：**
1. **DDD 核心支援** - Entity, ValueObject, AggregateRoot
2. **CQRS 模式** - Command/Query 分離
3. **Event Sourcing** - 事件溯源支援
4. **Outbox Pattern** - 可靠的事件發布
5. **Design by Contract** - 契約式設計
6. **BDD 測試框架** - ezSpec 支援

## 遷移指南

如果你的專案還在使用個別的 ezddd 依賴，請按以下步驟遷移：

1. 移除所有個別的 ezddd 依賴：
   - ezddd-core
   - ezcqrs
   - ezddd-gateway 相關
   - uContract
   - ezSpec

2. 加入 ezapp-starter：
   ```xml
   <dependency>
       <groupId>tw.teddysoft.ezapp</groupId>
       <artifactId>ezapp-starter</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

3. Import 路徑不需要改變，保持原樣即可
