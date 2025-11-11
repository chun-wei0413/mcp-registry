# Spec 檔案同步機制

## 📝 概述

為了讓不同 AI 助手（Claude、Gemini 等）能夠使用相同的開發規範，本專案使用以下檔案結構：

```
Spec.md       # 主要規範檔案（唯一維護）
CLAUDE.md     # Claude AI 使用的規範（自動同步）
GEMINI.md     # Gemini AI 使用的規範（自動同步）
```

## 🔄 同步機制

由於 Windows 對 symbolic link 的限制，本專案使用 **Git pre-commit hook** 自動同步這些檔案。

### 工作原理

1. **只編輯 `Spec.md`** - 這是唯一需要手動維護的檔案
2. **自動同步** - 每次 commit 時，如果 `Spec.md` 有變更，會自動複製到 `CLAUDE.md` 和 `GEMINI.md`
3. **透明化** - 變更會自動加入到 commit 中

### Pre-commit Hook 位置

```
.git/hooks/pre-commit
```

## ✏️ 如何更新規範

### 方法 1: 直接編輯（推薦）

```bash
# 1. 編輯主要檔案
vim Spec.md

# 2. Git commit 會自動同步
git add Spec.md
git commit -m "Update spec"
# Hook 會自動執行並同步到 CLAUDE.md 和 GEMINI.md
```

### 方法 2: 手動同步

如果需要手動同步（不透過 commit）：

```bash
cp Spec.md CLAUDE.md
cp Spec.md GEMINI.md
```

## 🛠️ 故障排除

### Hook 沒有執行？

確認 hook 是可執行的：

```bash
chmod +x .git/hooks/pre-commit
```

### 需要檢查同步狀態？

```bash
# 比較檔案差異
diff Spec.md CLAUDE.md
diff Spec.md GEMINI.md

# 如果有差異，手動同步
cp Spec.md CLAUDE.md && cp Spec.md GEMINI.md
```

### Hook 內容

查看 `.git/hooks/pre-commit` 的內容：

```bash
#!/bin/bash
# Pre-commit hook: Sync Spec.md to CLAUDE.md and GEMINI.md

# Check if Spec.md has changes
if git diff --cached --name-only | grep -q "^Spec.md$"; then
    echo "Syncing Spec.md to CLAUDE.md and GEMINI.md..."
    cp Spec.md CLAUDE.md
    cp Spec.md GEMINI.md
    git add CLAUDE.md GEMINI.md
    echo "✓ Synced successfully"
fi
```

## 📋 注意事項

1. **永遠不要直接編輯 `CLAUDE.md` 或 `GEMINI.md`** - 這些檔案會被自動覆蓋
2. **只維護 `Spec.md`** - 這是唯一的來源
3. **Commit 前確認** - 確認 `CLAUDE.md` 和 `GEMINI.md` 已同步
4. **Clone 後檢查** - 新 clone 的 repo 需要確認 hook 是否可執行

## 🔍 驗證同步

在 commit 之前，可以驗證三個檔案內容相同：

```bash
# 使用 md5sum 或 sha256sum
md5sum Spec.md CLAUDE.md GEMINI.md

# 或使用 diff
diff -s Spec.md CLAUDE.md
diff -s Spec.md GEMINI.md
```

## 🎯 為什麼不用 Symbolic Link？

Windows 系統對 symbolic link 有以下限制：

1. **需要管理員權限** - 建立 symlink 需要提升權限
2. **Git 支援不完整** - Git 在 Windows 上對 symlink 的支援有限
3. **跨平台問題** - 不同系統對 symlink 的處理方式不同

因此，使用 **Git hook + 副本** 的方式更穩定且跨平台。
