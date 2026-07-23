# 代码规范 — Web

> 本文档定义 Web 端 TypeScript + Next.js + React 的完整编码规范。

---

## 1. TypeScript 编码规范

### 1.1 严格模式

`tsconfig.json` 必须启用以下严格模式选项：

```json
{
  "compilerOptions": {
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "exactOptionalPropertyTypes": false,
    "forceConsistentCasingInFileNames": true
  }
}
```

- `strict: true` 同时开启 `strictNullChecks`、`noImplicitAny`、`strictFunctionTypes` 等 8 个子选项
- `noUncheckedIndexedAccess` 要求访问数组/对象索引时必须处理 `undefined` 情况
- `exactOptionalPropertyTypes` 建议设为 `false`，否则 `{ name?: string }` 无法赋值 `{ name: string | undefined }`，与 React Props 的类型推导冲突

### 1.2 类型定义

**interface vs type：**

- 描述对象结构、Props、API 响应时，优先使用 `interface`
- 联合类型、交叉类型、映射类型、工具类型 使用 `type`
- `interface` 支持声明合并，适合需要扩展的场景

```typescript
// ✅ interface 用于对象结构
interface VideoCardProps {
  video: VideoItem;
  onPlay: (id: string) => void;
  className?: string;
}

// ✅ type 用于联合类型
type PlaybackStatus = 'idle' | 'loading' | 'playing' | 'paused' | 'error';
```

**泛型使用：**

- 泛型参数命名：单个参数用 `T`，多个参数用有意义的简写（`TData`、`TError`）
- 必要时使用 `extends` 约束泛型范围

```typescript
// ✅ 有意义的泛型名
interface ApiResponse<TData> {
  data: TData;
  code: number;
  message: string;
}

// ✅ extends 约束
function pick<T extends Record<string, unknown>, K extends keyof T>(
  obj: T,
  keys: K[]
): Pick<T, K> {
  // ...
}
```

**类型推导原则：**

- 函数返回值尽量依赖 TypeScript 推导，不显式标注（除非需要约束返回值）
- 变量类型如果能从初始化推导清晰，不额外标注
- 回调参数类型尽量从上下文推导

```typescript
// ✅ 推导返回值
function formatDuration(seconds: number) {
  const min = Math.floor(seconds / 60);
  const sec = seconds % 60;
  return `${min}:${sec.toString().padStart(2, '0')}`;
}

// ❌ 不需要的显式标注
const name: string = 'hello';
```

### 1.3 命名约定

| 类型 | 命名风格 | 示例 |
|------|---------|------|
| 组件 | PascalCase | `VideoPlayer`, `HomeFeed` |
| 函数/变量 | camelCase | `fetchVideoList`, `currentPage` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `API_BASE_URL` |
| 类型/interface | PascalCase | `VideoItem`, `PageProps` |
| 枚举成员 | UPPER_SNAKE_CASE | `PlaybackStatus.PLAYING` |
| 文件名（组件） | PascalCase | `VideoCard.tsx` |
| 文件名（工具/Hook） | camelCase | `useVideoPlayer.ts`, `formatDate.ts` |
| 目录名 | kebab-case | `video-detail/`, `user-profile/` |
| 私有字段/方法 | 不强制 `_` 前缀，TS `private` 已足够 | — |
| 布尔值 | `is*` / `has*` / `should*` 前缀 | `isLoading`, `hasError`, `shouldShow` |
| 事件处理函数 | `handle*` 前缀 | `handlePlay`, `handleSubmit` |
| Props 回调 | `on*` 前缀 | `onPlay`, `onClose` |

### 1.4 禁止 any

- **禁止使用 `any`**，ESLint 规则 `@typescript-eslint/no-explicit-any: "error"`
- 遇到无法确定类型的场景，按优先级使用：
  1. 定义正确的类型/泛型
  2. `unknown` + 类型守卫
  3. `Record<string, unknown>` 用于未知结构的对象

```typescript
// ❌ 禁止
function parseData(data: any) { /* ... */ }

// ✅ 使用 unknown + 类型守卫
function parseData(data: unknown): VideoItem[] {
  if (!Array.isArray(data)) {
    throw new Error('Expected array');
  }
  return data.filter(isVideoItem);
}

function isVideoItem(item: unknown): item is VideoItem {
  return typeof item === 'object' && item !== null && 'id' in item;
}
```

- ESLint 中通过注释禁用 `any` 需要明确注明理由

```typescript
// ✅ 特殊情况（如第三方库类型定义不完善），注明原因
// eslint-disable-next-line @typescript-eslint/no-explicit-any -- Upstream library missing types
const result: any = legacyLib.getData();
```

---

## 2. React 编码规范

### 2.1 组件定义

- **仅使用函数组件**，不使用 class component
- Props 类型使用 `interface` 定义，紧邻组件声明之前
- 使用 `React.FC` 或直接标注返回 `ReactNode`/`JSX.Element`，团队内统一即可（本项目推荐直接用函数声明 + 返回值推导）

```typescript
// ✅ 标准写法
interface VideoCardProps {
  video: VideoItem;
  onPlay?: (videoId: string) => void;
  className?: string;
  children?: React.ReactNode;
}

export function VideoCard({ video, onPlay, className, children }: VideoCardProps) {
  return (
    <article className={className}>
      <h3>{video.title}</h3>
      {children}
    </article>
  );
}
```

- `children` 必须显式声明在 Props 中，不要依赖隐式的 `React.FC<PropsWithChildren>`
- 需要 `forwardRef` 时，用 `React.forwardRef` 包裹

```typescript
interface VideoPlayerProps {
  src: string;
  autoPlay?: boolean;
}

export const VideoPlayer = React.forwardRef<HTMLVideoElement, VideoPlayerProps>(
  function VideoPlayer({ src, autoPlay = false }, ref) {
    return <video ref={ref} src={src} autoPlay={autoPlay} />;
  }
);
```

- Props 默认值使用解构默认值，不使用 `defaultProps`

### 2.2 Hooks 规范

**useState：**

```typescript
// ✅ lazy initializer 用于计算开销大的初始值
const [items, setItems] = useState<VideoItem[]>(() => {
  const stored = localStorage.getItem('recent');
  return stored ? JSON.parse(stored) : [];
});

// ✅ 函数式更新用于依赖旧 state
setItems(prev => [...prev, newItem]);
```

**useEffect：**

- 明确依赖数组，不遗漏依赖
- 清理副作用（订阅、计时器、事件监听）
- 避免在 useEffect 中执行同步状态更新导致额外渲染
- 非必要不使用 `useEffect` — 优先在事件处理函数或 Server Components 中处理数据

```typescript
// ✅ 包含清理
useEffect(() => {
  const timer = setInterval(() => updatePlaybackTime(), 1000);
  return () => clearInterval(timer);
}, [updatePlaybackTime]);
```

**useCallback / useMemo：**

- 仅在以下场景使用：
  - `useCallback`：函数作为子组件 Props 传递，且子组件使用了 `React.memo`
  - `useMemo`：计算结果开销大，或引用稳定性影响下游 Hooks 的依赖
- 不要无差别包裹，过早优化的成本高于收益

**useRef：**

- 用于引用 DOM 节点或存储不触发重渲染的可变值
- 不要将 `useRef` 用于渲染逻辑中需要的数据

### 2.3 自定义 Hook

- 命名必须以 `use` 开头
- 职责单一，一个 Hook 只做一件事
- 返回值使用元组（类似 `useState`）或对象（属性较多时）

```typescript
// ✅ 对象返回值（属性多时）
function useVideoPlayer(src: string) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [status, setStatus] = useState<PlaybackStatus>('idle');

  const play = useCallback(() => videoRef.current?.play(), []);
  const pause = useCallback(() => videoRef.current?.pause(), []);

  return { videoRef, status, play, pause };
}

// ✅ 元组返回值（属性少时）
function useToggle(initial = false) {
  const [on, setOn] = useState(initial);
  const toggle = useCallback(() => setOn(prev => !prev), []);
  return [on, toggle] as const;
}
```

### 2.4 Server vs Client Component

**Server Component（默认）：**

- 直接使用 `async/await` 获取数据，无需 `useEffect`
- 不包含交互逻辑（事件处理、Hooks）
- 可以导入 Client Component，反之不行

**Client Component：**

- 文件顶部必须加 `'use client'` 指令
- 需要交互、Hooks、浏览器 API 时使用
- 尽量把交互逻辑下沉到叶子组件，保持父组件为 Server Component

```typescript
'use client';

// ✅ Client Component：包含交互
export function LikeButton({ videoId, initialLikes }: { videoId: string; initialLikes: number }) {
  const [likes, setLikes] = useState(initialLikes);
  return <button onClick={() => setLikes(l => l + 1)}>{likes} 赞</button>;
}
```

**边界原则：**

- Server Component → 可渲染 Client Component（通过 Props 传递数据）
- Client Component → 不可渲染 Server Component（但可通过 `children` prop 接收 Server Component 内容）
- 需要交互的组件才标记 `'use client'`，不要随意在顶层 Layout 标记

---

## 3. CSS/样式规范

### 3.1 CSS Modules

- 使用 `*.module.css` 文件命名
- Next.js 内置支持，无需额外配置
- className 绑定：

```typescript
// VideoCard.module.css
// .container { ... }
// .title { ... }

import styles from './VideoCard.module.css';

export function VideoCard() {
  return (
    <div className={styles.container}>
      <h3 className={styles.title}>标题</h3>
    </div>
  );
}
```

- 组合多个 className 时使用 `clsx`：

```typescript
import clsx from 'clsx';

<div className={clsx(styles.base, isActive && styles.active, className)} />
```

### 3.2 Tailwind CSS

- 类名顺序约定：Layout → Spacing → Typography → Visual → Misc
- 示例：`flex items-center gap-4 px-6 py-3 text-sm font-medium text-white bg-blue-500 rounded-lg hover:bg-blue-600`
- 自定义 theme 统一在 `tailwind.config.ts` 中扩展：

```typescript
// tailwind.config.ts
import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#...',
          500: '#...',
          900: '#...',
        },
      },
      spacing: {
        '18': '4.5rem',
      },
    },
  },
};
export default config;
```

- 响应式断点使用 Next.js 默认的 Tailwind 断点：`sm`(640) / `md`(768) / `lg`(1024) / `xl`(1280) / `2xl`(1536)
- 避免在 className 中写 `!important`，使用 Tailwind 的 `!` 前缀语法（如 `!text-red-500`）

### 3.3 设计 Token

- 使用 CSS 自定义属性（CSS Variables）统一管理设计 Token
- 定义在 `:root` 或主题类名下
- 示例结构：

```css
/* src/styles/tokens.css */
:root {
  /* 颜色 */
  --color-primary: #FF4D4F;
  --color-primary-hover: #FF7875;
  --color-bg-primary: #FFFFFF;
  --color-bg-secondary: #F5F5F5;
  --color-text-primary: #1A1A1A;
  --color-text-secondary: #666666;
  --color-border: #E8E8E8;
  --color-danger: #FF4D4F;

  /* 间距 */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  /* 圆角 */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-full: 9999px;

  /* 字号 */
  --font-size-xs: 12px;
  --font-size-sm: 14px;
  --font-size-base: 16px;
  --font-size-lg: 18px;
  --font-size-xl: 24px;

  /* 阴影 */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1);

  /* Z-Index */
  --z-dropdown: 100;
  --z-modal: 200;
  --z-toast: 300;
}

/* 暗色主题 */
[data-theme='dark'] {
  --color-bg-primary: #1A1A1A;
  --color-bg-secondary: #2A2A2A;
  --color-text-primary: #FFFFFF;
  --color-text-secondary: #AAAAAA;
  --color-border: #3A3A3A;
}
```

- 在 `globals.css` 中 import，Tailwind theme 中引用这些 CSS 变量以保持一致性
- 组件中优先使用 CSS 变量而非硬编码颜色/间距值

---

## 4. 文件与目录命名规范

### 4.1 组件文件

- 组件文件使用 **PascalCase** 命名：`VideoCard.tsx`、`HomeFeed.tsx`
- 每个文件只导出一个主组件
- 组件专属的子组件、样式、类型文件放在同目录下：

```
components/
  VideoCard/
    VideoCard.tsx          # 主组件
    VideoCard.module.css   # 样式
    VideoCard.types.ts     # 类型定义（如果复杂）
    index.ts               # 统一导出
```

- 目录内的 `index.ts` 用于统一导出，简化 import 路径

### 4.2 页面文件

遵循 Next.js App Router 文件约定：

| 文件 | 用途 |
|------|------|
| `page.tsx` | 页面内容（Server Component 默认） |
| `layout.tsx` | 布局容器，嵌套生效，状态保持 |
| `loading.tsx` | Suspense fallback，页面加载时的骨架屏 |
| `error.tsx` | Error Boundary，页面级错误处理 |
| `not-found.tsx` | 404 页面 |
| `route.ts` | API Route Handler（GET / POST 等） |
| `middleware.ts` | 根目录下，全局请求拦截 |

目录结构示例：

```
app/
  (home)/                    # 路由分组（不影响 URL）
    page.tsx                 # / → 首页
    layout.tsx               # 首页布局
  video/
    [id]/
      page.tsx               # /video/:id → 剧集详情
      loading.tsx
      error.tsx
    search/
      page.tsx               # /video/search → 搜索页
  profile/
    layout.tsx               # /profile/* 共享布局
    page.tsx                 # /profile → 个人中心
    history/
      page.tsx               # /profile/history → 观看历史
  api/
    video/
      route.ts               # /api/video
```

### 4.3 工具文件

- 工具函数文件使用 **camelCase** 命名：`formatDate.ts`、`apiClient.ts`
- 按功能分类放置：

```
lib/
  api/
    client.ts                # HTTP 请求封装
    video.ts                 # 视频相关 API
    auth.ts                  # 认证相关 API
  utils/
    formatDate.ts
    formatDuration.ts
    debounce.ts
  validation/
    video.schema.ts          # Zod Schema
    user.schema.ts
  hooks/
    useVideoPlayer.ts
    useInfiniteScroll.ts
  types/
    video.ts                 # 共享类型定义
    api.ts
```

---

## 5. 代码审查清单

提交 PR 前确认：

**类型安全：**

- [ ] 无 `any` 类型（或已注明合理理由）
- [ ] 所有 Props 有明确的 interface 定义
- [ ] API 响应数据经过 Zod 校验并推导类型
- [ ] `noUncheckedIndexedAccess` 下无类型错误

**React 规范：**

- [ ] 组件为函数组件，职责清晰
- [ ] `'use client'` 仅在必要时标记，且位于文件首行
- [ ] `useEffect` 依赖数组完整，含清理逻辑
- [ ] 自定义 Hook 命名以 `use` 开头
- [ ] 未使用 `useCallback`/`useMemo` 做不必要的优化

**样式规范：**

- [ ] CSS Modules 文件名正确（`*.module.css`）
- [ ] 无内联样式（除动态计算值外）
- [ ] 响应式布局已覆盖主要断点
- [ ] 设计 Token 使用 CSS 变量，非硬编码

**文件规范：**

- [ ] 文件名符合命名约定
- [ ] 组件有 `index.ts` 统一导出
- [ ] 导入使用路径别名（如 `@/components/VideoCard`）

**通用：**

- [ ] 无硬编码的环境地址、Token、密钥
- [ ] 错误状态有 UI 反馈（Loading、Empty、Error 三态）
- [ ] 无 console.log 残留
- [ ] ESLint 和 TypeScript 编译无错误
