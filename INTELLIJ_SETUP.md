# IntelliJ IDEA 專案設定指南

## 開啟專案

1. **開啟 IntelliJ IDEA**
2. **選擇 "Open"**
3. **選擇專案根目錄** `E:\Coding\mcp-registry`
4. **選擇根目錄下的 `pom.xml`** (確保是最外層的 pom.xml)
5. **點選 "Open as Project"**

## Maven 多模組結構

```
mcp-registry/                          <- 根目錄 (在這裡開啟專案)
├── pom.xml                           <- 根 POM (Parent POM)
└── mcp-registry-java/                <- Java 實現
    ├── pom.xml                       <- Java Parent POM
    ├── mcp-common/                   <- 共用模組
    ├── mcp-core/                     <- 核心領域模組 (Clean Architecture)
    ├── mcp-postgresql-server/        <- PostgreSQL MCP Server
    ├── mcp-mysql-server/            <- MySQL MCP Server
    └── testing-tools/               <- 測試工具
```

## 重要提醒

### ✅ 正確做法
- **開啟專案根目錄** (`mcp-registry`)
- **確保 IntelliJ 能識別到完整的 Maven 多模組結構**
- **所有 Java 檔案應該被正確標記為 Java 檔案**

### ❌ 錯誤做法
- 不要直接開啟 `mcp-registry-java` 子目錄
- 不要開啟個別的子模組目錄

## 驗證設定是否正確

1. **檢查 Project Structure**
   - `File` → `Project Structure` → `Modules`
   - 應該看到所有 Maven 模組被自動識別

2. **檢查 Maven 面板**
   - 開啟 Maven 工具視窗 (通常在右側)
   - 應該看到完整的模組樹狀結構

3. **測試編譯**
   - 在 Maven 面板中執行 `mcp-registry-parent` → `Lifecycle` → `compile`
   - 或在終端機執行：`mvn clean compile`

## 預期結果

設定完成後，你應該能夠：
- ✅ IntelliJ 正確識別所有 Java 檔案
- ✅ 語法高亮和自動完成正常工作
- ✅ 能夠在模組間導航
- ✅ Maven 編譯成功

## 技術棧資訊

- **Java**: 17
- **Spring Boot**: 3.2.1
- **Maven**: 多模組專案
- **架構**: Clean Architecture + DDD

## 故障排除

如果 Java 檔案仍無法被識別：

1. **重新載入 Maven 專案**
   - Maven 面板 → 重新整理按鈕

2. **清理並重建**
   ```bash
   mvn clean compile
   ```

3. **檢查 JDK 設定**
   - `File` → `Project Structure` → `Project`
   - 確保 Project SDK 設定為 Java 17

4. **重新匯入專案**
   - 關閉 IntelliJ
   - 重新開啟並選擇根目錄的 pom.xml