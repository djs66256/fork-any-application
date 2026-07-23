# ShortDrama 项目功能模块与各端实现状态整理

本项目是一个名为 **ShortDrama**（短剧内容平台）的 fork 型工程，旨在 fork 现有短剧应用并推进落地。仓库按端划分工作目录，覆盖产品调研、方案沉淀、多端开发与迭代的完整流程。

---

## 一、总体状态概览

项目当前处于**早期骨架阶段**。四端（Web、Backend、Android、iOS）均已搭建基础项目骨架，但所有端的实现均以占位内容为主，核心业务功能尚未开发。

| 端 | 技术栈 | 版本 | Bundle ID | 当前状态 |
|----|--------|------|-----------|---------|
| Web | Next.js 16, React 19, TypeScript, Zod | 0.1.0 | — | 骨架搭建完成 |
| Backend | Next.js 16, TypeScript, Zod | 0.1.0 | — | 骨架搭建完成，含健康检查 |
| Android | Kotlin 2.0.21, Jetpack Compose, Material3, AGP 8.7.0 | 0.1.0 | com.djs66256.short_drama | 骨架搭建完成 |
| iOS | Swift 6, SwiftUI, XcodeGen, Xcode 27 | 0.1.0 | com.djs66256.short_drama | 骨架搭建完成 |

---

## 二、功能模块清单与各端状态

### 2.1 应用壳 (App Shell) -- 已完成

所有端均已搭建可运行的基础项目骨架，展示应用名称和版本信息的占位页面。

| 端 | 状态 | 核心文件 | 说明 |
|----|------|---------|------|
| Web | 已完成 | `web/src/app/page.tsx`, `web/src/app/layout.tsx` | Home 页面展示应用名、版本、环境 |
| Backend | 已完成 | `backend/src/app/page.tsx` | 展示服务名和 `/api/health` 链接 |
| Android | 已完成 | `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | `HomeScreen()` Composable，居中展示标题和版本号 |
| iOS | 已完成 | `ios/ShortDrama/Sources/ContentView.swift`, `ShortDramaApp.swift` | VStack 展示图标、标题、版本号 |

**限制**：无网络请求层、无状态管理、无数据持久化、无路由系统（Web 仅有首页）。

---

### 2.2 健康检查 (Health Check) -- 已完成

后端专用的监控端点，返回服务存活状态。

| 端 | 状态 | 核心文件 | 说明 |
|----|------|---------|------|
| Backend | 已完成 | `backend/src/app/api/health/route.ts` | `GET /api/health`，返回 `{ status, timestamp, version }` |
| Web | 不适用 | — | — |
| Android | 不适用 | — | — |
| iOS | 不适用 | — | — |

**限制**：仅检查进程存活，未检查数据库连接等深度指标；无鉴权。

---

### 2.3 数据模型 (Data Models) -- 部分完成

核心数据实体的 Zod Schema 定义。

| 端 | 状态 | 核心文件 | 说明 |
|----|------|---------|------|
| Web | 进行中 | `web/src/lib/schemas.ts` | 定义了 `DramaSchema`（id, title, description, coverUrl, category, episodeCount） |
| Backend | 进行中 | `backend/src/lib/schemas.ts` | 仅定义了 `HealthResponseSchema`，尚未定义业务数据模型 |
| Android | 未实现 | — | 无类型安全的数据校验 |
| iOS | 未实现 | — | 无类型安全的数据校验 |

**限制**：两端 Schema 不一致、未共享、缺少用户/评论/剧集等模型。

---

### 2.4 深链 (Deeplink) -- 部分声明

通过自定义 URL Scheme `djsdrama://` 唤起应用。

| 端 | 状态 | 核心文件 | 说明 |
|----|------|---------|------|
| iOS | 规划中/已声明 | `ios/ShortDrama/Resources/Info.plist` | URL Scheme `djsdrama://` 已声明，路由处理逻辑未实现 |
| Android | 未实现 | — | Manifest 中无 intent-filter |
| Web | 不适用 | — | Web 使用 HTTPS 路由 |
| Backend | 不适用 | — | — |

**限制**：仅 iOS 声明了 Scheme，无路由解析/分发逻辑，未配置 Universal Links/App Links。

---

### 2.5 播放器 (Video Player) -- 待实施

核心功能模块，负责短剧视频的播放与控制。

| 端 | 状态 | 说明 |
|----|------|------|
| Web | 规划中 | 组件未开发 |
| Android | 规划中 | 组件未开发 |
| iOS | 规划中 | 组件未开发 |
| Backend | 规划中 | API 接口设计文档已存在（`docs/api/player.md`），定义了 `POST /api/player/start` 和 `POST /api/player/stop`，但参数待定、代码未实现 |

---

### 2.6 尚未实施的功能域

以下功能域在 wiki 索引中已列出但完全空白，所有端均为"规划中"状态：

| 功能域 | 说明 |
|--------|------|
| 首页 Feed | 短剧推荐流、内容列表 |
| 搜索 | 内容搜索功能 |
| 鉴权 | 用户登录、身份校验 |
| 个人中心 | 用户信息、观看历史 |
| 评论 | 短剧评论互动 |
| 分享 | 分享到社交平台 |
| 通知 | 推送通知 |
| 订阅/付费 | 订阅与付费体系 |
| 导航/路由 | 应用内导航与路由系统 |

---

## 三、文档体系现状

| 目录 | 内容 | 现状 |
|------|------|------|
| `docs/api/player.md` | 播放器 API 设计文档 | 已有，但参数待定 |
| `docs/product_research/` | 竞品分析目录 | 仅有索引和 mobile 频道占位 |
| `wiki/index.md` | 功能域索引总览 | 完整，标记了各功能域和各端状态 |
| `wiki/features/` | 各功能域详细文档 | 已覆盖 5 个功能域（app-shell, health-check, data-models, deeplink, video-player） |
| `PRODUCT.md` | 产品信息（名称、简介、竞品、技术标识） | 完整 |

---

## 四、总结

项目已完成**多端骨架搭建**，所有四端均可运行并展示占位内容。基础设施层面，后端具备健康检查端点，Web 端定义了 Drama 数据模型 Zod Schema，iOS 端声明了 deeplink URL Scheme。

**核心业务功能（Feed、搜索、鉴权、播放器等）均尚未实现**，所有端目前处于"骨架就绪、业务空白"的状态。下一步应优先推进播放器和首页 Feed 等核心体验功能的开发。
