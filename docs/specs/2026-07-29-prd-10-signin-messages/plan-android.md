# 实现计划：Android — PRD-10 签到与消息系统

> 创建日期：2026-07-29
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

本次 Android 端实现延续当前仓库既有的 `Compose UI → ViewModel → UseCase → Repository → RemoteDataSource → ApiService` 分层，不新增第三方依赖。计划按轻量 TDD 推进：先补齐签到/消息的数据契约与存储，再分别接入首页签到浮层、菜单消息预览和真实消息中心页，过程中始终用 JVM 单测锁定状态流转、路由与 Retrofit contract。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 端每个场景都需要有单元测试；优先复用现有 `src/test` 下的 MockK + Turbine + JUnit4 模式。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 签到与消息 Retrofit contract 正确 | 调用 `ApiService` 反射检查 5 个新接口 | path / query / header / 返回类型与 spec 一致，`preview` 能区分 `204` 空态 | 单元测试 | P0 |
| T-02 | 签到本地存储正确生成 installationId 并记录关闭态 | 首次读取、重复读取、写入 `serverDate` 关闭态 | installationId 只生成一次且可复用；关闭态按服务端业务日持久化 | 单元测试 | P0 |
| T-03 | 签到/消息 repository 正确映射服务端结果 | success / error / exception / `204 No Content` | DTO 映射为 domain；`preview` 空态不抛错；登录态可向 check-in 与 interaction 接口透传 token | 单元测试 | P0 |
| T-04 | 首页冷启动资格与签到提交状态流转正确 | `should_show_popup`、本地关闭态、提交成功/失败、评论弹层冲突 | 仅在可展示时弹出；关闭后同业务日不再弹；提交成功立即切换为“今日已签到”；失败保留重试 | 单元测试 | P0 |
| T-05 | 菜单消息预览状态独立于最近在看 | preview 成功 / 空态 / 失败，最近在看成功或失败 | 消息预览显示真实摘要、暂无消息或降级文案；不会阻塞最近在看区块 | 单元测试 | P0 |
| T-06 | 消息中心页匿名与登录态分流正确 | anonymous / authenticated，system success，interaction success 或 error | 匿名只显示系统消息与登录门槛；登录后加载互动消息；互动错误不影响系统消息区 | 单元测试 | P0 |
| T-07 | 登录回流消息页路由正确 | `AppDestination.login(returnRoute = menu/messages, source = menu_messages)` | 登录成功后回到 `menu/messages`，不回退到占位页或 profile | 单元测试 | P1 |
| T-08 | 菜单进入消息页仍保持“先关抽屉再导航” | `PendingRoute.MenuMessages` | 关闭动画完成后才导航，返回首页根上下文且菜单保持关闭 | 单元测试 | P1 |

## 实现步骤

### Step 1：补齐签到与消息的数据契约

- **测试场景**：
  - T-01 签到与消息 Retrofit contract 正确
  - T-03 repository / DTO 映射基线可落地
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/dto/CheckInDtos.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/dto/MessageDtos.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/model/CheckInModels.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/model/MessageModels.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/data/dto/CheckInDtosTest.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/data/dto/MessageDtosTest.kt`
- **实现内容**：
  1. 按现有 `ApiService` 风格新增 5 个接口声明，统一保留 `page/pageSize` 命名；`messages/preview` 使用可显式处理 `204` 空响应的返回类型。
  2. 新增签到与消息 DTO，延续现有 `@Serializable + toDomain()` 模式，避免把服务端字段名直接泄漏到 UI 层。
  3. 新增签到与消息 domain model，收敛首页、菜单、消息中心需要的最小字段集，避免 ViewModel 直接依赖 DTO。
  4. 先补 `ApiServiceTest` 与 DTO 映射测试，再落接口与 model，确保 contract 与 spec/design 一致。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.ApiServiceTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.dto.CheckInDtosTest" --tests "com.djs66256.short_drama.data.dto.MessageDtosTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增签到状态、签到提交、消息 preview、系统消息、互动消息接口 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CheckInDtos.kt` | 新增 | `SignInStatusDto`、`SignInDayDto` 及映射方法 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/MessageDtos.kt` | 新增 | `MessagePreviewDto`、系统/互动消息 DTO 与分页响应 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/CheckInModels.kt` | 新增 | 签到浮层与 7 日签到板 domain model |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/MessageModels.kt` | 新增 | 菜单 preview、系统消息、互动消息 domain model |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/ApiServiceTest.kt` | 修改 | 覆盖新增接口 contract |
| `android/app/src/test/java/com/djs66256/short_drama/data/dto/CheckInDtosTest.kt` | 新增 | 覆盖签到 DTO -> domain 映射 |
| `android/app/src/test/java/com/djs66256/short_drama/data/dto/MessageDtosTest.kt` | 新增 | 覆盖消息 DTO -> domain 映射与分页字段 |

### Step 2：建立签到/消息数据层与本地存储

- **测试场景**：
  - T-02 签到本地存储正确生成 installationId 并记录关闭态
  - T-03 repository 正确映射 success / error / exception / `204`
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/datasource/CheckInRemoteDataSource.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/datasource/MessageRemoteDataSource.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/repository/CheckInRepository.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/repository/MessageRepository.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/repository/CheckInRepositoryImpl.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/data/repository/MessageRepositoryImpl.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetCheckInStatusUseCase.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SubmitCheckInUseCase.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetMessagePreviewUseCase.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetSystemMessagesUseCase.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetInteractionMessagesUseCase.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/core/storage/CheckInLocalStoreTest.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/data/repository/CheckInRepositoryImplTest.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/data/repository/MessageRepositoryImplTest.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthInterceptorTest.kt`
- **实现内容**：
  1. 参考 `PlaybackSessionStore`，新增 `CheckInLocalStore`，在现有 DataStore 体系内统一管理 installationId 与 `dismissedServerDate`，不新增存储依赖。
  2. 新增 `CheckInRemoteDataSource` / `MessageRemoteDataSource`，延续现有 `HttpException -> ErrorDto -> ApiResult` 包装逻辑；对 preview 的 `204 No Content` 映射为业务空态而非异常。
  3. 新增 `CheckInRepository` / `MessageRepository` 及对应 use case，把 installationId 读取、header 透传、DTO 转 domain、分页参数封装收口到 data/domain 层。
  4. 更新 `AuthInterceptor.requiresAuth()`：把 `check-ins/status`、`check-ins`、`messages/interactions` 纳入自动 bearer 注入范围，确保登录用户走账号态、匿名用户仍可无 token 调用。
  5. 在 `RepositoryModule` 中补齐新增 repository/store 的注入绑定，保持现有 Hilt 组织方式。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.storage.CheckInLocalStoreTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.data.repository.CheckInRepositoryImplTest" --tests "com.djs66256.short_drama.data.repository.MessageRepositoryImplTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.core.network.AuthInterceptorTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt` | 新增 | installationId 生成、当日关闭态读写 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/CheckInRemoteDataSource.kt` | 新增 | 签到状态/提交的 ApiResult 包装 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/MessageRemoteDataSource.kt` | 新增 | preview / system / interactions 请求封装 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/CheckInRepository.kt` | 新增 | 签到仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/MessageRepository.kt` | 新增 | 消息仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/CheckInRepositoryImpl.kt` | 新增 | 安装标识、关闭态与签到接口整合 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/MessageRepositoryImpl.kt` | 新增 | 预览/列表的 domain 映射与分页透传 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetCheckInStatusUseCase.kt` | 新增 | 首页查询签到状态入口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SubmitCheckInUseCase.kt` | 新增 | 提交签到入口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetMessagePreviewUseCase.kt` | 新增 | 菜单消息预览入口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetSystemMessagesUseCase.kt` | 新增 | 系统消息列表入口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetInteractionMessagesUseCase.kt` | 新增 | 互动消息列表入口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 修改 | 为 check-in 与 interactions 补充 token 注入规则 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 注册新增 store / repository 绑定 |
| `android/app/src/test/java/com/djs66256/short_drama/core/storage/CheckInLocalStoreTest.kt` | 新增 | 校验 installationId 与关闭态持久化 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/CheckInRepositoryImplTest.kt` | 新增 | 校验签到状态、重复签到、异常映射 |
| `android/app/src/test/java/com/djs66256/short_drama/data/repository/MessageRepositoryImplTest.kt` | 新增 | 校验 preview 204、分页列表、错误态映射 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthInterceptorTest.kt` | 修改 | 校验 check-in / interaction 的 bearer 注入与匿名兼容 |

### Step 3：接入首页签到浮层与提交闭环

- **测试场景**：
  - T-04 首页冷启动资格与签到提交状态流转正确
  - T-02 本地关闭态与服务端业务日联动正确
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/CheckInPopup.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt`
- **实现内容**：
  1. 扩展 `HomeUiState`，新增签到子状态（可见性、提交中、serverDate、days、errorMessage），避免把签到逻辑塞进首页 feed 的 loading/error 字段。
  2. 在 `HomeViewModel.loadIfNeeded()` 复用现有一次性加载语义：首页 feed 成功后再异步检查签到资格；结合 `CheckInLocalStore` 与 `should_show_popup` 决定是否展示。
  3. 为评论弹层/登录占位冲突增加明确门闸：若 `CommentBottomSheet` 或 `CommentLoginPlaceholderDialog` 正在展示，则本次首页会话直接放弃签到浮层，不补弹。
  4. 新增 `CheckInPopup` composable，承载 7 日宫格、奖励文案、关闭按钮与“立即签到/今日已签到”按钮；保持 `HomeScreen` 只做宿主与回调编排。
  5. 先补 `HomeViewModelTest`，覆盖可展示、已关闭、提交成功、提交失败四类状态流转；再用 `HomeScreenTest` 锁定与评论弹层冲突时的显示判定 helper。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.home.viewmodel.HomeViewModelTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.home.ui.HomeScreenTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 修改 | 接入签到资格检查、关闭态、提交签到与错误处理 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 在首页宿主中挂载签到浮层并处理与评论弹层的冲突 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/CheckInPopup.kt` | 新增 | 签到浮层 UI 组件 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModelTest.kt` | 修改 | 覆盖签到展示、关闭、成功、失败流转 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/home/ui/HomeScreenTest.kt` | 修改 | 覆盖签到弹层显示条件与冲突判定 helper |

### Step 4：把菜单“我的消息”从静态文案升级为真实预览

- **测试场景**：
  - T-05 菜单消息预览状态独立于最近在看
  - T-08 点击消息入口仍走既有 close-menu-then-navigate 语义
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/components/MenuPanelComponents.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModelTest.kt`
- **实现内容**：
  1. 在 `MenuPanelUiState` 中新增独立的 `messagePreview` 子状态，避免沿用最近在看的 `isLoading/errorMessage` 导致两个区块互相污染。
  2. `MenuPanelViewModel` 在保留最近在看逻辑的基础上，新增 preview 拉取与轻量缓存；短时间重复打开菜单时优先复用最近一次成功结果。
  3. `MenuMessagePreview` 改为接收动态 entry / ui state：成功显示真实摘要，空态显示“暂无消息”，失败显示降级文案但入口仍可点击。
  4. `MenuPanelStaticEntries` 仅保留标题与路由等稳定元数据，把 summary 从纯静态文案调整为可被运行时数据覆盖的默认值。
  5. 测试优先覆盖 preview success / empty / error 与 recently viewed success / error 的交叉组合，确保消息区块与最近在看区块相互独立。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelViewModelTest"`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt` | 修改 | 新增消息 preview 子状态、加载与缓存逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt` | 修改 | 将 preview 状态透传到菜单 UI |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/components/MenuPanelComponents.kt` | 修改 | 渲染真实摘要、空态与降级文案 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt` | 修改 | 收口稳定标题/路由与默认占位文案 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModelTest.kt` | 修改 | 覆盖 preview 与最近在看并行状态流转 |

### Step 5：落地真实消息中心页与登录回流

- **测试场景**：
  - T-06 消息中心页匿名与登录态分流正确
  - T-07 登录回流消息页路由正确
  - T-08 菜单进入消息页仍保持先关抽屉再导航
- **目标文件**：
  - `android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/feature/messages/ui/MessageCenterScreen.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`
  - `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModelTest.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt`
  - `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt`
- **实现内容**：
  1. 新增 `MessageCenterViewModel`，基于 `AuthStateHolder.authStatus` 区分匿名/登录态：系统消息始终请求，互动消息仅登录态请求，局部失败只影响互动分区。
  2. 新增 `MessageCenterScreen`，使用双分区布局分别展示系统消息、互动消息或登录门槛；点击登录按钮时构造 `AppDestination.login(returnRoute = AppDestination.menuMessages(), source = "menu_messages")`。
  3. 在 `NavGraph` 中把 `menu/messages` 从 placeholder 替换成真实页面；同时把 `menuPlaceholderSpecs()` 中的 messages 占位移除，保留 booking/downloads 等现有占位路由。
  4. 路由测试优先覆盖登录回流与 placeholder 清理，导航测试复用现有 `MainNavigationViewModel.closeMenuThenNavigate()` 语义，确保无需引入新的菜单导航机制。
  5. 收尾时补充消息页匿名/登录态、互动消息失败、返回路径与底部导航可见性的回归测试。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.messages.viewmodel.MessageCenterViewModelTest"`
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest" --tests "com.djs66256.short_drama.navigation.NavGraphTest" --tests "com.djs66256.short_drama.navigation.MainNavigationViewModelTest"`
  - 运行 `cd android && ./gradlew assembleDebug`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt` | 新增 | 系统消息、互动消息、登录门槛与重试逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/messages/ui/MessageCenterScreen.kt` | 新增 | 真实消息中心页 UI |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 复用既有 `menu/messages` 与登录回流参数构造 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | 将 `menu/messages` 接入真实 screen，并移除对应 placeholder |
| `android/app/src/test/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModelTest.kt` | 新增 | 覆盖匿名/登录态、系统/互动分区与局部错误 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 覆盖消息页登录回流 route |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 校验 `menu/messages` 不再是 placeholder，底部栏规则不回退 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/MainNavigationViewModelTest.kt` | 修改 | 回归菜单关闭后再导航到消息页的时序 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3
   │           └────▶ Step 5
   └────▶ Step 4 ───▶ Step 5
```

## 验证总览

- [ ] 所有新增/修改单测通过（`cd android && ./gradlew test`）
- [ ] Debug 构建成功（`cd android && ./gradlew assembleDebug`）
- [ ] 无新增静态检查错误（`cd android && ./gradlew detekt`）
- [ ] 首页签到、菜单预览、消息中心三条主路径均有对应 JVM 单测覆盖
- [ ] `menu/messages` 已从 placeholder 路由切换为真实页面承接

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt` | 修改 | 新增签到/消息接口声明 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt` | 修改 | 覆盖 check-in 与 interaction token 注入 |
| `android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt` | 新增 | installationId 与关闭态本地存储 |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/CheckInDtos.kt` | 新增 | 签到 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/dto/MessageDtos.kt` | 新增 | 消息 DTO |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/CheckInRemoteDataSource.kt` | 新增 | 签到远端数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/data/datasource/MessageRemoteDataSource.kt` | 新增 | 消息远端数据源 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/CheckInRepositoryImpl.kt` | 新增 | 签到 repository 实现 |
| `android/app/src/main/java/com/djs66256/short_drama/data/repository/MessageRepositoryImpl.kt` | 新增 | 消息 repository 实现 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/CheckInModels.kt` | 新增 | 签到领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/model/MessageModels.kt` | 新增 | 消息领域模型 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/CheckInRepository.kt` | 新增 | 签到仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/MessageRepository.kt` | 新增 | 消息仓储接口 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetCheckInStatusUseCase.kt` | 新增 | 查询签到状态 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/SubmitCheckInUseCase.kt` | 新增 | 提交签到 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetMessagePreviewUseCase.kt` | 新增 | 查询菜单消息预览 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetSystemMessagesUseCase.kt` | 新增 | 查询系统消息 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/usecase/GetInteractionMessagesUseCase.kt` | 新增 | 查询互动消息 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt` | 修改 | 接入签到浮层状态管理 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` | 修改 | 首页承载签到浮层 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/CheckInPopup.kt` | 新增 | 签到弹层组件 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/viewmodel/MenuPanelViewModel.kt` | 修改 | 接入消息预览状态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/MenuPanelScreen.kt` | 修改 | 菜单消息区绑定真实状态 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/ui/components/MenuPanelComponents.kt` | 修改 | 菜单消息预览 UI 改造 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/menu/model/MenuPanelStaticEntries.kt` | 修改 | 收敛消息入口静态元数据 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/messages/viewmodel/MessageCenterViewModel.kt` | 新增 | 消息中心状态管理 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/messages/ui/MessageCenterScreen.kt` | 新增 | 真实消息中心页 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | `menu/messages` 接入真实页面 |
| `android/app/src/main/java/com/djs66256/short_drama/core/di/RepositoryModule.kt` | 修改 | 新增绑定 |
| `android/app/src/test/java/com/djs66256/short_drama/...` | 新增/修改 | 覆盖 DTO、store、repository、Home/Menu/Messages ViewModel 与路由回归 |
