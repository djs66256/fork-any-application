# ShortDrama 项目功能模块与实现状态整理

## 项目概况

ShortDrama 是一个竖屏短剧内容平台，目前处于**项目骨架搭建阶段**。四端（Web、Backend、Android、iOS）均已初始化基础工程，但核心业务功能尚未开发。

## 功能模块清单与实现状态

### 1. 应用壳 (App Shell) — ✅ 四端已完成

各端均已搭建可运行的项目骨架，展示应用名称和版本号的占位页面。

| 端 | 技术栈 | 入口文件 | 状态 |
|----|--------|---------|------|
| Web | Next.js 16, React 19, TypeScript | `web/src/app/page.tsx` | ✅ 骨架完成 |
| Backend | Next.js 16, TypeScript | `backend/src/app/page.tsx` | ✅ 骨架完成 |
| Android | Kotlin 2.0.21, Jetpack Compose, Material3 | `android/app/src/main/java/.../MainActivity.kt` | ✅ 骨架完成 |
| iOS | Swift 6, SwiftUI | `ios/ShortDrama/Sources/ShortDramaApp.swift` | ✅ 骨架完成 |

- 所有端仅渲染一个居中展示应用名和版本的占位页面
- 配置统一管理，遵循"禁止硬编码"原则
- 无多页面路由、网络请求、状态管理或数据持久化

### 2. 健康检查 (Health Check) — ✅ Backend 已完成

后端提供标准健康检查端点，用于监控和运维。

| 方法 | 路径 | 响应 | 源文件 |
|------|------|------|--------|
| GET | `/api/health` | `{ status: "ok", timestamp, version }` | `backend/src/app/api/health/route.ts` |

- 使用 Zod (`HealthResponseSchema`) 校验响应结构
- 仅检查服务进程存活，未检查数据库连接等深度指标
- 无鉴权，端点公开可访问

### 3. 数据模型 (Data Models) — 🚧 Web/Backend 进行中

核心数据模型定义，当前仅 Web 端定义了 Drama（短剧）实体的 Zod Schema。

| 端 | 状态 | 说明 |
|----|------|------|
| Web | 🚧 部分完成 | 已定义 `DramaSchema`（id, title, description, coverUrl, category, episodeCount） |
| Backend | 🚧 部分完成 | 仅有 `HealthResponseSchema`，未定义业务数据模型 |
| Android | — 待实施 | 无类型安全的数据校验 |
| iOS | — 待实施 | 无类型安全的数据校验 |

- Web 和 Backend 各自独立维护 schemas，未共享
- 缺少用户、评论、剧集等其他业务实体的模型

### 4. 深链 (Deeplink) — 📅 规划中（仅 iOS 已声明）

通过自定义 URL Scheme 唤起应用。目前仅 iOS 声明了 `djsdrama://` 但不具备路由处理逻辑。

| 端 | Scheme | 状态 |
|----|--------|------|
| iOS | `djsdrama://` | 📅 已声明 URL Scheme，路由逻辑未实现 |
| Android | 待定 | 📅 未声明 App Links 或 Deep Links |
| Web | N/A（使用 HTTPS 路由） | — 不适用 |

- 无路由解析和分发逻辑
- 未配置 Universal Links（iOS）/ App Links（Android）

### 5. 播放器 — 📅 各端规划中

核心功能模块，负责短剧播放、控制与交互。目前仅有 API 接口设计规划，各端代码尚未初始化。

- 规划的 API 端点：`POST /api/player/start`、`POST /api/player/stop`
- 各端播放器组件、手势交互、字幕等均未实现

### 6. 其他功能域 — 📅 完全空白

以下功能域在 wiki 中已登记但文档和代码均为零：

| 功能域 | Web | Android | iOS | Backend |
|--------|-----|---------|-----|---------|
| 首页 Feed | 📅 | 📅 | 📅 | 📅 |
| 搜索 | 📅 | 📅 | 📅 | 📅 |
| 鉴权 | 📅 | 📅 | 📅 | 📅 |
| 个人中心 | 📅 | 📅 | 📅 | 📅 |
| 评论 | 📅 | 📅 | 📅 | 📅 |
| 分享 | 📅 | 📅 | 📅 | 📅 |
| 通知 | 📅 | 📅 | 📅 | 📅 |
| 订阅/付费 | 📅 | 📅 | 📅 | 📅 |
| 导航/路由 | 📅 | 📅 | 📅 | 📅 |

## 各端技术栈总览

| 端 | 框架/语言 | 关键依赖 | 版本 |
|----|----------|---------|------|
| Web | Next.js 16, React 19, TS | Zod | 0.1.0 |
| Backend | Next.js 16, TS | Zod ^4.4.3 | 0.1.0 |
| Android | Kotlin 2.0.21, Compose, Material3 | AGP 8.7.0, Gradle | 0.1.0 |
| iOS | Swift 6, SwiftUI | XcodeGen, Xcode 27 | 0.1.0 |

## 当前能力总结

- **已完成**：四端应用壳骨架 + Backend 健康检查端点 + Web 端 Drama 数据模型
- **进行中**：数据模型跨端统一
- **仅声明**：iOS 深链 URL Scheme 声明
- **空白**：播放器、首页 Feed、搜索、鉴权等核心业务功能均未开始开发

## Wiki 与代码一致性验证

经实际阅读各端源代码文件，wiki 中的描述与代码状态一致，未发现过时或错误的文档内容。
