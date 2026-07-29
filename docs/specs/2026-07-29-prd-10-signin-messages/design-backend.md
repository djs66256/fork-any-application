# Backend 端技术方案：PRD-10 签到与消息系统

> 创建日期：2026-07-29
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
请求
  → Route Handler (`app/api/check-ins/*`, `app/api/messages/*`)
  → Middleware（withErrorHandler + 可选/强制 auth helper）
  → Service（CheckInService / MessageService）
  → Repository（CheckIn / SystemMessage / InteractionMessage）
  → Supabase 数据源（签到 / 系统消息）或 Mock seeded fixture（互动消息）
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `src/middleware/auth.ts` | 复用 | 签到接口复用 `resolveOptionalAuthContext()`；互动消息接口复用 `requireAuthContext()` |
| `src/lib/schemas.ts` | 扩展 | 新增签到 / 消息 schema，并复用既有 `PaginationSchema` |
| `src/repositories/repository-registry.ts` | 扩展 | 注册签到仓储与系统消息仓储的 `mock / supabase` 实现；互动消息固定绑定 mock repository |
| `src/services/comment/comment.service.ts` | 参考 | 沿用 Service 层 parse repository 结果、业务错误透传的模式 |
| `src/app/api/player/parse-playback-session-id.ts` | 参考 | 按同样模式新增 `parse-installation-id.ts`，统一解析 `X-Installation-Id` |
| `supabase/migrations/` | 新增 | 新建签到 / 消息相关 migration，不修改旧 migration |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增 `InstallationIdHeaderSchema`、签到 / 消息实体与列表 schema |
| `backend/src/lib/config.ts` | 修改 | 为签到与系统消息增加 `mock / supabase` repository 选择配置；互动消息首版固定走 mock seeded fixture |
| `backend/src/app/api/check-ins/status/route.ts` | 新增 | 提供签到状态查询接口 |
| `backend/src/app/api/check-ins/route.ts` | 新增 | 提供签到提交接口 |
| `backend/src/app/api/messages/preview/route.ts` | 新增 | 提供菜单消息预览接口 |
| `backend/src/app/api/messages/system/route.ts` | 新增 | 提供系统消息分页接口 |
| `backend/src/app/api/messages/interactions/route.ts` | 新增 | 提供互动消息分页接口 |
| `backend/src/app/api/check-ins/parse-installation-id.ts` | 新增 | 统一解析 `X-Installation-Id` |
| `backend/src/repositories/interfaces/check-in.repository.interface.ts` | 新增 | 定义签到仓储 contract |
| `backend/src/repositories/interfaces/system-message.repository.interface.ts` | 新增 | 定义系统消息仓储 contract |
| `backend/src/repositories/interfaces/interaction-message.repository.interface.ts` | 新增 | 定义互动消息仓储 contract |
| `backend/src/repositories/mock/check-in.mock.repository.ts` | 新增 | 提供匿名 / 登录签到的本地 mock 实现 |
| `backend/src/repositories/mock/system-message.mock.repository.ts` | 新增 | 提供系统消息 seeded fixture |
| `backend/src/repositories/mock/interaction-message.mock.repository.ts` | 新增 | 提供登录态互动消息 seeded fixture；首版固定走 mock 数据源 |
| `backend/src/repositories/supabase/check-in.supabase.repository.ts` | 新增 | 基于 Supabase 表实现签到查询与写入 |
| `backend/src/repositories/supabase/system-message.supabase.repository.ts` | 新增 | 基于 Supabase 表实现系统消息查询 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 增加新仓储 getter / setter / reset 逻辑 |
| `backend/src/services/check-in/check-in.service.ts` | 新增 | 封装签到状态计算、弹窗资格与幂等签到逻辑 |
| `backend/src/services/message/message.service.ts` | 新增 | 封装预览、系统消息、互动消息列表逻辑 |
| `backend/supabase/migrations/<timestamp>_create_signin_and_message_tables.sql` | 新增 | 建表与索引 |
| `backend/src/app/api/__tests__/check-ins*.test.ts` | 新增 | 覆盖签到接口行为 |
| `backend/src/app/api/__tests__/messages*.test.ts` | 新增 | 覆盖消息接口行为 |
| `backend/src/services/check-in/check-in.service.test.ts` | 新增 | 覆盖业务日与幂等逻辑 |
| `backend/src/services/message/message.service.test.ts` | 新增 | 覆盖 preview / list / auth gating 逻辑 |

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `app/api/check-ins/status/route.ts` | GET | `/api/check-ins/status` | `withErrorHandler` + 可选 auth | 返回单个 `SignInStatus` |
| `app/api/check-ins/route.ts` | POST | `/api/check-ins` | `withErrorHandler` + 可选 auth | 按账号或安装标识提交签到 |
| `app/api/messages/preview/route.ts` | GET | `/api/messages/preview` | `withErrorHandler` | 返回菜单消息预览 |
| `app/api/messages/system/route.ts` | GET | `/api/messages/system` | `withErrorHandler` | 返回系统消息分页列表 |
| `app/api/messages/interactions/route.ts` | GET | `/api/messages/interactions` | `withErrorHandler` + `requireAuthContext` | 返回登录用户的互动消息分页列表 |

### 3.2 路由分组策略

- 继续按资源分组，不新增版本前缀。
- `check-ins` 聚合签到状态与写入；`messages` 聚合预览、系统消息与互动消息。
- 消息 preview 单独成资源，而非复用列表 route 的参数特化，避免客户端多种 contract。

### 3.3 参数校验

```typescript
const MessageListQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(20).default(20),
});

const InstallationIdHeaderSchema = z.string().uuid();
```

- `messages/system` 与 `messages/interactions` 共用分页 query schema。
- 匿名签到请求统一通过 route helper 校验 `X-Installation-Id`。
- 登录态签到请求允许 header 同时存在，但 service 层忽略其记账优先级。

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
请求
  → withErrorHandler
  → （可选）resolveOptionalAuthContext / requireAuthContext
  → query/header schema parse
  → Route Handler
  → CheckInService / MessageService
  → JSON Response
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 全部新接口 | 统一输出 `{ error: { code, message } }` |
| `resolveOptionalAuthContext()` | `/api/check-ins/status`, `/api/check-ins` | 支持账号态优先、匿名兜底 |
| `requireAuthContext()` | `/api/messages/interactions` | 互动消息必须登录 |
| `getAuth(request)` | `/api/messages/interactions` | 从 request 中读取 `userId` |

### 4.3 错误传播方式

- route / helper 抛出的 `AppError` 直接由 `withErrorHandler` 捕获并格式化。
- service 层只抛业务语义错误，不直接构造 HTTP 响应。
- repository 层遇到 Supabase 不可用、fixture 不可用或返回结构非法时，统一抛 `Errors.serviceUnavailable(...)`，不在 PRD-10 首版对外新增 `INTERNAL_ERROR` contract。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `CheckInService` | 解析签到主体、计算签到板状态、执行同日幂等提交 | `userId?`, `installationId?` | `SignInStatus` | `CheckInRepository` |
| `MessageService` | 获取 preview、系统消息、互动消息列表 | `page/pageSize`, `userId?` | `MessagePreview | null` / list response | `SystemMessageRepository`, `InteractionMessageRepository` |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| `POST /api/check-ins` 查询当前状态 + 写入当日签到记录 | 单事务 / Upsert | 写入失败则整体失败，不返回部分成功状态 |
| 系统消息 / 互动消息列表读取 | 无显式事务 | 纯读请求，直接返回当前快照 |

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| 参数非法 | 匿名态缺少 / 非法安装标识，或分页参数越界 | 400 | `VALIDATION_ERROR` |
| 未授权 | 互动消息接口未登录 | 401 | `AUTH_UNAUTHORIZED` |
| 服务不可用 | Supabase、mock fixture 或 repository 不可用，或 repository 返回结构与 schema 不一致 | 503 | `SERVICE_UNAVAILABLE` |

### 5.4 签到主体设计

`CheckInService` 不把主体判断散落在 route 层，而是统一封装为：

```typescript
type CheckInSubject =
  | { type: 'user'; id: string }
  | { type: 'installation'; id: string };
```

解析规则：
1. 有 `userId` 时，固定返回 `type: 'user'`；
2. 无 `userId` 时，要求 `installationId` 合法并返回 `type: 'installation'`；
3. 两者都缺失时抛 `Errors.validationError(...)`，与现有 `withErrorHandler` 对 Zod 校验失败输出的 `VALIDATION_ERROR` 保持一致。

### 5.5 签到状态计算

`CheckInService` 返回 `SignInStatus` 时统一负责：
- 根据当前 `server_date` 计算当前轮次的第几天；
- 产出固定 7 个 `days` 项；
- 根据服务端可见条件（如今日是否已签到、当前轮次进度）计算 `should_show_popup` 的服务端展示资格；
- 第 8 天开启新一轮 7 日板。

其中冷启动、首页落点、当日本地 dismissed 状态以及评论/登录模态冲突均不进入服务端判断，仍由移动端按 `server_date` 和本地 UI 状态做最终弹层决策；backend 仅负责返回权威 `server_date`、`today_signed` 与服务端资格。

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| `check_in_records` | 新建 | 记录用户 / 安装在某个业务日的签到结果；直接以内联主体字段承载，不再拆 `check_in_subjects` |
| `system_messages` | 新建 | 提供菜单 preview 与系统消息列表 |
| `interaction_messages` | 本期不建表 | 首版继续由 mock repository / seeded fixture 提供登录态互动消息，等真实事件源接入后再补 Supabase 表 |

### 6.2 DDL

```sql
create table if not exists public.check_in_records (
  id uuid primary key default gen_random_uuid(),
  subject_type text not null check (subject_type in ('user', 'installation')),
  subject_id text not null,
  business_date date not null,
  streak_day integer not null check (streak_day between 1 and 7),
  created_at timestamptz not null default now()
);

create unique index if not exists idx_check_in_records_subject_date
  on public.check_in_records(subject_type, subject_id, business_date);

create index if not exists idx_check_in_records_subject_created_at
  on public.check_in_records(subject_type, subject_id, created_at desc);

create table if not exists public.system_messages (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  summary text not null,
  sent_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index if not exists idx_system_messages_sent_at
  on public.system_messages(sent_at desc);
```

### 6.3 字段详情

| 表名 | 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `check_in_records` | `subject_type` | text | `user` / `installation` | — | 签到主体类型 |
| `check_in_records` | `subject_id` | text | not null | — | 用户 ID 或安装 UUID |
| `check_in_records` | `business_date` | date | not null | — | 服务端业务日 |
| `check_in_records` | `streak_day` | integer | 1~7 | — | 当前轮次中的第几天 |
| `system_messages` | `title` | text | not null | — | 消息标题 |
| `system_messages` | `summary` | text | not null | — | 摘要文案 |
| `system_messages` | `sent_at` | timestamptz | not null | `now()` | 展示时间 |

### 6.4 索引策略

| 表名 | 索引名 | 类型（UNIQUE/INDEX） | 字段 | 用途 |
|------|--------|---------------------|------|------|
| `check_in_records` | `idx_check_in_records_subject_date` | UNIQUE | `subject_type, subject_id, business_date` | 同日幂等签到 |
| `check_in_records` | `idx_check_in_records_subject_created_at` | INDEX | `subject_type, subject_id, created_at desc` | 查询最近签到历史 |
| `system_messages` | `idx_system_messages_sent_at` | INDEX | `sent_at desc` | preview 与列表排序 |
| — | — | — | — | 首版互动消息仍走 mock / seeded fixture，本期不建表也不新增索引 |

### 6.5 回滚策略

- migration 仅做新增表与新增索引，不修改既有表结构；
- 如需回滚，删除新表和索引即可；
- 本轮 migration 只创建 `check_in_records` 与 `system_messages`；`interaction_messages` 明确留到后续切换 Supabase repository 时再补 migration。

---

## 7. 后台任务/队列设计

### 7.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| 无 | 首版无异步任务 | — | — | — | — |

### 7.2 任务生命周期

首版签到与消息均走同步读写，不引入后台任务、消息总线或 dead letter queue。

### 7.3 失败处理与死信队列

- 暂不需要；
- 若未来接入评论 / 点赞事件生成互动消息，再扩展异步投递与重试机制。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| 签到仓储实现 | `CHECK_INS_REPOSITORY` | `mock` / `supabase` | `supabase` | 控制签到 repository |
| 系统消息仓储实现 | `SYSTEM_MESSAGES_REPOSITORY` | `mock` / `supabase` | `supabase` | 控制系统消息 repository |
| 互动消息仓储实现 | `INTERACTION_MESSAGES_REPOSITORY` | `mock` | `mock` | 首版固定走 mock / seeded fixture |
| 服务端时区 / 业务日策略 | 复用服务端统一时区配置，不为 PRD-10 单独新增 `BUSINESS_TIMEZONE` | 跟随后端现有环境配置 | 跟随后端现有环境配置 | `server_date` 计算接入既有统一配置能力，避免本需求额外引入新环境变量 |
| 数据库连接 | `SUPABASE_URL` 等 | 本地 Supabase | 线上 Supabase | 复用既有配置 |

> ⚠️ 禁止硬编码任何常量。所有配置通过环境变量注入。

---

## 9. API 调用清单（调用外部服务）

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| Supabase Postgres | `check_in_records`, `system_messages` | 查询 / 提交签到，读取系统消息 | 复用现有 DB client 默认配置 | 仓储失败返回 `SERVICE_UNAVAILABLE` |
| Mock / seeded fixture | `interaction_messages` | 读取登录态互动消息 | 进程内同步读取 | fixture 解析失败或仓储不可用统一返回 `SERVICE_UNAVAILABLE` |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| 服务端业务日权威 | 以 `server_date` 为准 | `CheckInService` 注入统一的 `businessDateProvider`，所有签到计算都从这里取值 |
| 账号态优先 | 登录态覆盖安装态 | 在 service 层统一解析 `CheckInSubject`，有 `userId` 时忽略 installationId |
| 分页 contract 统一 | `page/pageSize` + `pagination` | 所有消息列表 schema 复用 `PaginationSchema` |
| 互动消息固定 mock | 首版不依赖评论事件链路 | `repository-registry` 将 `interaction_messages` 固定绑定到 mock repository / seeded fixture |
| preview 与系统消息同源 | 菜单只展示系统消息最新一条 | `MessageService.getPreview()` 直接复用 system repository 的 top-1 查询 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | `withErrorHandler` | 统一包裹所有新接口 |
| Service | `Errors.validationError / authUnauthorized / serviceUnavailable` | 只抛业务语义错误 |
| Repository | Zod parse + Supabase / fixture 错误映射 | 返回非法结构或 fixture 异常时统一抛 `Errors.serviceUnavailable(...)` |
| 日志 | `console.error/warn` + 既有日志体系 | 记录 query、subject 类型、error code，不记录敏感 token |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `VALIDATION_ERROR` | 400 | 参数校验失败 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `AUTH_UNAUTHORIZED` | 401 | 未登录 | `{ "error": { "code": "AUTH_UNAUTHORIZED", "message": "请先登录" } }` |
| `NOT_FOUND` | 404 | 保留给未来消息详情等扩展 | — |
| `CONFLICT` | 409 | 首版不单独暴露签到冲突 | — |
| `TOO_MANY_REQUESTS` | 429 | 预留防刷扩展 | — |
| `SERVICE_UNAVAILABLE` | 503 | 上游仓储不可用、fixture 异常或返回结构非法 | `{ "error": { "code": "SERVICE_UNAVAILABLE", "message": "Supabase unavailable" } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 匿名签到缺失 header | 无登录态且没带 `X-Installation-Id` | 400 `VALIDATION_ERROR` | route helper 使用 Zod 校验后直接拦截 |
| header 非 UUID | 伪造 installationId | 400 `VALIDATION_ERROR` | 对齐播放器 header parse 方式 |
| 同日重复签到 | 重复点击 / 网络重试 | 200 + 最新 `SignInStatus` | 通过唯一索引或仓储幂等实现 |
| preview 没有数据 | `system_messages` 为空 | 204 No Content | preview 空态 contract 在 route 层唯一化 |
| 互动消息未登录 | 匿名访问 `/api/messages/interactions` | 401 `AUTH_UNAUTHORIZED` | 由 `requireAuthContext()` 处理 |
| mock repository 返回脏数据 | fixture 结构错误 | 503 `SERVICE_UNAVAILABLE` | 首版统一折叠为仓储不可用，避免对外新增 `INTERNAL_ERROR` contract |
| Supabase 暂时不可用 | 连接失败 / 表不存在 | 503 `SERVICE_UNAVAILABLE` | 不把仓储异常暴露为 500 |

### 11.4 错误日志与监控

- `CheckInService`：记录主体类型、业务日、是否幂等命中；
- `MessageService`：记录 `preview/system/interactions` 三种查询的失败来源；
- 不记录 access token 或完整 installationId，可按截断形式输出调试信息。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| 单元测试 | `CheckInService` 业务日、轮次、幂等与主体选择逻辑 | Vitest |
| 单元测试 | `MessageService` preview / list / unauthorized 行为 | Vitest |
| 集成测试 | 5 个 Route 的 query/header/auth contract | Next route tests + Vitest |
| Repository 测试 | mock repository 的 fixture 与 supabase repository 的 parse | Vitest |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| B-01 | 匿名查询签到状态 | 无 token + 合法 installationId | 200，返回单个 `SignInStatus` | Route |
| B-02 | 匿名缺少 installationId | 无 token + 无 header | 400 `VALIDATION_ERROR` | Route |
| B-03 | 登录态优先签到 | token + installationId | 只按 `userId` 读写签到 | Service |
| B-04 | 同日重复签到 | 同一主体同一 `server_date` 重复 POST | 200，`today_signed=true`，不重复累计 | Service |
| B-05 | 第 8 天重开新一轮 | 已完成 7 天后进入下一业务日 | `days` 从第 1 天重新开始 | Service |
| B-06 | 获取消息 preview 空态 | 无系统消息 | 返回空态，不抛错 | Service / Route |
| B-07 | 匿名获取系统消息 | `page=1&pageSize=20` | 200 + `{ data, pagination }` | Route |
| B-08 | 匿名获取互动消息 | 无 token | 401 `AUTH_UNAUTHORIZED` | Route |
| B-09 | 已登录获取互动消息 | 合法 token | 200 + `{ data, pagination }` | Route |
| B-10 | repository 不可用 | Supabase query 报错 | 503 `SERVICE_UNAVAILABLE` | Repository / Route |
