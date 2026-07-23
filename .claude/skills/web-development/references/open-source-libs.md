# Web 开源库选型

> 本文件列出 Web 端将使用或可能使用到的开源库，按功能领域分组。
> 标记说明：
> - ✅ 已选定 / 强烈推荐
> - 🔶 备选 / 待评估
> - ⚠️ 需用户确认后才能引入

---

## 框架与工具链

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Next.js | React 全栈框架 | ✅ | 已确定技术栈 |
| React 19 | UI 库 | ✅ | |
| TypeScript | 类型系统 | ✅ | |
| Zod | 数据校验 | ✅ | 已确定 |
| ESLint + Prettier | 代码规范 | ✅ | |

---

## 样式

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Tailwind CSS | 原子化 CSS | 🔶 | 高效，但需评估团队偏好 |
| CSS Modules | 模块化 CSS | ✅ | Next.js 内置支持 |
| styled-components | CSS-in-JS | 🔶 | 需要客户端组件，与 RSC 兼容性差 |
| Vanilla Extract | 零运行时 CSS-in-JS | 🔶 | 支持 RSC |
| Radix UI / shadcn/ui | 无样式/可定制组件库 | 🔶 | 可访问性好 |
| NextUI / Ant Design | 完整组件库 | 🔶 | 快速开发 |

---

## 状态管理与数据请求

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| TanStack Query (React Query) | 服务端状态管理 | ✅ | 缓存、重新请求、乐观更新 |
| Zustand | 轻量客户端状态 | 🔶 | 比 Redux 轻量 |
| Jotai | 原子化状态 | 🔶 | 细粒度重渲染控制 |
| SWR | 数据请求 + 缓存 | 🔶 | Vercel 出品，TanStack Query 替代 |
| nuqs | URL 搜索参数管理 | 🔶 | Next.js 集成好 |

---

## 视频播放

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Vidstack | 现代视频播放器 | 🔶 | 支持 HLS/DASH、可定制 UI |
| Plyr | 轻量播放器 | 🔶 | 简洁、无障碍友好 |
| HLS.js | HLS 流播放 | 🔶 | 原生 HLS 不支持时使用 |
| Mux Player | 托管视频播放 | 🔶 | 需 Mux 服务 |

---

## 动画

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Framer Motion | 声明式动画 | ✅ | React 生态最流行 |
| Lottie Web | Lottie 动画 | 🔶 | 配合设计师 |
| GSAP | 高性能动画 | 🔶 | 复杂动画场景 |

---

## 工具

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| date-fns / dayjs | 日期处理 | 🔶 | 替代 moment.js |
| clsx + tailwind-merge | className 合并 | 🔶 | 配合 Tailwind |
| lodash / lodash-es | 工具函数 | 🔶 | 按需引入 |
| usehooks-ts | React Hooks 工具集 | 🔶 | |
| ky | HTTP 客户端 | 🔶 | 比 fetch 更方便 |

---

## 国际化

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| next-intl | Next.js 国际化 | 🔶 | Next.js 原生集成好 |
| next-i18next | Next.js 国际化 | 🔶 | i18next 生态 |
| react-i18next | React 国际化 | 🔶 | 通用方案 |

---

## 测试

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Vitest | 单元/组件测试 | ✅ | Vite 生态，速度快 |
| React Testing Library | 组件测试 | ✅ | |
| Playwright | E2E 测试 | ✅ | 跨浏览器、功能强大 |
| MSW (Mock Service Worker) | API Mock | 🔶 | 测试/开发期拦截请求 |
| Storybook | 组件开发/文档 | 🔶 | 组件隔离开发 |
| Chromatic | 视觉回归测试 | 🔶 | 配合 Storybook |

---

## 性能

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| @vercel/speed-insights | Web Vitals 监控 | 🔶 | Vercel 部署可用 |
| @next/bundle-analyzer | Bundle 分析 | 🔶 | |

---

## 安全

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| next-auth / Auth.js | 认证方案 | 🔶 | 多 Provider 支持 |
| helmet | HTTP Headers 安全 | 🔶 | |

---

## 埋点与分析

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| @vercel/analytics | 基础分析 | 🔶 | |
| firebase/analytics | Firebase 分析 | 🔶 | |

---

## 其他

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| html-react-parser | HTML 转 React | 🔶 | 富文本渲染 |
| react-virtuoso | 虚拟列表 | 🔶 | 长列表优化 |
| swiper | 滑动组件 | 🔶 | Banner/轮播 |
