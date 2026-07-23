# 基础库与基础能力 — Web

> 本文档定义 Web 端的基础库选型、集成方案与基础能力接入规范。

---

## 1. HTTP 客户端

### 1.1 fetch 封装

基于原生 `fetch` 封装统一的 HTTP 客户端，不引入额外的 HTTP 库（如 axios、ky），保持 bundle 体积最小。

```typescript
// lib/api/client.ts
import { ApiError, UnauthorizedError, NotFoundError } from './errors';

interface RequestConfig extends Omit<RequestInit, 'body'> {
  params?: Record<string, string | number | undefined>;
  body?: unknown;
  timeout?: number;
}

const DEFAULT_TIMEOUT = 15_000;

// base URL 从环境变量获取，禁止硬编码
function getBaseUrl(): string {
  if (typeof window === 'undefined') {
    // 服务端
    return process.env.API_BASE_URL || 'http://localhost:8080';
  }
  // 客户端：通过 Next.js rewrites 代理或环境变量
  return process.env.NEXT_PUBLIC_API_BASE_URL || '/api';
}

async function apiClient<T>(endpoint: string, config: RequestConfig = {}): Promise<T> {
  const { params, body, timeout = DEFAULT_TIMEOUT, ...init } = config;

  // 构建 URL，追加查询参数
  const url = new URL(`${getBaseUrl()}${endpoint}`);
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    });
  }

  // 设置超时
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  // 构建 Headers
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init.headers as Record<string, string>),
  };

  // 注入 Token（客户端从 Cookie/localStorage，服务端从 cookies()）
  const token = await getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(url.toString(), {
      ...init,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
      credentials: 'include', // 携带 Cookie
    });

    const data: unknown = await response.json().catch(() => null);

    if (!response.ok) {
      if (response.status === 401) {
        throw new UnauthorizedError();
      }
      if (response.status === 404) {
        throw new NotFoundError();
      }
      const message = (data as { message?: string })?.message ?? `请求失败 (${response.status})`;
      throw new ApiError(message, response.status);
    }

    return data as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError('请求超时，请检查网络后重试', 408, 'TIMEOUT');
    }
    if (error instanceof TypeError) {
      throw new ApiError('网络连接失败，请检查网络', 0, 'NETWORK_ERROR');
    }
    throw new ApiError('未知错误', 0, 'UNKNOWN');
  } finally {
    clearTimeout(timeoutId);
  }
}
```

### 1.2 请求/响应拦截

```typescript
// lib/api/client.ts（续）

// Token 管理
async function getAuthToken(): Promise<string | null> {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('access_token');
  }
  // 服务端从 Cookie 获取
  const { cookies } = await import('next/headers');
  const cookieStore = await cookies();
  return cookieStore.get('access_token')?.value ?? null;
}

// 日志（仅开发环境）
function logRequest(method: string, url: string, duration: number, status?: number) {
  if (process.env.NODE_ENV === 'development') {
    console.log(`[API] ${method} ${url} — ${status ?? 'error'} (${duration}ms)`);
  }
}

// 重试配置
interface RetryConfig {
  maxRetries: number;
  retryDelay: number;
  retryOn: number[]; // 需要重试的状态码
}

const defaultRetryConfig: RetryConfig = {
  maxRetries: 2,
  retryDelay: 1000,
  retryOn: [502, 503, 504], // 仅服务器错误重试
};

async function apiClientWithRetry<T>(
  endpoint: string,
  config: RequestConfig = {},
  retryConfig: Partial<RetryConfig> = {},
): Promise<T> {
  const retry = { ...defaultRetryConfig, ...retryConfig };
  let lastError: Error | null = null;

  for (let attempt = 0; attempt <= retry.maxRetries; attempt++) {
    try {
      return await apiClient<T>(endpoint, config);
    } catch (error) {
      lastError = error as Error;
      if (
        attempt < retry.maxRetries &&
        error instanceof ApiError &&
        retry.retryOn.includes(error.status)
      ) {
        await new Promise(resolve => setTimeout(resolve, retry.retryDelay * (attempt + 1)));
        continue;
      }
      throw error;
    }
  }
  throw lastError;
}

// 便捷方法
export const api = {
  get: <T>(endpoint: string, config?: RequestConfig) =>
    apiClientWithRetry<T>(endpoint, { ...config, method: 'GET' }),
  post: <T>(endpoint: string, body?: unknown, config?: RequestConfig) =>
    apiClientWithRetry<T>(endpoint, { ...config, method: 'POST', body }),
  put: <T>(endpoint: string, body?: unknown, config?: RequestConfig) =>
    apiClientWithRetry<T>(endpoint, { ...config, method: 'PUT', body }),
  delete: <T>(endpoint: string, config?: RequestConfig) =>
    apiClientWithRetry<T>(endpoint, { ...config, method: 'DELETE', ...config }),
};
```

**拦截器要点：**

1. Token 注入：自动从 Cookie（SSR）或 localStorage（CSR）读取并注入 Authorization Header
2. 错误处理：按状态码分类（401 跳登录、404 显示未找到、5xx 自动重试）
3. 超时处理：默认 15 秒，使用 AbortController 实现
4. 日志记录：开发环境下自动记录请求耗时和状态
5. 重试机制：仅对 5xx 服务器错误重试，指数退避

---

## 2. 数据校验

### 2.1 Schema 定义

所有 API 请求/响应数据、表单数据、URL 参数均通过 Zod Schema 校验。

```typescript
// lib/validation/video.schema.ts
import { z } from 'zod';

// 剧集基础信息
export const videoItemSchema = z.object({
  id: z.string().uuid(),
  title: z.string().min(1).max(100),
  coverUrl: z.string().url(),
  episodeCount: z.number().int().positive(),
  duration: z.number().int().positive(), // 秒
  tags: z.array(z.string()).default([]),
  playCount: z.number().int().nonnegative(),
  likeCount: z.number().int().nonnegative(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
});

export type VideoItem = z.infer<typeof videoItemSchema>;

// 分页响应
export const paginatedResponseSchema = <T extends z.ZodTypeAny>(itemSchema: T) =>
  z.object({
    data: z.array(itemSchema),
    total: z.number().int().nonnegative(),
    page: z.number().int().positive(),
    pageSize: z.number().int().positive(),
    hasMore: z.boolean(),
  });

export const videoListResponseSchema = paginatedResponseSchema(videoItemSchema);
export type VideoListResponse = z.infer<typeof videoListResponseSchema>;

// 剧集详情
export const videoDetailSchema = videoItemSchema.extend({
  description: z.string().max(2000),
  episodes: z.array(
    z.object({
      id: z.string().uuid(),
      title: z.string(),
      duration: z.number().int().positive(),
      videoUrl: z.string().url(),
      episodeNumber: z.number().int().positive(),
    })
  ),
  relatedVideos: z.array(videoItemSchema).max(20),
});

export type VideoDetail = z.infer<typeof videoDetailSchema>;

// 搜索参数
export const videoSearchParamsSchema = z.object({
  q: z.string().max(100).optional(),
  tag: z.string().optional(),
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().min(1).max(50).default(20),
  sort: z.enum(['latest', 'popular', 'trending']).default('latest'),
});

export type VideoSearchParams = z.infer<typeof videoSearchParamsSchema>;
```

### 2.2 类型推导

使用 `z.infer` 从 Schema 推导 TypeScript 类型，**不**手写重复的类型定义。

```typescript
// ✅ Schema 是唯一的数据源头
const userSchema = z.object({
  id: z.string(),
  nickname: z.string(),
  avatar: z.string().url(),
});
type User = z.infer<typeof userSchema>; // 自动推导

// ❌ 不要手写类型再同步 Schema
interface User {
  id: string;
  nickname: string;
  avatar: string;
}
// ❌ 再写一遍 Schema 容易不一致
```

校验并推导类型的最佳实践：

```typescript
// lib/api/video.ts
import { api } from './client';
import { videoDetailSchema, videoListResponseSchema, type VideoSearchParams } from '@/lib/validation/video.schema';

export async function fetchVideoList(params: VideoSearchParams) {
  const data: unknown = await api.get('/videos', { params });

  // parse 在不符合 Schema 时抛出 ZodError，fail fast
  return videoListResponseSchema.parse(data);
}

export async function fetchVideoById(id: string) {
  const data: unknown = await api.get(`/videos/${id}`);

  // safeParse 返回结果对象，不抛异常
  const result = videoDetailSchema.safeParse(data);
  if (!result.success) {
    console.error('Video data validation failed:', result.error.flatten());
    throw new Error('数据校验失败');
  }
  return result.data; // 这里类型已正确推导
}
```

### 2.3 服务端/客户端共享

Zod Schema 天然支持同构（isomorphic），定义在 `lib/validation/` 中，服务端和客户端均可引用。

- `lib/validation/` 中的 Schema 不包含任何服务端或客户端专属依赖
- Server Component、Client Component、Route Handler 均从同一份 Schema import
- API Route 中使用 Schema 校验请求体：

```typescript
// app/api/feedback/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';

const createFeedbackSchema = z.object({
  content: z.string().min(1).max(500),
  category: z.enum(['bug', 'feature', 'content', 'other']),
  contact: z.string().email().optional(),
});

export async function POST(request: NextRequest) {
  const body: unknown = await request.json();

  const result = createFeedbackSchema.safeParse(body);
  if (!result.success) {
    return NextResponse.json(
      { error: '请求参数不合法', details: result.error.flatten().fieldErrors },
      { status: 400 },
    );
  }

  // result.data 类型已正确推导
  const saved = await saveFeedback(result.data);
  return NextResponse.json(saved, { status: 201 });
}
```

---

## 3. 表单处理

### 3.1 React Hook Form

统一使用 React Hook Form 处理表单状态。

```bash
npm install react-hook-form @hookform/resolvers
```

```typescript
'use client';
import { useForm } from 'react-hook-form';

interface LoginFormData {
  phone: string;
  code: string;
}

export function LoginForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<LoginFormData>({
    defaultValues: { phone: '', code: '' },
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await sendLoginCode(data.phone);
    } catch {
      setError('phone', { message: '发送验证码失败，请稍后重试' });
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate>
      <label htmlFor="phone">手机号</label>
      <input
        id="phone"
        type="tel"
        {...register('phone', { required: '请输入手机号', pattern: { value: /^1\d{10}$/, message: '手机号格式不正确' } })}
      />
      {errors.phone && <p role="alert">{errors.phone.message}</p>}
      {/* ... */}
      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? '发送中...' : '获取验证码'}
      </button>
    </form>
  );
}
```

### 3.2 Zod 集成

```typescript
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const loginSchema = z.object({
  phone: z
    .string()
    .min(1, '请输入手机号')
    .regex(/^1\d{10}$/, '手机号格式不正确'),
  code: z
    .string()
    .length(6, '验证码为6位数字')
    .regex(/^\d{6}$/, '验证码格式不正确'),
});

type LoginFormData = z.infer<typeof loginSchema>;

// 使用 zodResolver
const { register, handleSubmit, formState } = useForm<LoginFormData>({
  resolver: zodResolver(loginSchema),
  defaultValues: { phone: '', code: '' },
});
```

### 3.3 错误展示

错误信息的 UI 展示规范：

- 每个字段的错误信息紧跟对应输入框下方
- 使用 `role="alert"` 确保无障碍
- 服务端校验错误通过 `setError` 设置到具体字段
- 通用错误（如网络异常）用 toast 提示

```typescript
// components/ui/FormField.tsx — 封装表单字段组件
import { type FieldError } from 'react-hook-form';
import clsx from 'clsx';

interface FormFieldProps {
  label: string;
  error?: FieldError;
  children: React.ReactNode;
  required?: boolean;
}

export function FormField({ label, error, children, required }: FormFieldProps) {
  return (
    <div className="form-field">
      <label className={clsx('form-label', required && 'form-label--required')}>
        {label}
      </label>
      {children}
      {error && (
        <p className="form-error" role="alert">
          {error.message}
        </p>
      )}
    </div>
  );
}
```

---

## 4. 国际化 (i18n)

### 4.1 翻译文件

使用 `next-intl` 管理国际化。

```bash
npm install next-intl
```

翻译文件结构：

```
messages/
  zh.json           # 中文（默认）
  en.json           # 英文
```

```json
// messages/zh.json
{
  "home": {
    "title": "推荐",
    "trending": "热门短剧",
    "loadMore": "加载更多"
  },
  "video": {
    "play": "播放",
    "episode": "第{episode}集",
    "like": "赞",
    "share": "分享"
  },
  "error": {
    "network": "网络连接失败，请检查网络",
    "notFound": "未找到该剧集",
    "retry": "重试"
  }
}
```

### 4.2 路由国际化

使用 `[locale]` 前缀方式组织路由：

```
app/
  [locale]/
    (home)/
      page.tsx          # /zh/home, /en/home
    video/
      [id]/
        page.tsx
    layout.tsx          # 带 i18n provider 的布局
  layout.tsx            # 根布局（设置 locale）
  middleware.ts         # 语言检测与重定向
```

```typescript
// middleware.ts
import createMiddleware from 'next-intl/middleware';

export default createMiddleware({
  locales: ['zh', 'en'],
  defaultLocale: 'zh',
  localeDetection: true, // 自动检测 Accept-Language
});

export const config = {
  matcher: ['/((?!api|_next|_vercel|.*\\..*).*)'],
};
```

```typescript
// app/[locale]/layout.tsx
import { NextIntlClientProvider } from 'next-intl';
import { getMessages } from 'next-intl/server';

export default async function LocaleLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  const messages = await getMessages();

  return (
    <NextIntlClientProvider messages={messages} locale={locale}>
      {children}
    </NextIntlClientProvider>
  );
}
```

### 4.3 日期/数字格式化

使用 `Intl` API 统一格式化：

```typescript
// lib/utils/format.ts

export function formatDate(isoString: string, locale = 'zh-CN'): string {
  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(isoString));
}

export function formatRelativeTime(isoString: string, locale = 'zh-CN'): string {
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });
  const diff = Date.now() - new Date(isoString).getTime();

  const units: { unit: Intl.RelativeTimeFormatUnit; ms: number }[] = [
    { unit: 'year', ms: 365 * 24 * 60 * 60 * 1000 },
    { unit: 'month', ms: 30 * 24 * 60 * 60 * 1000 },
    { unit: 'day', ms: 24 * 60 * 60 * 1000 },
    { unit: 'hour', ms: 60 * 60 * 1000 },
    { unit: 'minute', ms: 60 * 1000 },
  ];

  for (const { unit, ms } of units) {
    const value = Math.round(-diff / ms);
    if (Math.abs(value) >= 1) return rtf.format(value, unit);
  }
  return '刚刚';
}

export function formatNumber(num: number, locale = 'zh-CN'): string {
  return new Intl.NumberFormat(locale, {
    notation: num > 9999 ? 'compact' : 'standard',
  }).format(num);
}

export function formatDuration(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  return `${m}:${String(s).padStart(2, '0')}`;
}
```

---

## 5. 无障碍 (A11y)

### 5.1 语义化 HTML

- 使用 `<header>`、`<nav>`、`<main>`、`<section>`、`<article>`、`<footer>`、`<aside>` 等语义化标签替代 `<div>`
- 标题层级正确：h1 → h2 → h3，不跳级
- 每个页面有且仅有一个 `<h1>`
- `<form>` 中每个 input 都需要对应的 `<label htmlFor="...">`

```typescript
// ✅ 语义化页面结构
export function HomePage() {
  return (
    <>
      <header>
        <nav aria-label="主导航">
          <a href="/">首页</a>
        </nav>
      </header>
      <main>
        <h1>热门短剧</h1>
        <section aria-labelledby="trending-heading">
          <h2 id="trending-heading">今日热门</h2>
          {/* ... */}
        </section>
      </main>
      <footer>版权信息</footer>
    </>
  );
}
```

### 5.2 ARIA 属性

关键 ARIA 使用场景：

```typescript
// 对话框
<div role="dialog" aria-labelledby="dialog-title" aria-modal="true">
  <h2 id="dialog-title">确认删除</h2>
</div>

// 图标按钮（无可见文字）
<button aria-label="收藏">
  <HeartIcon />
</button>

// 加载状态
<div role="status" aria-live="polite">
  正在加载...
</div>

// 动态内容更新
<div aria-live="polite" aria-atomic="true">
  {/* 搜索结果数量变更时自动朗读 */}
</div>

// Tab 组件
<div role="tablist" aria-label="剧集分类">
  <button role="tab" aria-selected={active === 'all'} aria-controls="panel-all">
    全部
  </button>
</div>
<div role="tabpanel" id="panel-all" aria-labelledby="tab-all">
  {/* Tab 内容 */}
</div>
```

### 5.3 键盘导航

- 所有交互元素（按钮、链接、表单控件）可通过 Tab 键到达
- 自定义组件（如 Select、Modal）实现标准键盘行为：
  - Enter/Space：激活/选择
  - Escape：关闭弹窗/下拉
  - Arrow keys：列表/菜单导航
- `tabIndex={-1}` 用于需要编程式聚焦但不需要 Tab 到达的元素
- 焦点特性（focus trap）在 Modal/Dialog 中实现

```typescript
// hooks/useFocusTrap.ts
'use client';
import { useEffect, useRef } from 'react';

export function useFocusTrap(isActive: boolean) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isActive || !ref.current) return;

    const container = ref.current;
    const focusableSelector = 'a[href], button, textarea, input, select, [tabindex]:not([tabindex="-1"])';

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key !== 'Tab') return;

      const focusableElements = container.querySelectorAll<HTMLElement>(focusableSelector);
      const first = focusableElements[0];
      const last = focusableElements[focusableElements.length - 1];

      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last?.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first?.focus();
      }
    }

    container.addEventListener('keydown', handleKeyDown);
    return () => container.removeEventListener('keydown', handleKeyDown);
  }, [isActive]);

  return ref;
}
```

### 5.4 屏幕阅读器

- 图片必须有 `alt` 属性：信息性图片写描述，装饰性图片设 `alt=""`
- 纯装饰 SVG 图标加 `aria-hidden="true"`
- 使用 `aria-label` 或 `aria-labelledby` 为无文本的交互元素提供标签
- 色彩不可作为唯一的信息传达方式（图表、状态指示器）

```typescript
// ✅ 隐藏装饰元素
<span aria-hidden="true" className="decorative-icon">
  ⭐
</span>

// ✅ 跳过导航链接
<a href="#main-content" className="skip-link">
  跳过导航
</a>
```

---

## 6. 埋点与分析

### 6.1 事件命名

采用 `category_action` 格式，全部小写，用下划线分隔。

| 事件名 | 触发时机 | 参数 |
|--------|---------|------|
| `page_view` | 页面加载完成 | `page_name`, `page_url`, `referrer` |
| `video_play` | 开始播放剧集 | `video_id`, `video_title`, `source` |
| `video_pause` | 暂停播放 | `video_id`, `current_time`, `episode` |
| `video_complete` | 播放完一集 | `video_id`, `episode`, `total_duration` |
| `search_submit` | 提交搜索 | `keyword`, `result_count` |
| `button_click` | 按钮点击 | `button_name`, `page_name`, `context` |
| `share_click` | 分享操作 | `video_id`, `platform` |
| `login_start` | 开始登录 | `method`（phone/wechat/...） |
| `login_complete` | 登录成功 | `method`, `duration`（从开始到完成耗时） |
| `error_occurred` | 错误发生 | `error_type`, `error_message`, `page_name` |

### 6.2 自定义事件

```typescript
// lib/analytics/index.ts
type EventParams = Record<string, string | number | boolean | undefined>;

let analyticsProvider: ((event: string, params?: EventParams) => void) | null = null;

export function initAnalytics(send: (event: string, params?: EventParams) => void) {
  analyticsProvider = send;
}

export function trackEvent(event: string, params?: EventParams) {
  if (process.env.NODE_ENV === 'development') {
    console.log(`[Analytics] ${event}`, params);
  }
  analyticsProvider?.(event, {
    ...params,
    timestamp: Date.now(),
    app_version: process.env.NEXT_PUBLIC_APP_VERSION,
  });
}

// 页面浏览追踪 Hook
'use client';
import { useEffect, useRef } from 'react';
import { trackEvent } from '@/lib/analytics';

export function usePageView(pageName: string) {
  const tracked = useRef(false);

  useEffect(() => {
    if (tracked.current) return;
    tracked.current = true;

    trackEvent('page_view', {
      page_name: pageName,
      page_url: window.location.pathname,
      referrer: document.referrer,
    });
  }, [pageName]);
}
```

**触发时机原则：**

- 上报在关键动作**成功后**触发，不要预上报
- 避免在高频事件中上报（如 scroll、mousemove），需要时做节流处理
- 发送失败不影响主流程，静默失败不抛异常

### 6.3 隐私合规

- 埋点上报前必须获得用户同意（Cookie Consent / 隐私弹窗）
- 不在埋点中发送个人身份信息（PII）（真实姓名、手机号、邮箱等）
- `page_url` 中过滤查询参数，不发送 token、code 等敏感参数
- 用户选择退出后停止所有埋点上报

```typescript
// lib/analytics/index.ts（续）
let analyticsEnabled = false;

export function setAnalyticsEnabled(enabled: boolean) {
  analyticsEnabled = enabled;
}

export function trackEvent(event: string, params?: EventParams) {
  if (!analyticsEnabled) return; // 未授权不上报
  // ...
}

// 过滤敏感 URL 参数
function sanitizeUrl(url: string): string {
  try {
    const parsed = new URL(url);
    const sensitive = ['token', 'code', 'password', 'secret', 'access_token'];
    sensitive.forEach(param => parsed.searchParams.delete(param));
    return parsed.pathname + parsed.search;
  } catch {
    return url;
  }
}
```

---

## 7. SEO

### 7.1 Metadata

使用 Next.js `generateMetadata` API 为每个页面生成动态 meta 信息：

```typescript
// app/video/[id]/page.tsx
import type { Metadata } from 'next';

interface VideoPageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: VideoPageProps): Promise<Metadata> {
  const { id } = await params;
  const video = await fetchVideo(id);

  if (!video) {
    return { title: '未找到 — ShortDrama' };
  }

  return {
    title: `${video.title} — ShortDrama`,
    description: video.description.slice(0, 160),
    openGraph: {
      title: video.title,
      description: video.description.slice(0, 160),
      images: [{ url: video.coverUrl, width: 720, height: 1280 }],
      type: 'video.episode',
    },
    twitter: {
      card: 'summary_large_image',
      title: video.title,
      images: [video.coverUrl],
    },
  };
}
```

根 Layout 中的默认 Metadata：

```typescript
// app/layout.tsx
export const metadata: Metadata = {
  title: {
    default: 'ShortDrama — 竖屏短剧平台',
    template: '%s — ShortDrama',
  },
  description: '海量竖屏短剧，免费在线观看',
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL || 'https://shortdrama.com'),
  openGraph: {
    siteName: 'ShortDrama',
    type: 'website',
    locale: 'zh_CN',
  },
  robots: {
    index: true,
    follow: true,
  },
};
```

### 7.2 Sitemap

```typescript
// app/sitemap.ts
import type { MetadataRoute } from 'next';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = process.env.NEXT_PUBLIC_SITE_URL || 'https://shortdrama.com';

  // 动态获取所有剧集 ID
  const videoIds: string[] = await fetchAllVideoIds();

  const videoUrls: MetadataRoute.Sitemap = videoIds.map(id => ({
    url: `${baseUrl}/video/${id}`,
    lastModified: new Date(),
    changeFrequency: 'weekly',
    priority: 0.8,
  }));

  return [
    { url: baseUrl, lastModified: new Date(), changeFrequency: 'daily', priority: 1 },
    { url: `${baseUrl}/video/search`, lastModified: new Date(), changeFrequency: 'daily', priority: 0.9 },
    ...videoUrls,
  ];
}
```

```typescript
// app/robots.ts
import type { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  const baseUrl = process.env.NEXT_PUBLIC_SITE_URL || 'https://shortdrama.com';

  return {
    rules: { userAgent: '*', allow: '/', disallow: ['/api/', '/profile/'] },
    sitemap: `${baseUrl}/sitemap.xml`,
  };
}
```

### 7.3 结构化数据

使用 JSON-LD 格式添加结构化数据，帮助搜索引擎理解内容：

```typescript
// components/VideoStructuredData.tsx
export function VideoStructuredData({ video }: { video: VideoDetail }) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'TVSeries',
    name: video.title,
    description: video.description,
    image: video.coverUrl,
    numberOfEpisodes: video.episodeCount,
    url: `${process.env.NEXT_PUBLIC_SITE_URL}/video/${video.id}`,
  };

  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
    />
  );
}
```

- 剧集类型使用 `TVSeries` + `TVEpisode`
- 面包屑导航使用 `BreadcrumbList`
- 搜索功能使用 `SearchAction` Sitelinks Searchbox
