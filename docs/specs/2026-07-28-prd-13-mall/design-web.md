# Web 端技术方案：PRD-13 商城

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
app/mall/page.tsx (Server Route Shell)
→ features/mall/MallPageScreen.tsx (Client Feature)
  → features/mall/components/MallHeader
  → features/mall/components/MallShortcutGrid
  → features/mall/components/MallBannerCarousel
  → features/mall/components/MallProductGrid
  → features/mall/components/MallLoginIntercept
→ lib/mall/api.ts / lib/api-client.ts
→ GET /api/mall/products

app/mall/product/[id]/page.tsx
→ features/mall/MallProductPlaceholderScreen.tsx

features/mall/bridge/mall-bridge.ts
→ Native bridge available ? openSearch/requestLogin
→ fallback browser navigation (/search)
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `web/src/app/mall/page.tsx` | 修改 | 从占位路由改为 mall route shell |
| `web/src/app/search/page.tsx` | 不变 | 继续作为浏览器 / 本地开发模式下的搜索降级承接 |
| `web/src/features/placeholder-route/` | 不变 | 仍可复用到商品详情占位页的部分空态样式 |
| `web/src/lib/api-client.ts` | 扩展 | 继续承载商城 API 调用，不引入新请求库 |
| `web/src/lib/schemas.ts` | 扩展 | 新增商城实体与响应 schema |
| `web/src/lib/config.ts` | 扩展 | 补充商城路由 / bridge 开关等受控配置 |
| `web/src/features/mall/` | 新增 | 商城 H5 专属 feature 层 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `web/src/app/mall/page.tsx` | 修改 | 路由委托到 `MallPageScreen` |
| `web/src/app/mall/product/[id]/page.tsx` | 新增 | 商品详情 H5 占位页路由 |
| `web/src/features/mall/MallPageScreen.tsx` | 新增 | 商城首页主 Feature 组件 |
| `web/src/features/mall/MallProductPlaceholderScreen.tsx` | 新增 | 商品详情占位页 Feature |
| `web/src/features/mall/components/*` | 新增 | 顶部栏、快捷入口、banner、双列商品卡、登录拦截层等组件 |
| `web/src/features/mall/hooks/useMallPage.ts` | 新增 | 商城首页状态机、分页逻辑与登录态同步 |
| `web/src/features/mall/bridge/mall-bridge.ts` | 新增 | H5 与 Native 宿主桥接适配层 |
| `web/src/features/mall/bridge/mall-host-sync.ts` | 新增 | 处理 Native → H5 的 auth/context 同步消息 |
| `web/src/features/mall/config/mall-seed.ts` | 新增 | banner / shortcut 集中配置模块 |
| `web/src/lib/schemas.ts` | 修改 | 新增 `MallProductSchema`、`MallBannerSchema`、`MallLoginContextSchema` 等 |
| `web/src/lib/api-client.ts` | 修改 | 修正错误解析适配 `{ error: { code, message } }`，并复用到 mall API |
| `web/src/lib/config.ts` | 修改 | 新增 mall 相关受控配置读取 |
| `web/src/features/mall/*.test.tsx` | 新增 | 覆盖状态机、bridge 降级、登录拦截与列表错误态 |

---

## 3. 组件设计

### 3.1 组件层级树

```text
MallPageScreen
├── MallPageLayout
│   ├── MallHeader
│   │   ├── MallSearchEntry
│   │   └── MallCartEntry
│   ├── MallShortcutGrid
│   │   └── MallShortcutCard × 5
│   ├── MallBannerCarousel
│   │   └── MallBannerSlide × N
│   ├── MallProductSection
│   │   ├── MallProductGrid
│   │   │   └── MallProductCard × N
│   │   ├── MallListFooterState
│   │   └── MallEmptyState / MallErrorState
│   └── MallLoginInterceptOverlay (conditional)
└── MallToastHost (optional lightweight feedback)

MallProductPlaceholderScreen
├── MallProductPlaceholderCard
└── BackToMallAction
```

### 3.2 组件清单

| 组件名称 | 类型（Page / Section / Atom） | 职责 | Props 接口 |
|---------|------------------------------|------|-----------|
| `MallPageScreen` | Page | 商城首页路由承接与状态装配 | — |
| `MallHeader` | Section | 展示搜索入口与购物车入口 | `onSearch`, `onCart` |
| `MallShortcutGrid` | Section | 渲染 5 个快捷入口 | `shortcuts`, `onShortcutClick` |
| `MallBannerCarousel` | Section | 渲染受控 banner 列表 | `banners`, `onBannerClick` |
| `MallProductGrid` | Section | 双列商品 Feed 渲染 | `items`, `onProductClick`, `onLoadMore` |
| `MallProductCard` | Atom | 展示商品图、标题、价格、标签 | `product`, `onClick` |
| `MallLoginInterceptOverlay` | Section | 匿名商品点击后的 H5 页内拦截层 | `product`, `visible`, `onContinueLogin`, `onCancel` |
| `MallProductPlaceholderScreen` | Page | 已登录商品点击后的详情占位页 | `productId` |

### 3.3 组件接口定义

```typescript
interface MallProductCardProps {
  product: MallProduct;
  onClick: (product: MallProduct) => void;
}

interface MallLoginInterceptOverlayProps {
  visible: boolean;
  product: MallProduct | null;
  onContinueLogin: () => void;
  onCancel: () => void;
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Props | 商品、banner、shortcut、加载态 |
| 子 → 父 | Callback Props | 搜索、商品点击、快捷入口点击 |
| 跨层级 | Feature hook + 局部 context（如 Toast） | 首页状态机、bridge 能力、反馈消息 |
| 兄弟组件 | 提升状态到 `useMallPage` | 登录拦截层与商品列表共享当前商品上下文 |

### 3.5 响应式设计

| 断点 | 宽度范围 | 布局策略 | 关键变化 |
|------|---------|---------|---------|
| Mobile | `< 768px` | 固定双列商品网格 | 贴合 Native 容器首版布局 |
| Tablet | `768px - 1024px` | 双列 + 居中容器 | 放宽卡片宽度与区块边距 |
| Desktop | `> 1024px` | 仍保持移动稿居中容器 | 不扩展为 PC 商城站布局 |

### 3.6 无障碍（A11y）

| 关注点 | 策略 |
|--------|------|
| 语义化 HTML | 搜索/购物车/快捷入口/商品卡使用 button / link 语义 |
| 键盘导航 | 登录拦截层支持 Escape 关闭，焦点锁定在弹层内 |
| 屏幕阅读器 | banner、商品图、快捷入口提供 `aria-label` / 替代文本 |
| 色彩对比度 | 文本、价格、按钮满足 WCAG AA |

---

## 4. 状态管理方案

### 4.1 方案选择

| 维度 | 选择 | 理由 |
|------|------|------|
| 全局状态 | 不新增全局 store | 商城状态主要局限在 `/mall` 路由内 |
| 服务端状态 | 自定义 hook + `api-client.ts` | 避免新增 TanStack Query 等依赖 |
| 表单状态 | 无 | 本期没有表单输入 |
| 路由状态 | Next.js App Router | 与现有 Page 层约束一致 |
| URL 状态 | `productId` 使用动态路由；首页不暴露分页 query | 降低 Native 容器与浏览器模式差异 |

### 4.2 状态划分

| 状态名称 | 范围（全局/页面/局部） | 存储方式 | 跨页面持久化 |
|---------|----------------------|---------|------------|
| `items` / `page` / `hasNextPage` | 页面 | `useReducer` / `useState` | 否 |
| `isLoading` / `isAppending` | 页面 | `useReducer` | 否 |
| `appendError` | 页面 | `useReducer` | 否 |
| `loginInterceptVisible` | 页面 | `useState` | 否 |
| `activeProduct` | 页面 | `useState` | 否 |
| `isLoggedIn` | 页面 | `useReducer` / host sync | 否 |
| `pendingRestoreReason` | 页面 | `useReducer` | 否 |
| `containerMode` | 页面初始化 | 运行时探测 bridge | 否 |
| `bannerConfig` / `shortcuts` | 页面常量 | 集中配置模块 | 是（构建期常量） |

### 4.3 全局状态结构

```typescript
interface MallPageState {
  items: MallProduct[];
  page: number;
  hasNextPage: boolean;
  isLoading: boolean;
  isAppending: boolean;
  errorMessage: string | null;
  appendError: string | null;
  loginInterceptVisible: boolean;
  activeProduct: MallProduct | null;
  isLoggedIn: boolean;
  pendingRestoreReason: 'search-return' | 'login-return' | 'container-recreated' | null;
}
```

### 4.4 服务端状态管理

```typescript
async function fetchMallProducts(query: MallProductsQuery): Promise<MallProductsResponse> {
  const result = await api.get('/api/mall/products', {
    params: {
      page: query.page,
      pageSize: query.pageSize,
    },
  });

  return MallProductsResponseSchema.parse(result);
}
```

### 4.5 状态流转

```text
进入 /mall
→ loadFirstPage
→ success(content) | empty | error

滚动触底
→ appendNextPage
→ appendSuccess | appendError(keep items)

匿名点击商品
→ showLoginInterceptOverlay
→ cancel => hide overlay
→ continue => bridge.requestLogin(context)
```

---

## 5. 路由设计

### 5.1 路由清单

| 路径 Pattern | 页面组件 | 参数 | 认证守卫 | 懒加载 | 说明 |
|-------------|---------|------|---------|--------|------|
| `/mall` | `MallPageScreen` | — | 否 | 否 | 商城首页 H5 |
| `/mall/product/[id]` | `MallProductPlaceholderScreen` | `id: string` | 否（但进入前由业务登录分流控制） | 否 | 商品详情占位页 |
| `/search` | `SearchPage` | — | 否 | 否 | 浏览器 / 本地开发模式下的降级承接 |

### 5.2 路由层级

```text
App Router
├── /mall
│   └── MallPageScreen
└── /mall/product/[id]
    └── MallProductPlaceholderScreen
```

### 5.3 路由守卫

- `/mall` 不设置认证守卫，匿名用户可浏览列表。
- `/mall/product/[id]` 不在路由层做登录守卫；是否允许跳转由 `/mall` 页内点击逻辑先判断登录态并做分流。
- 直接浏览器访问 `/mall/product/[id]` 时只做 `id` 参数合法性校验，合法则展示详情占位页；非法则回退 404 或商城首页受控错误页。

### 5.4 数据预取与加载

| 页面 | 预取方式 | 预取内容 |
|------|---------|---------|
| `/mall` | 客户端首次加载 | 商品第一页、banner config、shortcut config |
| `/mall/product/[id]` | 服务端参数校验 + 客户端静态渲染 | 仅 `id` 参数，不额外请求真实商品详情 |

---

## 6. API 调用层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | `web/src/lib/api-client.ts` | 继续使用 fetch wrapper |
| 请求参数构造 | `lib/mall/api.ts` | 商城 API 专用封装 |
| 响应校验 | Zod schema | `MallProductsResponseSchema` |
| bridge 适配 | `features/mall/bridge/mall-bridge.ts` | 封装 Native 搜索与登录消息 |

### 6.2 客户端封装

```typescript
export const mallApi = {
  async getProducts(query: MallProductsQuery): Promise<MallProductsResponse> {
    const result = await api.get('/api/mall/products', {
      params: { page: query.page, pageSize: query.pageSize },
    });
    return MallProductsResponseSchema.parse(result);
  },
};
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 首屏请求失败 | 0（手动重试） | — | 通过页面“重试”按钮重新发起 |
| 分页追加失败 | 0（手动重试） | — | 通过尾部重试入口再次请求当前页 |
| H5 资源加载失败 | 依赖浏览器 / 容器刷新 | — | 由 Native 容器负责整页重试 |

### 6.4 请求 Hook 封装

- `useMallPage` 统一处理：
  - 首屏 loading / empty / error
  - append loading / append error
  - 请求 token 防乱序
  - `AbortController` 取消已过期请求
  - Native → H5 的 `mall.syncAuthState` / `mall.restoreContext` 消息订阅
- `mall-host-sync.ts` 负责把宿主消息映射为 Web 可消费的事件：
  - `mall.syncAuthState`：刷新 `isLoggedIn`，使匿名/已登录商品点击路径可稳定切换；
  - `mall.restoreContext(reason='login-return')`：关闭登录拦截层，必要时刷新首页状态；
  - `mall.restoreContext(reason='search-return')`：重新聚焦商城页，不强制丢弃已有列表；
  - `mall.restoreContext(reason='container-recreated')`：允许退化为重新加载 `/mall` 首屏。
- `api-client.ts` 需要补齐对 `{ error: { code, message } }` 的解析，而不是只读顶层 `message`。

---

## 7. SSR / CSR 策略

### 7.1 渲染策略选择

| 页面 | 渲染策略 | 原因 |
|------|---------|------|
| `/mall` | CSR-first（Route Shell 可为 Server Component） | 需要运行时 bridge 探测、滚动分页、登录拦截层 |
| `/mall/product/[id]` | SSR shell + 轻量客户端交互 | 仅参数校验与占位说明 |

### 7.2 框架支持

- Next.js App Router 页面层继续只做路由委托。
- 业务 UI 与状态逻辑下沉到 `src/features/mall/`。
- 不引入新状态管理或数据请求框架。

### 7.3 数据预取策略

| 页面 | 预取方法 | 预取数据 |
|------|---------|---------|
| `/mall` | 客户端 `useEffect` / mount 触发 | 商品第一页 |
| `/mall/product/[id]` | 路由参数校验 | 不预取真实详情 |

### 7.4 SEO 策略

- 本期商城主要服务于 Native WebView 承载，不以 SEO 为目标。
- 仍保留基础 `title` / `description`，但不为商品详情占位页补充结构化数据。

---

## 8. 性能优化

### 8.1 优化清单

| 优化项 | 策略 | 目标 |
|--------|------|------|
| 首屏加载 | 先渲染静态区块骨架，再请求商品第一页 | 进入 `/mall` 后快速看到页面结构 |
| 商品列表 | 双列网格 + 图片懒加载 | 减少首屏图片压力 |
| 请求防抖 | 触底加载时单页仅一个请求在途 | 防止重复追加 |
| 组件拆分 | `MallProductCard`、`MallBannerCarousel` 等分层组件化 | 降低重渲染范围 |
| 资源缓存 | 复用浏览器图片缓存 | 提高回到商城时体验 |

### 8.2 加载体验

| 场景 | 策略 |
|------|------|
| 首屏加载 | 静态区块骨架 + Feed skeleton |
| 路由切换 | 维持 App Router 默认切换；商品详情占位页提供即时内容 |
| 数据加载 | 首屏错误态 / 空态 / 尾部追加错误分离 |

---

## 9. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| API Base URL | `NEXT_PUBLIC_API_URL` | 现有配置 | 现有配置 | 继续复用现有后端 API 基址 |
| 应用名 | `NEXT_PUBLIC_APP_NAME` | 现有配置 | 现有配置 | 复用现有 config |
| 应用版本 | `NEXT_PUBLIC_APP_VERSION` | 现有配置 | 现有配置 | 复用现有 config |
| Mall bridge 开关（可选） | `NEXT_PUBLIC_MALL_BRIDGE_ENABLED` | `true/false` | `true/false` | 仅用于本地开发显式控制宿主模式 |

> ⚠️ 禁止硬编码任何常量。banner、shortcut、H5 bridge 开关、API 地址都应集中在 config / feature config 中，不散落在组件里。

---

## 10. API 调用清单

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `GET /api/mall/products` | 首次进入 `/mall` | 默认 `page=1&pageSize=20` | 初始化商品列表状态 | 首屏错误态 + 重试 |
| `GET /api/mall/products` | 触底加载更多 | 当前 `page + 1` | 追加商品列表 | 尾部错误提示 + 重试 |

---

## 11. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Web 端实现方式 |
|---------|---------------|---------------|
| banner 集中配置 | Web 单点管理 banner seed/config | `features/mall/config/mall-seed.ts` 导出受控配置 |
| 搜索入口分流 | Native 走 bridge，浏览器走 `/search` | `mall-bridge.ts` 检测宿主能力后分流 |
| 搜索返回契约 | Native 搜索退出后回到 mall 上下文 | 订阅 `mall.restoreContext(reason='search-return')`，必要时重新聚焦商城页 |
| 登录拦截顺序 | 先 H5 页内拦截，再调用 Native 登录 | `MallLoginInterceptOverlay` + `requestLogin()` |
| 登录上下文 | `MallLoginContext` | 点击商品时组装 `{ source:'mall', productId, returnTarget:'/mall' }` |
| 登录态同步 | 宿主返回权威登录态 | 订阅 `mall.syncAuthState` 更新 `isLoggedIn`，登录成功后允许进入详情页 |
| 追加失败不清空列表 | append failure 保留 items | `useMallPage` 将首屏错误与追加错误分离 |
| 请求防乱序 | 旧请求不得覆盖新状态 | hook 内维护 `requestToken` + `AbortController` |
| 最低恢复保证 | 回到商城首页首屏即可 | 浏览器模式由路由天然保证；Native 模式由容器重载 `/mall` |

---

## 12. 边界与错误处理

### 12.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | `api-client.ts` + schema parse | 统一 HTTP / Network / Timeout 错误 |
| Feature 状态层 | `useMallPage` reducer | 区分首屏错误、空态、追加错误 |
| UI 层 | 内联错误区 + 轻提示 | 避免整页白屏 |
| bridge 层 | `mall-bridge.ts` fallback | 搜索 / 登录 bridge 失败时停留当前页 |

### 12.2 错误码映射表

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `VALIDATION_ERROR` | 请求参数异常，请稍后重试 | 首屏错误态 / 轻提示 |
| `INVALID_PARAMS` | 请求参数异常，请稍后重试 | 首屏错误态 / 轻提示 |
| `INTERNAL_ERROR` | 服务开小差了，请稍后重试 | 首屏错误态 / 尾部错误 |
| `SERVICE_UNAVAILABLE` | 服务暂不可用，请稍后再试 | 首屏错误态 / 尾部错误 |
| `NETWORK_ERROR` | 网络异常，请检查后重试 | 首屏错误态 / 尾部错误 |

### 12.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 首屏请求失败 | `/api/mall/products` 超时 / 5xx | 展示首屏错误态与重试按钮 | 🔴 |
| 分页请求失败 | 触底请求失败 | 保留已有列表，仅展示尾部错误 | 🔴 |
| bridge 不存在 | 浏览器 / 本地开发模式 | 自动 fallback 到 `/search` 或停留当前页 | 🔴 |
| bridge 调用抛错 | Native 宿主未注册能力 | 展示轻提示，不跳空白页 | 🔴 |
| Native 未同步登录态 | 容器首次加载后未收到 `mall.syncAuthState` | 默认按匿名处理并允许宿主稍后补发；超时后不直接放行详情 | 🔴 |
| 搜索返回商城 | Native 搜索页关闭后回到 H5 | 处理 `mall.restoreContext(reason='search-return')`；必要时重聚焦 `/mall` | 🔴 |
| 登录返回商城 | 登录取消 / 成功 / 关闭 | 处理 `mall.syncAuthState` + `mall.restoreContext(reason='login-return')`，关闭拦截层并刷新登录判断 | 🔴 |
| 快速重复点击商品 | 匿名用户连续点击 | 仅保留一个拦截层实例 | 🟡 |
| 商品图片失效 | `image_url` 加载失败 | 展示统一占位图 | 🟢 |
| banner 配置为空 | 无活动横幅 | 隐藏区块或展示空占位 | 🟢 |
| 直接进入详情页且 ID 非法 | `/mall/product/[id]` 非法参数 | 404 或回退商城首页 | 🟡 |

### 12.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `/mall` 首屏 | 商品骨架 + 静态区块先显 | 列表渲染 | 展示空态文案 | 首屏错误态 + 重试 | — |
| 商品列表尾部 | 尾部 loading | 继续追加 | — | 尾部错误 + 重试 | — |
| 登录拦截层 | — | 显示拦截文案 | — | bridge 失败轻提示 | 非法商品上下文时禁止打开 |
| `/mall/product/[id]` | 轻量加载 | 占位信息 | — | 参数非法回退 | — |

---

## 13. 测试策略

### 13.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | schema、bridge adapter、状态 reducer | 核心逻辑全覆盖 | Vitest |
| 组件测试 | 首页渲染、登录拦截、错误态、空态 | 关键交互全覆盖 | Testing Library |
| 集成测试 | `/mall` 路由 + mock API 行为 | 核心流程覆盖 | Vitest + MSW |

### 13.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| WEB-MALL-01 | 首页加载成功 | mock API 返回第一页 | 进入 `/mall` | 搜索/快捷入口/banner/商品网格都可见 | 组件 |
| WEB-MALL-02 | 首屏空列表 | API 返回 `data=[]` | 进入 `/mall` | 展示空态但顶部区块仍可见 | 组件 |
| WEB-MALL-03 | 首屏失败 | API 500 | 进入 `/mall` | 展示错误态与重试按钮 | 组件 |
| WEB-MALL-04 | 分页追加失败 | 第二页 API 失败 | 触底加载 | 已有列表保留，尾部出现错误提示 | 集成 |
| WEB-MALL-05 | 浏览器模式点搜索 | bridge 不可用 | 点击搜索 | 跳转 `/search` | 组件 |
| WEB-MALL-06 | 匿名点击商品 | 未登录 | 点击商品卡 | 出现登录拦截层 | 组件 |
| WEB-MALL-07 | 继续登录 bridge 成功 | bridge 可用 | 点击“继续登录” | 调用 `requestLogin(context)` | 单元 |
| WEB-MALL-08 | 取消登录拦截 | 拦截层已显示 | 点击取消 | 关闭弹层，列表状态不变 | 组件 |

### 13.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| API 请求 | MSW | 模拟第一页、分页失败、空列表 |
| Native bridge | 手动挂载 `window.__MALL_NATIVE_BRIDGE__` stub | 验证搜索 / 登录分流 |
| 路由 | Next.js navigation mock | 验证浏览器 fallback |
| 图片加载 | jsdom 占位 | 不依赖真实远端资源 |

---

## 14. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 复用现有 Next.js / React / Zod / Vitest / Testing Library |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 15. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 直接在组件里拼 bridge 逻辑导致 iOS/Android 行为分叉 | Web / iOS / Android | 🔴 | 中 | 抽象 `mall-bridge.ts` 统一 Web 侧宿主接口 | 浏览器模式全部 fallback |
| 引入额外状态库导致依赖膨胀 | Web | 🟡 | 中 | 使用 `useReducer` + 自定义 hook | 保持局部状态 |
| `api-client.ts` 与后端错误结构不一致 | Web / Backend | 🔴 | 中 | 同步修正 error parsing 以兼容 `{ error: { code, message } }` | 在 mall API 封装层临时兜底 |
| banner 配置散落到组件内 | Web | 🟡 | 中 | 集中到 `features/mall/config/mall-seed.ts` | 回退为单一 config 文件 |
| 详情页直接访问缺少商品详情数据 | Web | 🟢 | 高 | 首版明确只做占位页，不请求真实详情 | 展示固定占位内容 |

---

## 16. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | Web、已知限制 | `/mall` 当前仍为占位页，mall 应作为 H5 承载 |
| `wiki/features/search-discovery/index.md` | Web、入口与路由 | `/search` 当前为占位页，可作为浏览器模式 fallback |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `web/CLAUDE.md` | Web 五层架构、SSR-first、Core 层 API 封装约束 |
| `web/src/app/mall/page.tsx` | 当前 mall 占位页入口 |
| `web/src/app/search/page.tsx` | 浏览器模式下的搜索降级承接 |
| `web/src/lib/api-client.ts` | 现有 fetch 包装层 |
| `web/src/lib/config.ts` | 现有 config 入口 |
| `web/src/lib/types.ts` | 现有 `ApiError` / `NetworkError` 定义 |
| `web/src/features/placeholder-route/PlaceholderRouteScreen.tsx` | 现有占位路由组件可复用样式 |
| `docs/specs/2026-07-28-prd-13-mall/design.md` | 商城共享 bridge、分页与登录拦截 contract |
