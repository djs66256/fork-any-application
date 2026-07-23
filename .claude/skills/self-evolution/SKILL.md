---
name: self-evolution
description: >
  项目 harness 自我进化 skill。从会话中收集改进素材，积累到阈值后通过 skill-creator 的 evals 流程
  驱动 skill 进化，或直接优化 CLAUDE.md 等文档。
  触发场景：用户提到"记录一下"、"回顾会话"、"改进 harness"、"进化"、"优化工作流"、
  "有哪些可以改进的"、"self-evolution"、"总结经验"。
  仅用户主动触发，不做自动提示。
---

# Self Evolution

## 定位

self-evolution 是项目 AI harness 的「自我进化引擎」。不参与业务功能开发，而是让 harness 本身（skills、CLAUDE.md、短期记忆）持续变好。

核心流程：**会话中发现问题 → 用户触发记录 → 素材积累 → 用户触发进化 → 改进落地**。

## 进化主线

| 进化主线 | 作用域 | 进化方式 |
|---------|--------|---------|
| **Skill 进化** | `.claude/skills/` 下已有 skill | 通过 skill-creator（三方外部 skill）的 evals 流程：设计用例 → 评估效果 → 迭代优化 |
| **新增 Skill** | 记录类型为 `skill-missing` | 先询问用户是否需要新增，需要则走 skill-creator 的 create 流程 |
| **文档进化** | `CLAUDE.md`、`PRODUCT.md`、各端 `CLAUDE.md` | 分析记录 → 制定方案 → 用户审核 → 直接修改 |
| **记忆进化** | `docs/short_memory.md` | 在 skill/文档进化时同步处理：提升为长期规则后清理 |

记忆是过渡态：`docs/short_memory.md` 存放「每次会话都要重新说明的上下文」。同类记忆多次出现时，在进化流程中提炼为 skill reference 或 CLAUDE.md 的长期规则。

## 能力线

| 能力线 | 职责 | 执行者 | 规范 |
|--------|------|--------|------|
| **记录** | 回顾会话，提取改进素材 | 主 agent（不用 subagent） | [references/record.md](references/record.md) |
| **进化** | 分析累积记录，按改进目标走不同分支执行 | 主 agent | [references/evolve.md](references/evolve.md) |

## 记录概览

详细流程见 [references/record.md](references/record.md)。

- 仅用户主动触发
- 记录 10 种类型（含 `rework`、`memory-lookup`）和 4 级严重程度
- 每条记录含「改进目标」字段，用于按目标分组触发进化阈值
- 同时评估 `docs/short_memory.md` 的利用情况
- 无问题且短期记忆无异常则不创建文件
- 产物：`docs/evolution/<YYYY-MM-dd>-<name>/record.md` + 更新 `docs/evolution/index.md`

## 进化概览

详细流程见 [references/evolve.md](references/evolve.md)。

- 触发条件（合并为一次提示）：用户主动 / 同一改进目标 ≥ 3 条待进化 / 存在 blocker
- 分支：
  - 已有 skill → skill-creator evals 流程
  - `skill-missing` → 先询问用户是否新建
  - CLAUDE.md/文档 → 直接修改流程
  - `short_memory.md` → 随 skill/文档进化同步清理
- 产物：`docs/evolution/<YYYY-MM-dd>-<name>/evolution.md`

## 与已有 Skill 的关系

self-evolution 处于元层次：观察项目路径中所有 skill 和 CLAUDE.md 的表现，驱动它们的改进。

## 关键约束

- **仅用户触发**：不做自动提示
- **无问题不产出**：回顾后没问题就不创建文件
- **先记录后进化**：进化变更必须有 record 来源
- **用户审核必须**：进化方案经用户确认后才能执行
- **Skill 进化走 evals**：修改 skill 时通过 skill-creator 的测试用例评估效果
- **记忆生命周期**：短期记忆在进化时提升为长期规则后清理

## 资源索引

| 文件 | 用途 |
|------|------|
| [references/record.md](references/record.md) | 记录流程：触发方式、回顾维度、类型定义、严重程度判定、写入流程、短期记忆格式 |
| [references/evolve.md](references/evolve.md) | 进化流程：触发标准、进化分支、skill evals 流程、文档直接修改流程 |
| [assets/record-template.md](assets/record-template.md) | record.md 模板 |
| [assets/evolution-template.md](assets/evolution-template.md) | evolution.md 模板 |
| [assets/index-template.md](assets/index-template.md) | docs/evolution/index.md 模板 |
