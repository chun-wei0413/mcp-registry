# Maven Dependencies Reference

## 核心依賴

### EZ App Starter (包含所有 EZDDD 框架功能)
```xml
<dependency>
    <groupId>tw.teddysoft.ezapp</groupId>
    <artifactId>ezapp-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**ezapp-starter 已包含：**
- ezddd-core (DDD 核心框架)
- ezcqrs (CQRS 支援)
- uContract (Design by Contract)
- ezSpec (BDD 測試框架)
- 所有 ezddd-gateway 功能 (Outbox Pattern, Event Sourcing 等)

## Spring Boot 依賴
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## 資料庫相關
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

## 測試相關
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## 重要提醒
- **不需要**單獨引入 ezddd-core, ezcqrs, uContract, ezSpec
- **不需要**單獨引入 ezddd-gateway 相關模組
- 所有 EZDDD 框架功能都由 ezapp-starter 提供
