# 应用壳 (App Shell)

> 最后更新：2026-07-22

## 功能概述

应用壳是各端应用的启动入口和基础框架。当前所有端均已搭建基础项目骨架，展示应用名称和版本信息的占位页面，尚未实现短剧浏览、播放等核心业务功能。

- **覆盖端**：Web、Android、iOS
- **核心价值**：为后续业务开发提供可运行的项目骨架
- **当前状态**：骨架搭建完成，内容为占位实现

## 入口与路由

### Web
- 入口组件：`web/src/app/layout.tsx`（根布局）+ `web/src/app/page.tsx`（首页）
- 路由方案：Next.js App Router，文件系统路由
- 当前页面：`/` 首页

### Backend
- 入口组件：`backend/src/app/layout.tsx` + `backend/src/app/page.tsx`
- 提供 `/` 页面展示后端服务信息，含 `/api/health` 链接

### Android
- 入口 Activity：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:1`
- Manifest 声明：`android/app/src/main/AndroidManifest.xml:10`，标记为 LAUNCHER
- 当前页面：`HomeScreen()` Composable

### iOS
- 入口 App：`ios/ShortDrama/Sources/ShortDramaApp.swift:3`
- 当前页面：`ContentView` 展示应用名和版本

## 核心逻辑

当前所有端的应用壳逻辑极为简单：渲染一个居中展示应用名和版本号的页面。

### Web 端
1. `layout.tsx` 定义 HTML 结构和全局样式引入
2. `page.tsx` 从 `config` 读取应用名、版本、环境变量并渲染
3. 支持明暗主题（`globals.css` 中的 `prefers-color-scheme` 媒体查询）

### Backend 端
1. 与 Web 类似，渲染服务名、版本、环境
2. 额外提供 `/api/health` 链接

### Android 端
1. `MainActivity.onCreate()` 调用 `setContent` 加载 Compose UI
2. `ShortDramaTheme` 包装 MaterialTheme
3. `HomeScreen()` Composable 居中显示标题和版本

### iOS 端
1. `ShortDramaApp` 使用 `WindowGroup` 加载 `ContentView`
2. `ContentView` 使用 VStack 展示图标、标题、版本

## 多端实现

### Web
- 源文件：`web/src/app/page.tsx:1-11`，`web/src/app/layout.tsx:1-31`
- 配置：`web/src/lib/config.ts:1-7`（通过 `NEXT_PUBLIC_*` 环境变量注入）
- 样式：CSS Modules（`page.module.css`）+ 全局 CSS（`globals.css`）
- 技术：Next.js 16, React 19, TypeScript

### Android
- 源文件：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt:1-58`
- 构建配置：`android/app/build.gradle.kts`（AGP 8.7.0, compileSdk 36, minSdk 26）
- 技术：Kotlin 2.0.21, Jetpack Compose, Material3
- 签名：release 构建签名配置从 `release-keystore.properties` 读取（当前文件不存在时跳过）

### iOS
- 源文件：`ios/ShortDrama/Sources/ContentView.swift:1-22`，`ios/ShortDrama/Sources/ShortDramaApp.swift:1-11`
- 项目配置：`ios/project.yml`（XcodeGen 生成，target iOS 18.0, Swift 6.0）
- Info.plist：`ios/ShortDrama/Resources/Info.plist`（支持竖屏、djsdrama:// URL Scheme）
- 技术：Swift 6, SwiftUI, Xcode 27
- 代码签名：Debug 自动签名，Release 手动签名（需替换证书）

## 配置管理

各端均通过配置统一管理应用标识和版本信息，遵循“禁止硬编码”原则：

| 端 | 配置方式 | 关键字段 |
|----|---------|---------|
| Web | 环境变量 `NEXT_PUBLIC_APP_NAME` / `NEXT_PUBLIC_APP_VERSION` | appId: `com.djs66256.short_drama` |
| Backend | 环境变量 `APP_NAME` / `APP_VERSION` | — |
| Android | `build.gradle.kts` 中 `defaultConfig` | applicationId: `com.djs66256.short_drama`, versionName: `0.1.0` |
| iOS | `project.yml` → `PRODUCT_BUNDLE_IDENTIFIER`, `MARKETING_VERSION` | bundleId: `com.djs66256.short_drama`, version: `0.1.0` |

## 依赖关系

- 各端应用壳互相独立，无跨端依赖
- Web/Backend 依赖 `config.ts` 配置模块
- Android 依赖 Jetpack Compose 和 Material3
- iOS 依赖 SwiftUI 框架

## 已知限制

- 各端均为占位页面，未实现实际业务功能（短剧列表、播放器、搜索等）
- Android 端仅有 `MainActivity`，无多 Activity 或 Navigation 组件
- iOS 端仅有 `ContentView`，无 TabView 或 NavigationStack
- Web 端仅有首页，无路由系统配置
- 各端均无网络请求层、状态管理、数据持久化实现
- Android 签名配置仅在 `release-keystore.properties` 存在时生效，当前文件不存在
