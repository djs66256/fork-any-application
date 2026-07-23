# Plan 撰写规范

Plan 是本阶段连接「技术方案」和「具体代码实现」的桥梁。采用**轻量 TDD** 方式：每个步骤先定义测试场景，再描述实现内容。

## 执行者

主 agent 派发 subagent（各端可并行）。

## 前置条件

- `design-human-review` 阶段已通过
- 各端 design-{platform}.md 已就绪

## 轻量 TDD 流程

每个实现步骤遵循四步循环：

1. **定义测试用例**：列出测试场景的输入和预期输出
2. **编写代码**：实现对应功能
3. **补充测试**：完成代码后补充更多边界测试
4. **验证通过**：运行测试确认全部通过，回写变更文件列表

Plan 中每个步骤包含：
- 关联的测试编号
- 目标文件路径
- 实现内容描述
- 验证方式（命令）

## 派发方式

主 agent 为各涉及平台并行派发 plan subagent：

```
Subagent：
  description: "Plan：<platform>-<feature-name>"
  prompt: |
    你是一个 <platform> 端开发计划撰写 agent。

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill，指定阶段：`plan-platforms`
    2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`
    3. 读取技术方案 `docs/specs/<YYYY-MM-dd>-<name>/design-<platform>.md`
    4. 读取共享设计 `docs/specs/<YYYY-MM-dd>-<name>/design.md`

    > **注意**：`<platform>/CLAUDE.md` 在访问 `<platform>/` 目录时会自动加载，无需显式读取。

    ## 任务

    按 `assets/plan-template.md` 模板，撰写 <platform> 端的实现计划，
    输出到 `docs/specs/<YYYY-MM-dd>-<name>/plan-<platform>.md`。

    ## 测试要求

    各端测试要求：
    - Web：业务逻辑、状态转换、数据校验的改动需补充测试
    - Backend：需要编写单元测试；业务逻辑、参数校验、数据转换的改动同步补齐测试
    - iOS：每个场景都需要有单元测试；新增业务逻辑同步补齐测试
    - Android：每个场景都需要有单元测试；新增业务逻辑同步补齐测试

    ## 步骤设计原则

    - 每个步骤聚焦一个可独立验证的改动单元
    - 步骤粒度：一个步骤 = 一个测试场景 + 对应的实现（小步快跑）
    - 步骤数建议 3-8 个，太多说明需要拆分为子任务
    - 步骤之间标注依赖关系

    ## 完成标志

    - plan-<platform>.md 已写入，所有步骤清晰可执行
```

### 并行性

各平台 plan subagent 可同时派发，互不依赖。**优先使用 agent team 模式派发**，详见 [references/agent-team.md](agent-team.md)。全部完成后调用 `workflow.py mark-platform plan-platforms <platform> --status completed`，全部标记完成后调用 `workflow.py advance` 推进到 coding-platforms。

## 注意事项

- Plan 关注「怎么做」（实现细节），design 关注「做什么」（架构设计）
- 测试场景必须先列出，实现步骤再描述
- 步骤间如有依赖关系需明确标注
- 如某端不涉及，跳过该端的 subagent 派发
