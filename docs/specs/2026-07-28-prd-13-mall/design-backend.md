# Backend 端技术方案：PRD-13 商城

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

```text
请求
→ app/api/mall/products/route.ts
→ withErrorHandler
→ MallProductsQuerySchema.parse(query)
→ MallService.listProducts(query)
→ MallRepositoryInterface.listProducts(query)
→ MallMockRepository.listProducts(query)
→ 返回 { data, pagination }
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/` | 扩展 | 新增商城资源路由组 `mall/products` |
| `backend/src/services/` | 扩展 | 新增 `services/mall/mall.service.ts` 承载商城只读业务逻辑 |
| `backend/src/repositories/interfaces/` | 扩展 | 新增 `MallRepositoryInterface`，避免将商品模型混入短剧仓储 |
| `backend/src/repositories/mock/` | 扩展 | 新增 `MallMockRepository` 提供首版商品 seed 数据 |
| `backend/src/repositories/repository-registry.ts` | 扩展 | 新增 `getMallRepository()` / `setMallRepository()` / `reset` 接线 |
| `backend/src/lib/schemas.ts` | 扩展 | 新增商城 query / entity / response schema |
| `backend/src/middleware/error-handler.ts` | 不变 | 继续复用统一错误处理 contract |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/mall/products/route.ts` | 新增 | 商城商品列表 GET 路由 |
| `backend/src/app/api/__tests__/mall-products.test.ts` | 新增 | 覆盖 query 校验、空态、错误态、分页 |
| `backend/src/services/mall/mall.service.ts` | 新增 | 商城商品列表 service |
| `backend/src/services/mall/mall.service.test.ts` | 新增 | service 层 schema 校验与异常映射测试 |
| `backend/src/repositories/interfaces/mall.repository.interface.ts` | 新增 | 商城仓储抽象 |
| `backend/src/repositories/mock/mall.mock.repository.ts` | 新增 | 商品 seed 与分页切片实现 |
| `backend/src/repositories/__tests__/mall.mock.repository.test.ts` | 新增 | mock 仓储分页顺序与边界测试 |
| `backend/src/repositories/repository-registry.ts` | 修改 | 注册 mall repository |
| `backend/src/lib/schemas.ts` | 修改 | 新增 `MallProductSchema`、`MallProductsQuerySchema`、`MallProductsResponseSchema` |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增补商城 schema 的合法/非法输入测试 |
| `backend/src/lib/errors.ts` | 不变 | 继续复用现有错误码，无需新增商城专属 code |

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/mall/products/route.ts` | `GET` | `/api/mall/products` | `withErrorHandler` → query schema parse → `MallService` | 商城首页双列 Feed 的唯一数据源 |

### 3.2 路由分组策略

- 商城相关接口统一归属新资源组 `/api/mall/*`，与现有 `/api/dramas/*` 分离。
- 首版只暴露只读列表接口，不在本期引入 `/cart`、`/orders`、`/wallet`、`/coupon` 等交易资源。
- 路由层继续保持“**Route 只做入参解析、调用 service、返回 JSON**”的现有模式，不在 Route 中拼接 seed 逻辑。

### 3.3 参数校验

```typescript
export const MallProductsQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(20),
});

export const MallProductSchema = z.object({
  id: z.string().uuid(),
  title: z.string().trim().min(1).max(200),
  image_url: z.string().url(),
  price: z.number().nonnegative(),
  tags: z.array(z.string().trim().min(1).max(20)).max(3).default([]),
});

export const MallProductsResponseSchema = z.object({
  data: z.array(MallProductSchema),
  pagination: PaginationSchema,
});
```

- Route 读取 `searchParams.get('page')`、`searchParams.get('pageSize')` 后交给 `MallProductsQuerySchema.parse(...)`。
- `page` / `pageSize` 的默认值与约束完全与 shared `design.md` 保持一致。
- 仓储输出统一再经过 `MallProductsResponseSchema.parse(...)`，确保 mock seed 与未来 Supabase 实现共享同一 contract。

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
请求
→ [logger]
→ [cors]
→ withErrorHandler(handler)
→ MallProductsQuerySchema.parse(query)
→ MallService.listProducts(query)
→ NextResponse.json({ data, pagination })
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `logger` | 全局 | 继续记录请求基础日志 |
| `cors` | 全局 | 继续沿用现有跨域策略 |
| `withErrorHandler` | 路由级 | 统一捕获 `AppError` / `ZodError` / 未知异常 |
| `auth` | 不使用 | 商城商品列表为公开只读接口，不要求登录 |
| `rate limit` | 预留 | 首版不单独实现；若后续接 Redis 限流，可接在 `/api/mall/*` 路由组 |

### 4.3 错误传播方式

- query 校验失败：直接抛 `ZodError`，由 `withErrorHandler` 返回 `400 + VALIDATION_ERROR + details`。
- service / repository 主动发现业务错误：抛 `AppError`（如未来数据源不可用时可抛 `Errors.serviceUnavailable('mall-products')`）。
- 未知异常：由 `withErrorHandler` 记录 `console.error` 并返回 `500 + INTERNAL_ERROR`。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `MallService` | 聚合商品列表读取、schema 校验、异常映射 | `MallProductsQuery` | `PaginatedResult<MallProduct>` | `MallRepositoryInterface` |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| `GET /api/mall/products` | 无事务 | 只读查询，无事务需求 |

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| `ZodError` | query 参数非法 | 400 | `VALIDATION_ERROR` |
| `AppError` | 仓储结果不可用 / schema 校验后的业务错误 | 500/503 | `INTERNAL_ERROR` / `SERVICE_UNAVAILABLE` |
| `Errors.internal('Invalid mall products result')` | 仓储返回的数据结构不符合 schema | 500 | `INTERNAL_ERROR` |

### 5.4 Service 设计要点

- 新建独立 `MallService`，避免把商品列表逻辑继续塞进 `DramaService`。
- `MallService` 对 repository 返回值做二次 schema parse，保证 mock / future supabase 实现都必须满足统一 contract。
- 首版不引入排序、筛选、关键词等业务逻辑，service 只负责分页边界与错误映射。

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| 无 | 无 | 首版不新增真实数据库表，使用 mock seed 数据 |

### 6.2 DDL

```sql
-- No-op for PRD-13 mall phase 1.
-- 首版商城商品列表不创建真实 Supabase 表。
```

### 6.3 字段详情

| 表名 | 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| 无 | — | — | — | — | 暂无真实表结构变更 |

### 6.4 索引策略

| 表名 | 索引名 | 类型（UNIQUE/INDEX） | 字段 | 用途 |
|------|--------|---------------------|------|------|
| 无 | — | — | — | 暂无 |

### 6.5 回滚策略

- 首版无 migration，因此无需数据库级回滚。
- 若未来引入 Supabase `mall_products` 表，应新增独立 migration，而不是修改首版设计文档中约定的 contract。

---

## 7. 后台任务/队列设计

### 7.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| 无 | — | — | — | — | — |

### 7.2 任务生命周期

```text
本期无异步任务 / 队列。
```

### 7.3 失败处理与死信队列

- 商城首版没有推送、订单、库存同步、运营配置下发等异步任务。
- 如果后续引入运营配置同步或缓存预热，再按 Redis / MQ 方案补充，不在本期提前设计。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| 应用名 | `APP_NAME` | 现有配置 | 现有配置 | 复用现有配置 |
| 应用版本 | `APP_VERSION` | 现有配置 | 现有配置 | 复用现有配置 |
| 数据源仓储选择（可选） | `MALL_PRODUCTS_REPOSITORY` | `mock`（默认） | 预留 `supabase` | 如未来需要切换真实数据源，可新增该配置 |
| Supabase URL | `SUPABASE_URL` | 现有配置 | 现有配置 | 仅未来接真实仓储时使用 |
| Redis URL | `REDIS_URL` | 现有配置 | 现有配置 | 本期不直接使用 |

> ⚠️ 禁止硬编码任何常量。首版即便走 mock repository，也应通过 `config` / registry 控制仓储接线，而不是把环境选择写死在 route 中。

---

## 9. API 调用清单（调用外部服务）

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| 无 | — | — | — | 首版仅使用本地 mock repository |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| 默认分页 | 首次请求固定 `page=1&pageSize=20` | `MallProductsQuerySchema` 默认值定义 |
| 公开只读列表 | 商品浏览不要求登录 | route 不接 auth middleware |
| 固定稳定顺序 | 首版按确定性顺序分页 | `MallMockRepository` 内固定商品 seed 顺序切片 |
| 空态 contract | 空列表返回 `200 + data=[] + pagination` | repository 对超大页码或空 seed 返回合法空分页 |
| 价格语义 | 返回原始 number，不返回格式化文案 | schema 只保留 `price: number` |
| 图片兜底责任 | `image_url` 合法但可能失效，由前端兜底 | backend 只校验 URL 结构，不负责占位图字段 |
| 追加不清空列表 | append failure 不影响已有内容 | 后端通过稳定分页 contract 支持前端状态机实现 |
| 请求防乱序 | 旧响应不能覆盖新状态 | 后端不做 session state，保持纯函数式分页接口 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | `withErrorHandler` | 捕获 `AppError` / `ZodError` / 未知异常 |
| Service | schema parse + `Errors.internal(...)` | 统一把异常仓储输出转成受控错误 |
| Repository | mock seed 切片 | 不抛框架异常，尽量返回合法分页结构 |
| 日志 | `console.error` + 现有 logger | 保留 request 维度基础日志 |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `VALIDATION_ERROR` | 400 | query 参数校验失败 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `INVALID_PARAMS` | 400 | 预留的业务参数错误 | `{ "error": { "code": "INVALID_PARAMS", "message": "..." } }` |
| `INTERNAL_ERROR` | 500 | 服务内部错误 / schema parse 失败 | `{ "error": { "code": "INTERNAL_ERROR", "message": "Internal server error" } }` |
| `SERVICE_UNAVAILABLE` | 503 | 未来真实数据源不可用 | `{ "error": { "code": "SERVICE_UNAVAILABLE", "message": "Service unavailable: mall-products" } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 缺省分页参数 | 不传 `page` / `pageSize` | 自动取默认值 `1 / 20` | 与 shared design 一致 |
| 非法分页参数 | `page=0`、`pageSize=101` | 返回 400 + `VALIDATION_ERROR` | 不静默兜底 |
| 超大页码 | `page > total_pages` | 返回 200 + 空数组 + 合法分页 | 前端据此停止追加 |
| seed 为空 | 仓储无商品数据 | 返回 200 + 空数组 | 前端展示空态 |
| seed 数据字段非法 | mock seed 不符合 schema | service 抛 `Errors.internal` | 避免脏数据直接出接口 |
| 未来仓储不可用 | Supabase / 其他数据源失败 | 抛 `SERVICE_UNAVAILABLE` | 预留扩展 |
| 高频访问 | 请求量超过限制 | 本期不单独限流；后续可加 429 | 先不超设计 |

### 11.4 错误日志与监控

- 接口测试覆盖非法 query、超大页码、空列表、合法分页切片。
- service 测试覆盖 repository 返回非法结构时的 `INTERNAL_ERROR` 映射。
- 若未来切 Supabase，实现层需补充 `requestId / page / pageSize` 维度日志。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| 单元测试 | `MallService` schema 校验、异常映射 | Vitest |
| 集成测试 | `GET /api/mall/products` 路由 query 校验与响应结构 | Vitest + Next Route Handler 测试模式 |
| Repository 测试 | `MallMockRepository` 的固定顺序、切片、空态、超大页码 | Vitest |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| BE-MALL-01 | 默认分页加载 | `GET /api/mall/products` | `200`，`page=1`，`page_size=20` | 集成 |
| BE-MALL-02 | 非法页码 | `page=0` | `400 + VALIDATION_ERROR` | 集成 |
| BE-MALL-03 | 非法 pageSize | `pageSize=101` | `400 + VALIDATION_ERROR` | 集成 |
| BE-MALL-04 | 超大页码 | `page=999` | `200 + data=[]` + 合法 pagination | 集成 |
| BE-MALL-05 | 固定顺序切片 | seed 数据 25 条 | 第 1 / 2 页顺序稳定且无重复 | Repository |
| BE-MALL-06 | 非法 seed 结构 | repository 返回坏数据 | service 抛 `INTERNAL_ERROR` | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| 商品仓储 | `MallMockRepository` / stub repository | service 测试通过依赖注入替换 |
| Next Request | Route 测试构造请求对象 | 不依赖真实 HTTP Server |
| Supabase / Redis | 不接入 | 首版商城接口不依赖真实基础设施 |

---

## 13. 安全考虑

- **认证与授权**：商品列表为匿名可读，不引入登录依赖。
- **输入校验**：query 全量经过 Zod schema 校验。
- **敏感数据处理**：不返回任何用户敏感字段；`id` 仅为商品资源标识。
- **SQL 注入防护**：首版无数据库查询；未来接真实表时使用参数化查询。
- **CSRF/XSS 防护**：只读 GET 接口不涉及写操作；字符串字段按 JSON 序列化输出，不回传 HTML 片段。

---

## 14. 性能考虑

- **预期 QPS**：低；首版以内测和演示场景为主。
- **缓存策略**：首版不加 Redis，mock seed 读成本可忽略。
- **数据库优化**：当前无数据库；未来若接真实表，优先按 `sort_order` / `created_at` 建索引保证稳定分页。
- **连接池配置**：沿用现有 Supabase / runtime 配置，本期无新增要求。

---

## 15. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 复用现有 Next.js + Zod + Vitest 能力 |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 16. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 商品模型与短剧模型混用导致仓储职责混乱 | Backend | 🟡 | 中 | 独立新增 `MallRepositoryInterface` 与 `MallService` | 暂时保留 mock repository，不把商品字段塞进 drama 仓储 |
| mock seed 结构不稳定导致前端联调频繁改 schema | Backend / Web | 🔴 | 中 | 先以 `MallProductsResponseSchema` 固化 contract，再写 seed | schema 测试拦截坏数据 |
| 未来切真实数据源时 contract 漂移 | Backend / 全端 | 🟡 | 中 | route / service 层先固化 schema，真实仓储只替换 repository 实现 | 保留 mock 仓储开关 |
| 首版无限流可能被高频访问拖慢 | Backend | 🟢 | 低 | 保持接口简单只读；必要时后续对 `/api/mall/*` 增加 Redis 限流 | 临时在网关层限流 |

---

## 17. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/features/app-shell/index.md` | Backend、已知限制 | 当前应用壳仍未接入真实 mall 内容，需新增 `/api/mall/products` |
| `wiki/features/search-discovery/index.md` | Backend、多端实现 | 参考现有 Route + Service + Mock Repository 的实现与测试风格 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `backend/CLAUDE.md` | Backend 四层架构、统一错误处理、测试要求 |
| `backend/src/lib/schemas.ts` | 现有分页 schema 与 Zod 风格 |
| `backend/src/lib/errors.ts` | `ErrorCode` / `AppError` / `Errors.*` 约定 |
| `backend/src/middleware/error-handler.ts` | `withErrorHandler` 错误格式 |
| `backend/src/services/drama/drama.service.ts` | 现有 service 层 parse / error mapping 模式 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 分页结果类型与仓储抽象风格 |
| `backend/src/repositories/repository-registry.ts` | registry 接线模式 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | mock seed / 切片 / schema parse 风格 |
| `backend/src/app/api/dramas/rankings/route.ts` | Route 层 query parse + service 调用写法 |
| `docs/specs/2026-07-28-prd-13-mall/design.md` | 商城共享 contract 与状态机 |
