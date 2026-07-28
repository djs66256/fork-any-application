# 实现计划：Backend — PRD-08 用户登录与注册

> 创建日期：2026-07-28
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本计划聚焦 Backend 端移动端认证闭环落地：补齐 `/api/auth/*` 与 `/api/users/me`，将受保护接口从骨架态 `x-user-id` / Bearer=userId 语义收敛到真实 JWT 校验，并以轻量 TDD 方式同步完善 schema、service、repository、route 与 middleware 自动化测试。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 认证 schema 与错误码 contract 可覆盖手机号、验证码、refresh token 与成功响应结构 | 合法/非法 `phone`、`code`、`refreshToken`、auth success payload | schema parse 成功；非法输入抛校验错误；新增认证错误码映射到预期 HTTP 状态 | 单元测试 | P0 |
| T-02 | `AuthService` 能映射 OTP 发送、验证码建会话、refresh、当前用户读取与细粒度认证错误 | mock Supabase Auth 返回 success、invalid code、expired code、rate limit、service unavailable、profile 缺失等结果 | 返回统一 `AuthSession` / `AuthUser`；错误被映射为 `AUTH_INVALID_PHONE`、`AUTH_INVALID_CODE`、`AUTH_CODE_EXPIRED`、`AUTH_REFRESH_EXPIRED`、`AUTH_RATE_LIMITED`、`SERVICE_UNAVAILABLE`、`NOT_FOUND` 等 | 单元测试 | P0 |
| T-03 | `verifyJwt` / `requireAuthContext` / `resolveOptionalAuthContext` 建立真实 auth contract | 有效 Bearer、缺失 Bearer、非法 Bearer、未知 role、公开接口携带无效 token | 有效 token 注入 `request.auth`；受保护接口返回 `AUTH_UNAUTHORIZED`；可选 auth 场景返回匿名上下文；未知 role 回落 `viewer` | 单元测试 | P0 |
| T-04 | `/api/auth/otp-requests`、`/api/auth/sessions`、`/api/auth/session-refreshes` route 正确处理成功与校验/业务错误 | 各接口合法 body、非法 body、service 抛出认证错误 | 成功返回 `{ code: 0, data, message: 'ok' }`；失败由 `withErrorHandler` 输出统一错误 body | 单元测试 | P0 |
| T-05 | `/api/users/me` 与 `DELETE /api/auth/session` contract 正确，logout 保持幂等 | 有效/无效 Authorization；token 缺失；已失效 token；service signOut 反馈已无会话 | `GET /api/users/me` 在未授权时返回 `AUTH_UNAUTHORIZED`；`DELETE /api/auth/session` 对缺失/失效 token 仍返回 `200 + { code: 0, data: null, message: 'ok' }` | 单元测试 | P0 |
| T-06 | 排行接口切换到可选真实 auth context 后仍支持匿名访问与个性化 booked 状态 | 无 token、有效 Bearer、无效 Bearer | 匿名请求正常返回；有效 token 将 `authContext` 传给 service；无效 token 在公开接口按匿名降级 | 单元测试 | P1 |

## 实现步骤

### Step 1：补齐认证 schema、响应 envelope 与细粒度错误码 contract

- **关联测试**：T-01
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/lib/errors.ts`、`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/lib/__tests__/errors.test.ts`
- **实现内容**：
  1. 在 `backend/src/lib/schemas.ts` 中新增手机号 OTP 登录所需 schema：`PhoneSchema`、`CountryCodeSchema`、`OtpCodeSchema`、`SendOtpRequestSchema`、`CreateAuthSessionRequestSchema`、`RefreshAuthSessionRequestSchema`、`AuthUserSchema`、`AuthSessionSchema` 以及认证成功 envelope schema，保证 `/api/auth/*` 和 `/api/users/me` 共用同一数据 contract。
  2. 在 `backend/src/lib/errors.ts` 中新增并标准化认证业务错误码与状态码映射，至少覆盖 `AUTH_INVALID_PHONE`、`AUTH_INVALID_CODE`、`AUTH_UNAUTHORIZED`、`AUTH_REFRESH_EXPIRED`、`AUTH_CODE_COOLDOWN`、`AUTH_CODE_EXPIRED`、`AUTH_RATE_LIMITED`，并提供对应 `Errors.*` 工厂方法，避免后续 route/service 回退到笼统 `UNAUTHORIZED`。
  3. 扩展 `backend/src/lib/__tests__/schemas.test.ts` 与 `backend/src/lib/__tests__/errors.test.ts`，验证认证请求/响应 schema、脱敏用户结构和新增错误码到 HTTP status 的映射，为后续 service/route 改造提供可复用基线。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/lib/__tests__/schemas.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/lib/__tests__/errors.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增 auth request/response 与 success envelope schema |
| `backend/src/lib/errors.ts` | 修改 | 新增细粒度认证错误码、状态码映射与工厂方法 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补充 auth schema 成功/失败用例 |
| `backend/src/lib/__tests__/errors.test.ts` | 修改 | 补充认证错误码与响应格式断言 |

### Step 2：实现 `AuthService`、auth repository 与数据映射测试

- **关联测试**：T-02
- **目标文件**：`backend/src/services/auth/auth.service.ts`、`backend/src/services/auth/auth.service.test.ts`、`backend/src/repositories/interfaces/auth-profile.repository.interface.ts`、`backend/src/repositories/supabase/auth-profile.supabase.repository.ts`、`backend/src/repositories/supabase/user.supabase.repository.ts`
- **实现内容**：
  1. 新增 `AuthService`，封装 OTP 发送、验证码建会话、refresh、当前用户查询与 logout，统一完成 Supabase Auth 返回到 `AuthSession` / `AuthUser` 的数据转换，并在 service 层集中处理 provider 错误到业务错误码的映射。
  2. 新增 auth profile repository 接口与 Supabase 实现，收敛 profile/user 摘要查询逻辑；如现有 `UserSupabaseRepository` 已能复用部分能力，则抽取或扩展为按 auth user id 查询当前用户摘要，避免 route/service 直接拼装数据。
  3. 在 `backend/src/services/auth/auth.service.test.ts` 中使用 mock Supabase client 与 mock repository 补齐主路径与边界：OTP 成功、验证码错误/过期、refresh 失效、profile 缺失、自动注册首登 `isNewUser=true`、老用户 `isNewUser=false`、logout 忽略“会话已不存在”类上游反馈。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/services/auth/auth.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/auth/auth.service.ts` | 新增 | 认证主服务与 Supabase/AuthUser/AuthSession 映射 |
| `backend/src/services/auth/auth.service.test.ts` | 新增 | 覆盖 OTP、createSession、refresh、getCurrentUser、logout |
| `backend/src/repositories/interfaces/auth-profile.repository.interface.ts` | 新增 | 定义 auth user 摘要查询 contract |
| `backend/src/repositories/supabase/auth-profile.supabase.repository.ts` | 新增 | 实现 profile/auth user 聚合读取 |
| `backend/src/repositories/supabase/user.supabase.repository.ts` | 修改 | 抽取或补充当前用户摘要查询复用逻辑 |

### Step 3：重构 auth middleware，接入 `verifyJwt`、`requireAuthContext` 与 `resolveOptionalAuthContext`

- **关联测试**：T-03
- **目标文件**：`backend/src/middleware/auth.ts`、`backend/src/middleware/__tests__/auth.test.ts`
- **实现内容**：
  1. 在现有 `backend/src/middleware/auth.ts` 基础上保留 `extractBearerToken` 与真实 `verifyJwt()` 能力，但去除移动端正式链路对 `x-user-id` / Bearer 原文即 userId 的依赖，新增 `requireAuthContext(handler)`，统一在受保护 route 中注入 `request.auth`。
  2. 新增 `resolveOptionalAuthContext(request)` helper，用于 `DELETE /api/auth/session` 和公开但可识别登录态的接口；缺失/无效 token 时返回空上下文而不是直接 401，以满足 logout 幂等与 rankings 匿名降级需求。
  3. 新建或扩展 `backend/src/middleware/__tests__/auth.test.ts`，覆盖 `verifyJwt` 成功/失败、未知 role 回落 viewer、`requireAuthContext` 返回 `AUTH_UNAUTHORIZED`、`resolveOptionalAuthContext` 在无效 token 下返回匿名上下文，以及 `getAuth(request)` 的注入读取行为。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/middleware/__tests__/auth.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/middleware/auth.ts` | 修改 | 引入真实 JWT contract、requireAuthContext、resolveOptionalAuthContext |
| `backend/src/middleware/__tests__/auth.test.ts` | 新增 | 覆盖 middleware auth contract 与 request.auth 注入 |

### Step 4：新增移动端认证 route，并以 route tests 固定成功/失败响应 contract

- **关联测试**：T-04、T-05
- **目标文件**：`backend/src/app/api/auth/otp-requests/route.ts`、`backend/src/app/api/auth/sessions/route.ts`、`backend/src/app/api/auth/session-refreshes/route.ts`、`backend/src/app/api/auth/session/route.ts`、`backend/src/app/api/users/me/route.ts`、`backend/src/app/api/__tests__/auth-otp-requests.test.ts`、`backend/src/app/api/__tests__/auth-sessions.test.ts`、`backend/src/app/api/__tests__/auth-session-refreshes.test.ts`、`backend/src/app/api/__tests__/auth-session.test.ts`、`backend/src/app/api/__tests__/users-me.test.ts`
- **实现内容**：
  1. 新增 `/api/auth/otp-requests`、`/api/auth/sessions`、`/api/auth/session-refreshes` 三个 route，统一使用 Zod schema parse + `AuthService`，成功返回 `{ code: 0, data, message: 'ok' }`，失败通过 `withErrorHandler` 走统一错误结构。
  2. 新增 `GET /api/users/me`，接入 `requireAuthContext` 与 `AuthService.getCurrentUser()`，明确未授权失败时的 `AUTH_UNAUTHORIZED` contract；同时新增 `DELETE /api/auth/session`，通过 `resolveOptionalAuthContext` 与 bearer 提取实现幂等 logout，保证 token 缺失、失效、当前服务端无会话时仍返回成功 envelope。
  3. 为每个 route 新增独立 tests，覆盖请求参数校验、service 正常返回、细粒度认证错误透传、logout 幂等、`me` 未授权，以及 route 层是否按统一 envelope 与错误 body 响应。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/auth-otp-requests.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/auth-sessions.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/auth-session-refreshes.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/auth-session.test.ts`
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/users-me.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/auth/otp-requests/route.ts` | 新增 | 发送验证码接口 |
| `backend/src/app/api/auth/sessions/route.ts` | 新增 | 验证码创建会话接口 |
| `backend/src/app/api/auth/session-refreshes/route.ts` | 新增 | refresh token 换发接口 |
| `backend/src/app/api/auth/session/route.ts` | 新增 | 幂等 logout 接口 |
| `backend/src/app/api/users/me/route.ts` | 新增 | 当前用户摘要接口 |
| `backend/src/app/api/__tests__/auth-otp-requests.test.ts` | 新增 | 发送验证码 route tests |
| `backend/src/app/api/__tests__/auth-sessions.test.ts` | 新增 | 建会话 route tests |
| `backend/src/app/api/__tests__/auth-session-refreshes.test.ts` | 新增 | refresh route tests |
| `backend/src/app/api/__tests__/auth-session.test.ts` | 新增 | logout 幂等 route tests |
| `backend/src/app/api/__tests__/users-me.test.ts` | 新增 | me route tests |

### Step 5：改造 rankings 可选 auth 语义，完成真实 Bearer 可选登录态接入

- **关联测试**：T-06
- **目标文件**：`backend/src/app/api/dramas/rankings/route.ts`、`backend/src/app/api/__tests__/dramas-rankings.test.ts`、`backend/src/services/drama/drama.service.ts`、`backend/src/repositories/interfaces/drama.repository.interface.ts`
- **实现内容**：
  1. 将 `backend/src/app/api/dramas/rankings/route.ts` 从 `getOptionalUserId()` 迁移到 `resolveOptionalAuthContext()`，只在解析到真实 JWT 后向 service 传递 `authContext`，不再接受 `x-user-id` 作为移动端正式鉴权来源。
  2. 检查 `DramaService.listRankings()` 与 repository interface 的可选 auth 入参是否已满足需求；若类型定义仍是弱语义，则收紧到统一 `AuthContext`，确保 `is_booked` 个性化能力与新 middleware 保持一致。
  3. 更新 `backend/src/app/api/__tests__/dramas-rankings.test.ts`，从旧 header 占位场景改为覆盖匿名访问、有效 Bearer 透传 authContext、无效 Bearer 降级匿名访问，保证公开榜单接口在 auth 改造后不破坏既有只读能力。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test -- src/app/api/__tests__/dramas-rankings.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/dramas/rankings/route.ts` | 修改 | 迁移到 resolveOptionalAuthContext |
| `backend/src/app/api/__tests__/dramas-rankings.test.ts` | 修改 | 覆盖匿名/有效 Bearer/无效 Bearer 场景 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 对齐 rankings 可选 auth context 类型 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 统一 rankings auth context contract |

### Step 6：执行后端全量验证，确认 build/test/lint 命令口径

- **关联测试**：T-01、T-02、T-03、T-04、T-05、T-06
- **目标文件**：`backend/package.json`、`docs/specs/2026-07-28-prd-08-login/plan-backend.md`
- **实现内容**：
  1. 基于现有 `backend/package.json` 真实脚本，使用 `npm run test`、`npm run build`、`npm run lint` 作为推荐的全量验证命令，不额外臆造仓库不存在的命令。
  2. 在功能步骤完成后执行全量回归，重点确认新 auth routes、middleware、schema、service、rankings tests 与现有公共 route/middleware tests 没有互相破坏。
  3. 若 lint 或 build 暴露出类型/导入/Next route 兼容性问题，在不扩散需求范围的前提下回补到对应步骤所涉文件，确保 plan 的终态可直接指导 coding 阶段落地。
- **验证方式**：
  - 在 `backend/` 下运行 `npm run test`
  - 在 `backend/` 下运行 `npm run build`
  - 在 `backend/` 下运行 `npm run lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/package.json` | 只读确认 | 验证真实可用脚本为 test/build/lint |
| `docs/specs/2026-07-28-prd-08-login/plan-backend.md` | 修改 | 固化推荐验证命令与完成口径 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 4 ──▶ Step 6
          │           ▲
          └──▶ Step 3 ┘
Step 3 ──▶ Step 5 ──▶ Step 6
```

## 验证总览

- [ ] 所有测试通过（在 `backend/` 下运行 `npm run test`）
- [ ] Build 成功（在 `backend/` 下运行 `npm run build`）
- [ ] 无新增 lint 错误（在 `backend/` 下运行 `npm run lint`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 补充 auth request/response schema 与 envelope |
| `backend/src/lib/errors.ts` | 修改 | 接入细粒度认证错误码与状态映射 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | auth schema 测试 |
| `backend/src/lib/__tests__/errors.test.ts` | 修改 | auth error code 测试 |
| `backend/src/services/auth/auth.service.ts` | 新增 | 认证业务服务 |
| `backend/src/services/auth/auth.service.test.ts` | 新增 | 认证 service 单元测试 |
| `backend/src/repositories/interfaces/auth-profile.repository.interface.ts` | 新增 | auth profile repository contract |
| `backend/src/repositories/supabase/auth-profile.supabase.repository.ts` | 新增 | auth profile repository Supabase 实现 |
| `backend/src/repositories/supabase/user.supabase.repository.ts` | 修改 | 复用或抽取当前用户摘要查询 |
| `backend/src/middleware/auth.ts` | 修改 | 真实 JWT、requireAuthContext、resolveOptionalAuthContext |
| `backend/src/middleware/__tests__/auth.test.ts` | 新增 | auth middleware 测试 |
| `backend/src/app/api/auth/otp-requests/route.ts` | 新增 | 发送验证码 route |
| `backend/src/app/api/auth/sessions/route.ts` | 新增 | 创建会话 route |
| `backend/src/app/api/auth/session-refreshes/route.ts` | 新增 | 刷新会话 route |
| `backend/src/app/api/auth/session/route.ts` | 新增 | 幂等 logout route |
| `backend/src/app/api/users/me/route.ts` | 新增 | 当前用户 route |
| `backend/src/app/api/__tests__/auth-otp-requests.test.ts` | 新增 | OTP 请求 route tests |
| `backend/src/app/api/__tests__/auth-sessions.test.ts` | 新增 | 创建会话 route tests |
| `backend/src/app/api/__tests__/auth-session-refreshes.test.ts` | 新增 | refresh route tests |
| `backend/src/app/api/__tests__/auth-session.test.ts` | 新增 | logout route tests |
| `backend/src/app/api/__tests__/users-me.test.ts` | 新增 | me route tests |
| `backend/src/app/api/dramas/rankings/route.ts` | 修改 | rankings 可选 auth 改造 |
| `backend/src/app/api/__tests__/dramas-rankings.test.ts` | 修改 | rankings auth contract tests |
| `backend/src/services/drama/drama.service.ts` | 修改 | 对齐 rankings 可选 auth context 类型 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 统一 rankings auth context contract |
