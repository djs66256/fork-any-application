# 技术方案（共享部分）：PRD-13 商城

> 创建日期：2026-07-28
> 对应需求：spec.md

## 整体架构

```mermaid
flowchart LR
    MallTab[iOS / Android Mall Tab] --> MallContainer[商城 Native 容器\nWKWebView / WebView]
    MallContainer --> MallH5[Web Mall H5\n/mall + /mall/product/[id]]

    MallH5 --> ProductAPI[GET /api/mall/products]
    ProductAPI --> MallRoute[Route Handler]
    MallRoute --> DramaService[DramaService / MallService 扩展]
    DramaService --> DramaRepo[DramaRepositoryInterface]
    DramaRepo --> MockRepo[DramaMockRepository / Mall Mock Source]
    MockRepo --> MallSeed[商品 seed 数据]

    MallH5 --> BannerConfig[Web 集中配置模块\nMallBanner[] / MallShortcut[]]

    MallH5 -. bridge: openSearch .-> NativeSearch[复用现有 Native Search Route]
    MallH5 -. bridge: requestLogin .-> NativeLogin[统一 Native 全屏登录承接页]
    NativeLogin -. returnTarget=/mall .-> MallContainer

    Browser[浏览器 / 本地开发] --> MallH5
    Browser -. fallback .-> WebSearch[/search 占位路由]
```

### 架构说明

- 本期商城遵循 `PRODUCT.md` 已收敛的承载策略：**商城首页与商品详情占位页由 Web H5 提供，Android / iOS 只负责 Native 容器、bridge、返回语义和 tab 高亮保持**。
- Backend 延续现有四层架构与 repository-registry 模式：**Route → Service → Repository → Mock Data / Infrastructure**；首版仅新增只读接口 `GET /api/mall/products`，不引入交易写接口。
- 商城首屏固定区块拆分为两类来源：
  - **商品 Feed**：由 Backend `GET /api/mall/products` 提供分页数据。
  - **活动横幅 + 快捷入口**：由 Web H5 集中配置模块单点管理，避免在 Native 客户端或页面组件内散落硬编码图片 URL / 文案。
- 匿名点击商品卡的分流固定为：**H5 页内登录拦截覆盖层 → bridge 触发 Native 全屏登录承接页 → 返回商城上下文**；不直接复用 home 菜单链路下的 `menu/login` 语义。
- 搜索入口复用既有搜索能力，但按承载环境分流：
  - **Native 容器模式**：H5 通过 bridge 请求宿主打开现有 Native 搜索页。
  - **浏览器 / 本地开发模式**：H5 直接跳转 Web `/search` 占位路由。
- 首版不新增真实数据库表或 migration。商品列表先基于 mock / seed 数据完成稳定 contract；未来若接入 Supabase 真数据源，客户端与 H5 contract 保持不变。

## API 设计

### 涉及变更

| 类型 | 数量 | 说明 |
|------|------|------|
| 新增接口 | 1 | 新增商城商品分页接口 `GET /api/mall/products` |
| 修改接口 | 0 | 不修改现有 `/api/dramas`、`/api/dramas/search`、`/api/dramas/rankings` 等契约 |
| 废弃接口 | 0 | 无 |

> 兼容性说明：当前 Backend 已形成两种稳定响应约定：**成功响应按资源体直出**（如 `{ data, pagination }` / `{ data }`），**失败响应统一由 `withErrorHandler` 输出** `{ error: { code, message } }`，Zod 参数校验失败时可附带 `details`。商城接口继续沿用这一现有 contract，不额外引入新的成功包裹层。

### 新增接口

#### `GET /api/mall/products`

- **功能简介**：返回商城首页双列商品 Feed 的分页数据，供 H5 首屏加载与滚动追加使用。
- **Path Parameters**：无
- **认证要求**：公开只读接口，不要求登录。

- **Query Parameters**：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | `1` | 页码，`int >= 1` |
| `pageSize` | number | 否 | `20` | 每页数量，`1 ~ 100`，首版默认固定使用 `20` |

- **Request Body**：无

- **Response**：

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440101",
      "title": "轻奢真丝睡衣礼盒",
      "image_url": "https://example.com/mall/products/pajama-gift-box.jpg",
      "price": 199.0,
      "tags": ["热卖", "包邮"]
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 42,
    "total_pages": 3
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | `MallProduct[]` | 当前页商品数据 |
| `data[].id` | `string(uuid)` | 商品唯一标识；同时用于 H5 商品详情占位路由与 `MallLoginContext.productId` |
| `data[].title` | `string` | 商品标题 |
| `data[].image_url` | `string(url)` | 商品主图 URL；失效时由前端兜底占位图 |
| `data[].price` | `number` | 服务端权威原始价格值；前端负责格式化展示 |
| `data[].tags` | `string[]` | 商品标签；前端可按布局截断展示 |
| `pagination` | `object` | 现有统一分页结构 |
| `pagination.page` | `number` | 当前页码 |
| `pagination.page_size` | `number` | 当前页大小 |
| `pagination.total` | `number` | 总条数 |
| `pagination.total_pages` | `number` | 总页数 |

- **Error Codes**：

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功；允许空列表 |
| 400 | `VALIDATION_ERROR` | `page` / `pageSize` 的 query schema 校验失败 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |
| 503 | `SERVICE_UNAVAILABLE` | 数据源不可用（预留给未来真实仓储实现） |

### Zod Schema 定义

```typescript
import { z } from 'zod';

export const MallProductSchema = z.object({
  id: z.string().uuid(),
  title: z.string().trim().min(1).max(200),
  image_url: z.string().url(),
  price: z.number().nonnegative(),
  tags: z.array(z.string().trim().min(1).max(20)).max(3).default([]),
});

export type MallProduct = z.infer<typeof MallProductSchema>;

export const MallProductsQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(20),
});

export type MallProductsQuery = z.infer<typeof MallProductsQuerySchema>;

export const MallProductsResponseSchema = z.object({
  data: z.array(MallProductSchema),
  pagination: PaginationSchema,
});

export type MallProductsResponse = z.infer<typeof MallProductsResponseSchema>;

export const MallBannerSchema = z.object({
  id: z.string().min(1),
  image_url: z.string().url(),
  target_type: z.enum(['none', 'product', 'search', 'web']),
  target_value: z.string().default(''),
  sort_order: z.number().int().min(0),
});

export type MallBanner = z.infer<typeof MallBannerSchema>;

export const MallShortcutSchema = z.object({
  key: z.enum(['orders', 'coupon', 'wallet', 'same-style', 'subsidy']),
  title: z.string().min(1),
  icon: z.string().min(1),
  behavior: z.enum(['placeholder-feedback']),
});

export type MallShortcut = z.infer<typeof MallShortcutSchema>;

export const MallSearchContextSchema = z.object({
  source: z.literal('mall'),
  returnTarget: z.literal('/mall'),
});

export type MallSearchContext = z.infer<typeof MallSearchContextSchema>;

export const MallLoginContextSchema = z.object({
  source: z.literal('mall'),
  productId: z.string().uuid(),
  returnTarget: z.literal('/mall'),
});

export type MallLoginContext = z.infer<typeof MallLoginContextSchema>;

export const MallBridgeMessageSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('mall.openSearch'),
    payload: MallSearchContextSchema,
  }),
  z.object({
    type: z.literal('mall.requestLogin'),
    payload: MallLoginContextSchema,
  }),
]);

export type MallBridgeMessage = z.infer<typeof MallBridgeMessageSchema>;

export const MallHostAuthStateSchema = z.object({
  source: z.literal('mall'),
  isLoggedIn: z.boolean(),
  reason: z.enum(['initial-load', 'login-success', 'login-cancel', 'app-resume']),
  returnTarget: z.literal('/mall'),
});

export type MallHostAuthState = z.infer<typeof MallHostAuthStateSchema>;

export const MallHostMessageSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('mall.syncAuthState'),
    payload: MallHostAuthStateSchema,
  }),
  z.object({
    type: z.literal('mall.restoreContext'),
    payload: z.object({
      source: z.literal('mall'),
      reason: z.enum(['search-return', 'login-return', 'container-recreated']),
      returnTarget: z.literal('/mall'),
      preserveScroll: z.boolean().default(false),
    }),
  }),
]);

export type MallHostMessage = z.infer<typeof MallHostMessageSchema>;
```

### 契约补充

- 首版商品列表按**固定确定性顺序**返回；不支持客户端传排序、筛选或搜索条件。
- `page=1&pageSize=20` 是商城首页唯一默认首屏参数；H5 与 Native 测试用例也以这一默认值作为基线。
- `price` 只返回原始数值，不返回混合货币符号的展示字符串，避免前后端重复维护展示文案。
- 空列表返回 `200 + data=[] + 合法 pagination`，由 H5 展示空态而不是错误态。
- Route 层直接使用 `MallProductsQuerySchema.parse(...)` 校验 query；非法参数统一进入 `withErrorHandler` 的 `ZodError` 分支，返回 `VALIDATION_ERROR + details`。
- `MallLoginContext` 是 H5 → Native 登录承接的最小跨端 contract，不携带 token、昵称或其他敏感字段；Native 不从 H5 接收任何可直接信任的登录态结果。
- `MallHostAuthState` 是 Native → H5 的登录态同步 contract。H5 首次加载完成后必须从宿主收到一次 `mall.syncAuthState`，登录成功 / 取消 / App 恢复前台后宿主也要再次同步该消息；若宿主无法增量回传，则至少重载 `/mall` 并在页面初始化阶段注入等价的登录态结果。
- `MallSearchContext` 与 `MallHostMessage.restoreContext` 共同定义“从 Native 搜索临时离开商城再返回商城”的闭环：Native 打开搜索时记录 `returnTarget=/mall`，搜索关闭或回退后必须重新选中 mall tab，并向 H5 发送 `mall.restoreContext(reason='search-return')`；若容器已被系统回收，允许降级为重载商城首页首屏。
- `MallHostMessage.restoreContext` 也适用于登录承接返回与容器重建场景：
  - `reason='login-return'`：登录页关闭、取消、失败或成功后都要回到 mall 承接；
  - `reason='container-recreated'`：容器被系统回收后，宿主需重载 `/mall` 并同步最新登录态；
  - `preserveScroll=true` 仅作为增强项，首版允许为 `false`，但必须保证 tab 高亮与商城首页可恢复。

## 数据模型

### 新增/变更数据表

| 表名 | 操作 | 说明 |
|------|------|------|
| `mall products seed`（逻辑模型） | 新建 | 商城商品列表的固定种子顺序与 mock 数据；首版允许存在于 Repository mock 层 |
| `mall banner config`（逻辑模型） | 新建 | Web H5 集中配置模块中的 banner seed/config，非数据库表 |
| `dramas` / 其他现有表 | 不变 | 商城首版不复用短剧主数据表，不新增真实 Supabase migration |

> 本期不要求新增真实数据库表或 Supabase migration。Backend 首版仍可通过 `DramaMockRepository` 扩展商城 seed 数据，后续再按相同 contract 迁移到真实仓储实现。

### 共享实体设计

| 实体 | 字段 | 说明 |
|------|------|------|
| `MallProduct` | `id / title / image_url / price / tags` | 商城商品卡数据实体 |
| `MallProductsQuery` | `page / pageSize` | 商品列表请求参数 |
| `MallProductsResponse` | `data[] / pagination` | 商品列表分页响应 |
| `MallBanner` | `id / image_url / target_type / target_value / sort_order` | 活动横幅配置实体 |
| `MallShortcut` | `key / title / icon / behavior` | 5 个快捷入口的固定配置 |
| `MallPageState` | `items / page / hasNextPage / isLoading / isAppending / appendError / loginInterceptVisible` | H5 商城首页状态机 |
| `MallContainerState` | `loading / success / error / lastLoadedUrl` | Native 容器承载状态 |
| `MallSearchContext` | `source=mall / returnTarget=/mall` | 商城搜索 bridge 上下文 |
| `MallLoginContext` | `source=mall / productId / returnTarget=/mall` | 商城登录承接上下文 |
| `MallBridgeMessage` | `mall.openSearch` / `mall.requestLogin` | H5 → Native 的桥接消息 |
| `MallHostAuthState` | `source=mall / isLoggedIn / reason / returnTarget` | Native → H5 的登录态同步消息 |
| `MallHostMessage` | `mall.syncAuthState` / `mall.restoreContext` | Native → H5 的宿主同步消息 |

### 字段语义

| 字段 | 类型 | 语义 | 备注 |
|------|------|------|------|
| `MallProduct.id` | `uuid string` | 商品唯一标识 | 同时作为详情占位页路由参数 |
| `MallProduct.image_url` | `string(url)` | 商品主图 | 失效时前端展示占位图，不视为接口失败 |
| `MallProduct.price` | `number` | 原始价格值 | H5 本地格式化为 `¥199` 等文案 |
| `MallBanner.target_type` | `none / product / search / web` | banner 点击行为类型 | 首版允许 `none` |
| `MallBanner.target_value` | `string` | 点击行为补充参数 | 当 `target_type=product` 时对应 `productId` |
| `MallShortcut.behavior` | `placeholder-feedback` | 快捷入口行为类型 | 首版只允许页内反馈，不跳真实页面 |
| `MallSearchContext.returnTarget` | `'/mall'` | 搜索返回目标 | Native 搜索关闭后必须回到商城承接 |
| `MallLoginContext.returnTarget` | `'/mall'` | 登录取消/关闭/成功后的返回目标 | Native 必须尊重该值恢复商城承接 |
| `MallHostAuthState.isLoggedIn` | `boolean` | Native 当前权威登录态 | H5 不自行猜测登录结果 |
| `MallHostAuthState.reason` | `initial-load / login-success / login-cancel / app-resume` | 登录态同步原因 | 用于 H5 区分首次同步与返回同步 |
| `MallHostMessage.restoreContext.reason` | `search-return / login-return / container-recreated` | 宿主要求 H5 执行上下文恢复的原因 | 首版允许只恢复到商城首页首屏 |
| `MallContainerState.lastLoadedUrl` | `string` | 容器最近一次成功加载的 H5 URL | 用于重试与容器重建恢复 |

### 数据来源策略

| 场景 | 数据来源 | 说明 |
|------|---------|------|
| 商城首页商品 Feed | Backend `GET /api/mall/products` | 首版由 mock repository 提供 20+ 条商品，支持分页切片 |
| 活动横幅 | Web H5 集中配置模块 | 不从 Native 传入，不在页面组件内散落硬编码 |
| 快捷入口 | Web H5 固定配置 | 固定 5 项，顺序不支持动态增删 |
| 登录态判断 | Native AuthSession / App Auth State + H5 初始化注入（如需） | H5 不自行持有权威登录 token |
| 搜索承接 | Native 现有搜索页 / Web `/search` 占位路由 | 由承载环境决定 |

## 跨端共享逻辑

| 共享逻辑 | 说明 | 涉及端 |
|---------|------|--------|
| 商城承载策略 | 首页与详情占位页都由 H5 承载，Native 只负责容器与桥接 | Web / iOS / Android |
| 默认首屏请求 | 首次进入固定请求 `GET /api/mall/products?page=1&pageSize=20` | Backend / Web / iOS / Android |
| 容器三态 | Native 容器统一维护 `loading / success / error` 三态，不回退 placeholder | iOS / Android |
| 搜索入口分流 | Native 容器模式走 `mall.openSearch` bridge；浏览器 / 本地开发模式走 `/search` | Web / iOS / Android |
| 搜索返回契约 | Native 打开搜索前记录 `MallSearchContext(returnTarget='/mall')`；搜索关闭或回退后必须恢复 mall 承接，并向 H5 发送 `mall.restoreContext(reason='search-return')`，若容器已重建则降级重载 `/mall` | Web / iOS / Android |
| 登录拦截顺序 | 匿名点击商品卡必须先出现 H5 页内登录拦截覆盖层，再决定是否调用 Native 登录承接 | Web / iOS / Android |
| 登录态同步契约 | Native 首次加载完成、登录成功/取消、App 恢复前台后都要向 H5 同步 `mall.syncAuthState`；若无法增量同步，至少重载 `/mall` 并注入等价登录态结果 | Web / iOS / Android |
| 登录返回契约 | 登录取消 / 失败 / 关闭 / 成功后都回到 `returnTarget=/mall`，保持 mall tab 高亮，并向 H5 发送 `mall.restoreContext(reason='login-return')` | Web / iOS / Android |
| 详情承接策略 | 已登录商品点击进入 H5 商品详情占位页，不新增 Native 商品详情页面 | Web / iOS / Android |
| 分页状态机 | 首屏失败与追加失败分开建模；追加失败不清空已有列表 | Web / Backend |
| 请求防乱序 | 旧请求晚于新请求返回时，不得覆盖最新列表状态 | Web |
| 快捷入口行为 | 购物车与 5 个快捷入口首版都停留在商城上下文内给出“功能开发中”反馈 | Web |
| banner 配置单点 | banner 只允许来自 Web H5 集中配置模块，并按 `sort_order` 排序渲染 | Web |
| 最低恢复保证 | 若容器被系统回收，返回商城时至少恢复商城首页首屏，不要求完整滚动位置恢复 | Web / iOS / Android |

### 状态机约定

```text
首次进入商城 Tab
→ Native 容器 loading
→ H5 成功加载 / 容器 error(retryable)
→ H5 请求商品第一页(page=1,pageSize=20)
→ success(content) | empty | error(retryable)

滚动触底
→ appending(next page)
→ appendSuccess(append items) | appendError(keep current items)

匿名点击商品卡
→ showLoginInterceptOverlay
→ cancel => stay in mall context
→ continue login => bridge mall.requestLogin(MallLoginContext)
→ native full screen login
→ success / cancel / fail / close
→ return /mall and keep mall tab highlighted

已登录点击商品卡
→ push H5 detail placeholder route
→ back to mall list
```

### bridge 事件约定

#### H5 → Native

| 事件 | 触发方 | Payload | 宿主行为 | 失败兜底 |
|------|--------|---------|---------|---------|
| `mall.openSearch` | Web H5 | `MallSearchContext` | 打开现有 Native 搜索页，并记录 `returnTarget=/mall` 以供返回恢复 | H5 保留当前页并提示“暂时无法打开搜索” |
| `mall.requestLogin` | Web H5 | `MallLoginContext` | 打开统一 Native 全屏登录承接页，并记录登录返回目标 | 继续停留在登录拦截覆盖层，不跳空白页 |

#### Native → H5

| 事件 | 触发方 | Payload | H5 行为 | 宿主兜底 |
|------|--------|---------|---------|---------|
| `mall.syncAuthState` | Native 容器 | `MallHostAuthState` | 更新当前权威登录态，决定后续商品点击是继续拦截还是放行详情页 | 若 H5 页面已被销毁，宿主重载 `/mall` 并重新发送该消息 |
| `mall.restoreContext` | Native 容器 | `{ source:'mall', reason, returnTarget:'/mall', preserveScroll }` | 根据 `reason` 恢复商城上下文；首版至少恢复首页首屏 | 若无法直接回调 H5，宿主至少重新选中 mall tab 并重载 `/mall` |

### 分页一致性约定

| 场景 | 约束 | 原因 |
|------|------|------|
| 首屏加载失败 | 展示首屏错误态与重试按钮 | 让用户知道列表尚未成功初始化 |
| 追加失败 | 保留当前 `items`，仅记录 `appendError` | 避免把已加载商品清空 |
| 重复触底 | 同一页只允许一个请求在途 | 防止重复商品 |
| 旧请求乱序返回 | 仅最新 token / query 对应的结果可提交到状态 | 防止新状态被旧响应覆盖 |
| 超大页码 | 服务端返回空数组，前端停止追加 | 与现有分页 contract 保持一致 |

## 安全考虑

- **认证与授权**：
  - 商城首页商品浏览与 banner 浏览不要求登录。
  - 登录态只影响“点击商品卡后能否直接进入详情占位页”，不影响列表读取。
  - Native 登录承接页是唯一可信登录闭环，H5 不自行决定“登录成功”。

- **数据校验**：
  - Backend 使用 `MallProductsQuerySchema` 校验 query，并使用 `MallProductsResponseSchema` 校验仓储输出。
  - Web H5 对商品列表响应、banner config、bridge message 都使用 Zod schema 做运行时校验。
  - Native 宿主对 bridge payload 中的 `source / productId / returnTarget` 再做一次白名单校验，不直接信任 H5 传入的任意字符串。

- **敏感数据处理**：
  - `MallLoginContext` 仅保留最小必要字段，不传 token、手机号、昵称等敏感信息。
  - 商城 H5 地址、API base URL、Native bridge 开关都走各端已有 config 模块，不在页面代码或容器代码中硬编码环境地址。
  - banner 图片 URL 允许是公开 CDN / 远端资源，但不得在 Native 客户端或页面组件中散落常量值。

- **内容与输入安全**：
  - `image_url`、banner 跳转目标、商品 `id` 都需要经过 schema 校验后再消费。
  - H5 详情占位页只渲染受控数据和固定说明，不接收富文本 HTML 注入。
  - 购物车、钱包、券包、订单等未实现入口不透传到未知页面，统一停留在当前商城上下文内反馈。

## 边界与错误处理（⚠️ 重点，最易遗漏）

### 错误处理架构

- **全局错误处理策略**：
  - Backend 继续使用 `withErrorHandler` 捕获 `AppError` / `ZodError`，输出统一错误 JSON。
  - Web H5 对 API 请求错误、schema 校验失败、bridge 不可用、资源加载失败分别映射为首屏错误态、尾部错误态或轻提示。
  - Native 容器只负责 H5 页加载失败、bridge 调用失败、登录承接返回恢复失败这三类宿主级错误，不干预 H5 内部列表状态机。
- **错误响应格式**：
  - 成功：沿用现有资源体直出结构（`{ data, pagination }`）。
  - 失败：`{ error: { code, message } }`；Zod 校验失败时可额外附带 `details`。
- **错误日志与监控**：
  - 首版以自动化测试 + 本地联调日志为主。
  - 后续若接入真实仓储，再按 `page / pageSize / requestId` 增补服务端请求日志；H5 / Native 再补 bridge 失败埋点。

### API 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 用户提示文案 |
|-----------|------------|------|-------------|
| `VALIDATION_ERROR` | 400 | `page` / `pageSize` schema 校验失败 | 请求参数有误，请稍后重试 |
| `INVALID_PARAMS` | 400 | 预留给 service 层补充的业务参数错误 | 请求参数有误，请稍后重试 |
| `UNAUTHORIZED` | 401 | 预留；商品列表接口当前不要求登录 | 请先登录 |
| `FORBIDDEN` | 403 | 预留 | 当前不可访问 |
| `NOT_FOUND` | 404 | 预留；列表接口当前不使用 | 资源不存在 |
| `CONFLICT` | 409 | 预留；首版只读接口不使用 | 当前状态冲突 |
| `TOO_MANY_REQUESTS` | 429 | 预留；需要限流时使用 | 请求过于频繁，请稍后重试 |
| `INTERNAL_ERROR` | 500 | 服务内部错误 | 服务开小差了，请稍后重试 |
| `SERVICE_UNAVAILABLE` | 503 | 真实数据源或依赖服务不可用 | 服务暂不可用，请稍后重试 |
| `NETWORK_ERROR` | 客户端本地态 | 网络中断 / 超时 / DNS 失败 | 网络异常，请检查后重试 |

### 边界场景处理

| 场景 | 触发条件 | API / 客户端行为 | 说明 |
|------|---------|------------------|------|
| 非法页码 | `page < 1` / `pageSize < 1` / `pageSize > 100` | 返回 400 + `VALIDATION_ERROR` | Route 层 schema 直接拦截 |
| 商品列表为空 | `data=[]` | H5 展示空态；搜索/快捷入口/横幅区仍保留 | 不视为错误 |
| 首屏请求失败 | 断网 / 超时 / 5xx | H5 展示首屏错误态 + 重试 | 不回退 placeholder |
| 追加请求失败 | 滚动加载下一页超时 / 5xx | 保留现有商品，仅展示尾部错误 | 不清空列表 |
| 旧请求晚于新请求返回 | 连续重试 / 触底并发 | 旧结果丢弃 | 防止状态回滚 |
| 商品图片加载失败 | `image_url` 失效 | 用统一占位图 | 不中断点击 |
| banner 配置为空 | 配置模块未提供 banner | 横幅区隐藏或空占位 | 不影响其他区块 |
| banner 跳商品目标非法 | `target_type=product` 但 `target_value` 非法 | 忽略点击并提示 | 不进入错误路由 |
| bridge 未注册搜索能力 | Native 容器未暴露 `mall.openSearch` | H5 留在当前页并提示 | 浏览器模式继续走 `/search` |
| bridge 未注册登录能力 | Native 容器未暴露 `mall.requestLogin` | 保持登录拦截层可见并提示 | 不直接跳空白页 |
| 匿名用户快速重复点商品 | 短时间内多次点击 | 只展示一个登录拦截层 | 防止多层弹层 |
| 登录取消 / 失败 / 关闭 | Native 登录承接退出 | 回到 `/mall`，tab 高亮保持 | 最低恢复首页首屏 |
| 容器被系统回收 | 登录返回或切后台后重建 | 重新加载商城首页首屏 | 完整滚动位置恢复不是首版强要求 |
| 浏览器直接访问详情页 | `/mall/product/[id]` 刷新进入 | H5 自行校验 `id`，非法则回退商城首页或展示受控错误页 | 不新增 Native 依赖 |

## 性能考虑

- **预期 QPS**：首版主要面向 mock / 内测环境，商品列表接口读压力远低于首页 Feed 主链路。
- **分页大小**：固定默认 `pageSize=20`，兼顾移动端双列首屏密度与渲染成本。
- **仓储成本**：首版走 mock seed 切片，不新增 Redis 缓存；未来替换为真实仓储时优先保证 contract 稳定，而非提前设计复杂缓存。
- **请求去重**：H5 必须对分页请求做在途去重与乱序保护，避免重复 append。
- **容器复用**：Android / iOS 切换 tab 后尽量复用既有 WebView / WKWebView 实例，减少每次回到 mall 的冷启动成本。
- **资源策略**：banner 与商品图优先使用可缓存图片资源；H5 首屏先渲染骨架与静态区，再渐进加载 Feed 图片。
- **状态恢复**：首版优先保留已加载页和滚动位置；若实现代价过高，允许退化为恢复首页首屏，但不允许退回 placeholder。

## 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | 入口与路由、已知限制 | 确认 mall / earn 仍为占位频道，且 H5 承载策略已写入应用壳文档 |
| `wiki/features/search-discovery/index.md` | 入口与路由、多端实现 | 确认 Native 搜索页归属 home-owned 路由，Web `/search` 当前仍为占位页 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-28-prd-13-mall/spec.md` | 商城需求范围、登录拦截路径、搜索分流与 banner 配置策略 |
| `backend/src/lib/schemas.ts` | 现有 `PaginationSchema`、`DramaSchema`、query schema 风格，可扩展为商城 schema |
| `backend/src/services/drama/drama.service.ts` | 现有 service 层 parse / error handling 模式 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 现有 repository 抽象与分页结果类型 |
| `backend/src/repositories/repository-registry.ts` | repository-registry 默认走 mock repository 的接线方式 |
| `backend/src/middleware/error-handler.ts` | 统一错误响应 `withErrorHandler` 的现有 contract |
| `backend/src/lib/errors.ts` | `ErrorCode` 枚举与 `AppError` 约定 |
| `web/src/app/mall/page.tsx` | `/mall` 当前仍为占位路由 |
| `web/src/app/search/page.tsx` | 浏览器 / 本地开发模式的搜索降级承接点 |
| `web/src/lib/api-client.ts` | Web 现有请求封装方式 |
| `web/src/lib/config.ts` | Web 现有配置模块入口 |
| `ios/ShortDrama/Sources/App/TabNavigationHostView.swift` | mall tab 当前仍复用 `PlaceholderTabView` |
| `ios/ShortDrama/Sources/App/AppRoute.swift` | 搜索路由归属 `.home`，可复用现有搜索承接 |
| `ios/ShortDrama/Sources/App/NavigationRouter.swift` | iOS 当前 tab / path 管理模式 |
| `ios/ShortDrama/Sources/Features/Ranking/Views/RankingHomeView.swift` | 现有 iOS 登录拦截仅为 alert，说明商城需单独定义登录承接闭环 |
| `ios/ShortDrama/Sources/Features/Ranking/RankingRouteBuilder.swift` | 现有 `RankingLoginContext` 结构可作为 `MallLoginContext` 命名与字段粒度参考 |
| `ios/ShortDrama/Sources/Core/Config/AppConfig.swift` | iOS 配置读取入口 |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt` | Android mall 当前仍为 placeholder，search/ranking/classification 归属 HOME graph |
| `android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` | Android top-level tabs 与现有搜索路由常量 |
| `android/app/src/main/java/com/djs66256/short_drama/domain/repository/AuthSessionProvider.kt` | Android 已有登录态读取抽象 |
| `android/app/src/main/java/com/djs66256/short_drama/feature/ranking/viewmodel/RankingViewModel.kt` | Android 当前 `RequireLogin(returnRoute)` 语义可作为商城返回契约参考 |
| `android/app/src/main/java/com/djs66256/short_drama/core/config/AppConfig.kt` | Android 配置读取入口 |

---
