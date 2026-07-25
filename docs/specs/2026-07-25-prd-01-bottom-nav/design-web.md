# Web 端技术方案：PRD-01 底部导航与应用路由

> 创建日期：2026-07-25
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Web 端继续沿用 `web/CLAUDE.md` 约束的五层架构，不引入新的全局状态或后端调用，重点是在 Next.js App Router 下补齐占位路由，并通过首页入口把新增路由串起来。

```
┌─────────────────────────────────────────────────┐
│ Pages / Routes（src/app/）                      │
│ ├── /                 → HomePage                │
│ ├── /search           → SearchPage              │
│ ├── /rankings         → RankingsPage            │
│ ├── /mall             → MallPage                │
│ ├── /play/[id]        → PlayPage                │
│ └── /detail/[id]      → DetailPage              │
├─────────────────────────────────────────────────┤
│ Feature（src/features/）                        │
│ ├── home/HomeScreen                            │
│ ├── placeholder-route/PlaceholderRouteScreen   │
│ ├── player/PlayerScreen                        │
│ └── drama-detail/DramaDetailScreen             │
├─────────────────────────────────────────────────┤
│ Shared UI（src/components/ui/）                 │
│ ├── Container                                   │
│ └── Card                                        │
├─────────────────────────────────────────────────┤
│ State / Data                                    │
│ ├── Global State：本期不新增                     │
│ ├── Server State：本期不涉及                     │
│ └── Local State：仅组件只读 props / 路由参数      │
├─────────────────────────────────────────────────┤
│ Core / Config（src/lib/）                       │
│ └── config.ts（应用名/版本/环境）                │
└─────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `web/src/app/page.tsx` | 不变 | 继续作为首页路由委托层，只渲染 `HomeScreen` |
| `web/src/app/play/[id]/page.tsx` | 小幅扩展 | 继续保留动态路由；metadata 与参数校验策略补齐 |
| `web/src/app/detail/[id]/page.tsx` | 小幅扩展 | 继续保留动态路由；metadata 与参数校验策略补齐 |
| `web/src/features/home/HomeScreen.tsx` | 扩展 | 补齐 `/search`、`/rankings`、`/mall` 首页入口 |
| `web/src/features/player/PlayerScreen.tsx` | 不变 | 继续作为播放页占位 Feature |
| `web/src/features/drama-detail/DramaDetailScreen.tsx` | 不变 | 继续作为详情页占位 Feature |
| `web/src/components/ui/*` | 不变 | 继续复用 `Container`、`Card` 作为占位页基础布局 |
| `web/src/lib/api-client.ts` | 不变 | 本期不新增 API 调用 |

### 1.2 新增路由的组织方式

本期不引入路由分组或额外 layout，保持当前扁平 App Router 结构，直接在 `src/app/` 下新增三个一级 segment：

- `web/src/app/search/page.tsx`
- `web/src/app/rankings/page.tsx`
- `web/src/app/mall/page.tsx`

原因：

1. 当前 `app/` 下已有 `/`、`/play/[id]`、`/detail/[id]` 的扁平目录结构，直接补齐新 segment 成本最低。
2. `/search`、`/rankings`、`/mall` 均为独立可访问路由，不需要共享嵌套 layout。
3. 现阶段页面都是占位页，Page 层应保持薄路由委托，实际 UI 复用下沉到 Feature 层。

为避免三个占位页重复实现，新增共享 Feature 模块 `placeholder-route/`：

- `PlaceholderRouteScreen.tsx`：统一渲染占位页标题、说明文案、当前路由语义。
- `routePlaceholders.ts`：维护三条占位路由的只读配置，供首页入口、页面 metadata、占位组件共同消费。

这样既不引入全局状态，也能把“路由语义”“页面标题”“首页入口”收敛在同一处配置，降低后续频道替换时的改动面。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `web/src/app/search/page.tsx` | 新增 | 注册 `/search` 占位路由，导出 route-level metadata，委托 `PlaceholderRouteScreen` |
| `web/src/app/rankings/page.tsx` | 新增 | 注册 `/rankings` 占位路由，导出 route-level metadata，委托 `PlaceholderRouteScreen` |
| `web/src/app/mall/page.tsx` | 新增 | 注册 `/mall` 占位路由，导出 route-level metadata，委托 `PlaceholderRouteScreen` |
| `web/src/features/placeholder-route/PlaceholderRouteScreen.tsx` | 新增 | 统一承载搜索、排行、商城占位页 UI |
| `web/src/features/placeholder-route/routePlaceholders.ts` | 新增 | 定义标题、说明、href 等只读配置，供首页与页面复用 |
| `web/src/features/placeholder-route/index.ts` | 新增 | 统一导出占位路由 Feature |
| `web/src/features/placeholder-route/PlaceholderRouteScreen.test.tsx` | 新增 | 覆盖共享占位组件的代表性渲染 case |
| `web/src/features/home/HomeScreen.tsx` | 修改 | 首页补齐 `/search`、`/rankings`、`/mall` 导航入口，并保留 `/play/sample`、`/detail/sample` 示例入口 |
| `web/src/features/home/HomeScreen.test.tsx` | 修改 | 增加新增首页入口的断言 |
| `web/src/app/play/[id]/page.tsx` | 修改 | metadata 与动态参数校验策略对齐本方案 |
| `web/src/app/detail/[id]/page.tsx` | 修改 | metadata 与动态参数校验策略对齐本方案 |

> 说明：`web/src/app/layout.tsx` 的根 metadata 模板已满足本期需求，无需新增全局 layout 或全局导航容器。

---

## 3. 组件设计

### 3.1 组件层级树

```
RootLayout
├── HomePage (/)
│   └── HomeScreen
│       ├── AppInfoCard
│       ├── PrimaryRouteNav
│       │   ├── Link → /search
│       │   ├── Link → /rankings
│       │   └── Link → /mall
│       └── SecondarySampleNav
│           ├── Link → /play/sample
│           └── Link → /detail/sample
├── SearchPage (/search)
│   └── PlaceholderRouteScreen(route="search")
├── RankingsPage (/rankings)
│   └── PlaceholderRouteScreen(route="rankings")
├── MallPage (/mall)
│   └── PlaceholderRouteScreen(route="mall")
├── PlayPage (/play/[id])
│   └── PlayerScreen(dramaId)
└── DetailPage (/detail/[id])
    └── DramaDetailScreen(dramaId)
```

### 3.2 组件清单

| 组件名称 | 类型（Page / Section / Atom） | 职责 | Props 接口 |
|---------|------------------------------|------|-----------|
| `HomeScreen` | Page Feature | 展示应用信息，并补齐首页导航入口 | 无 |
| `PlaceholderRouteScreen` | Page Feature | 统一渲染搜索/排行/商城占位页 | `title: string`、`description: string`、`pathLabel: string` |
| `PlayerScreen` | Page Feature | 播放页占位展示，读取 `dramaId` | `dramaId: string` |
| `DramaDetailScreen` | Page Feature | 详情页占位展示，读取 `dramaId` | `dramaId: string` |
| `Container` | Shared UI | 统一页面宽度与外边距 | 现有接口不变 |
| `Card` | Shared UI | 占位页卡片容器 | 现有接口不变 |

### 3.3 组件接口定义

```typescript
interface PlaceholderRouteScreenProps {
  title: string;
  description: string;
  pathLabel: string;
}

export function PlaceholderRouteScreen({
  title,
  description,
  pathLabel,
}: PlaceholderRouteScreenProps) {
  // 仅渲染静态占位内容，不持有客户端状态
}
```

```typescript
export const routePlaceholders = {
  search: {
    href: '/search',
    title: '搜索',
    description: '搜索页占位，后续 PRD 在此接入真实搜索体验。',
  },
  rankings: {
    href: '/rankings',
    title: '排行榜',
    description: '排行页占位，后续 PRD 在此接入榜单与排序能力。',
  },
  mall: {
    href: '/mall',
    title: '商城',
    description: '商城页占位，后续 PRD 在此接入商品与权益能力。',
  },
} as const;
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| Page → Feature | Props | `page.tsx` 向 `PlaceholderRouteScreen`、`PlayerScreen`、`DramaDetailScreen` 传入只读数据 |
| Feature 内部 | 本地只读常量 | 首页入口配置与占位页配置放在 feature 内部常量模块，不提升为全局 store |
| 路由参数 | Next.js App Router `params` | `/play/[id]`、`/detail/[id]` 读取动态参数 |

### 3.5 首页导航入口补齐思路

当前 `HomeScreen` 只有 `/play/sample` 与 `/detail/sample` 两个示例入口，不足以覆盖 PRD 要求的路由骨架验证。本期建议调整为“两层入口”：

1. `PrimaryRouteNav`：新增 `/search`、`/rankings`、`/mall` 三个主入口，体现 Web 端已具备与频道规划一致的可访问路由骨架。
2. `SecondarySampleNav`：保留现有 `/play/sample`、`/detail/sample` 示例链接，继续承担二级路由验证入口。

这样既满足“首页入口补齐”，也不把 Web 强行做成移动端底部 TabBar，符合 spec 中“Web 只需路由承载，不要求底部导航视觉实现”的范围约束。

### 3.6 响应式设计

| 断点 | 宽度范围 | 布局策略 | 关键变化 |
|------|---------|---------|---------|
| Mobile | < 768px | 单列卡片布局 | 入口链接纵向换行或自动换行 |
| Tablet | 768px - 1024px | 单列居中布局 | 主入口与示例入口分组展示 |
| Desktop | > 1024px | 单列居中布局 | 保持当前 Container 宽度，不额外引入侧边栏 |

### 3.7 无障碍（A11y）

| 关注点 | 策略 |
|--------|------|
| 语义化 HTML | 首页入口使用 `<nav>`，占位页使用 `<main>` + `<h1>` |
| 键盘导航 | 所有入口维持可 Tab 聚焦的 `Link` |
| 屏幕阅读器 | 页面标题与链接文案直接表达“搜索 / 排行榜 / 商城”语义 |
| 焦点管理 | 继续由浏览器默认焦点流和 Next.js 路由切换行为承载，本期不新增复杂焦点控制 |

---

## 4. 状态管理方案

### 4.1 方案选择

| 维度 | 选择 | 理由 |
|------|------|------|
| 全局状态 | 本期不新增 | 路由骨架不需要跨页面共享状态 |
| 服务端状态 | 本期不涉及 | 不新增后端 API，不做远端数据获取 |
| 表单状态 | 本期不涉及 | 搜索页仅为占位页，没有真实输入逻辑 |
| 路由状态 | Next.js App Router | 使用文件系统路由表达页面状态 |
| URL 状态 | `params` / `pathname` | 动态参数仅来自 `/play/[id]`、`/detail/[id]` |

### 4.2 状态划分

| 状态名称 | 范围（全局/页面/局部） | 存储方式 | 跨页面持久化 |
|---------|----------------------|---------|------------|
| 首页入口配置 | 局部 | feature 内只读常量 | 否 |
| 占位页展示文案 | 局部 | feature 内只读常量 | 否 |
| 动态路由参数 `id` | 页面 | App Router `params` | 由 URL 天然持久化 |

### 4.3 无新增全局状态方案

本期明确不新增 Zustand、Redux、Context 全局 store，也不新增 TanStack Query / SWR 一类服务端状态缓存层。

原因：

1. 新增页面全部为占位页，不存在跨路由共享业务状态。
2. 首页新增入口只是静态导航链接，不需要选中态同步到全局。
3. 动态路由页已经可通过 URL 参数还原，不需要额外缓存或持久化。

### 4.4 服务端状态管理

本期不涉及服务端状态管理，不新增请求 Hook，不新增缓存策略，不修改 `web/src/lib/api-client.ts`。

### 4.5 状态流转

```
用户访问 URL
  → App Router 命中 page.tsx
  → Server Component 读取只读配置 / params
  → 渲染 Feature 组件
  → 用户点击 Link
  → Next.js 路由跳转到目标占位页
```

---

## 5. 路由设计

### 5.1 路由清单

| 路径 Pattern | 页面组件 | 参数 | 认证守卫 | 懒加载 | 说明 |
|-------------|---------|------|---------|--------|------|
| `/` | `HomePage` | — | 否 | 否 | 首页，补齐路由导航入口 |
| `/search` | `SearchPage` | — | 否 | 否 | 搜索占位页 |
| `/rankings` | `RankingsPage` | — | 否 | 否 | 排行占位页 |
| `/mall` | `MallPage` | — | 否 | 否 | 商城占位页 |
| `/play/[id]` | `PlayPage` | `id: string` | 否 | 否 | 播放占位页 |
| `/detail/[id]` | `DetailPage` | `id: string` | 否 | 否 | 详情占位页 |

### 5.2 路由层级

```
app/
├── layout.tsx
├── page.tsx
├── search/
│   └── page.tsx
├── rankings/
│   └── page.tsx
├── mall/
│   └── page.tsx
├── play/
│   └── [id]/page.tsx
├── detail/
│   └── [id]/page.tsx
├── loading.tsx
├── error.tsx
└── not-found.tsx
```

本期保持单一 Root Layout，不新增 route group、parallel route、intercepting route。

### 5.3 路由守卫

本期无登录态与权限模型，所有页面均为公开页面，不新增认证守卫。

### 5.4 数据预取与加载

| 页面 | 预取方式 | 预取内容 |
|------|---------|---------|
| `/` | Next.js `Link` 默认预取 | 首页到静态占位页的路由资源 |
| `/search` | 无额外预取 | 本期不拉取搜索数据 |
| `/rankings` | 无额外预取 | 本期不拉取榜单数据 |
| `/mall` | 无额外预取 | 本期不拉取商城数据 |
| `/play/[id]` | 无额外预取 | 仅消费 `params.id` |
| `/detail/[id]` | 无额外预取 | 仅消费 `params.id` |

### 5.5 动态参数校验

对 `/play/[id]` 与 `/detail/[id]` 建议在 Page 层先做最小化参数校验：

- `id.trim()` 为空时走 `notFound()`。
- 非空时继续渲染占位页。
- 本期不做远端存在性校验，因为 spec 与 design.md 已明确“本期不新增后端 API”。

---

## 6. API 调用层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | 本期不涉及 | 不新增接口调用 |
| 请求拦截器 | 本期不涉及 | 不新增 token、日志等网络逻辑 |
| 响应拦截器 | 本期不涉及 | 不新增统一错误处理扩展 |
| 响应校验 | 本期不涉及 | 无新增接口响应可校验 |

### 6.2 客户端封装

- `web/src/lib/api-client.ts` 保持不变。
- 本需求文档不新增任何 RESTful API 调用。
- 不臆造搜索、排行、商城接口。

### 6.3 请求重试策略

本期不涉及。

### 6.4 请求 Hook 封装

本期不涉及。

---

## 7. SSR / CSR 策略

### 7.1 渲染策略选择

| 页面 | 渲染策略 | 原因 |
|------|---------|------|
| `/` | SSR-first（Server Component） | 首页当前为静态信息 + 导航链接，适合直接服务端输出 |
| `/search` | SSR-first（Server Component） | 占位页无需客户端状态 |
| `/rankings` | SSR-first（Server Component） | 占位页无需客户端状态 |
| `/mall` | SSR-first（Server Component） | 占位页无需客户端状态 |
| `/play/[id]` | SSR-first（Server Component） | 仅依赖 `params.id`，无需 `use client` |
| `/detail/[id]` | SSR-first（Server Component） | 仅依赖 `params.id`，无需 `use client` |

### 7.2 框架支持

- 继续使用 Next.js App Router。
- Page 层全部保持 Server Component。
- Feature 层默认写成纯展示组件，不引入 `use client`。
- 本期不新增客户端导航容器，不模拟移动端 Tab 状态保持。

### 7.3 数据预取策略

| 页面 | 预取方法 | 预取数据 |
|------|---------|---------|
| 静态占位页 | 无 | 不拉取任何接口数据 |
| 动态占位页 | 读取 `params` | 不访问后端 |

### 7.4 metadata 策略

沿用 `web/src/app/layout.tsx` 中已有的全局 metadata 模板：

- `default: "ShortDrama"`
- `template: "%s — ShortDrama"`

各页面策略如下：

| 页面 | metadata 方案 | 说明 |
|------|--------------|------|
| `/` | 可选补 `export const metadata` | 若需要可显式声明“首页”，否则沿用 root 默认标题 |
| `/search` | `export const metadata` | 标题为“搜索” |
| `/rankings` | `export const metadata` | 标题为“排行榜” |
| `/mall` | `export const metadata` | 标题为“商城” |
| `/play/[id]` | `generateMetadata` | 基于路由参数生成“播放”语义标题，可带 `id`，不发请求 |
| `/detail/[id]` | `generateMetadata` | 基于路由参数生成“详情”语义标题，可带 `id`，不发请求 |

补充约束：

1. metadata 仅表达路由语义，不依赖任何后端数据。
2. 本期不新增 Open Graph、JSON-LD、sitemap、canonical 配置扩展。
3. 为避免标题与页面文案漂移，`/search`、`/rankings`、`/mall` 的标题建议与 `routePlaceholders.ts` 共用同一份只读配置。

---

## 8. 性能优化

### 8.1 优化清单

| 优化项 | 策略 | 目标 |
|--------|------|------|
| 代码复用 | 三个新增占位页复用同一个 `PlaceholderRouteScreen` | 降低重复代码与维护成本 |
| 首屏输出 | 保持 Server Component 直出 | 避免无意义客户端 hydration 负担 |
| 路由分段 | 使用 App Router 自然分段 | 不新增自定义路由容器 |
| Bundle 控制 | 不新增依赖 | 避免为占位页引入额外体积 |
| 缓存策略 | 本期不新增 | 因为无新增远端数据 |

### 8.2 加载体验

| 场景 | 策略 |
|------|------|
| 首页加载 | 继续 SSR 直出应用信息与入口链接 |
| 静态占位页跳转 | 依赖 Next.js 路由切换与默认预取 |
| 动态占位页刷新 | 由服务器根据 URL 重新渲染，不依赖客户端缓存 |

---

## 9. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| 应用名称 | `NEXT_PUBLIC_APP_NAME` | 现有配置 | 现有配置 | 继续由 `config.ts` 提供首页展示 |
| 应用版本 | `NEXT_PUBLIC_APP_VERSION` | 现有配置 | 现有配置 | 继续由 `config.ts` 提供首页展示 |
| 运行环境 | `NODE_ENV` | `development` | `production` | 已由 Next.js 提供 |

结论：

- 本期不新增环境变量。
- 本期不新增 API base URL 使用场景。
- 页面路由、metadata、占位文案均不依赖环境配置。

---

## 10. API 调用清单

本期不涉及 API 调用，不新增、不修改、不废弃任何 Web 端接口消费。

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| — | 本期不涉及 | — | — | — |

---

## 11. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Web 端实现方式 |
|---------|---------------|---------------|
| 公开路由命名统一 | 对外统一使用 `play`、`detail` | 保留现有 `/play/[id]`、`/detail/[id]`，不再引入别名路由 |
| 占位页策略 | 各频道与子页面都提供占位内容 | `/search`、`/rankings`、`/mall` 使用统一占位 Feature 承载 |
| 无后端依赖 | 本期仅做客户端导航骨架 | 文档中明确 Web 端不新增 API、不接入缓存 |
| 路由可访问性 | `/`、`/play/[id]`、`/detail/[id]`、`/search`、`/rankings`、`/mall` 均可访问 | 在 `src/app/` 注册对应 `page.tsx` |
| 二级路由语义 | `play/:id`、`detail/:id` 属于子页面路由 | Web 端通过首页示例入口进入，不引入移动端 Tab 容器 |

---

## 12. 边界与错误处理

### 12.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| Page 层 | `notFound()` + `not-found.tsx` | 动态参数非法时回落到 404 |
| App 层 | `error.tsx` | 继续复用 Next.js App Router 全局错误边界 |
| 网络层 | 本期不涉及 | 无新增 API 调用 |
| 日志 | `console.error`（沿用现有 error 边界） | 本期不新增监控 SDK |

### 12.2 错误码映射表

| 后端错误码 / 端内语义 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `INVALID_ROUTE_PARAMS` | 页面参数无效 | `notFound()` 或展示 404 |
| `UNSUPPORTED_ROUTE` | 暂不支持该页面 | 访问未注册路由时进入 `not-found.tsx` |
| `NAVIGATION_STATE_LOST` | 本期不涉及 | Web 无多 Tab 状态恢复 |
| `TAB_STATE_RESTORED_PARTIALLY` | 本期不涉及 | Web 无多 Tab 状态恢复 |
| `DEEPLINK_CONTAINER_NOT_READY` | 本期不涉及 | Web 不处理 `djsdrama://` deeplink |

### 12.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 动态参数为空白 | `/play/%20`、`/detail/%20` | trim 后判空，进入 `notFound()` | 🔴 |
| 路由未注册 | 访问不存在的路径 | 由现有 `not-found.tsx` 处理 | 🟡 |
| metadata 与页面文案不一致 | 手工复制标题时遗漏 | 使用 `routePlaceholders.ts` 共享标题配置 | 🟡 |
| 首页入口遗漏 | 新页面已注册但首页无链接 | 通过 `HomeScreen.test.tsx` 约束入口完整性 | 🟡 |

### 12.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| `HomeScreen` | 无 | 展示应用信息与导航入口 | 不适用 | 不适用 | 渲染异常由 `error.tsx` 处理 |
| `PlaceholderRouteScreen` | 无 | 展示标题、路径语义与占位说明 | 不适用 | 不适用 | 渲染异常由 `error.tsx` 处理 |
| `PlayerScreen` | 无 | 展示 `dramaId` 与占位说明 | 不适用 | 不适用 | 非法参数走 `notFound()` |
| `DramaDetailScreen` | 无 | 展示 `dramaId` 与占位说明 | 不适用 | 不适用 | 非法参数走 `notFound()` |

---

## 13. 测试策略

### 13.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 组件测试 | `HomeScreen` 新增入口、`PlaceholderRouteScreen` 代表性渲染 | 关键路径全覆盖 | Vitest + Testing Library |
| 页面级 smoke test | `/search`、`/rankings`、`/mall` 对应 page 的 render / metadata | 路由文件最小可用覆盖 | Vitest |
| 现有回归测试 | `PlayerScreen`、`DramaDetailScreen` 现有展示能力 | 维持现有覆盖 | Vitest + Testing Library |
| E2E | 本期不新增 | 本需求仅为路由骨架，不新增 Playwright/Cypress | — |

### 13.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| W-01 | 首页补齐主入口 | 渲染 `HomeScreen` | 查询主导航链接 | 存在 `/search`、`/rankings`、`/mall` 三个入口 | 组件 |
| W-02 | 首页保留现有二级示例入口 | 渲染 `HomeScreen` | 查询示例链接 | 仍存在 `/play/sample`、`/detail/sample` | 组件 |
| W-03 | 占位页共享组件渲染 | 传入 `title/description/pathLabel` | 渲染 `PlaceholderRouteScreen` | 页面显示正确标题与说明 | 组件 |
| W-04 | 新增占位路由 page smoke | 遍历 `search/rankings/mall` page | 渲染页面或读取 metadata | 页面可渲染且标题符合预期 | 页面 smoke |
| W-05 | 播放页参数展示回归 | 渲染 `PlayerScreen` | 传入 `dramaId` | 继续展示 ID 与“播放页”占位 | 组件 |
| W-06 | 详情页参数展示回归 | 渲染 `DramaDetailScreen` | 传入 `dramaId` | 继续展示 ID 与“详情页”占位 | 组件 |

### 13.3 用例复用策略

`/search`、`/rankings`、`/mall` 三个页面交互同构，应遵循“相同交互复用单一 case”的约束：

1. 共享 UI 行为只在 `PlaceholderRouteScreen.test.tsx` 保留一个代表性组件 case。
2. 三个 page 文件使用表驱动 smoke test 验证 route key、title、render 是否正确。
3. 不为三个占位页分别写三套重复交互测试。

### 13.4 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| `next/link` | 继续沿用 Testing Library 渲染结果 | 当前 `HomeScreen.test.tsx` 模式可复用 |
| route 配置 | 直接导入只读常量 | 无需网络 mock |
| API 请求 | 本期不涉及 | 不新增 MSW |

---

## 14. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| — | — | 本期不新增 | 继续复用 Next.js、React、Vitest、Testing Library 现有能力 |

---

## 15. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 三个新增页面重复实现，后续替换成本高 | Web 路由骨架 | 🟡 | 中 | 通过 `PlaceholderRouteScreen` + `routePlaceholders.ts` 复用 | 拆回各自独立 page，但不推荐 |
| 首页新增入口与实际路由不一致 | 首页导航 | 🟡 | 中 | 首页入口与 route metadata 共用配置源 | 先保留最小手工维护，再补统一配置 |
| 动态路由参数非法导致异常展示 | `/play/[id]`、`/detail/[id]` | 🟡 | 中 | Page 层先 trim 校验，非法则 `notFound()` | 兜底展示占位文案 |
| 误将本期扩展成搜索/排行真实功能 | 开发范围控制 | 🟡 | 中 | 文档中明确“本期仅占位路由，不新增 API/状态管理/缓存” | 回退到纯静态占位实现 |

---

## 16. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/index.md` | 索引 | 确认 wiki 的功能/架构/API 组织方式 |
| `wiki/architecture/overview.md` | 整体架构 / 跨端涉及 | 确认 Web 使用 Next.js App Router，当前为骨架阶段 |
| `wiki/features/app-shell/index.md` | Web 入口与路由 | 确认现有 Web 仅有首页骨架，适合在现有结构上平铺新增路由 |
| `wiki/features/deeplink/index.md` | 覆盖端说明 | 确认 Web 不处理 `djsdrama://` deeplink |
| `wiki/api/dramas.md` | API 状态 | 确认当前无搜索/排行/商城相关接口，不应臆造 API |
| `wiki/api/player.md` | 播放器 API 状态 | 确认本期不需要新增播放器接口消费 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-25-prd-01-bottom-nav/spec.md` | 明确 Web 需补齐 `/search`、`/rankings`、`/mall` 路由且不需要底部 TabBar 视觉 |
| `docs/specs/2026-07-25-prd-01-bottom-nav/design.md` | 明确本期纯客户端导航骨架，不新增后端 API、缓存或数据库 |
| `web/src/app/page.tsx` | 首页 Page 层当前仅委托 `HomeScreen` |
| `web/src/app/play/[id]/page.tsx` | 已有播放页动态路由与 metadata 结构 |
| `web/src/app/detail/[id]/page.tsx` | 已有详情页动态路由与 metadata 结构 |
| `web/src/app/layout.tsx` | 已有根 metadata 模板可直接复用 |
| `web/src/features/home/HomeScreen.tsx` | 当前首页只有 `/play/sample`、`/detail/sample` 两个入口，需要补齐主入口 |
| `web/src/features/home/HomeScreen.test.tsx` | 已有首页入口断言模式，可扩展为搜索/排行/商城入口测试 |
| `web/src/features/player/PlayerScreen.test.tsx` | 已有播放页占位回归测试 |
| `web/src/features/drama-detail/DramaDetailScreen.test.tsx` | 已有详情页占位回归测试 |
| `.claude/skills/feature-workflow/assets/design-web-template.md` | 本文档模板来源 |
| `.claude/skills/feature-workflow/references/web-design/frontend-design.md` | Web 端方案设计规范参考 |
