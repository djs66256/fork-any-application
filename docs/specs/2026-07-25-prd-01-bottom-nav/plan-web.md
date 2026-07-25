# 实现计划：Web — PRD-01 底部导航与应用路由

> 创建日期：2026-07-25
> 对应技术方案：design-web.md
> 对应需求：spec.md

## 概述

本期 Web 端不实现移动端底部 Tab UI，而是在现有 Next.js App Router 骨架上补齐 `/search`、`/rankings`、`/mall` 三个一级路由，并通过共享 `placeholder-route` feature、首页两层入口、`/play/[id]` 与 `/detail/[id]` 的参数校验与 metadata 对齐，完成与移动端频道规划一致的路由承载层。测试以 Vitest + Testing Library 为主，并遵循“相同交互复用单一 case”。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 共享占位组件渲染正确 | `title`、`description`、`pathLabel` | 页面展示正确标题、说明与路由语义 | 组件测试 | P0 |
| T-02 | 首页补齐主入口且保留示例入口 | 渲染 `HomeScreen` | 同时存在 `/search`、`/rankings`、`/mall` 与 `/play/sample`、`/detail/sample` 链接 | 组件测试 | P0 |
| T-03 | 三个新增 page 可正确委托共享占位组件 | 渲染 `/search`、`/rankings`、`/mall` page | 页面可渲染且标题配置正确 | 页面 smoke | P0 |
| T-04 | 动态路由参数为空白时走兜底 | `/play/%20`、`/detail/%20` | 调用 `notFound()` 或等价 404 兜底，不把空白参数传给页面 | 页面测试 | P0 |
| T-05 | 动态页面 metadata 与路由语义一致 | `id = sample` | `/play/[id]` 生成“播放”语义标题，`/detail/[id]` 生成“详情”语义标题 | 页面测试 | P1 |
| T-06 | 同构占位页测试仅保留代表性 case | search / rankings / mall | 共享 UI 行为只测一次，其余通过表驱动 smoke 覆盖 | 测试策略约束 | P1 |

## 实现步骤

### Step 1：建立共享 placeholder-route Feature 与代表性测试

- **关联测试**：T-01、T-06
- **目标文件**：`web/src/features/placeholder-route/PlaceholderRouteScreen.tsx`、`web/src/features/placeholder-route/routePlaceholders.ts`、`web/src/features/placeholder-route/index.ts`、`web/src/features/placeholder-route/PlaceholderRouteScreen.test.tsx`
- **实现内容**：
  1. 新增 `PlaceholderRouteScreen`，统一渲染标题、说明文案和当前路径语义。
  2. 新增 `routePlaceholders.ts`，集中维护 `search`、`rankings`、`mall` 的 `href`、标题与说明。
  3. 新增 `index.ts` 统一导出，方便 page 层与首页复用。
  4. 先补一个代表性组件测试，验证共享占位组件渲染正确，不为三个同构页面分别写三套重复交互用例。
- **验证方式**：
  - 运行 `npm test -- PlaceholderRouteScreen`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/features/placeholder-route/PlaceholderRouteScreen.tsx` | 新增 | 统一承载搜索 / 排行榜 / 商城占位页 |
| `web/src/features/placeholder-route/routePlaceholders.ts` | 新增 | 集中维护占位页只读配置 |
| `web/src/features/placeholder-route/index.ts` | 新增 | 统一导出 placeholder-route feature |
| `web/src/features/placeholder-route/PlaceholderRouteScreen.test.tsx` | 新增 | 共享占位组件代表性渲染测试 |

### Step 2：注册 `/search`、`/rankings`、`/mall` 路由并补齐首页入口

- **关联测试**：T-02、T-03
- **目标文件**：`web/src/app/search/page.tsx`、`web/src/app/rankings/page.tsx`、`web/src/app/mall/page.tsx`、`web/src/features/home/HomeScreen.tsx`、`web/src/features/home/HomeScreen.test.tsx`
- **实现内容**：
  1. 在 `src/app/` 下新增 `search`、`rankings`、`mall` 三个 page，分别从共享配置中取标题与说明，并导出 route-level metadata。
  2. 修改 `HomeScreen`，把首页导航拆成两层：主入口 `/search`、`/rankings`、`/mall`；示例入口 `/play/sample`、`/detail/sample`。
  3. 扩展 `HomeScreen.test.tsx`，校验新增主入口与原有示例入口同时存在。
  4. 对新增 page 做表驱动 smoke 测试，验证它们都正确委托共享占位组件。
- **验证方式**：
  - 运行 `npm test -- HomeScreen`
  - 运行 `npm test`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/app/search/page.tsx` | 新增 | 注册 `/search` 路由并委托占位组件 |
| `web/src/app/rankings/page.tsx` | 新增 | 注册 `/rankings` 路由并委托占位组件 |
| `web/src/app/mall/page.tsx` | 新增 | 注册 `/mall` 路由并委托占位组件 |
| `web/src/features/home/HomeScreen.tsx` | 修改 | 首页补齐三条主入口并保留两条示例入口 |
| `web/src/features/home/HomeScreen.test.tsx` | 修改 | 断言首页入口完整性 |

### Step 3：对齐 `/play/[id]`、`/detail/[id]` 的参数校验与 metadata

- **关联测试**：T-04、T-05
- **目标文件**：`web/src/app/play/[id]/page.tsx`、`web/src/app/detail/[id]/page.tsx`、相关页面测试文件（如新增）
- **实现内容**：
  1. 在两个动态 page 中对 `params.id` 做最小化参数校验：`trim()` 后为空时走 `notFound()`。
  2. 将 metadata 从硬编码应用标题调整为与当前路由语义对齐，确保“播放 / 详情”标题一致。
  3. 保持 Page 层只负责参数读取、metadata 和 Feature 委托，不把业务 UI 下沉到 page 文件中。
  4. 用页面级测试覆盖合法参数与空白参数场景，并校验 metadata 语义。
- **验证方式**：
  - 运行 `npm test`
  - 运行 `npm run build`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/app/play/[id]/page.tsx` | 修改 | 增加空白参数校验并对齐播放页 metadata |
| `web/src/app/detail/[id]/page.tsx` | 修改 | 增加空白参数校验并对齐详情页 metadata |
| `web/src/app/play/[id]/page.test.tsx` | 新增 | 覆盖播放页参数校验与 metadata |
| `web/src/app/detail/[id]/page.test.tsx` | 新增 | 覆盖详情页参数校验与 metadata |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3
```

## 验证总览

- [ ] 所有测试通过（`npm test`）
- [ ] Build 成功（`npm run build`）
- [ ] 无新增 lint 错误（`npm run lint`）
- [ ] 同构占位页测试遵循“相同交互复用单一 case”约束

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/app/search/page.tsx` | 新增 | `/search` 占位路由 |
| `web/src/app/rankings/page.tsx` | 新增 | `/rankings` 占位路由 |
| `web/src/app/mall/page.tsx` | 新增 | `/mall` 占位路由 |
| `web/src/features/placeholder-route/PlaceholderRouteScreen.tsx` | 新增 | 共享占位页组件 |
| `web/src/features/placeholder-route/routePlaceholders.ts` | 新增 | 占位页配置源 |
| `web/src/features/placeholder-route/index.ts` | 新增 | 统一导出 |
| `web/src/features/placeholder-route/PlaceholderRouteScreen.test.tsx` | 新增 | 代表性组件测试 |
| `web/src/features/home/HomeScreen.tsx` | 修改 | 首页补齐主入口与示例入口 |
| `web/src/features/home/HomeScreen.test.tsx` | 修改 | 首页入口断言扩展 |
| `web/src/app/play/[id]/page.tsx` | 修改 | 参数校验与播放页 metadata 对齐 |
| `web/src/app/detail/[id]/page.tsx` | 修改 | 参数校验与详情页 metadata 对齐 |
| `web/src/app/play/[id]/page.test.tsx` | 新增 | 播放页 page 层测试 |
| `web/src/app/detail/[id]/page.test.tsx` | 新增 | 详情页 page 层测试 |