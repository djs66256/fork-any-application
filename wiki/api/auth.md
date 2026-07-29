# Auth API 文档

> 最后更新：2026-07-29

---

## 概述

移动端认证 API 负责承接 Android / iOS 的手机号验证码登录、自动注册、会话刷新、当前用户查询与退出登录闭环。当前客户端**不直接接入 Supabase Auth SDK**，而是统一通过 Backend 暴露的 RESTful 接口完成认证流程；Backend 再负责与 Supabase Auth、local test OTP bypass、本地 profile 回填逻辑交互，并把对外契约收敛为统一的 `{ code, data, message }` envelope（`backend/src/app/api/auth/otp-requests/route.ts:7-14`、`backend/src/app/api/auth/sessions/route.ts:7-14`、`backend/src/app/api/auth/session-refreshes/route.ts:7-14`、`backend/src/app/api/auth/session/route.ts:6-13`、`backend/src/app/api/users/me/route.ts:7-13`）。

当前 auth contract 的关键特点：

- 登录 / 注册一体化：手机号首次验证码校验成功即自动注册，不提供独立注册页（`docs/specs/2026-07-28-prd-08-login/spec.md:423-429`、`backend/src/services/auth/auth.service.ts:250-299`）。
- 对外 payload 使用 camelCase，而不是内部 schema 的 snake_case（`backend/src/app/api/auth/_helpers.ts:12-29`、`backend/src/lib/schemas.ts:347-384`）。
- `GET /api/users/me`、`DELETE /api/auth/session` 与受保护业务接口统一依赖真实 bearer access token 校验，不再以 `x-user-id` 或 `Bearer <user-id>` 作为正式移动端 contract（`backend/src/middleware/auth.ts:27-76`、`docs/specs/2026-07-28-prd-08-login/spec.md:651-655`）。
- 本地开发环境支持 test OTP bypass；短信 provider 不可用时统一映射为 `SERVICE_UNAVAILABLE`，而不是误报手机号错误（`backend/src/services/auth/auth.service.ts:58-68`、`backend/src/services/auth/auth.service.ts:141-164`）。
- 登出接口保持幂等：缺少 token、fake token、已失效 session 都应返回 `200 + data: null`（`backend/src/services/auth/auth.service.ts:379-416`、`backend/src/app/api/__tests__/auth-session.test.ts:18-45`）。

---

## POST /api/auth/otp-requests

### 功能简介

创建一次手机号 OTP 请求并触发验证码发送，是移动端登录页的第一步。Route 仅负责 request body 解析与 schema 校验，真实逻辑由 `AuthService.sendOtp()` 处理；在 test OTP bypass 场景下，请求不会依赖真实短信 provider，也不会要求外部短信服务可用（`backend/src/app/api/auth/otp-requests/route.ts:7-14`、`backend/src/services/auth/auth.service.ts:210-248`）。

### 代码文件路径

- Route：`backend/src/app/api/auth/otp-requests/route.ts:7-14`
- Service：`backend/src/services/auth/auth.service.ts:210-248`
- Schema：`backend/src/lib/schemas.ts:287-315`

### path / method

`POST /api/auth/otp-requests`

### Request Body

```json
{
  "countryCode": "+86",
  "phone": "13800138000",
  "scene": "login"
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `countryCode` | string | 否 | `+86` | 国家区号，必须匹配 `^\+[1-9]\d{0,3}$` |
| `phone` | string | 是 | — | 11 位大陆手机号，必须匹配 `^1\d{10}$` |
| `scene` | enum | 否 | `login` | 当前只支持 `login` |

### Success Response

```json
{
  "code": 0,
  "data": {
    "requestId": "test_otp_13800138000",
    "cooldownSeconds": 60,
    "expiresInSeconds": 300
  },
  "message": "ok"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.requestId` | string | OTP 请求标识；test bypass 下返回 `test_otp_<phone>` |
| `data.cooldownSeconds` | number | 冷却时间，当前固定 60 秒 |
| `data.expiresInSeconds` | number | 验证码有效期，当前固定 300 秒 |

### 当前行为说明

- Route 使用 `SendOtpRequestSchema` 做 body 校验，非法手机号或非法区号会在进入 Service 前被拒绝（`backend/src/app/api/auth/otp-requests/route.ts:7-10`、`backend/src/lib/schemas.ts:299-315`）。
- 若开启 test OTP bypass 且命中测试手机号，Service 直接返回固定 `requestId/cooldownSeconds/expiresInSeconds`，不依赖真实短信服务（`backend/src/services/auth/auth.service.ts:210-217`）。
- 非 bypass 情况下，Service 会通过 `supabase.auth.signInWithOtp(..., { shouldCreateUser: true })` 发送验证码，并为“首次登录自动注册”预留用户创建语义（`backend/src/services/auth/auth.service.ts:219-241`）。
- 当上游短信 provider 未启用、配置不支持或 Supabase Auth 不可用时，统一返回 `SERVICE_UNAVAILABLE`，避免把 provider 配置问题误报为 `AUTH_INVALID_PHONE`（`backend/src/services/auth/auth.service.ts:127-164`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功创建 OTP 请求 |
| 400 | `VALIDATION_ERROR` | 请求体字段不合法 |
| 400 | `AUTH_INVALID_PHONE` | 手机号格式不正确 |
| 409 | `AUTH_CODE_COOLDOWN` | 发送过于频繁，需等待冷却 |
| 429 | `AUTH_RATE_LIMITED` | 命中验证码发送限流 |
| 503 | `SERVICE_UNAVAILABLE` | 短信 provider 不可用或 Auth 上游异常 |

---

## POST /api/auth/sessions

### 功能简介

校验短信验证码并创建登录会话。该接口同时承载“首次验证码通过即自动注册”和“已存在用户登录”两种语义，对外统一返回 `AuthSessionPayload`。Route 只做 schema 校验，`AuthService.createSession()` 负责与 Supabase `verifyOtp` 交互、识别是否首登、回填 profile，并最终生成对外会话数据（`backend/src/app/api/auth/sessions/route.ts:7-14`、`backend/src/services/auth/auth.service.ts:250-299,475-497`）。

### 代码文件路径

- Route：`backend/src/app/api/auth/sessions/route.ts:7-14`
- Payload 映射：`backend/src/app/api/auth/_helpers.ts:12-29`
- Service：`backend/src/services/auth/auth.service.ts:250-299,475-497`
- Schema：`backend/src/lib/schemas.ts:317-367`

### path / method

`POST /api/auth/sessions`

### Request Body

```json
{
  "countryCode": "+86",
  "phone": "13800138000",
  "code": "123456"
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `countryCode` | string | 否 | `+86` | 国家区号 |
| `phone` | string | 是 | — | 11 位大陆手机号 |
| `code` | string | 是 | — | 6 位数字验证码 |

### Success Response

```json
{
  "code": 0,
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<refresh-token>",
    "expiresAt": "2026-07-29T13:34:56.000Z",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "phone": "138****8000",
      "displayName": null,
      "avatarUrl": null,
      "role": "viewer",
      "isNewUser": true
    }
  },
  "message": "ok"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.accessToken` | string | 受保护接口使用的 bearer access token |
| `data.refreshToken` | string | 用于换发新 session 的 refresh token |
| `data.expiresAt` | string | access token 过期时间，ISO 8601 |
| `data.user.id` | string | 用户 UUID |
| `data.user.phone` | string | 已脱敏手机号 |
| `data.user.displayName` | string \| null | 展示昵称，当前可为空 |
| `data.user.avatarUrl` | string \| null | 头像 URL，当前可为空 |
| `data.user.role` | enum | `admin` / `editor` / `viewer`，移动端默认 `viewer` |
| `data.user.isNewUser` | boolean | 本次是否首登 |

### 当前行为说明

- Route 会使用 `CreateAuthSessionRequestSchema` 做请求体校验（`backend/src/app/api/auth/sessions/route.ts:7-10`、`backend/src/lib/schemas.ts:317-327`）。
- 如果命中 test OTP bypass，错误验证码直接返回 `AUTH_INVALID_CODE`；正确验证码会走本地 local auth session 流程，并按手机号是否首次出现决定 `isNewUser`（`backend/src/services/auth/auth.service.ts:250-257,418-473`）。
- 正常路径下，Service 使用 `supabase.auth.verifyOtp` 校验验证码，并根据 `created_at === last_sign_in_at` 或 `app_metadata.is_new_user` 识别是否首登（`backend/src/services/auth/auth.service.ts:107-117,262-293`）。
- 构建会话时，如果 profile 尚未存在，会先调用 `ensureAuthUserProfile()` 兜底回填，再重新读取 profile，避免首次登录因 profile 缺失失败（`backend/src/services/auth/auth.service.ts:475-497`）。
- 最终响应通过 `mapAuthSessionPayload()` 从 snake_case 会话实体映射为 camelCase payload（`backend/src/app/api/auth/_helpers.ts:23-29`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 验证码校验成功并返回会话 |
| 400 | `VALIDATION_ERROR` | 请求体字段不合法 |
| 400 | `AUTH_INVALID_CODE` | 验证码错误 |
| 410 | `AUTH_CODE_EXPIRED` | 验证码已过期 |
| 429 | `AUTH_RATE_LIMITED` | 校验请求过于频繁 |
| 404 | `NOT_FOUND` | 会话已创建但用户 profile 无法回填 |
| 503 | `SERVICE_UNAVAILABLE` | Auth 上游异常 |

---

## POST /api/auth/session-refreshes

### 功能简介

使用 refresh token 换发新的 session，对外仍返回与登录成功相同的 `AuthSessionPayload`。客户端在 access token 过期或 `GET /api/users/me` 返回 401 时走该接口；refresh 失败则应清空本地 session，退回匿名态（`docs/specs/2026-07-28-prd-08-login/spec.md:466-478`、`backend/src/app/api/auth/session-refreshes/route.ts:7-14`、`backend/src/services/auth/auth.service.ts:302-357`）。

### 代码文件路径

- Route：`backend/src/app/api/auth/session-refreshes/route.ts:7-14`
- Payload 映射：`backend/src/app/api/auth/_helpers.ts:23-29`
- Service：`backend/src/services/auth/auth.service.ts:302-357`
- Schema：`backend/src/lib/schemas.ts:324-367`

### path / method

`POST /api/auth/session-refreshes`

### Request Body

```json
{
  "refreshToken": "<refresh-token>"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `refreshToken` | string | 是 | refresh token，不能为空字符串 |

### Success Response

```json
{
  "code": 0,
  "data": {
    "accessToken": "<new-jwt>",
    "refreshToken": "<new-refresh-token>",
    "expiresAt": "2026-07-29T14:34:56.000Z",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "phone": "138****8000",
      "displayName": null,
      "avatarUrl": null,
      "role": "viewer",
      "isNewUser": false
    }
  },
  "message": "ok"
}
```

### 当前行为说明

- Route 使用 `RefreshAuthSessionRequestSchema` 校验 body（`backend/src/app/api/auth/session-refreshes/route.ts:7-10`、`backend/src/lib/schemas.ts:324-327`）。
- 若 refresh token 属于 local test session，会走本地 `refreshLocalAuthSession()` 并返回新的本地 token 对（`backend/src/services/auth/auth.service.ts:302-323`）。
- 非本地路径下，Service 调用 `supabase.auth.refreshSession({ refresh_token })`；若返回缺失 session/user 或出现 refresh/jwt 类错误，会统一映射为 `AUTH_REFRESH_EXPIRED`（`backend/src/services/auth/auth.service.ts:192-203,325-356`）。
- refresh 成功后，仍会复用 `buildSession()` 生成当前用户摘要，因此返回结构与登录成功完全一致（`backend/src/services/auth/auth.service.ts:336-350,475-497`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | refresh 成功 |
| 400 | `VALIDATION_ERROR` | `refreshToken` 缺失或为空 |
| 401 | `AUTH_REFRESH_EXPIRED` | refresh token 无效、过期或已失效 |
| 503 | `SERVICE_UNAVAILABLE` | Auth 上游异常 |

---

## GET /api/users/me

### 功能简介

查询当前登录用户摘要，并验证 access token 当前是否仍有效。该接口由 `requireAuthContext()` 强制要求 bearer token，通过 `verifyJwt()` 统一校验 Supabase JWT 或 local test access token；成功后返回 camelCase 的 `CurrentUserPayload`（`backend/src/app/api/users/me/route.ts:7-13`、`backend/src/middleware/auth.ts:27-103`、`backend/src/app/api/__tests__/users-me.test.ts:36-109`）。

### 代码文件路径

- Route：`backend/src/app/api/users/me/route.ts:7-13`
- Auth Middleware：`backend/src/middleware/auth.ts:27-103,132-138`
- Service：`backend/src/services/auth/auth.service.ts:359-377`
- Schema：`backend/src/lib/schemas.ts:369-384`
- 测试：`backend/src/app/api/__tests__/users-me.test.ts:31-109`

### path / method

`GET /api/users/me`

### Headers

| 字段 | 必填 | 说明 |
|------|------|------|
| `Authorization` | 是 | `Bearer <accessToken>` |

### Success Response

```json
{
  "code": 0,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "phone": "138****8000",
    "displayName": null,
    "avatarUrl": null,
    "role": "viewer",
    "isNewUser": false
  },
  "message": "ok"
}
```

### 当前行为说明

- `requireAuthContext()` 会在 Route 执行前强制完成鉴权，失败直接抛出 `AUTH_UNAUTHORIZED`（`backend/src/middleware/auth.ts:69-103`）。
- `verifyJwt()` 会优先识别 local test access token；否则使用 `supabase.auth.getUser(token)` 校验真实 access token，并把 role 收口到 `admin/editor/viewer`（`backend/src/middleware/auth.ts:27-63`）。
- Route 通过 `getAuth(request)` 拿到 userId 后，再调用 `AuthService.getCurrentUser(userId)` 查询用户摘要（`backend/src/app/api/users/me/route.ts:7-13`）。
- 测试已覆盖三类关键路径：valid bearer 成功、缺少 Authorization 时返回 401、profile 不存在时透传 404（`backend/src/app/api/__tests__/users-me.test.ts:36-109`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | access token 有效，成功返回当前用户 |
| 401 | `AUTH_UNAUTHORIZED` | 未携带 token、token 非法或 token 失效 |
| 404 | `NOT_FOUND` | token 对应用户存在，但 profile 不存在 |
| 503 | `SERVICE_UNAVAILABLE` | 鉴权依赖的 Auth 上游异常 |

---

## DELETE /api/auth/session

### 功能简介

使当前客户端会话退出，是移动端设置页“退出登录”的后端承接接口。接口本身保持幂等：无论 Authorization 缺失、token 已失效、session 已不存在，Route 都会返回 `200 + data: null`；客户端仍应以本地清理 session 为准（`backend/src/app/api/auth/session/route.ts:6-13`、`backend/src/services/auth/auth.service.ts:379-416`、`docs/specs/2026-07-28-prd-08-login/spec.md:255-261`）。

### 代码文件路径

- Route：`backend/src/app/api/auth/session/route.ts:6-13`
- Helper：`backend/src/app/api/auth/_helpers.ts:32-39`
- Service：`backend/src/services/auth/auth.service.ts:379-416`
- 测试：`backend/src/app/api/__tests__/auth-session.test.ts:13-45`

### path / method

`DELETE /api/auth/session`

### Headers

| 字段 | 必填 | 说明 |
|------|------|------|
| `Authorization` | 否 | `Bearer <accessToken>`；缺失时仍按幂等成功处理 |

### Success Response

```json
{
  "code": 0,
  "data": null,
  "message": "ok"
}
```

### 当前行为说明

- Route 先用 `extractAccessToken()` 从 `Authorization` 头中提取 access token；若格式不匹配 `Bearer `，则直接视为 `undefined`（`backend/src/app/api/auth/_helpers.ts:32-39`、`backend/src/app/api/auth/session/route.ts:6-13`）。
- 当 token 缺失时，`AuthService.logout()` 直接返回，不报错（`backend/src/services/auth/auth.service.ts:379-382`）。
- 对 local test access token，会直接在本地 local session store 中吊销（`backend/src/services/auth/auth.service.ts:384-387`）。
- 对真实 Supabase session，会调用 `supabase.auth.admin.signOut(accessToken)`；若遇到 `400/401/403/404`、`invalid jwt`、`session not found` 等“已无有效会话”类错误，会吞掉并保持 success，只有真正的上游服务不可用才会抛 `SERVICE_UNAVAILABLE`（`backend/src/services/auth/auth.service.ts:389-416`）。
- 测试已覆盖“带 bearer access token 正常登出”和“Authorization 缺失时保持幂等 success”两条关键路径（`backend/src/app/api/__tests__/auth-session.test.ts:18-45`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 登出成功，或幂等处理已失效 / 缺失会话 |
| 503 | `SERVICE_UNAVAILABLE` | Auth 上游真正不可用 |

---

## 与受保护业务接口的关系

认证 API 本身之外，PRD-08 还把既有排行相关接口从骨架态认证升级为真实 auth context：

- `GET /api/dramas/rankings` 通过 `resolveOptionalAuthContext()` 解析 bearer token；若 token 有效，则把 `authContext.userId` 传给 `DramaService.listRankings()`，用于计算 `is_booked`（`backend/src/app/api/dramas/rankings/route.ts:8-23`、`backend/src/middleware/auth.ts:65-67`）。
- `POST /api/dramas/:id/book` 通过 `requireAuthContext()` 强制要求真实登录态，再从 `getAuth(request)` 读取 userId 提交预约（`backend/src/app/api/dramas/[id]/book/route.ts:16-28`、`backend/src/middleware/auth.ts:97-103,132-138`）。

因此，移动端认证完成后，排行预约不再依赖 `x-user-id` 或伪造 bearer，而是统一基于真实 access token 执行。

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录 PRD-08 移动端认证 API，补充 OTP 请求、登录/自动注册、refresh、me、logout，以及真实 bearer auth 对排行/预约接口的影响 |

---

*本文档由 llm-wiki skill 自动维护。*