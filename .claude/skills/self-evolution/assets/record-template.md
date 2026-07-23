# 会话回顾记录

## 基本信息

| 字段 | 值 |
|------|-----|
| 日期 | YYYY-MM-DD |
| 会话名称 | <核心任务名，如 fix-auth-bug、design-player-speed> |
| 涉及 Skill | <逗号分隔，如 feature-workflow, llm-wiki> |

---

## 记录条目

### 条目 1：<简短标题，5-10 个字>

| 字段 | 值 |
|------|-----|
| **类型** | skill-design / skill-missing / claude-md / memory-gap / process-bottleneck / rework / memory-lookup / tool-experience / coordination / other |
| **严重程度** | blocker / repetitive / friction / observation |
| **上下文** | 当时在做什么任务？哪个 skill 的哪个阶段？ |
| **问题描述** | 具体发生了什么？为什么这是个问题？ |
| **期望行为** | 理想情况下应该怎么运作？ |

---

### 条目 2：<简短标题>

<同上格式>

---

## 短期记忆利用评估

<如果 `docs/short_memory.md` 不存在或为空，写「无短期记忆，跳过评估」>

| 记忆主题 | 状态 | 累计利用 | 本次利用场景 | 评估 |
|---------|------|---------|------------|------|
| <主题> | <有效/过时/待验证> | N 次 | <在什么任务/步骤中用了这条记忆，解决了什么问题> / 未使用 | <记对了帮助到了 / 应该用但漏了 / 信息已过时> |

**评估结论**：<一句话总结本次短期记忆的利用质量>

---

## 总结

| 指标 | 数量 |
|------|------|
| 总记录数 | N |
| blocker | N |
| repetitive | N |
| friction | N |
| observation | N |

**说明**：<一句话总结本次会话的 harness 表现>
