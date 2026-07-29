# 认证体系 (Auth)

> 最后更新：2026-07-29
> 覆盖端：Android / iOS / Backend / Web Admin

## 功能概述

当前仓库中的认证体系已经分成两条明显不同的实现层级：一条是 Web Admin 路径已经接通的真实 Supabase JWT + role 校验链路，另一条是移动端业务写接口仍在使用 skeleton auth 基线，只把 `x-user-id` 或 `Authorization: Bearer <user-id>` 解析为用户标识，并未做真实 JWT 验签。PRD-09 评论系统需要复用的也是后者这条移动端业务写接口基线，而不是把评论文档写成“已接通真实用户 JWT”。

- 核心价值：区分当前仓库里“真实鉴权”与“占位鉴权”两套并存基线，避免后续评论、预约等业务能力误判后端认证现状
- 覆盖范围：Backend 管理端认证、Backend 用户侧 skeleton auth、Android 登录态占位建模、iOS 登录态占位建模、Web Admin 登录入口
- 当前状态：Admin 路径已落地真实 JWT + role 校验；移动端业务写接口仍是 skeleton auth / `x-user-id` / `Bearer <user-id>` 基线；移动端客户端自身登录闭环仍未在当前 worktree 中形成真实用户 token 注入

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Web Admin | 管理后台登录 | `POST /api/admin/auth/login`，返回 Supabase access token 与用户 role | `backend/src/app/api/admin/auth/login/route.ts:8-35` |
| Web Admin | 管理接口访问保护 | `requireRole([...])` 包裹 admin routes | `backend/src/app/api/admin/stats/route.ts:7-18`, `backend/src/app/api/admin/dramas/route.ts:9-45`, `backend/src/app/api/admin/users/route.ts:7-22`, `backend/src/app/api/admin/users/[id]/role/route.ts:8-25` |
| Backend 用户侧业务接口 | 预约等需要登录的写接口 | `POST /api/dramas/:id/book` 通过 `getAuthenticatedUserId()` 读取 userId | `backend/src/app/api/dramas/[id]/book/route.ts:16-28`, `backend/src/middleware/auth.ts:17-33` |
| Backend 用户侧可选身份接口 | 排行列表按用户态返回 `is_booked` | `GET /api/dramas/rankings` 通过 `getOptionalUserId()` 解析可选 userId | `backend/src/app/api/dramas/rankings/route.ts:8-23`, `backend/src/middleware/auth.ts:17-24` |
| Android | 排行预约登录拦截占位 | `RankingEffect.RequireLogin(returnRoute)`，当前 UI 未接真实登录跳转 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:46-49,193-205`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:222-230` |
| iOS | 排行预约登录拦截占位 | `RankingLoginContext` + alert 提示，当前未接真实登录流 | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:14-16,125-159,213-220`, `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:56-67,81-110` |

## 核心逻辑

### 流程：Admin 路径真实 JWT + role 校验

1. Web Admin 登录使用 Supabase 邮箱密码登录，成功后返回 access token 和从 `app_metadata.role` 读取的角色。
   - 源文件：`backend/src/app/api/admin/auth/login/route.ts:8-35`
2. 受保护的 admin route 统一通过 `requireRole()` 包裹。
   - 源文件：`backend/src/app/api/admin/stats/route.ts:7-18`, `backend/src/app/api/admin/dramas/route.ts:9-45`, `backend/src/app/api/admin/users/route.ts:7-22`
3. `requireRole()` 会先从 `Authorization: Bearer <jwt>` 中抽取 token，再调用 Supabase Admin client 的 `auth.getUser(token)` 做真实 JWT 验证。
   - 源文件：`backend/src/middleware/auth.ts:6-15,44-69,91-115`
4. 验证通过后，中间件从 `user.app_metadata.role` 中读取角色，只接受 `admin / editor / viewer` 三种值；未知值会被降级成 `viewer`。
   - 源文件：`backend/src/middleware/auth.ts:56-64`
5. 角色符合要求时，把 `{ userId, role }` 注入到 `request.auth`，供后续 handler 用 `getAuth()` 读取。
   - 源文件：`backend/src/middleware/auth.ts:111-126`, `backend/src/app/api/admin/users/[id]/role/route.ts:15-18`
6. Supabase migration 还把 `profiles.role` 与 `auth.users.raw_app_meta_data.role` 做了同步，并通过 RLS 为 admin/editor/viewer 建立不同授权边界。
   - 源文件：`backend/supabase/migrations/20260727000400_auth_hook_role_sync.sql:3-30`, `backend/supabase/migrations/20260727000500_enable_rls.sql:13-67`

### 流程：移动端业务写接口 skeleton auth / userId 解析

1. 用户侧 helper 先尝试读取 `x-user-id` header；如果没有，再尝试把 `Authorization: Bearer ...` 中 Bearer 后的整段字符串直接当作 userId 使用。
   - 源文件：`backend/src/middleware/auth.ts:17-24`
2. `getAuthenticatedUserId()` 只是要求能解析出一个 userId，否则返回 401；它不会对 Bearer token 做真实 JWT 验证。
   - 源文件：`backend/src/middleware/auth.ts:26-33`
3. 预约接口 `POST /api/dramas/:id/book` 使用这套 helper 作为当前移动端业务写接口的真实基线。
   - 源文件：`backend/src/app/api/dramas/[id]/book/route.ts:16-28`
4. 排行列表接口 `GET /api/dramas/rankings` 则使用 `getOptionalUserId()`，可选地把 userId 透给 service，用来给榜单项补充 `is_booked`。
   - 源文件：`backend/src/app/api/dramas/rankings/route.ts:8-23`
5. 测试明确验证了：
   - `Authorization: Bearer user-1` 会被当作 userId；
   - 当 `x-user-id` 与 Authorization 同时存在时，优先 `x-user-id`；
   - header 缺失时返回 401。
   - 源文件：`backend/src/app/api/__tests__/dramas-book.test.ts:18-77`
6. PRD-09 评论文档应继续按照这套 skeleton auth 记述“当前基线”，而不是写成已经完成真实用户 JWT 升级。
   - 源文件：`docs/specs/2026-07-29-prd-09-comments/spec.md:80-92,332-365,529-534`

### 流程：移动端客户端登录态仍是占位建模

1. Android 侧确实存在 `AuthSessionProvider` 接口，但接口只有 `isLoggedIn(): Boolean`，没有 token、user profile 或 user id 暴露。
   - 源文件：`android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt:3-5`
2. 当前 Hilt 默认实现把 `isLoggedIn()` 固定返回 `false`，因此排行预约里的登录拦截只是产品语义占位。
   - 源文件：`android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt:48-52`
3. Android 的 `AuthInterceptor` 已预留未来 JWT 注入位置，但当前不会注入任何 `Authorization` header。
   - 源文件：`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt:6-21`
4. Android 当前真正落地的身份型 header 只有匿名播放会话 `X-Playback-Session-Id`，它服务于 player history，而不是登录用户认证。
   - 源文件：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:86-107`
5. iOS 排行页同样只有 `isUserLoggedIn` 闭包和 `RankingLoginContext` 提示建模，默认构造函数里该闭包默认返回 `false`。
   - 源文件：`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:31-54,117-159`
6. iOS 通用 `APIClient` 会发送 endpoint 自带 headers，但当前 Drama/Ranking endpoints 没有附加 Authorization；也没有独立 token store。
   - 源文件：`ios/ShortDrama/Sources/Core/Network/APIClient.swift:44-58`, `ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:77-117`

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| Admin 请求未携带 Bearer token | `requireRole()` 返回 401“请先登录” | `backend/src/middleware/auth.ts:91-101` |
| Admin Bearer token 无效或 Supabase 验证失败 | `verifyJwt()` 返回 null，最终由 `requireRole()` 返回 401 | `backend/src/middleware/auth.ts:44-68,91-101` |
| Admin 角色不满足访问要求 | `requireRole()` 返回 403“无权访问” | `backend/src/middleware/auth.ts:103-109` |
| 用户侧写接口既没有 `x-user-id` 也没有 Bearer | `getAuthenticatedUserId()` 抛 401 | `backend/src/middleware/auth.ts:26-33` |
| 用户侧同时传 `x-user-id` 与 Bearer | 优先使用 `x-user-id` | `backend/src/middleware/auth.ts:17-24`, `backend/src/app/api/__tests__/dramas-book.test.ts:47-57` |
| Android 排行命中登录拦截 | 发出 `RequireLogin(returnRoute)`，但当前 NavGraph 不会真正导航到登录流 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:193-205`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:227-229` |
| iOS 排行命中登录拦截 | 展示 alert，不会真正打开独立登录页 | `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:61-67,81-110` |

## 多端实现

### Web Admin

- 登录入口：`backend/src/app/api/admin/auth/login/route.ts:8-35`
- 权限保护：`backend/src/middleware/auth.ts:44-126`
- 受保护 route 示例：`backend/src/app/api/admin/dramas/route.ts:9-45`, `backend/src/app/api/admin/users/route.ts:7-22`, `backend/src/app/api/admin/users/[id]/role/route.ts:8-25`
- 特点：这是当前仓库里唯一已经接入真实 Supabase JWT 验证与 RBAC 的路径

### Android

- 登录态接口：`android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt:3-5`
- 默认实现：`android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt:48-52`
- 网络鉴权占位：`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt:6-21`
- 登录拦截建模：`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:46-49,193-205`
- 特点：客户端已有“是否登录”和“回到原榜单 route”的抽象，但未形成真实 token 注入或登录闭环

### iOS

- 登录拦截建模：`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:14-16,125-159,213-220`
- UI 承接：`ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:56-67,81-110`
- 通用网络发送：`ios/ShortDrama/Sources/Core/Network/APIClient.swift:44-58`
- 特点：当前只有结构化 `RankingLoginContext` 与 alert 提示，不会给 Drama/Ranking 请求附加真实 Authorization

### Backend

- Skeleton auth / admin auth 汇总：`backend/src/middleware/auth.ts:6-126`
- 用户模型 schema：`backend/src/lib/schemas.ts:332-410`
- Profiles repository：`backend/src/repositories/supabase/user.supabase.repository.ts:5-98`
- RLS 与 role 同步：`backend/supabase/migrations/00000000000001_init_tables.sql:73-113`, `backend/supabase/migrations/20260727000400_auth_hook_role_sync.sql:3-30`, `backend/supabase/migrations/20260727000500_enable_rls.sql:13-67`
- 特点：后端已经具备“真实 admin 鉴权”和“用户侧 skeleton auth”两条共存基线，PRD-09 需要明确复用后者

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `POST /api/admin/auth/login` | [../../api/admin.md](../../api/admin.md) | Admin 登录入口，返回真实 Supabase access token |
| `GET /api/admin/stats` | [../../api/admin.md](../../api/admin.md) | 示例 admin 受保护接口，要求真实 JWT + role |
| `GET /api/admin/users` | [../../api/admin.md](../../api/admin.md) | 示例 admin 列表接口，要求真实 JWT + role |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 当前移动端业务写接口的 skeleton auth 基线代表 |
| `GET /api/dramas/rankings` | [../../api/dramas.md](../../api/dramas.md) | 通过可选 userId 返回当前用户维度的 `is_booked` |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Backend `request.auth` | request 临时注入 | 请求级 | `requireRole()` 验签成功后写入 `{ userId, role }` | `backend/src/middleware/auth.ts:111-126` |
| Backend `AuthContext.role` | Supabase JWT `app_metadata.role` | 请求级 | Admin 路径真实权限来源 | `backend/src/middleware/auth.ts:56-64` |
| Backend 用户侧 `userId` | `x-user-id` 或 `Bearer <user-id>` | 请求级 | Skeleton auth 只解析 userId，不验签 | `backend/src/middleware/auth.ts:17-33` |
| Android `AuthSessionProvider.isLoggedIn()` | Hilt 注入接口 | 应用级 | 当前仅承载“是否登录”的占位判断，默认恒为 false | `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt:3-5`, `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt:48-52` |
| Android `RequireLogin(returnRoute)` | `SharedFlow<RankingEffect>` | 页面级 | 排行预约命中未登录时发出登录拦截上下文 | `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt:46-49,73-77,193-205` |
| iOS `routeEffect` / `RankingLoginContext` | `@Published` | 页面级 | 排行预约命中未登录时记录来源榜单与 dramaId | `ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:14-16,23-30,125-159,213-220` |
| Backend `profiles.role` | Supabase `profiles` 表 + auth hook | 持久层 | Admin / viewer / editor 角色会同步到 JWT app metadata | `backend/supabase/migrations/20260727000400_auth_hook_role_sync.sql:13-30` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 排行体系 | 登录拦截复用 | 当前移动端业务唯一成型的“需要登录动作”建模来自预约榜 |
| 评论能力 | 认证基线依赖 | PRD-09 评论写接口需明确沿用 skeleton auth，而不是误记成真实 JWT |
| 管理平台 | 真实鉴权依赖 | Admin 面板所有写接口与用户角色管理都依赖 `requireRole()` |
| 用户资料 | 展示字段来源 | `profiles` 提供 `email / display_name / avatar_url / role` 等字段 |

### 外部依赖

| 服务 | 用途 | 接入方式 |
|------|------|---------|
| Supabase Auth | Admin 登录与 JWT 验签 | `getSupabaseAdmin().auth.signInWithPassword()` / `auth.getUser(token)` |
| Supabase Postgres + RLS | 存储 profile 与角色，并执行 RLS | `profiles` 表、role 同步 trigger、RLS policies |
| OkHttp Interceptor | Android 未来 Authorization 注入扩展点 | `AuthInterceptor`（当前为空实现） |
| URLSession | iOS 业务请求承载 | `APIClient`（当前 endpoints 未附加 Authorization） |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| 移动端业务写接口仍未接真实 JWT | 评论、预约等用户写操作文档必须按 skeleton auth 记述，不能误导为生产级认证 | 2026-07-29 | `backend/src/middleware/auth.ts:17-33` |
| Android 登录态默认恒为 false | 排行登录拦截只能验证语义，无法验证真实登录回流 | 2026-07-29 | `android/app/src/main/java/com/djs66256/short_drama/core/di/AppModule.kt:48-52` |
| iOS 登录拦截只是 alert 提示 | 当前没有独立登录页或自动恢复写操作链路 | 2026-07-29 | `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift:61-67` |
| iOS/Android 客户端都未形成真实 token store | 无法从客户端代码证明存在用户 access token 持久化与自动续用 | 2026-07-29 | Android `AuthInterceptor` 为空；iOS `APIClient` 仅发送 endpoint headers |
| Backend 用户 profile 公开 route 尚未形成文档化用户接口 | 评论用户摘要当前只能从 schema / repository / migration 基线侧面确认 | 2026-07-29 | 本轮未发现独立 `/api/profile` 用户侧 route |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：补齐认证体系文档，明确区分 Admin 真实 JWT + role 校验 与移动端业务写接口 skeleton auth / `x-user-id` / `Bearer <user-id>` 两套并存基线 |

---
*本文档由 llm-wiki skill 自动维护。*