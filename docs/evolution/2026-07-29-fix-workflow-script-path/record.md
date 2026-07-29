# 会话回顾记录

## 基本信息

| 字段 | 值 |
|------|-----|
| 日期 | 2026-07-29 |
| 会话名称 | fix-workflow-script-path |
| 涉及 Skill | feature-workflow |

---

## 记录条目

### 条目 1：workflow.py 路径在 worktree 中不可达

| 字段 | 值 |
|------|-----|
| **类型** | skill-design |
| **严重程度** | repetitive |
| **改进目标** | `.claude/skills/feature-workflow/SKILL.md`、`.claude/skills/feature-workflow/references/worktree.md`、`.claude/skills/feature-workflow/references/spec-writing.md`、`.claude/skills/feature-workflow/references/qa-blackbox-testing.md`、`.claude/commands/fast-forward.md` |
| **上下文** | 在 feature-workflow 的各个阶段推进时，agent 需要执行 `python3 scripts/workflow.py <command>` 来更新 workflow 状态。 |
| **问题描述** | `workflow.py` 脚本位于 skill 目录内（`.claude/skills/feature-workflow/scripts/workflow.py`），命令 `python3 scripts/workflow.py` 的路径基准是 skill 自身目录。但 SKILL.md 中未明确声明这一基准，导致 agent 在 worktree 或其它上下文中从错误目录执行命令而失败。Agent 每次推进阶段都需要手动定位脚本位置。 |
| **期望行为** | 在 SKILL.md 中声明命令路径以 skill 自身目录为基准（如「本 skill 中所有脚本路径相对于 skill 根目录，即 `scripts/workflow.py`」），而不写死从项目根出发的完整路径。不同 agent 根据声明自行解析到实际的脚本位置，避免因 worktree、agent 隔离等场景下路径不可达。 |

---

## 短期记忆利用评估

无短期记忆，跳过评估。

---

## 总结

| 指标 | 数量 |
|------|------|
| 总记录数 | 1 |
| blocker | 0 |
| repetitive | 1 |
| friction | 0 |
| observation | 0 |

**说明**：feature-workflow 中 `scripts/workflow.py` 路径基准是 skill 自身目录，但 SKILL.md 未明确声明。agent 在 worktree 等场景下从错误目录执行导致失败。修复方向：在 SKILL.md 中声明脚本路径以 skill 目录为基准。
