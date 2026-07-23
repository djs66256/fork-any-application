# 架构设计 — Web

> 本文档定义 Web 端的整体架构设计规范。

---

## 1. 整体架构

```
┌─────────────────────────────────────────────┐
│           Pages（路由与数据获取层）              │
│  Server Components（SSR/SSG/ISR）             │
│  Client Components（'use client' leaf）       │
├─────────────────────────────────────────────┤
│           Components（UI 展示层）               │
│  纯展示组件 / 通用组件 / 业务组件                  │
├─────────────────────────────────────────────┤
│           Hooks（状态与逻辑层）                   │
│  自定义 Hook / 数据请求 / 业务逻辑                  │
├─────────────────────────────────────────────┤
│           Services（服务与数据访问层）             │
│  API 封装 / Zod 校验 / 数据转换                   │
├─────────────────────────────────────────────┤
│           Backend API                         │
│  RESTful API（由 backend/ 维护）                │
└─────────────────────────────────────────────┘
```

### 各层职责详细说明

**Pages 层（`app/` 目录）：**

- 负责路由定义、页面级数据获取、元数据配置
- 默认使用 Server Component，只在需要交互时下沉到 Client Component
- 不应包含复杂的 UI 逻辑或状态管理

**Components 层（`components/` 目录）：**

- 纯 UI 展示组件，通过 Props 接收数据
- 分为三类：
  - `ui/`：通用 UI 组件（Button、Modal、Input 等）
  - `layout/`：布局组件（Header、Sidebar、BottomNav 等）
  - `features/`：业务组件（VideoCard、PlaylistPanel、CommentList 等）
- 业务组件可包含局部状态，但不应直接调用 API

**Hooks 层（`hooks/` 目录）：**

- 封装组件逻辑、数据请求、副作用管理
- 自定义 Hook 是 Component 与 Service 之间的桥梁
- 一个 Hook 负责一个独立的功能领域

**Services 层（`lib/` 目录）：**

- 封装所有后端通信逻辑
- 统一错误处理、请求/响应拦截
- 数据出入前经过 Zod Schema 校验
- 不持有 UI 状态，纯函数或 Class

---

## 2. 路由设计

### 2.1 目录结构

```
app/
├── (home)/                    # 路由分组：首页相关
│   ├── layout.tsx             # 首页专属布局
│   └── page.tsx               # /
├── (auth)/                    # 路由分组：认证相关
│   ├── login/
│   │   └── page.tsx           # /login
│   └── register/
│       └── page.tsx           # /register
├── video/
│   ├── [id]/                  # 并行路由：剧集详情 + 评论区
│   │   ├── @player/
│   │   │   └── page.tsx       # /video/:id 的播放器部分
│   │   ├── @comments/
│   │   │   └── page.tsx       # /video/:id 的评论区
│   │   ├── layout.tsx         # 并行路由的 Layout
│   │   └── page.tsx           # 默认子路由
│   └── search/
│       └── page.tsx           # /video/search
├── profile/
│   ├── layout.tsx             # /profile/* 共享布局
│   ├── page.tsx               # /profile
│   ├── history/
│   │   └── page.tsx           # /profile/history
│   └── settings/
│       └── page.tsx           # /profile/settings
├── api/                       # API Routes
│   └── ...
├── layout.tsx                 # 根布局
├── loading.tsx                # 根 loading
├── error.tsx                  # 根 error boundary
├── not-found.tsx              # 404
└── global-error.tsx           # 根布局错误时的 fallback
```

### 2.2 动态路由

- `[id]`：单个动态段，如 `/video/123`
- `[...slug]`：通配符（catch-all），匹配多级路径
- `[[...slug]]`：可选通配符（optional catch-all），含父路径本身

```typescript
// app/video/[id]/page.tsx
interface VideoPageProps {
  params: Promise<{ id: string }>;
}

export default async function VideoPage({ params }: VideoPageProps) {
  const { id } = await params;
  const video = await fetchVideo(id);
  return <VideoDetail video={video} />;
}

// 静态生成提前声明
export async function generateStaticParams() {
  const videos = await fetchTrendingVideoIds();
  return videos.map(id => ({ id }));
}
```

### 2.3 中间件

`middleware.ts` 放在项目根目录（与 `app/` 同级），作用于所有路由：

```typescript
// middleware.ts
import { NextRequest, NextResponse } from 'next/server';

export function middleware(request: NextRequest) {
  const token = request.cookies.get('session')?.value;
  const { pathname } = request.nextUrl;

  // 需要登录的路由
  const protectedPaths = ['/profile', '/settings'];
  const isProtected = protectedPaths.some(p => pathname.startsWith(p));

  if (isProtected && !token) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('redirect', pathname);
    return NextResponse.redirect(loginUrl);
  }

  // 已登录用户访问登录/注册页，重定向到首页
  if (token && (pathname === '/login' || pathname === '/register')) {
    return NextResponse.redirect(new URL('/', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    // 排除静态资源和 API Routes
    '/((?!api|_next/static|_next/image|favicon.ico).*)',
  ],
};
```

中间件典型用途：
- 认证鉴权与路由守卫
- 国际化语言检测与重定向
- A/B 测试分流
- Bot 检测与防护

---

## 3. 状态管理

### 3.1 URL State

浏览器地址栏中的状态，适用于分页、筛选、搜索关键词等需要可分享/书签的状态。

```typescript
// app/video/search/page.tsx
interface SearchPageProps {
  searchParams: Promise<{ q?: string; page?: string; tag?: string }>;
}

export default async function SearchPage({ searchParams }: SearchPageProps) {
  const { q = '', page = '1', tag = '' } = await searchParams;
  const results = await searchVideos({ query: q, page: Number(page), tag });
  // ...
}
```

Client Component 中使用 `useSearchParams`（需 `'use client'`）。如需类型安全的 URL 参数管理，使用 `nuqs`：

```typescript
'use client';
import { useQueryState } from 'nuqs';

export function SearchInput() {
  const [query, setQuery] = useQueryState('q', { defaultValue: '' });
  return <input value={query} onChange={e => setQuery(e.target.value)} />;
}
```

### 3.2 Server State

服务端数据（API 响应）使用 **TanStack Query (React Query)** 管理，负责缓存、重新请求、乐观更新。

```typescript
'use client';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchVideos } from '@/lib/api/video';

export function useVideoList(category: string) {
  return useQuery({
    queryKey: ['videos', category],
    queryFn: () => fetchVideos({ category }),
    staleTime: 5 * 60 * 1000,        // 5 分钟内视为新鲜
    gcTime: 30 * 60 * 1000,           // 30 分钟后清除缓存
    retry: 2,                          // 失败重试 2 次
  });
}

export function useLikeVideo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (videoId: string) => likeVideo(videoId),
    // 乐观更新
    onMutate: async (videoId) => {
      // 取消进行中的查询
      await queryClient.cancelQueries({ queryKey: ['video', videoId] });
      // 保存旧数据
      const previous = queryClient.getQueryData<VideoDetail>(['video', videoId]);
      // 乐观更新
      if (previous) {
        queryClient.setQueryData(['video', videoId], {
          ...previous,
          isLiked: true,
          likeCount: previous.likeCount + 1,
        });
      }
      return { previous };
    },
    onError: (_err, videoId, context) => {
      // 回滚
      if (context?.previous) {
        queryClient.setQueryData(['video', videoId], context.previous);
      }
    },
    onSettled: (_data, _error, videoId) => {
      // 重新请求最新数据
      queryClient.invalidateQueries({ queryKey: ['video', videoId] });
    },
  });
}
```

### 3.3 Client State

仅存在于客户端的 UI 状态（弹窗开关、主题、播放器状态等），按作用域分层：

| 作用域 | 方案 | 示例 |
|--------|------|------|
| 组件内部 | `useState` / `useReducer` | 表单输入、展开/收起 |
| 父子组件 | Props 传递 / 组合 | 列表项选中状态 |
| 页面级 | React Context | 页面主题、筛选面板状态 |
| 全局 | Zustand（轻量场景）/ Context | 用户设置、全局播放器状态 |

Context 与 Zustand 的选择原则：
- 状态变化频率高、消费组件多 → Zustand（Selector 精确订阅，避免无关重渲染）
- 状态稳定、少量消费组件 → React Context

```typescript
// stores/player.ts — Zustand
import { create } from 'zustand';

interface PlayerState {
  currentVideoId: string | null;
  status: 'idle' | 'playing' | 'paused';
  volume: number;
  play: (videoId: string) => void;
  pause: () => void;
  setVolume: (volume: number) => void;
}

export const usePlayerStore = create<PlayerState>((set) => ({
  currentVideoId: null,
  status: 'idle',
  volume: 1,
  play: (videoId) => set({ currentVideoId: videoId, status: 'playing' }),
  pause: () => set({ status: 'paused' }),
  setVolume: (volume) => set({ volume: Math.max(0, Math.min(1, volume)) }),
}));
```

### 3.4 Form State

使用 **React Hook Form + Zod**，Provider 组合在 Client Component 中：

```typescript
'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const feedbackSchema = z.object({
  content: z.string().min(1, '请输入反馈内容').max(500, '反馈内容不超过500字'),
  contact: z.string().email('请输入有效的邮箱').optional().or(z.literal('')),
  category: z.enum(['bug', 'feature', 'content', 'other']),
});

type FeedbackFormData = z.infer<typeof feedbackSchema>;

export function FeedbackForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FeedbackFormData>({
    resolver: zodResolver(feedbackSchema),
    defaultValues: { content: '', contact: '', category: 'other' },
  });

  const onSubmit = async (data: FeedbackFormData) => {
    await submitFeedback(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <textarea {...register('content')} />
      {errors.content && <p role="alert">{errors.content.message}</p>}
      {/* ... */}
    </form>
  );
}
```

---

## 4. 数据请求

### 4.1 Server Components

Server Component 中直接使用 `fetch`，Next.js 自动去重、缓存与重新验证。

```typescript
// app/video/[id]/page.tsx (Server Component)
async function fetchVideo(id: string): Promise<VideoDetail> {
  const res = await fetch(`${apiBaseUrl}/videos/${id}`, {
    // 默认：force-cache（静态）
    // next: { revalidate: 60 },  // ISR: 60 秒后重新验证
    // cache: 'no-store',         // SSR: 每次请求都重新获取
  });

  if (!res.ok) {
    if (res.status === 404) throw new NotFoundError();
    throw new ApiError(`Failed to fetch video: ${res.status}`);
  }

  const json: unknown = await res.json();
  return videoDetailSchema.parse(json); // Zod 校验
}
```

Server Component 的优势：
- 数据请求在服务端执行，减少客户端 JavaScript 体积
- 直接访问数据库、文件系统等后端资源（无需额外 API 层）
- 同一页面内多个 fetch 自动去重

### 4.2 Client Components

Client Component 中通过 TanStack Query 管理数据请求，不直接在 `useEffect` 中 fetch。

```typescript
'use client';
import { useQuery } from '@tanstack/react-query';

export function VideoList({ category }: { category: string }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['videos', category],
    queryFn: () => fetchVideos(category),
  });

  if (isLoading) return <VideoListSkeleton />;
  if (error) return <ErrorFallback error={error} />;
  return /* ... */;
}
```

### 4.3 Server Actions

Next.js Server Actions 用于表单提交、数据变更等场景（**注意：当前项目后端 API 为 RESTful 设计，Server Actions 仅在前端直接操作数据库时使用，现阶段以 API Route 或 Service 层调用为主**）。

```typescript
// app/actions/feedback.ts
'use server';

import { z } from 'zod';
import { revalidatePath } from 'next/cache';

const feedbackSchema = z.object({
  content: z.string().min(1).max(500),
  category: z.enum(['bug', 'feature', 'content', 'other']),
});

export async function submitFeedback(formData: FormData) {
  const parsed = feedbackSchema.safeParse({
    content: formData.get('content'),
    category: formData.get('category'),
  });

  if (!parsed.success) {
    return { error: parsed.error.flatten().fieldErrors };
  }

  // 调用后端 API
  await apiPost('/feedback', parsed.data);
  revalidatePath('/feedback');
  return { success: true };
}
```

Client 端使用 `useActionState`：

```typescript
'use client';
import { useActionState } from 'react';
import { submitFeedback } from '@/app/actions/feedback';

export function FeedbackForm() {
  const [state, formAction] = useActionState(submitFeedback, {});
  // ...
}
```

---

## 5. SSR / SSG / ISR 策略

### 5.1 SSR（Dynamic Rendering）

每次请求都重新渲染，适用于个性化内容、实时数据。

```typescript
// 方式 1：使用动态函数（cookies、headers、searchParams）自动触发
import { cookies } from 'next/headers';

export default async function Page() {
  const token = (await cookies()).get('session')?.value;
  // 自动变为 dynamic
}

// 方式 2：显式声明
export const dynamic = 'force-dynamic';

// 方式 3：fetch 时禁用缓存
const res = await fetch(url, { cache: 'no-store' });
```

### 5.2 SSG（Static Generation）

构建时生成静态页面，适用于不频繁变化的内容（如关于页、帮助页）。

```typescript
// 默认行为：不带动态函数的页面 + fetch 走缓存
// 显式声明
export const dynamic = 'force-static';
```

### 5.3 ISR（Incremental Static Regeneration）

基于时间的重新验证：

```typescript
// 页面级配置（秒）
export const revalidate = 3600; // 每小时重新验证

// fetch 级配置
const res = await fetch(url, { next: { revalidate: 60 } });
```

按需重新验证（On-demand Revalidation）：

```typescript
// app/api/revalidate/route.ts
import { revalidatePath, revalidateTag } from 'next/cache';
import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  const { tag, path } = await request.json();

  if (path) revalidatePath(path);
  if (tag) revalidateTag(tag);

  return NextResponse.json({ revalidated: true });
}
```

渲染策略选择指南：

| 场景 | 策略 | 原因 |
|------|------|------|
| 首页、推荐列表 | ISR（revalidate: 60-300） | 平衡新鲜度与性能 |
| 剧集详情页 | SSG + ISR | 内容不变，偶尔更新缩略图 |
| 个人中心、观看历史 | SSR | 用户相关，无法预生成 |
| 搜索页 | SSR | 查询参数驱动，无限组合 |
| 登录/注册页 | SSG | 纯静态，无动态数据 |

---

## 6. 错误处理

### 6.1 Error Boundary

`error.tsx` 捕获同级和子级 `page.tsx` 的运行时错误：

```typescript
'use client'; // error.tsx 必须是 Client Component

import { useEffect } from 'react';

interface ErrorBoundaryProps {
  error: Error & { digest?: string };
  reset: () => void; // 重新渲染
}

export default function ErrorBoundary({ error, reset }: ErrorBoundaryProps) {
  useEffect(() => {
    // 可选：上报错误到监控服务
    console.error('Page error:', error);
  }, [error]);

  return (
    <div role="alert">
      <h2>页面出错了</h2>
      <p>{error.message}</p>
      <button onClick={reset}>重试</button>
    </div>
  );
}
```

### 6.2 全局错误

`global-error.tsx` 在最外层捕获错误（包括 RootLayout 的错误），必须包含 `<html>` 和 `<body>` 标签：

```typescript
'use client';

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html>
      <body>
        <h1>应用发生严重错误</h1>
        <button onClick={reset}>重试</button>
      </body>
    </html>
  );
}
```

### 6.3 Not Found

```typescript
// app/video/[id]/page.tsx
import { notFound } from 'next/navigation';

export default async function VideoPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const video = await fetchVideo(id);

  if (!video) {
    notFound(); // 触发最近的 not-found.tsx
  }

  return <VideoDetail video={video} />;
}

// app/video/[id]/not-found.tsx
export default function VideoNotFound() {
  return <h2>未找到该剧集</h2>;
}
```

### 6.4 API 错误

统一错误处理策略：

```typescript
// lib/api/errors.ts
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public code?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export class NotFoundError extends ApiError {
  constructor(message = 'Resource not found') {
    super(message, 404, 'NOT_FOUND');
    this.name = 'NotFoundError';
  }
}

export class UnauthorizedError extends ApiError {
  constructor(message = 'Unauthorized') {
    super(message, 401, 'UNAUTHORIZED');
    this.name = 'UnauthorizedError';
  }
}

// lib/api/client.ts — 统一 fetch 封装
async function apiFetch<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, options);

  if (res.status === 401) {
    // token 过期，跳转登录
    throw new UnauthorizedError();
  }

  if (res.status === 404) {
    throw new NotFoundError();
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(
      (body as { message?: string }).message || `Request failed: ${res.status}`,
      res.status,
    );
  }

  return res.json() as Promise<T>;
}
```

UI 层错误提示使用 toast：

```typescript
// hooks/useApiCall.ts
'use client';
import { useState } from 'react';
import { toast } from '@/components/ui/toast';

export function useApiCall<TArgs extends unknown[], TResult>(
  fn: (...args: TArgs) => Promise<TResult>,
) {
  const [isLoading, setIsLoading] = useState(false);

  const execute = async (...args: TArgs): Promise<TResult | undefined> => {
    setIsLoading(true);
    try {
      return await fn(...args);
    } catch (error) {
      if (error instanceof ApiError) {
        toast.error(error.message);
      } else {
        toast.error('网络异常，请稍后重试');
      }
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return { execute, isLoading };
}
```
