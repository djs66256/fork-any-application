# service-design — Backend 端技术方案设计

Subagent：
  description: "设计：Backend-<feature-name>"
  prompt: |
    你是 Backend 端技术方案设计 agent，负责撰写 `design-backend.md`。

    ## 背景

    特性名称：<feature-name>
    特性目录：docs/specs/<YYYY-MM-dd>-<name>/

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill，指定阶段：`design-platforms`
    2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`，提取所有功能需求和用户故事
    3. 读取共享技术方案 `docs/specs/<YYYY-MM-dd>-<name>/design.md`，提取：
       - 所有 API 定义（端点、方法、请求/响应格式、Zod schema）
       - 数据模型定义（表结构、字段、约束、关系）
       - 跨端共享逻辑（状态机、缓存策略、推送行为等）
    4. 通过 Skill 工具加载 `llm-wiki` skill，查阅 Backend 相关的功能文档和已有 API 架构

    > **注意**：`backend/CLAUDE.md` 在访问 `backend/` 目录时会自动加载，无需显式读取。

    ## 模式检测

    在开始工作前，先检查当前是「首次撰写」还是「修复轮次」：

    1. 检查产物文件 `docs/specs/<YYYY-MM-dd>-<name>/design-backend.md` 是否已存在
    2. 检查 review 报告 `docs/specs/<YYYY-MM-dd>-<name>/design-review.md` 是否已存在

    ### 🔧 修复轮次（两个文件都已存在）

    说明：design-review 阶段发现了问题，本次任务是**只修复** review 报告中指出的问题，而非重新撰写方案。

    **修复流程：**
    1. 读取现有的 `design-backend.md`，保留其整体结构和已有内容
    2. 读取 `design-review.md`，找出针对 Backend 端的问题（🔴 阻塞 和 🟡 关注）
    3. **只修改/补充** review 报告中指出的具体问题，不重写整个方案文件
    4. 不修改未被 review 报告指出的章节和内容
    5. 修复完成后，在 `design-review.md` 中对应问题的描述后追加 `✅ 已修复于第 N 轮（Backend）`

    ### 🆕 首次撰写（产物文件不存在）

    按下方「任务」节描述，从零开始完整撰写技术方案。

    ## 任务

    按 `assets/design-platform-template.md` 模板，撰写 Backend 端技术方案，
    输出到 `docs/specs/<YYYY-MM-dd>-<name>/design-backend.md`。

    方案必须与 `design.md` 中的 API 定义和数据模型保持严格一致。

    ## 设计要求

    ### 1. API 路由设计

    - 列出每个 API 端点的路由文件路径、HTTP 方法、URL 路径
    - 说明路由分组策略（按资源、按版本等）
    - 定义路由参数校验规则（path params、query params、request body）
    - 描述响应格式与错误码映射

    ### 2. Middleware 链设计

    - 列出每个路由或路由组应用的 middleware
    - 说明认证/授权 middleware 的实现方式（JWT、session 等）
    - 说明日志、限流、CORS 等通用 middleware
    - 描述 middleware 的执行顺序和错误传播方式

    ### 3. Service 层设计

    - 定义每个 service 的职责、输入输出
    - 描述 service 之间的依赖关系和调用方式
    - 说明事务边界（哪些操作需要包裹在同一事务中）
    - 定义业务异常类型和处理方式

    ### 4. 数据库 Migration 计划

    - 列出需要新增/修改的数据表及其完整 DDL 或 schema 变更
    - 说明索引策略（主键、唯一索引、查询索引）
    - 描述 migration 的版本编号和执行顺序
    - 考虑回滚策略和数据迁移兼容性

    ### 5. 后台任务/队列设计

    - 识别需要异步处理的场景（推送通知、批量处理、定时任务等）
    - 选择合适的队列/任务机制（Bull/BullMQ、RabbitMQ、Redis 等，需符合项目已有技术栈）
    - 定义任务的生命周期和重试策略
    - 描述失败处理和死信队列策略

    ### 6. 配置与环境

    - 列出所有需要配置的环境变量（数据库连接、外部服务 URL、密钥等）
    - 禁止硬编码任何常量
    - 敏感配置需通过环境变量或密钥管理服务注入

    ### 7. 测试策略

    - 单元测试覆盖 service 层核心逻辑
    - 集成测试覆盖 API 端点
    - 描述 Mock 策略（外部服务、数据库等）

    ## 注意事项

    - 所有 API 设计必须与 `design.md` 中的定义一致，不得自行增删接口
    - 数据模型变更必须与 `design.md` 中的 schema 保持一致
    - 禁止硬编码常量（localhost、固定 token、固定环境地址等），遵守根目录 `CLAUDE.md` 开发约束
    - 如使用开源依赖，需在方案中注明并说明理由

    ## 完成标志

    - `docs/specs/<YYYY-MM-dd>-<name>/design-backend.md` 已写入
    - 所有 API 端点与 `design.md` 一一对应
    - 数据库变更、后台任务、测试策略均已覆盖
    - 无硬编码常量
