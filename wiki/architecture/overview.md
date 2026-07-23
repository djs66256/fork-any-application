# 系统总览 架构文档

> 最后更新：2026-07-22

## 概述

ShortDrama（短剧内容平台）是一个多端短剧内容应用，覆盖 Web、Android、iOS 三端用户界面，以及一个 Backend 后端服务。本项目采用 monorepo 结构，按端划分工作目录，统一产品信息在 `PRODUCT.md` 中维护。

- **产品名称**：ShortDrama
- **产品定位**：竖屏短剧的浏览、推荐与播放平台
- **技术标识**：appId=`com.djs66256.short_drama`, schema=`djsdrama://`
- **当前版本**：0.1.0（各端一致）

## 架构设计

### 整体架构

```
┌──────────────────────────────────────────────────────────────┐
│                        用户界面层                              │
├──────────────┬──────────────┬──────────────┬─────────────────┤
│   Web 前端   │  Android App │   iOS App    │  Backend Admin  │
│  Next.js 16  │   Kotlin +   │   Swift +    │   Next.js 16    │
│  React 19    │   Compose    │   SwiftUI    │   (SSR Pages)   │
└──────┬───────┴──────┬───────┴──────┬───────┴───────┬─────────┘
       │              │              │               │
       │    REST API  │    REST API  │    REST API   │
       │              │              │               │
       ▼              ▼              ▼               ▼
┌──────────────────────────────────────────────────────────────┐
│                    Backend API 服务层                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  API Routes (Next.js App Router)                      │   │
│  │  ├── /api/health         健康检查                     │   │
│  │  └── (更多接口规划中...)                               │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  共享层                                                │   │
│  │  ├── lib/config.ts       应用配置（环境变量注入）       │   │
│  │  ├── lib/schemas.ts      数据校验（Zod）               │   │
│  │  └── (数据访问层规划中...)                              │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│                      数据与基础设施层                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  数据库 (规划中: Supabase)                            │   │
│  │  视频存储 CDN (规划中)                                │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### 核心流程调用栈

#### 流程：健康检查

```
入口：HTTP GET /api/health

1. route.ts::GET()                     — API 路由处理器
   ├── config.ts::config               — 读取应用版本号
   ├── schemas.ts::HealthResponseSchema — Zod 校验响应结构
   └── NextResponse.json()             — 返回 JSON 响应
```

| 调用层级 | 文件 | 函数 | 职责 |
|---------|------|------|------|
| 入口 | `backend/src/app/api/health/route.ts:L5` | `GET()` | 处理健康检查请求 |
| 1 | `backend/src/lib/config.ts:L1` | `config` | 提供应用配置（版本号） |
| 2 | `backend/src/lib/schemas.ts:L3` | `HealthResponseSchema` | Zod 校验响应数据 |
| 3 | `backend/src/app/api/health/route.ts:L13` | `NextResponse.json()` | 构造并返回 JSON 响应 |

### 设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 使用 Next.js App Router 作为后端框架 | 统一前后端技术栈（Web 和 Backend 均基于 Next.js），减少上下文切换；支持 API Route 和 SSR 页面共存 | Backend 端同时承载管理页面和 API，职责边界需注意 |
| 使用 Zod 做数据校验 | 运行时类型安全，与 TypeScript 类型系统互补；支持从 Schema 推断类型 | 所有 API 输入输出需定义 Schema，增加一些初始化开销 |
| Monorepo 按端划分目录 | 便于各端独立演进，避免构建配置耦合 | 跨端共享代码需要额外处理（如共享 Schema 定义） |
| Android 使用 Jetpack Compose | 声明式 UI，与 React/SwiftUI 心智模型一致 | Android 端需 Kotlin 2.0+ 和 Compose 生态 |
| iOS 使用 SwiftUI + XcodeGen | 声明式 UI，XcodeGen 避免 .xcodeproj 冲突，便于 CI 集成 | iOS 端工程由 project.yml 生成，需 XcodeGen 工具 |
| 禁止硬编码常量 | 保证多环境部署的灵活性，避免安全隐患 | 所有环境相关值需通过配置文件/环境变量注入 |

## 跨端涉及

| 端 | 相关模块/文件 | 说明 |
|----|-------------|------|
| Web | `web/src/app/page.tsx`, `web/src/app/layout.tsx`, `web/src/lib/config.ts`, `web/src/lib/schemas.ts` | Next.js 16 + React 19 前端，展示首页，定义 Drama 数据模型 |
| Android | `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`, `android/app/src/main/AndroidManifest.xml` | Kotlin + Jetpack Compose，展示 HomeScreen 占位页面 |
| iOS | `ios/ShortDrama/Sources/ShortDramaApp.swift`, `ios/ShortDrama/Sources/ContentView.swift`, `ios/project.yml` | Swift 6 + SwiftUI，展示 ContentView 占位页面，声明 djsdrama:// URL Scheme |
| Backend | `backend/src/app/layout.tsx`, `backend/src/app/page.tsx`, `backend/src/app/api/health/route.ts`, `backend/src/lib/config.ts`, `backend/src/lib/schemas.ts` | Next.js 16 后端，提供管理页面和 /api/health 接口 |

## 技术栈总览

| 层级 | Web | Backend | Android | iOS |
|------|-----|---------|---------|-----|
| 语言 | TypeScript | TypeScript | Kotlin 2.0.21 | Swift 6 |
| UI 框架 | React 19 + Next.js 16 | Next.js 16 (SSR) | Jetpack Compose + Material3 | SwiftUI |
| 数据校验 | Zod | Zod | — | — |
| 构建工具 | next build | next build | AGP 8.7.0 + Gradle | XcodeGen + Xcode 27 |
| 标识符 | — | — | com.djs66256.short_drama | com.djs66256.short_drama |
| 最低版本 | — | — | minSdk 26 / compileSdk 36 | iOS 18.0 |
| 版本 | 0.1.0 | 0.1.0 | 0.1.0 | 0.1.0 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-22 | 从各端代码提取信息，初始创建 |

---

*本文档由 llm-wiki skill 自动维护。*
