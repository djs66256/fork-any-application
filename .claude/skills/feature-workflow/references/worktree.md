# Worktree 操作规范

feature-workflow 使用 Claude 内置的 `EnterWorktree` / `ExitWorktree` 工具进行功能开发隔离。本规范定义 worktree 的创建、使用和合并流程。

## 约束

- **全部使用 `EnterWorktree` / `ExitWorktree` 内置工具**，不直接使用原生 `git worktree` 命令
- worktree 由 `EnterWorktree` 工具自动在 `.claude/worktrees/` 下创建和管理
- **分支命名**：`feature/<YYYY-MM-dd>-<name>`（与 spec 目录名一致）
- 创建 worktree 时，`EnterWorktree` 的 `name` 参数传入 `YYYY-MM-dd-<name>` 即可

## 创建 worktree

在 `worktree-setup` 阶段使用 `EnterWorktree` 工具创建 worktree：

1. 调用 `EnterWorktree` 工具，`name` 参数为 `YYYY-MM-dd-<name>`
2. 工具自动创建 worktree 并将会话切换到 worktree 目录
3. 进入后执行 `python3 scripts/workflow.py init <name>`

```bash
# 在 worktree 中执行 init
python3 scripts/workflow.py init <name>
```

## 日常开发约束

- 在 worktree 中修改代码，不要操作主仓库
- 定期 commit 以保存进度
- commit message 格式：`<type>(<scope>): <description>`
  - type: feat / fix / refactor / test / docs / chore
  - scope: 平台名（backend/ios/android/web）或 cross

## 退出 worktree（阶段结束时）

在 `worktree-merge` 阶段，代码合并完成后使用 `ExitWorktree` 工具退出并清理 worktree：

1. 先推送分支并合并到 main（通过 Bash 执行 git 命令）
2. 调用 `ExitWorktree` 工具，参数：
   - `action`: `"remove"`
   - `discard_changes`: `false`
3. 工具会自动清理 worktree 目录和关联分支

注意：在调用 `ExitWorktree` 之前，务必确认代码已合并到 main 并推送成功。

## 合回主干

在 `worktree-merge` 阶段执行：

```bash
# 1. 确认当前在 worktree 目录中
pwd

# 2. 推送分支到远程
git push origin feature/<YYYY-MM-dd>-<name>

# 3. 切回主仓库的 main 分支（需要先退出 worktree，或通过 cd <project-root> 切换）
cd <project-root>
git checkout main
git pull origin main

# 4. 合并 feature 分支（使用 --no-ff 保留分支历史）
git merge --no-ff feature/<YYYY-MM-dd>-<name>

# 5. 推送到远程
git push origin main
```

然后使用 `ExitWorktree` 工具退出并清理 worktree。

## 异常处理

| 情况 | 处理方式 |
|------|---------|
| worktree 创建失败（分支已存在） | 检查是否已有同名分支，如属于本项目则复用，否则换名 |
| 合并冲突 | 手动解决冲突后 `git add . && git commit` |
| ExitWorktree 失败（有未提交变更） | 先 `git add . && git commit` 或 `git stash`，然后重试 |
| main 有新的远程更新 | `git pull --rebase origin main` 后再 merge |

## 相关命令参考

```bash
# 列出所有 worktree
git worktree list

# 查看 worktree 详细信息
git worktree list --porcelain

# 删除已不使用的 worktree（包括主分支的 worktree）
git worktree prune
```
