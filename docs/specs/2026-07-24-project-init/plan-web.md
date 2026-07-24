# 实现计划：Web — 项目初始化与架构设计

> 创建日期：2026-07-24
> 对应技术方案：design-web.md
> 对应需求：spec.md（Section 4.3 Web 前端分层架构、Section 6.5 US-05 Web 工程初始化）

## 概述

在现有 Next.js 16 + React 19 项目骨架基础上，按照五层架构（Page → Feature → Shared UI → Core → Design System）重新组织目录结构，补齐各层骨架代码与测试。从零搭建 Design System CSS tokens，实现 api-client fetch wrapper，创建 Button/Card/Container 通用组件，建立 Home/Player/DramaDetail 三个 Feature 模块，重构页面路由并补充 App Shell（loading/error/404）。完成后 `npm run dev` 可访问 `/`、`/play/[id]`、`/detail/[id]` 三个路由。

当前 Web 工程已通过 `create-next-app` 初始化，包含 `app/layout.tsx`、`app/page.tsx`、`lib/config.ts`、`lib/schemas.ts`、`app/globals.css`。本轮在现有基础上重构并补齐五层架构。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。Web 端业务逻辑、状态转换、数据校验的改动需补充测试。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | tokens.css 定义设计 Token | 读取 `tokens.css` 内容 | 文件包含 `--color-primary`, `--spacing-md`, `--radius-md` 等 CSS 自定义属性 | 单元测试 | P0 |
| T-02 | config 模块读取环境变量 | `vi.stubEnv('NEXT_PUBLIC_APP_NAME', 'TestApp')` | `config.app.name` 返回 `"TestApp"` | 单元测试 | P0 |
| T-03 | HealthResponseSchema 校验合法数据 | `{ status: "ok", version: "0.1.0", services: { database: "connected", redis: "connected" } }` | `parse()` 成功返回原数据 | 单元测试 | P0 |
| T-04 | HealthResponseSchema 拒绝非法数据 | `{ status: "error" }` | `parse()` 抛出 ZodError | 单元测试 | P0 |
| T-05 | api-client apiFetch 发起 GET 请求 | `apiFetch('/api/health')` | 请求 URL 包含 baseUrl + `/api/health`，响应数据正确返回 | 单元测试 | P0 |
| T-06 | api-client apiFetch 处理 HTTP 错误 | 服务器返回 500 | 抛出 `ApiError`，message 和 status 正确 | 单元测试 | P1 |
| T-07 | Button 渲染 primary variant | `variant="primary"` 渲染 Button | DOM 中 button 元素含 `data-variant="primary"` 属性 | 组件测试 | P0 |
| T-08 | Container 应用 maxWidth | `maxWidth="768px"` 渲染 Container | 渲染元素 `style.maxWidth` 为 `"768px"` | 组件测试 | P1 |
| T-09 | HomeScreen 展示完整内容 | 渲染 HomeScreen | 展示应用名称、版本号、环境标识、两个导航链接（`/play/sample`、`/detail/sample`） | 组件测试 | P0 |
| T-10 | PlayerScreen 展示 dramaId | `dramaId="test123"` 渲染 PlayerScreen | 页面内容包含 `"test123"` | 组件测试 | P1 |
| T-11 | DramaDetailScreen 展示 dramaId | `dramaId="test456"` 渲染 DramaDetailScreen | 页面内容包含 `"test456"` | 组件测试 | P1 |

## 实现步骤

<!-- 每个步骤遵循：定义测试 → 写实现 → 验证 → 补充测试 → 记录变更 -->

### Step 1：Design System — CSS tokens + globals.css 重构

- **关联测试**：T-01
- **目标文件**：`web/src/styles/tokens.css`、`web/src/styles/globals.css`、`web/src/app/layout.tsx`
- **实现内容**：
  1. 创建 `web/src/styles/tokens.css`，定义 CSS 自定义属性：
     - 颜色：`--color-primary`、`--color-primary-hover`、`--color-secondary`、`--color-background`、`--color-surface`、`--color-text-primary`、`--color-text-secondary`、`--color-border`、`--color-error`
     - 间距：`--spacing-xs`(4px)、`--spacing-sm`(8px)、`--spacing-md`(16px)、`--spacing-lg`(24px)、`--spacing-xl`(32px)、`--spacing-2xl`(48px)
     - 圆角：`--radius-sm`(4px)、`--radius-md`(8px)、`--radius-lg`(12px)
     - 字体：`--font-size-sm`、`--font-size-md`、`--font-size-lg`、`--font-size-xl`、`--font-size-2xl`
     - 暗色模式：使用 `prefers-color-scheme: dark` 媒体查询覆盖颜色变量
  2. 将 `web/src/app/globals.css` 迁移为 `web/src/styles/globals.css`，内容为 CSS reset（box-sizing、margin/padding 归零、基础 body 样式），并引入 `@import './tokens.css'`
  3. 删除旧的 `web/src/app/globals.css`
  4. 更新 `web/src/app/layout.tsx`：
     - 将 `import "./globals.css"` 改为 `import "@/styles/globals.css"`
     - `<html lang="en">` 改为 `<html lang="zh-CN">`
     - metadata title 改用 template 模式：`{ default: 'ShortDrama', template: '%s — ShortDrama' }`
  5. 编写 T-01 测试文件 `web/src/styles/tokens.test.ts`：使用 `fs.readFileSync` 读取 tokens.css，验证关键 CSS 变量存在
- **验证方式**：✅ 已完成
  - 运行 `npx vitest run src/styles/tokens.test.ts` 确认 T-01 通过
  - 运行 `npm run build` 确认 layout 改动不破坏构建
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/styles/tokens.css` | 新增 | CSS 自定义属性（颜色、间距、圆角、字体、暗色模式） |
| `web/src/styles/globals.css` | 新增（迁移） | CSS reset + 基础全局样式，import tokens.css |
| `web/src/styles/tokens.test.ts` | 新增 | 验证 tokens.css 包含必需 CSS 变量（T-01） |
| `web/src/app/globals.css` | 删除 | 内容迁移至 styles/globals.css |
| `web/src/app/layout.tsx` | 修改 | 更新 CSS 引入路径、lang="zh-CN"、metadata template |

---

### Step 2：Core 层 — types、schemas、config 与测试基础设施

- **关联测试**：T-02、T-03、T-04
- **目标文件**：`web/src/lib/types.ts`、`web/src/lib/schemas.ts`（扩展）、`web/src/lib/config.test.ts`、`web/src/lib/schemas.test.ts`
- **前置条件**：Step 1 完成
- **实现内容**：
  1. 安装测试依赖（需征得用户同意）：
     - `vitest`、`@testing-library/react`、`@testing-library/jest-dom`、`jsdom` 作为 devDependencies
     - 注意 `zod` 尚未在 `package.json` 中，`web/src/lib/schemas.ts` 已引用，本次一并安装 zod 作为 dependency
  2. 创建 `web/vitest.config.ts`：配置 `environment: 'jsdom'`、`globals: true`、`setupFiles: ['./tests/setup.ts']`、`@` 路径别名
  3. 创建 `web/tests/setup.ts`：引入 `@testing-library/jest-dom/vitest`
  4. 更新 `web/package.json`：`scripts` 中新增 `"test": "vitest run"` 和 `"test:watch": "vitest"`
  5. 创建 `web/src/lib/types.ts`，定义错误类和共享类型：
     - `ApiError`（含 `status`、`message`）
     - `NetworkError`
     - `TimeoutError`
     - `PaginationMeta` 接口（`page`, `pageSize`, `total`, `totalPages`）
     - `PaginatedResponse<T>` 接口
  6. 扩展 `web/src/lib/schemas.ts`，新增 `HealthResponseSchema`（与 Backend `backend/src/lib/schemas.ts` 中的结构对齐：`status: enum(["ok","degraded","error"])`、`version: string`、`services: { database, redis }`），保留现有 `DramaSchema`
  7. 编写 `web/src/lib/config.test.ts`（T-02）：`vi.stubEnv` 设置环境变量后读取 `config`，验证默认值和环境变量覆盖
  8. 编写 `web/src/lib/schemas.test.ts`（T-03、T-04）：测试 `HealthResponseSchema.parse()` 合法 / 非法输入
- **验证方式**：✅ 已完成
  - 运行 `npm test -- src/lib/config.test.ts` 确认 T-02 通过
  - 运行 `npm test -- src/lib/schemas.test.ts` 确认 T-03、T-04 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/package.json` | 修改 | 新增 zod dependency；新增 vitest / @testing-library/react / @testing-library/jest-dom / jsdom devDependencies；新增 test/test:watch scripts |
| `web/vitest.config.ts` | 新增 | Vitest 配置：jsdom、setupFiles、路径别名 |
| `web/tests/setup.ts` | 新增 | Testing Library jest-dom matchers 初始化 |
| `web/src/lib/types.ts` | 新增 | ApiError、NetworkError、TimeoutError 类；PaginationMeta、PaginatedResponse 类型 |
| `web/src/lib/schemas.ts` | 修改 | 新增 HealthResponseSchema；补充 EpisodeSchema |
| `web/src/lib/config.test.ts` | 新增 | 测试 config 读取环境变量（T-02） |
| `web/src/lib/schemas.test.ts` | 新增 | 测试 HealthResponseSchema 校验合法/非法数据（T-03、T-04） |

---

### Step 3：Core 层 — api-client fetch wrapper

- **关联测试**：T-05、T-06
- **目标文件**：`web/src/lib/api-client.ts`、`web/src/lib/api-client.test.ts`
- **前置条件**：Step 2 完成（依赖 types.ts 中的 ApiError、NetworkError、TimeoutError）
- **实现内容**：
  1. 创建 `web/src/lib/api-client.ts`：
     - `getBaseUrl()`：从 `NEXT_PUBLIC_API_URL` 环境变量读取，fallback `http://localhost:3001`
     - `apiFetch<T>(endpoint, config)`：封装 fetch，支持 query params、JSON body、超时控制（AbortController）、统一错误转换
     - 便捷方法 `api.get()`、`api.post()`
     - 错误处理：HTTP 非 2xx → `ApiError`；AbortError → `TimeoutError`；TypeError → `NetworkError`
  2. 编写 `web/src/lib/api-client.test.ts`（T-05、T-06）：
     - T-05：使用 `vi.fn()` mock 全局 `fetch`，验证 apiFetch 正确拼接 URL、携带 headers、返回 JSON
     - T-06：mock fetch 返回 500 response，验证抛出 ApiError 且 status/message 正确
- **验证方式**：✅ 已完成
  - 运行 `npm test -- src/lib/api-client.test.ts` 确认 T-05、T-06 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/lib/api-client.ts` | 新增 | fetch 封装：baseURL、timeout、错误转换、api.get/api.post 便捷方法 |
| `web/src/lib/api-client.test.ts` | 新增 | 测试 apiFetch 请求拼接和错误处理（T-05、T-06） |

---

### Step 4：Shared UI 层 — Button、Card、Container 组件

- **关联测试**：T-07、T-08
- **目标文件**：`web/src/components/ui/Button.tsx`、`web/src/components/ui/Card.tsx`、`web/src/components/ui/Container.tsx` 及其 CSS Module 文件
- **前置条件**：Step 1 完成（依赖 tokens.css 中的 CSS 变量）
- **实现内容**：
  1. 创建 `web/src/components/ui/Button.tsx` + `Button.module.css`：
     - Props：`variant`（`'primary' | 'secondary' | 'ghost'`）、`size`（`'sm' | 'md' | 'lg'`）、`disabled`、`onClick`、`className`、`children`
     - 使用 `<button>` 元素，通过 `data-variant` 和 `data-size` 属性配合 CSS Module 控制样式
     - 样式引用 tokens.css 变量（`var(--color-primary)` 等）
  2. 创建 `web/src/components/ui/Card.tsx` + `Card.module.css`：
     - Props：`children`、`className`、`as`（`'div' | 'article' | 'section'`，默认 `'div'`）
     - 渲染为可配置的语义化标签，样式包含背景色、边框、圆角、内边距
  3. 创建 `web/src/components/ui/Container.tsx` + `Container.module.css`：
     - Props：`children`、`className`、`maxWidth`（默认 `'960px'`）
     - 使用 `max-width` + `margin: 0 auto` 居中，支持响应式
  4. 创建 `web/src/components/ui/index.ts`：集中导出 Button、Card、Container
  5. 编写 `web/src/components/ui/Button.test.tsx`（T-07）：渲染 Button 各 variant，验证 `data-variant` 属性
  6. 编写 `web/src/components/ui/Container.test.tsx`（T-08）：渲染 Container 指定 maxWidth，验证 style.maxWidth
- **验证方式**：✅ 已完成
  - 运行 `npm test -- src/components/ui/Button.test.tsx` 确认 T-07 通过
  - 运行 `npm test -- src/components/ui/Container.test.tsx` 确认 T-08 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/components/ui/Button.tsx` | 新增 | 通用按钮组件（primary/secondary/ghost，sm/md/lg） |
| `web/src/components/ui/Button.module.css` | 新增 | Button 组件样式（引用 tokens 变量） |
| `web/src/components/ui/Card.tsx` | 新增 | 通用卡片容器（支持 as 多态标签） |
| `web/src/components/ui/Card.module.css` | 新增 | Card 组件样式 |
| `web/src/components/ui/Container.tsx` | 新增 | 通用布局容器（max-width 居中） |
| `web/src/components/ui/Container.module.css` | 新增 | Container 组件样式 |
| `web/src/components/ui/index.ts` | 新增 | Shared UI 统一导出 |
| `web/src/components/ui/Button.test.tsx` | 新增 | 测试 Button variant 渲染（T-07） |
| `web/src/components/ui/Container.test.tsx` | 新增 | 测试 Container maxWidth（T-08） |

---

### Step 5：Feature 层 — HomeScreen、PlayerScreen、DramaDetailScreen

- **关联测试**：T-09、T-10、T-11
- **目标文件**：`web/src/features/home/`、`web/src/features/player/`、`web/src/features/drama-detail/`
- **前置条件**：Step 2（依赖 config.ts）、Step 4（依赖 Container、Card 等 Shared UI 组件）
- **实现内容**：
  1. 创建 `web/src/features/home/HomeScreen.tsx`：
     - 从 `@/lib/config` 读取 `config.app.name`、`config.app.version`、`config.app.env`
     - 使用 Container + Card 布局：垂直居中，展示应用名称（h1）、版本号、环境标识
     - 底部 `<nav>` 区域包含两个 Next.js `<Link>`：指向 `/play/sample` 和 `/detail/sample`
  2. 创建 `web/src/features/home/index.ts`：导出 HomeScreen
  3. 创建 `web/src/features/player/PlayerScreen.tsx`：
     - Props：`dramaId: string`
     - 占位内容：标题 "播放页" + dramaId + "待实现" 提示
  4. 创建 `web/src/features/player/index.ts`：导出 PlayerScreen
  5. 创建 `web/src/features/drama-detail/DramaDetailScreen.tsx`：
     - Props：`dramaId: string`
     - 占位内容：标题 "详情页" + dramaId + "待实现" 提示
  6. 创建 `web/src/features/drama-detail/index.ts`：导出 DramaDetailScreen
  7. 编写 `web/src/features/home/HomeScreen.test.tsx`（T-09）：渲染 HomeScreen，验证应用名称、版本号、环境标识、两个 Link 存在于 DOM
  8. 编写 `web/src/features/player/PlayerScreen.test.tsx`（T-10）：传入 dramaId，验证渲染内容包含 dramaId
  9. 编写 `web/src/features/drama-detail/DramaDetailScreen.test.tsx`（T-11）：传入 dramaId，验证渲染内容包含 dramaId
- **验证方式**：✅ 已完成
  - 运行 `npm test -- src/features/home/HomeScreen.test.tsx` 确认 T-09 通过
  - 运行 `npm test -- src/features/player/PlayerScreen.test.tsx` 确认 T-10 通过
  - 运行 `npm test -- src/features/drama-detail/DramaDetailScreen.test.tsx` 确认 T-11 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/features/home/HomeScreen.tsx` | 新增 | 首页 Feature：展示应用信息 + 路由导航 |
| `web/src/features/home/index.ts` | 新增 | Home Feature 统一导出 |
| `web/src/features/player/PlayerScreen.tsx` | 新增 | 播放页 Feature 占位骨架 |
| `web/src/features/player/index.ts` | 新增 | Player Feature 统一导出 |
| `web/src/features/drama-detail/DramaDetailScreen.tsx` | 新增 | 详情页 Feature 占位骨架 |
| `web/src/features/drama-detail/index.ts` | 新增 | DramaDetail Feature 统一导出 |
| `web/src/features/home/HomeScreen.test.tsx` | 新增 | 测试 HomeScreen 渲染内容（T-09） |
| `web/src/features/player/PlayerScreen.test.tsx` | 新增 | 测试 PlayerScreen dramaId 展示（T-10） |
| `web/src/features/drama-detail/DramaDetailScreen.test.tsx` | 新增 | 测试 DramaDetailScreen dramaId 展示（T-11） |

---

### Step 6：Page 层 — 路由页面重构与新增

- **关联测试**：T-09、T-10、T-11（此步骤为页面层整合，验证依赖 Feature 组件测试覆盖）
- **目标文件**：`web/src/app/page.tsx`（重构）、`web/src/app/play/[id]/page.tsx`、`web/src/app/detail/[id]/page.tsx`
- **前置条件**：Step 5 完成（依赖 HomeScreen、PlayerScreen、DramaDetailScreen）
- **实现内容**：
  1. 重构 `web/src/app/page.tsx`：
     - 从直接渲染 `<h1>` + `<p>` 改为委托 `HomeScreen` 组件
     - 保持 Server Component（无 `'use client'`），纯 SSR 输出
     - 移除 `page.module.css` 引用（如有）
  2. 创建 `web/src/app/play/[id]/page.tsx`：
     - Server Component，接收 `params: Promise<{ id: string }>`
     - 渲染 `PlayerScreen` 组件，传入 `params.id`
     - 设置页面级 `metadata`（title: `播放 — ShortDrama`）
  3. 创建 `web/src/app/detail/[id]/page.tsx`：
     - Server Component，接收 `params: Promise<{ id: string }>`
     - 渲染 `DramaDetailScreen` 组件，传入 `params.id`
     - 设置页面级 `metadata`（title: `详情 — ShortDrama`）
  4. 删除 `web/src/app/page.module.css`（首页样式由 Feature 内部 CSS Module 或全局样式处理）
- **验证方式**：✅ 已完成
  - 运行 `npm run build` 确认构建成功
  - 运行 `npm test` 确认所有已有测试通过
  - 运行 `npm run dev`，浏览器访问 `/`、`/play/test123`、`/detail/test456`，验证三个路由正常渲染
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/app/page.tsx` | 修改 | 重构为委托 HomeScreen 渲染，移除内联 UI |
| `web/src/app/page.module.css` | 删除 | 首页样式迁移至 Feature 内部 |
| `web/src/app/play/[id]/page.tsx` | 新增 | 播放页路由：SSR 渲染 PlayerScreen |
| `web/src/app/detail/[id]/page.tsx` | 新增 | 详情页路由：SSR 渲染 DramaDetailScreen |

---

### Step 7：App Shell — loading、error、not-found + 环境变量配置

- **关联测试**：无独立测试（Next.js 框架内置页面，通过构建验证）
- **目标文件**：`web/src/app/loading.tsx`、`web/src/app/error.tsx`、`web/src/app/not-found.tsx`、`web/.env.example`
- **前置条件**：Step 6 完成（页面路由已就位）
- **实现内容**：
  1. 创建 `web/src/app/loading.tsx`：
     - 全局 Suspense fallback，展示 "加载中..." 占位文案
     - 使用 Container 组件居中布局
  2. 创建 `web/src/app/error.tsx`：
     - `'use client'` 组件
     - Props：`{ error: Error & { digest?: string }; reset: () => void }`
     - UI：`<main role="alert">`，展示 "页面出错了" 标题 + error.message + "重试" 按钮
  3. 创建 `web/src/app/not-found.tsx`：
     - Server Component，展示 404 信息："页面不存在" + 返回首页链接
  4. 创建 `web/.env.example`：
     - 内容：
       ```
       NEXT_PUBLIC_APP_NAME=ShortDrama
       NEXT_PUBLIC_APP_VERSION=0.1.0
       NEXT_PUBLIC_API_URL=http://localhost:3001
       ```
     - 注释说明各变量用途
- **验证方式**：✅ 已完成
  - 运行 `npm run build` 确认构建成功，无新增错误
  - 运行 `npm run dev`，访问 `/nonexistent` 验证 404 页面渲染
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/app/loading.tsx` | 新增 | 全局 Loading 骨架（Suspense fallback） |
| `web/src/app/error.tsx` | 新增 | 全局 Error Boundary（client component，含重试按钮） |
| `web/src/app/not-found.tsx` | 新增 | 404 页面（含返回首页链接） |
| `web/.env.example` | 新增 | 环境变量示例：APP_NAME、APP_VERSION、API_URL |

---

### Step 8：CLAUDE.md 更新与最终验证

- **关联测试**：全部测试
- **目标文件**：`web/CLAUDE.md`
- **前置条件**：Step 1-7 全部完成
- **实现内容**：
  1. 更新 `web/CLAUDE.md`：
     - 移除 "当前目录尚未看到可确认的 Web 工程配置文件" 和 "在未补充真实构建配置前，不要编造运行、测试、构建命令" 的过时说明
     - 补充架构说明：五层架构（Page → Feature → Shared UI → Core → Design System）的职责与约束
     - 补充命令约定：`npm run dev`（开发）、`npm run build`（构建）、`npm run lint`（lint）、`npm test`（测试）、`npm run test:watch`（测试监听）
     - 补充测试要求：Vitest + Testing Library；组件测试覆盖关键 UI；schemas.ts 和 api-client.ts 必须测试
     - 补充目录结构说明：features/、components/ui/、lib/、styles/ 的职责划分
     - 保留原有的技术约束和开发约定
  2. 运行全量测试：`npm test` 确认所有测试通过
  3. 运行 lint：`npm run lint` 确认无新增 lint 错误
  4. 运行 build：`npm run build` 确认 SSG/SSR 构建成功
- **验证方式**：✅ 已完成
  - 运行 `npm test` 确认所有测试通过
  - 运行 `npm run lint` 确认无 lint 错误
  - 运行 `npm run build` 确认构建成功
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/CLAUDE.md` | 修改 | 更新为实际架构、命令约定、测试要求、目录结构说明 |

## 依赖关系

```
Step 1: Design System (tokens + globals)
  │
  └──▶ Step 2: Core — types, schemas, config, test infra
         │
         ├──▶ Step 3: Core — api-client
         │
         └──▶ Step 4: Shared UI (Button, Card, Container)
                │
                └──▶ Step 5: Features (HomeScreen, Player, DramaDetail)
                       │
                       └──▶ Step 6: Pages (路由页面重构)
                              │
                              └──▶ Step 7: App Shell (loading, error, 404, .env)
                                     │
                                     └──▶ Step 8: CLAUDE.md + 最终验证
```

说明：
- Step 1 无前置依赖，最先执行
- Step 2 依赖 Step 1（layout 引入 styles/globals.css，globals.css 引入 tokens.css）
- Step 3 依赖 Step 2（api-client 使用 types.ts 中的错误类）
- Step 4 依赖 Step 1（CSS 变量），与 Step 2/3 可并行
- Step 5 依赖 Step 2（config.ts）和 Step 4（Shared UI 组件）
- Step 6 依赖 Step 5（Feature 组件）
- Step 7 依赖 Step 6（页面路由就位后添加 Shell）
- Step 8 依赖全部前序步骤完成

## 验证总览

- [x] 所有测试通过（`npm test`）
- [x] Build 成功（`npm run build`）
- [x] Lint 通过（`npm run lint`）
- [x] `npm run dev` 可访问 `/`，展示应用名称、版本号、环境标识、导航链接
- [x] `npm run dev` 可访问 `/play/test123`，展示 "播放页" + "test123"
- [x] `npm run dev` 可访问 `/detail/test456`，展示 "详情页" + "test456"
- [x] 访问不存在路由（`/nonexistent`）展示 404 页面
- [x] tokens.css 在浏览器中查看时包含完整 CSS 自定义属性
- [x] 暗色模式（`prefers-color-scheme: dark`）下颜色正确切换
- [x] web/CLAUDE.md 包含实际的构建/测试/开发命令和架构说明

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/styles/tokens.css` | 新增 | CSS 自定义属性（颜色、间距、圆角、字体、暗色模式） |
| `web/src/styles/globals.css` | 新增（迁移） | CSS reset + 基础全局样式 |
| `web/src/styles/tokens.test.ts` | 新增 | 验证 tokens.css 包含必需 CSS 变量（T-01） |
| `web/src/app/globals.css` | 删除 | 内容迁移至 styles/globals.css |
| `web/src/app/page.module.css` | 删除 | 首页样式迁移至 Feature 内部 |
| `web/src/app/layout.tsx` | 修改 | CSS 引入路径、lang="zh-CN"、metadata template |
| `web/src/app/page.tsx` | 修改 | 重构为委托 HomeScreen 渲染 |
| `web/src/app/play/[id]/page.tsx` | 新增 | 播放页路由 |
| `web/src/app/detail/[id]/page.tsx` | 新增 | 详情页路由 |
| `web/src/app/loading.tsx` | 新增 | 全局 Loading 骨架 |
| `web/src/app/error.tsx` | 新增 | 全局 Error Boundary |
| `web/src/app/not-found.tsx` | 新增 | 404 页面 |
| `web/src/lib/types.ts` | 新增 | ApiError、NetworkError、TimeoutError；PaginationMeta 等共享类型 |
| `web/src/lib/schemas.ts` | 修改 | 新增 HealthResponseSchema、EpisodeSchema |
| `web/src/lib/config.test.ts` | 新增 | 测试 config 读取环境变量（T-02） |
| `web/src/lib/schemas.test.ts` | 新增 | 测试 Schema 校验（T-03、T-04） |
| `web/src/lib/api-client.ts` | 新增 | fetch 封装（baseURL、timeout、错误处理） |
| `web/src/lib/api-client.test.ts` | 新增 | 测试 apiFetch 请求拼接与错误处理（T-05、T-06） |
| `web/src/components/ui/Button.tsx` | 新增 | 通用按钮组件（variant/size/disabled） |
| `web/src/components/ui/Button.module.css` | 新增 | Button 样式 |
| `web/src/components/ui/Button.test.tsx` | 新增 | 测试 Button variant 渲染（T-07） |
| `web/src/components/ui/Card.tsx` | 新增 | 通用卡片容器 |
| `web/src/components/ui/Card.module.css` | 新增 | Card 样式 |
| `web/src/components/ui/Container.tsx` | 新增 | 通用布局容器（max-width 居中） |
| `web/src/components/ui/Container.module.css` | 新增 | Container 样式 |
| `web/src/components/ui/Container.test.tsx` | 新增 | 测试 Container maxWidth（T-08） |
| `web/src/components/ui/index.ts` | 新增 | Shared UI 统一导出 |
| `web/src/features/home/HomeScreen.tsx` | 新增 | 首页 Feature（应用信息 + 导航） |
| `web/src/features/home/index.ts` | 新增 | Home Feature 导出 |
| `web/src/features/home/HomeScreen.test.tsx` | 新增 | 测试 HomeScreen 渲染（T-09） |
| `web/src/features/player/PlayerScreen.tsx` | 新增 | 播放页 Feature 骨架 |
| `web/src/features/player/index.ts` | 新增 | Player Feature 导出 |
| `web/src/features/player/PlayerScreen.test.tsx` | 新增 | 测试 PlayerScreen（T-10） |
| `web/src/features/drama-detail/DramaDetailScreen.tsx` | 新增 | 详情页 Feature 骨架 |
| `web/src/features/drama-detail/index.ts` | 新增 | DramaDetail Feature 导出 |
| `web/src/features/drama-detail/DramaDetailScreen.test.tsx` | 新增 | 测试 DramaDetailScreen（T-11） |
| `web/vitest.config.ts` | 新增 | Vitest 配置 |
| `web/tests/setup.ts` | 新增 | Testing Library 初始化 |
| `web/.env.example` | 新增 | 环境变量示例 |
| `web/package.json` | 修改 | 新增 zod、vitest、testing-library 等依赖 |
| `web/CLAUDE.md` | 修改 | 更新为实际架构、命令、测试要求 |
