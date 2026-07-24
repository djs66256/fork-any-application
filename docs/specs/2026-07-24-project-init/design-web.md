# Web 端技术方案：项目初始化与架构设计

> 创建日期：2026-07-24
> 对应共享方案：design.md
> 对应需求：spec.md（Section 4.3 Web 前端分层架构、Section 6.5 US-05 Web 工程初始化）

---

## 1. 架构设计

### 1.1 整体架构：五层分层模型

```
┌─────────────────────────────────────────────────┐
│  Page 层（app/）                                  │
│  路由定义、页面级数据获取、元数据配置                 │
│  ├── /            → HomePage（SSR）              │
│  ├── /play/[id]   → PlayPage（SSR 占位）          │
│  └── /detail/[id] → DetailPage（SSR 占位）        │
├─────────────────────────────────────────────────┤
│  Feature 层（features/）                          │
│  按业务领域组织的页面组合逻辑                        │
│  ├── home/        → HomeScreen                   │
│  ├── player/      → PlayerScreen（骨架）          │
│  └── drama-detail/ → DramaDetailScreen（骨架）    │
├─────────────────────────────────────────────────┤
│  Shared UI 层（components/ui/）                   │
│  跨功能复用的通用 UI 组件                           │
│  ├── Button、Card、Container                      │
│  └── 后续按需扩展（Modal、Input、Skeleton 等）       │
├─────────────────────────────────────────────────┤
│  Core 层（lib/）                                  │
│  网络请求、配置、数据校验、类型定义                   │
│  ├── api-client.ts    fetch 封装                  │
│  ├── config.ts        环境变量管理                  │
│  ├── schemas.ts       Zod Schema（与 Backend 一致） │
│  └── types.ts         共享 TypeScript 类型          │
├─────────────────────────────────────────────────┤
│  Design System 层（styles/）                       │
│  CSS 自定义属性 + 全局样式                          │
│  ├── globals.css      全局 reset + 基础样式         │
│  └── tokens.css       设计 Token（颜色/间距/圆角等）  │
└─────────────────────────────────────────────────┘
```

### 1.2 与现有架构的关系

当前 Web 工程已通过 `create-next-app` 初始化，包含基础的 `app/layout.tsx`、`app/page.tsx` 和 `lib/config.ts`、`lib/schemas.ts`。本轮设计是在现有骨架基础上，按照五层架构重新组织目录结构并补齐各层骨架。

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `src/app/page.tsx` | 重构 | 从直接渲染改为委托 HomeScreen 组件 |
| `src/app/layout.tsx` | 修改 | 添加 Inter 字体、metadata 模板化、引入 tokens.css |
| `src/lib/config.ts` | 保持 | 已使用 `NEXT_PUBLIC_*` 环境变量，符合规范 |
| `src/lib/schemas.ts` | 扩展 | 补充 HealthResponseSchema（与 Backend 对齐） |
| `src/app/globals.css` | 拆分 | 拆分为 globals.css（reset） + tokens.css（变量） |

---

## 2. 核心文件变更

### 2.1 完整文件清单

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `web/src/app/layout.tsx` | 修改 | 更新 metadata、引入 tokens.css、lang="zh-CN" |
| `web/src/app/page.tsx` | 重构 | 改为委托 HomeScreen 组件，SSR 数据获取 |
| `web/src/app/play/[id]/page.tsx` | 新增 | 播放页骨架，SSR |
| `web/src/app/detail/[id]/page.tsx` | 新增 | 详情页骨架，SSR |
| `web/src/app/loading.tsx` | 新增 | 全局 Loading 骨架 |
| `web/src/app/error.tsx` | 新增 | 全局 Error Boundary |
| `web/src/app/not-found.tsx` | 新增 | 404 页面 |
| `web/src/features/home/HomeScreen.tsx` | 新增 | 首页 Feature 组件 |
| `web/src/features/home/index.ts` | 新增 | Feature 统一导出 |
| `web/src/features/player/PlayerScreen.tsx` | 新增 | 播放页 Feature 骨架 |
| `web/src/features/player/index.ts` | 新增 | Feature 统一导出 |
| `web/src/features/drama-detail/DramaDetailScreen.tsx` | 新增 | 详情页 Feature 骨架 |
| `web/src/features/drama-detail/index.ts` | 新增 | Feature 统一导出 |
| `web/src/components/ui/Button.tsx` | 新增 | 通用按钮组件 |
| `web/src/components/ui/Button.module.css` | 新增 | Button 样式 |
| `web/src/components/ui/Card.tsx` | 新增 | 通用卡片组件 |
| `web/src/components/ui/Card.module.css` | 新增 | Card 样式 |
| `web/src/components/ui/Container.tsx` | 新增 | 通用容器组件 |
| `web/src/components/ui/Container.module.css` | 新增 | Container 样式 |
| `web/src/components/ui/index.ts` | 新增 | UI 组件统一导出 |
| `web/src/lib/api-client.ts` | 新增 | fetch 封装，统一 base URL、headers、错误处理 |
| `web/src/lib/config.ts` | 保持 | 已存在，无需修改 |
| `web/src/lib/schemas.ts` | 扩展 | 新增 HealthResponseSchema（与 Backend 对齐） |
| `web/src/lib/types.ts` | 新增 | 共享 TypeScript 类型定义 |
| `web/src/styles/globals.css` | 新增（迁移） | CSS reset + 基础全局样式 |
| `web/src/styles/tokens.css` | 新增 | CSS 自定义属性（设计 Token） |
| `web/.env.example` | 新增 | 环境变量示例文件 |

### 2.2 目录结构总览（改造后）

```
web/
├── .env.example
├── src/
│   ├── app/
│   │   ├── layout.tsx              # 根布局（metadata、字体、全局样式引入）
│   │   ├── page.tsx                # / → HomePage
│   │   ├── loading.tsx             # 全局 Loading 骨架
│   │   ├── error.tsx               # 全局 Error Boundary
│   │   ├── not-found.tsx           # 404 页面
│   │   ├── play/
│   │   │   └── [id]/
│   │   │       └── page.tsx        # /play/[id] → PlayPage
│   │   └── detail/
│   │       └── [id]/
│   │           └── page.tsx        # /detail/[id] → DetailPage
│   ├── features/
│   │   ├── home/
│   │   │   ├── HomeScreen.tsx      # 首页 Feature 组件
│   │   │   └── index.ts
│   │   ├── player/
│   │   │   ├── PlayerScreen.tsx    # 播放页 Feature 骨架
│   │   │   └── index.ts
│   │   └── drama-detail/
│   │       ├── DramaDetailScreen.tsx # 详情页 Feature 骨架
│   │       └── index.ts
│   ├── components/
│   │   └── ui/
│   │       ├── Button.tsx
│   │       ├── Button.module.css
│   │       ├── Card.tsx
│   │       ├── Card.module.css
│   │       ├── Container.tsx
│   │       ├── Container.module.css
│   │       └── index.ts
│   ├── lib/
│   │   ├── api-client.ts           # fetch 封装
│   │   ├── config.ts               # 环境变量管理
│   │   ├── schemas.ts              # Zod Schema
│   │   └── types.ts                # 共享类型
│   └── styles/
│       ├── globals.css             # CSS reset + 基础样式
│       └── tokens.css              # 设计 Token
```

---

## 3. 组件设计

### 3.1 组件层级树

```
RootLayout
├── HomePage (/)
│   └── HomeScreen (features/home/)
│       ├── <h1> 应用名称（来自 config.app.name）
│       ├── <p> 版本号（来自 config.app.version）
│       ├── <p> 环境标识（来自 config.app.env）
│       └── 路由导航链接
│           ├── Link → /play/sample
│           └── Link → /detail/sample
├── PlayPage (/play/[id])
│   └── PlayerScreen (features/player/)
│       └── 占位内容："播放页 — 待实现"
└── DetailPage (/detail/[id])
    └── DramaDetailScreen (features/drama-detail/)
        └── 占位内容："详情页 — 待实现"
```

### 3.2 组件清单

| 组件名称 | 类型 | 文件路径 | 职责 | Props 接口 |
|---------|------|---------|------|-----------|
| HomeScreen | Feature | `features/home/HomeScreen.tsx` | 首页内容：展示应用信息 + 路由导航 | 无（数据从 lib/config 获取） |
| PlayerScreen | Feature | `features/player/PlayerScreen.tsx` | 播放页骨架占位 | `dramaId: string` |
| DramaDetailScreen | Feature | `features/drama-detail/DramaDetailScreen.tsx` | 详情页骨架占位 | `dramaId: string` |
| Button | Shared UI | `components/ui/Button.tsx` | 通用按钮 | `variant`, `size`, `children`, `onClick`, `disabled`, `className` |
| Card | Shared UI | `components/ui/Card.tsx` | 通用卡片容器 | `children`, `className`, `as?` |
| Container | Shared UI | `components/ui/Container.tsx` | 页面内容区最大宽度约束 | `children`, `className`, `maxWidth?` |

### 3.3 组件接口定义

```typescript
// features/home/HomeScreen.tsx
// 无外部 Props，数据从 config.ts 读取
export function HomeScreen(): React.ReactElement;

// features/player/PlayerScreen.tsx
interface PlayerScreenProps {
  dramaId: string;
}
export function PlayerScreen({ dramaId }: PlayerScreenProps): React.ReactElement;

// features/drama-detail/DramaDetailScreen.tsx
interface DramaDetailScreenProps {
  dramaId: string;
}
export function DramaDetailScreen({ dramaId }: DramaDetailScreenProps): React.ReactElement;

// components/ui/Button.tsx
type ButtonVariant = 'primary' | 'secondary' | 'ghost';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  disabled?: boolean;
  onClick?: () => void;
  className?: string;
  children: React.ReactNode;
}
export function Button(props: ButtonProps): React.ReactElement;

// components/ui/Card.tsx
interface CardProps {
  children: React.ReactNode;
  className?: string;
  as?: 'div' | 'article' | 'section';
}
export function Card({ children, className, as: Tag = 'div' }: CardProps): React.ReactElement;

// components/ui/Container.tsx
interface ContainerProps {
  children: React.ReactNode;
  className?: string;
  maxWidth?: string;
}
export function Container({ children, className, maxWidth }: ContainerProps): React.ReactElement;
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| Page → Feature | Props（`params.id`） | PlayPage、DetailPage 将动态路由参数传入 Feature 组件 |
| Feature → Shared UI | Props | HomeScreen 内使用 Container/Card 布局 |
| 全局配置 | 直接 import `config` | 应用名、版本号、环境标识等只读常量 |

当前初始化阶段数据流简单：页面组件从 URL 获取动态参数，通过 Props 传递给 Feature 组件。不涉及跨层级状态共享或 Context。

### 3.5 响应式设计

| 断点 | 宽度范围 | 布局策略 | 关键变化 |
|------|---------|---------|---------|
| Mobile | < 768px | 单列，Container 占满宽度 | 内容居中，padding 为 `--spacing-md` |
| Tablet | 768px - 1024px | 单列，Container 最大宽度 720px | 居中，两侧留白增大 |
| Desktop | > 1024px | 单列，Container 最大宽度 960px | 居中，两侧留白增大 |

使用 CSS Grid/Flexbox 实现自适应，`Container` 组件通过 `max-width` + `margin: 0 auto` 限制内容最大宽度。

### 3.6 无障碍（A11y）

| 关注点 | 策略 |
|--------|------|
| 语义化 HTML | 使用 `<main>`、`<nav>`、`<h1>` 等语义标签 |
| 键盘导航 | 所有链接和按钮可通过 Tab 键到达 |
| 屏幕阅读器 | RootLayout 设置 `lang="zh-CN"` |
| 色彩对比度 | 使用 CSS 变量管理颜色，确保 WCAG AA 对比度 |

---

## 4. 状态管理方案

### 4.1 方案选择

| 维度 | 选择 | 理由 |
|------|------|------|
| 全局状态 | 暂无（初始化阶段） | 当前无跨页面共享的客户端状态 |
| 服务端状态 | 暂无（初始化阶段） | 后续业务 PRD 引入 TanStack Query |
| 表单状态 | 无 | 初始化阶段无表单 |
| 路由状态 | Next.js App Router（内置） | 文件系统路由，框架原生支持 |
| URL 状态 | `params`（App Router） | 动态路由参数通过 `params` prop 获取 |

### 4.2 当前阶段状态划分

当前初始化阶段不使用任何客户端状态管理库。页面数据全部通过 Server Components 的 `async/await` 在服务端获取，或直接从 `lib/config.ts` 读取编译时常量。

```typescript
// app/page.tsx（Server Component）
import { config } from '@/lib/config';
import { HomeScreen } from '@/features/home';

export default function HomePage() {
  // 无异步数据获取，配置为编译时常量
  return <HomeScreen />;
}
```

### 4.3 后续演进

当业务 PRD 引入实际功能后，按以下顺序引入状态管理：

1. **TanStack Query**：管理从后端 API 获取的服务端状态（剧集列表、详情、搜索等）
2. **Zustand**：管理全局客户端状态（播放器状态、用户偏好等）
3. **React Hook Form + Zod**：管理表单状态

### 4.4 状态流转

```
用户访问 URL
       → Next.js App Router 匹配路由
       → Server Component 获取数据（config / fetch API）
       → 渲染 Feature 组件
       → 客户端水合（Hydration）
       → 用户交互（导航链接点击）
       → Next.js Link 客户端路由跳转
```

---

## 5. 路由设计

### 5.1 路由清单

| 路径 Pattern | 页面组件 | 参数 | 认证守卫 | 懒加载 | 渲染策略 | 说明 |
|-------------|---------|------|---------|--------|---------|------|
| `/` | HomePage | — | 否 | 否 | SSR | 首页，展示应用信息和导航 |
| `/play/[id]` | PlayPage | `id: string` | 否 | 否 | SSR | 播放页占位 |
| `/detail/[id]` | DetailPage | `id: string` | 否 | 否 | SSR | 详情页占位 |

### 5.2 路由层级

```
RootLayout（app/layout.tsx）
├── / → page.tsx（HomePage）
├── /play/[id] → play/[id]/page.tsx（PlayPage）
├── /detail/[id] → detail/[id]/page.tsx（DetailPage）
├── loading.tsx（全局 Suspense fallback）
├── error.tsx（全局 Error Boundary）
└── not-found.tsx（404 页面）
```

当前为单 Layout 的扁平路由结构，不涉及嵌套 Layout 或路由分组。后续业务扩展时可按需添加 `(home)`、`(auth)` 等路由分组。

### 5.3 路由导航

首页内使用 Next.js `<Link>` 组件进行客户端导航：

```typescript
// features/home/HomeScreen.tsx
import Link from 'next/link';

<nav>
  <Link href="/play/sample">播放页示例</Link>
  <Link href="/detail/sample">详情页示例</Link>
</nav>
```

### 5.4 数据预取

| 页面 | 预取方式 | 预取内容 |
|------|---------|---------|
| HomePage | 无需预取 | 配置为编译时常量 |
| PlayPage | 无需预取 | 占位页面，仅展示 `dramaId` |
| DetailPage | 无需预取 | 占位页面，仅展示 `dramaId` |

初始化阶段各页面不依赖异步 API 数据（除 HomePage 可选调用 `/api/health`），因此不涉及 `generateStaticParams` 或 `revalidate` 配置。

---

## 6. API 调用层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | 原生 `fetch` 封装 | 保持 bundle 体积最小，不引入 axios/ky |
| 请求拦截 | `api-client.ts` 内部 | Token 注入（预留）、超时控制 |
| 响应拦截 | `api-client.ts` 内部 | 统一错误码解析、状态码分类处理 |
| 响应校验 | Zod `parse` / `safeParse` | API 返回数据经 Schema 校验后使用 |

### 6.2 客户端封装

```typescript
// lib/api-client.ts

import { ApiError, NetworkError, TimeoutError } from './types';

const DEFAULT_TIMEOUT = 15_000;

function getBaseUrl(): string {
  return process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3001';
}

interface RequestConfig extends Omit<RequestInit, 'body'> {
  params?: Record<string, string | undefined>;
  body?: unknown;
  timeout?: number;
}

export async function apiFetch<T>(
  endpoint: string,
  config: RequestConfig = {},
): Promise<T> {
  const { params, body, timeout = DEFAULT_TIMEOUT, ...init } = config;

  const url = new URL(`${getBaseUrl()}${endpoint}`);
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined) url.searchParams.set(key, value);
    });
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init.headers as Record<string, string>),
  };

  try {
    const response = await fetch(url.toString(), {
      ...init,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      throw new ApiError(
        (errorBody as { message?: string }).message || `Request failed: ${response.status}`,
        response.status,
      );
    }

    const data: unknown = await response.json();
    return data as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new TimeoutError();
    }
    if (error instanceof TypeError) {
      throw new NetworkError();
    }
    throw new ApiError('Unknown error', 0);
  } finally {
    clearTimeout(timeoutId);
  }
}

// 便捷方法
export const api = {
  get: <T>(endpoint: string, config?: RequestConfig) =>
    apiFetch<T>(endpoint, { ...config, method: 'GET' }),
  post: <T>(endpoint: string, body?: unknown, config?: RequestConfig) =>
    apiFetch<T>(endpoint, { ...config, method: 'POST', body }),
};
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 2 | 指数退避（1s, 2s） | 仅对 5xx 服务端错误和网络错误重试 |
| 5xx 服务端错误 | 2 | 指数退避（1s, 2s） | 自动重试 |
| 4xx 客户端错误 | 0 | — | 不重试，直接抛出错误 |
| 401 Token 过期 | — | — | 当前无认证需求，预留 |

### 6.4 请求 Hook 封装

当前初始化阶段不封装通用 Hook。业务 PRD 阶段引入 TanStack Query 后，通过 `useQuery` / `useMutation` 管理服务端状态。

---

## 7. SSR / CSR 策略

### 7.1 渲染策略选择

| 页面 | 渲染策略 | 原因 |
|------|---------|------|
| HomePage（`/`） | SSR | SEO 友好，首次访问直接输出完整 HTML |
| PlayPage（`/play/[id]`） | SSR（默认） | 占位页面，后续根据数据获取方式调整 |
| DetailPage（`/detail/[id]`） | SSR（默认） | 占位页面，后续根据数据获取方式调整 |

### 7.2 框架支持

使用 Next.js App Router（v16），默认所有页面为 **Server Component**，仅在需要交互时下沉到 `'use client'` 的 Client Component。

初始化阶段所有页面均为 Server Component（无交互需求），`HomeScreen`、`PlayerScreen`、`DramaDetailScreen` 作为纯展示组件，不需要 `'use client'` 指令。

### 7.3 数据预取策略

| 页面 | 预取方法 | 预取数据 |
|------|---------|---------|
| HomePage | 无需预取 | `config` 为编译时常量 |
| PlayPage | 无需预取 | 仅展示 `params.id` |
| DetailPage | 无需预取 | 仅展示 `params.id` |

### 7.4 SEO 策略

```typescript
// app/layout.tsx — 根 Metadata
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: {
    default: 'ShortDrama',
    template: '%s — ShortDrama',
  },
  description: 'ShortDrama content platform',
};
```

后续业务页面通过 `generateMetadata` 为每个页面生成动态 meta 信息（标题、描述、Open Graph 等）。

---

## 8. 性能优化

### 8.1 优化清单

| 优化项 | 策略 | 目标 |
|--------|------|------|
| 代码分割 | 暂不需要 | 初始化阶段组件数量极少 |
| 图片优化 | 暂不需要 | 初始化阶段无图片资源 |
| 字体加载 | `next/font/google`（Geist） | 内置优化：子集化 + `font-display: swap` |
| 缓存策略 | 暂不需要 | 初始化阶段无动态数据 |
| Bundle 分析 | 后续引入 | 初始化阶段产出极小 |

### 8.2 加载体验

| 场景 | 策略 |
|------|------|
| 首屏加载 | SSR 直出 HTML，无异步数据依赖 |
| 路由切换 | Next.js `<Link>` prefetch + 客户端导航 |

---

## 9. 配置与环境

### 9.1 环境变量清单

| 配置项 | 环境变量 Key | 开发环境值 | 说明 |
|--------|-------------|----------|------|
| 应用名称 | `NEXT_PUBLIC_APP_NAME` | `ShortDrama` | 展示在首页和 `<title>` 中 |
| 应用版本 | `NEXT_PUBLIC_APP_VERSION` | `0.1.0` | 展示在首页 |
| API 地址 | `NEXT_PUBLIC_API_URL` | `http://localhost:3001` | Backend 服务地址 |
| 运行环境 | `NODE_ENV` | `development` | Next.js 内置，自动读取 |

### 9.2 .env.example

```bash
# .env.example
NEXT_PUBLIC_APP_NAME=ShortDrama
NEXT_PUBLIC_APP_VERSION=0.1.0
NEXT_PUBLIC_API_URL=http://localhost:3001
```

实际开发时复制为 `.env.local` 并根据需要调整值。

> 注意：禁止硬编码任何常量。所有环境相关配置通过环境变量管理，客户端可访问的变量以 `NEXT_PUBLIC_` 前缀。

---

## 10. API 调用清单

### 10.1 当前阶段 API 调用

| API 端点 | 方法 | 调用时机 | 调用位置 | 请求数据 | 成功后操作 | 错误处理 |
|---------|------|---------|---------|---------|-----------|---------|
| `/api/health` | GET | HomePage SSR（可选） | `app/page.tsx` | 无 | 展示服务状态 | 静默降级，不阻断页面渲染 |

### 10.2 API 调用细节

```typescript
// lib/schemas.ts — 与 Backend 保持一致的 Schema

import { z } from 'zod';

export const HealthResponseSchema = z.object({
  status: z.literal('ok'),
  timestamp: z.string(),
  version: z.string(),
});

export type HealthResponse = z.infer<typeof HealthResponseSchema>;

// 已有的 DramaSchema 保持不变
export const DramaSchema = z.object({
  id: z.string(),
  title: z.string().min(1),
  description: z.string(),
  coverUrl: z.string().url(),
  category: z.string(),
  episodeCount: z.number().int().positive(),
});

export type Drama = z.infer<typeof DramaSchema>;
```

### 10.3 后续阶段 API 扩展点

| 功能域 | 预期端点 | 说明 |
|--------|---------|------|
| 剧集列表 | `GET /api/dramas` | 分页列表，含筛选与排序 |
| 剧集详情 | `GET /api/dramas/:id` | 完整信息含分集列表 |
| 搜索 | `GET /api/dramas/search?q=` | 关键词搜索 |
| 播放 | `GET /api/episodes/:id/play` | 播放地址获取 |

以上端点待 Backend 实现后在 Web 端同步对接。

---

## 11. 跨端共享逻辑落地

### 11.1 与 design.md 的对应关系

| 共享逻辑 | design.md 定义 | Web 端实现方式 |
|---------|---------------|---------------|
| HealthResponse Schema | `{ status: 'ok', timestamp: string, version: string }` | `lib/schemas.ts` 中定义 `HealthResponseSchema`，与 Backend 完全一致 |
| Zod 校验 | 所有 API 响应经 Zod Schema 校验 | `api-client.ts` 配合 `schemas.ts`，响应数据 `parse` / `safeParse` 后使用 |
| 禁止硬编码 | 环境地址、token 等不得硬编码 | 全部通过 `NEXT_PUBLIC_*` 环境变量管理 |
| RESTful 约束 | API 设计遵循 RESTful | `api-client.ts` 封装 GET/POST/PUT/DELETE，路径拼接使用模板字符串 |

---

## 12. 边界与错误处理

### 12.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `api-client.ts` 内 `try/catch` | 统一错误码解析，分类抛出 `ApiError` / `NetworkError` / `TimeoutError` |
| UI 层 | `error.tsx`（React Error Boundary） | 捕获页面渲染错误，展示 fallback UI |
| 路由层 | `not-found.tsx` | 不存在的路由显示 404 页面 |

### 12.2 Error Boundary

```typescript
// app/error.tsx
'use client';

import { useEffect } from 'react';

interface ErrorBoundaryProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function ErrorBoundary({ error, reset }: ErrorBoundaryProps) {
  useEffect(() => {
    console.error('Page error:', error);
  }, [error]);

  return (
    <main role="alert">
      <h1>页面出错了</h1>
      <p>{error.message || '发生了未知错误'}</p>
      <button onClick={reset}>重试</button>
    </main>
  );
}
```

### 12.3 错误码映射表

| 后端错误码 | HTTP 状态 | 用户提示 | 交互方式 |
|-----------|----------|---------|---------|
| `NOT_FOUND` | 404 | "未找到请求的资源" | 展示 `not-found.tsx` |
| `INTERNAL_ERROR` | 500 | "服务器异常，请稍后重试" | Error Boundary fallback + 重试按钮 |
| `NETWORK_ERROR` | — | "网络连接失败，请检查网络" | 内联错误提示 + 重试 |
| `TIMEOUT` | 408 | "请求超时，请检查网络后重试" | 内联错误提示 + 重试 |

### 12.4 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| API 不可达 | fetch 抛出 TypeError | 静默降级，不阻断首页渲染 | 🟡 |
| SSR 渲染异常 | Server Component 中 throw | `error.tsx` 捕获 + 可重试 | 🔴 |
| 路由不存在 | 访问未定义路径 | `not-found.tsx` 展示 404 页面 | 🟡 |
| 水合不匹配 | 服务端/客户端 HTML 不一致 | 严格保证 SSR 输出确定性 | 🟡 |

### 12.5 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| HomePage | —（SSR 直出） | 展示应用信息 + 导航链接 | 不适用 | `error.tsx` 捕获渲染异常 | 不适用 |
| PlayPage | —（SSR 直出） | 展示占位内容 | 不适用 | `error.tsx` 捕获渲染异常 | 不适用 |
| DetailPage | —（SSR 直出） | 展示占位内容 | 不适用 | `error.tsx` 捕获渲染异常 | 不适用 |

---

## 13. 测试策略

### 13.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | 工具函数、数据校验（schemas.ts）、config.ts | >80% | Vitest |
| 组件测试 | HomeScreen、Button、Card 等关键组件的渲染和 Props | >60% | Vitest + Testing Library |

当前初始化阶段不涉及集成测试和 E2E 测试。后续业务 PRD 引入实际交互后再补充。

### 13.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|------|------|------|---------|
| 1 | HomeScreen 渲染 | — | 渲染 HomeScreen | 展示应用名称、版本号、环境标识、两个导航链接 | 组件 |
| 2 | config 读取环境变量 | `NEXT_PUBLIC_APP_NAME=TestApp` | 读取 `config.app.name` | 返回 `"TestApp"` | 单元 |
| 3 | HealthResponseSchema 校验合法数据 | `{ status: "ok", timestamp: "...", version: "1.0" }` | 调用 `HealthResponseSchema.parse()` | 解析成功 | 单元 |
| 4 | HealthResponseSchema 拒绝非法数据 | `{ status: "error" }` | 调用 `HealthResponseSchema.parse()` | 抛出 ZodError | 单元 |
| 5 | Button 渲染不同 variant | `variant="primary"` | 渲染 Button | 应用对应 CSS class | 组件 |
| 6 | Container 限制宽度 | `maxWidth="768px"` | 渲染 Container | `max-width: 768px` 生效 | 组件 |
| 7 | PlayPage 展示 dramaId | URL: `/play/test123` | 渲染 PlayPage | 页面展示 `"test123"` | 组件 |
| 8 | DetailPage 展示 dramaId | URL: `/detail/test456` | 渲染 DetailPage | 页面展示 `"test456"` | 组件 |

### 13.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| API 请求 | MSW 或 `vi.fn()` | 初始化阶段 API 调用极少，使用 `vi.fn()` 即可 |
| 浏览器 API | jsdom（Vitest 内置） | 无需额外配置 |
| 环境变量 | `vi.stubEnv()` | Vitest 内置环境变量 mock |
| Next.js 路由 | `next/navigation` mock | mock `useParams`、`Link` 等 |

### 13.4 测试配置

```typescript
// vitest.config.ts（后续创建）
import { defineConfig } from 'vitest/config';
import path from 'path';

export default defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./tests/setup.ts'],
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
```

---

## 14. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| zod | ^3.x | 数据校验（schemas.ts） | 与 Backend 共享 Schema；TypeScript-first，支持 `z.infer` 类型推导 |
| vitest | ^2.x | 单元测试/组件测试 | 与 Vite 生态一致，速度快，原生支持 TypeScript |
| @testing-library/react | ^16.x | React 组件测试 | 业界标准，强调测试用户行为而非实现细节 |
| @testing-library/jest-dom | ^6.x | DOM 断言扩展 | 提供 `toBeInTheDocument()` 等语义化断言 |
| jsdom | ^25.x | 测试环境 DOM 模拟 | Vitest 使用 jsdom 模拟浏览器环境 |

> 注意：zod 在 web/package.json 中尚未添加，需在本轮实现时安装。vitest 及相关测试依赖后续创建测试文件时安装。所有开源依赖需征得用户同意后添加（遵守根目录 `CLAUDE.md` 开发约束）。

---

## 15. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 目录重构导致现有代码引用失效 | Web 端 | 🟡 | 中 | 逐步迁移，保持 import 路径别名 `@/` 不变 | git revert |
| Backend API 不可达导致首页健康检查失败 | Web 首页 | 🟢 | 中 | `try/catch` 包裹健康检查调用，静默降级 | 移除健康检查调用 |
| CSS 变量浏览器兼容性 | Web 所有页面 | 🟢 | 低 | CSS 自定义属性在现代浏览器中广泛支持（>96%） | Polyfill（postcss-custom-properties） |

---

## 16. 参考资料

### 16.1 已查阅的文件

| 文件 | 关键内容 |
|------|---------|
| `CLAUDE.md`（根目录） | 项目定位、目录结构、开发约束、文档约定 |
| `PRODUCT.md` | 产品名称（ShortDrama）、技术标识 |
| `web/CLAUDE.md` | Web 端技术约束、架构约束、测试要求 |
| `web/package.json` | 当前依赖：next 16.2.11, react 19.2.4, typescript 5 |
| `web/src/lib/config.ts` | 环境变量管理：`NEXT_PUBLIC_APP_NAME`、`NEXT_PUBLIC_APP_VERSION` |
| `web/src/lib/schemas.ts` | 现有 DramaSchema |
| `web/src/app/layout.tsx` | RootLayout，metadata，next/font |
| `web/src/app/page.tsx` | 现有首页（需重构） |
| `backend/src/lib/schemas.ts` | HealthResponseSchema（Web 端需对齐） |
| `backend/src/app/api/health/route.ts` | `/api/health` 端点定义 |
| `backend/CLAUDE.md` | Backend 技术栈：TypeScript、Next.js、Zod、Redis、Supabase |

### 16.2 已查阅的规范文档

| 文档 | 相关章节 |
|------|---------|
| `web-development/references/standards/architecture.md` | 分层架构、路由设计、状态管理、数据请求、SSR/SSG/ISR 策略、错误处理 |
| `web-development/references/standards/coding-standards.md` | TypeScript 规范、React 规范、CSS 规范、文件命名、代码审查清单 |
| `web-development/references/standards/foundation.md` | HTTP 客户端、数据校验、表单处理、SEO |
| `feature-workflow/references/design-writing.md` | 设计文档撰写规范 |
| `feature-workflow/references/web-design/frontend-design.md` | Web 端设计 agent 定义 |
