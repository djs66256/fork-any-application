# Backend 端技术方案：PRD-08 用户登录与注册

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Backend 端继续沿用当前仓库的四层结构：**Route → Service → Repository → Infrastructure / Shared**。认证能力以 `AuthService` 为核心，统一承接 OTP 发送、验证码校验、会话换发、当前用户查询与退出登录；Route 只负责 request parsing、middleware 组合与 `{ code, data, message }` 成功响应；认证失败与参数校验失败继续通过统一错误机制输出业务错误码。

```text
POST /api/auth/otp-requests
POST /api/auth/sessions
POST /api/auth/session-refreshes
GET  /api/users/me
DELETE /api/auth/session

请求
  -> Route Handler
  -> withErrorHandler
  -> Zod schema parse
  -> AuthService
      -> getSupabaseAdmin()
      -> Supabase Auth (sendOtp / verifyOtp / refreshSession / getUser / signOut)
      -> AuthProfileRepository / UserSupabaseRepository
      -> AuthSession / AuthUser mapper
  -> NextResponse.json({ code: 0, data, message: 'ok' })

受保护业务接口
  -> requireAuthContext / verifyJwt
  -> request.auth = { userId, role }
  -> Route / Service 消费真实 AuthContext
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/admin/auth/login/route.ts` | 不变 | 继续服务后台管理端邮箱密码登录，不与移动端认证 contract 混用 |
| `backend/src/app/api/admin/auth/logout/route.ts` | 不变 | 继续服务后台管理端退出，不复用到移动端用户会话 |
| `backend/src/app/api/auth/*` | 新增 | 新增移动端认证资源域路由 |
| `backend/src/app/api/users/me/route.ts` | 新增 | 提供当前用户摘要接口 |
| `backend/src/middleware/auth.ts` | 修改 | 下线移动端骨架态 userId 读取语义，统一验证真实 Bearer JWT |
| `backend/src/middleware/error-handler.ts` | 不变 | 继续统一处理 AppError / ZodError |
| `backend/src/services/*` | 新增/扩展 | 新增 `AuthService`，受保护业务服务继续消费 `AuthContext` |
| `backend/src/repositories/supabase/user.supabase.repository.ts` | 扩展 | 增加按 auth user id 获取当前用户摘要、必要时补 phone 摘要映射 |
| `backend/src/infrastructure/supabase.ts` | 不变 | 继续通过 `getSupabaseAdmin()` 访问 Auth 与 profile 资源 |
| `backend/src/lib/schemas.ts` | 修改 | 补齐 auth request / response / current user schema |
| `backend/src/lib/errors.ts` | 修改 | 新增 `AUTH_INVALID_PHONE`、`AUTH_INVALID_CODE`、`AUTH_UNAUTHORIZED`、`AUTH_REFRESH_EXPIRED`、`AUTH_CODE_COOLDOWN`、`AUTH_CODE_EXPIRED`、`AUTH_RATE_LIMITED` 等认证业务码，并保持与现有 AppError 机制一致 |
| `backend/src/app/api/dramas/rankings/route.ts` | 修改 | 不再读取 `x-user-id` 作为正式移动端 userId 来源 |

### 1.2 设计原则

1. **移动端认证接口与后台管理端认证隔离**：用户登录采用手机号 OTP，后台仍保留邮箱密码登录，避免 contract 串用。
2. **Backend 是唯一会话编排方**：移动端不直接接入 Supabase Auth SDK，避免双端和服务端出现三套认证语义。
3. **成功响应统一包裹**：本期认证接口成功响应全部使用 `{ code, data, message }`，与 spec 保持一致。
4. **失败响应保持统一错误语义**：认证相关错误继续走 `AppError` / `withErrorHandler`，不在 Route 内自行散落格式。
5. **不引入新依赖**：继续使用现有 Next.js、Supabase、Zod 能力完成认证闭环；若无必须，不新增 Redis/第三方短信 SDK 集成。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/auth/otp-requests/route.ts` | 新增 | 发送验证码接口 |
| `backend/src/app/api/auth/sessions/route.ts` | 新增 | 验证码创建/恢复会话接口 |
| `backend/src/app/api/auth/session-refreshes/route.ts` | 新增 | refresh token 换发 session |
| `backend/src/app/api/auth/session/route.ts` | 新增 | `DELETE /api/auth/session` 退出登录 |
| `backend/src/app/api/users/me/route.ts` | 新增 | 获取当前用户摘要 |
| `backend/src/services/auth/auth.service.ts` | 新增 | 认证主服务，封装 Supabase Auth 交互与 session/user 映射 |
| `backend/src/services/auth/auth.service.test.ts` | 新增 | 覆盖 OTP、自动注册、refresh、logout、用户摘要聚合 |
| `backend/src/repositories/interfaces/auth-profile.repository.interface.ts` | 新增 | 约束用户摘要查询与 profile 补全读取接口 |
| `backend/src/repositories/supabase/auth-profile.supabase.repository.ts` | 新增 | 从 `profiles` + `auth.users` 组装 `AuthUser` |
| `backend/src/repositories/supabase/user.supabase.repository.ts` | 修改 | 视实现选择复用或抽取当前用户摘要能力 |
| `backend/src/middleware/auth.ts` | 修改 | 移除/弱化 `x-user-id` 占位语义，统一真实 JWT 校验与 `getAuth()` |
| `backend/src/app/api/dramas/rankings/route.ts` | 修改 | 匿名仍可访问；已登录态只能来自真实 auth context，而不是 header 占位 |
| `backend/src/lib/schemas.ts` | 修改 | 增加 auth request、`AuthUserSchema`、`AuthSessionSchema`、响应 envelope schema |
| `backend/src/lib/errors.ts` | 修改 | 新增并统一使用 `AUTH_INVALID_PHONE`、`AUTH_INVALID_CODE`、`AUTH_UNAUTHORIZED`、`AUTH_REFRESH_EXPIRED`、`AUTH_CODE_COOLDOWN`、`AUTH_CODE_EXPIRED`、`AUTH_RATE_LIMITED` 等认证业务码，不再回退到笼统 `UNAUTHORIZED` |
| `backend/src/app/api/__tests__/auth-otp-requests.test.ts` | 新增 | 覆盖手机号校验、频率限制、上游错误 |
| `backend/src/app/api/__tests__/auth-sessions.test.ts` | 新增 | 覆盖自动注册/已存在用户登录/验证码失败 |
| `backend/src/app/api/__tests__/auth-session-refreshes.test.ts` | 新增 | 覆盖 refresh 成功、refresh 失败、非法参数 |
| `backend/src/app/api/__tests__/auth-session.test.ts` | 新增 | 覆盖 logout 成功、无 token、幂等语义 |
| `backend/src/app/api/__tests__/users-me.test.ts` | 新增 | 覆盖当前用户摘要成功/无 token/profile 缺失 |
| `backend/src/middleware/__tests__/auth.test.ts` | 修改 | 覆盖新的 JWT 校验 contract 与 `request.auth` 注入 |

> 注：当前阶段只输出设计文档，不直接修改实现文件。

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/auth/otp-requests/route.ts` | `POST` | `/api/auth/otp-requests` | `withErrorHandler` + body Zod 校验 | 发送登录验证码 |
| `backend/src/app/api/auth/sessions/route.ts` | `POST` | `/api/auth/sessions` | `withErrorHandler` + body Zod 校验 | 验证码创建会话 |
| `backend/src/app/api/auth/session-refreshes/route.ts` | `POST` | `/api/auth/session-refreshes` | `withErrorHandler` + body Zod 校验 | refresh token 换发新会话 |
| `backend/src/app/api/auth/session/route.ts` | `DELETE` | `/api/auth/session` | `withErrorHandler` + 可选 access token 解析 | 当前客户端退出登录（始终按幂等成功语义返回） |
| `backend/src/app/api/users/me/route.ts` | `GET` | `/api/users/me` | `withErrorHandler` + `requireAuthContext` | 当前用户摘要 |
| `backend/src/app/api/dramas/rankings/route.ts` | `GET` | `/api/dramas/rankings` | `withErrorHandler` + 可选 auth context | 匿名可读，但已登录态 booking 状态来自真实 JWT |

### 3.2 路由分组策略

- 认证资源全部收敛到 `/api/auth/*`：
  - `otp-requests` 表示 OTP 请求资源；
  - `sessions` 表示用户登录 session 资源；
  - `session-refreshes` 表示会话刷新动作资源；
  - `session` 表示当前客户端持有的单一会话资源。
- 当前用户摘要作为用户资源的自我视图，使用 `GET /api/users/me`，而不是放到 `/api/auth/me`。
- 后续评论、消息、资产等需要登录的业务接口统一依赖这套 auth context，不再各自设计临时鉴权方式。

### 3.3 参数校验

```typescript
import { z } from 'zod';

export const PhoneSchema = z.string().trim().regex(/^1\d{10}$/);
export const CountryCodeSchema = z.literal('+86');
export const OtpCodeSchema = z.string().trim().regex(/^\d{6}$/);

export const OtpRequestSceneSchema = z.literal('login');

export const SendOtpRequestSchema = z.object({
  phone: PhoneSchema,
  countryCode: CountryCodeSchema,
  scene: OtpRequestSceneSchema,
});

export const CreateAuthSessionRequestSchema = z.object({
  phone: PhoneSchema,
  countryCode: CountryCodeSchema,
  code: OtpCodeSchema,
});

export const RefreshAuthSessionRequestSchema = z.object({
  refreshToken: z.string().trim().min(1),
});
```

参数约束结论：

| 参数 | 规则 | 说明 |
|------|------|------|
| `phone` | `^1\d{10}$` | 本期仅支持 +86 11 位手机号 |
| `countryCode` | `+86` | 首版不支持国际区号 |
| `scene` | `login` | 为后续业务复用预留，但本期只允许 login |
| `code` | 6 位数字 | 统一验证码输入格式 |
| `refreshToken` | 非空 string | 不强行假定 JWT 结构，由上游决定 |

### 3.4 成功响应 contract

所有认证接口成功响应统一使用：

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

可复用 helper：

```typescript
function success<T>(data: T) {
  return NextResponse.json({ code: 0, data, message: 'ok' });
}
```

### 3.5 `AuthUser` 组装规则

| 字段 | 来源 | 说明 |
|------|------|------|
| `id` | `auth.users.id` | 当前认证用户主键 |
| `phone` | `auth.users.phone` 或 profile 中的 phone 摘要字段 | 返回前统一脱敏 |
| `displayName` | `profiles.display_name` | 可为空 |
| `role` | `profiles.role` / `auth.users.app_metadata.role` | 默认 viewer |
| `isNewUser` | `create session` 过程上下文 | 仅登录成功响应需要真实值；`me` 默认 false |

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
认证公开接口
请求
  -> withErrorHandler
  -> body/json parse
  -> Zod 校验
  -> AuthService
  -> 成功 envelope

受保护接口
请求
  -> withErrorHandler
  -> requireAuthContext
      -> extractBearerToken
      -> verifyJwt via Supabase Admin
      -> inject request.auth
  -> Route Handler
  -> Service / Repository
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 全部 auth route + protected route | 统一收敛 AppError / ZodError |
| `requireAuthContext`（可基于现有 `requireAuth` / `requireRole` 重构） | `GET /api/users/me`、受保护业务接口 | 校验 Bearer JWT 并注入 `{ userId, role }` |
| `resolveOptionalAuthContext`（新增 helper） | `DELETE /api/auth/session`、匿名可读但可带登录态的业务接口 | 尝试解析 Bearer JWT；失败时返回空上下文，不提前拦截 logout 幂等语义 |
| `requireRole` | 后台管理接口 | 本期不变 |

### 4.3 `auth.ts` 重构方向

当前 `backend/src/middleware/auth.ts` 同时包含：
- `getOptionalUserId()`：允许从 `x-user-id` 读取 userId；
- `requireAuth()`：只校验是否带了 Bearer header；
- `verifyJwt()`：已经具备真实 Supabase JWT 校验能力。

本期收口策略：

1. **保留并强化 `extractBearerToken()` / `verifyJwt()`**。  
2. **新增 `requireAuthContext(handler)`**：未通过 JWT 校验直接返回 401，并统一注入 `request.auth`。  
3. **受保护业务接口改为消费 `getAuth(request)`**。  
4. **`getOptionalUserId()` 不再作为移动端正式认证 helper**；如果短期为了兼容骨架测试保留，也需明确标注 deprecated，只允许内部过渡使用。  

### 4.4 错误传播方式

- Route / Service 中遇到业务错误统一抛 `AppError`。
- 参数校验失败统一抛 `ZodError` 或主动抛 `Errors.invalidParams(...)`。
- `withErrorHandler` 负责：
  - `AppError -> HTTP status + error body`
  - `ZodError -> 400 + VALIDATION_ERROR + details`
  - 未知异常 -> 500 + INTERNAL_ERROR
- 认证 Route 不自行拼装失败 JSON，避免与全局错误格式分叉。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `AuthService.sendOtp()` | 发起手机号 OTP 请求 | `SendOtpRequest` | `{ requestId, cooldownSeconds, expiresInSeconds }` | `getSupabaseAdmin()` |
| `AuthService.createSession()` | 校验 OTP，自动注册/登录，返回 `AuthSession` | `CreateAuthSessionRequest` | `AuthSession` | `getSupabaseAdmin()` + `AuthProfileRepository` |
| `AuthService.refreshSession()` | 使用 refresh token 换发新会话 | `RefreshAuthSessionRequest` | `AuthSession` | `getSupabaseAdmin()` + `AuthProfileRepository` |
| `AuthService.getCurrentUser()` | 读取当前 auth user 的摘要 | `authUserId` | `AuthUser` | `AuthProfileRepository` |
| `AuthService.logout()` | 退出当前 access token 对应会话 | `accessToken` / `authUserId` | `void` | `getSupabaseAdmin()` |

### 5.2 关键流程

#### 发送验证码

```text
sendOtp
-> normalize +86 + phone => E.164 phone
-> supabase.auth.signInWithOtp({ phone }) 或等价 OTP API
-> 成功后返回 requestId/cooldown/expiresIn
-> 上游频率限制映射为 429
```

#### 验证码创建会话

```text
createSession
-> verifyOtp(phone, code)
-> 取得 supabase session + user
-> 识别是否首次用户（根据 profile 是否已存在 / auth metadata / create path 判断）
-> 读取/补齐 profiles 摘要
-> map => AuthSession
```

#### refresh 会话

```text
refreshSession
-> supabase.auth.refreshSession({ refresh_token })
-> session 成功 => 重新读取 profile 摘要
-> map => AuthSession
-> 失败 => AUTH_REFRESH_EXPIRED
```

#### 当前用户

```text
getCurrentUser
-> request.auth.userId
-> repository.findAuthUserById(userId)
-> 返回 AuthUser(isNewUser=false)
```

#### 退出登录

```text
logout
-> route 通过 resolveOptionalAuthContext / extractBearerToken 尝试解析当前 bearer
-> 若 token 可用：调用 signOut / revoke 当前 session
-> 若 token 缺失、已失效或上游反馈当前会话不存在：不再向上抛 401，统一按幂等退出处理
-> 始终返回 null data
```

### 5.3 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| OTP 发送 | 无显式数据库事务 | 依赖 Supabase Auth 原子行为 |
| 验证码登录 + profile 摘要组装 | 无业务库事务 | 以 Supabase Auth 成功为准；profile 不存在则尝试惰性创建或返回明确错误 |
| refresh + 用户摘要读取 | 无业务库事务 | refresh 失败直接报错；读取摘要失败不返回脏 session |
| logout | 无业务库事务 | 服务端失败不影响客户端本地清理策略 |

### 5.4 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| 参数校验失败 | `countryCode` / `scene` / 请求体结构不合法 | 400 | `VALIDATION_ERROR` |
| 手机号格式非法 | 手机号不满足 +86 11 位规则 | 400 | `AUTH_INVALID_PHONE` |
| OTP 格式非法或错误 | 验证码格式不合法或与服务端不匹配 | 400 | `AUTH_INVALID_CODE` |
| OTP 已过期 | 验证码超过有效期 | 410 | `AUTH_CODE_EXPIRED` |
| Access token 无效 | Bearer JWT 不合法、缺失或已过期 | 401 | `AUTH_UNAUTHORIZED` |
| Refresh token 无效 | refresh token 已失效、过期或被吊销 | 401 | `AUTH_REFRESH_EXPIRED` |
| 发送冷却中 | 同手机号仍在发送冷却窗口 | 409 | `AUTH_CODE_COOLDOWN` |
| 发送/校验过于频繁 | 上游限流 | 429 | `AUTH_RATE_LIMITED` |
| 用户摘要缺失 | auth user 存在但 profile 不存在且无法补齐 | 404 | `NOT_FOUND` |
| 上游服务不可用 | Supabase Auth 不可用 | 503 | `SERVICE_UNAVAILABLE` |
| 未知错误 | 其他未分类异常 | 500 | `INTERNAL_ERROR` |

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| `auth.users` | 不变 | 继续由 Supabase Auth 管理 |
| `profiles` | 可能修改 | 若现有 schema 无法提供手机号摘要，则补充 phone / masked_phone 等字段 |
| 业务自建 OTP 表 | 不新增 | 首版不自建验证码表 |
| 业务自建 session 表 | 不新增 | 首版不自建 session 表 |

### 6.2 已有 schema 基础

当前 migration 已确认：

- `profiles.id` 关联 `auth.users(id)`；
- `profiles.role` 已存在，默认 `viewer`；
- auth hook 已在创建 auth user 时插入 `profiles` 并同步 role 到 `auth.users.raw_app_meta_data`。

因此本期优先方案是**复用现有 profiles + auth.users**，不引入新的用户表。

### 6.3 候选 DDL（仅当需要手机号字段时）

```sql
-- Migration: add phone snapshot to profiles for mobile auth summary
ALTER TABLE profiles
ADD COLUMN IF NOT EXISTS phone TEXT;

CREATE INDEX IF NOT EXISTS idx_profiles_phone ON profiles(phone);
```

### 6.4 字段详情

| 表名 | 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `profiles` | `id` | UUID | PK, FK -> `auth.users(id)` | — | 用户主键 |
| `profiles` | `display_name` | TEXT | nullable | null | 展示名 |
| `profiles` | `role` | `user_role` | not null | `viewer` | 权限角色 |
| `profiles` | `phone`（可选） | TEXT | nullable | null | 如需落盘手机号摘要来源 |

### 6.5 回滚策略

- 若新增 `profiles.phone`，可通过独立 migration 回滚该列；
- 认证主链路本身尽量不依赖新的 schema 改造，优先从 `auth.users.phone` 读取，降低回滚成本。

---

## 7. 后台任务/队列设计

### 7.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| 无新增后台任务 | — | — | — | — | — |

### 7.2 说明

- 首版认证闭环不引入新的异步队列；OTP 发送、验证码校验、refresh、logout 都是同步请求-响应流程。
- 若后续需要加入登录审计、风控打点或短信补偿，可在后续 PRD 中增量引入 Redis 队列，但不作为本期前置依赖。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| Supabase URL | `SUPABASE_URL` | 本地 Supabase 地址 | 生产项目地址 | 必需 |
| Supabase anon key | `SUPABASE_ANON_KEY` | 本地默认值 | 生产 anon key | OTP / user client 能力依赖 |
| Supabase service role key | `SUPABASE_SERVICE_ROLE_KEY` | 本地默认值 | 生产 service role | JWT 验证、管理能力依赖 |
| App name | `APP_NAME` | 可选 | 可选 | 日志与响应文案辅助 |
| Redis URL | `REDIS_URL` | 可空 | 可空 | 本期 auth 主链路不强依赖 |

> ⚠️ 禁止硬编码任何常量。所有配置通过环境变量注入。

### 8.1 开发 / CI 依赖口径

- 开发与 CI 只要求：
  - 本地 Supabase 栈可用，或
  - 测试 OTP fixture / mock auth client 可用。
- 真实生产短信供应商配置不阻塞本期开发与自动化验证。

---

## 9. API 调用清单（调用外部服务）

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| Supabase Auth | OTP 发送接口 | `POST /api/auth/otp-requests` | 使用 SDK 默认/现有 HTTP 超时 | 返回 `SERVICE_UNAVAILABLE` |
| Supabase Auth | OTP 校验接口 | `POST /api/auth/sessions` | 同上 | 返回 `AUTH_INVALID_CODE` / `AUTH_CODE_EXPIRED` / `AUTH_RATE_LIMITED` / `SERVICE_UNAVAILABLE` |
| Supabase Auth | Session refresh | `POST /api/auth/session-refreshes` | 同上 | 返回 `AUTH_REFRESH_EXPIRED` |
| Supabase Auth | getUser(token) | `GET /api/users/me` / 受保护接口 JWT 校验 | 同上 | 返回 `AUTH_UNAUTHORIZED` |
| Supabase Auth | signOut / revoke | `DELETE /api/auth/session` | 同上 | 本地端仍可继续登出 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| 统一认证入口 | 移动端只调 Backend REST API | 新增 `/api/auth/*` 与 `/api/users/me` |
| 自动注册 | 首次验证码通过即自动创建用户 | 复用 Supabase Auth user 创建 + profile hook；返回 `isNewUser=true` |
| `AuthSession` 一致结构 | create / refresh 返回完全一致 | `AuthService.mapSession()` 统一映射 |
| 真实 JWT 校验 | 受保护接口不再使用骨架 userId | `verifyJwt()` + `requireAuthContext` |
| refresh 失败回匿名 | 401 让端侧单飞 refresh 或清 session | 后端明确区分 access token 失效与 refresh token 失效的 401 |
| 退出登录幂等 | logout 失败不阻止本地清理 | `DELETE /api/auth/session` 尽量返回幂等成功语义 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Middleware | `withErrorHandler` | 统一输出错误响应 |
| Auth Middleware | `verifyJwt()` / `requireAuthContext` | 负责 token 校验与 `request.auth` 注入 |
| Service | `AppError` | 认证业务错误统一抛出 |
| 日志 | `console.error/warn` + request context | 记录 route、scene、requestId，不记录 token/验证码/完整手机号 |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `AUTH_INVALID_PHONE` | 400 | 手机号格式非法 | `{ "error": { "code": "AUTH_INVALID_PHONE", "message": "Invalid phone" } }` |
| `AUTH_INVALID_CODE` | 400 | 验证码格式非法或不匹配 | `{ "error": { "code": "AUTH_INVALID_CODE", "message": "Invalid verification code" } }` |
| `VALIDATION_ERROR` | 400 | Zod 字段级校验失败 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `AUTH_UNAUTHORIZED` | 401 | access token 无效或会话失效 | `{ "error": { "code": "AUTH_UNAUTHORIZED", "message": "Authentication required" } }` |
| `AUTH_REFRESH_EXPIRED` | 401 | refresh token 无效、过期或已吊销 | `{ "error": { "code": "AUTH_REFRESH_EXPIRED", "message": "Refresh token expired" } }` |
| `AUTH_CODE_COOLDOWN` | 409 | 验证码发送冷却中 | `{ "error": { "code": "AUTH_CODE_COOLDOWN", "message": "Please wait before requesting another code" } }` |
| `AUTH_CODE_EXPIRED` | 410 | 验证码已过期 | `{ "error": { "code": "AUTH_CODE_EXPIRED", "message": "Verification code expired" } }` |
| `AUTH_RATE_LIMITED` | 429 | OTP 请求/校验触发限流 | `{ "error": { "code": "AUTH_RATE_LIMITED", "message": "Too many auth requests" } }` |
| `NOT_FOUND` | 404 | 当前用户摘要不存在 | `{ "error": { "code": "NOT_FOUND", "message": "User not found" } }` |
| `SERVICE_UNAVAILABLE` | 503 | 上游服务不可用 | `{ "error": { "code": "SERVICE_UNAVAILABLE", "message": "Service unavailable: Supabase Auth" } }` |
| `INTERNAL_ERROR` | 500 | 未知内部错误 | `{ "error": { "code": "INTERNAL_ERROR", "message": "Internal server error" } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 手机号格式非法 | 少位数 / 非数字 | 400 | route 层直接拦截 |
| 验证码格式非法 | 非 6 位数字 | 400 | route 层直接拦截 |
| 验证码错误 | 上游校验失败但未过期 | 400 + `AUTH_INVALID_CODE` | 不泄露底层 provider 原文 |
| 验证码已过期 | OTP 超过有效期 | 410 + `AUTH_CODE_EXPIRED` | 端侧清理当前验证码态并允许重发 |
| auth user 已创建但 profile 缺失 | hook 失败或脏数据 | 404 / 惰性补偿后 200 | 优先尝试补齐，再决定报错 |
| refresh token 无效 | 已吊销 / 已过期 | 401 + `AUTH_REFRESH_EXPIRED` | 端侧据此清 session |
| `Authorization` 缺失 | 请求 `me` 时无 token | 401 + `AUTH_UNAUTHORIZED` | `requireAuthContext` 拦截 |
| logout 时 token 缺失或已失效 | 服务端已无会话或 bearer 不可用 | 200 + `code=0` | 统一幂等成功，减轻端侧分支 |
| 排行公开接口带了无效 token | 可选 auth context 解析失败 | 视策略降级为匿名访问或返回 401 | 推荐对公开列表降级匿名，booking 操作仍强鉴权 |

### 11.4 错误日志与监控

- 打点建议：
  - `auth_otp_requested`
  - `auth_session_created`
  - `auth_session_refreshed`
  - `auth_session_refresh_failed`
  - `auth_logout_requested`
  - `auth_profile_missing`
- 记录字段：`requestId`, `scene`, `route`, `userId`（若已知）, `statusCode`, `errorCode`。
- 严禁记录：`accessToken`, `refreshToken`, `code`, 完整手机号。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| 单元测试 | `AuthService` OTP / session / refresh / user mapping 逻辑 | vitest |
| 集成测试 | auth routes + middleware + error handler | vitest + Next route handler 测试 |
| Repository 测试 | profile/auth user 映射与缺失场景 | mock Supabase client |
| Schema 测试 | auth request/response schema | vitest |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| B-01 | 发送 OTP 成功 | 合法手机号 | `200 + code=0 + cooldownSeconds` | Route / Service |
| B-02 | 手机号非法 | `phone=123` | `400 + AUTH_INVALID_PHONE` | Route |
| B-03 | OTP 校验成功且首登 | 新手机号 + 正确验证码 | `AuthSession.user.isNewUser=true` | Service |
| B-04 | OTP 校验成功且老用户 | 已存在手机号 + 正确验证码 | `isNewUser=false` | Service |
| B-05 | OTP 错误 | 错误验证码 | `400 + AUTH_INVALID_CODE` | Route / Service |
| B-06 | refresh 成功 | 合法 refresh token | 返回全量新 `AuthSession` | Service |
| B-07 | refresh 失败 | 过期 refresh token | `401 + AUTH_REFRESH_EXPIRED` | Service |
| B-08 | me 成功 | 合法 access token | 返回脱敏手机号与 role | Route / Middleware |
| B-09 | me 无 token | 无 Authorization | `401 + AUTH_UNAUTHORIZED` | Middleware |
| B-10 | logout 幂等 | token 已失效 | `200 + code=0` | Route |
| B-11 | 公开排行匿名可读 | 无 token 调 rankings | 正常返回列表 | Route |
| B-12 | rankings 带无效 token | 非法 Bearer | 按设计降级匿名或明确 401 | Route |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| Supabase Auth | mock admin client methods | 不依赖真实 Supabase 网络 |
| Profile Repository | mock repository | 控制 profile 存在/缺失/补齐场景 |
| Zod parse | 真实执行 | 保证参数校验与实现一致 |

---

## 13. 安全考虑

- **认证与授权**：
  - `verifyJwt()` 必须作为受保护接口唯一正式鉴权入口；
  - `x-user-id` 不得继续作为移动端生产认证方案。
- **输入校验**：
  - 所有 auth body 都做 Zod 校验；
  - 对手机号、验证码统一正则与 trim 规则。
- **敏感数据处理**：
  - 返回 UI 的手机号必须脱敏；
  - 错误 message 不透出底层 provider 响应原文。
- **SQL 注入防护**：
  - 继续使用 Supabase query builder，无手写拼接 SQL。
- **CSRF/XSS 防护**：
  - 移动端 Bearer Token API 不依赖 Cookie，会话接口不承担浏览器 CSRF 面；
  - message 文案来自受控错误映射，避免透传上游 HTML/脚本内容。

---

## 14. 性能考虑

- **预期 QPS**：登录链路低频，无需新增缓存层。
- **缓存策略**：`me` 不缓存，以 token 实时校验为准。
- **数据库优化**：当前用户摘要查询走 `profiles.id` 主键；如新增 `profiles.phone`，只做简单索引即可。
- **连接池配置**：沿用当前 Supabase client 复用策略；`getSupabaseAdmin()` 单例实例足够。

---

## 15. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 继续复用 Next.js、Supabase、Zod 现有能力 |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 16. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| `profiles` 缺少手机号字段，无法稳定返回脱敏手机号 | `GET /api/users/me`、登录后视图 | 🔴 | 中 | 优先从 `auth.users.phone` 读取；必要时补 migration | 临时返回可识别用户标识，后续再补字段 |
| 现有骨架接口依赖 `getOptionalUserId()` | 排行等旧业务接口 | 🔴 | 中 | 本期统一梳理受保护接口与公开接口的 auth contract | 先保留 deprecated helper，仅供过渡 |
| Supabase OTP/refresh 行为与本地 fixture 存在差异 | 开发 / CI 验证 | 🟡 | 中 | 以 service 层 mock + 本地 stack 双轨验证 | 用测试夹具替代真实 OTP 下发 |
| logout 上游接口幂等性不稳定 | 移动端退出体验 | 🟡 | 低 | Backend 端封装稳定语义，尽量吞掉“已失效”类上游错误 | 端侧始终按本地登出成功处理 |
| 后续 PRD 对登录拦截来源上下文要求扩展 | 评论/消息/资产复用 | 🟡 | 高 | 从一开始就定义 `source/returnRoute/pendingAction` 结构 | 兼容新增 source 枚举 |

---

## 17. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/decisions/2026-07-24-supabase-baas.md` | 认证基础设施 | Supabase Auth 为统一用户认证基础 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `backend/CLAUDE.md` | Backend 四层架构、Zod、AppError、RESTful 约束 |
| `backend/src/middleware/auth.ts` | 当前真实 JWT 校验基础与骨架态 helper 共存 |
| `backend/src/middleware/error-handler.ts` | 全局错误处理已可复用 |
| `backend/src/lib/errors.ts` | 错误码和 AppError 映射基础 |
| `backend/src/infrastructure/supabase.ts` | `getSupabaseAdmin()` 生命周期与配置 |
| `backend/src/lib/schemas.ts` | 当前 schema 风格基线 |
| `backend/src/repositories/supabase/user.supabase.repository.ts` | `profiles` 查询基础能力 |
| `backend/src/app/api/admin/auth/login/route.ts` | `{ code, data, message }` 成功响应风格样例 |
| `backend/src/app/api/admin/auth/logout/route.ts` | logout route 成功响应风格样例 |
| `backend/supabase/migrations/00000000000001_init_tables.sql` | `profiles` 与 `auth.users` 关联 |
| `backend/supabase/migrations/20260727000200_add_role_to_profiles.sql` | `profiles.role` 字段 |
| `backend/supabase/migrations/20260727000400_auth_hook_role_sync.sql` | 创建 auth user 时自动建 profile、同步 role |
