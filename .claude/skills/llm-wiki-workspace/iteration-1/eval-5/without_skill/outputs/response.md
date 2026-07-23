# ShortDrama 项目 Wiki 完整文档

> 生成日期：2026-07-22
> 基于各端源代码提取生成

---

## 一、产品概览

### 1.1 产品信息

| 字段 | 值 |
|------|-----|
| **产品名称** | ShortDrama（短剧） |
| **产品简介** | 短剧内容平台，提供竖屏短剧的浏览、推荐与播放体验 |
| **竞品** | 红果 |
| **应用 ID** | `com.djs66256.short_drama` |
| **自定义 URL Scheme** | `djsdrama://` |

### 1.2 项目定位

当前项目是一个用于 fork 现有应用并推进后续落地的 harness 工程，覆盖从产品调研、方案沉淀，到多端应用开发与后续迭代的完整流程。仓库按端划分工作目录，便于将产品、研发和文档协作统一在同一仓库中进行。

---

## 二、目录结构与职责

```
fork-any-application/
├── PRODUCT.md                    # 产品信息（名称、竞品、技术标识）
├── CLAUDE.md                     # 全局规则、目录职责、跨端约束
├── android/                      # Android 应用（Kotlin + Jetpack Compose）
│   ├── CLAUDE.md                 # Android 端专属规范
│   ├── app/
│   │   ├── build.gradle.kts      # 应用构建配置
│   │   ├── proguard-rules.pro    # 混淆规则
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       └── java/com/djs66256/short_drama/MainActivity.kt
│   ├── build.gradle.kts          # 根构建配置（AGP 8.7.0, Kotlin 2.0.21）
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── keystore/.gitkeep
│   └── release-keystore.properties.example
├── ios/                          # iOS 应用（Swift 6 + SwiftUI）
│   ├── CLAUDE.md                 # iOS 端专属规范
│   ├── project.yml               # XcodeGen 项目描述
│   ├── Configs/
│   │   ├── Debug.xcconfig
│   │   └── Release.xcconfig
│   └── ShortDrama/
│       ├── Sources/
│       │   ├── ShortDramaApp.swift
│       │   └── ContentView.swift
│       ├── Resources/
│       │   ├── Info.plist
│       │   └── Assets.xcassets/
│       └── Tests/
│           └── ShortDramaTests.swift
├── web/                          # Web 前端（Next.js 16 + React 19）
│   ├── CLAUDE.md                 # Web 端专属规范
│   ├── package.json
│   ├── tsconfig.json
│   ├── next.config.ts
│   └── src/
│       ├── app/
│       │   ├── layout.tsx
│       │   ├── page.tsx
│       │   ├── globals.css
│       │   └── page.module.css
│       └── lib/
│           ├── config.ts
│           └── schemas.ts
├── backend/                      # 后端服务（Next.js 16 + TypeScript）
│   ├── CLAUDE.md                 # Backend 端专属规范
│   ├── package.json
│   ├── tsconfig.json
│   ├── next.config.ts
│   └── src/
│       ├── app/
│       │   ├── layout.tsx
│       │   ├── page.tsx
│       │   ├── globals.css
│       │   ├── page.module.css
│       │   └── api/health/route.ts
│       └── lib/
│           ├── config.ts
│           └── schemas.ts
├── wiki/                         # AI 维护的项目知识库
│   ├── index.md                  # Wiki 总索引
│   ├── features/
│   │   ├── index.md              # 功能域索引
│   │   ├── app-shell/index.md
│   │   ├── data-models/index.md
│   │   ├── deeplink/index.md
│   │   ├── health-check/index.md
│   │   └── video-player/index.md
│   └── api/
│       └── index.md              # API 文档索引
└── docs/                         # 项目文档
    ├── api/player.md             # 播放器 API 文档
    └── product_research/         # 产品调研与竞品分析
```

---

## 三、各端技术栈

| 端 | 语言/框架 | 核心依赖 | 构建工具 | 最低版本要求 |
|----|----------|---------|---------|------------|
| **Backend** | TypeScript, Next.js 16, React 19 | Zod 4.4.3 | next build | Node.js 20+ |
| **Web** | TypeScript, Next.js 16, React 19 | Zod（安装但未使用） | next build | — |
| **Android** | Kotlin 2.0.21, Jetpack Compose | Compose BOM 2024.09, Material3, Activity Compose 1.9.2, Lifecycle ViewModel 2.8.6 | AGP 8.7.0, Gradle | minSdk 26 (Android 8.0), compileSdk 36 |
| **iOS** | Swift 6, SwiftUI | 无外部依赖 | XcodeGen, Xcode 27 | iOS 18.0 |

### 3.1 版本信息一致性

所有端统一版本号 `0.1.0`，应用标识统一为 `com.djs66256.short_drama`。

---

## 四、功能模块总览

### 4.1 功能矩阵

| 功能域 | Web | Android | iOS | Backend | 状态 |
|--------|-----|---------|-----|---------|------|
| **应用壳 (App Shell)** | 已完成 | 已完成 | 已完成 | — | 各端骨架搭建完成 |
| **健康检查 (Health Check)** | — | — | — | 已完成 | `GET /api/health` |
| **数据模型 (Data Models)** | 进行中 | — | — | 进行中 | Web 有 DramaSchema，Backend 仅有 HealthResponseSchema |
| **深链 (Deeplink)** | — | — | 已声明 | — | iOS 声明 `djsdrama://`，路由逻辑未实现 |
| **播放器 (Video Player)** | 规划中 | 规划中 | 规划中 | 规划中 | API 文档已定义，代码未实现 |
| 首页 Feed | 规划中 | 规划中 | 规划中 | 规划中 | 完全空白 |
| 搜索 | 规划中 | 规划中 | 规划中 | 规划中 | 完全空白 |
| 鉴权 | 规划中 | 规划中 | 规划中 | 规划中 | 完全空白 |
| 个人中心 | 规划中 | 规划中 | 规划中 | 规划中 | 完全空白 |
| 评论 | 规划中 | 规划中 | 规划中 | 规划中 | 完全空白 |
| 分享 | 规划中 | 规划中 | 规划中 | 规划中 | 完全空白 |
| 通知 | 规划中 | 规划中 | 规划中 | 规划中 | 完全空白 |
| 订阅/付费 | 规划中 | 规划中 | 规划中 | 规划中 | 完全空白 |
| 导航/路由 | 规划中 | 规划中 | 规划中 | 规划中 | 完全空白 |

---

## 五、已完成功能模块详解

### 5.1 应用壳 (App Shell)

**概述**：各端应用的基础启动框架，展示应用名称和版本信息的占位页面。

#### Web 端

- **入口文件**：`web/src/app/layout.tsx`（根布局）、`web/src/app/page.tsx`（首页）
- **路由方案**：Next.js App Router（文件系统路由）
- **页面内容**：展示应用名称、版本号、环境信息
- **配置来源**：`web/src/lib/config.ts`，通过 `NEXT_PUBLIC_APP_NAME` / `NEXT_PUBLIC_APP_VERSION` 环境变量注入
- **样式**：CSS Modules (`page.module.css`) + 全局 CSS (`globals.css`)，支持明暗主题
- **字体**：Geist Sans + Geist Mono

#### Backend 端

- **入口文件**：`backend/src/app/layout.tsx`、`backend/src/app/page.tsx`
- **页面内容**：展示服务名称、版本、环境，含 `/api/health` 链接
- **配置来源**：`backend/src/lib/config.ts`，通过 `APP_NAME` / `APP_VERSION` / `NODE_ENV` 环境变量注入

#### Android 端

- **入口文件**：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`
- **Manifest**：`android/app/src/main/AndroidManifest.xml`，声明为 LAUNCHER Activity
- **UI 架构**：
  - `MainActivity.onCreate()` → `setContent` 加载 Compose
  - `ShortDramaTheme` 包装 MaterialTheme
  - `HomeScreen()` Composable 居中显示标题和版本
- **构建配置**：
  - `applicationId`: `com.djs66256.short_drama`
  - `compileSdk`: 36, `minSdk`: 26, `targetSdk`: 36
  - `versionCode`: 1, `versionName`: `0.1.0`
  - Kotlin JVM target: 17
- **签名**：debug 默认签名，release 从 `release-keystore.properties` 读取（文件不存在时跳过）
- **混淆**：`android/app/proguard-rules.pro` 配置了 Kotlin、Compose、Retrofit/OkHttp 规则（预置）

#### iOS 端

- **入口文件**：`ios/ShortDrama/Sources/ShortDramaApp.swift`
- **UI 架构**：`WindowGroup` → `ContentView`（VStack 展示图标、标题、版本）
- **项目配置**：`ios/project.yml`（XcodeGen 生成）
  - `PRODUCT_BUNDLE_IDENTIFIER`: `com.djs66256.short_drama`
  - `MARKETING_VERSION`: `0.1.0`
  - `CURRENT_PROJECT_VERSION`: `1`
  - target iOS 18.0, Swift 6.0
- **Info.plist**：`ios/ShortDrama/Resources/Info.plist`
  - 仅支持竖屏 (`UIInterfaceOrientationPortrait`)
  - 声明 URL Scheme `djsdrama`
  - 使用 AccentColor 作为启动屏背景
- **代码签名**：Debug 自动签名，Release 手动签名（需配置 Team ID + Provisioning Profile）
- **测试**：Swift Testing 框架，`ios/ShortDrama/Tests/ShortDramaTests.swift`

---

### 5.2 健康检查 (Health Check)

- **归属端**：Backend
- **API 端点**：`GET /api/health`
- **源文件**：`backend/src/app/api/health/route.ts`

**请求**：无参数

**响应格式**：
```json
{
  "status": "ok",
  "timestamp": "2026-07-22T...",
  "version": "0.1.0"
}
```

**处理流程**：
1. 构造 data 对象（status、timestamp、version）
2. 使用 `HealthResponseSchema`（Zod）校验响应结构
3. 通过 `NextResponse.json()` 返回

**依赖关系**：
- `@/lib/config`：获取应用版本号
- `@/lib/schemas`：`HealthResponseSchema` 做响应校验
- Next.js API Route + NextResponse

**已知限制**：
- 仅检查进程存活，未检查数据库、外部服务等深度健康指标
- 无鉴权，端点公开可访问

---

### 5.3 数据模型 (Data Models)

**概述**：项目核心数据模型定义，使用 Zod 做运行时校验。

#### Web 端 — DramaSchema

- **源文件**：`web/src/lib/schemas.ts`
- **依赖**：zod

```typescript
DramaSchema = z.object({
  id: z.string(),            // 短剧唯一标识
  title: z.string().min(1),  // 短剧标题（非空）
  description: z.string(),    // 短剧描述
  coverUrl: z.string().url(), // 封面图 URL
  category: z.string(),       // 分类
  episodeCount: z.number().int().positive(), // 集数（正整数）
})
```

**导出类型**：`Drama`（从 `DramaSchema` 推断）

#### Backend 端 — HealthResponseSchema

- **源文件**：`backend/src/lib/schemas.ts`

```typescript
HealthResponseSchema = z.object({
  status: z.literal('ok'),
  timestamp: z.string(),
  version: z.string(),
})
```

**已知问题**：
- Web 和 Backend 的 schemas 不一致（Web 有 Drama 但 Backend 没有）
- 两端未共享 Schema 定义，存在重复定义风险
- Android/iOS 端无类型安全的数据校验

---

### 5.4 深链 (Deeplink)

- **概述**：通过自定义 URL Scheme 实现外部唤起应用
- **当前状态**：仅 iOS 声明，路由处理逻辑未实现

| 端 | Scheme | 配置位置 | 状态 |
|----|--------|---------|------|
| iOS | `djsdrama://` | `Info.plist` → `CFBundleURLTypes` | 已声明，路由处理未实现 |
| Android | 待定 | `AndroidManifest.xml` intent-filter | 未实现 |
| Web | N/A | 使用标准 HTTPS 路由 | 不适用 |

**iOS 配置详情**（`ios/ShortDrama/Resources/Info.plist`）：
- `CFBundleURLName`: `com.djs66256.short_drama`
- `CFBundleURLSchemes`: `["djsdrama"]`

**已知限制**：
- 仅 iOS 声明了 Scheme，Android 未声明
- 无路由解析和分发逻辑
- 未配置 Universal Links（iOS）/ App Links（Android）
- 无 deeplink 测试覆盖

---

### 5.5 播放器 (Video Player) — 规划中

- **概述**：短剧视频播放器，核心功能模块
- **当前状态**：API 设计文档已完成，各端代码尚未初始化

#### API 设计

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/player/start` | 启动播放 | API 文档已定义，代码未实现 |
| POST | `/api/player/stop` | 停止播放并上报进度 | API 文档已定义，代码未实现 |

**路径变更记录**：
- `/api/video/play` → `/api/player/start`（2026-07-22）

---

## 六、API 接口总览

### 6.1 已实现

| 方法 | 路径 | 端 | 说明 | 源文件 |
|------|------|-----|------|--------|
| GET | `/api/health` | Backend | 健康检查，返回 `{status, timestamp, version}` | `backend/src/app/api/health/route.ts` |

### 6.2 已设计（未实现）

| 方法 | 路径 | 说明 | 文档 |
|------|------|------|------|
| POST | `/api/player/start` | 开始播放视频 | `docs/api/player.md` |
| POST | `/api/player/stop` | 停止播放并上报进度 | `docs/api/player.md` |

### 6.3 API 设计规范

- 所有接口遵循 RESTful 风格
- 请求与响应格式均为 JSON
- 参数校验使用 Zod
- Backend 端通过 Next.js App Router 的 Route Handler 实现

---

## 七、架构信息

### 7.1 整体架构

项目采用多端分离的架构模式：

```
                     ┌──────────┐
                     │  Backend │  Next.js 16 + TypeScript + Zod
                     │  :3000   │  RESTful API
                     └────┬─────┘
                          │
            ┌─────────────┼─────────────┐
            │             │             │
       ┌────┴────┐  ┌────┴────┐  ┌────┴────┐
       │   Web   │  │ Android │  │   iOS   │
       │ Next.js │  │ Compose │  │ SwiftUI │
       └─────────┘  └─────────┘  └─────────┘
```

- **Backend** 和 **Web** 共享 Next.js 技术栈但为独立工程（各自有独立的 package.json、tsconfig）
- **Android** 使用 Kotlin + Jetpack Compose + Material3
- **iOS** 使用 Swift 6 + SwiftUI
- 各端通过 RESTful API 与 Backend 通信

### 7.2 各端架构约束

#### Backend 端

- 语言：TypeScript
- 分层：接口层（API Route）→ 业务逻辑层 → 数据访问层
- 数据校验：Zod
- 配置：环境变量注入（`backend/src/lib/config.ts`）
- 计划接入：Supabase（数据访问层）

#### Web 端

- 语言：TypeScript
- 分层：页面层 → 状态层 → 数据访问层
- 状态管理：React Hooks
- 数据校验：Zod
- 配置：`NEXT_PUBLIC_*` 环境变量（`web/src/lib/config.ts`）

#### Android 端

- 语言：Kotlin
- UI：Jetpack Compose（声明式）
- 分层：UI（Composable）→ 状态管理（ViewModel）→ 业务逻辑 → 数据访问
- 配置：`build.gradle.kts` 中的 `defaultConfig` + `release-keystore.properties`

#### iOS 端

- 语言：Swift 6
- UI：SwiftUI（声明式）
- 分层：View → 状态管理 → 业务逻辑 → 数据访问
- 配置：XcodeGen `project.yml` + Info.plist + xcconfig

### 7.3 配置管理策略

遵循"禁止硬编码"原则：

| 端 | 配置机制 | 示例 |
|----|---------|------|
| Backend | `process.env` → `config.ts` | `APP_NAME`, `APP_VERSION`, `NODE_ENV` |
| Web | `process.env` → `config.ts`（`NEXT_PUBLIC_*` 前缀） | `NEXT_PUBLIC_APP_NAME`, `NEXT_PUBLIC_APP_VERSION` |
| Android | `build.gradle.kts` + keystore properties 文件 | `applicationId`, `versionName`, 签名配置 |
| iOS | `project.yml` + xcconfig + Info.plist | `PRODUCT_BUNDLE_IDENTIFIER`, `MARKETING_VERSION` |

### 7.4 安全与签名

#### Android

- Debug 构建无签名（开发调试用）
- Release 构建签名从 `release-keystore.properties` 读取
- Release 启用 ProGuard 混淆 + 资源压缩
- ProGuard 规则覆盖 Kotlin、Compose、Retrofit/OkHttp

#### iOS

- Debug 构建自动签名（`CODE_SIGN_STYLE = Automatic`）
- Release 构建手动签名（需配置 `DEVELOPMENT_TEAM`、`PROVISIONING_PROFILE_SPECIFIER`、`CODE_SIGN_IDENTITY`）

---

## 八、开发协作约定

### 8.1 全局规则（CLAUDE.md）

- 每端代码只能修改对应路径下的文件
- API 使用 RESTful 设计
- 禁止硬编码常量（地址、token、密钥等）
- 新增开源依赖前需征得用户同意
- 产品信息统一维护在 `PRODUCT.md`，各元内容文件不得内嵌具体产品信息

### 8.2 文档约定

| 文档类型 | 存放位置 | 例子 |
|---------|---------|------|
| 产品信息 | `PRODUCT.md` | 产品名称、竞品、技术标识 |
| 需求/项目跟踪/设计记录 | `docs/` | `docs/api/player.md` |
| 竞品分析 | `docs/product_research/` | `docs/product_research/mobile/` |
| 项目知识/关键决策 | `wiki/` | `wiki/features/`、`wiki/api/` |

### 8.3 测试要求

各端 CLAUDE.md 均要求：
- 核心业务逻辑需要单元测试
- 新增功能时同步补齐测试
- 场景无法测试时需要说明原因

当前状态：
- iOS：已配置 Swift Testing 框架，仅有占位测试
- Backend / Web / Android：尚未发现测试文件

---

## 九、已知问题与限制汇总

### 9.1 数据模型不一致

- Web 端定义了 `DramaSchema`，Backend 端仅有 `HealthResponseSchema`
- 两端未共享 Schema，存在重复定义和维护不一致的风险
- Android/iOS 无对应的类型安全数据模型

### 9.2 功能缺失

- 核心业务功能（Feed、搜索、播放器、鉴权等）均未实现
- 各端仅完成项目骨架搭建
- 深链仅 iOS 声明了 Scheme，无路由处理逻辑

### 9.3 测试覆盖

- 仅 iOS 有测试框架配置，测试内容为占位实现
- 其他端无测试文件

### 9.4 基础设施

- 健康检查仅检查进程存活，无深度健康探测
- 无 CI/CD 配置
- 无数据库连接层（Supabase 待接入）
- 无网络请求层封装

### 9.5 安全

- 健康检查端点无鉴权
- Android release 签名配置可选（文件不存在时跳过）
- iOS release 签名需手动配置

---

## 十、文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 产品信息 | `PRODUCT.md` | 产品名称、简介、竞品、技术标识 |
| 全局规范 | `CLAUDE.md` | 目录职责、协作约定、开发约束 |
| Backend 规范 | `backend/CLAUDE.md` | 后端技术栈、架构、测试要求 |
| Web 规范 | `web/CLAUDE.md` | Web 技术栈、架构、测试要求 |
| Android 规范 | `android/CLAUDE.md` | Android 技术栈、架构、测试要求 |
| iOS 规范 | `ios/CLAUDE.md` | iOS 技术栈、架构、测试要求 |
| Wiki 索引 | `wiki/index.md` | 功能域矩阵、技术栈总览 |
| 功能域索引 | `wiki/features/index.md` | 各功能域状态 |
| 应用壳文档 | `wiki/features/app-shell/index.md` | App Shell 详细说明 |
| 数据模型文档 | `wiki/features/data-models/index.md` | Schema 定义与多端对比 |
| 深链文档 | `wiki/features/deeplink/index.md` | URL Scheme 配置 |
| 健康检查文档 | `wiki/features/health-check/index.md` | Health API 说明 |
| 播放器文档 | `wiki/features/video-player/index.md` | 播放器功能规格 |
| API 索引 | `wiki/api/index.md` | API 文档入口 |
| 播放器 API | `docs/api/player.md` | Player API 接口定义 |
| 竞品分析 | `docs/product_research/index.md` | 竞品调研入口 |

---

*本文档基于项目各端源代码（backend/、web/、android/、ios/）及已有 wiki/、docs/ 目录内容综合提取生成。*
