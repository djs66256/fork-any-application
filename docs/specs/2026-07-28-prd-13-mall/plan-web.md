# 实现计划：Web — PRD-13 商城

> 创建日期：2026-07-28
> 对应技术方案：design-web.md
> 对应需求：spec.md

## 概述

Web 端本期负责把 `/mall` 从占位路由升级为可被 Native 容器加载的商城 H5 页面，并补齐商品详情占位页、首页状态机、bridge 分流与登录拦截闭环。实现计划遵循轻量 TDD，先覆盖业务逻辑、状态转换、数据校验，再按现有 `web/` 五层结构分步落地页面、Feature、Core 与测试。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 商城 schema 与配置校验通过 | 合法的 `MallProduct` / `MallProductsResponse` / banner / shortcut / bridge payload | Zod 解析成功；非法 `productId`、非法 URL、非法返回结构被拒绝 | 单元测试 | P0 |
| T-02 | 商城首页首屏成功渲染 | `/api/mall/products?page=1&pageSize=20` 返回第一页数据；浏览器模式初始为匿名 | `/mall` 展示搜索、购物车、5 个快捷入口、banner、双列商品列表 | 组件测试 | P0 |
| T-03 | 首屏空态与首屏错误态正确分流 | API 返回 `data=[]`，或返回 `{ error: { code, message } }` / 网络异常 | 空列表时保留顶部区块并展示空态；失败时展示错误态与重试入口 | 组件测试 | P0 |
| T-04 | 分页状态机处理追加成功、追加失败与乱序保护 | 第一页成功、第二页失败或旧请求晚于新请求返回 | 追加失败不清空已有列表；同页不重复请求；旧响应不覆盖新状态 | 单元测试 | P0 |
| T-05 | 搜索入口 bridge 分流正确 | Native bridge 可用或不可用两种环境 | bridge 可用时发送 `mall.openSearch`；不可用时降级到 `/search` | 单元测试 | P1 |
| T-06 | 匿名点击商品卡触发登录拦截 | `isLoggedIn=false` 且点击商品卡 | 显示商城页内登录拦截层，不直接跳转详情 | 组件测试 | P0 |
| T-07 | 继续登录与取消登录的状态转换正确 | 拦截层已显示；bridge 成功、bridge 失败、用户取消 | 成功时发送 `mall.requestLogin(MallLoginContext)`；失败时停留当前页并提示；取消时关闭弹层且列表状态不变 | 单元测试 | P0 |
| T-08 | 已登录点击商品卡进入详情占位页 | `isLoggedIn=true` 且商品 `id` 合法 | 跳转 `/mall/product/[id]` 并展示详情占位页；非法参数时受控兜底 | 组件测试 | P1 |
| T-09 | 宿主消息同步登录态与恢复商城上下文 | Native 发送 `mall.syncAuthState` / `mall.restoreContext` | `isLoggedIn`、拦截层、恢复标记与首页状态按消息更新，首版至少可恢复商城首页首屏 | 单元测试 | P0 |
| T-10 | API client 兼容后端错误结构 | 非 2xx 响应体为 `{ error: { code, message } }` | `api-client.ts` 正确提取 message，供 mall API 与现有调用方复用 | 单元测试 | P0 |

## 实现步骤

### Step 1：补齐商城 Core 契约与配置入口

- **关联测试**：T-01、T-10
- **目标文件**：`web/src/lib/schemas.ts`、`web/src/lib/config.ts`、`web/src/lib/api-client.ts`、`web/src/lib/schemas.test.ts`、`web/src/lib/config.test.ts`、`web/src/lib/api-client.test.ts`
- **实现内容**：
  1. 在 `web/src/lib/schemas.ts` 新增商城领域 schema 与类型：`MallProductSchema`、`MallProductsResponseSchema`、`MallBannerSchema`、`MallShortcutSchema`、`MallLoginContextSchema`、`MallSearchContextSchema`、`MallHostAuthStateSchema`、`MallHostMessageSchema`。
  2. 在 `web/src/lib/config.ts` 增加 mall 相关受控配置读取与浏览器/宿主模式辅助配置，避免把 banner、bridge 开关或承接常量散落在组件内。
  3. 修正 `web/src/lib/api-client.ts` 的错误解析逻辑，使其兼容后端现有 `{ error: { code, message } }` 结构，同时保持现有调用方式不变。
  4. 在 `web/src/lib/schemas.test.ts`、`web/src/lib/config.test.ts`、`web/src/lib/api-client.test.ts` 先补齐合法/非法输入、错误路径和配置读取测试，覆盖商城数据校验与 error parsing。
- **验证方式**：
  - 运行 `cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm test -- --runInBand web/src/lib/schemas.test.ts web/src/lib/config.test.ts web/src/lib/api-client.test.ts` 确认 T-01、T-10 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/lib/schemas.ts` | 修改 | 新增商城实体、bridge 消息与宿主同步 schema |
| `web/src/lib/config.ts` | 修改 | 新增 mall 受控配置读取 |
| `web/src/lib/api-client.ts` | 修改 | 兼容后端错误结构解析 |
| `web/src/lib/schemas.test.ts` | 修改 | 覆盖商城 schema 合法/非法输入 |
| `web/src/lib/config.test.ts` | 修改 | 覆盖 mall 配置读取与默认值 |
| `web/src/lib/api-client.test.ts` | 修改 | 覆盖 `{ error: { code, message } }` 错误路径 |

### Step 2：实现商城 API 封装、集中配置与 bridge 适配层

- **关联测试**：T-01、T-05、T-07、T-09
- **目标文件**：`web/src/lib/mall/api.ts`、`web/src/features/mall/config/mall-seed.ts`、`web/src/features/mall/bridge/mall-bridge.ts`、`web/src/features/mall/bridge/mall-host-sync.ts`、`web/src/features/mall/bridge/mall-bridge.test.ts`、`web/src/features/mall/bridge/mall-host-sync.test.ts`
- **实现内容**：
  1. 新增 `web/src/lib/mall/api.ts`，统一封装 `GET /api/mall/products` 调用与响应校验，供 Feature hook 调用。
  2. 新增 `web/src/features/mall/config/mall-seed.ts`，集中维护 banner 与 5 个快捷入口配置，确保图片 URL、文案与行为不在页面组件中硬编码散落。
  3. 新增 `web/src/features/mall/bridge/mall-bridge.ts`，封装 `mall.openSearch`、`mall.requestLogin`、浏览器模式 fallback `/search` 与错误兜底。
  4. 新增 `web/src/features/mall/bridge/mall-host-sync.ts`，统一订阅并解析 `mall.syncAuthState`、`mall.restoreContext` 宿主消息，输出 Web 可消费的事件。
  5. 为 bridge 与 host sync 补充单元测试，覆盖 bridge 存在/不存在、调用抛错、登录上下文组装、宿主消息校验与异常 payload 过滤。
- **验证方式**：
  - 运行 `cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm test -- --runInBand web/src/features/mall/bridge/mall-bridge.test.ts web/src/features/mall/bridge/mall-host-sync.test.ts web/src/lib/schemas.test.ts` 确认 T-05、T-07、T-09 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/lib/mall/api.ts` | 新增 | 商城商品列表 API 封装 |
| `web/src/features/mall/config/mall-seed.ts` | 新增 | banner 与 shortcut 集中配置 |
| `web/src/features/mall/bridge/mall-bridge.ts` | 新增 | 搜索与登录 bridge 适配层 |
| `web/src/features/mall/bridge/mall-host-sync.ts` | 新增 | 宿主消息订阅与解析 |
| `web/src/features/mall/bridge/mall-bridge.test.ts` | 新增 | 覆盖 bridge 分流、fallback 与失败兜底 |
| `web/src/features/mall/bridge/mall-host-sync.test.ts` | 新增 | 覆盖 auth/context 消息解析与异常过滤 |

### Step 3：实现商城首页状态机与业务逻辑 Hook

- **关联测试**：T-03、T-04、T-06、T-07、T-09
- **目标文件**：`web/src/features/mall/hooks/useMallPage.ts`、`web/src/features/mall/hooks/useMallPage.test.ts`
- **实现内容**：
  1. 新增 `useMallPage`，统一管理 `items`、`page`、`hasNextPage`、`isLoading`、`isAppending`、`errorMessage`、`appendError`、`loginInterceptVisible`、`activeProduct`、`isLoggedIn`、`pendingRestoreReason` 等页面状态。
  2. 在 hook 内先实现首屏加载、空态、错误态、重试、分页追加、同页请求去重、乱序响应保护与追加失败保留已有列表。
  3. 接入 bridge 与 host sync：匿名点击商品显示登录拦截，继续登录触发 `mall.requestLogin`，宿主消息更新登录态并关闭/恢复拦截层。
  4. 为 hook 增加单元测试，重点覆盖状态转换、追加失败不清空列表、宿主消息驱动的登录态切换、取消登录拦截、container recreated 恢复语义。
- **验证方式**：
  - 运行 `cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm test -- --runInBand web/src/features/mall/hooks/useMallPage.test.ts` 确认 T-03、T-04、T-06、T-07、T-09 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/features/mall/hooks/useMallPage.ts` | 新增 | 商城首页状态机、分页与登录拦截逻辑 |
| `web/src/features/mall/hooks/useMallPage.test.ts` | 新增 | 覆盖业务逻辑、状态转换、数据校验相关测试 |

### Step 4：落地商城首页 Feature 组件与 `/mall` 路由承接

- **关联测试**：T-02、T-03、T-05、T-06、T-07
- **目标文件**：`web/src/app/mall/page.tsx`、`web/src/features/mall/MallPageScreen.tsx`、`web/src/features/mall/components/MallHeader.tsx`、`web/src/features/mall/components/MallShortcutGrid.tsx`、`web/src/features/mall/components/MallBannerCarousel.tsx`、`web/src/features/mall/components/MallProductGrid.tsx`、`web/src/features/mall/components/MallProductCard.tsx`、`web/src/features/mall/components/MallLoginInterceptOverlay.tsx`、`web/src/features/mall/index.ts`、`web/src/features/mall/MallPageScreen.test.tsx`
- **实现内容**：
  1. 把 `web/src/app/mall/page.tsx` 从 `PlaceholderRouteScreen` 改为仅委托 `MallPageScreen`，保持 Page 层只做路由承接。
  2. 新增商城首页 Feature 组件与子组件，渲染搜索入口、购物车入口、快捷入口、banner、双列商品卡、空态、错误态、尾部 loading 与尾部错误提示。
  3. 在组件层接入 `useMallPage`，确保匿名点击先弹 H5 登录拦截层，bridge 失败只做当前页提示，不离开商城上下文。
  4. 使用现有 `components/ui/`、`styles/` token 与 CSS Modules 组织样式，不把业务逻辑写入 Shared UI。
  5. 补充 `MallPageScreen.test.tsx`，覆盖首页成功渲染、空态、首屏错误态、匿名点击商品、浏览器模式点搜索 fallback 等关键交互。
- **验证方式**：
  - 运行 `cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm test -- --runInBand web/src/features/mall/MallPageScreen.test.tsx` 确认 T-02、T-03、T-05、T-06、T-07 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/app/mall/page.tsx` | 修改 | `/mall` 路由委托到商城 Feature |
| `web/src/features/mall/MallPageScreen.tsx` | 新增 | 商城首页页面级 Feature |
| `web/src/features/mall/components/MallHeader.tsx` | 新增 | 搜索与购物车入口 |
| `web/src/features/mall/components/MallShortcutGrid.tsx` | 新增 | 5 个快捷入口区域 |
| `web/src/features/mall/components/MallBannerCarousel.tsx` | 新增 | 活动横幅区域 |
| `web/src/features/mall/components/MallProductGrid.tsx` | 新增 | 商品列表区与尾部状态 |
| `web/src/features/mall/components/MallProductCard.tsx` | 新增 | 单个商品卡 |
| `web/src/features/mall/components/MallLoginInterceptOverlay.tsx` | 新增 | 页内登录拦截层 |
| `web/src/features/mall/index.ts` | 新增 | Feature 导出入口 |
| `web/src/features/mall/MallPageScreen.test.tsx` | 新增 | 商城首页核心交互测试 |

### Step 5：落地商品详情占位页与商城路由收口验证

- **关联测试**：T-08、T-09
- **目标文件**：`web/src/app/mall/product/[id]/page.tsx`、`web/src/features/mall/MallProductPlaceholderScreen.tsx`、`web/src/features/mall/MallProductPlaceholderScreen.test.tsx`
- **实现内容**：
  1. 新增 `web/src/app/mall/product/[id]/page.tsx`，只做参数读取与 Feature 委托，不在 Page 层写业务逻辑。
  2. 新增 `MallProductPlaceholderScreen`，展示商品占位承接信息、返回商城动作与非法 `id` 的受控兜底。
  3. 补充详情占位页测试，覆盖已登录跳转成功、直接访问非法参数时的回退/错误显示、返回商城语义。
  4. 收口验证 mall Feature 的导出、路由路径与已有 `/search` 占位承接兼容，不引入新的全局状态或依赖。
- **验证方式**：
  - 运行 `cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm test -- --runInBand web/src/features/mall/MallProductPlaceholderScreen.test.tsx web/src/features/mall/MallPageScreen.test.tsx` 确认 T-08、T-09 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/app/mall/product/[id]/page.tsx` | 新增 | 商品详情占位页路由入口 |
| `web/src/features/mall/MallProductPlaceholderScreen.tsx` | 新增 | 商品详情占位页 Feature |
| `web/src/features/mall/MallProductPlaceholderScreen.test.tsx` | 新增 | 覆盖详情占位页与参数兜底测试 |

### Step 6：执行 Web 端总体验证并补齐回归测试

- **关联测试**：T-01 ~ T-10
- **目标文件**：`web/src/lib/*.test.ts`、`web/src/features/mall/**/*.test.tsx`、`web/src/features/mall/**/*.test.ts`
- **实现内容**：
  1. 回看前 5 步测试缺口，补齐业务逻辑、状态转换、数据校验相关遗漏测试，尤其是分页乱序、bridge 失败、宿主恢复消息与非法商品上下文。
  2. 运行 Web 端全量测试、lint、build，确认新 mall Feature 不破坏现有 home、search、detail、player 占位路由。
  3. 如发现测试或构建中的路径、导出、样式引用问题，在对应已有文件内收口修复，但不扩展需求范围。
- **验证方式**：
  - 运行 `cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm test` 确认 T-01 ~ T-10 全部通过
  - 运行 `cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm run lint` 确认无新增 lint 错误
  - 运行 `cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm run build` 确认构建成功
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/lib/*.test.ts` | 修改 | 补齐商城相关 Core 回归测试 |
| `web/src/features/mall/**/*.test.ts` | 修改/新增 | 补齐商城业务逻辑与 bridge 测试 |
| `web/src/features/mall/**/*.test.tsx` | 修改/新增 | 补齐商城页面与交互回归测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2 ──▶ Step 3 ──▶ Step 4 ──▶ Step 5 ──▶ Step 6
```

## 验证总览

- [x] 所有测试通过（`cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm test`）
- [x] Build 成功（`cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm run build`）
- [x] 无新增 lint 错误（`cd /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-28-prd-13-mall/web && npm run lint`）

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `web/src/lib/schemas.ts` | 修改 | 新增商城 schema 与类型 |
| `web/src/lib/config.ts` | 修改 | 新增 mall 配置读取 |
| `web/src/lib/api-client.ts` | 修改 | 兼容后端错误结构 |
| `web/src/lib/mall/api.ts` | 新增 | 商城 API 封装 |
| `web/src/app/mall/page.tsx` | 修改 | 商城首页路由委托 |
| `web/src/app/mall/product/[id]/page.tsx` | 新增 | 商品详情占位页路由 |
| `web/src/features/mall/MallPageScreen.tsx` | 新增 | 商城首页 Feature |
| `web/src/features/mall/MallProductPlaceholderScreen.tsx` | 新增 | 商品详情占位页 Feature |
| `web/src/features/mall/hooks/useMallPage.ts` | 新增 | 商城状态机与业务逻辑 |
| `web/src/features/mall/config/mall-seed.ts` | 新增 | banner/shortcut 集中配置 |
| `web/src/features/mall/bridge/mall-bridge.ts` | 新增 | 搜索与登录 bridge 适配 |
| `web/src/features/mall/bridge/mall-host-sync.ts` | 新增 | 宿主消息同步 |
| `web/src/features/mall/components/*` | 新增 | 商城首页组件拆分 |
| `web/src/lib/schemas.test.ts` | 修改 | 商城 schema 测试 |
| `web/src/lib/config.test.ts` | 修改 | mall 配置测试 |
| `web/src/lib/api-client.test.ts` | 修改 | error parsing 测试 |
| `web/src/features/mall/**/*.test.ts` | 新增/修改 | bridge、hook、状态逻辑测试 |
| `web/src/features/mall/**/*.test.tsx` | 新增/修改 | 页面与交互测试 |
