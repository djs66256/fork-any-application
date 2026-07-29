# 实现计划：Android — PRD-13 商城

> 创建日期：2026-07-28
> 对应技术方案：design-android.md
> 对应需求：spec.md

## 概述

Android 端本期目标是将 `mall` 一级频道从 `PlaceholderScreen` 替换为真实商城容器，使用 `AppConfig` 提供的商城 H5 地址加载 `/mall`，并通过 `MallViewModel` 承载 loading / success / error 三态、搜索 bridge、登录承接与返回恢复语义。实现过程遵循轻量 TDD：先补 JVM 单元测试，再落地 ViewModel / 导航 / WebView 容器代码；新增业务逻辑同步补齐单元测试。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> Android 要求：每个场景都需要单元测试；新增业务逻辑同步补齐测试。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 商城容器初始状态使用配置化首页地址 | fake `AppConfig.mallBaseUrl=https://mall.example.com`，初始化 `MallViewModel` | `currentUrl=https://mall.example.com/mall`，`state=Loading`，无硬编码 URL | 单元测试 | P0 |
| T-02 | 页面加载成功/失败/重试正确驱动容器状态 | 依次触发加载成功、加载失败、点击重试 | 状态在 `Loading / Success / Error` 间正确切换，重试后重新加载商城首页 | 单元测试 | P0 |
| T-03 | 搜索 bridge 合法与非法 payload 都被安全处理 | `mall.openSearch` 合法 payload / 缺失 `returnTarget` 或非法 source | 合法请求发出 `OpenSearch` effect，非法 payload 被忽略且不破坏现有状态 | 单元测试 | P0 |
| T-04 | 登录 bridge 记录上下文并拦截重复请求 | `mall.requestLogin(productId, returnTarget)` 合法 payload / 空 `productId` / 重复点击 | 合法请求发出 `OpenMallLogin` effect 并记录 `pendingLoginContext`，非法与重复请求被忽略 | 单元测试 | P0 |
| T-05 | 搜索返回、登录返回与前后台恢复会同步登录态并恢复商城上下文 | 调用 search return、login return、app resume / container recreated 入口 | 发出 `mall.syncAuthState` 与 `mall.restoreContext` 对应 effect，清空待登录上下文，至少恢复到 `/mall` 首页 | 单元测试 | P0 |
| T-06 | Mall 路由与登录路由接线正确且不再回退占位页 | 调用 `AppDestination.mallLogin(productId, returnTarget)`，检查 `NavGraph` mall graph | `mall` 与 `mall/login` 路由字符串稳定，mall graph 接入 `MallScreen`/`MallLoginScreen`，不再使用商城 placeholder | 单元测试 | P1 |

## 实现步骤

### Step 1：补齐商城配置与 ViewModel 初始合同

- **关联测试**：T-01
- **目标文件**：`android/app/build.gradle.kts`、`android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/model/MallLoginContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt`
- **实现内容**：
  1. 先在 `android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt` 增加 T-01，用 fake `AppConfig` 验证 `MallViewModel` 初始化时会把 `mallBaseUrl` 拼成 `/mall` 首页地址，并以 `Loading` 作为初始宿主态。
  2. 在 `android/app/build.gradle.kts` 复用现有 `local.properties` / gradle 配置读取方式补充 `mall.base.url` 对应的 `BuildConfig` 字段，不在 `MallScreen` 或 `NavGraph` 内硬编码商城地址。
  3. 扩展 `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` 与 `android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt`，暴露 `mallBaseUrl` 配置入口。
  4. 新增 `android/app/src/main/java/com/djs66256/short_drama/feature/mall/model/MallLoginContext.kt` 作为 `productId + returnTarget + source` 的最小上下文模型。
  5. 新增 `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt`，先落最小 `UiState`、`MallContainerState` 与首页 URL 初始化逻辑，为后续场景复用同一测试骨架。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.mall.viewmodel.MallViewModelTest"` 确认 T-01 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/build.gradle.kts` | 修改 | 增加商城 H5 地址的 BuildConfig 注入 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 修改 | 扩展 `mallBaseUrl` 配置接口 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt` | 修改 | 暴露 `BuildConfig` 中的商城地址 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/model/MallLoginContext.kt` | 新增 | 定义商城登录上下文模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt` | 新增 | 定义商城容器初始状态与首页 URL |
| `android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt` | 新增 | 新增商城 ViewModel 初始态测试 |

### Step 2：落实容器加载、错误与重试状态机

- **关联测试**：T-02
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt`
- **实现内容**：
  1. 先在 `MallViewModelTest.kt` 补 T-02，覆盖 WebView 首页首次成功、加载失败、点击重试三条状态流转路径。
  2. 在 `MallViewModel.kt` 中补齐页面事件处理入口，例如 `onPageLoadStarted`、`onPageLoadSucceeded`、`onPageLoadFailed`、`retryLoadHome`，并维护 `lastLoadedHomeUrl` 与 `currentUrl`。
  3. 新增 `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt`，用 Compose 渲染 loading / error / success 三态，并把重试动作回调给 ViewModel。
  4. 确保加载失败时只展示商城宿主错误态，不回退 `NavGraph` 中的 mall placeholder。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.mall.viewmodel.MallViewModelTest"` 确认 T-02 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt` | 修改 | 增加加载成功/失败/重试状态机 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt` | 新增 | 渲染商城宿主三态与重试入口 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt` | 修改 | 增加容器状态流转测试 |

### Step 3：打通搜索 bridge 请求与安全校验

- **关联测试**：T-03
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt`
- **实现内容**：
  1. 先在 `MallViewModelTest.kt` 增加 T-03，覆盖合法 `mall.openSearch` payload 与非法 source / returnTarget 情况。
  2. 新增 `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt`，定义 WebView 向上抛出的统一 bridge 回调，不把 payload 解析逻辑散落到 `NavGraph`。
  3. 在 `MallViewModel.kt` 中解析并校验搜索 bridge，只允许 `source=mall` 且 `returnTarget=/mall` 的消息进入 effect；非法 payload 仅记录日志并忽略。
  4. 在 `MallScreen.kt` 中消费 `OpenSearch` effect，复用现有 `onOpenSearch` 导航回调打开搜索页。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.mall.viewmodel.MallViewModelTest"` 确认 T-03 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt` | 修改 | 增加搜索 bridge 解析与 effect 发送 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt` | 修改 | 消费搜索 effect 并回调导航 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt` | 新增 | 承接 WebView 与 JS bridge 事件 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt` | 修改 | 增加搜索 bridge 场景测试 |

### Step 4：完成登录 bridge、登录态同步与返回恢复

- **关联测试**：T-04、T-05
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/model/MallLoginContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt`、`android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt`
- **实现内容**：
  1. 先在 `MallViewModelTest.kt` 增加 T-04、T-05，覆盖合法登录请求、空 `productId`、重复点击、搜索返回、登录返回、前后台恢复与容器重建恢复场景。
  2. 在 `MallViewModel.kt` 引入对现有 `AuthSessionProvider` 的读取，统一生成 `mall.syncAuthState` 所需的登录态 payload，并在登录成功/取消、搜索返回、App resume 后触发对应 effect。
  3. 在 `MallViewModel.kt` 维护 `pendingLoginContext`，确保重复 `mall.requestLogin` 不会重复打开登录承接页；返回恢复后及时清空待处理上下文。
  4. 在 `MallWebViewContainer.kt` 中补齐宿主消息注入入口，用于把 `mall.syncAuthState` 与 `mall.restoreContext` 发回 H5；在容器重建时最少回到商城首页 `/mall`。
  5. 在 `MallScreen.kt` 中串联 ViewModel effect 与 WebView 容器调用，确保登录承接前后仍停留在商城语义内。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.mall.viewmodel.MallViewModelTest"` 确认 T-04、T-05 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt` | 修改 | 增加登录 bridge、登录态同步、返回恢复逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/model/MallLoginContext.kt` | 修改 | 补齐登录上下文字段与校验语义 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt` | 修改 | 串联登录承接与恢复回调 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt` | 修改 | 增加 host message 注入与恢复调用 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt` | 修改 | 增加登录与恢复相关测试 |

### Step 5：接入 Mall NavGraph、登录路由与最终页面装配

- **关联测试**：T-06
- **目标文件**：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt`、`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallLoginScreen.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt`
- **实现内容**：
  1. 先在 `RoutesTest.kt` 与 `NavGraphTest.kt` 中新增 T-06，锁定 `mall`、`mall/login` 路由 helper 与 mall graph 接线结果，防止回退到 `PlaceholderScreen`。
  2. 在 `AppDestination.kt` 新增 `Route.MALL_LOGIN` 与 `mallLogin(productId, returnTarget)` helper，保持参数生成方式与现有 `searchResult` / `ranking` helper 一致。
  3. 在 `NavGraph.kt` 将 `Graph.MALL` 的根页面从 `PlaceholderScreen` 替换为 `MallScreen`，并新增 `mall/login` composable 作为商城专属登录承接页。
  4. 新增 `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallLoginScreen.kt`，承接 close / cancel / success 三类登录返回动作，并统一回调 ViewModel 触发恢复逻辑。
  5. 收尾 `MallWebViewContainer.kt` 的 `AndroidView(WebView)` 装配、bridge 注册、页面回调与宿主消息发送入口，确保页面最终可按 MallScreen -> MallWebViewContainer -> MallLoginScreen 闭环工作。
- **验证方式**：
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.navigation.RoutesTest" --tests "com.djs66256.short_drama.navigation.NavGraphTest"` 确认 T-06 通过
  - 运行 `cd android && ./gradlew test --tests "com.djs66256.short_drama.feature.mall.viewmodel.MallViewModelTest"` 做回归验证
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增商城登录路由与 helper |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | mall graph 接入 `MallScreen` 与 `MallLoginScreen` |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt` | 修改 | 串联导航、ViewModel 与 WebView 容器 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt` | 修改 | 完成 WebView 容器接线 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallLoginScreen.kt` | 新增 | 商城专属登录承接页 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 校验商城路由 helper |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 校验 mall graph 不再使用 placeholder |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5
```

## 验证总览

- [x] 所有测试通过（`cd android && JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew test`）
- [x] Build 成功（`cd android && JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`）
- [x] 无新增 lint 错误（`cd android && JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew detekt`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `android/app/build.gradle.kts` | 修改 | 注入商城 H5 地址配置 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | 修改 | 新增 `mallBaseUrl` |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt` | 修改 | 返回 `mallBaseUrl` 配置值 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | 修改 | 新增商城登录路由与 helper |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | 修改 | mall graph 接入真实商城页面 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/model/MallLoginContext.kt` | 新增 | 商城登录上下文模型 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt` | 新增 | 商城容器状态、bridge、恢复逻辑 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt` | 新增 | 商城根 Composable 与状态承接 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt` | 新增 | WebView 容器与 JS bridge 接线 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallLoginScreen.kt` | 新增 | 商城专属登录承接页 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt` | 新增 | 商城业务逻辑单元测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | 修改 | 增补商城路由测试 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | 修改 | 增补 mall graph 接线测试 |