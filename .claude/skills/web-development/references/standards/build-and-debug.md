# 编译、运行与调试 — Web

> 本文档定义 Web 端的构建、运行与调试规范。

---

## 1. 构建系统

### 1.1 next.config.ts

```typescript
// next.config.ts
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  // 图片优化
  images: {
    // 允许的远程图片域名
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'cdn.shortdrama.com',
      },
      {
        protocol: 'https',
        hostname: 'img.shortdrama.com',
      },
    ],
    // 指定支持的图片格式
    formats: ['image/avif', 'image/webp'],
    // 设备尺寸（用于生成响应式 srcset）
    deviceSizes: [640, 750, 828, 1080, 1200, 1920],
  },

  // HTTP Headers（安全相关）
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: [
          {
            key: 'X-Frame-Options',
            value: 'DENY',
          },
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff',
          },
          {
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin',
          },
        ],
      },
    ];
  },

  // 重定向
  async redirects() {
    return [
      {
        source: '/home',
        destination: '/',
        permanent: true,
      },
    ];
  },

  // Rewrites（代理后端 API，隐藏真实地址 + 解决跨域）
  async rewrites() {
    return [
      {
        source: '/api/proxy/:path*',
        destination: `${process.env.API_BASE_URL}/:path*`,
      },
    ];
  },

  // 实验性功能
  experimental: {
    // 可选：开启 Partial Prerendering
    // ppr: 'incremental',
  },

  // 压缩配置
  compress: true, // 默认开启 gzip/brotli
};

export default nextConfig;
```

**关键配置说明：**

- `images.remotePatterns`：必须配置所有 CDN 域名，否则 Next.js Image Optimization 不会处理外部图片
- `headers`：安全 Headers 在此统一声明（取代 helmet 中间件）
- `rewrites`：用于代理后端 API，避免在客户端暴露后端真实地址，同时解决跨域
- `compress`：生产环境默认开启 gzip/brotli 压缩

### 1.2 环境变量

| 文件 | 用途 | 加载时机 |
|------|------|---------|
| `.env` | 所有环境通用 | 始终加载 |
| `.env.local` | 本地覆盖（不提交 Git） | 始终加载，覆盖 `.env` |
| `.env.development` | 开发环境（`next dev`） | 仅开发 |
| `.env.production` | 生产环境（`next start`） | 仅生产 |
| `.env.test` | 测试环境 | 仅测试 |

**命名约定：**

- `NEXT_PUBLIC_*` 前缀：暴露给浏览器端代码，可在 Client Component 中访问
- 无前缀：仅服务端可访问，不在客户端 bundle 中出现
- 所有环境相关值必须通过环境变量配置，**禁止硬编码**

```bash
# .env.local 示例（不提交到 Git）
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_SITE_URL=http://localhost:3000
NEXT_PUBLIC_APP_NAME=ShortDrama
NEXT_PUBLIC_APP_VERSION=1.0.0

# 第三方服务 Key（仅服务端可读，不加 NEXT_PUBLIC_）
UPLOAD_API_SECRET=xxxx

# .env.production 示例
API_BASE_URL=https://api.shortdrama.com
NEXT_PUBLIC_SITE_URL=https://shortdrama.com
```

在代码中安全使用：

```typescript
// ✅ Server Component / Route Handler 中使用服务端变量
const apiUrl = process.env.API_BASE_URL;
const secret = process.env.UPLOAD_API_SECRET;

// ✅ Client Component 中使用公开变量
const siteUrl = process.env.NEXT_PUBLIC_SITE_URL;
const appName = process.env.NEXT_PUBLIC_APP_NAME;

// ❌ 禁止：在 Client Component 中使用非 NEXT_PUBLIC_ 变量（值为 undefined）
```

### 1.3 Bundle 分析

```bash
npm install -D @next/bundle-analyzer
```

```typescript
// next.config.ts
import withBundleAnalyzer from '@next/bundle-analyzer';

const withAnalyzer = withBundleAnalyzer({
  enabled: process.env.ANALYZE === 'true',
});

const nextConfig: NextConfig = { /* ... */ };

export default withAnalyzer(nextConfig);
```

```bash
# 运行时启用分析
ANALYZE=true npm run build
# 浏览器自动打开三个页面：client.html / edge.html / nodejs.html
```

定期检查要点：
- 页面 First Load JS 是否超过 150KB（gzip 后）
- 是否有重复打包的依赖
- `node_modules` 中是否有不适合客户端 bundle 的服务端包被打入

---

## 2. 常用命令

### 2.1 开发

```bash
# 启动开发服务器（默认 http://localhost:3000）
npm run dev

# 指定端口
npm run dev -- -p 4000

# 开启 Turbopack（更快的 HMR）
npm run dev -- --turbo
```

### 2.2 构建

```bash
# 生产构建
npm run build

# 构建 + Bundle 分析
ANALYZE=true npm run build

# 跳过 TypeScript 类型检查（仅紧急情况，不可常态化）
npm run build -- --no-lint
```

构建产物位于 `.next/` 目录，不应提交到 Git（已在 `.gitignore` 中）。

### 2.3 启动

```bash
# 启动生产服务器（必须先在本地 npm run build）
npm run start

# 指定端口
npm run start -- -p 4000
```

### 2.4 Lint

```bash
# ESLint 检查所有文件
npm run lint

# 自动修复
npm run lint -- --fix

# 检查特定目录
npm run lint -- app/components/
```

ESLint 配置文件为 `eslint.config.mjs`（ESLint flat config），包含 `eslint-config-next` 和 `@typescript-eslint` 规则。

### 2.5 测试

```bash
# 运行单元测试（Vitest）
npm run test

# 监听模式
npm run test -- --watch

# 运行测试并生成覆盖率报告
npm run test -- --coverage

# 运行 E2E 测试（Playwright）
npm run test:e2e

# E2E 测试 UI 模式（可视化调试）
npm run test:e2e -- --ui

# 运行特定测试文件
npm run test -- src/components/VideoCard.test.tsx
```

### 2.6 TypeScript 检查

```bash
# 类型检查（不生成文件）
npx tsc --noEmit
```

---

## 3. 调试

### 3.1 React DevTools

1. 安装 Chrome 扩展 [React Developer Tools](https://chrome.google.com/webstore/detail/react-developer-tools/fmkadmapgofadopljbjfkapdkoienihi)
2. 开发模式下打开 Chrome DevTools → **Components** 面板
3. 可查看：
   - 组件树结构与层级
   - 每个组件的 Props、State、Hooks 值
   - 组件渲染来源（为何重新渲染）
   - 组件渲染耗时（需开启 "Record why each component rendered"）

**常用技巧：**

- 选中页面元素 → Components 面板自动定位到对应组件
- 在组件上右键 → "Log this component data to console"
- Profiler 面板用于分析渲染性能

### 3.2 浏览器 DevTools

**Console 面板：**

- 查看 `console.error`、`console.warn` 输出
- React 开发模式下 hydration 不匹配警告会在此显示
- Network 请求的错误响应也会打印在 Console

**Network 面板：**

- 检查 API 请求的 URL、Headers、Payload、Response
- 筛选 XHR/Fetch 查看 API 调用
- 查看 waterfall 分析请求时序
- 右键请求 → "Copy as fetch" 可在控制台中重现请求

**Sources 面板：**

- 开发模式下可在 `webpack://` 或 `_N_E/` 下找到源码
- 直接设置断点调试 TypeScript 代码（Source Map 已启用）

**Application 面板：**

- Local Storage / Session Storage：查看客户端存储数据
- Cookies：检查 Session Cookie 是否正确设置
- Service Workers：检查 PWA 缓存状态

### 3.3 断点调试

**方法一：`debugger` 语句**

```typescript
async function fetchVideoList(params: VideoSearchParams) {
  debugger; // 浏览器执行到此处会自动暂停
  const data = await api.get('/videos', { params });
  return videoListResponseSchema.parse(data);
}
```

**方法二：Sources 面板断点**

1. 打开 DevTools → Sources
2. 在文件树中找到目标文件（开发模式下的源码路径）
3. 点击行号设置断点
4. 刷新页面或触发操作，代码执行到该行时暂停

**方法三：条件断点**

在 Sources 面板中右键行号 → "Add conditional breakpoint" → 输入条件（如 `id === '123'`），仅当条件满足时暂停。

**方法四：DOM 断点**

Elements 面板 → 右键元素 → Break on → subtree modifications / attribute modifications / node removal

### 3.4 Next.js DevTools

开发模式下，页面右下角或浏览器 Console 中会有 Next.js 的编译状态指示：

- **Compiling**：页面正在编译中（首次访问或文件变更后）
- **Ready**：编译完成，页面可用
- 页面左下角显示当前路由信息和渲染模式（SSR/SSG/ISR 标识）

查看路由和中间件匹配：

```bash
# 终端中运行，查看所有路由信息
npx next info
```

**常见调试场景：**

- 页面空白无报错：检查 Compiling 是否完成、Network 中 HTML 响应是否正常
- Hydration 错误：检查服务端和客户端渲染是否一致（最常见原因是使用了 `typeof window` 判断或 `Date.now()` 等非确定性值）
- API Route 不响应：检查 `route.ts` 是否导出正确的 HTTP 方法函数（`GET`、`POST` 等需大写）

---

## 4. 性能分析

### 4.1 Lighthouse

Chrome DevTools → Lighthouse 面板 → Generate report。

**关注指标：**

| 指标 | 目标 | 说明 |
|------|------|------|
| Performance | >= 90 | 综合性能评分 |
| FCP (First Contentful Paint) | < 1.8s | 首次内容绘制 |
| LCP (Largest Contentful Paint) | < 2.5s | 最大内容绘制 |
| TBT (Total Blocking Time) | < 200ms | 总阻塞时间 |
| CLS (Cumulative Layout Shift) | < 0.1 | 累积布局偏移 |
| Accessibility | >= 90 | 无障碍评分 |
| Best Practices | >= 90 | 最佳实践 |
| SEO | >= 90 | SEO 评分 |

**测试模式：**

- Navigation（默认）：测试完整页面加载
- Timespan：测试一段时间内的用户交互（如切换 Tab、播放视频）
- Snapshot：测试当前页面状态

### 4.2 Core Web Vitals

三大核心指标及优化要点：

**LCP（最大内容绘制，< 2.5s）：**

- 使用 Next.js `<Image>` 组件（自动懒加载、WebP/AVIF 格式、响应式尺寸）
- 预加载关键 Hero 图片：`<link rel="preload" as="image" href="..." />`
- 服务端渲染首屏内容，避免客户端 water-fall
- 减少服务端响应时间（TTFB）

**INP（交互到下一次绘制，< 200ms）：**

- 拆分长任务（Long Task），使用 `setTimeout` 或 `scheduler.postTask`
- 避免在主线程中进行大量 DOM 操作
- 使用 `useTransition` 标记非紧急更新

```typescript
'use client';
import { useTransition } from 'react';

export function VideoFilter() {
  const [isPending, startTransition] = useTransition();

  const handleFilterChange = (tag: string) => {
    startTransition(() => {
      setActiveTag(tag);  // 非紧急更新，可被中断
    });
  };

  return (
    <div>
      {/* 筛选按钮 */}
      {isPending && <Spinner />}
    </div>
  );
}
```

**CLS（累积布局偏移，< 0.1）：**

- 图片和视频预留尺寸（width/height 或 aspect-ratio）
- 广告、嵌入内容提前预留空间
- 避免在已有内容上方动态插入元素
- 字体加载使用 `font-display: swap` + 预加载

```css
/* 预留图片尺寸，避免加载后撑开布局 */
.video-cover {
  aspect-ratio: 9 / 16;
  width: 100%;
}
```

**监控方案：**

```typescript
// app/layout.tsx
import { SpeedInsights } from '@vercel/speed-insights/next';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html>
      <body>
        {children}
        <SpeedInsights />
      </body>
    </html>
  );
}
```

也可以在 Vercel 控制台查看 Web Vitals 数据。

### 4.3 React Profiler

React DevTools → Profiler 面板：

1. 点击 Record 按钮
2. 执行需要分析的操作（如页面切换、列表滚动）
3. 点击 Stop 录制
4. 分析结果：
   - **Flamegraph**：查看每个组件的渲染耗时
   - **Ranked**：按渲染耗时降序排列组件
   - **Timeline**：按时间线查看渲染事件

**手动 Profiler 代码埋点：**

```typescript
import { Profiler } from 'react';

function onRenderCallback(
  id: string,
  phase: 'mount' | 'update' | 'nested-update',
  actualDuration: number,
  baseDuration: number,
  startTime: number,
  commitTime: number,
) {
  if (actualDuration > 50) {
    console.warn(`[Perf] ${id} (${phase}) took ${actualDuration}ms`);
  }
}

export function App() {
  return (
    <Profiler id="HomePage" onRender={onRenderCallback}>
      <HomePage />
    </Profiler>
  );
}
```

### 4.4 Network 面板

**关键分析操作：**

1. 打开 DevTools → Network → 勾选 "Disable cache"（仅在分析冷启动时）
2. 刷新页面，观察 waterfall（请求瀑布流）

**分析要点：**

- 是否有阻塞渲染的请求（`<head>` 中的同步 JS/CSS）
- API 请求是否有串行等待（waterfall 中的阶梯状空白）
- 资源大小：JS bundle 是否过大，图片是否未压缩
- 是否有失败的请求（红色标记），是否影响页面功能
- 请求数量：首屏是否发起了过多请求

**节流测试：**

Network 面板 → Throttling → 选择 "Slow 4G" 或 "Fast 3G" 模拟弱网环境。

**HAR 导出：**

Network 面板中右键 → "Save all as HAR with content"，可将完整的请求/响应信息导出为 HAR 文件，用于离线分析或提交给后端排查。
