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

self-evolution 是项目 AI harness 的「自我进化引擎」。它不参与业务功能开发，而是让 harness 本身（skills、CLAUDE.md、短期记忆）持续变好。

核心流程：**会话中发现问题 → 用户触发记录 → 素材积累 → 用户触发进化 → 改进落地**。

```
会话 → [记录] → docs/evolution/ 素材池 → [进化] → Skill/CLAUDE.md
                   ↑                              ↓
             用户主动触发                    用户审核后执行
```

## 进化主线

| 进化主线 | 作用域 | 进化方式 |
|---------|--------|---------|
| **Skill 进化** | `.claude/skills/` 下所有 skill | 通过 skill-creator 的 evals 流程：设计用例 → 评估效果 → 迭代优化 |
| **文档进化** | `CLAUDE.md`、`PRODUCT.md`、各端 `CLAUDE.md` | 分析记录 → 制定方案 → 用户审核 → 直接修改 |
| **记忆进化** | `docs/short_memory.md` | 短期记忆捕获「每次会话都要重新说明的上下文」，进化时提升为长期规则 |

记忆是过渡态：`docs/short_memory.md` 存放「需要记住但还没到写进文档的程度」的上下文。同类记忆多次出现时，在进化流程中提炼为 skill reference 或 CLAUDE.md 的长期规则。

## 能力线

| 能力线 | 职责 | 触发 | 执行者 | 规范 |
|--------|------|------|--------|------|
| **记录** | 回顾会话，提取改进素材 | 用户主动调用 | 主 agent（不用 subagent） | [references/record.md](references/record.md) |
| **进化** | 分析累积记录，制定改进方案，经审核后执行 | 用户主动调用 | 主 agent | [references/evolve.md](references/evolve.md) |

## 记录

详细流程见 [references/record.md](references/record.md)。概览：

- **触发**：仅用户主动（"记录一下"、"回顾会话"等）
- **执行**：主 agent 回顾会话，按模板提取改进条目
- **无问题则不产出**：如果会话中没有发现值得记录的问题，不创建 record.md
- **产物**：`docs/evolution/<YYYY-MM-dd>-<name>/record.md` + 更新 `docs/evolution/index.md`

记录的字段只包含事实描述（类型、严重程度、上下文、问题描述、期望行为），不含建议和目标——那是进化阶段的工作。

## 进化

详细流程见 [references/evolve.md](references/evolve.md)。概览：

- **触发**：用户主动（"进化"、"优化 harness"），或同领域 3+ 条记录时主 agent 提示
- **分支**：
  - 目标是 **skill** → 走 skill-creator 的 evals 流程（设计测试用例 → 基线对比 → 迭代优化）
  - 目标是 **CLAUDE.md/文档** → 走直接修改流程（分析 → 方案 → 审核 → 执行）
- **产物**：`docs/evolution/<YYYY-MM-dd>-<name>/evolution.md`

## 与已有 Skill 的关系

self-evolution 处于**元层次**（meta-level）：观察项目路径中所有 skill 和 CLAUDE.md 的表现，驱动它们的改进。不通过被改进的 skill 来修改自身，避免死循环。

## 关键约束

- **仅用户触发**：记录和进化都不自动执行
- **无问题不产出**：回顾后没问题就不创建文件
- **先记录后进化**：进化变更必须有 record 来源
- **用户审核必须**：进化方案经用户确认后才能执行
- **Skill 进化走 evals**：修改 skill 时优先通过 skill-creator 的测试用例评估效果，避免主观判断
- **小步快跑**：单次进化不超过 5 个文件变更
- **记忆生命周期**：`docs/short_memory.md` 中的内容在进化时提升为长期规则，同步清理
- **产品信息引用**：涉及产品名、竞品名时引用 `PRODUCT.md`，不硬编码

## 资源索引

| 文件 | 用途 |
|------|------|
| [references/record.md](references/record.md) | 记录流程：触发方式、回顾方法、类型定义、严重程度判定、写入流程 |
| [references/evolve.md](references/evolve.md) | 进化流程：触发标准、分析→方案→审核→执行→归档全流程 |
| [assets/record-template.md](assets/record-template.md) | record.md 模板 |
| [assets/evolution-template.md](assets/evolution-template.md) | evolution.md 模板 |
