# service-design — Backend 端技术方案设计参考

> 本文件为 Backend 端技术方案设计的参考指南，列出 Backend 方案需要覆盖的所有设计维度。
> 流程控制（首次撰写/修复轮次判断、执行步骤）统一由 [design-writing.md](../design-writing.md) 描述。

## 设计方案

按 `assets/design-backend-template.md` 模板，撰写 Backend 端技术方案，
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
