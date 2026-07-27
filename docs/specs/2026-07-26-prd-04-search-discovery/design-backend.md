# Backend 端技术方案：PRD-04 搜索发现

> 创建日期：2026-07-26
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Backend 端在现有 `GET /api/dramas` 首页列表能力之上，新增搜索与热搜两个只读接口，继续沿用当前四层结构：Route → Service → Repository → Shared。首版数据仍来自 mock repository，不引入真实搜索服务、数据库表、异步任务或新依赖。

```text
GET /api/dramas/search
  -> Route Handler (`app/api/dramas/search/route.ts`)
     -> Zod Query 校验（q / page / pageSize）
     -> DramaService.searchDramas(params)
        -> DramaRepository.search(params)
           -> 基于现有 drama mock 数据做 title/category 包含匹配
           -> 结果分页切片
        -> DramaListResponseSchema.parse(result)
     -> 返回 { data, pagination }

GET /api/dramas/hot-search
  -> Route Handler (`app/api/dramas/hot-search/route.ts`)
     -> DramaService.listHotSearches()
        -> DramaRepository.listHotSearches()
           -> 返回静态/种子热搜 Top 10
        -> HotSearchListResponseSchema.parse(result)
     -> 返回 { data }
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/dramas/route.ts` | 不变 | 保持首页 Feed canonical `GET /api/dramas` 行为，不被搜索发现改写 |
| `backend/src/app/api/dramas/search/route.ts` | 新增 | 承载关键词搜索路由，复用现有 `withErrorHandler` |
| `backend/src/app/api/dramas/hot-search/route.ts` | 新增 | 承载热搜榜路由，保持只读返回 |
| `backend/src/services/drama/drama.service.ts` | 扩展 | 在现有 `DramaService` 中增加 `searchDramas`、`listHotSearches` 两个只读能力 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 扩展 | 增加搜索参数类型、搜索查询方法、热搜列表方法 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 扩展 | 复用现有 12 条 drama 种子数据，增加 title/category 匹配与热搜种子数据 |
| `backend/src/lib/schemas.ts` | 扩展 | 新增搜索 query schema、热搜 item/response schema，复用 `DramaSchema` / `DramaListResponseSchema` |
| `backend/src/middleware/error-handler.ts` | 不变 | 继续作为统一错误出口，不为本期新增专用错误中间件 |
| `backend/src/lib/config.ts` | 不变 | 首版不新增搜索相关环境变量 |

### 1.2 设计取舍

- **不单独拆出 SearchService**：首版搜索与热搜都属于 `Drama` 资源域上的只读派生能力，且共用同一 mock 数据源；为避免过早拆分服务，本期收敛在 `DramaService` 中实现。
- **不引入真实存储**：spec 与 shared design 已明确首版允许静态 / 伪实时热搜，搜索结果也只要求匹配现有 drama 数据，因此现阶段无需接 Supabase 表或 Redis 缓存。
- **不改变现有首页接口**：`GET /api/dramas` 继续服务首页 Feed；搜索发现新增能力通过新路由补充，不修改旧客户端行为。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/dramas/search/route.ts` | 新增 | 新增 `GET /api/dramas/search` 路由，解析 query 并调用 `DramaService.searchDramas` |
| `backend/src/app/api/dramas/hot-search/route.ts` | 新增 | 新增 `GET /api/dramas/hot-search` 路由，调用 `DramaService.listHotSearches` |
| `backend/src/services/drama/drama.service.ts` | 修改 | 增加搜索与热搜 service 方法，并对 repository 输出做 schema 校验 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 增加 `SearchDramasParams`、`HotSearchItem` 相关接口定义与 repository 方法 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 基于现有种子数据实现 title/category 大小写不敏感搜索与热搜种子返回 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 若 `DramaRepositoryInterface` 扩展搜索 / 热搜方法，则必须同步补齐 Supabase 实现；首版可采用与 mock 一致的 `title/category` 搜索与静态热搜占位实现，确保所有实现满足同一接口契约 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 `SearchDramaQuerySchema`、`HotSearchItemSchema`、`HotSearchListResponseSchema` |
| `backend/src/app/api/__tests__/dramas-search.test.ts` | 新增 | 覆盖搜索路由成功、空结果、参数非法、分页边界、内部错误 |
| `backend/src/app/api/__tests__/dramas-hot-search.test.ts` | 新增 | 覆盖热搜路由成功、内部错误 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 补充 service 层搜索命中、空结果、热搜输出校验 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 补充 mock repository 搜索匹配、大小写不敏感、category 命中、热搜数量边界测试 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 若接口扩展到所有 repository 实现，则补充 Supabase repository 的搜索 / 热搜契约测试，至少覆盖空结果与基本成功路径 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补充新增搜索 query / 热搜 schema 校验 |

> 注：测试文件命名可在实现阶段按现有 Vitest 目录习惯微调，但覆盖范围必须满足本文档约束。

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/dramas/search/route.ts` | `GET` | `/api/dramas/search` | `withErrorHandler` + Zod query 校验 | 关键词搜索接口，返回 `DramaListResponse` |
| `backend/src/app/api/dramas/hot-search/route.ts` | `GET` | `/api/dramas/hot-search` | `withErrorHandler` | 热搜榜接口，返回 `{ data: HotSearchItem[] }` |
| `backend/src/app/api/dramas/route.ts` | `GET` | `/api/dramas` | `withErrorHandler` + Zod query 校验 | 既有首页 Feed 接口，保持不变 |

### 3.2 路由分组策略

- 继续按资源维度归档在 `app/api/dramas/` 下，不引入 `/api/v1` 或额外别名。
- `search` 与 `hot-search` 都是 `dramas` 资源域上的派生查询，因此采用子路径而不是平铺到 `/api/search`。
- 首页 Feed、搜索结果、热搜榜共用同一 `DramaService` / `DramaRepositoryInterface`，避免路由层直接操作 mock 数据。

### 3.3 参数校验

```typescript
export const SearchDramaQuerySchema = z.object({
  q: z.string().trim().min(1).max(50),
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(10),
});

export const HotSearchItemSchema = z.object({
  rank: z.number().int().min(1),
  keyword: z.string().trim().min(1).max(50),
  score: z.number().int().min(0),
});

export const HotSearchListResponseSchema = z.object({
  data: z.array(HotSearchItemSchema).max(10),
});
```

参数约束结论：

| 参数 | 规则 | 说明 |
|------|------|------|
| `q` | `trim().min(1).max(50)` | 搜索关键词必填，去首尾空格后不能为空 |
| `page` | `int >= 1` | 与现有 `/api/dramas` 保持一致 |
| `pageSize` | `int >= 1 && <= 100` | 默认 10，最大 100 |
| 热搜接口 query | 无 | 首版固定返回最多 10 条，不分页 |

### 3.4 响应契约

| 接口 | 成功响应 | 说明 |
|------|---------|------|
| `GET /api/dramas/search` | `DramaListResponse` | 严格复用现有 drama 列表结构 |
| `GET /api/dramas/hot-search` | `{ data: HotSearchItem[] }` | 轻量列表结构，不带 pagination |
| 所有失败响应 | `{ error: { code, message } }` | 与现有 `withErrorHandler` 输出保持一致 |

### 3.5 匹配与排序规则

- 搜索匹配维度固定为 **`title + category`**，与 spec / shared design 一致。
- 匹配方式采用**大小写不敏感包含匹配**；实现时统一将关键词与待匹配字段做标准化（如 `trim().toLowerCase()`）后比较。
- 搜索结果排序首版沿用 mock repository 当前稳定数据顺序，不额外引入“相关度排序”“最新排序”或“热度排序”。
- 当 `page` 超过总页数时，返回 `200 + data=[]`，保留正确 pagination 元信息，不抛错。

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
请求
  -> withErrorHandler
  -> Route 内解析 searchParams
  -> Zod Query 校验（仅 /search）
  -> DramaService
  -> DramaRepository
  -> Schema parse 输出校验
  -> JSON 响应
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 路由级 | 统一捕获 `AppError` / `ZodError` / 未知异常，并输出 JSON 错误结构 |
| `auth` | 不接入 | 搜索发现首版匿名可访问，不增加认证约束 |
| `logger` | 不新增改造 | 继续沿用现有日志能力，不为本期新增 request logger 规范 |
| `cors` | 不新增改造 | 保持现状，本期无跨域特殊需求 |
| `rate limit` | 不接入 | 首版不新增 Redis 限流链路；后续若接公网再补 |

### 4.3 错误传播方式

- **query 参数错误**：由 route 中的 Zod parse 直接抛出，`withErrorHandler` 统一转成 `400 + VALIDATION_ERROR`。
- **业务层异常**：service / repository 主动抛出 `AppError` 时，由 `withErrorHandler` 输出对应错误码。
- **未知异常**：由 `withErrorHandler` 兜底记录日志并返回 `500 + INTERNAL_ERROR`。
- **内部数据契约错误**：service 在对 repository 输出做 schema 校验时，如果发现是服务端内部数据不合法，应转换为 `Errors.internal(...)`，避免把内部数据问题错误标记为客户端参数错误。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `DramaService.listDramas` | 既有首页 Feed 查询 | `PaginationParams` | `PaginatedResult<Drama>` | `DramaRepositoryInterface` |
| `DramaService.searchDramas` | 执行关键词搜索并校验列表响应 | `SearchDramasParams` | `PaginatedResult<Drama>` | `DramaRepositoryInterface` |
| `DramaService.listHotSearches` | 返回热搜榜并校验响应 | 无 | `HotSearchListResponse` | `DramaRepositoryInterface` |
| `DramaService.getDramaById` | 保持未实现 | `id` | `Drama` | `DramaRepositoryInterface` |
| `DramaService.createDrama` | 保持未实现 | `payload` | `Drama` | `DramaRepositoryInterface` |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| 搜索查询 | 不涉及事务 | 纯只读内存查询，无回滚 |
| 热搜查询 | 不涉及事务 | 纯只读静态数据返回，无回滚 |

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| 参数校验异常 | `q` 为空/超长，`page/pageSize` 非法 | 400 | `VALIDATION_ERROR` |
| 内部数据异常 | repository 返回结构不满足响应 schema | 500 | `INTERNAL_ERROR` |
| 未知内部异常 | service / repository 运行时异常 | 500 | `INTERNAL_ERROR` |

### 5.4 Service 方法设计

#### `searchDramas(params)`

职责：
- 接收已通过 route 校验的 `q/page/pageSize`
- 调用 repository 执行搜索
- 使用 `DramaListResponseSchema` 校验输出契约
- 保证大页码时仍返回空数组而不是异常

实现约束：
- service 不做额外排序策略，只做编排与输出校验
- 不在 service 层持久化用户搜索词
- 不增加缓存、限流或异步逻辑

#### `listHotSearches()`

职责：
- 调用 repository 返回热搜种子数据
- 使用 `HotSearchListResponseSchema` 校验数据结构
- 控制输出始终不超过 10 条

实现约束：
- 首版热搜为只读种子数据，不依赖实时统计
- service 不做分页，不做个性化，不做 AB 分流

---

## 6. Repository 扩展设计

### 6.1 接口扩展

建议在 `backend/src/repositories/interfaces/drama.repository.interface.ts` 中补充：

```typescript
export interface SearchDramasParams extends PaginationParams {
  q: string;
}

export interface HotSearchItem {
  rank: number;
  keyword: string;
  score: number;
}

export interface DramaRepositoryInterface {
  findMany(params: PaginationParams): Promise<PaginatedResult<Drama>>;
  search(params: SearchDramasParams): Promise<PaginatedResult<Drama>>;
  listHotSearches(): Promise<{ data: HotSearchItem[] }>;
  findById(id: string): Promise<Drama | null>;
  create(data: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama>;
  update(id: string, data: Partial<Omit<Drama, 'id' | 'created_at' | 'updated_at'>>): Promise<Drama | null>;
  delete(id: string): Promise<boolean>;
}
```

### 6.2 Mock Repository 实现策略

| 能力 | 实现方式 | 说明 |
|------|---------|------|
| 搜索数据源 | 复用 `HOMEPAGE_DRAMAS` | 首版不引入第二套数据源 |
| 搜索匹配 | `title` / `category` 标准化后做包含匹配 | 与 shared design 保持一致 |
| 分页行为 | 对过滤结果再做 slice | 与现有 `/api/dramas` 保持同一分页语义 |
| 热搜数据源 | 新增 `HOT_SEARCH_ITEMS` 常量 | 最多 10 条，稳定顺序 |
| 数据隔离 | 继续通过 clone 返回数据副本 | 避免测试与运行态共享引用 |

### 6.3 多实现仓储的一致性约束

由于当前仓库内已存在 `DramaSupabaseRepository`，如果本期选择在 `DramaRepositoryInterface` 上直接扩展 `search(...)` 与 `listHotSearches()`，则 **所有实现类都必须同步满足新契约**，否则 TypeScript 编译无法通过。

首版约束固定如下：

- `backend/src/repositories/interfaces/drama.repository.interface.ts`：新增 `search(...)` 与 `listHotSearches()` 后，必须同步更新 `backend/src/services/drama/drama.service.ts` 的调用入口，避免只改 repository 抽象而未闭合 service 层。
- `DramaMockRepository`：实现完整搜索与热搜种子数据能力，用于当前默认开发路径。
- `DramaSupabaseRepository`：即使本期默认运行路径不使用，也必须补齐同名方法以满足接口一致性；其中：
  - `search(...)` 可基于 `dramas` 表的 `title` / `category` 执行大小写不敏感匹配；
  - `listHotSearches()` 首版允许返回与 mock 对齐的静态热搜种子数据，或在实现中显式复用共享常量；
  - 若当前 Supabase 数据源暂不具备热搜真实来源，仍应保证返回结构满足 `HotSearchListResponseSchema`。
- 对应测试必须覆盖 mock 与 supabase 两类实现，并按当前真实目录分别落在 `backend/src/repositories/__tests__/drama.mock.repository.test.ts` 与 `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`，至少验证：接口方法存在、返回结构合法、空结果与成功路径可用。

### 6.4 未来可演进点（本期不做）

- `supabase/drama.supabase.repository.ts` 进一步优化为真实数据库检索与排序能力
- 基于 Redis 做热搜缓存或限流
- 基于真实搜索日志计算热搜榜
- 引入全文索引、标签搜索、演员搜索、联想词补全

> 以上能力均不属于 PRD-04 首版交付，不写入本期接口契约。

---

## 7. Schema 设计

### 7.1 新增 Schema

| Schema | 文件 | 用途 |
|-------|------|------|
| `SearchDramaQuerySchema` | `backend/src/lib/schemas.ts` | 校验 `/api/dramas/search` query 参数 |
| `HotSearchItemSchema` | `backend/src/lib/schemas.ts` | 约束单条热搜项结构 |
| `HotSearchListResponseSchema` | `backend/src/lib/schemas.ts` | 约束热搜接口成功响应 |

### 7.2 复用 Schema

| Schema | 用途 | 说明 |
|-------|------|------|
| `DramaSchema` | 单条搜索结果 | 与首页 Feed 卡片字段保持一致 |
| `DramaListResponseSchema` | 搜索接口成功响应 | 复用已有 `data + pagination` 契约 |
| `ErrorCode` / `AppError` | 错误表示 | 保持现有错误体系，不新增搜索专用错误码枚举 |

### 7.3 约束说明

- 搜索结果**不得**新增 `actor`、`play_count`、`heat` 等首页与 spec 未定义字段。
- 热搜项只包含 `rank`、`keyword`、`score`，不引入跳转类型、icon、趋势箭头等额外字段。
- `SearchHistoryItemSchema` 只存在于 shared design 与移动端本地持久化语义中，Backend 不新增该 schema，也不提供历史 API。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| 应用名称 | `APP_NAME` | 现有配置 | 现有配置 | 与搜索发现无新增耦合 |
| 应用版本 | `APP_VERSION` | 现有配置 | 现有配置 | 可用于通用日志/健康检查，不新增搜索用途 |
| 监听端口 | `PORT` | 现有配置 | 现有配置 | 搜索发现不新增端口 |
| Supabase URL | `SUPABASE_URL` | 现有配置 | 现有配置 | 本期搜索不接入真实库 |
| Supabase Anon Key | `SUPABASE_ANON_KEY` | 现有配置 | 现有配置 | 本期不直接使用 |
| Supabase Service Role Key | `SUPABASE_SERVICE_ROLE_KEY` | 现有配置 | 现有配置 | 本期不直接使用 |
| Redis URL | `REDIS_URL` | 现有配置 | 现有配置 | 本期不用于缓存、队列或限流 |

关键结论：

- **首版不新增任何搜索相关环境变量**。
- **首版不新增 config 模块字段**，继续使用 `backend/src/lib/config.ts` 现有结构。
- 搜索发现的首版能力全部可以在当前 mock repository + shared schema 范式下完成，无需额外环境注入。

> ⚠️ 遵守 `backend/CLAUDE.md`：所有环境相关值仍通过 config 模块读取；本期仅因为没有新增配置需求，所以不修改 config。

---

## 9. 数据库 Migration 计划

### 9.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| 无 | 无 | 首版搜索与热搜都基于现有 mock 数据，不新增数据库表 |

### 9.2 是否需要 Migration

**不需要。** 原因如下：

1. `GET /api/dramas/search` 直接复用现有 mock drama 数据集做只读过滤，不落库。
2. `GET /api/dramas/hot-search` 首版允许使用静态 / 种子数据，不依赖真实搜索日志表。
3. 首版没有用户账户联动，也不需要保存搜索历史，因此不需要 `search_history`、`search_logs`、`hot_keywords` 等新表。
4. spec 与 shared design 都明确首版目标是建立搜索发现主链路，而非建设完整内容检索基础设施。

### 9.3 回滚策略

- 无 migration 文件可回滚。
- 若功能回退，只需删除新增 route / service / repository / schema / test 改动即可。

---

## 10. 后台任务/队列设计

### 10.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| 无 | 无 | 无 | 无 | 无 | 无 |

### 10.2 是否需要异步队列

**不需要。** 原因如下：

- 搜索查询是同步只读过滤，数据量极小，直接在请求链路内完成即可。
- 热搜榜首版不做真实日志聚合，不需要离线计算、定时任务或 Redis 队列消费。
- 搜索历史由移动端本地存储，Backend 不承担异步落库或同步任务。
- shared design 已明确首版不新增异步任务。

### 10.3 后续演进边界（本期不做）

- 若未来引入真实搜索日志统计，可再评估定时聚合任务或队列化计算。
- 若未来热搜榜来自实时流量，再考虑 Redis / Supabase 上的缓存与聚合链路。

---

## 11. API 调用清单（调用外部服务）

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| 无 | 无 | 无 | 无 | 无 |

本期 Backend 不调用第三方服务，也不新增对 Supabase 或 Redis 的实时调用链路。

---

## 12. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| 搜索 API 契约 | `GET /api/dramas/search?q&page&pageSize -> DramaListResponse` | 新增 `/api/dramas/search` route，service 输出统一走 `DramaListResponseSchema` |
| 热搜 API 契约 | `GET /api/dramas/hot-search -> { data: HotSearchItem[] }` | 新增 `/api/dramas/hot-search` route，返回最多 10 条热搜项 |
| 匹配规则 | `title + category`，大小写不敏感包含匹配 | mock repository 统一标准化字符串后过滤 |
| 大页码行为 | `200 + data=[]` | 搜索结果分页逻辑对齐现有 `/api/dramas` |
| 首版不持久化历史 | 搜索历史只保存在端侧 | Backend 不新增 history API、表或日志写入 |
| 热搜失败不阻塞手动搜索 | `/hot-search` 失败只影响局部区块 | 两个 route 相互独立，无共享失败链路 |
| 错误响应格式 | `{ error: { code, message } }` | 继续复用 `withErrorHandler` 与 `Errors` |

---

## 13. 边界与错误处理

### 13.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | `withErrorHandler` | 统一返回 JSON 错误结构 |
| Query 校验 | Zod | 拦截 `q/page/pageSize` 非法请求 |
| Service | `Errors.internal(...)` | 包装内部输出契约错误，避免误报为 400 |
| Repository | 只读查询 | 仅负责数据过滤与组装，不直接决定 HTTP 语义 |
| 日志 | `console.error` + 现有测试日志 | 复用当前机制，不新增监控平台 |

### 13.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `VALIDATION_ERROR` | 400 | `q` 为空/超长，或 `page/pageSize` 非法 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `INTERNAL_ERROR` | 500 | repository/service 未知异常，或内部输出不满足 schema | `{ "error": { "code": "INTERNAL_ERROR", "message": "Internal server error" } }` |

### 13.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 空参数/缺参数 | `q` 缺失、为空或仅空格 | 返回 400 `VALIDATION_ERROR` | 客户端应优先阻止，但服务端必须兜底 |
| 参数边界值 | `q` 长度 > 50，`page=0`，`pageSize=101` | 返回 400 `VALIDATION_ERROR` | 与 spec 对齐 |
| 特殊字符 | emoji、零宽字符、注入片段、全角空格 | 作为普通字符串匹配或无结果返回 | 不应触发 500 |
| 大页码 | `page` 超过总页数 | 返回 200 + `data=[]` | 分页元信息保持正确 |
| 搜索空结果 | 无 drama 命中 | 返回 200 + `data=[]` | 不返回 404 |
| 热搜不足 10 条 | 种子数据少于 10 条 | 按实际条数返回 | 不补伪项 |
| 热搜重复关键词 | 种子数据配置错误 | 实现层应尽量避免，测试层需覆盖 | 首版由固定常量控制 |
| 内部数据格式错误 | repository 返回字段缺失或不合法 | 包装为 500 `INTERNAL_ERROR` | 避免把内部数据问题暴露为客户端错误 |

### 13.4 错误日志与监控

- 本期不新增日志依赖、不新增告警平台。
- `withErrorHandler` 继续作为未知异常的统一日志出口。
- service 层若捕获到 repository 输出契约错误，应记录最小必要上下文（接口名、异常类型），但不输出敏感环境信息。

---

## 14. 测试策略

### 14.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| Route 测试 | `/api/dramas/search`、`/api/dramas/hot-search` 正常与异常响应 | Vitest + `NextRequest` |
| Service 单元测试 | 搜索编排、输出 schema 校验、热搜返回 | Vitest |
| Repository 单元测试 | 搜索匹配、分页切片、热搜数量与顺序 | Vitest |
| Schema 测试 | query / hot search schema 边界校验 | Vitest + Zod |
| 错误处理中回归测试 | 参数异常 400、内部异常 500 | Vitest |

### 14.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| B-01 | 搜索正常命中标题 | `/api/dramas/search?q=逆袭&page=1&pageSize=10` | 200，返回命中标题项，分页结构完整 | Route |
| B-02 | 搜索命中分类 | `/api/dramas/search?q=都市&page=1&pageSize=10` | 200，返回 `category=都市` 的结果 | Route / Repository |
| B-03 | 搜索大小写不敏感 | `q=city` 等英文样例或统一标准化测试数据 | 命中逻辑大小写不敏感 | Repository |
| B-04 | 搜索空结果 | `/api/dramas/search?q=不存在的关键词` | 200，`data=[]` | Route / Service |
| B-05 | 搜索空参数 | `/api/dramas/search?q=   ` | 400，`VALIDATION_ERROR` | Route |
| B-06 | 搜索超长参数 | `q` 长度 51 | 400，`VALIDATION_ERROR` | Route / Schema |
| B-07 | 分页参数非法 | `page=0` 或 `pageSize=101` | 400，`VALIDATION_ERROR` | Route |
| B-08 | 大页码分页边界 | `page=999&pageSize=10` | 200，`data=[]` 且 pagination 正确 | Route / Repository |
| B-09 | 热搜正常返回 | `/api/dramas/hot-search` | 200，`data.length <= 10`，字段含 `rank/keyword/score` | Route |
| B-10 | 热搜内部异常 | mock repository 抛错 | 500，`INTERNAL_ERROR` | Route / Service |
| B-11 | 内部输出契约错误 | mock 非法热搜项或非法 drama 结果 | 500，`INTERNAL_ERROR` | Service |

### 14.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| Drama 数据源 | 复用 `DramaMockRepository` | 不依赖真实 Supabase |
| 热搜数据源 | repository 内固定种子数据 | 不依赖真实日志系统 |
| Service 依赖 | 依赖注入 mock repository | 验证 service 编排与错误包装 |
| Route 依赖 | 直接构造 `NextRequest` | 保持与现有 route 测试风格一致 |

### 14.4 测试落地要求

- `backend/src/app/api/__tests__/` 至少新增搜索与热搜两个 route 级测试文件。
- `backend/src/services/drama/drama.service.test.ts` 必须新增 `searchDramas` 与 `listHotSearches` 测试。
- `backend/src/repositories/__tests__/drama.mock.repository.test.ts` 必须补齐搜索匹配规则测试，而不仅验证分页。
- 不要求新增端到端测试框架；继续使用仓库当前 `vitest` 体系。

---

## 15. 安全考虑

- **认证与授权**：首版搜索与热搜接口均匿名可访问，不新增登录依赖。
- **输入校验**：`q/page/pageSize` 全量经 Zod 校验，客户端与服务端双重兜底。
- **敏感数据处理**：Backend 不保存用户搜索历史或个人化偏好，不新增用户行为存储。
- **注入防护**：首版不拼接 SQL；特殊字符只作为普通字符串处理。
- **错误暴露控制**：响应仅输出 `{ code, message }`，不回传内部堆栈或环境信息。

---

## 16. 性能考虑

- **预期 QPS**：首版基于 mock 数据，QPS 压力极低，以行为正确性优先。
- **搜索复杂度**：当前数据量为种子数据规模，内存过滤足以满足本地开发目标。
- **缓存策略**：首版不新增 Redis 缓存；热搜直接由固定种子数据提供。
- **数据库优化**：首版无数据库查询，因此不存在 migration 或索引优化动作。
- **后续演进**：若未来接真实库，再围绕 `title` / `category` 搜索建立索引或全文检索策略。

---

## 17. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | 无 | 无 | 首版完全复用现有 Next.js、Zod、Vitest 与 mock repository 体系 |

关键结论：

- **首版不新增任何开源依赖。**
- 现有技术栈已足以完成搜索发现 Backend 端交付。

---

## 18. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| mock 数据搜索覆盖有限 | 搜索结果真实性不足 | 🟡 | 中 | 明确首版目标为链路打通；测试覆盖 title/category 两类匹配 | 保持接口契约不变，后续替换为真实 repository |
| 热搜静态数据不具备实时性 | 产品展示与真实热度脱节 | 🟡 | 高 | 在文档中明确首版只做种子热搜，不承诺实时统计 | 回退到更少固定项，保持接口稳定 |
| 内部 schema 校验错误被误判为 400 | 错误语义不准确 | 🟡 | 中 | service 层包装内部输出错误为 `INTERNAL_ERROR` | 如实现未完成，至少补自动化测试阻断 |

---

## 19. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/api/dramas.md` | `GET /api/dramas` | 现有 drama 列表契约、分页行为与错误码风格 |
| `wiki/architecture/overview.md` | Backend API 服务层 / 设计决策 | 当前后端仍基于 `DramaService -> DramaMockRepository` 提供首页列表 |
| `wiki/features/homepage-feed/index.md` | Backend / API 引用 / 状态管理 | 搜索结果需要复用现有 `DramaSchema` 与分页语义 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `backend/src/app/api/dramas/route.ts` | 现有 `/api/dramas` query 校验与 service 接线方式 |
| `backend/src/lib/schemas.ts` | `DramaSchema`、`DramaListResponseSchema` 与 schema 风格 |
| `backend/src/lib/errors.ts` | `ErrorCode`、`AppError` 与错误响应格式 |
| `backend/src/middleware/error-handler.ts` | `AppError` / `ZodError` / 未知异常的统一处理方式 |
| `backend/src/services/drama/drama.service.ts` | 当前 `DramaService` 结构，适合作为搜索与热搜的扩展入口 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 当前 drama 种子数据、clone 策略与分页实现 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | repository 抽象边界与分页返回类型 |
| `backend/src/lib/config.ts` | 现有环境变量读取方式，确认首版无需新增配置 |
| `backend/src/app/api/__tests__/dramas.test.ts` | 现有 route 测试风格与分页边界断言 |
| `backend/src/services/drama/drama.service.test.ts` | 现有 service 测试模式 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 现有 repository 测试模式 |
| `docs/specs/2026-07-26-prd-04-search-discovery/spec.md` | 搜索发现需求范围、参数约束、自动化验收要求 |
| `docs/specs/2026-07-26-prd-04-search-discovery/design.md` | shared API、匹配规则、错误码与跨端约束 |
