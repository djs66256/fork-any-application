---
description: 自动推进 feature-workflow 直到完成，人工确认步骤直接通过
---

## 流程概述

本命令自动推进 feature-workflow 全流程，包括 worktree 生命周期管理：

1. **创建 worktree** — 使用 `EnterWorktree` 工具创建隔离开发环境
2. **推进 feature-workflow** — 调用 `Skill("feature-workflow")`，所有人工确认步骤自动通过
3. **提交并合并回 master** — 将所有变更 commit，合并到 master 并推送
4. **清理 worktree** — 使用 `ExitWorktree` 工具删除 worktree

## 执行步骤

### 1. 创建 worktree

调用 `EnterWorktree` 工具，`name` 参数格式为 `YYYY-MM-dd-<feature-name>`（使用当天的日期和需求名）。

进入 worktree 后，执行 init：

```bash
python3 scripts/workflow.py init <name>
```

### 2. 推进 feature-workflow

调用 `Skill("feature-workflow")`，推进需求「$ARGUMENTS」直到完成功能。流程中所有需要人类批准的步骤（spec-human-review、design-human-review、code-human-review）直接通过，不暂停等待用户确认。

注意：feature-workflow skill 内部会按自身的 15 阶段流程推进，主 agent 需要在各人工确认阶段自动调用 `python3 scripts/workflow.py human-review <stage> --approve` 通过。

### 3. 提交并合并回 master

feature-workflow 所有阶段完成后（阶段 15 completed），将 worktree 中的所有变更提交并合并回 master：

```bash
# 确认当前在 worktree 目录中
pwd

# 提交所有未提交的变更
git add .
git commit -m "chore: fast-forward complete - $ARGUMENTS"

# 推送 feature 分支到远程
git push origin feature/$(date +%Y-%m-%d)-<name>

# 回到主仓库并合并
cd <project-root>
git checkout master
git pull origin master
git merge --no-ff feature/$(date +%Y-%m-%d)-<name>
git push origin master
```

### 4. 清理 worktree

调用 `ExitWorktree` 工具，参数：
- `action`: `"remove"`
- `discard_changes`: `true`（如有未提交变更则丢弃，因为第 3 步已提交）
