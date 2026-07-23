# Coding Subagent 派发规范

coding-platforms 阶段是核心实现阶段。每端派发一个 coding subagent，subagent 内部再派发 code-review subagent 进行审查。

## 前置条件

- `plan-platforms` 阶段已完成
- 各端 plan-{platform}.md 已就绪

## 派发方式

主 agent 为各涉及平台并行派发 coding subagent：

```
Subagent：
  description: "Coding：<platform>-<feature-name>"
  prompt: |
    你是一个 <platform> 端开发 agent。

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill，获取完整规范上下文
    2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`
    3. 读取共享技术方案 `docs/specs/<YYYY-MM-dd>-<name>/design.md`
    4. 读取 <platform> 端方案 `docs/specs/<YYYY-MM-dd>-<name>/design-<platform>.md`
    5. 读取实现计划 `docs/specs/<YYYY-MM-dd>-<name>/plan-<platform>.md`
    6. 读取 `<platform>/CLAUDE.md` 了解开发规范和测试要求

    ## 执行流程

    按 plan-<platform>.md 中的步骤顺序执行，每个步骤：

    1. **定义测试**：按 plan 中定义的测试场景编写测试用例
    2. **实现代码**：编写满足测试的代码
    3. **运行验证**：执行测试命令，确认新增测试通过
    4. **补充测试**：针对边界场景补充额外测试
    5. **运行全量验证**：确认不引入 regression
    6. **回写变更**：将变更文件及其内容简介回写到 plan 中

    ## 约束

    - 仅修改 `<platform>/` 目录下的文件
    - 禁止硬编码常量（localhost、固定 token、固定环境地址等）
    - API 调用遵循 RESTful 设计
    - 新增开源依赖需确认（如 plan 中未列出，暂停并询问）
    - 代码风格遵守 `<platform>/CLAUDE.md` 中的规范

    ## Code Review

    所有步骤完成后，在 subagent 内部派发 code-review subagent 进行内审：

    ```
    Subagent：
      description: "Code Review：<platform>-<feature-name>"
      prompt: |
        你对 <platform> 端代码进行质量审查。

        ## 准备

        1. 通过 Skill 工具加载 `feature-workflow` skill
        2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`
        3. 读取 <platform> 端方案 `docs/specs/<YYYY-MM-dd>-<name>/design-<platform>.md`
        4. 读取实现计划 `docs/specs/<YYYY-MM-dd>-<name>/plan-<platform>.md`
        5. 读取 `<platform>/CLAUDE.md`
        6. 使用 `git diff main --name-only` 获取变更文件列表

        ## 审查维度

        ### 通用维度
        - [ ] 实现是否与 plan 一致
        - [ ] 实现是否与 design 一致
        - [ ] 所有测试是否通过
        - [ ] 是否有硬编码常量
        - [ ] 代码风格是否符合平台规范
        - [ ] 是否有明显的性能问题
        - [ ] 错误处理是否完备

        ### <platform> 专属维度
        - Backend：API 响应格式是否统一、参数校验是否完备、是否有 SQL 注入风险
        - iOS：内存管理、线程安全、View 层级
        - Android：内存泄漏、线程安全、Lifecycle 感知
        - Web：响应式设计、可访问性、bundle size

        ## 修复

        发现问题后：
        - 可自行修复的：直接修改代码，然后重新运行测试验证
        - 无法自行修复的：记录到 review 文档

        ## 输出

        按 `assets/code-review-template.md` 模板，将 review 结果输出到
        `docs/specs/<YYYY-MM-dd>-<name>/code-<platform>-review.md`。

        ## Review 循环

        修复完成后重新自检，直到所有可修复问题都已处理。
        如果 3 轮循环后仍有问题，上报给主 agent。
    ```

    ## Code Review 循环

    code-review subagent 完成后：

    1. 读取 `code-<platform>-review.md`，检查是否有遗留问题
    2. 如有可自行修复的问题：修复代码 → 重新运行测试 → 重新派发 code-review subagent
    3. 如有需要人工判断的问题：记录并继续
    4. 循环直到无新增问题或达到 3 轮上限

    ## 完成后

    Code review 循环完成后，向主 agent 报告编码和 review 结果摘要。
```

## 主 agent 后续操作

1. Coding subagent 返回后，检查 review 文档
2. 调用 `workflow.py mark-platform coding-platforms <platform> --status completed`
3. 如有遗留问题需要人工介入，向用户展示并等待回复
4. 全部平台完成后，调用 `workflow.py advance` 推进到 code-human-review

## 并行性

- 各平台 coding subagent **可并行派发**
- 每个 coding subagent 内部的 code-review subagent **串行执行**（在同一 coding subagent 内）

## 各端约束参考

各端 CLAUDE.md 中的测试和规范要求 summary：

| 端 | 测试要求 | 特殊约束 |
|----|---------|---------|
| Backend | 单元测试；业务逻辑、参数校验、数据转换的改动同步补齐 | API RESTful，统一响应格式 |
| Web | 业务逻辑、状态转换、数据校验的改动需补充测试 | React/Vue 规范 |
| iOS | 每个场景都需要有单元测试 | SwiftUI/UIKit，内存管理 |
| Android | 每个场景都需要有单元测试 | Compose/View，Lifecycle |

## 完成标志（单平台）

- 所有 plan 步骤已完成
- 所有测试通过
- code-{platform}-review.md 已输出
- 所有可修复的 review 问题已解决
