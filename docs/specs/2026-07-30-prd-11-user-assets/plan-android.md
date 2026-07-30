# 实现计划：Android — PRD-11 个人资产管理

> 创建日期：2026-07-30
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端本期目标是把菜单中的 `menu/booking` 从占位承接页升级为真实“我的预约”页面，复用现有 Compose + Navigation Compose + Hilt + ViewModel + Repository 分层，接通受保护的预约资产接口、登录承接与登录后回流，同时保留 `menu/downloads` 为统一占位页。实现按轻量 TDD 推进：先锁定 API/鉴权与状态机测试，再落 ViewModel、页面装配、导航接线和回归测试，确保 `summary` 只读服务端、Tab 切换不串页、401 能退回登录承接态。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 端每个关键场景都补齐 `src/test/` 单元测试。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | `GET /api/users/me/bookings` 的 API / DTO / repository contract 正确 | `status=online|upcoming`、`page=1`、`pageSize=20`，服务端返回 `{ data, pagination, summary }` | `ApiService` query 命名正确；DTO 能解析 snake_case 字段；repository 正确映射为 `BookingAssetsPage`、`BookingAssetSummary` 与条目列表 | 单元测试 | P0 |
| T-02 | `AuthInterceptor.requiresAuth()` 将 booking 列入受保护白名单 | 请求 `https://example.com/api/users/me/bookings?...`，本地有/无 access token | booking 请求自动携带 Bearer token；无 token 时保持原请求；匿名接口仍不加鉴权头 | 单元测试 | P0 |
| T-03 | `BookingAssetsViewModel` 正确处理登录态、首屏加载与错误回退 | `AuthStateHolder` 依次给出 `Restoring / Anonymous / Authenticated`，接口返回成功、空列表、401、5xx | restoring 时先 loading；匿名时显示登录承接态且不发请求；登录后拉首屏；401 清空旧数据并退回登录承接态；5xx 进入可重试错误态 | 单元测试 | P0 |
| T-04 | `BookingAssetsViewModel` 的 Tab 切换、分页和防乱序稳定 | 连续切换 `ONLINE/UPCOMING`、旧请求晚返回、下一页失败 | 只消费最后一次有效请求结果；`summary` 直接使用服务端返回；append 失败仅显示 footer 错误，不清空已有内容 | 单元测试 | P0 |
| T-05 | `menu/booking` 升级为真实页且仍遵守菜单关闭后导航 | 从菜单点击“我的预约”，`PendingRoute.MenuBooking` 入队并在关闭动画后消费 | `NavGraph` 跳到真实 `BookingAssetsScreen`；`menu/downloads` 仍进入 `PlaceholderScreen`；菜单返回语义不变 | 单元测试 | P0 |
| T-06 | 登录承接与回流保持 booking 上下文 | 匿名用户在 booking 页点击登录，`returnRoute=menu/booking`，登录成功/取消 | 发出 `RequireLogin(AppDestination.menuBooking())`；登录成功回到 `menu/booking`；取消登录后仍停留 booking 登录承接态 | 单元测试 | P0 |

## 实现步骤

### Step 1：先锁定预约资产接口 contract 与鉴权白名单

- **关联测试**：T-01、T-02
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetDto.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetSummaryDto.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetsResponseDto.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAsset.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetStatus.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetSummary.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetsPage.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetsQuery.kt`、`android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/core/network/AuthInterceptorTest.kt`
- **实现内容**：
  1. 先在 `ApiServiceTest.kt` 中补 T-01，锁定 `GET users/me/bookings` 的 path 与 `status/page/pageSize` query 命名，避免后续 contract 漂移。
  2. 在 `ApiService.kt` 新增 `getUserBookings(status, page, pageSize)`，返回新的 `BookingAssetsResponseDto`，与 shared/backend design 的 `{ data, pagination, summary }` 对齐。
  3. 新增 booking 相关 DTO 与 domain model，显式处理 `drama_id`、`cover_url`、`episode_count`、`booked_at`、`availability_status`、`online_count`、`upcoming_count` 等字段。
  4. 在 `AuthInterceptorTest.kt` 中补 T-02，并修改 `AuthInterceptor.requiresAuth()`，把 `users/me/bookings` 纳入受保护白名单，确保 booking 接口走统一 Bearer token 注入。
  5. 保持匿名接口与现有登录接口不受影响，不在页面层手动拼接 Authorization header。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.ApiServiceTest" --tests "com.djs66256.short_drama.core.network.AuthInterceptorTest"` 确认 T-01、T-02 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增用户预约资产接口定义 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 修改 | 把 `users/me/bookings` 纳入 `requiresAuth()` 白名单 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetDto.kt` | 新增 | 定义预约资产条目 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetSummaryDto.kt` | 新增 | 定义双 Tab 摘要 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetsResponseDto.kt` | 新增 | 定义 booking 列表响应 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAsset.kt` | 新增 | 定义预约资产领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetStatus.kt` | 新增 | 定义 `ONLINE/UPCOMING` 状态枚举 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetSummary.kt` | 新增 | 定义摘要领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetsPage.kt` | 新增 | 定义列表聚合领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetsQuery.kt` | 新增 | 定义查询条件模型 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 新增 booking 接口 contract 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthInterceptorTest.kt` | 修改 | 新增 booking 鉴权白名单测试 |

### Step 2：打通 Booking repository / data source / use case 映射链路

- **关联测试**：T-01
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt`、`android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt`、`android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetBookingAssetsUseCase.kt`、`android/app/src/test/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSourceTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/data/repository/DramaRepositoryImplTest.kt`
- **实现内容**：
  1. 先在 `DramaRemoteDataSourceTest.kt`、`DramaRepositoryImplTest.kt` 中补 T-01，锁定 booking 响应的 success 映射、错误透传与 `summary` 不被本地重算。
  2. 在 `DramaRemoteDataSource.kt` 新增 `getUserBookings(status, page, pageSize)`，继续沿用 `ApiResult.Success/Error/Exception` 包装模式。
  3. 在 `DramaRepository.kt` 扩展 `getBookingAssets(query: BookingAssetsQuery)` 契约，并由 `DramaRepositoryImpl.kt` 完成 DTO 到 domain 的映射。
  4. 新增 `GetBookingAssetsUseCase.kt`，将 booking 读取能力收口到 domain/usecase 层，供 `BookingAssetsViewModel` 直接调用。
  5. 映射逻辑只做字段转换与状态枚举收敛，不在 repository 内引入 UI 语义或本地计数逻辑。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.datasource.DramaRemoteDataSourceTest" --tests "com.djs66256.short_drama.data.repository.DramaRepositoryImplTest"` 确认 T-01 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 修改 | 新增 booking 远程读取方法 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt` | 修改 | 扩展 booking 读取仓储 contract |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 修改 | 实现 booking DTO 到 domain 映射 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetBookingAssetsUseCase.kt` | 新增 | 新增预约资产读取用例 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSourceTest.kt` | 修改 | 补 booking 数据源测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/DramaRepositoryImplTest.kt` | 修改 | 补 booking repository 映射测试 |

### Step 3：实现 `BookingAssetsViewModel` / `BookingAssetsUiState` 状态机

- **关联测试**：T-03、T-04
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/booking/model/BookingAssetsUiState.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModelTest.kt`
- **实现内容**：
  1. 先在 `BookingAssetsViewModelTest.kt` 中覆盖 restoring/anonymous/authenticated、首屏 success/empty/error、401 回登录承接态、Tab 快切防乱序、append 失败保留旧列表等核心场景。
  2. 新增 `BookingAssetsUiState.kt`，明确建模 `selectedStatus`、`summary`、`items`、`authGate`、`isLoading`、`isRefreshing`、`isAppending`、`appendErrorMessage`、`errorMessage`、`page`、`hasNextPage` 等状态。
  3. 在 `BookingAssetsViewModel.kt` 中订阅 `AuthStateHolder`，把 booking 页拆成 `Restoring / LoginGate / Loading / Success / Empty / Error / AppendError` 这些稳定状态，而不是散落在 UI 中做条件判断。
  4. 复用 ranking 已有的 request token 思路，为 refresh 与 append 分别维护请求 token，确保旧请求晚返回时不会覆盖当前 Tab。
  5. `summary` 始终使用服务端返回值；401 时清空旧用户 booking 数据并切回登录承接态；429/5xx 走友好错误提示与重试入口。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.booking.viewmodel.BookingAssetsViewModelTest"` 确认 T-03、T-04 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/model/BookingAssetsUiState.kt` | 新增 | 定义 booking 页面 UI 状态与 effect |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt` | 新增 | 实现登录承接、首屏、Tab、分页和防乱序状态机 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModelTest.kt` | 新增 | 覆盖 booking ViewModel 关键路径测试 |

### Step 4：把 `menu/booking` 升级为真实 `BookingAssetsScreen`，并收口菜单文案与路由接线

- **关联测试**：T-05
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/BookingAssetsScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingStatusTabs.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingAssetCard.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingAssetsEmptyState.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingAssetsErrorState.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingAssetsLoginGate.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntriesTest.kt`
- **实现内容**：
  1. 先在 `NavGraphTest.kt`、`MainNavigationViewModelTest.kt`、`MenuPanelStaticEntriesTest.kt` 中补 T-05，锁定 `PendingRoute.MenuBooking` 的消费时机、菜单关闭后导航语义，以及 booking/downloads 入口文案与目标路由。
  2. 新增 `BookingAssetsScreen.kt` 与必要的 UI 组件，承接顶部标题、双 Tab、列表卡片、空态、错误态、登录承接态与 append footer。
  3. 在 `NavGraph.kt` 中把 `AppDestination.menuBooking()` 从 `PlaceholderScreen` 替换为真实 `BookingAssetsScreen`；`menu/downloads` 继续保留 `PlaceholderScreen`，但文案改为统一的“功能开发中”占位表达。
  4. 保持 `PendingRoute.MenuBooking` 不变，继续通过 `MainNavigationViewModel.closeMenuThenNavigate()` 完成“先关菜单再导航”的既有壳层时序。
  5. 更新 `MenuPanelStaticEntries.kt` 中“我的预约 / 我的下载”副标题，使 booking 指向真实资产页语义，downloads 保持明确占位语义。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.NavGraphTest" --tests "com.djs66256.short_drama.navigation.MainNavigationViewModelTest" --tests "com.djs66256.short_drama.feature.menu.model.MenuPanelStaticEntriesTest"` 确认 T-05 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/BookingAssetsScreen.kt` | 新增 | 预约资产页根 Composable |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingStatusTabs.kt` | 新增 | 双 Tab 组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingAssetCard.kt` | 新增 | 预约资产卡片 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingAssetsEmptyState.kt` | 新增 | 空态组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingAssetsErrorState.kt` | 新增 | 错误态组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/BookingAssetsLoginGate.kt` | 新增 | 登录承接态组件 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 将 `menu/booking` 接入真实 booking 页面 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改/回归 | 保持菜单关闭后导航与 `PendingRoute.MenuBooking` 协作 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt` | 修改 | 收口 booking/downloads 文案 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 增加 booking 真实页与 downloads 占位回归测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | 增加菜单关闭后跳 booking 的回归测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntriesTest.kt` | 修改 | 增加 booking/downloads 文案与动作测试 |

### Step 5：收口登录承接、`menu/booking` 回流与最终回归测试

- **关联测试**：T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModelTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt`
- **实现内容**：
  1. 先在 `LoginViewModelTest.kt`、`RoutesTest.kt`、`NavGraphTest.kt` 中补 T-06，锁定 `AppDestination.menuBooking()` 作为 booking 登录回流的 canonical route。
  2. 在 `BookingAssetsScreen` 与 `NavGraph.kt` 的接线中，把匿名用户点击登录收口为 `AppDestination.login(returnRoute = AppDestination.menuBooking(), source = "menu_booking")`。
  3. 保持 `LoginViewModel.resolveSuccessRoute()` 现有安全策略不变，但补回归测试确保 `menu/booking` 不会被误判成非法 returnRoute，并能在登录成功后正确回到 booking 页面。
  4. 校验取消登录、关闭登录页时不会把用户带离 booking 上下文；登录成功后页面重新走 booking 首屏加载，而不是返回菜单或 profile。
  5. 在本步骤完成后执行 booking 相关测试回归，确认接口、鉴权、状态机、导航与登录回流串成闭环。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.auth.viewmodel.LoginViewModelTest" --tests "com.djs66256.short_drama.navigation.RoutesTest" --tests "com.djs66256.short_drama.navigation.NavGraphTest"` 确认 T-06 通过
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.booking.viewmodel.BookingAssetsViewModelTest"` 做 booking 状态机回归
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改/回归 | 固定 booking 登录回流使用 canonical route helper |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 接通 booking 登录承接与登录后返回 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt` | 修改/回归 | 保持成功回流策略并补 booking 场景稳定性 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModelTest.kt` | 修改 | 增加 `menu/booking` 回流回归测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 增加 booking 登录回流 route 断言 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 增加 booking 登录承接与返回回归测试 |

## 依赖关系

```text
Step 1（API/DTO + 鉴权白名单）
  └──▶ Step 2（DataSource/Repository/UseCase）
        └──▶ Step 3（ViewModel/UiState 状态机）
              └──▶ Step 4（BookingAssetsScreen + menu/booking 真实页）
                    └──▶ Step 5（登录回流与最终回归）
```

## 验证总览

- [ ] booking API / 鉴权 contract 通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.ApiServiceTest" --tests "com.djs66256.short_drama.core.network.AuthInterceptorTest"`）
- [ ] booking data / repository 映射通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.data.datasource.DramaRemoteDataSourceTest" --tests "com.djs66256.short_drama.data.repository.DramaRepositoryImplTest"`）
- [ ] booking ViewModel 状态机通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.booking.viewmodel.BookingAssetsViewModelTest"`）
- [ ] booking 导航与菜单回归通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.NavGraphTest" --tests "com.djs66256.short_drama.navigation.MainNavigationViewModelTest" --tests "com.djs66256.short_drama.feature.menu.model.MenuPanelStaticEntriesTest"`）
- [ ] 登录回流回归通过（`cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.auth.viewmodel.LoginViewModelTest" --tests "com.djs66256.short_drama.navigation.RoutesTest"`）
- [ ] 所有测试通过（`cd android && ./gradlew test`）
- [ ] Build 成功（`cd android && ./gradlew assembleDebug`）
- [ ] 无新增 lint 错误（`cd android && ./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增 booking 资产接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 修改 | 扩展 booking 鉴权白名单 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetDto.kt` | 新增 | booking 条目 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetSummaryDto.kt` | 新增 | booking 摘要 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/BookingAssetsResponseDto.kt` | 新增 | booking 列表 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSource.kt` | 修改 | 新增 booking 远程读取 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/DramaRepositoryImpl.kt` | 修改 | 实现 booking 映射逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAsset.kt` | 新增 | booking 领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetStatus.kt` | 新增 | booking 状态枚举 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetSummary.kt` | 新增 | booking 摘要模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetsPage.kt` | 新增 | booking 列表聚合模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/BookingAssetsQuery.kt` | 新增 | booking 查询模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/DramaRepository.kt` | 修改 | 扩展 booking contract |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetBookingAssetsUseCase.kt` | 新增 | booking 读取用例 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/model/BookingAssetsUiState.kt` | 新增 | booking 页面状态定义 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModel.kt` | 新增 | booking 状态机实现 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/BookingAssetsScreen.kt` | 新增 | booking 根页面 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/booking/ui/components/*` | 新增 | booking 组件集合 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 固定 booking 登录回流 route |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 接入真实 booking 页面与登录承接 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt` | 修改/回归 | 维持 `PendingRoute.MenuBooking` 导航协作 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt` | 修改 | 收口 booking/downloads 文案 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModel.kt` | 修改/回归 | 保持 booking 登录成功回流稳定 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | booking API contract 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthInterceptorTest.kt` | 修改 | booking 鉴权测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/datasource/DramaRemoteDataSourceTest.kt` | 修改 | booking 数据源测试 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/DramaRepositoryImplTest.kt` | 修改 | booking repository 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/booking/viewmodel/BookingAssetsViewModelTest.kt` | 新增 | booking ViewModel 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | booking 页面与登录回流回归测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | booking 菜单导航回归测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | booking route helper 测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntriesTest.kt` | 修改 | booking/downloads 菜单项测试 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/auth/viewmodel/LoginViewModelTest.kt` | 修改 | booking 登录回流测试 |