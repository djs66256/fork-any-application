# 架构设计 — Web

> 本文档定义 Web 端的整体架构设计规范。

---

## 1. 整体架构

<!-- TODO: 补充架构图与分层说明 -->

```
Pages (Server Components / Client Components)
    ↓
Components (UI)
    ↓
Hooks (State & Logic)
    ↓
Services / API Layer
    ↓
Backend API
```

<!-- TODO: 各层职责详细说明 -->

---

## 2. 路由设计

<!-- TODO: 补充 Next.js App Router 路由规范 -->

### 2.1 目录结构

<!-- TODO: app/ 目录结构、路由分组、平行路由 -->

### 2.2 动态路由

<!-- TODO: [id]、[...slug]、generateStaticParams -->

### 2.3 中间件

<!-- TODO: middleware.ts 用途（Auth、Redirect、i18n）-->

---

## 3. 状态管理

<!-- TODO: 补充状态分层策略 -->

### 3.1 URL State

<!-- TODO: searchParams、useSearchParams、nuqs -->

### 3.2 Server State

<!-- TODO: TanStack Query、SWR、缓存策略 -->

### 3.3 Client State

<!-- TODO: Zustand / Jotai / Context，何时提升为全局状态 -->

### 3.4 Form State

<!-- TODO: React Hook Form + Zod -->

---

## 4. 数据请求

<!-- TODO: 补充数据获取策略 -->

### 4.1 Server Components

<!-- TODO: fetch 直接调用、cache、revalidate -->

### 4.2 Client Components

<!-- TODO: TanStack Query、useEffect + fetch -->

### 4.3 Server Actions

<!-- TODO: useActionState、formAction -->

---

## 5. SSR / SSG / ISR 策略

<!-- TODO: 补充渲染策略选择 -->

### 5.1 SSR (Dynamic)

<!-- TODO: cache: 'no-store'、动态渲染 -->

### 5.2 SSG (Static)

<!-- TODO: 构建时生成 -->

### 5.3 ISR

<!-- TODO: revalidate 配置、on-demand revalidation -->

---

## 6. 错误处理

<!-- TODO: 补充错误处理策略 -->

### 6.1 Error Boundary

<!-- TODO: error.tsx、reset() -->

### 6.2 全局错误

<!-- TODO: global-error.tsx -->

### 6.3 Not Found

<!-- TODO: not-found.tsx、notFound() -->

### 6.4 API 错误

<!-- TODO: 统一错误处理、toast 提示 -->
