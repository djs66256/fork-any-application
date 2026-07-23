# 常见问题 — Web

> 本文档收集 Web 开发中的常见问题与解决方案。

---

## 构建问题

### Q1: `Type error: Cannot find module '@/components/...'`

**现象**：构建时报错，找不到路径别名 `@/` 对应的模块。

**原因**：TypeScript 或 Next.js 未正确配置路径别名。

**解决方案**：

```json
// tsconfig.json
{
  "compilerOptions": {
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

确认 `src/` 目录存在且目标文件也在其中。如果路径别名已配置但仍报错，检查：

1. 文件是否实际存在（区分大小写）
2. `tsconfig.json` 是否在 `include` 中包含 `src/`
3. 重启 TypeScript 服务（VS Code: `Cmd+Shift+P` → "TypeScript: Restart TS server"）

---

### Q2: `Error: error: React.Children.only expected to receive a single React element child`

**现象**：编译通过但运行时抛出上述错误。

**原因**：某个将 `children` 作为直接 ReactNode 渲染的组件收到了多个子元素或文本节点。

**解决方案**：检查报错组件中 `children` 的渲染方式。

```tsx
// ❌ 错误：隐式假设 children 是单个元素
function Layout({ children }: { children: React.ReactNode }) {
  return <div>{React.Children.only(children)}</div>;
}

// ✅ 正确：接受多个 children
function Layout({ children }: { children: React.ReactNode }) {
  return <div>{children}</div>;
}
```

如需严格单元素限制，确保调用方没有多余的空格/换行：

```tsx
// ❌ 产生了额外的文本节点（换行）
<Layout>
  <Page />
</Layout>

// ✅ 紧贴标签（或使用 fragment）
<Layout><Page /></Layout>
```

---

### Q3: `Module not found: Can't resolve 'fs'`

**现象**：在浏览器端代码中 import Node.js 模块（如 `fs`、`path`、`crypto`）时报错。

**原因**：服务端模块被 import 到了 Client Component 中。

**解决方案**：

1. 确认文件顶部是否有 `'use client'` 指令，如果没有，检查间接 import 链中是否被 Client Component 导入
2. 如果必须使用 Node 模块的逻辑，将其改为 Server Component 或 API Route 调用
3. 检查 `next.config.ts` 中 `webpack` 配置是否错误地将服务端模块打包到客户端

```tsx
// ❌ Client Component 中调用服务端逻辑
'use client';
import { readFile } from 'fs';

// ✅ 通过 API Route 间接访问
'use client';
async function loadData() {
  const res = await fetch('/api/load-file');
  return res.json();
}
```

---

### Q4: `Export encountered errors` — TypeScript 编译通过但 Next.js build 失败

**现象**：`tsc --noEmit` 通过，但 `next build` 失败。

**原因**：常见原因是页面级组件导出不符合 Next.js 规范，或 layout/page 文件中有非法的导出。

**解决方案**：

1. 确保 `page.tsx` / `layout.tsx` 有 default export
2. 检查是否有接口类型需要在文件顶部重新导出但未标注 `export type`
3. 确认 `generateStaticParams`、`generateMetadata` 等命名导出拼写正确
4. 检查 Next.js 版本是否支持当前使用的 API（如 `params` 从 `Promise` 类型变为直接访问的版本差异）

---

## 运行时问题

### Q5: `Hydration failed because the initial UI does not match what was rendered on the server`

**现象**：浏览器 Console 中看到 hydration mismatch 警告或错误，页面可能闪烁或样式异常。

**原因**：服务端渲染的 HTML 与客户端首次渲染不一致。

**常见触发场景：**

1. 在渲染逻辑中使用了 `typeof window !== 'undefined'` 导致分支
2. 使用了 `Date.now()`、`Math.random()` 等非确定性值
3. 渲染了 `<time>` 元素但没有 `suppressHydrationWarning`
4. HTML 嵌套不规范（如 `<p>` 内嵌套 `<div>`）

**解决方案：**

```tsx
// ❌ 错误：SSR 和 CSR 产生不同结果
function Timestamp() {
  return <time>{new Date().toISOString()}</time>;
}

// ✅ 方案 1：使用 useEffect 延迟到客户端渲染
function Timestamp() {
  const [time, setTime] = useState<string>('');
  useEffect(() => {
    setTime(new Date().toISOString());
  }, []);
  if (!time) return <time suppressHydrationWarning>...</time>;
  return <time>{time}</time>;
}

// ✅ 方案 2：抑制 hydration warning（确定差异无害时）
function Timestamp() {
  return <time suppressHydrationWarning>{new Date().toISOString()}</time>;
}
```

```tsx
// ❌ 错误：条件性渲染依赖 window
function MobileOnly() {
  const isMobile = typeof window !== 'undefined' && window.innerWidth < 768;
  return isMobile ? <MobileView /> : <DesktopView />;
}

// ✅ 正确：使用 CSS Media Query 或 useEffect
function ResponsiveView() {
  const [isMobile, setIsMobile] = useState(false);
  useEffect(() => {
    setIsMobile(window.innerWidth < 768);
  }, []);
  // 服务端渲染一致的输出，客户端再切换
  if (isMobile === null) return null;
  return isMobile ? <MobileView /> : <DesktopView />;
}
```

---

### Q6: `Unhandled Runtime Error: Cannot read properties of undefined (reading 'xxx')`

**现象**：页面崩溃，报错 `TypeError: Cannot read properties of undefined`。

**原因**：访问了未定义对象的属性，常见于：
1. API 响应结构与 Zod Schema 不匹配导致 `parse` 抛异常未捕获
2. 数组/对象索引越界（`noUncheckedIndexedAccess` 为 false 时不会 TypeScript 报错）
3. 异步数据到达前渲染组件

**解决方案：**

```tsx
// ❌ 危险：假设 data 一定存在
function VideoTitle({ data }: { data?: VideoItem }) {
  return <h1>{data.title}</h1>; // data 为 undefined 时崩溃
}

// ✅ 安全：先做空值守卫
function VideoTitle({ data }: { data?: VideoItem }) {
  if (!data) return <h1>加载中...</h1>;
  return <h1>{data.title}</h1>;
}

// ✅ 使用 optional chaining + 默认值
function VideoTitle({ data }: { data?: VideoItem }) {
  return <h1>{data?.title ?? '未知标题'}</h1>;
}

// ✅ 使用 ErrorBoundary 捕获并提供 fallback
```

强类型 + 严格模式防护：

```json
// tsconfig.json 中开启
{
  "compilerOptions": {
    "noUncheckedIndexedAccess": true
  }
}
```

```typescript
// 访问数组元素时必须处理 undefined
const firstVideo = videos[0]; // 类型为 VideoItem | undefined
if (!firstVideo) return <EmptyState />;
```

---

## React 问题

### Q7: `useEffect` 无限循环

**现象**：页面卡死或浏览器崩溃，`useEffect` 不断重复执行。

**原因**：`useEffect` 的依赖数组中包含了每次渲染都会变化的值。

**解决方案：**

```tsx
// ❌ 错误：每次渲染都创建新对象/数组
function useFetch() {
  const [data, setData] = useState(null);
  useEffect(() => {
    fetchData({ page: 1 }).then(setData);
  }, [{ page: 1 }]); // 每次都是新对象！
}

// ✅ 正确：提取为稳定的引用
function useFetch() {
  const [data, setData] = useState(null);
  const params = useMemo(() => ({ page: 1 }), []); // stable reference
  useEffect(() => {
    fetchData(params).then(setData);
  }, [params]);
}

// ✅ 更好：使用基本类型作为依赖
function useFetch(page: number) {
  const [data, setData] = useState(null);
  useEffect(() => {
    fetchData({ page }).then(setData);
  }, [page]); // page 是基本类型，按值比较
}
```

```tsx
// ❌ 错误：在 effect 中 setState 触发渲染 → effect 再次执行
function BadCounter() {
  const [count, setCount] = useState(0);
  useEffect(() => {
    setCount(count + 1); // 触发死循环
  }, [count]);
}

// ✅ 正确：使用函数式更新避免依赖
function GoodCounter() {
  const [count, setCount] = useState(0);
  useEffect(() => {
    setCount(prev => prev + 1); // 不依赖外部 count
  }, []); // 空依赖，只执行一次
}
```

---

### Q8: 状态更新后立即读取值仍是旧值

**现象**：`setState` 后下一行 console.log 仍然显示旧值。

**原因**：React 的状态更新是异步批处理的，当前渲染周期内读取的是闭包中的旧值。

**解决方案：**

```tsx
// ❌ 错误：期望 setState 同步生效
function handleSubmit() {
  setSubmitting(true);
  console.log(submitting); // 仍然是 false
  // ...
}

// ✅ 正确 1：使用新值直接操作（不依赖 state）
function handleSubmit() {
  setSubmitting(true);
  doSubmit(); // 不需要读 submitting 状态
}

// ✅ 正确 2：使用 useEffect 监听状态变化
useEffect(() => {
  if (submitting) {
    doSubmit().finally(() => setSubmitting(false));
  }
}, [submitting]);

// ✅ 正确 3：使用 useRef 进行同步读取
function handleSubmit() {
  isSubmittingRef.current = true;
  // ...
}
```

---

### Q9: `useCallback` / `useMemo` 没有实际优化效果

**现象**：使用了 `useCallback` 包裹函数，但子组件仍频繁重渲染。

**原因**：

1. 子组件没有 `React.memo`，props 变化是否重渲染取决于父组件是否重渲染
2. 依赖数组中的某个值本身不稳定（如对象/函数每次重新创建）
3. `useMemo` 用于简单的计算（如 `a + b`），开销比重新计算还大

**解决方案：**

```tsx
// ❌ 无效优化：子组件未 memo
function Parent() {
  const handleClick = useCallback(() => { /* ... */ }, []);
  return <Child onClick={handleClick} />; // Child 未 memo，父组件更新它仍会渲染
}

// ✅ 有效：useCallback 配合 React.memo
const Child = React.memo(function Child({ onClick }: { onClick: () => void }) {
  return <button onClick={onClick}>Click</button>;
});

function Parent() {
  const handleClick = useCallback(() => { /* ... */ }, []);
  return <Child onClick={handleClick} />;
}

// ✅ 不需要 useCallback 的场景：
// 1. 函数只传递给原生 HTML 元素（<button onClick={fn}>）
// 2. 没有性能问题的简单组件
```

---

### Q10: `error.tsx` 无法捕获特定页面错误

**现象**：页面抛出异常但 `error.tsx` 没有生效。

**原因**：

1. `error.tsx` 放在错误的位置（它只能捕获同级和子级的 `page.tsx`）
2. `error.tsx` 缺少 `'use client'` 指令
3. Layout 中抛出的错误由父级 `error.tsx` 捕获，不是同级

**解决方案：**

```
app/
  layout.tsx          # 根布局错误 → global-error.tsx
  error.tsx           # 捕获 page.tsx 和子路由的错误
  video/
    layout.tsx        # video 布局错误 → app/error.tsx
    error.tsx         # 捕获 video/page.tsx 和 video/*/page.tsx 的错误
    [id]/
      page.tsx
```

确保 `error.tsx` 文件顶部有：

```tsx
'use client'; // 必须
```

---

## 样式问题

### Q11: CSS Modules 样式不生效

**现象**：使用 `styles.className` 不显示样式。

**原因**：

1. 文件名不是 `*.module.css`（缺少 `.module`）
2. 类名命名使用了大驼峰但 CSS 中是小写
3. CSS Modules 需要 Next.js 内置支持，但文件路径或 import 方式不正确

**解决方案：**

```tsx
// ✅ 正确：文件名必须是 *.module.css
import styles from './VideoCard.module.css';

// ✅ 类名在 CSS 中保持 kebab-case
// CSS: .card-title { ... }
<div className={styles['card-title']}></div>

// ✅ 或使用 camelCase（CSS Modules 自动转换）
// CSS: .cardTitle { ... }
<div className={styles.cardTitle}></div>
```

如果 `styles` 对象所有属性值为 `undefined`，检查是否同时配置了不兼容的 CSS 预处理器。

---

### Q12: Tailwind 类名不生效（动态构建类名）

**现象**：使用字符串拼接的 Tailwind 类名不生效。

**原因**：Tailwind 使用静态分析提取类名，动态拼接的类名无法被识别。

**解决方案：**

```tsx
// ❌ 错误：动态类名不会被 Tailwind 编译
const color = 'red';
<div className={`bg-${color}-500`}>...</div>; // 不会生效
<div className={`text-${size}`}>...</div>;    // 不会生效

// ❌ 错误：props 透传字符串也不会被识别
<div className={props.className}>...</div>;

// ✅ 正确：使用完整类名映射
const colorMap = { red: 'bg-red-500', blue: 'bg-blue-500', green: 'bg-green-500' } as const;
<div className={colorMap[color]}>...</div>;

// ✅ 正确：使用条件类名
<div className={clsx('text-base', isLarge && 'text-lg')}>...</div>;

// ✅ 正确：使用 style 属性处理完全动态的值
<div style={{ fontSize: `${dynamicSize}px` }}>...</div>;
```

---

### Q13: z-index 不生效

**现象**：设置了高 `z-index` 但元素仍被覆盖。

**原因**：`z-index` 受层叠上下文（stacking context）限制，非根 stacking context 中的 `z-index` 只在同层比较。

**常见创建 stacking context 的属性：**

- `position: relative/absolute/fixed/sticky` + `z-index` 非 auto
- `opacity < 1`
- `transform`、`filter`、`perspective`
- `isolation: isolate`

**解决方案：**

```css
/* 使用 CSS 变量统一管理 z-index */
:root {
  --z-dropdown: 100;
  --z-sticky: 200;
  --z-modal-backdrop: 300;
  --z-modal: 400;
  --z-toast: 500;
}
```

当元素无法突破父级层的 z-index 时，考虑使用 Portal 将其渲染到 body 下：

```tsx
import { createPortal } from 'react-dom';

function Modal({ children }: { children: React.ReactNode }) {
  return createPortal(
    <div className="modal" style={{ zIndex: 'var(--z-modal)' }}>{children}</div>,
    document.body
  );
}
```

---

## 性能问题

### Q14: 首页加载慢，LCP 高

**现象**：Lighthouse LCP 超过 4 秒。

**原因分析流程：**

1. **TTFB 高（> 800ms）**：服务端处理慢或数据库查询慢
2. **FCP 到 LCP 间隔大**：首屏关键图片/字体加载慢
3. **阻塞资源多**：同步 JS/CSS 过多

**解决方案：**

服务端优化：

```typescript
// 1. 在 Server Component 中合并多个 fetch（Next.js 自动去重）
const [trending, recommended] = await Promise.all([
  fetchVideos('trending'),
  fetchVideos('recommended'),
]);

// 2. 大列表使用分页或 infinite scroll，首屏只加载前 10 条

// 3. 静态数据使用 ISR
export const revalidate = 300; // 5 分钟
```

图片优化：

```tsx
// 使用 Next.js Image 组件
import Image from 'next/image';

// 首屏关键图片用 priority（预加载，不懒加载）
<Image
  src={heroVideo.coverUrl}
  alt={heroVideo.title}
  width={390}
  height={693}
  priority  // 首屏图片
/>

// 列表中图片用懒加载（默认行为）
<Image
  src={video.coverUrl}
  alt={video.title}
  width={160}
  height={284}
  sizes="(max-width: 768px) 50vw, 25vw"
/>
```

字体优化：

```tsx
// next.config.ts 中配置字体预加载
// 使用 next/font 自动处理字体优化
import { Noto_Sans_SC } from 'next/font/google';

const notoSans = Noto_Sans_SC({
  subsets: ['latin'],
  display: 'swap', // 字体加载前使用系统字体
  preload: true,
});
```

---

### Q15: 页面滚动卡顿（长列表）

**现象**：视频列表滚动时掉帧、卡顿。

**原因**：

1. 列表中渲染了大量 DOM 节点（无虚拟化）
2. 列表项组件重渲染过于频繁
3. 图片懒加载触发 Layout Shift

**解决方案：**

1. 使用虚拟列表（react-virtuoso）：

```tsx
import { Virtuoso } from 'react-virtuoso';

function VideoList({ videos }: { videos: VideoItem[] }) {
  return (
    <Virtuoso
      data={videos}
      itemContent={(index, video) => <VideoCard video={video} />}
      totalCount={videos.length}
      useWindowScroll
    />
  );
}
```

2. 使用 `React.memo` + 稳定 Props 减少重渲染：

```tsx
const VideoCard = React.memo(function VideoCard({
  video,
  onPlay,
}: {
  video: VideoItem;
  onPlay: (id: string) => void;
}) {
  // ...
});
```

3. 图片预留占位空间：

```css
.video-card-image-wrapper {
  aspect-ratio: 9 / 16;
  overflow: hidden;
  background: var(--color-bg-secondary); /* 占位背景色 */
}
```

---

### Q16: API 请求重复发送（useEffect 中 fetch）

**现象**：打开页面时同样 API 被调用了多次。

**原因**：

1. React Strict Mode 开发模式下 useEffect 会执行两次
2. 依赖数组不完整，effect 被重复触发
3. 多个组件各自发起相同查询

**解决方案：**

```tsx
// ✅ 方案 1：使用 TanStack Query（推荐，自动去重 + 缓存）
function VideoPage({ id }: { id: string }) {
  const { data } = useQuery({
    queryKey: ['video', id],
    queryFn: () => fetchVideo(id),
    staleTime: 5 * 60 * 1000,
  });
  // ...
}

// ✅ 方案 2：Server Component 中 fetch（天然不会重复）
export default async function VideoPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const video = await fetchVideo(id); // 不会重复
  return <VideoDetail video={video} />;
}

// ❌ 避免：useEffect + fetch
useEffect(() => {
  fetchVideo(id).then(setVideo);  // Strict Mode 下会调两次
}, [id]);
```

---

## 兼容性问题

### Q17: 微信内置浏览器视频自动播放失败

**现象**：微信内置浏览器中视频不会自动播放。

**原因**：微信内置浏览器对自动播放限制严格，必须由用户手势触发首次播放，且要求 `muted`。

**解决方案：**

```tsx
function AutoPlayVideo({ src }: { src: string }) {
  const videoRef = useRef<HTMLVideoElement>(null);

  // 微信环境下首次必须静音
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    video.muted = true;
    const play = () => video.play().catch(() => {});
    play();

    // 用户首次点击后恢复声音
    document.addEventListener('click', () => {
      video.muted = false;
    }, { once: true });
  }, []);

  return (
    <video
      ref={videoRef}
      src={src}
      playsInline  // iOS 微信需要此属性
      webkit-playsinline="true"
      x5-video-player-type="h5"  // 微信 X5 内核
      x5-video-player-fullscreen="true"
      preload="metadata"
    />
  );
}
```

**关键属性说明：**

| 属性 | 用途 |
|------|------|
| `playsInline` | iOS 内联播放（不强制全屏） |
| `webkit-playsinline` | 同上，兼容旧版 |
| `x5-video-player-type="h5"` | 微信 Android 启用同层播放 |
| `x5-video-player-fullscreen` | 微信 Android 是否全屏 |

---

### Q18: iOS Safari 底部安全区域适配

**现象**：固定在底部的 Tab Bar / 按钮被 iPhone 底部指示条遮挡。

**原因**：iOS Safari 有 safe area inset，固定定位不会自动避开。

**解决方案：**

```css
/* 使用 CSS env() 函数适配安全区域 */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding-bottom: env(safe-area-inset-bottom, 0);
  /* 从 iOS 11.2 开始支持，第二个参数是 fallback */
}

/* 确保 viewport 包含 safe area */
```

```html
<!-- layout.tsx 中设置 viewport meta -->
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
```

```tsx
// 也可用 useSafeArea 自定义 Hook（适用于需要动态场景）
'use client';

export function useSafeArea() {
  const [insets, setInsets] = useState({ bottom: 0, top: 0 });

  useEffect(() => {
    const style = getComputedStyle(document.documentElement);
    setInsets({
      bottom: parseInt(style.getPropertyValue('env(safe-area-inset-bottom)')) || 0,
      top: parseInt(style.getPropertyValue('env(safe-area-inset-top)')) || 0,
    });
  }, []);

  return insets;
}
```

---

### Q19: Android Chrome 底部导航栏遮挡

**现象**：Android Chrome 中 `100vh` 高度溢出，底部被浏览器导航栏遮挡。

**原因**：移动端浏览器中 `100vh` 包含地址栏区域，但地址栏隐藏/显示时视口高度会变化。

**解决方案：**

```css
/* 使用 dvh（dynamic viewport height）替代 vh */
.page-container {
  min-height: 100dvh; /* 动态视口高度 */
  /* fallback for old browsers */
  min-height: 100vh;
}

/* 也可用 svh（small viewport height，视口最小时的 100%）*/
.page-container {
  min-height: 100svh;
}
```

```css
/* 或使用 max-height + overflow 控制 */
.page-container {
  height: 100dvh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch; /* iOS 弹性滚动 */
}
```

---

### Q20: iOS Safari 点击 300ms 延迟

**现象**：iOS Safari 中点击按钮有 300ms 延迟才响应。

**原因**：旧版 iOS Safari 等待 300ms 判断是否是双击缩放。

**解决方案（现代方案，已基本不需要）：**

```html
<!-- 如果 viewport 设置了 user-scalable=no 或 width=device-width，
     iOS Safari 9.3+ 已自动禁用 300ms 延迟 -->
<meta name="viewport" content="width=device-width, initial-scale=1" />
```

如果仍有延迟（旧设备），使用 CSS：

```css
button, a, [role="button"] {
  touch-action: manipulation; /* 禁用双击缩放，消除延迟 */
}
```
