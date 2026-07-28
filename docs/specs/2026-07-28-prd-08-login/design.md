# 技术方案（共享部分）：PRD-08 用户登录与注册

> 创建日期：2026-07-28
> 对应需求：spec.md

## 整体架构

```mermaid
flowchart LR
    ProfileEntry[iOS / Android 我的频道匿名入口] --> LoginPage[全屏 Native 登录页]
    Intercept[排行预约登录拦截] --> LoginPage
    LoginPage --> OtpRequestAPI[POST /api/auth/otp-requests]
    LoginPage --> SessionCreateAPI[POST /api/auth/sessions]
    AppRestore[冷启动 / 前台恢复] --> MeAPI[GET /api/users/me]
    AppRestore --> RefreshAPI[POST /api/auth/session-refreshes]
    Settings[设置页退出登录] --> LogoutAPI[DELETE /api/auth/session]

    OtpRequestAPI --> AuthRoutes[Next.js Route Handlers]
    SessionCreateAPI --> AuthRoutes
    MeAPI --> AuthRoutes
    RefreshAPI --> AuthRoutes
    LogoutAPI --> AuthRoutes

    AuthRoutes --> AuthService[AuthService]
    AuthService --> SupabaseAdmin[getSupabaseAdmin()]
    AuthService --> ProfileRepo[User/Profile Repository]
    SupabaseAdmin --> SupabaseAuth[Supabase Auth]
    ProfileRepo --> Profiles[(profiles)]

    ProtectedAPI[受保护业务接口] --> JwtMiddleware[JWT 校验中间件]
    JwtMiddleware --> SupabaseAdmin
    JwtMiddleware --> DramaRoutes[业务 Route / Service]

    AuthService --> Envelope[统一 ApiEnvelope]
    Envelope --> MobileState[移动端 AuthStatus 状态机]
    MobileState --> ProfileView[我的频道登录后视图]
    MobileState --> ReturnRoute[返回来源页 / 安全默认页]
```

### 架构说明

- 本期采用 **Backend 统一认证入口 + 移动端统一状态机** 的方案，移动端不直接接入 Supabase Auth SDK，而是只消费 Backend 暴露的 RESTful 认证接口。
- Backend 继续遵循现有四层结构：**Route → Service → Repository → Infrastructure / Shared**。认证相关 Route 负责参数解析与响应包裹，AuthService 负责与 Supabase Auth 交互、会话换发和用户摘要聚合。
- 现有受保护业务接口从骨架态 `x-user-id` / “Bearer 即 userId” 语义，收敛为 **真实 Bearer JWT 校验**；排行预约作为首个复用登录拦截的业务入口。
- iOS / Android 共用同一组 `AuthSession`、`AuthUser`、`LoginInterceptionContext`、`AuthStatus` 语义，并共同遵守 single-flight refresh、最多一次重放、失败后清本地回匿名的契约。
- “我的”频道、登录页、设置页、业务拦截回跳都属于 Native 承接能力；Web 不在本期交付范围内。

## API 设计

### 涉及变更

| 类型 | 数量 | 说明 |
|------|------|------|
| 新增接口 | 5 | OTP 请求、会话创建、会话刷新、当前用户、退出登录 |
| 修改接口 | 2 | 认证中间件升级为真实 JWT 校验；排行预约等受保护接口改为消费真实 auth context |
| 废弃接口 | 0 | 不立即删除旧骨架 helper，但不再作为移动端正式认证 contract |

### 新增接口

#### `POST /api/auth/otp-requests`

- **功能简介**：创建一次登录 OTP 请求，并由 Backend 调用 Supabase Auth 发送验证码。
- **Path Parameters**：无
- **Query Parameters**：无
- **Request Body**：

```json
{
  "phone": "13800138000",
  "countryCode": "+86",
  "scene": "login"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | string | 是 | 11 位中国大陆手机号，不带空格与分隔符 |
| `countryCode` | string | 是 | 本期固定为 `+86` |
| `scene` | string | 是 | 本期固定为 `login`，为后续评论/消息/资产复用保留扩展位 |

- **Response**：

```json
{
  "code": 0,
  "data": {
    "requestId": "otp_req_xxx",
    "cooldownSeconds": 60,
    "expiresInSeconds": 300
  },
  "message": "ok"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.requestId` | string | OTP 请求标识，主要用于日志关联与测试夹具 |
| `data.cooldownSeconds` | number | 客户端发送验证码按钮倒计时 |
| `data.expiresInSeconds` | number | 本次验证码有效期 |

- **Error Codes**：

| HTTP 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `AUTH_INVALID_PHONE` / `VALIDATION_ERROR` | 手机号、区号、scene 非法 |
| 409 | `AUTH_CODE_COOLDOWN` | 发送冷却中 |
| 429 | `AUTH_RATE_LIMITED` | 发送过于频繁 |
| 503 | `SERVICE_UNAVAILABLE` | 短信 / Auth 上游暂不可用 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

#### `POST /api/auth/sessions`

- **功能简介**：校验验证码，自动注册或登录，并返回统一 `AuthSession`。
- **Path Parameters**：无
- **Query Parameters**：无
- **Request Body**：

```json
{
  "phone": "13800138000",
  "countryCode": "+86",
  "code": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | string | 是 | 手机号 |
| `countryCode` | string | 是 | 本期固定 `+86` |
| `code` | string | 是 | 6 位数字验证码 |

- **Response**：

```json
{
  "code": 0,
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<refresh-token>",
    "expiresAt": "2026-07-28T12:34:56Z",
    "user": {
      "id": "user_xxx",
      "phone": "138****8000",
      "displayName": null,
      "role": "viewer",
      "isNewUser": true
    }
  },
  "message": "ok"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.accessToken` | string | 受保护接口 Bearer Token |
| `data.refreshToken` | string | 刷新会话凭证 |
| `data.expiresAt` | string | access token 过期时间 |
| `data.user` | `AuthUser` | 当前用户摘要，包含 `isNewUser` |

- **Error Codes**：

| HTTP 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 登录/自动注册成功 |
| 400 | `AUTH_INVALID_PHONE` / `AUTH_INVALID_CODE` / `VALIDATION_ERROR` | 手机号或验证码格式非法，或验证码错误 |
| 410 | `AUTH_CODE_EXPIRED` | 验证码已过期 |
| 429 | `AUTH_RATE_LIMITED` | 校验尝试过于频繁 |
| 503 | `SERVICE_UNAVAILABLE` | Auth 上游不可用 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

#### `POST /api/auth/session-refreshes`

- **功能简介**：使用 refresh token 换发新会话；仅用于恢复与 401 兜底，不承接首次登录。
- **Path Parameters**：无
- **Query Parameters**：无
- **Request Body**：

```json
{
  "refreshToken": "<refresh-token>"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `refreshToken` | string | 是 | 客户端安全存储中的 refresh token |

- **Response**：

```json
{
  "code": 0,
  "data": {
    "accessToken": "<new-jwt>",
    "refreshToken": "<new-refresh-token>",
    "expiresAt": "2026-07-28T13:34:56Z",
    "user": {
      "id": "user_xxx",
      "phone": "138****8000",
      "displayName": null,
      "role": "viewer",
      "isNewUser": false
    }
  },
  "message": "ok"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | `AuthSession` | 与创建会话接口返回结构完全一致，便于端侧原子替换 |

- **Error Codes**：

| HTTP 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 刷新成功 |
| 400 | `VALIDATION_ERROR` | refresh token 缺失或格式非法 |
| 401 | `AUTH_REFRESH_EXPIRED` | refresh token 无效、过期或已吊销 |
| 503 | `SERVICE_UNAVAILABLE` | Auth 上游不可用 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

#### `GET /api/users/me`

- **功能简介**：校验当前 access token 并返回登录后视图需要的最小用户摘要。
- **Path Parameters**：无
- **Query Parameters**：无
- **Request Header**：

```text
Authorization: Bearer <accessToken>
```

- **Response**：

```json
{
  "code": 0,
  "data": {
    "id": "user_xxx",
    "phone": "138****8000",
    "displayName": null,
    "role": "viewer",
    "isNewUser": false
  },
  "message": "ok"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.id` | string | 当前用户 ID |
| `data.phone` | string | 脱敏手机号，用于我的频道摘要展示 |
| `data.displayName` | string\|null | 当前展示名 |
| `data.role` | string | 权限角色，默认 `viewer` |
| `data.isNewUser` | boolean | 对于 `me` 默认返回 `false`，仅保留结构一致性 |

- **Error Codes**：

| HTTP 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 401 | `AUTH_UNAUTHORIZED` | access token 缺失、非法或已过期 |
| 404 | `NOT_FOUND` | token 对应用户不存在或 profile 缺失 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

#### `DELETE /api/auth/session`

- **功能简介**：使当前客户端会话退出；客户端无论接口成功与否都清本地状态。
- **Path Parameters**：无
- **Query Parameters**：无
- **Request Header**：

```text
Authorization: Bearer <accessToken>
```

- **Response**：

```json
{
  "code": 0,
  "data": null,
  "message": "ok"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | null | 成功退出后无资源体返回 |

- **Error Codes**：

| HTTP 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功；即使服务端已无会话也保持幂等 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

### 修改接口 / 契约

#### 受保护业务接口认证语义升级

- **变更说明**：排行预约、后续评论/消息/资产等受保护接口，不再依赖 `x-user-id` 或把 Bearer 字符串直接当 userId，而是统一消费真实 `AuthContext`。
- **变更前**：`getOptionalUserId()` 可从 `x-user-id` 或 Bearer 原文读取 userId，占位语义明显。
- **变更后**：
  - 公开接口仍可匿名访问；
  - 受保护接口统一通过 JWT 校验得到 `userId + role`；
  - 返回 401 时，移动端按 shared contract 触发 single-flight refresh 或登录拦截。
- **向后兼容性**：对移动端正式认证链路不兼容，但属于本期明确要下线的骨架态语义。

## 数据模型

### 新增/变更数据表

| 表名 | 操作 | 说明 |
|------|------|------|
| `auth.users` | 复用 | Supabase Auth 主用户表，负责 OTP 与 session |
| `profiles` | 修改（逻辑扩展） | 继续作为用户摘要来源，新增/确认读取 `phone` 脱敏展示所需字段或映射来源 |
| `auth session`（Supabase 内部） | 复用 | 不在业务库单独建表，由 Supabase 管理 access/refresh token 生命周期 |
| `otp request`（逻辑模型） | 不新建业务表 | 首版复用 Supabase 能力与上游限流，不在业务库新增 OTP 落地表 |

> 本期设计不要求新增自建认证表；核心目标是接通 Supabase Auth 与 `profiles` 摘要聚合。若当前 `profiles` 尚无手机号字段，可通过 auth user 的 phone 信息映射出脱敏手机号；只有在现有 schema 无法满足时才补一条 migration，为 `profiles` 新增 `phone` 或等价摘要字段。

### 共享 Schema 定义

```typescript
import { z } from 'zod';

export const PhoneSchema = z.string().trim().regex(/^1\d{10}$/);
export const CountryCodeSchema = z.literal('+86');
export const OtpCodeSchema = z.string().trim().regex(/^\d{6}$/);

export const SendOtpRequestSchema = z.object({
  phone: PhoneSchema,
  countryCode: CountryCodeSchema,
  scene: z.literal('login'),
});

export const CreateAuthSessionRequestSchema = z.object({
  phone: PhoneSchema,
  countryCode: CountryCodeSchema,
  code: OtpCodeSchema,
});

export const RefreshAuthSessionRequestSchema = z.object({
  refreshToken: z.string().trim().min(1),
});

export const AuthUserSchema = z.object({
  id: z.string().uuid(),
  phone: z.string().min(1),
  displayName: z.string().trim().min(1).nullable(),
  role: z.enum(['admin', 'editor', 'viewer']).default('viewer'),
  isNewUser: z.boolean(),
});

export const AuthSessionSchema = z.object({
  accessToken: z.string().min(1),
  refreshToken: z.string().min(1),
  expiresAt: z.string(),
  user: AuthUserSchema,
});

export const AuthStatusSchema = z.enum([
  'anonymous',
  'restoring',
  'authenticated',
  'refreshing',
  'expired',
]);
```

### 共享实体约束

| 实体 | 关键字段 | 约束 |
|------|---------|------|
| `AuthSession` | `accessToken`, `refreshToken`, `expiresAt`, `user` | 刷新接口与创建会话接口返回结构必须完全一致 |
| `AuthUser` | `id`, `phone`, `displayName`, `role`, `isNewUser` | `phone` 需脱敏；`isNewUser` 仅在首次登录成功时为 `true` |
| `LoginInterceptionContext` | `source`, `returnRoute`, `pendingAction` | 排行预约先落地；后续 PRD 可继续扩展来源枚举 |
| `AuthStatus` | `anonymous/restoring/authenticated/refreshing/expired` | 三端对状态名与状态迁移保持一致 |

## 跨端共享逻辑

| 共享逻辑 | 说明 | 涉及端 |
|---------|------|--------|
| 主动登录入口 | “我的”频道匿名态展示 CTA，进入全屏 Native 登录页 | iOS / Android |
| 协议门禁 | 未勾选协议时，发送验证码与确认登录按钮都不可提交 | iOS / Android |
| 登录注册一体化 | 首次验证码校验成功即自动注册，无独立注册页 | Backend / iOS / Android |
| 会话原子替换 | refresh 成功后必须一次性替换整份 `AuthSession` | iOS / Android |
| single-flight refresh | 同一时刻最多一条 refresh 请求，其它 401 请求等待同一结果 | iOS / Android |
| 401 最多重试一次 | refresh 成功后仅重放当前失败请求一次，避免无限循环 | iOS / Android |
| refresh 失败回匿名 | refresh 失败后清 session、置 `expired/anonymous`、必要时重新拦截登录 | Backend / iOS / Android |
| 登录成功回跳 | 有 `returnRoute` 时优先回来源路由；无则回“我的”频道或安全默认页 | iOS / Android |
| 排行预约拦截复用 | iOS `RankingLoginContext` 与 Android `RequireLogin(returnRoute)` 统一映射到 `LoginInterceptionContext` | iOS / Android |
| 退出登录本地优先 | logout 请求失败也要清本地状态，避免假登录 | Backend / iOS / Android |
| 错误文案脱敏 | 不向用户暴露底层 Supabase 错误、token 或堆栈 | Backend / iOS / Android |

### 共享状态机

```text
anonymous
-> 用户主动点登录 / 业务触发拦截
-> login_presented
-> otp_sending -> otp_sent(cooldown)
-> session_creating
-> authenticated

restoring
-> me success -> authenticated
-> me 401 / expired -> refreshing
-> refresh success -> authenticated
-> refresh failed -> expired -> anonymous

authenticated
-> protected request 401 -> refreshing
-> refresh success -> authenticated
-> refresh failed -> expired -> anonymous

authenticated
-> tap logout -> logging_out
-> local clear complete -> anonymous
```

### 回跳与安全默认页规则

| 场景 | 成功后回跳 | 失败 / 取消时回退 |
|------|-----------|------------------|
| 从“我的”频道主动进入登录 | 回“我的”频道登录后态 | 回“我的”频道匿名态 |
| 排行预约拦截进入登录 | 回原排行 route，并保留榜单类型 / 内容类型 | 回来源排行页，不执行预约 |
| returnRoute 丢失或非法 | 回“我的”频道或当前频道根页 | 同样回安全默认页 |

## 安全考虑

- **认证与授权**：
  - 移动端不直连 Supabase Auth SDK，避免两端各自持有不同认证实现。
  - 受保护接口统一要求 `Authorization: Bearer <accessToken>`，由 Backend 通过 Supabase Admin 验证 JWT。
  - `refreshToken` 只允许提交给刷新接口，不能出现在普通业务请求中。
- **数据校验**：
  - 手机号、验证码、countryCode、scene、refreshToken 都由 Backend 使用 Zod 做参数校验。
  - 端侧在提交前也做同构校验，避免无效请求打到服务端。
- **敏感数据处理**：
  - token、验证码、完整手机号不得写入日志、埋点明文或错误提示。
  - iOS 使用 Keychain 或等价安全存储；Android 使用安全存储封装保存 `AuthSession`。
  - 返回给 UI 的手机号必须为脱敏值，例如 `138****8000`。
- **会话安全**：
  - refresh 失败后立即清本地 session，禁止继续以旧 token 访问受保护接口。
  - logout 以“本地清理完成”为用户退出成功的判定标准，避免网络异常导致假登录残留。

## 边界与错误处理（⚠️ 重点，最易遗漏）

### 错误处理架构

- **全局错误处理策略**：
  - Backend 统一通过 `withErrorHandler` 处理 `AppError` / `ZodError`；认证 Route 成功响应使用 `{ code, data, message }`，错误继续收敛到统一业务码与用户可理解 message。
  - iOS 统一映射到 `APIError`，Android 统一映射到 `ApiResult.Error/Exception`，再由 ViewModel 归一成页面状态与轻提示。
- **错误响应格式**：认证成功统一使用 `{ code, data, message }`；失败统一返回带业务错误码与 message 的错误结构，由端侧用 HTTP status + 业务码共同判断。
- **错误日志与监控**：
  - requestId / request route / source scene 可打点；
  - 不记录 token、验证码、完整手机号；
  - refresh 失败、连续 401、OTP 频率限制属于重点监控事件。

### API 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 用户提示文案 |
|-----------|------------|------|-------------|
| `AUTH_INVALID_PHONE` | 400 | 手机号格式非法 | 请输入正确的手机号 |
| `AUTH_INVALID_CODE` | 400 | 验证码格式非法或与当前输入不匹配 | 请输入正确的验证码 |
| `AUTH_UNAUTHORIZED` | 401 | access token 缺失、非法或已失效 | 登录已失效，请重新登录 |
| `AUTH_REFRESH_EXPIRED` | 401 | refresh token 无效、过期或已吊销 | 登录状态已过期，请重新登录 |
| `AUTH_CODE_COOLDOWN` | 409 | 验证码发送冷却中 | 请稍后再试 |
| `AUTH_CODE_EXPIRED` | 410 | 验证码已过期 | 验证码已过期，请重新获取 |
| `AUTH_RATE_LIMITED` | 429 | OTP 发送或校验达到限流阈值 | 操作过于频繁，请稍后再试 |
| `VALIDATION_ERROR` | 400 | 字段级校验失败 | 请检查填写内容 |
| `FORBIDDEN` | 403 | 当前角色无权限 | 当前账号无权访问 |
| `NOT_FOUND` | 404 | 用户摘要不存在 / 资源不存在 | 当前账号信息不存在 |
| `CONFLICT` | 409 | 并发状态冲突 | 当前操作暂不可用，请重试 |
| `SERVICE_UNAVAILABLE` | 503 | Auth / 短信上游不可用 | 暂时无法登录，请稍后重试 |
| `INTERNAL_ERROR` | 500 | 服务内部错误 | 服务开小差了，请稍后重试 |

### 边界场景处理

| 场景 | 触发条件 | API / 客户端行为 | 说明 |
|------|---------|-----------------|------|
| 未勾选协议直接尝试发送/登录 | 客户端前置门禁被绕过 | 客户端阻止提交；后端不依赖该字段决定鉴权 | 协议勾选属于端侧交互门禁 |
| 验证码已过期 | 用户输入过期 OTP | `/api/auth/sessions` 返回 410 + `AUTH_CODE_EXPIRED` | 保持登录页输入态并允许重新获取 |
| 验证码格式非法/错误 | 用户输入错误 OTP | `/api/auth/sessions` 返回 400 + `AUTH_INVALID_CODE` | 保持验证码输入态并允许修正 |
| OTP 频率限制 | 短时间重复发送或校验 | `/api/auth/otp-requests` / `/api/auth/sessions` 返回 409/429 + `AUTH_CODE_COOLDOWN` / `AUTH_RATE_LIMITED` | 客户端显示倒计时或剩余秒数 |
| 本地 session 格式损坏 | JSON 解码失败 / 缺字段 | 客户端直接清本地并回匿名 | 不进入脏登录态 |
| 多请求同时 401 | 多个页面同时命中 access token 失效 | 客户端只发一次 refresh，其余等待结果 | single-flight 必须全局而非单接口级 |
| refresh token 已失效 | `POST /api/auth/session-refreshes` 返回 401 + `AUTH_REFRESH_EXPIRED` | 客户端立即清 session 并回匿名/expired | 不再继续重试 refresh |
| refresh 成功但原请求再 401 | 账号被服务端撤销或上下文变化 | 原请求最多再试一次后回匿名并触发登录拦截 | 防止无限重试 |
| logout 网络失败 | 服务端退出失败或超时 | 客户端仍清本地 session 并回匿名 | 用户视角以本地退出为准 |
| returnRoute 无效 | 来源页已不存在或参数损坏 | 回安全默认页（我的 / 当前频道根页） | 不允许崩溃或卡死空白页 |
| App 长时间后台后恢复 | access token 已过期 | 前台恢复优先 refresh，失败则回匿名 | 避免先显示已登录再闪退匿名 |
| 开发环境无真实短信通道 | 本地 / CI 不具备真实下发能力 | 用本地 Supabase phone fixture 或测试 OTP 夹具完成验证 | 不阻塞本期开发与自动化测试 |

## 性能考虑

- **预期 QPS**：登录与刷新属于低频用户态接口，首版无需额外引入 Redis 缓存或队列。
- **缓存策略**：
  - `GET /api/users/me` 不做端外缓存，始终以当前 token 校验结果为准；
  - 端侧只缓存当前 `AuthSession`，不缓存过期用户态接口响应。
- **数据库优化**：
  - 用户摘要主要走 `profiles.id` 主键查询；
  - 不新增高频业务表扫描。
- **端侧性能**：
  - 启动恢复阶段以轻量 loading / skeleton 避免闪烁；
  - refresh 单飞减少并发 401 风暴。

## 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/decisions/2026-07-24-supabase-baas.md` | Supabase 选型 | Supabase Auth 作为统一认证基础设施 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `backend/src/middleware/auth.ts` | 当前仍存在 `x-user-id` / Bearer 占位语义，但已具备 `verifyJwt()` 基础 |
| `backend/src/middleware/error-handler.ts` | `withErrorHandler` 已统一处理 `AppError` 与 `ZodError` |
| `backend/src/lib/errors.ts` | 现有错误码枚举、HTTP status 映射与错误响应格式 |
| `backend/src/infrastructure/supabase.ts` | `getSupabaseAdmin()` 已关闭 `autoRefreshToken` 与 `persistSession` |
| `backend/src/lib/schemas.ts` | 已有 Zod schema 风格，可按同样模式扩展 auth schema |
| `backend/src/repositories/supabase/user.supabase.repository.ts` | 当前 `profiles` 仓储基础可复用为用户摘要查询 |
| `backend/supabase/migrations/00000000000001_init_tables.sql` | `profiles` 与 `auth.users` 已建立关联 |
| `backend/supabase/migrations/20260727000200_add_role_to_profiles.sql` | `profiles.role` 已存在 |
| `backend/supabase/migrations/20260727000400_auth_hook_role_sync.sql` | auth user 创建与 profile role 同步的 hook 已存在 |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 目前还没有登录 / 设置 route，需要本期补齐 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | 已支持 `pendingRoute` 与跨页回跳 |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | 已有 `RankingLoginContext` 可作为 iOS 拦截来源上下文 |
| `ios/ShortDrama/Sources/Core/Network/APIClient.swift` | 当前支持 headers 注入，但还没有统一 auth provider / refresh 机制 |
| `ios/ShortDrama/Sources/Core/Storage/PlaybackSessionStore.swift` | 已有 Keychain 封装模式，可复用到 auth session 安全存储 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | `.profile` 仍为 placeholder，需要承接“我的”频道登录入口 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 排行拦截回调和 profile placeholder 都已具备接入点 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 已有 ranking/profile 等 route，需要新增 login/settings route |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 认证注入仍是 skeleton，需要补真实 token 注入与 refresh 协作 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | 当前只有 `isLoggedIn()`，需要扩展为真实 session provider |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt` | 当前注入的是假登录实现 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | 已有 `RequireLogin(returnRoute)` effect，可直接接入本期拦截链路 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/PlaybackSessionStore.kt` | 已有 DataStore 存储范式，可复用到 auth session 持久化 |
