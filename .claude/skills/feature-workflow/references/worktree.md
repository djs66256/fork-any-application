# Worktree 操作规范

feature-workflow 使用 git worktree 进行功能开发隔离。本规范定义 worktree 的创建、使用和合并流程。

## 约束

- **所有 worktree 放在 `.worktree/` 目录下**，`.gitignore` 已忽略此路径
- **分支命名**：`feature/<YYYY-MM-dd>-<name>`（与 spec 目录名一致）
- **worktree 路径命名**：`.worktree/<YYYY-MM-dd>-<name>`
- 使用原生 `git worktree` 命令，不使用 `EnterWorktree` / `ExitWorktree` 工具

## 创建 worktree

在 `worktree-setup` 阶段创建 worktree：

```bash
# 从 main 分支创建新分支和 worktree
git worktree add .worktree/<YYYY-MM-dd>-<name> -b feature/<YYYY-MM-dd>-<name>

# 验证创建成功
git worktree list
```

创建后进入 worktree 目录进行后续开发：

```bash
cd .worktree/<YYYY-MM-dd>-<name>
```

## 日常开发约束

- 在 worktree 中修改代码，不要操作主仓库
- 定期 commit 以保存进度
- commit message 格式：`<type>(<scope>): <description>`
  - type: feat / fix / refactor / test / docs / chore
  - scope: 平台名（backend/ios/android/web）或 cross

## 合回主干

在 `worktree-merge` 阶段执行：

```bash
# 1. 确认当前在 worktree 目录中
pwd  # 应显示 .worktree/<YYYY-MM-dd>-<name>

# 2. 推送分支到远程
git push origin feature/<YYYY-MM-dd>-<name>

# 3. 切回主仓库的 main 分支
cd <project-root>
git checkout main
git pull origin main

# 4. 合并 feature 分支（使用 --no-ff 保留分支历史）
git merge --no-ff feature/<YYYY-MM-dd>-<name>

# 5. 推送到远程
git push origin main

# 6. 清理 worktree
git worktree remove .worktree/<YYYY-MM-dd>-<name>

# 7. 删除本地分支（可选）
git branch -d feature/<YYYY-MM-dd>-<name>
```

## 异常处理

| 情况 | 处理方式 |
|------|---------|
| worktree 创建失败（分支已存在） | 检查是否已有同名分支，如属于本项目则复用，否则换名 |
| 合并冲突 | 手动解决冲突后 `git add . && git commit` |
| worktree remove 失败（有未提交变更） | 先 `git add . && git commit` 或 `git stash`，然后重试 remove |
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
