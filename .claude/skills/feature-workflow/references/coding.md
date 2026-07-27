# Coding Subagent 派发规范

## 占位符替换

本文件中 Subagent 定义块内的 `<YYYY-MM-dd>-<name>`、`<feature-name>`、`<platform>` 为占位符。主 agent 派发前需替换为实际值：
- `<YYYY-MM-dd>-<name>` → 实际日期和需求名（如 `2026-07-23-add-playback-speed`）
- `<feature-name>` → 可读的需求标题（如 `视频倍速播放`）
- `<platform>` → 目标平台名（`backend` / `ios` / `android` / `web`）

> **注意**：Coding subagent 加载 `references/<platform>-code-review/` 和 `references/common-code-review/` 中的子 subagent 定义时，同样需要将占位符替换为实际值后再派发。

coding-platforms 阶段是核心实现阶段。每端派发一个 coding subagent，subagent 内部进行 **build & lint → tests → review** 的渐进式验证循环。

## 前置条件

- `plan-platforms` 阶段已完成
- 各端 plan-{platform}.md 已就绪

## 派发方式

主 agent 为各涉及平台派发 coding subagent。

**派发优先级**：优先使用 agent team 模式，agent team 不可用时使用独立 subagent。详见 [references/agent-team.md](agent-team.md)。

> **重要约束**：coding subagent 只修改 `<platform>/` 目录下的文件，各端目录互不重叠，天然不会产生文件冲突。**派发 subagent 时不得使用 `isolation: 'worktree'`**。

```
Subagent：
  description: "Coding：<platform>-<feature-name>"
  prompt: |
    你是一个 <platform> 端开发 agent。

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill，指定阶段：`coding-platforms`
    2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`
    3. 读取共享技术方案 `docs/specs/<YYYY-MM-dd>-<name>/design.md`
    4. 读取 <platform> 端方案 `docs/specs/<YYYY-MM-dd>-<name>/design-<platform>.md`
    5. 读取实现计划 `docs/specs/<YYYY-MM-dd>-<name>/plan-<platform>.md`

    > **注意**：`<platform>/CLAUDE.md` 在访问 `<platform>/` 目录时会自动加载，无需显式读取。

    ## 模式检测

    开始工作前，先判断当前是「首次编码」还是「修复轮次」：

    1. 检查 `docs/specs/<YYYY-MM-dd>-<name>/code-<platform>-review.md` 是否已存在（有 review 报告说明之前已编码过）
    2. 检查 plan-<platform>.md 中的步骤进度标记

    ### 🔧 修复轮次（review 报告或进度标记已存在）

    说明：之前已完成编码并通过了部分验证，本次任务是**只修复** review 或人反馈中指出的问题。

    **修复流程：**
    1. 读取 `code-<platform>-review.md`，了解 review 发现的问题（🔴 阻塞 和 🟡 关注）
    2. 读取 `plan-<platform>.md`，查看当前进度（已完成步骤的 ✅ 标记）
    3. 使用 `git diff main --name-only` 了解当前变更范围
    4. **只修改/补充** review 报告中指出的具体问题，不重写整个实现
    5. 保留 plan 中的进度标记（已完成步骤的 ✅ 不要重置）
    6. **修复后逐项自检**：对照 review 报告中每个标记为需修复的问题，逐一确认已修改到位，不可仅凭"改了代码"就认为修复完成。在 review 文档的对应条目后标注修复状态和修改内容
    7. 修复后仍需通过 Build & Lint → Tests → Review 的渐进验证循环
    8. 修复完成后更新 `code-<platform>-review.md` 中的问题状态

    ### 🆕 首次编码（review 报告不存在）

    按 plan-<platform>.md 中的步骤顺序，从第一步开始完整实现。

    ## 代码修改范围

    你**只能修改 `<platform>/` 目录下的文件**。

    禁止修改：
    - 其他平台的代码目录
    - `docs/`、`wiki/` 目录下的文档
    - 根目录配置文件（除非 plan 中明确要求）
    - `.claude/`、`scripts/` 等基础设施文件

    ## 进度持久化

    在 plan-<platform>.md 中维护进度。每个步骤完成后在对应步骤的「验证方式」旁标注 `✅ 已完成`。

    ## 执行流程

    按 plan-<platform>.md 中的步骤顺序执行。每个步骤遵循以下验证循环（按成本从低到高）：

    ```
    编写代码
      ↓
    Build & Lint（成本最低）  ←──────────┐
      ↓ 不通过则修复                      │
    Tests（成本中等）          ←──────────┤
      ↓ 不通过则修复                      │
    Review（成本最高）         ←──────────┘
      ↓ 不通过则修复后重走全流程
    通过 ✅
    ```

    ### 第 1 层：Build & Lint（成本最低）

    编写代码后首先执行 build 和 lint 检查：

    1. 运行 build 命令，确认编译通过
    2. 运行 lint 命令，确认无新增警告/错误
    3. 不通过 → 修复代码 → 重新第 1 层

    ### 第 2 层：Tests（成本中等）

    Build & Lint 通过后，执行测试验证：

    1. 按 plan 中定义的测试场景编写测试用例
    2. 运行新增测试，确认全部通过
    3. 运行全量测试，确认不引入 regression
    4. 针对边界场景补充额外测试
    5. 不通过 → 修复代码或测试 → 回到第 1 层

    ### 第 3 层：Review（成本最高）

    所有步骤的 Build & Tests 通过后，按照 [references/code-review.md](code-review.md) 执行 code review。

    Review 修复循环：
    1. Review 发现问题 → 按 code-review.md 中的修复策略处理
    2. 修复代码 → 回到第 1 层（Build & Lint）重走验证流程 → 重新 review
    3. 循环直到无新增问题或达到 3 轮上限

    ## 约束

    - 仅修改 `<platform>/` 目录下的文件
    - 禁止硬编码常量（localhost、固定 token、固定环境地址等）
    - API 调用遵循 RESTful 设计
    - 新增开源依赖需确认（如 plan 中未列出，暂停并询问）

    ## 验收标准（逐项自检）

    **所有以下条件必须满足才可向主 agent 报告完成。** 每项需提供新鲜证据（命令输出、文件内容），不得口头声明：

    | 验收项 | 验证方式 | 证据 |
    |--------|---------|------|
    | Build 通过 | 运行 build 命令 | 退出码为 0，无编译错误 |
    | Lint 通过 | 运行 lint 命令 | 退出码为 0，无新增警告/错误 |
    | 新增测试通过 | 运行新增测试 | 退出码为 0，所有新增用例通过 |
    | 全量测试通过 | 运行全量测试 | 退出码为 0，无 regression |
    | Review 通过 | 检查 code-review.md | 结论为「所有问题已修复」或仅有低严重度遗留项 |
    | 修改范围合规 | `git diff main --name-only` | 变更仅包含 `<platform>/` 目录下的文件 |
    | 无硬编码 | 检查变更内容 | 无硬编码 URL、token、环境变量 |
    | 修复逐项验证（修复轮次） | 对照上轮 code-review.md 逐项检查 | 每个标记「✅ 已修复」的问题确实已被修改到位，无假修复 |

    验收不通过的项目 → 修复后重新从第 1 层（Build & Lint）开始验证，直到全部通过。

    ## 完成后

    验收全部通过后，将变更文件及其内容简介回写到 plan 中，然后向主 agent 报告编码和 review 结果摘要。
```

## 并行性

- 各平台 coding subagent **可并行派发**
- 每个 coding subagent 内部的 code review 专项 subagent **并行派发**，review 修复循环 **串行执行**

## 主 agent 后续操作

1. Coding subagent 返回后，确认 subagent 自行验收已通过
2. 检查 review 文档是否已输出
3. 调用 `workflow.py mark-platform coding-platforms <platform> --status completed`
4. 如有遗留问题需要人工介入，向用户展示并等待回复
5. 全部平台完成后，调用 `workflow.py advance` 推进到 code-human-review
