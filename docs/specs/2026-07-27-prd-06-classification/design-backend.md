# Backend 端技术方案：PRD-06 分类浏览

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Backend 端继续沿用当前仓库已落地的四层结构：Route → Service → Repository → Infrastructure / Shared。在不引入新依赖、不修改现有成功响应包裹风格的前提下，为分类浏览补齐一个新的只读接口 `GET /api/dramas/tags`，并对既有 `GET /api/dramas/search` 做增量扩展，使其匹配范围从 `title + category` 扩展为 `title + category + tags`，从而让「分类页 → 标签点击 → 搜索结果页」链路完整闭合。

```text
GET /api/dramas/tags?gender=all|male|female
  -> Route Handler (`app/api/dramas/tags/route.ts`)
     -> withErrorHandler
     -> ClassificationTagsQuerySchema.parse({ gender })
     -> DramaService.listClassificationTags(query)
        -> DramaRepository.listClassificationTags(query)
           -> Mock: 固定三维度种子 + all 去重合并
           -> Supabase: 首版可复用同一套仓库内种子 / 兼容真实内容表聚合
        -> ClassificationTagsResponseSchema.parse(result)
     -> 返回 { data: { gender, dimensions } }

GET /api/dramas/search?q=标签名&page=1&pageSize=10
  -> Route Handler (`app/api/dramas/search/route.ts`)
     -> withErrorHandler
     -> SearchDramaQuerySchema.parse(...)
     -> DramaService.searchDramas(query)
        -> DramaRepository.search(query)
           -> Mock: title/category/tags 三字段匹配
           -> Supabase: title/category(+ tags 列或兼容表达式) 查询
        -> DramaListResponseSchema.parse(result)
     -> 返回 { data, pagination }
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/dramas/search/route.ts` | 修改 | 保持既有 query contract 与成功响应结构不变，仅继续复用扩展后的搜索能力 |
| `backend/src/app/api/dramas/tags/route.ts` | 新增 | 新增分类标签接口路由，承载 `gender` query 解析与 service 调用 |
| `backend/src/services/drama/drama.service.ts` | 扩展 | 在现有列表 / 搜索 / 热搜 / 排行 / 预约能力上，新增分类标签查询编排 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 扩展 | 增加分类标签查询参数与返回契约 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 扩展 | 新增分类标签种子与 `all` 合并逻辑；搜索从 `title + category` 扩展到 `title + category + tags` |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 扩展 | 补齐分类标签能力接口，并把搜索查询扩展到 tags 匹配 |
| `backend/src/lib/schemas.ts` | 扩展 | 新增 classification gender/query/response schema |
| `backend/src/lib/errors.ts` | 不变 | 继续复用 `VALIDATION_ERROR`、`INTERNAL_ERROR`、`SERVICE_UNAVAILABLE` 等既有错误码 |
| `backend/src/middleware/error-handler.ts` | 不变 | 继续统一处理 Zod 与 AppError，输出 `{ error: { code, message } }` |
| `backend/supabase/migrations/*` | 不变 | 首版不要求新增真实表；若后续接入配置化标签，再通过新增 migration 演进 |

### 1.2 与 shared design / 代码现状的兼容说明

1. **成功响应继续沿用当前资源体直出风格**  
   当前 Backend 已实现的 `GET /api/dramas`、`GET /api/dramas/search`、`GET /api/dramas/rankings` 都采用“成功直接返回资源体、失败返回 `{ error: { code, message } }`”模式。本期分类接口继续保持一致，不新增 `{ code, data, message }` 包裹层。

2. **分类标签首版使用 Repository 内种子数据，不强依赖真实数据库表**  
   shared design 已明确“classification tag seed”为逻辑模型，因此本期设计不要求马上新增数据库表或 migration。Mock / Supabase repository 都可在仓库层维护同一套 canonical 种子结构，优先保障 contract 稳定与跨端闭环。

3. **搜索扩展属于增量兼容，而不是另起新接口**  
   分类页标签点击后明确复用现有 `GET /api/dramas/search` 与移动端既有搜索结果页，因此后端不新增 `/api/dramas/tag-search`、`/api/tags/search` 等新接口，只在原接口内扩展匹配字段。

4. **Service 边界继续集中在 DramaService**  
   当前 `DramaService` 已承载列表、搜索、排行、热搜、预约等 `dramas` 资源域能力。分类浏览仍然围绕 drama 内容索引展开，因此继续把分类标签聚合放在 `DramaService`，不拆分新的 `ClassificationService`。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/dramas/tags/route.ts` | 新增 | 新增 `GET /api/dramas/tags` 路由，解析 `gender` 并返回分类标签结构 |
| `backend/src/app/api/dramas/search/route.ts` | 修改 | 保持 route 结构不变，继续调用扩展后的 `DramaService.searchDramas` |
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增 `listClassificationTags`，并继续对 repository 返回结果做 schema 校验 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 新增 `ClassificationTagsQuery`、`ClassificationDimension`、`ClassificationTagsResult`、`listClassificationTags()` |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 新增固定三维度分类种子、`all` 合并逻辑、空维度保留逻辑，并把搜索扩展到 tags |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 实现 `listClassificationTags()`；搜索查询扩展到 tags 匹配 |
| `backend/src/lib/schemas.ts` | 修改 | 增加 `ClassificationGenderSchema`、`ClassificationTagsQuerySchema`、`ClassificationDimensionSchema`、`ClassificationTagsResponseSchema` |
| `backend/src/app/api/__tests__/dramas-tags.test.ts` | 新增 | 覆盖合法 `gender`、默认 `all`、非法参数、空维度、内部异常 |
| `backend/src/app/api/__tests__/dramas-search.test.ts` | 修改 | 增补 tags 搜索命中场景，验证分类标签点击链路可达 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 增加分类标签输出校验与异常包装测试 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 增加 `all` 合并、固定三维度、空维度保留、tags 搜索命中测试 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 补充 tags 查询表达式与分类标签 contract 测试 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加 classification query / response schema 测试 |

> 注：当前阶段只输出设计文档，不直接修改实现文件。

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/dramas/tags/route.ts` | `GET` | `/api/dramas/tags` | `withErrorHandler` + Route 内 Zod query 校验 | 返回分类页固定三维度标签结构，匿名可访问 |
| `backend/src/app/api/dramas/search/route.ts` | `GET` | `/api/dramas/search` | `withErrorHandler` + Route 内 Zod query 校验 | 保持原有搜索路由与分页 contract，仅扩展匹配范围 |
| `backend/src/app/api/dramas/hot-search/route.ts` | `GET` | `/api/dramas/hot-search` | `withErrorHandler` | 现有热搜接口，不变 |
| `backend/src/app/api/dramas/rankings/route.ts` | `GET` | `/api/dramas/rankings` | `withErrorHandler` + Route 内 Zod query 校验 | 现有排行接口，不变 |

### 3.2 路由分组策略

- 分类标签接口属于 `dramas` 资源域的辅助查询视图，因此放在 `app/api/dramas/tags/route.ts`，而非顶层 `/api/tags`。
- 保持 RESTful 语义：`/api/dramas/tags` 表达“围绕 drama 内容域暴露标签浏览数据”。
- 搜索仍走 `GET /api/dramas/search`，不引入新的分类搜索接口，避免 Android / iOS 路由和 API 语义分叉。

### 3.3 参数校验

```typescript
import { z } from 'zod';

export const ClassificationGenderSchema = z.enum(['all', 'male', 'female']);

export const ClassificationTagsQuerySchema = z.object({
  gender: ClassificationGenderSchema.default('all'),
});

export const ClassificationDimensionSchema = z.object({
  key: z.enum(['era_background', 'theme_plot', 'character_setting']),
  name: z.string().min(1),
  tags: z.array(z.string().trim().min(1)).default([]),
});

export const ClassificationTagsResponseSchema = z.object({
  data: z.object({
    gender: ClassificationGenderSchema,
    dimensions: z.array(ClassificationDimensionSchema).length(3),
  }),
});
```

参数约束结论：

| 参数 | 规则 | 说明 |
|------|------|------|
| `gender` | `all | male | female` | 默认 `all`，非法值返回 `400 + VALIDATION_ERROR` |
| `q` | `trim().min(1).max(50)` | 保持既有搜索 query 规则不变 |
| `page` | `int >= 1` | 保持既有分页语义 |
| `pageSize` | `1 <= int <= 100` | 保持既有搜索分页上限 |

### 3.4 响应契约

#### `GET /api/dramas/tags`

成功响应：

```json
{
  "data": {
    "gender": "all",
    "dimensions": [
      {
        "key": "era_background",
        "name": "时代背景",
        "tags": ["都市", "校园", "民国", "古装"]
      },
      {
        "key": "theme_plot",
        "name": "主题情节",
        "tags": ["逆袭", "系统", "闪婚", "甜宠"]
      },
      {
        "key": "character_setting",
        "name": "角色设定",
        "tags": ["大女主", "霸总", "萌宝", "龙王"]
      }
    ]
  }
}
```

#### `GET /api/dramas/search`

成功响应结构保持不变：

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "title": "逆袭归来后我成了豪门团宠",
      "description": "落魄千金重回豪门，在误会与守护中逆风翻盘。",
      "cover_url": "https://example.com/dramas/001.jpg",
      "category": "都市",
      "episode_count": 68,
      "tags": ["逆袭", "豪门"],
      "rating": 8.9,
      "created_at": "2026-07-25T00:00:00Z",
      "updated_at": "2026-07-25T00:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total": 1,
    "total_pages": 1
  }
}
```

#### 错误响应

继续沿用当前统一错误结构：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed"
  }
}
```

### 3.5 分类聚合与搜索匹配规则

| 规则 | Backend 落地方式 |
|------|-----------------|
| 固定三维度 | repository 返回固定顺序的 `era_background -> theme_plot -> character_setting` |
| 空维度不省略 | 即使某维度无标签，也返回该维度对象与 `tags=[]` |
| `all` 去重合并 | 以男频、女频标签按预设顺序合并，保留首次出现顺序 |
| 搜索可命中性 | 分类种子中的标签必须能在至少一个 drama 的 `title/category/tags` 命中 |
| 搜索扩展 | mock 与 supabase 两套 repository 都把 tags 纳入搜索匹配 |
| 超大页码 | 保持既有 200 + `data=[]` + 正确分页信息 |

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
GET /api/dramas/tags
请求
  -> withErrorHandler
  -> 解析 searchParams
  -> ClassificationTagsQuerySchema.parse(...)
  -> DramaService.listClassificationTags(...)
  -> DramaRepository.listClassificationTags(...)
  -> ClassificationTagsResponseSchema.parse(...)
  -> JSON 响应

GET /api/dramas/search
请求
  -> withErrorHandler
  -> SearchDramaQuerySchema.parse(...)
  -> DramaService.searchDramas(...)
  -> DramaRepository.search(...)
  -> DramaListResponseSchema.parse(...)
  -> JSON 响应
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 路由级 | 统一处理 `AppError`、`ZodError`、未知异常 |
| `requireAuth` | 不使用 | 分类标签与搜索均为公开只读接口，无需登录 |
| `withLogger` | 暂不新增 | 当前 API route 未统一挂载，分类接口保持现状 |
| `withCors` | 暂不新增 | 现有 route 未统一使用，本期不额外改造 |
| 限流 middleware | 不新增 | 当前仓库无统一限流链路，且未获准新增依赖 |

### 4.3 错误传播方式

- **参数错误**：Zod 抛错后由 `withErrorHandler` 转成 `400 + VALIDATION_ERROR`。
- **业务错误**：service / repository 抛出 `AppError` 后原样交给 `withErrorHandler`。
- **内部契约错误**：service 对 repository 返回值做 schema 校验失败时，统一转为 `Errors.internal(...)`，避免把服务端问题暴露成客户端参数错误。
- **未知异常**：记录 `console.error` 并返回 `500 + INTERNAL_ERROR`。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `DramaService.listDramas` | 既有首页 Feed 查询 | `PaginationParams` | `PaginatedResult<Drama>` | `DramaRepositoryInterface` |
| `DramaService.searchDramas` | 搜索查询 | `SearchDramasParams` | `PaginatedResult<Drama>` | `DramaRepositoryInterface` |
| `DramaService.listClassificationTags` | 分类标签聚合查询 | `ClassificationTagsQuery` | `ClassificationTagsResult` | `DramaRepositoryInterface` |
| `DramaService.listHotSearches` | 既有热搜查询 | 无 | `HotSearchListResponse` | `DramaRepositoryInterface` |
| `DramaService.listRankings` | 既有排行查询 | `RankingParams` | `PaginatedResult<RankingDrama>` | `DramaRepositoryInterface` |
| `DramaService.bookDrama` | 既有预约提交 | `BookDramaParams` | `BookDramaResponse` | `DramaRepositoryInterface` |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| 分类标签查询 | 不涉及事务 | 纯只读查询，无回滚 |
| 搜索查询 | 不涉及事务 | 纯只读查询，无回滚 |
| 热搜 / 排行 / 预约 | 维持现有实现 | 不属于本 PRD 新增边界 |

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| 参数校验异常 | `gender` / `q` / `page` / `pageSize` 非法 | 400 | `VALIDATION_ERROR` |
| 数据源不可用 | repository 无法访问 Supabase 或种子装载失败 | 503 / 500 | `SERVICE_UNAVAILABLE` / `INTERNAL_ERROR` |
| 内部输出不合法 | repository 返回结构不满足 schema | 500 | `INTERNAL_ERROR` |
| 未知异常 | service / repository 运行时异常 | 500 | `INTERNAL_ERROR` |

### 5.4 Service 方法设计

#### `listClassificationTags(params)`

职责：
- 接收 route 已校验的 `gender`；
- 调用 repository 读取固定三维度标签结构；
- 使用 `ClassificationTagsResponseSchema` 校验输出；
- 保证首版返回结构长度恒为 3；
- 不在 service 层做 UI 相关补空逻辑，contract 由 repository 直接保证完整。

#### `searchDramas(params)`

职责：
- 保持现有搜索 route / response / pagination 行为不变；
- 调用扩展后的 repository 搜索实现；
- 将 tags 匹配能力对客户端完全透明化；
- 对输出继续使用 `DramaListResponseSchema` 做守卫校验。

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| 无强制新增表 | 不变 | 首版分类标签采用 repository 内逻辑种子，不阻塞当前 PRD |
| `dramas`（逻辑搜索索引） | 逻辑扩展 | 搜索实现需覆盖 tags 维度；若真实存储缺 tags 列，则以兼容方案承接 |

### 6.2 当前阶段结论

- **本期不要求为了分类标签种子本身新增 migration**。shared design 已允许首版以内存 / 常量形式维护分类种子。
- **但若 coding 阶段要在 Supabase 路径真实满足 `title + category + tags` 搜索闭环，就必须补齐可查询的 tags 存储结构，并通过新增 migration 演进**；不能修改已有 `00000000000001_init_tables.sql`。
- 因此本期 coding 的落地优先级应明确为：
  1. 先保证 mock/runtime 路径与自动化测试闭环；
  2. 若当前 Supabase 表结构尚无 tags 能力，则新增 migration（如为 `dramas` 增加合适的 tags 列或等价索引结构）后再扩展 repository 查询；
  3. 若当轮不落 Supabase migration，则必须在实现与测试中显式标注 Supabase 路径暂不承诺 tags 命中，不能在文档里写成已完整支持。
- 若后续要把分类标签改为运营可配置，可再新增 migration，例如：
  - `classification_tag_groups`
  - `classification_tags`
  - 或在 `dramas` 表补齐更适合检索的 tags 结构

### 6.3 Supabase 兼容策略

| 现状 | 兼容策略 |
|------|---------|
| 当前 `dramas` 映射中 `tags` 仍为空数组 | 分类接口先使用仓库内种子，不强依赖真实 tags 列；这只解决分类接口本身，不自动解决 Supabase 搜索 tags 命中 |
| 现有 Supabase 搜索只查 `title/category` | coding 阶段若表已具备 tags 列，则扩展 `.or(...)`；若未具备，则必须先新增 migration 补齐查询能力，否则 Supabase 路径不能宣称满足 PRD-06 tags 搜索要求 |
| 当前分类页只需稳定 contract | 优先保证 mock 路径与测试闭环；Supabase 路径需按当轮实际落地情况在代码与测试中如实声明 |

---

## 7. 后台任务/队列设计

本 PRD 不新增后台任务、Redis 队列、异步聚合或定时同步流程。

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| 无 | — | — | — | — | — |

原因：
- 分类标签首版由静态 / mock 种子直接提供；
- 搜索能力只是字段匹配扩展，不涉及离线索引构建；
- 当前仓库尚无针对该能力的 Redis 队列基础设施落地需求。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| Supabase URL | `SUPABASE_URL` | 通过环境注入 | 通过环境注入 | 现有仓库约定 |
| Supabase anon key | `SUPABASE_ANON_KEY` | 通过环境注入 | 通过环境注入 | 现有仓库约定 |
| Supabase service key | `SUPABASE_SERVICE_ROLE_KEY` | 通过环境注入 | 通过环境注入 | 现有仓库约定 |
| Redis URL | `REDIS_URL` | 可选 | 可选 | 本 PRD 不直接使用 |
| API 端口 | `PORT` | 由 config 默认 | 由环境注入 | 本 PRD 不新增新配置 |

> ⚠️ 本期不新增分类专属环境变量；禁止硬编码标签接口地址、数据库连接、token 或环境常量。

---

## 9. API 调用清单（调用外部服务）

本 PRD Backend 不新增外部第三方服务调用。所有请求仍然是：
- 本地 mock repository 路径；或
- 通过 `getSupabaseAdmin()` 访问 Supabase。

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| Supabase | `dramas` / `bookings` 表查询 | 搜索 / 排行 / 预约 / 未来分类聚合 | 沿用现有客户端默认 | 失败时抛 `AppError` 并由 `withErrorHandler` 转为统一错误响应 |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| 默认加载 | 默认请求 `gender=all` | `ClassificationTagsQuerySchema.default('all')` 在 route 层收敛 |
| 固定三维度 | 始终返回三个固定分组 | repository 内维护 canonical 维度顺序，schema 用 `.length(3)` 守卫 |
| 空维度展示 | `tags=[]` 也不省略维度 | repository 直接返回空数组，不允许裁剪掉分组 |
| `all` 合并规则 | 男频 / 女频固定顺序去重合并 | repository 提供 deterministic merge 工具函数 |
| 搜索可命中性 | 标签必须至少命中一个 drama | 种子数据设计与测试共同约束；mock 测试需覆盖标签点搜可命中 |
| 标签点击复用搜索 | 搜索接口不变，仅扩展匹配能力 | `search()` 同时匹配 `title/category/tags` |
| 并发保护 | 端侧只消费最后一次结果 | Backend 无需特别处理，继续返回确定性响应 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | `withErrorHandler` + Zod parse | 负责 query 解析与统一错误出口 |
| Service | schema parse + `Errors.internal()` 包装 | 防止 repository 非法结果直接泄露 |
| Repository | 抛 `Errors.*` | 对 Supabase / mock 内部异常做领域化表达 |
| 日志 | `console.error`（现状） | 由 `withErrorHandler` 统一记录未知错误 |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `VALIDATION_ERROR` | 400 | `gender` / `q` / 分页参数非法 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `SERVICE_UNAVAILABLE` | 503 | 数据源暂不可用 | `{ "error": { "code": "SERVICE_UNAVAILABLE", "message": "..." } }` |
| `INTERNAL_ERROR` | 500 | 内部聚合、映射、schema 校验失败 | `{ "error": { "code": "INTERNAL_ERROR", "message": "..." } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 缺省 `gender` | 未传 query | 自动视为 `all` | 减少端侧分支 |
| 非法 `gender` | `gender=unknown` | 400 + `VALIDATION_ERROR` | 由 Zod 统一拦截 |
| 某维度无标签 | 单个维度为空 | 200 + 保留维度对象 + `tags=[]` | 保证左右锚点稳定 |
| 当前性别无任何标签 | 三个维度全空 | 200 + 三维度全空数组 | 结构完整优先于内容非空 |
| 标签搜索无结果 | query 合法但未命中 | 200 + `data=[]` | 由搜索结果页空态承接 |
| tags 搜索命中 | query 命中 drama.tags | 200 + 正常结果 | 是 PRD-06 关键新增能力 |
| Supabase 无 tags 支持 | 真实表结构未补齐 | 需在 coding 阶段采用兼容实现并以测试说明限制 | 不能静默与 spec 背离 |

### 11.4 错误日志与监控

- 首版继续使用本地日志与自动化测试作为主要验证手段；
- 不新增埋点、APM 或告警依赖；
- 若后续切入真实内容源，建议增加 `gender` / `query` 维度的请求日志，便于排查分类标签与搜索索引不一致问题。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| 单元测试 | `DramaService.listClassificationTags`、`searchDramas` tags 扩展逻辑 | Vitest |
| Route 集成测试 | `/api/dramas/tags` 的 query 校验、成功与失败响应 | Vitest + Next route 测试模式 |
| Repository 测试 | mock / supabase repository 的固定三维度、all 去重、tags 搜索匹配 | Vitest |
| Schema 测试 | classification query / response schema | Vitest |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| BE-T01 | 默认 `gender=all` | 不传 gender | 返回 3 个固定维度，`data.gender=all` | Route |
| BE-T02 | 男频标签集 | `gender=male` | 返回 3 个维度，标签内容符合男频种子 | Repository / Route |
| BE-T03 | 女频标签集 | `gender=female` | 返回 3 个维度，标签内容符合女频种子 | Repository / Route |
| BE-T04 | `all` 去重合并 | male/female 有重复标签 | 返回去重后的稳定顺序 | Repository |
| BE-T05 | 空维度保留 | 某维度 tags 为空 | 仍返回该维度对象与 `tags=[]` | Repository / Service |
| BE-T06 | 非法 gender | `gender=other` | 400 + `VALIDATION_ERROR` | Route |
| BE-T07 | tags 搜索命中 | `q=萌宝` | 返回至少 1 条包含该标签的 drama | Repository / Route |
| BE-T08 | title/category 兼容命中 | `q=都市` | 保持既有匹配语义成立 | Repository |
| BE-T09 | 搜索无结果 | `q=不存在的标签词` | 200 + `data=[]` | Route |
| BE-T10 | repository 返回非法结构 | 构造坏数据 | service 转成 `INTERNAL_ERROR` | Service |

### 12.3 不在本期测试范围

- 真实 Supabase 线上数据回填与运营配置后台；
- Redis 缓存命中；
- 复杂搜索排序优化；
- 设备级黑盒体验验证（将在 workflow 后续 QA 阶段统一记录）。

---

## 13. 参考资料

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-27-prd-06-classification/spec.md` | PRD-06 功能边界、固定三维度、标签点击复用搜索结果页 |
| `docs/specs/2026-07-27-prd-06-classification/design.md` | shared contract、状态机、错误语义、跨端共享逻辑 |
| `backend/src/lib/schemas.ts` | 当前 Search / Ranking schema 与新增 classification schema 的落点 |
| `backend/src/app/api/dramas/search/route.ts` | 现有搜索 route 样板 |
| `backend/src/app/api/dramas/hot-search/route.ts` | 轻量只读接口样板 |
| `backend/src/services/drama/drama.service.ts` | 现有 service 编排方式与 schema 防御模式 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 仓库接口扩展位置 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 当前 mock 搜索只匹配 `title + category` 的改造点 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 当前 Supabase 搜索只匹配 `title + category` 的改造点 |
