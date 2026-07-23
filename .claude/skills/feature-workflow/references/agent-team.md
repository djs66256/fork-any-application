# Agent Team 能力参考

Agent Team 是 Claude Code 的多 agent 协作能力，将多个专业 subagent 组织成一个团队并行工作，由 team lead 统一调度和汇总结果。

## 前置条件

Agent Team 是**实验性功能**，需要以下条件全部满足：

- Claude Code v2.1.32+
- Opus 4.6 模型
- 环境变量 `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`

> 未启用时回退到独立 subagent 模式派发，功能和行为保持一致。

## 核心理念

单个 agent 处理复杂多平台任务时上下文窗口利用率高、等待时间长。Agent team 按职能（平台、领域）将任务拆分给多个 teammate，每个 teammate 拥有独立的上下文和 git worktree，并行执行后由 team lead 汇总。

## 团队结构

```
Team Lead（主 agent）
  ├── coding-ios     # iOS 端编码
  ├── coding-backend  # Backend 端编码
  └── coding-android  # Android 端编码
```

- **Team Lead**：拆解任务、派发 teammate、汇总结果
- **Teammate**：独立执行被分配的子任务，在隔离的 git worktree 中工作

## 协调机制

Teammate 之间通过两个渠道协调：

- **共享任务板**（`.claude/tasks/`）：Agent 在此认领、追踪、完成结构化任务
- **点对点消息**：Teammate 之间可直接通信，用于讨论方案、同步进度

Team lead 负责最终汇总所有 teammate 的结果。

## 工作隔离

每个 teammate 运行在独立的 git worktree 中，代码变更仅在 teammate 完成并通过验证后才合并，避免文件冲突。

## 如何触发

无需特殊命令，在自然语言中描述并行拆解意图即可。例如：

> 「用 agent team 并行完成 iOS、Android、Backend 三端的编码任务」

Team lead 会拆解任务、派发 teammate，并在完成后汇总结果。

## 职能拆解参考

feature-workflow 中适合 agent team 的场景通常按**平台**维度拆解：

| 阶段 | Teammate 命名示例 | 派发 prompt 来源 |
|------|------------------|-----------------|
| design-platforms | `design-<platform>` | [design-writing.md](design-writing.md) |
| design-review (Phase 2) | `review-design-<platform>` | [design-review.md](design-review.md) |
| plan-platforms | `plan-<platform>` | [plan-writing.md](plan-writing.md) |
| coding-platforms | `coding-<platform>` | [coding.md](coding.md) |

每个 teammate 的 prompt 复用对应阶段 reference 中已定义的 subagent prompt，替换 `<platform>` 和 `<feature-name>` 占位符即可，无需重新定义。

不涉及的平台不分配 teammate。

## 注意事项

- **写冲突**：多个 teammate 修改同一文件时可能产生合并冲突，按平台维度拆解可有效避免
- **Token 消耗**：N 个 teammate 约等于 N 倍 token 消耗
- **上下文隔离**：teammate 之间无法看到对方的完整上下文，关键信息需通过消息显式传递
- **模型统一**：所有 teammate 使用相同模型，不支持为不同类型 teammate 指定不同模型
- **实验性**：功能可能变化或被移除，需保持回退路径
