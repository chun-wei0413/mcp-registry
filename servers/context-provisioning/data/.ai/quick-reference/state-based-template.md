# State-based Aggregate 快速模板

直接複製此模板開始實作 State-based Aggregate。

```java
package [package].[aggregate].entity;

import tw.teddysoft.ezddd.entity.AggregateRoot;
import [package].common.DateProvider;
import static tw.teddysoft.ucontract.Contract.*;
import java.util.*;

/**
 * [Aggregate] Aggregate Root - State-based 實作
 */
public class [Aggregate] extends AggregateRoot<[Aggregate]Id> {
    public static final String CATEGORY = "[Aggregate]";

    private [Aggregate]Id [aggregate]Id;
    private String name;
    private boolean isDeleted;

    /**
     * Public constructor
     */
    public [Aggregate]([Aggregate]Id [aggregate]Id, String name) {
        super();

        requireNotNull("[Aggregate] id", [aggregate]Id);
        requireNotNull("Name", name);

        // 1. 直接設定狀態（State-based 關鍵）
        this.[aggregate]Id = [aggregate]Id;
        this.name = name;
        this.isDeleted = false;

        // 2. 發布事件
        apply(new [Aggregate]Events.[Aggregate]Created(
            [aggregate]Id,
            name,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));

        // 3. 後置條件
        ensure("State initialized", () ->
            getId().equals([aggregate]Id) &&
            getName().equals(name)
        );
    }

    // Command Method
    public void rename(String newName) {
        requireNotNull("New name", newName);

        if (ignore("Name unchanged", () -> this.name.equals(newName))) {
            return;
        }

        // 1. 直接修改狀態
        this.name = newName;

        // 2. 發布事件
        apply(new [Aggregate]Events.[Aggregate]Renamed(
            [aggregate]Id,
            newName,
            new HashMap<>(),
            UUID.randomUUID(),
            DateProvider.now()
        ));

        ensure("Name updated", () -> this.name.equals(newName));
    }

    // ❌ State-based 不需要 when() 方法

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
2. `[Aggregate]` → Aggregate 名稱（如 `Plan`、`User`）
3. `[aggregate]` → 小寫 aggregate 名稱（如 `plan`、`user`）
4. 新增業務欄位和方法
