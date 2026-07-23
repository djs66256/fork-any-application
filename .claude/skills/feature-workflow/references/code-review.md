# Code Review Subagent 规范

Code review 在 coding subagent **内部**创建，而非由主 agent 直接派发。本文件定义 code-review subagent 的标准 prompt 模板和审查维度。

## 触发时机

Coding subagent 完成所有 plan 步骤的实现后，在 subagent 内部创建 code-review subagent。

## Subagent 定义

```
Subagent：
  description: "Code Review：<platform>-<feature-name>"
  prompt: |
    你对 <platform> 端代码进行质量审查。

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill，获取 code review 规范
    2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`
    3. 读取 <platform> 端方案 `docs/specs/<YYYY-MM-dd>-<name>/design-<platform>.md`
    4. 读取实现计划 `docs/specs/<YYYY-MM-dd>-<name>/plan-<platform>.md`
    5. 读取 `<platform>/CLAUDE.md` 了解该端开发规范
    6. 使用 `git diff main --name-only` 获取变更文件列表

    ## 审查维度

    ### 通用维度（所有平台适用）

    - [ ] **plan 一致性**：实现是否严格按照 plan-<platform>.md 中的步骤执行？
    - [ ] **design 一致性**：实现是否符合 design-<platform>.md 中的架构设计？
    - [ ] **测试通过**：所有测试命令执行后是否全部通过？新增测试是否覆盖了关键场景？
    - [ ] **无硬编码**：是否包含硬编码的 URL、token、环境变量？（参考 CLAUDE.md 约束）
    - [ ] **代码风格**：命名、缩进、注释是否符合该平台 `CLAUDE.md` 要求？
    - [ ] **错误处理**：异常场景是否有适当的错误处理？
    - [ ] **性能**：是否有明显的 N+1 查询、不必要的重复渲染、内存泄漏等？
    - [ ] **API 一致性**：API 调用是否与 design.md 中定义的接口完全一致？

    ### Backend 专属维度

    - [ ] **RESTful 合规**：路径命名、HTTP 方法、状态码使用是否正确？
    - [ ] **响应格式统一**：是否使用了统一的 `{ code, data, message }` 结构？
    - [ ] **参数校验**：是否对所有输入做了 Zod schema 校验？
    - [ ] **数据库安全**：是否有 SQL 注入风险？是否有适当的索引？
    - [ ] **单元测试**：业务逻辑、参数校验、数据转换是否都有测试？

    ### iOS 专属维度

    - [ ] **内存管理**：是否有循环引用？weak/strong 引用是否正确？
    - [ ] **线程安全**：UI 更新是否在主线程？后台任务是否合理派发？
    - [ ] **View 层级**：View 层级是否合理？是否有不必要的嵌套？
    - [ ] **单元测试**：每个场景是否都有单元测试？

    ### Android 专属维度

    - [ ] **内存泄漏**：Context 引用是否正确？是否有未取消的协程/订阅？
    - [ ] **线程安全**：UI 更新是否在主线程？后台任务是否正确使用协程？
    - [ ] **Lifecycle 感知**：是否正确处理了 Activity/Fragment 生命周期？
    - [ ] **单元测试**：每个场景是否都有单元测试？

    ### Web 专属维度

    - [ ] **响应式设计**：是否适配了不同屏幕尺寸？
    - [ ] **可访问性**：是否有适当的 aria 标签、焦点管理？
    - [ ] **状态管理**：状态流转是否正确？是否有不必要的全局状态？
    - [ ] **Bundle 大小**：是否有不必要的依赖导入？
    - [ ] **测试覆盖**：业务逻辑、状态转换、数据校验的改动是否都有测试？

    ## 修复策略

    按优先级处理：

    1. **高严重度**（逻辑错误、安全漏洞、测试失败）→ 立即修复，修复后重新运行测试
    2. **中严重度**（规范违反、性能问题）→ 直接修复
    3. **低严重度**（代码风格、可维护性）→ 直接修复
    4. **需人工决策**（架构选择、权衡取舍）→ 记录到遗留问题

    ## 输出

    按 `assets/code-review-template.md` 模板输出 review 到：
    `docs/specs/<YYYY-MM-dd>-<name>/code-<platform>-review.md`

    ## Review 循环

    修复完成后，自检修复项并重新评估。如果 3 轮 review 后仍有问题，上报给 coding subagent 主流程。

    ## 完成标志

    - code-<platform>-review.md 已输出
    - 所有可自动修复的问题已解决
    - 遗留问题已清晰罗列
```

## 与主 agent 的关系

Code review subagent 由 coding subagent 内部创建和驱动，主 agent 不直接参与。原因是：

1. 减少主 agent 的协调负担
2. Coding subagent 拥有代码修改的上下文，更高效
3. Review 修复循环在 coding subagent 内部完成更快捷

主 agent 最终读取 `code-{platform}-review.md` 来判断是否存在需要人工决策的遗留问题。
