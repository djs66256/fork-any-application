# 代码 Review：Android — PRD-13 商城

> Review 日期：2026-07-29

## 审查结果

| 维度 | 状态 | 说明 |
|------|------|------|
| 实现与 design 一致性 | ✅ | `mall` 一级 tab 已切换为真实 `MallScreen`，并接入 `MallWebViewContainer`、`MallLoginScreen`、搜索 bridge、登录承接与宿主恢复闭环。 |
| 无硬编码环境常量 | ✅ | 商城 H5 地址通过 `AppConfig.mallBaseUrl` / `BuildConfigAppConfig` 提供，未在页面或导航层硬编码环境地址。 |
| 代码风格符合平台规范 | ✅ | `detekt` 通过；本轮仅发现 `MallWebViewContainer.kt` 2 个未使用 import，已删除。 |
| 路由与导航闭环 | ✅ | `AppDestination.mallLogin(productId, returnTarget)`、`NavGraph` 中的 mall graph 与 `MallLoginScreen` 接线稳定，商城不再回退占位页。 |
| 容器状态与宿主通信 | ✅ | `MallViewModel` 已覆盖加载成功/失败/重试、`mall.openSearch`、`mall.requestLogin`、`mall.syncAuthState`、`mall.restoreContext` 等核心语义。 |
| 测试覆盖核心场景 | ✅ | `MallViewModelTest`、`RoutesTest`、`NavGraphTest` 覆盖初始化、bridge、恢复与路由 contract。 |
| 构建结果可用 | ✅ | `assembleDebug`、`test`、`detekt` 均通过；验证时使用 Android Studio 自带 JBR 21 作为 `JAVA_HOME`。 |

## 变更文件审查

| 文件 | 审查结果 | 问题数 |
|------|---------|--------|
| `android/app/build.gradle.kts` | ✅ | 0 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | ✅ | 0 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/BuildConfigAppConfig.kt` | ✅ | 0 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | ✅ | 0 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | ✅ | 0 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/model/MallLoginContext.kt` | ✅ | 0 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModel.kt` | ✅ | 0 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallScreen.kt` | ✅ | 0 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallWebViewContainer.kt` | ✅ | 0 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/mall/ui/MallLoginScreen.kt` | ✅ | 0 |
| `android/app/src/test/java/com/djs66256/short_drama/feature/mall/viewmodel/MallViewModelTest.kt` | ✅ | 0 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/RoutesTest.kt` | ✅ | 0 |
| `android/app/src/test/java/com/djs66256/short_drama/navigation/NavGraphTest.kt` | ✅ | 0 |

## 发现的问题

本轮收口过程中发现 1 个样式问题，已修复：

| 问题编号 | 摘要 | 状态 | 说明 |
|---------|------|------|------|
| #1 | `MallWebViewContainer.kt` 存在 2 个未使用 import，导致 `detekt` 失败 | ✅ 已修复 | 已删除未使用的 `MALL_RETURN_TARGET` / `MALL_SOURCE` import，并重新验证 `detekt` 通过。 |

## 修复记录

| 轮次 | 修复项 |
|------|--------|
| 1 | 在 `build.gradle.kts` 注入 `MALL_BASE_URL`，并通过 `AppConfig` / `BuildConfigAppConfig` 暴露配置访问。 |
| 1 | 新增 `MallLoginContext`、`MallViewModel`、`MallScreen`、`MallWebViewContainer`、`MallLoginScreen`，补齐商城容器三态、bridge 与恢复闭环。 |
| 1 | 在 `NavGraph` 中将 mall graph 从占位页切为真实商城页面，并增加 `mall/login` 登录承接路由。 |
| 1 | 补充 `MallViewModelTest`、`RoutesTest`、`NavGraphTest`，锁定初始化、登录/搜索 bridge、恢复与导航 contract。 |
| 1 | 删除 `MallWebViewContainer.kt` 中 2 个未使用 import，修复 `detekt` 失败。 |

## 验证记录

| 命令 | 结果 | 说明 |
|------|------|------|
| `cd android && JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew test` | ✅ 通过 | `testDebugUnitTest`、`testReleaseUnitTest` 与聚合 `test` 均通过。 |
| `cd android && JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug` | ✅ 通过 | `assembleDebug` 成功。 |
| `cd android && JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew detekt` | ✅ 通过 | 无新增 detekt 问题。 |

## 遗留问题（需人工决策）

无。

## 结论

- [x] ✅ 所有问题已修复，代码质量合格
- [ ] ⚠️ 存在遗留问题，需人工确认
