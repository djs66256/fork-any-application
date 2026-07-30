# 个人资产管理 (User Assets)

> 最后更新：2026-07-30
> 覆盖端：Web / Android / iOS / Backend（Web 本期 skipped）

## 功能概述

PRD-11 首版把移动端菜单中的“我的预约”从占位承接升级为真实 Native 资产页，并新增受保护的 `GET /api/users/me/bookings` 作为预约资产读取入口；“我的下载”继续保留为占位页，不新增下载 API 或真实下载数据（`backend/src/app/api/users/me/bookings/route.ts:10-29`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt:95-105`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:34-38`、`docs/specs/2026-07-30-prd-11-user-assets/design-web.md:12-15`）。

- **核心价值**：把排行预约写入能力补成“可再次查看的个人资产”闭环，让用户能在菜单内查看已预约内容，并区分“已上线 / 待上线”两类状态（`backend/src/repositories/supabase/drama.supabase.repository.ts:526-578`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingStatusTabs.kt:10-26`、`ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/BookingAssetsTabBar.swift:8-19`）。
- **覆盖范围**：Backend 新增预约资产读取 contract；Android / iOS 接入菜单入口、登录承接、双 Tab、summary、空态 / 错误态 / 分页；Web 明确 skipped（`backend/src/lib/schemas.ts:348-379`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/BookingAssetsScreen.kt:48-170`、`ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift:17-126`、`docs/specs/2026-07-30-prd-11-user-assets/design-web.md:6-15`）。
- **当前状态**：预约资产读取、菜单承接、登录后回 booking route、双 Tab summary 与下载占位策略都已落地；设备 / 模拟器黑盒执行仍未完成，当前证据以代码、自动化测试与 QA 用例设计为准（`backend/src/app/api/__tests__/users-me-bookings.test.ts:55-83`、`backend/src/app/api/__tests__/users-me-bookings.test.ts:116-150`、`backend/src/app/api/__tests__/users-me-bookings.test.ts:198-251`、`docs/specs/2026-07-30-prd-11-user-assets/qa-test.md:30-35`、`docs/specs/2026-07-30-prd-11-user-assets/qa-test.md:75-115`）。

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Web | 本期不提供用户端资产页 | skipped，不新增 `/assets`、`/bookings`、`/downloads` 等页面 | `docs/specs/2026-07-30-prd-11-user-assets/design-web.md:12-15`、`docs/specs/2026-07-30-prd-11-user-assets/design-web.md:30-37` |
| Android | 菜单“我的预约” | `PendingRoute.MenuBooking` → `AppDestination.menuBooking()` | `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt:95-100`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:153-156` |
| Android | 菜单“我的下载” | `PendingRoute.MenuDownloads` → `AppDestination.menuDownloads()` 占位页 | `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt:101-105`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:157-159`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:691-706` |
| iOS | 菜单“我的预约” | `router.closeMenuPanelThenNavigate(to: .bookingAssets)`，公开路由名 `menu/booking` | `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:34-36`、`ios/ShortDrama/Sources/App/AppRoute.swift:13-15`、`ios/ShortDrama/Sources/App/AppRoute.swift:73-75` |
| iOS | 菜单“我的下载” | `router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .downloads))` | `ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:37-38` |
| Backend | 当前用户预约资产列表 | `GET /api/users/me/bookings` | `backend/src/app/api/users/me/bookings/route.ts:10-29` |

## 核心逻辑

### 流程：菜单进入“我的预约”，而“我的下载”继续占位

1. Android 菜单静态区同时暴露“我的预约”和“我的下载”，但两者指向不同语义：预约进入真实 booking route，下载仍进入 placeholder route（`android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt:95-105`）。
2. Android 应用壳沿用“先关菜单再导航”机制：`MainNavigationViewModel.closeMenuThenNavigate()` 先把目标路由写入 `pendingMenuRoute` 并关闭菜单，`NavGraph` 在动画结束后再消费 `PendingRoute.MenuBooking` 或 `PendingRoute.MenuDownloads`（`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:86-116`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:153-159`）。
3. iOS 菜单也先通过 `closeMenuPanelThenNavigate(to:)` 关闭 overlay，再在 `markMenuPanelDidClose()` 时执行 `.bookingAssets` 或 `.menuPlaceholder(kind: .downloads)` 真正跳转（`ios/ShortDrama/Sources/App/NavigationRouter.swift:131-157`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:34-38`）。
4. iOS `TabNavigationHostView` 已把 `.bookingAssets` 绑定到 `BookingAssetsView`，因此“我的预约”不再走旧 placeholder；“我的下载”仍由 `MenuPlaceholderView` 承接（`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:12-27`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPlaceholderView.swift:3-26`）。

### 流程：Backend 返回 `{ data, pagination, summary }` 的预约资产列表

1. Route 使用 `requireAuthContext(...)` 保护 `GET /api/users/me/bookings`，先以 `BookingAssetQuerySchema` 解析 `status/page/pageSize`，再通过 `getAuth(request)` 读取当前用户并调用 `DramaService.listUserBookings()`（`backend/src/app/api/users/me/bookings/route.ts:10-29`、`backend/src/lib/schemas.ts:351-379`）。
2. 共享 schema 固定了首版 contract：`status` 只允许 `online/upcoming`，默认值为 `online`；分页默认 `page=1`、`pageSize=20`；成功响应固定包含 `data`、snake_case 的 `pagination` 和 `summary`（`backend/src/lib/schemas.ts:348-379`）。
3. Service 不在业务层重写字段，而是只校验 repository 结果是否符合 `BookingAssetListResponseSchema`；若 repository 返回非法结构，则统一包装为 `INTERNAL_ERROR`（`backend/src/services/drama/drama.service.ts:112-120`）。
4. Supabase repository 先全量查询当前用户 booking 生成 summary，再按当前 Tab 状态查询分页列表；分页查询按 `created_at DESC`、`drama_id DESC` 排序，并继续返回 `page / page_size / total / total_pages`（`backend/src/repositories/supabase/drama.supabase.repository.ts:526-578`）。
5. 状态映射由服务端统一收口：`announced -> upcoming`，`ongoing/completed -> online`；无效 join 行会在 `parseBookingAssetRows()` 中被丢弃，未知 `dramas.status` 会告警并从 summary / data 中排除（`backend/src/repositories/supabase/drama.supabase.repository.ts:287-377`）。
6. 自动化测试已覆盖默认 query、显式 query、401、400、超大页码空列表、503 与 500 场景，因此当前 contract 不是设计态而是已有实现（`backend/src/app/api/__tests__/users-me-bookings.test.ts:55-83`、`backend/src/app/api/__tests__/users-me-bookings.test.ts:85-114`、`backend/src/app/api/__tests__/users-me-bookings.test.ts:116-150`、`backend/src/app/api/__tests__/users-me-bookings.test.ts:152-196`、`backend/src/app/api/__tests__/users-me-bookings.test.ts:198-251`）。

### 流程：匿名登录承接，并在登录后回到 booking route

1. Android `BookingAssetsViewModel` 在匿名或过期状态下展示登录门槛，并在用户点击登录时发出 `RequireLogin(AppDestination.menuBooking())`，由 `NavGraph` 统一跳转到 `login?returnRoute=menu/booking&source=menu_booking`（`android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt:144-148`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:313-324`）。
2. Android 登录成功后，`LoginViewModel.resolveSuccessRoute()` 会保留非空且非 `login/settings` 的 returnRoute，因此 `menu/booking` 会被原样恢复，而不是退回 profile（`android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:218-225`）。
3. iOS `BookingAssetsView` 在 `authStore.status` 为 `.anonymous` 或 `.expired` 时展示 `BookingAssetsLoginGateView`，点击登录后通过 `BookingAssetsRouteBuilder.loginContext()` 构造 `source = .bookingAssets`、`returnRoute = .bookingAssets` 的统一登录上下文（`ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift:17-37`、`ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift:113-126`、`ios/ShortDrama/Sources/Features/BookingAssets/BookingAssetsRouteBuilder.swift:3-9`）。
4. iOS 登录页由 `AppShellView.fullScreenCover(item: presentedLoginContext)` 统一承载；登录成功时调用 `router.completeLogin()`，其中 `.bookingAssets` 会被保留或重新导航回 booking route，而不会被覆盖到其它页面（`ios/ShortDrama/Sources/App/AppShellView.swift:31-48`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:191-230`）。
5. 两端在拿到有效登录态后都会自动刷新首屏：Android 监听 `AuthStatus.Authenticated` 后调用 `refresh(status = selectedStatus, isRetry = false)`；iOS 在 access token 变化时执行 `loadIfNeeded()`，并在 401 时重新退回登录门槛（`android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt:150-191`、`ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift:32-37`、`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:38-57`、`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:145-189`）。

### 流程：双 Tab 与 summary 由服务端统一驱动

1. Android Tab 文案直接渲染 `status.label + summary.countFor(status)`，并把当前选中值写回 `BookingAssetsUiState.selectedStatus`；刷新成功和追加成功都会用服务端返回的 `page.summary` 覆盖本地 summary（`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingStatusTabs.kt:10-26`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetSummary.kt:3-11`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt:254-299`）。
2. iOS Tab 同样直接渲染 `status.title + summary.count(for: status)`，而 `BookingAssetsViewModel` 在首屏加载和分页追加时都把 `response.summary` 作为唯一摘要来源，不在客户端自行重算（`ios/ShortDrama/Sources/Features/BookingAssets/Views/Components/BookingAssetsTabBar.swift:8-19`、`ios/ShortDrama/Sources/Domain/Entities/BookingAssetSummary.swift:3-17`、`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:75-103`、`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:137-149`）。
3. Backend summary 与列表使用同一用户口径，但 summary 会基于全量有效 booking 聚合，而列表只返回当前 Tab 分页切片，因此超大页码仍应返回空 `data` 且保持正确 `summary/pagination`（`backend/src/repositories/supabase/drama.supabase.repository.ts:531-578`、`backend/src/app/api/__tests__/users-me-bookings.test.ts:152-196`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 未登录访问预约资产页 | 返回登录门槛，而不是直接暴露 401 原始错误页 | `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/BookingAssetsScreen.kt:102-145`、`ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift:19-26` |
| 登录态过期 | Android 将 `authGate` 切回 `Expired`；iOS `isUnauthorized(_:)` 后调用 `resetForLoginGate()` | `android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt:311-321`、`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:150-189` |
| 分页追加失败 | 两端都保留已有列表，只暴露 append error，不清空已成功数据 | `android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt:287-308`、`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:75-103` |
| 脏 join / 未知状态 | Backend 丢弃非法行，未知 `dramas.status` 仅告警不透出给客户端 | `backend/src/repositories/supabase/drama.supabase.repository.ts:308-377` |
| 我的下载 | Android / iOS 都继续进入 placeholder，不新增下载接口 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:691-706`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:37-38` |
| Web 范围 | 明确 skipped，不新增 Web 用户端资产页或 booking API client | `docs/specs/2026-07-30-prd-11-user-assets/design-web.md:12-15`、`docs/specs/2026-07-30-prd-11-user-assets/design-web.md:30-37` |
| QA 执行 | 当前只完成黑盒用例设计，设备 / 模拟器执行仍阻塞 | `docs/specs/2026-07-30-prd-11-user-assets/qa-test.md:30-35`、`docs/specs/2026-07-30-prd-11-user-assets/qa-test.md:75-115` |

## 多端实现

### Web

- 本期不新增用户端 booking / downloads 页面，也不接入 `GET /api/users/me/bookings`；Web 只在方案文档中显式记录 skipped 边界（`docs/specs/2026-07-30-prd-11-user-assets/design-web.md:12-15`、`docs/specs/2026-07-30-prd-11-user-assets/design-web.md:30-37`）。

### Android

- 核心入口：菜单静态项把“我的预约”指向 `PendingRoute.MenuBooking`，把“我的下载”指向 `PendingRoute.MenuDownloads`（`android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt:95-105`）。
- 壳层导航：`MainNavigationViewModel` 统一处理 close-menu-then-navigate，`NavGraph` 负责把 `menu/booking` 渲染为 `BookingAssetsScreen` 并把登录回流源标记为 `menu_booking`（`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:86-116`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:153-159`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:313-324`）。
- 列表与状态：`BookingAssetsScreen` 负责顶部标题、Tab、列表、空态、错误态、登录门槛与分页触底；`BookingAssetsViewModel` 负责 summary、首屏刷新、分页追加、401 回登录门槛和错误文案映射（`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/BookingAssetsScreen.kt:48-170`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt:32-360`）。
- 与 Backend 交互：`ApiService.getUserBookings()` 通过 RESTful `GET users/me/bookings` 获取数据，`BookingAssetStatus` / `BookingAssetSummary` 保持与服务端状态语义一致（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:90-96`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetStatus.kt:3-16`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetSummary.kt:3-11`）。

### iOS

- 核心入口：菜单面板把“我的预约”指向 `.bookingAssets`，把“我的下载”指向 `.menuPlaceholder(kind: .downloads)`（`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:34-38`）。
- 壳层导航：`AppRoute.bookingAssets` 对外公开路由名 `menu/booking`，`NavigationRouter` 负责菜单关闭后导航与登录完成后的 route 恢复，`TabNavigationHostView` 再把 `.bookingAssets` 渲染为 `BookingAssetsView`（`ios/ShortDrama/Sources/App/AppRoute.swift:13-15`、`ios/ShortDrama/Sources/App/AppRoute.swift:73-75`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:131-230`、`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:12-27`）。
- 列表与状态：`BookingAssetsView` 根据 `authStore.status` 在 loading / login gate / authenticated content 间切换；`BookingAssetsViewModel` 持有 `selectedStatus`、`summary`、分页状态和 401 回登录门槛逻辑（`ios/ShortDrama/Sources/Features/BookingAssets/Views/BookingAssetsView.swift:17-126`、`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:4-210`）。
- 与 Backend 交互：`GetUserBookingsEndpoint` 以 `Authorization: Bearer <accessToken>` 调用 `/api/users/me/bookings`，查询参数继续使用 `status/page/pageSize`（`ios/ShortDrama/Sources/Data/DataSources/DramaRemoteDataSource.swift:119-137`）。

### Backend

- Route 层：`GET /api/users/me/bookings` 由 Next.js Route Handler 暴露，并继续沿用 `withErrorHandler` + `requireAuthContext` 的受保护接口模式（`backend/src/app/api/users/me/bookings/route.ts:1-29`）。
- Shared contract：`BookingAssetAvailabilityStatusSchema`、`BookingAssetQuerySchema`、`BookingAssetSchema`、`BookingAssetSummarySchema`、`BookingAssetListResponseSchema` 定义了预约资产响应结构（`backend/src/lib/schemas.ts:348-379`）。
- Service / Repository 分层：`DramaService.listUserBookings()` 只做 schema 校验；`DramaSupabaseRepository.listUserBookings()` 负责 `bookings JOIN dramas`、状态映射、分页与 summary 聚合（`backend/src/services/drama/drama.service.ts:112-120`、`backend/src/repositories/supabase/drama.supabase.repository.ts:526-578`）。

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/users/me/bookings` | [../../api/user-assets.md](../../api/user-assets.md) | 当前用户预约资产分页列表与 `summary` 摘要 |
| `POST /api/dramas/:id/book` | [../../api/dramas.md](../../api/dramas.md) | 预约写入入口，资产页读取的上游来源 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `BookingAssetsUiState` | `MutableStateFlow` | 页面级 | 持有当前 Tab、summary、items、分页、loading / refresh / append / login gate 状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt:37-50`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt:197-321` |
| Android 登录回流目标 | route 参数 `returnRoute=menu/booking` | 页面级 | 登录成功后保留 booking route，而不是回到 profile 默认页 | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:313-324`、`android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt:218-225` |
| iOS `selectedStatus` / `summary` / `viewState` | `@Published` | 页面级 | 控制当前 Tab、摘要、首屏 / 空态 / 错误态与分页 | `ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:18-33`、`ios/ShortDrama/Sources/Features/BookingAssets/ViewModels/BookingAssetsViewModel.swift:137-182` |
| iOS 登录回流上下文 | `presentedLoginContext` | 应用级 | 保存 `.bookingAssets` returnRoute，并由 `completeLogin()` 恢复 | `ios/ShortDrama/Sources/Features/BookingAssets/BookingAssetsRouteBuilder.swift:3-9`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:183-230` |
| Backend booking query / summary | request query + repository 聚合 | 请求级 | query 默认值和 `summary` 聚合都在服务端统一收口，客户端不自行重算 | `backend/src/lib/schemas.ts:351-379`、`backend/src/repositories/supabase/drama.supabase.repository.ts:531-578` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 菜单入口 + 关闭后导航 | “我的预约 / 我的下载”都由首页菜单壳层承接，先关菜单再导航 |
| 认证体系 | 登录门槛与登录回流 | 匿名进入 booking 页时复用统一登录页 / 登录弹层，并在成功后恢复 booking route |
| 排行体系 | 写入来源 | 现有预约写操作仍来自 `POST /api/dramas/:id/book`，资产页只新增读取闭环 |
| 数据模型 | 共享 booking assets contract | Android / iOS DTO 和 Backend Zod schema 需共享 `online/upcoming` 与 `summary` 语义 |

### 外部依赖

| 服务 | 用途 | 接入方式 |
|------|------|---------|
| Supabase | 查询 `bookings` 与 `dramas` 关联数据 | Backend `DramaSupabaseRepository` 通过 `getSupabaseAdmin()` 读取 |
| RESTful API | 为移动端资产页提供分页与摘要数据 | Android Retrofit / iOS URLSession APIClient 调用 `/api/users/me/bookings` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 本期 skipped | Web 用户端不提供“我的预约 / 我的下载”真实页面 | 2026-07-30 | 范围依据见 `design-web.md`（`docs/specs/2026-07-30-prd-11-user-assets/design-web.md:12-15`） |
| 我的下载仍是占位页 | 用户只能看到功能开发中文案，不能查看真实下载资产 | 2026-07-30 | Android / iOS 都未新增下载 API（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:691-706`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:37-38`） |
| iOS 仍保留旧的 `.booking` placeholder enum case | 代码中仍存在历史残留枚举，但菜单已不再使用它 | 2026-07-30 | 当前真实入口已切到 `.bookingAssets`（`ios/ShortDrama/Sources/Domain/Entities/MenuPlaceholderKind.swift:3-47`、`ios/ShortDrama/Sources/Features/MenuPanel/Views/MenuPanelContainerView.swift:34-38`） |
| 设备 / 模拟器黑盒测试未执行 | 当前 wiki 结论主要来自代码、自动化测试与 QA 用例设计 | 2026-07-30 | QA 文档已据实标记阻塞（`docs/specs/2026-07-30-prd-11-user-assets/qa-test.md:30-35`、`docs/specs/2026-07-30-prd-11-user-assets/qa-test.md:75-115`） |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-30 | 初始创建：收录 PRD-11 个人资产管理首版，覆盖预约资产读取 API、Android / iOS 菜单进入真实 booking 页、登录承接回流、双 Tab summary 与“我的下载”占位策略 |

---

*本文档由 llm-wiki skill 自动维护。*