# Package 結構指南

本文檔明確定義 Clean Architecture + DDD 的正確 package 結構。

## 🎯 核心原則

1. **aggregate 包含業務邏輯**：entity、usecase、adapter
2. **io 包含框架配置**：在 rootPackage 層級，不在 aggregate 下
3. **common 包含共用元件**：在 rootPackage 層級

## 📦 正確的 Package 結構

```
[rootPackage]/
├── [aggregate1]/              # 聚合根（如 plan、user、order）
│   ├── entity/               # 領域實體層
│   │   ├── [Aggregate].java
│   │   ├── [Aggregate]Id.java
│   │   └── event/           # 領域事件
│   ├── usecase/             # 應用邏輯層
│   │   ├── service/         # Use Case 實作
│   │   └── port/            # 介面定義
│   │       ├── in/          # 輸入埠（Use Case 介面）
│   │       ├── out/         # 輸出埠（Repository、Projection 等）
│   │       └── dto/         # 資料傳輸物件
│   └── adapter/             # 適配器層
│       ├── in/              # 輸入適配器
│       │   └── controller/  # REST Controller
│       └── out/             # 輸出適配器
│           ├── repository/  # Repository 實作
│           └── projection/  # Projection 實作
├── [aggregate2]/            # 其他聚合根
├── common/                  # 共用元件
│   └── entity/             # 共用實體（如 DateProvider）
└── io/                      # 框架配置層 ⚠️ 注意：在 rootPackage 下
    └── springboot/
        ├── config/          # Spring Boot 配置
        │   ├── BootstrapConfig.java
        │   └── orm/         # ORM 相關配置
        └── Application.java # 主程式入口
```

## ❌ 常見錯誤

### 錯誤 1：在 aggregate 下創建 infrastructure
```
# 錯誤
[rootPackage]/[aggregate]/infrastructure/  ❌

# 正確
[rootPackage]/io/                           ✅
```

### 錯誤 2：混淆 adapter 和 io
```
# 錯誤
[rootPackage]/[aggregate]/io/repository/  ❌

# 正確
[rootPackage]/[aggregate]/adapter/out/repository/     ✅
```

## 📍 Package 層級對應

| Clean Architecture 層 | Package 位置 | 說明 |
|---------------------|-------------|------|
| Entity | `[aggregate]/entity/` | 業務邏輯核心 |
| Use Case | `[aggregate]/usecase/` | 應用邏輯 |
| Adapter | `[aggregate]/adapter/` | 介面適配 |
| Framework | `io/springboot/` | 框架配置（在 rootPackage 下） |

## 🔧 創建目錄結構指令

### 正確的創建指令
```bash
# 創建 aggregate 結構
mkdir -p src/main/java/[rootPackage]/[aggregate]/{entity,usecase,adapter}
mkdir -p src/main/java/[rootPackage]/[aggregate]/entity/event
mkdir -p src/main/java/[rootPackage]/[aggregate]/usecase/{service,port/{in,out,dto}}
mkdir -p src/main/java/[rootPackage]/[aggregate]/adapter/{in/controller,out/repository}

# 創建框架層結構（注意：在 rootPackage 下）
mkdir -p src/main/java/[rootPackage]/io/springboot/config

# 創建共用層
mkdir -p src/main/java/[rootPackage]/common
```

## 📝 範例

假設 rootPackage 是 `com.example.myapp`，有一個 `plan` aggregate：

```
com.example.myapp/
├── plan/                           # Plan 聚合根
│   ├── entity/
│   │   ├── Plan.java
│   │   ├── PlanId.java
│   │   └── event/
│   │       └── PlanCreated.java
│   ├── usecase/
│   │   ├── service/
│   │   │   └── CreatePlanService.java
│   │   └── port/
│   │       ├── in/
│   │       │   └── CreatePlanUseCase.java
│   │       └── out/
│   │           └── repository/
│   │               └── PlanRepository.java
│   └── adapter/
│       ├── in/
│       │   └── controller/
│       │       └── PlanController.java
│       └── out/
│           └── repository/
│               └── JpaPlanRepository.java
├── common/
│   └── entity/
│       └── DateProvider.java
└── io/                             # 注意：在 com.example.myapp 下
    └── springboot/
        ├── config/
        │   └── BootstrapConfig.java
        └── Application.java
```

## 🚨 重要提醒

1. **io package 永遠在 rootPackage 下**，不在 aggregate 下
2. **每個 aggregate 是獨立的**，包含自己的 entity、usecase、adapter
3. **common 是共用的**，所有 aggregate 都可以使用
4. **不要使用 infrastructure 這個名稱**，使用 io 或 adapter

## 📚 相關文檔

- [專案結構](./tech-stacks/java-ca-ezddd-spring/project-structure.md)
- [Clean Architecture 模式](./tech-stacks/java-ca-ezddd-spring/examples/aggregate/README.md)
- [DDD 實踐指南](./tech-stacks/java-ca-ezddd-spring/examples/aggregate/README.md)

---

💡 **記住**: io 在 rootPackage 下，不在 aggregate 下！