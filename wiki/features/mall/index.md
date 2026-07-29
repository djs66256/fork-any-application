# 商城频道 (Mall)

> 最后更新：2026-07-29
> 覆盖端：Web / Android / iOS / Backend

## 功能概述

商城频道在既有 5 Tab 容器中补齐了首个按产品策略由 H5 承载、再由 Native 容器接入的真实一级业务入口。当前 Web 已提供 `/mall` 商城首页与 `/mall/product/[id]` 商品详情占位页；Android 与 iOS 都已把底部 `mall` tab 从 placeholder 切换为真实 WebView / WKWebView 容器，并围绕搜索复用、匿名商品点击登录拦截、登录返回恢复、容器重建恢复和 tab 高亮保持补齐宿主桥接；Backend 同步提供 `GET /api/mall/products` 作为商城首页双列商品 Feed 的唯一分页数据源。与首页、剧场等 Native 页面不同，商城首页固定区块中的 banner / shortcut 由 Web 侧集中 seed/config 管理，而商品列表通过 Backend 统一下发。

- 核心价值：把商城从“一级 tab 占位页”推进为真实可浏览的商业化入口，同时保持 H5 页面承载策略与 Native 容器闭环一致。
- 覆盖范围：Web 商城首页与商品详情占位页、Android / iOS Mall 容器与 bridge、Backend `GET /api/mall/products`、登录态同步与上下文恢复 contract。
- 当前状态：Web / Android / iOS / Backend 已实现；赚钱频道仍保持占位。

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Web | 商城首页 | `/mall` | `web/src/app/mall/page.tsx:1-5` |
| Web | 商品详情占位页 | `/mall/product/[id]`，非法 `id` 直接 `notFound()` | `web/src/app/mall/product/[id]/page.tsx:1-39` |
| Android | 底部导航“商城”一级频道 | `TopLevelTab.MALL`，graph=`mall_graph`，root=`mall` | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:344-401` |
| Android | 商城登录承接页 | `mall/login?productId={productId}&returnTarget=/mall` | `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:366-400` |
| iOS | 底部导航“商城”一级频道 | `AppTab.mall`，root view=`MallContainerView()` | `ios/ShortDrama/Sources/App/TabNavigationHostView.swift:48-61` |
| iOS | 商城登录承接页 | `AppRoute.mallLogin(context:)`，由 `fullScreenCover` 承载 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:66-89,230-238`、`ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift:41-51` |
| Backend | 商城商品 Feed 接口 | `GET /api/mall/products?page&pageSize` | `backend/src/app/api/mall/products/route.ts:1-18` |

### 承载策略

- 商城首页和商品详情继续由 H5 承载，符合 `PRODUCT.md` 中“商城（mall）与赚钱（earn）频道使用 H5 页面承载，由 Native 容器接入”的产品策略（`PRODUCT.md:22-25`）。
- Android / iOS 不新增独立 Native 商品详情页；商品点击最终仍进入 Web `/mall/product/[id]` 占位页（`web/src/app/mall/product/[id]/page.tsx:30-39`）。
- 搜索入口不在商城域内重复实现搜索页面，而是复用现有搜索承接：浏览器模式跳 `/search`，Native 模式通过 bridge 让宿主打开既有搜索页（`web/src/features/mall/bridge/mall-bridge.ts:42-59`）。

## 核心逻辑

### 流程：进入商城 Tab 并加载商城首页

1. Web `/mall` page 只做路由委托，把页面实际渲染交给 `MallPageScreen`，符合 Page 层只做委托的约束（`web/src/app/mall/page.tsx:1-5`、`web/CLAUDE.md`）。
2. `MallPageScreen` 固定按「Header → ShortcutGrid → BannerCarousel → ProductGrid → 登录拦截层」顺序组织页面，不把状态逻辑放在 Page 层（`web/src/features/mall/MallPageScreen.tsx:15-82`）。
3. `useMallPage()` 首次进入时请求 `fetchMallProducts({ page: 1, pageSize: config.mall.pageSize })`，并用 `requestIdRef + inFlightPagesRef` 防止乱序覆盖与同页并发重复请求（`web/src/features/mall/hooks/useMallPage.ts:176-224`）。
4. `fetchMallProducts()` 通过 Core 层 `api-client.ts` 请求 `/api/mall/products`，返回后再用 `MallProductsResponseSchema` 做结构校验，符合“Feature 不直接发请求”的端约束（`web/src/lib/mall/api.ts:1-14`、`web/CLAUDE.md`）。
5. Backend route 使用 `MallProductsQuerySchema` 解析 `page/pageSize`，通过 `MallService(getMallRepository())` 调用仓储并直接返回 `{ data, pagination }` JSON（`backend/src/app/api/mall/products/route.ts:7-18`）。
6. `MallService` 会再次用 `MallProductsResponseSchema` 校验 repository 输出；若返回结构非法，则统一包装为 `INTERNAL_ERROR`（`backend/src/services/mall/mall.service.ts:9-22`）。
7. 当前默认数据源为 `MallMockRepository`：固定 25 条商品种子，按稳定顺序分页切片，第一页默认 20 条，第二页 5 条（`backend/src/repositories/mock/mall.mock.repository.ts:4-218`、`backend/src/repositories/__tests__/mall.mock.repository.test.ts:22-93`）。

### 流程：搜索、快捷入口与 banner 承接

1. 商城顶部搜索按钮调用 `openMallSearch({ source: 'mall', returnTarget: '/mall' })`。
   - Native 模式：若存在 `__MALL_NATIVE_BRIDGE__` 且 `bridgeEnabled` 为真，则向宿主发送 `mall.openSearch`（`web/src/features/mall/bridge/mall-bridge.ts:13-58`）。
   - 浏览器模式：直接 fallback 到 `config.mall.searchFallbackRoute`，当前默认承接为 `/search`（`web/src/features/mall/bridge/mall-bridge.ts:54-58`）。
2. 购物车和 5 个快捷入口当前都留在商城上下文内，仅通过 `feedbackMessage` 显示“功能开发中”，不跳转到未知页（`web/src/features/mall/hooks/useMallPage.ts:282-285,379-380`）。
3. banner contract 由 `target_type + target_value` 驱动：
   - `search`：复用搜索链路；
   - `product`：先校验 `target_value` 是否为合法 UUID，再按登录态决定进入登录拦截或 `/mall/product/[id]`；
   - `web`：仅允许合法 URL；
   - `none`：留在页面内给出占位反馈（`web/src/features/mall/hooks/useMallPage.ts:286-329`）。
4. 当 banner 指向一个当前未出现在首屏列表中的商品 ID 时，页面会用 `createBannerTargetProduct(productId)` 生成最小占位商品对象，以便匿名态依然能展示页内登录拦截（`web/src/features/mall/hooks/useMallPage.ts:87-95,293-306`）。

### 流程：匿名商品点击、登录拦截与返回恢复

1. 商品卡点击和 `target_type=product` banner 点击共用相同登录态分流：未登录时不直接跳详情，而是把当前商品写入 `activeProduct` 并展示 `MallLoginInterceptOverlay`（`web/src/features/mall/hooks/useMallPage.ts:331-341`、`web/src/features/mall/MallPageScreen.tsx:73-78`）。
2. 用户点击“继续登录”后，H5 通过 `requestMallLogin({ source: 'mall', productId, returnTarget: '/mall' })` 发出 `mall.requestLogin` bridge 消息（`web/src/features/mall/hooks/useMallPage.ts:347-362`、`web/src/features/mall/bridge/mall-bridge.ts:61-66`）。
3. Android 宿主收到 bridge 消息后，`MallViewModel` 校验 `MallLoginContext`，发出 `OpenMallLogin` effect，由 `NavGraph` 打开 `mall/login` 承接页（`android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt:124-155`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:348-400`）。
4. iOS 宿主收到 bridge 消息后，`MallContainerViewModel` 设置 `pendingLoginContext` 并发出 `.requestLogin` route effect，由 `NavigationRouter.presentMallLogin(_:)` 打开 `fullScreenCover` 登录承接（`ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift:67-76`、`ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift:65-72`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:230-238`）。
5. 登录承接完成、取消或关闭后，Android 会触发 `MallLoginResult`，统一同步 `mall.syncAuthState` 与 `mall.restoreContext(reason='login-return')`（`android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt:163-175`）；iOS 则通过 `dismissMallLogin(completed:)` 回到 mall tab，并在容器内发送 `login-success` / `login-cancel` + `restoreContext(login-return)`（`ios/ShortDrama/Sources/App/NavigationRouter.swift:234-238`、`ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift:90-100`）。
6. Web 端收到宿主消息后会更新 `isLoggedIn`，并在 `login-return` 场景下关闭拦截层但不强制重载第一页，从而保留商城页面上下文（`web/src/features/mall/hooks/useMallPage.ts:226-249`、`web/src/features/mall/hooks/useMallPage.test.ts:414-442`）。

### 流程：宿主搜索返回、登录态同步与容器重建

1. Web H5 约定接收两类宿主消息：
   - `mall.syncAuthState`：同步宿主登录态与原因；
   - `mall.restoreContext`：表达 `search-return` / `login-return` / `container-recreated` 三类恢复语义（`web/src/lib/schemas.ts:118-147`）。
2. Web 目前通过 `window.addEventListener('message', ...)` 监听宿主消息，并用 `MallHostMessageSchema` 做结构校验（`web/src/features/mall/bridge/mall-host-sync.ts:12-50`、`web/src/features/mall/bridge/mall-host-sync.test.ts:4-92`）。
3. Android Native → H5 当前不是 `postMessage`，而是向页面注入 `window.dispatchEvent(new CustomEvent('mall.syncAuthState' ...))` 与 `window.dispatchEvent(new CustomEvent('mall.restoreContext' ...))`（`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt:191-209`）。
4. iOS Native → H5 也通过 `MallHostMessage.script` 注入同名 `CustomEvent`，而不是 `window.postMessage`（`ios/ShortDrama/Sources/Features/Mall/Models/MallBridgeMessage.swift:70-117`、`ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift:33-43`）。
5. 也就是说，当前 Native → H5 的真实实现与 Web H5 的监听方式并不完全一致：Native 发 `CustomEvent('mall.syncAuthState' / 'mall.restoreContext')`，而 Web 监听的是 `message` 事件。这是当前文档需要保留的实现事实与限制。
6. 在 Web 既有状态机里，`container-recreated` 会清空 `pendingRestoreReason` 并重新拉取第一页；`search-return` 与 `login-return` 则只清掉 restore marker，不主动重载，以尽量保留上下文（`web/src/features/mall/hooks/useMallPage.ts:237-249`、`web/src/features/mall/hooks/useMallPage.test.ts:374-442`）。
7. Android 容器重建时会重新把 `currentUrl` 指回 `/mall`，同步最新 auth state，并发送 `restoreContext(container-recreated)`（`android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt:181-190`）；iOS 则在 `handleContainerRecreated()` 中重载 home URL 并发送 `containerRecreated` 恢复事件（`ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift:102-105`）。

### 流程：商品详情占位页与参数守卫

1. 商品详情页入口为 `/mall/product/[id]`。
2. Page 层会先 `trim()`，再用 `MallProductIdSchema.safeParse()` 校验 UUID；非法 ID 直接 `notFound()`，不渲染模糊占位页（`web/src/app/mall/product/[id]/page.tsx:10-18,30-39`）。
3. 页面 metadata 也复用同一套 ID 解析结果，合法时输出具体商品占位标题，不合法则输出泛化描述（`web/src/app/mall/product/[id]/page.tsx:20-28`）。

## 多端实现

### Web

- Page 层：`web/src/app/mall/page.tsx:1-5`、`web/src/app/mall/product/[id]/page.tsx:1-39`
- Feature 层：`web/src/features/mall/MallPageScreen.tsx:15-82`
- 状态机：`web/src/features/mall/hooks/useMallPage.ts:19-386`
- Bridge：`web/src/features/mall/bridge/mall-bridge.ts:1-67`
- Host sync：`web/src/features/mall/bridge/mall-host-sync.ts:1-50`
- Core API：`web/src/lib/mall/api.ts:1-14`
- Shared schema：`web/src/lib/schemas.ts:45-147`
- 自动化证据：`web/src/features/mall/hooks/useMallPage.test.ts`、`web/src/features/mall/bridge/mall-host-sync.test.ts`、`web/src/features/mall/MallPageScreen.test.tsx`、`web/src/lib/schemas.test.ts`

### Android

- 一级频道承载：`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:344-401`
- 宿主页面：`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt:34-166`
- ViewModel：`android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt:31-229`
- WebView bridge：`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt:30-211`
- 共享类型：`android/app/src/main/java/com/djs66256/short_drama/feature/mall/model/MallLoginContext.kt:1-98`
- 登录承接页：`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallLoginScreen.kt:19-63`

### iOS

- 一级频道承载：`ios/ShortDrama/Sources/App/TabNavigationHostView.swift:48-61`
- 路由与搜索 / 登录返回：`ios/ShortDrama/Sources/App/NavigationRouter.swift:215-243`
- 宿主页面：`ios/ShortDrama/Sources/Features/Mall/Views/MallContainerView.swift:3-80`
- ViewModel：`ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift:3-153`
- WKWebView bridge：`ios/ShortDrama/Sources/Features/Mall/Views/Components/MallWebView.swift:4-100`
- bridge / host message 类型：`ios/ShortDrama/Sources/Features/Mall/Models/MallBridgeMessage.swift:3-117`

### Backend

- Route：`backend/src/app/api/mall/products/route.ts:1-18`
- Service：`backend/src/services/mall/mall.service.ts:1-23`
- Repository contract：`backend/src/repositories/interfaces/mall.repository.interface.ts:1-15`
- Mock repository：`backend/src/repositories/mock/mall.mock.repository.ts:1-218`
- 自动化证据：`backend/src/app/api/__tests__/mall-products.test.ts:1-147`、`backend/src/services/mall/mall.service.test.ts:1-91`、`backend/src/repositories/__tests__/mall.mock.repository.test.ts:1-93`

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/mall/products` | [../../api/mall.md](../../api/mall.md) | 商城首页双列商品 Feed 的唯一分页数据源 |
| `POST /api/auth/otp-requests` | [../../api/auth.md](../../api/auth.md) | 统一登录承接链路中的验证码请求 |
| `POST /api/auth/sessions` | [../../api/auth.md](../../api/auth.md) | 商城登录承接页复用的会话创建接口 |
| `GET /api/users/me` | [../../api/auth.md](../../api/auth.md) | Native 宿主恢复时同步当前登录态 |
| `DELETE /api/auth/session` | [../../api/auth.md](../../api/auth.md) | 登录后仍复用统一登出 contract |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Web `MallPageHookState` | `useReducer` | 页面级 | 聚合商品列表、分页、error、登录拦截、反馈、登录态与 restore marker | `web/src/features/mall/hooks/useMallPage.ts:19-174` |
| Web `requestIdRef` / `inFlightPagesRef` | `useRef` | 页面级 | 保护分页请求不乱序、不重复 | `web/src/features/mall/hooks/useMallPage.ts:177-220` |
| Android `MallUiState` | `MutableStateFlow` | 页面级 | 聚合容器 loading/success/error、当前 URL、待处理登录上下文 | `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt:31-77` |
| Android `effects` | `MutableSharedFlow<MallEffect>` | 页面级 | 打开搜索、打开登录承接页、向 H5 发送 host message | `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt:73-77` |
| iOS `MallContainerViewModel` published state | `@Published` | 页面级 | 管理容器状态、URLRequest、loadRevision、待处理登录上下文、routeEffect 与 hostMessage | `ios/ShortDrama/Sources/Features/Mall/ViewModels/MallContainerViewModel.swift:14-22` |
| iOS `NavigationRouter.pendingMallRestoreRequest` | `@Published` | 应用级 | 记录搜索或登录返回后 mall 需要恢复的上下文 | `ios/ShortDrama/Sources/App/NavigationRouter.swift:35-39,221-243` |
| Backend mall response | Zod schema + JSON 响应 | 请求级 | 统一 `MallProduct[] + pagination` 契约 | `backend/src/lib/schemas.ts:45-147` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 应用壳 | 一级频道承载 | 商城作为 Android / iOS 5 Tab 容器中的真实一级频道落地 |
| 搜索发现 | 入口复用 | 商城搜索入口复用现有搜索承接，而不是新增商城域内搜索页 |
| 认证体系 | 登录承接复用 | 商城匿名点击商品时复用统一 Native 登录承接与 auth contract |
| App Shell | tab 高亮与上下文恢复 | 搜索返回、登录返回、容器重建都依赖应用壳保持 mall tab 语义 |
| Web Core API | Feature 通过 Core 层发请求 | 商城 H5 不绕过 `api-client.ts` 直调接口 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js App Router | Web 商城 H5 页面与 Backend mall route 承载 | `web/src/app/mall/**` + `backend/src/app/api/mall/**` |
| Zod | mall query、response、bridge/host message contract 校验 | `web/src/lib/schemas.ts`、`backend/src/lib/schemas.ts` |
| Android WebView | 宿主承载 H5、桥接搜索/登录与 CustomEvent 注入 | `MallWebViewContainer` |
| iOS WKWebView | 宿主承载 H5、桥接搜索/登录与 CustomEvent 注入 | `MallWebView` |
| Backend mock repository | 首版商品 Feed 数据源 | `MallMockRepository` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Native → H5 host message 真实实现与 Web 监听方式不完全一致 | Android / iOS 通过 `CustomEvent('mall.syncAuthState' / 'mall.restoreContext')` 注入，但 Web 当前监听 `message` 事件；文档需按代码保留这一事实 | 2026-07-29 | `MallWebViewContainer.kt:191-209`、`MallBridgeMessage.swift:74-80`、`mall-host-sync.ts:32-49` |
| 商品 Feed 当前仍是 mock / seed 数据 | 商城列表顺序和内容固定，不是线上真实商品服务 | 2026-07-29 | `backend/src/repositories/mock/mall.mock.repository.ts:4-218` |
| 购物车、订单、券包、钱包、同款、国补专区仍为占位反馈 | 用户无法进入真实交易或资产页面 | 2026-07-29 | `useMallPage.ts:282-285,379-380` |
| 商品详情仍为占位页 | 尚未实现 SKU、库存、加购、下单、支付等交易能力 | 2026-07-29 | `web/src/app/mall/product/[id]/page.tsx:20-39` |
| 登录承接页仍是占位 / 模拟承接 | Android `MallLoginScreen` 与 iOS `MallLoginPlaceholderView` 当前用于闭环演示，不是完整账号页 | 2026-07-29 | `MallLoginScreen.kt:19-63`、`MallContainerView.swift:41-51` |
| 设备级黑盒未自动执行 | 当前结论主要来自代码、单测与 QA 文档，真机容器行为仍待补测 | 2026-07-29 | `docs/specs/2026-07-28-prd-13-mall/qa-test.md:1-220` |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录 PRD-13 商城频道的 H5 承载策略、`/mall` 与 `/mall/product/[id]`、Android/iOS WebView 容器接入、搜索/登录 bridge、`GET /api/mall/products`、匿名商品登录拦截与商城上下文恢复链路 |

---
*本文档由 llm-wiki skill 自动维护。*
