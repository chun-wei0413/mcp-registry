# Event Sourcing Aggregate 快速模板

直接複製此模板開始實作 Event Sourcing Aggregate。

```java
package [package].[aggregate].entity;

import tw.teddysoft.ezddd.entity.EsAggregateRoot;
import [package].common.DateProvider;
import static tw.teddysoft.ucontract.Contract.*;
import java.util.*;

/**
 * [Aggregate] Aggregate Root - Event Sourcing 實作
 */
public class [Aggregate] extends EsAggregateRoot<[Aggregate]Id, [Aggregate]Events> {
    public static final String CATEGORY = "[Aggregate]";

    private [Aggregate]Id [aggregate]Id;
    private String name;
    private boolean isDeleted;

    /**
     * Business constructor
     */
    public [Aggregate]([Aggregate]Id [aggregate]Id, String name) {
        super();

        requireNotNull("[Aggregate] id", [aggregate]Id);
        requireNotNull("Name", name);

        // ❌ 不直接修改狀態
        // ✅ 透過 apply + when 修改
        apply(new [Aggregate]Events.[Aggregate]Created(
            [aggregate]Id,
            name,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));

        ensure("State initialized", () ->
            getId().equals([aggregate]Id) &&
            getName().equals(name)
        );
    }

    /**
     * Event Sourcing reconstruction constructor
     */
    public [Aggregate](List<[Aggregate]Events> events) {
        super(events);
    }

    // Command Method
    public void rename(String newName) {
        requireNotNull("New name", newName);

        if (ignore("Name unchanged", () -> this.name.equals(newName))) {
            return;
        }

        // ❌ 不直接修改狀態
        // ✅ 透過 apply + when 修改
        apply(new [Aggregate]Events.[Aggregate]Renamed(
            [aggregate]Id,
            newName,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));

        ensure("Name updated", () -> this.name.equals(newName));
    }

    // ✅ Event Sourcing 必須實作 when() 方法
    @Override
    protected void when([Aggregate]Events event) {
        switch (event) {
            case [Aggregate]Events.[Aggregate]Created e -> {
                this.[aggregate]Id = e.[aggregate]Id();
                this.name = e.name();
                this.isDeleted = false;
            }
            case [Aggregate]Events.[Aggregate]Renamed e -> {
                this.name = e.newName();
            }
            case [Aggregate]Events.[Aggregate]Deleted e -> {
                this.isDeleted = true;
            }
        }
    }

    @Override
    public void ensureInvariant() {
        invariant("Category correct", () -> getCategory().equals(CATEGORY));
        invariantNotNull("[Aggregate] Id", [aggregate]Id);
        if (!isDeleted) {
            invariantNotNull("Name", name);
        }
    }

    public String getName() { return name; }

    @Override
    public [Aggregate]Id getId() { return [aggregate]Id; }

    @Override
    public String getCategory() { return CATEGORY; }

    @Override
    public boolean isDeleted() { return isDeleted; }
}
```

## 替換指南

1. `[package]` → 專案 package（如 `ntut.csie.sslab`）
2. `[Aggregate]` → Aggregate 名稱（如 `Plan`）
3. `[aggregate]` → 小寫 aggregate 名稱（如 `plan`）
4. 新增業務欄位和方法
5. 在 when() 中處理所有事件
