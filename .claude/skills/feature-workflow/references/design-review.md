# 技术方案 Review Subagent

design-review 阶段使用**单个 subagent**，一次性审查全部方案（design.md + 各端 design-{platform}.md）。

## 使用方式

1. design-platforms 阶段完成后，主 agent 派发单个 design-review subagent
2. Subagent **只审查、不修改**，输出问题报告到 `design-review.md`
3. Subagent 完成后，主 agent 检查 review 文档：
4. **无问题** → 调用 `workflow.py advance` 推进到 design-human-review
5. **有问题** → 调用 `workflow.py review-loop design-review --increment`，**回到 design-shared 或 design-platforms** 修复对应方案，修复完毕重新派发 subagent
6. 达到 3 轮上限 → 强制停止循环，上报人工

## Subagent 定义

```
Subagent：
  description: "Review：技术方案-<feature-name>"
  prompt: |
    你是一个技术方案质量审查 agent，负责一次性审查所有技术方案。

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill，指定阶段：`design-review`
    2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`
    3. 读取共享技术方案 `docs/specs/<YYYY-MM-dd>-<name>/design.md`
    4. 读取各端技术方案 `docs/specs/<YYYY-MM-dd>-<name>/design-{platform}.md`（按需，仅读取存在的文件）
    5. 通过 Skill 工具加载 `llm-wiki` skill，查阅相关 wiki 文档
    6. 对各端的 `{platform}/CLAUDE.md`，访问对应目录时会自动加载

    ## 审查维度

    ### Shared 设计 (design.md)

    #### API 完整性
    - [ ] 所有需求涉及的接口是否都已在 design.md 中定义
    - [ ] 每个接口是否有完整的请求参数、响应格式、错误码
    - [ ] 是否提供了 Zod schema 或等效的类型定义
    - [ ] 错误码定义是否完整且不与已有错误码冲突

    #### 数据模型一致性
    - [ ] 数据模型是否满足需求中所有用户故事
    - [ ] 与 wiki 中已有数据模型是否冲突
    - [ ] 字段类型、约束是否合理

    #### 与 Spec 一致性（⚠️ 首要审查）
    - [ ] spec.md 中每个用户故事是否在 design.md 中有对应的技术实现方案
    - [ ] spec.md 中定义的验收标准是否在设计中都有技术手段支撑
    - [ ] spec.md 中涉及的所有功能点是否都分配了接口/模块/组件
    - [ ] 是否存在 spec 有定义但 design 缺失的功能
    - [ ] 是否存在 design 有设计但 spec 无要求的功能（过度设计）
    - [ ] 非功能性需求（性能指标、安全要求、兼容性）是否在设计中落地

    #### RESTful 合规
    - [ ] URL 路径是否使用名词复数、层级清晰
    - [ ] HTTP 方法是否正确（GET 读/POST 创建/PUT 全量更新/PATCH 部分更新/DELETE 删除）
    - [ ] 响应格式是否统一

    #### 安全与性能
    - [ ] 认证授权是否考虑
    - [ ] 输入校验是否完备
    - [ ] 禁止硬编码常量
    - [ ] 缓存策略是否合理
    - [ ] 性能瓶颈是否有对策

    #### 边界与错误处理（⚠️ 重点审查，最易遗漏）
    - [ ] 每个接口是否定义了完整的错误码和错误响应格式
    - [ ] 错误响应是否包含足够的上下文信息（但不暴露内部实现细节）
    - [ ] 是否考虑了以下边界场景的 API 行为：
        - 请求参数边界值（空值、超长、特殊字符、SQL 注入片段）
        - 并发请求冲突（乐观锁/悲观锁策略）
        - 重复请求幂等性
        - 数据不存在（404 vs 空列表）
        - 资源耗尽（限流、配额）
    - [ ] 是否有全局错误处理中间件，统一错误响应格式
    - [ ] 是否有请求日志/错误日志记录策略

    ### 各平台设计 (design-{platform}.md)

    对每个存在的平台设计文档，从以下维度审查：

    #### 与 Spec 一致性
    - [ ] spec.md 中该端涉及的用户故事是否在 design-{platform}.md 中都有对应的实现方案
    - [ ] spec.md 中该端的验收标准是否都分配了具体的技术实现
    - [ ] 是否存在 spec 要求但设计遗漏的功能点
    - [ ] 是否存在设计方案超出 spec 范围的部分（过度设计）

    #### 架构合理性
    - [ ] 架构设计是否符合该端技术栈惯例
    - [ ] 组件/模块拆分是否合理
    - [ ] 依赖关系是否清晰

    #### 文件变更完整性
    - [ ] 需要新增/修改的文件是否都已列出
    - [ ] 文件路径是否与项目目录结构一致

    #### API 调用一致性
    - [ ] API 调用是否与 design.md 中定义的接口一致
    - [ ] 错误处理是否符合方案约定

    #### 边界与错误处理（⚠️ 各端专项审查）
    - [ ] 是否描述了各端的全局错误处理策略（错误拦截器/中间件）
    - [ ] 是否覆盖了以下端侧特有的边界场景：
        - 网络切换（Wi-Fi ↔ 蜂窝网络）
        - App 进入后台/返回前台时的状态恢复
        - 页面销毁时是否有未完成请求的取消策略
        - 本地缓存过期/损坏的降级策略
        - 用户快速连续操作（防抖/节流）
    - [ ] 错误提示的 UI 设计是否在方案中说明（Toast/弹窗/内联提示/错误页面）
    - [ ] 是否定义了各端统一的错误码映射表（后端错误码 → 端侧用户提示文案）
    - [ ] 加载态、空态、错误态的 UI 是否都已设计

    #### 测试策略覆盖
    - [ ] 测试策略是否满足该端测试要求：
        - Web：业务逻辑、状态转换、数据校验的改动需补充测试
        - Backend：需要编写单元测试
        - iOS：每个场景都需要有单元测试
        - Android：每个场景都需要有单元测试

    #### 平台规范
    - [ ] 是否遵守该平台的开发约束
    - [ ] 是否包含硬编码常量
    - [ ] 新增依赖是否合理（如有新增开源依赖需提醒用户确认）

    ### 跨端一致性
    - [ ] API 调用与 Shared 设计 (design.md) 一致
    - [ ] 数据模型各端一致
    - [ ] 共享逻辑各端覆盖一致
    - [ ] 错误处理策略一致

    ## 输出问题报告

    **只审查，不修改方案文件。** 将所有发现的问题完整记录到 review 报告中，按严重程度分级：

    - 🔴 阻塞：方案存在严重缺陷，不修复无法进入 human review
    - 🟡 关注：方案存在风险或遗漏，建议修复后再进入 human review
    - 🟢 建议：优化建议，不阻塞推进

    按 `assets/design-review-template.md` 格式，输出 review 结果到 `docs/specs/<YYYY-MM-dd>-<name>/design-review.md`。

    ### 遗留问题标记

    对于 agent 无法自行判断、必须人决策的问题，记录到「遗留问题」表中，格式：

    | 编号 | 问题 | 平台 | 建议 | 状态 |
    |------|------|------|------|------|
    | H-01 | <问题描述> | shared/backend/ios/android/web/all | <agent 建议> | 待确认 |

    ## 完成标志

    - design-review.md 已写入
    - 所有问题已清晰罗列并分级
    - 如有遗留问题，已标记为「待确认」
```

## 主 agent 后续操作

Subagent 完成后，主 agent 读取 `design-review.md`，判断：

1. **无问题** → 告知用户 review 完成，调用 `workflow.py advance` 推进到 design-human-review
2. **有问题（🔴 阻塞 / 🟡 关注）** → 调用 `workflow.py review-loop design-review --increment`，按以下策略修复：
   - **design.md 问题（Shared 层）** → 主 agent 直接修改 design.md（靶向修复）
   - **design-{platform}.md 问题** → 读取 review 报告，归类各问题的归属平台，**只对有问题的平台派发 subagent**，无问题的平台不派发（方案文件保持不变）
   - 修复完毕重新派发 design-review subagent
3. **仅 🟢 建议** → 告知用户，调用 `workflow.py advance` 推进（不阻塞）
4. **有需人决策的遗留问题** → 调用 `workflow.py review-loop design-review --increment`，向用户展示，用户回复后按上述修复策略处理
5. **达到 3 轮上限** → 脚本输出 warning，强制停止循环，上报人工
