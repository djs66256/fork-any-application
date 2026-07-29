# 进化记录

## 基本信息

| 字段 | 值 |
|------|-----|
| 日期 | 2026-07-29 |
| 会话名称 | fix-workflow-script-path |
| 进化来源 | [record.md](record.md) 条目 1 |
| 进化分支 | skill-design → SKILL.md 直接修改 |

## 变更摘要

在 `SKILL.md` 中新增「路径约定」章节，声明本 skill 文档及所有 reference 文件中出现的相对路径（`scripts/`、`references/`、`assets/` 等）均以 skill 根目录为基准。同步更新 `fast-forward.md` 中的路径说明。

## 变更文件

| 文件 | 变更 |
|------|------|
| `.claude/skills/feature-workflow/SKILL.md` | 在「定位」后新增「路径约定」章节 |
| `.claude/commands/fast-forward.md` | 新增「路径约定」章节，并明示 init 步骤需 cd 到 skill 目录执行 |

## 设计决策

- **不写死绝对路径**（如 `.claude/skills/feature-workflow/`）：不同 agent 加载 skill 的路径可能不同
- **声明路径基准而非替换每条命令**：SKILL.md 和所有 reference 文件中保持现有的 `scripts/workflow.py` 写法不变，通过顶层的路径约定声明来解决歧义
- **reference 文件不改动**：worktree.md、spec-writing.md、qa-blackbox-testing.md 中的 `scripts/workflow.py` 引用由 SKILL.md 的路径约定覆盖，无需逐文件修改
