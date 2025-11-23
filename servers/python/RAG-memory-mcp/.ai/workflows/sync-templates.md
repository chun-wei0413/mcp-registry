# Workflow: 同步範本 (Sync Templates)

## 📋 概述
此工作流程用於同步 AI 編碼範本與專案程式碼，確保範本保持最新且正確反映專案的設計模式。

## 🎯 目標
- 檢查範本與專案程式碼的同步狀態
- 識別需要更新的範本
- 更新過期範本並管理版本號
- 產生同步報告

## 📝 前置條件
- [ ] **[待實作]** 需要建立 `.ai/scripts/check-template-sync.sh`
- [ ] **[待實作]** 需要建立 `.ai/scripts/update-template-version.sh`
- [ ] 存在 `.versions.json` 版本控制檔案
- [ ] 具有 jq 工具（用於處理 JSON）

> ⚠️ **注意**: 目前 check-template-sync.sh 和 update-template-version.sh 尚未實作。
> 可先手動進行範本同步，或參考 `.ai/scripts/IMPLEMENTATION-PLAN.md` 了解實作計畫。

## 🔄 工作流程步驟

### 1. 檢查同步狀態
```bash
# 執行同步檢查
.ai/scripts/check-template-sync.sh report

# 若需要詳細資訊
.ai/scripts/check-template-sync.sh detailed
```

**預期輸出**：
- 同步狀態摘要
- 需要更新的範本清單
- 統計資訊（已同步/待更新）

### 2. 分析差異
對於每個需要更新的範本：

```bash
# 比較範本與來源檔案
diff -u .ai/tech-stacks/java-ca-ezddd-spring/examples/[template] \
        src/main/java/.../[source]
```

**決策點**：
- 結構變更 → 準備 minor 版本更新
- 新增功能 → 準備 patch 版本更新
- 破壞性變更 → 準備 major 版本更新
- 無重要變更 → 跳過

### 3. 更新範本內容

#### 3.1 單一範本更新
```bash
# 編輯範本檔案
vim .ai/tech-stacks/java-ca-ezddd-spring/examples/[template]

# 更新版本號
.ai/scripts/update-template-version.sh update \
    [template] [major|minor|patch] "[change description]"
```

#### 3.2 批次更新
```bash
# 批次更新特定類別
.ai/scripts/update-template-version.sh batch \
    "usecase/.*" patch "Sync with latest patterns"
```

### 4. 驗證更新
```bash
# 重新執行檢查確認更新成功
.ai/scripts/check-template-sync.sh report

# 檢查版本更新
.ai/scripts/update-template-version.sh list
```

### 5. 更新索引文檔
- 編輯 `TEMPLATE-INDEX.md` 更新版本資訊
- 確認新增或移除的範本已反映在索引中

### 6. 產生同步報告
```bash
# 產生報告（可選）
cat > .ai/reports/sync-report-$(date +%Y%m%d).md << EOF
# 範本同步報告 - $(date +%Y-%m-%d)

## 摘要
- 檢查範本數：$TOTAL
- 已更新：$UPDATED
- 跳過：$SKIPPED

## 更新清單
$(cat updated-templates.txt)

## 下次檢查建議
- 日期：$(date -d "+1 month" +%Y-%m-%d)
- 重點：patterns 類別
EOF
```

### 7. 提交變更
```bash
git add .ai/
git commit -m "chore: Sync templates with project patterns

- Updated X templates to match latest implementations
- Version bumps: Y minor, Z patch
- All templates now synced as of $(date +%Y-%m-%d)"
```

## ⏱️ 執行時機

### 建議執行頻率
| 觸發條件 | 頻率 | 範圍 |
|---------|------|------|
| 定期檢查 | 每週 | 全部範本 |
| 重大重構後 | 立即 | 受影響範本 |
| 新增模式後 | 立即 | 新增範本 |
| PR 前 | 每次 | 變更相關範本 |

### 自動化選項
```yaml
# 可設定為 cron job
0 9 * * 1  # 每週一早上 9 點

# 或整合到 CI/CD
on:
  schedule:
    - cron: '0 1 * * 1'
  workflow_dispatch:
```

## 📊 成功指標
- [ ] 所有範本通過同步檢查
- [ ] 版本號正確遞增
- [ ] 索引文檔已更新
- [ ] 無編譯錯誤
- [ ] 測試範本可正常使用

## 🚨 異常處理

### 常見問題
1. **檢查腳本執行失敗**
   - 檢查執行權限：`chmod +x .ai/scripts/*.sh`
   - 確認 jq 已安裝：`which jq`

2. **版本檔案損壞**
   - 從備份恢復：`cp .versions.json.backup .versions.json`
   - 或重新建立

3. **大量範本需要更新**
   - 分批處理，優先更新 critical patterns
   - 考慮是否需要架構調整

## 📚 相關資源
- [範本同步規範](../tech-stacks/java-ca-ezddd-spring/TEMPLATE-SYNC-GUIDE.md)
- [範本索引](../tech-stacks/java-ca-ezddd-spring/examples/TEMPLATE-INDEX.md)
- [同步工作流程](../tech-stacks/java-ca-ezddd-spring/TEMPLATE-SYNC-WORKFLOW.md)

## 📝 注意事項
- 保持範本簡潔，移除專案特定邏輯
- 更新範本時同步更新相關文檔
- 重大變更需要團隊討論
- 保留變更歷史以便追蹤