# Script Automation Workflow

## 目的
為專案創建和管理自動化腳本，提升開發效率和維護品質。

## 腳本類型

### 1. 知識管理腳本

#### 更新索引
**目的**：保持 .ai/INDEX.md 文件的更新
```bash
# 掃描新文件
find .ai -name "*.md" | while read file; do
  # 檢查是否在 INDEX.md 中
  # 添加缺失的文件
done

# 檢查無效連結
grep -oP '\]\(\./[^)]+\)' .ai/INDEX.md | while read link; do
  # 驗證連結有效性
done
```

#### 知識驗證
**目的**：確保文檔與代碼保持同步
```bash
# 比對文檔中的代碼範例與實際代碼
# 檢查文檔中提到的文件是否存在
# 驗證 API 文檔與實際 API 的一致性
```

#### 知識推薦
**目的**：根據當前任務推薦相關文檔
```bash
# 基於任務類型推薦文檔
# 分析代碼變更推薦需要更新的文檔
# 推薦相關的 tech-stack 資源
```

### 2. 代碼分析腳本

#### 統計分析
```bash
# 通用代碼統計
echo "=== 代碼統計 ==="
find . -type f -name "*.js" -o -name "*.ts" -o -name "*.py" -o -name "*.java" | wc -l
# 計算總行數
# 統計測試覆蓋率
# 找出 TODO 和 FIXME
```

#### 架構驗證
```bash
# 檢查架構層依賴
# 驗證命名規範
# 檢查循環依賴
```

### 3. 自動化工作流程

#### 文檔生成
```bash
# 從代碼生成 API 文檔
# 更新架構圖
# 生成變更日誌
```

#### 品質檢查
```bash
# 執行 linter
# 檢查格式
# 運行測試
# 安全掃描
```

## 創建腳本的指導原則

### 1. 通用性
- 使用環境變數而非硬編碼路徑
- 支援不同的專案結構
- 提供配置選項

### 2. 可移植性
- 使用 POSIX 兼容的 shell 語法
- 避免平台特定命令
- 提供跨平台替代方案

### 3. 錯誤處理
```bash
set -euo pipefail  # 嚴格錯誤處理
trap 'echo "Error on line $LINENO"' ERR
```

### 4. 文檔化
```bash
#!/bin/bash
# Script: update-index.sh
# Purpose: 更新知識庫索引
# Usage: ./update-index.sh [options]
# Options:
#   -v  verbose mode
#   -d  dry run
```

## 腳本模板

### 基礎模板
```bash
#!/bin/bash
set -euo pipefail

# 設定變數
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 函數定義
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

# 主邏輯
main() {
    log_info "開始執行..."
    # 腳本邏輯
    log_info "執行完成"
}

# 執行主函數
main "$@"
```

## 與其他系統整合

### Git Hooks
```bash
# .git/hooks/pre-commit
#!/bin/bash
.ai/scripts/validate-commits.sh
```

### CI/CD
```yaml
# GitHub Actions
- name: Validate Knowledge
  run: |
    .ai/scripts/validate-knowledge.sh
```

### IDE 整合
```json
// VS Code tasks.json
{
  "label": "Update AI Index",
  "type": "shell",
  "command": "${workspaceFolder}/.ai/scripts/update-index.sh"
}
```

## 腳本管理

### 1. 版本控制
- 所有腳本納入版本控制
- 記錄變更歷史
- 使用語義化版本

### 2. 測試
```bash
# 測試腳本
bats test/scripts/*.bats
shellcheck scripts/*.sh
```

### 3. 維護
- 定期檢查腳本有效性
- 更新過時的命令
- 改進性能

## 常用腳本範例

### 1. 快速設置環境
```bash
請創建一個設置開發環境的腳本
```

### 2. 批量更新文檔
```bash
請創建腳本批量更新所有 README 文件的版權年份
```

### 3. 自動化部署
```bash
請創建一個自動化部署腳本，包含測試和回滾功能
```

## 安全考量

1. **輸入驗證**
   - 驗證所有用戶輸入
   - 使用白名單而非黑名單
   - 避免 shell 注入

2. **權限管理**
   - 最小權限原則
   - 不以 root 運行
   - 檢查文件權限

3. **敏感資訊**
   - 不在腳本中硬編碼密碼
   - 使用環境變數或密鑰管理
   - 清理臨時文件

## 故障排除

### 常見問題
1. **權限錯誤**：使用 `chmod +x script.sh`
2. **路徑問題**：使用絕對路徑或 `$SCRIPT_DIR`
3. **依賴缺失**：在腳本開始檢查依賴

### 調試技巧
```bash
# 啟用調試模式
set -x  # 顯示執行的命令
bash -n script.sh  # 語法檢查
```

## 最佳實踐

1. **單一職責**：每個腳本只做一件事
2. **冪等性**：多次執行結果相同
3. **可觀察性**：提供足夠的日誌輸出
4. **優雅退出**：處理中斷信號
5. **配置外部化**：使用配置文件而非硬編碼