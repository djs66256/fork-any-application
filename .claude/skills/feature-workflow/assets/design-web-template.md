# Web 端技术方案：{{功能名称}}

> 创建日期：{{YYYY-MM-DD}}
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

<!-- 描述 Web 端架构层级（Pages → Components → State → API → Cache） -->

```
┌─────────────────────────────────────────────────┐
│  Pages / Routes                                 │
│  ├── Layout Components                          │
│  └── Page Components                            │
├─────────────────────────────────────────────────┤
│  State Management                               │
│  ├── Global State (Zustand / Redux)             │
│  ├── Server State (TanStack Query / SWR)        │
│  └── Local State (useState / useReducer)        │
├─────────────────────────────────────────────────┤
│  Data Layer                                     │
│  ├── API Client (fetch / axios)                 │
│  ├── Response Validation (Zod)                  │
│  └── Cache Strategy (SW / HTTP Cache)           │
└─────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| | 扩展 / 新增 / 不变 | |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `web/.../xxx.tsx` | 新增 | |
| `web/.../xxx.tsx` | 修改 | |
| `web/.../xxx.tsx` | 删除 | |

---

## 3. 组件设计

### 3.1 组件层级树

```
{{Page}}/
├── {{PageLayout}}
│   ├── {{HeaderSection}}
│   │   ├── {{SearchBar}}
│   │   └── {{FilterTabs}}
│   ├── {{ContentSection}}
│   │   ├── {{ItemCard}} (mapped list)
│   │   │   ├── {{Thumbnail}}
│   │   │   └── {{ActionButton}}
│   │   └── {{EmptyState}}
│   └── {{Footer}}
└── {{Modal}} (conditional render)
```

### 3.2 组件清单

| 组件名称 | 类型（Page / Section / Atom） | 职责 | Props 接口 |
|---------|------------------------------|------|-----------|
| | | | |

### 3.3 组件接口定义

```typescript
// 以 React + TypeScript 为例
interface {{ComponentName}}Props {
  /** 描述 */
  data: Item[];
  /** 加载中 */
  isLoading: boolean;
  /** 操作回调 */
  onAction: (id: string) => void;
}

export const {{ComponentName}}: React.FC<{{ComponentName}}Props> = ({
  data,
  isLoading,
  onAction,
}) => {
  // ...
};
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Props | |
| 子 → 父 | Callback Props | |
| 跨层级 | Context / Store | |
| 兄弟组件 | 提升状态到公共父组件 / Store | |

### 3.5 响应式设计

| 断点 | 宽度范围 | 布局策略 | 关键变化 |
|------|---------|---------|---------|
| Mobile | < 768px | 单列 / 底部 Tab 导航 | 隐藏侧边栏 |
| Tablet | 768px - 1024px | 双列 / 侧边栏折叠 | |
| Desktop | > 1024px | 多列 / 侧边栏常驻 | 展开完整导航 |

### 3.6 无障碍（A11y）

<!-- ARIA 标签、键盘导航、屏幕阅读器支持、焦点管理 -->

| 关注点 | 策略 |
|--------|------|
| 语义化 HTML | |
| 键盘导航 | Tab 顺序、Enter/Escape 快捷键 |
| 屏幕阅读器 | aria-label、role 属性 |
| 色彩对比度 | WCAG AA 至少 4.5:1（正文） |

---

## 4. 状态管理方案

### 4.1 方案选择

| 维度 | 选择 | 理由 |
|------|------|------|
| 全局状态 | Zustand / Redux Toolkit / Context | |
| 服务端状态 | TanStack Query / SWR | |
| 表单状态 | React Hook Form / Formik | |
| 路由状态 | React Router / Vue Router / Next.js App Router | |
| URL 状态 | useSearchParams / query-string | |

### 4.2 状态划分

| 状态名称 | 范围（全局/页面/局部） | 存储方式 | 跨页面持久化 |
|---------|----------------------|---------|------------|
| | | | 是 / 否 |

### 4.3 全局状态结构

```typescript
// Zustand store 示例
interface AppState {
  // 用户态
  user: User | null;
  // 操作状态
  // ...
}

interface AppActions {
  setUser: (user: User | null) => void;
  // ...
}
```

### 4.4 服务端状态管理

```typescript
// TanStack Query 示例
const useItems = () => {
  return useQuery({
    queryKey: ['items'],
    queryFn: () => api.getItems(),
    staleTime: 5 * 60 * 1000, // 5 分钟内视为新鲜
  });
};

const useCreateItem = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: api.createItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['items'] }),
  });
};
```

### 4.5 状态流转

```
用户操作 → 触发 Action / Mutation
         → 更新 Store / Query Cache
         → Optimistic Update（可选）
         → 依赖组件重渲染
         → 成功后确认 / 失败后回滚
```

---

## 5. 路由设计

### 5.1 路由清单

| 路径 Pattern | 页面组件 | 参数 | 认证守卫 | 懒加载 | 说明 |
|-------------|---------|------|---------|--------|------|
| `/items` | ItemsPage | — | ✅ | ✅ | |
| `/items/:id` | ItemDetailPage | id: string | ✅ | ✅ | |

### 5.2 路由层级

```
Root Layout (App Shell: Header + Sidebar + Outlet)
├── Public Routes
│   ├── /login → LoginPage
│   └── /register → RegisterPage
├── Protected Routes (requireAuth: true)
│   ├── /dashboard → DashboardPage
│   │   └── /dashboard/:id → DashboardDetailPage
│   └── /settings → SettingsPage
└── Error Routes
    ├── /404 → NotFoundPage
    └── /403 → ForbiddenPage
```

### 5.3 路由守卫

```typescript
// ProtectedRoute 组件
const ProtectedRoute = () => {
  const user = useUserStore(s => s.user);
  if (!user) return <Navigate to="/login" replace />;
  return <Outlet />;
};
```

### 5.4 数据预取与加载

| 页面 | 预取方式 | 预取内容 |
|------|---------|---------|
| | loader / getServerSideProps / 客户端 fetch | |

---

## 6. API 调用层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | fetch / axios / ky | |
| 请求拦截器 | Token 注入、请求日志、CSRF token | |
| 响应拦截器 | 统一错误处理、401 → Token 刷新 | |
| 响应校验 | Zod schema 校验 | |

### 6.2 客户端封装

```typescript
// API 客户端示例
const apiClient = {
  async get<T>(url: string, schema: z.ZodSchema<T>): Promise<T> {
    const res = await fetch(`${BASE_URL}${url}`, {
      headers: { Authorization: `Bearer ${getToken()}` },
    });
    if (!res.ok) throw new ApiError(res.status, await res.json());
    return schema.parse(await res.json());
  },
  async post<T>(url: string, body: unknown, schema: z.ZodSchema<T>): Promise<T> { ... },
};
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 2 | 指数退避 | |
| 5xx 服务端错误 | 3 | 指数退避 | |
| 401 Token 过期 | — | — | 静默刷新 token 后重放 |

### 6.4 请求 Hook 封装

<!-- useQuery / useMutation 的通用封装：error handling、loading state、cache invalidation -->

---

## 7. SSR / CSR 策略

### 7.1 渲染策略选择

| 页面 | 渲染策略 | 原因 |
|------|---------|------|
| Landing Page | SSG | 内容稳定，SEO 关键 |
| Login / Register | CSR | 用户相关，无需 SEO |
| Dashboard | CSR | 高度交互、需登录 |
| Detail Page | SSR | SEO 友好 + 实时数据 |

### 7.2 框架支持

<!-- Next.js (App Router / Pages Router) / Nuxt / Remix / SPA -->

### 7.3 数据预取策略

| 页面 | 预取方法 | 预取数据 |
|------|---------|---------|
| SSR 页面 | getServerSideProps / RSC | |
| SSG 页面 | getStaticProps + revalidate | |
| CSR 页面 | useEffect / TanStack Query | |

### 7.4 SEO 策略

<!-- Meta tags、Open Graph、Structured Data (JSON-LD)、Sitemap -->

---

## 8. 性能优化

### 8.1 优化清单

| 优化项 | 策略 | 目标 |
|--------|------|------|
| 代码分割 | `React.lazy` + `Suspense` / `next/dynamic` | 首屏 JS < 200KB |
| 图片优化 | `next/image` / WebP / 懒加载 / 响应式 srcSet | LCP < 2.5s |
| 字体加载 | `font-display: swap` / 子集化 | 无闪烁 |
| 缓存策略 | SW Cache / HTTP Cache-Control / TanStack Query staleTime | 重复访问秒开 |
| Bundle 分析 | `@next/bundle-analyzer` / `webpack-bundle-analyzer` | 识别大依赖 |
| 关键 CSS | 内联首屏 CSS / Tailwind JIT | FCP < 1.8s |

### 8.2 加载体验

| 场景 | 策略 |
|------|------|
| 首屏加载 | 骨架屏 / SSR 直出 |
| 路由切换 | Suspense fallback / 进度条 |
| 数据加载 | TanStack Query `placeholderData` / optimistic update |

---

## 9. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| API Base URL | `NEXT_PUBLIC_API_URL` | `http://localhost:3000/api` | | |
| CDN 地址 | `NEXT_PUBLIC_CDN_URL` | | | |
| Feature Flags | `NEXT_PUBLIC_FEATURE_*` | | | |
| Analytics ID | `NEXT_PUBLIC_GA_ID` | | | |

> ⚠️ 禁止硬编码任何常量。使用 `.env` / `.env.local` / `.env.production` 管理配置，客户端可访问的变量以 `NEXT_PUBLIC_` 前缀。

---

## 10. API 调用清单

<!-- 列出本端需要调用的所有 API，与 design.md 保持严格一致 -->

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `METHOD /api/path` | 页面加载 / 用户操作 | Store / 组件状态 / URL params | 更新 Store / 刷新 Query / 导航 | Toast / 内联 / 跳转 |

---

## 11. 跨端共享逻辑落地

<!-- 对应 design.md 中「跨端共享逻辑」章节 -->

| 共享逻辑 | design.md 定义 | Web 端实现方式 |
|---------|---------------|---------------|
| | | |

---

## 12. 边界与错误处理

### 12.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | axios interceptor / fetch wrapper | 统一错误码解析 |
| 状态层 | TanStack Query `onError` / global error handler | |
| UI 层 | ErrorBoundary / Toast / 内联 Message | |
| 日志 | console.error + 错误上报 SDK (Sentry / Datadog) | |

### 12.2 错误边界

```typescript
// React Error Boundary
class ErrorBoundary extends React.Component<Props, State> {
  // 捕获渲染错误，展示 fallback UI
}
```

### 12.3 错误码映射表

<!-- 与 design.md 中错误码对应，补充 Web 端交互方式 -->

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `INVALID_PARAMS` | | 表单字段内联校验提示 |
| `UNAUTHORIZED` | | Toast + 跳转登录页 |
| `FORBIDDEN` | | Toast / 返回上一页 / 403 页面 |
| `NOT_FOUND` | | Toast / 404 页面 / 空态页 |
| `CONFLICT` | | Toast + 重试引导 |
| `RATE_LIMITED` | | Toast（含倒计时 / Retry-After） |
| `INTERNAL_ERROR` | | Toast / ErrorBoundary fallback + 重试 |
| `NETWORK_ERROR` | | Toast + 重试按钮 |

### 12.4 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 网络断开 | `navigator.onLine` → false | 展示离线提示、暂停请求、队列暂存操作 | 🟡 |
| 网络恢复 | `online` 事件 | 重放暂存请求、刷新过期数据 | 🟡 |
| 页面卸载时未完成请求 | `unload` / 路由切换 | `AbortController` 取消请求 | 🔴 |
| Token 过期 | API 返回 401 | 静默刷新 / 跳转登录 | 🔴 |
| 浏览器后退/前进 | `popstate` 事件 | 保持页面状态 / 重新加载（视场景） | 🟡 |
| 用户快速连续操作 | 快速点击 / 重复提交 | 防抖（300ms）/ 按钮 loading + disabled | 🔴 |
| SSR 水合不匹配 | 服务端/客户端数据不一致 | `suppressHydrationWarning` / 客户端优先渲染 | 🟡 |
| localStorage 满 | `QuotaExceededError` | 清理过期数据 / 降级为内存存储 | 🟢 |
| 跨浏览器兼容 | Safari / Firefox / Chrome 差异 | Polyfill / 渐进增强 | 🟡 |

### 12.5 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| | | | | | |

---

## 13. 测试策略

### 13.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | 工具函数、状态逻辑、数据校验 | | Vitest / Jest |
| 组件测试 | 关键交互、状态变化 | | Testing Library |
| 集成测试 | 页面级流程（含 API Mock） | | Vitest + MSW |
| E2E 测试 | 关键用户完整流程 | | Playwright / Cypress |

### 13.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| | 列表加载成功 | 正常 API | 进入页面 | 列表渲染、loading 消失 | 组件 |
| | 列表加载失败 | API 500 | 进入页面 | 展示错误 + 重试按钮 | 组件 |
| | 空列表 | API 返回 `[]` | 进入页面 | 展示 empty state | 组件 |
| | 创建成功 | 填写有效表单 | 点击提交 | 成功提示 + 列表刷新 | 集成 |
| | 网络断开 | 离线状态 | 点击提交 | 离线提示 | 单元 |

### 13.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| API 请求 | MSW (Mock Service Worker) | |
| 浏览器 API | jsdom / happy-dom | |
| localStorage | 自定义 mock | |
| 路由 | MemoryRouter | |

---

## 14. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| | | | |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 15. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| | | 🔴/🟡/🟢 | 高/中/低 | | |

---

## 16. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| | | |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| | |
