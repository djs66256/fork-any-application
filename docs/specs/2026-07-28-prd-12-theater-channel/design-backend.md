# Backend 端技术方案：PRD-12 剧场频道

> 创建日期：2026-07-28
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Backend 端在现有 `dramas` 资源域之上新增剧场频道只读查询能力，继续遵守当前仓库已落地的四层结构：Route → Service → Repository → Infrastructure / Shared。首版保持与当前代码现状一致，以 `DramaMockRepository` 作为默认运行路径，不新增第三方依赖，不引入新的数据库表或队列；剧场 Feed 数据由固定种子和确定性顺序切片产生。

```text
GET /api/dramas/channel
  -> Route Handler (`app/api/dramas/channel/route.ts`)
     -> withErrorHandler
     -> Zod Query 校验（channel / page / pageSize）
     -> DramaService.listTheaterFeed(params)
        -> DramaRepositoryInterface.listTheaterFeed(params)
           -> Mock: 复用现有 drama/ranking seed + 固定 theater seed 顺序映射
           -> Supabase: 本期不要求实现；如已有仓储需补齐接口占位或兼容实现
        -> TheaterFeedResponseSchema.parse(result)
     -> 返回 { data, pagination }
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/dramas/route.ts` | 不变 | 继续服务首页 Feed，不被剧场频道接口覆盖 |
| `backend/src/app/api/dramas/rankings/route.ts` | 不变 | 排行接口继续提供默认榜单与预约榜，不被剧场接口替代 |
| `backend/src/app/api/dramas/channel/route.ts` | 新增 | 承载剧场频道 Feed 查询 |
| `backend/src/services/drama/drama.service.ts` | 扩展 | 增加剧场频道查询能力，维持 drama 资源域集中编排 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 扩展 | 新增剧场频道 query / result 契约 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 扩展 | 增加剧场固定 seed、频道分流、稳定排序和分页逻辑 |
| `backend/src/lib/schemas.ts` | 扩展 | 新增剧场频道 query / response schema |
| `backend/src/middleware/error-handler.ts` | 不变 | 继续作为统一错误出口 |
| `backend/src/lib/errors.ts` | 不变 | 复用现有错误码集合，不新增专属错误码 |
| `backend/src/repositories/supabase/*` | 可选扩展 | 若存在对应 drama 仓储实现，仅补齐接口签名或显式 not implemented；本期交付不依赖真实数据源 |

### 1.2 与 shared design / 代码现状的兼容说明

1. **成功响应 contract 兼容现有代码**  
   当前 Backend 真实实现与已有测试均采用“成功直接返回资源体、失败返回 `{ error: { code, message } }`”模式，例如 `GET /api/dramas`、`GET /api/dramas/rankings`。因此本期 `GET /api/dramas/channel` 继续沿用：
   - 成功返回 `{ data, pagination }`
   - 失败由 `withErrorHandler` 返回 `{ error: { code, message } }`

2. **数据结构复用现有 `DramaSchema` 风格**  
   现有 `DramaSchema` 已承载标题、封面、分类、标签、评分、时间戳等通用字段；剧场卡片只在此基础上新增 `heat`，而不是重新发明一套只给剧场使用的字段集。

3. **排序规则以 mock seed 为权威顺序源**  
   spec 已明确 `channel=all` 需要确定性排序。当前代码中最稳定、最可控的数据源是 `DramaMockRepository` 内的固定 ranking/drama seed，因此 Backend 方案要求：
   - coding 阶段在 mock repository 内声明单独的 theater seed 顺序；
   - 所有分页都基于同一份顺序切片；
   - 不以 `created_at` 或动态字段做临时排序，避免顺序漂移。

4. **非 `all` 频道首版显式返回空结果**  
   首版不伪造真人 / 动漫 / 电影等频道内容，而是通过合法空列表让移动端展示空态，减少未来真实运营数据接入时的兼容负担。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/dramas/channel/route.ts` | 新增 | 新增 `GET /api/dramas/channel` 路由，解析 query 后通过 `getDramaRepository()` 创建 `DramaService` 并调用 `listTheaterFeed` |
| `backend/src/services/drama/drama.service.ts` | 修改 | 增加剧场频道列表编排与 schema 校验逻辑 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 新增 `TheaterFeedParams`、`TheaterDrama`、`listTheaterFeed` 等契约 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 新增剧场 seed、`heat` 字段映射、频道分流、稳定分页 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 `TheaterChannelSchema`、`TheaterFeedQuerySchema`、`TheaterDramaSchema`、`TheaterFeedResponseSchema` |
| `backend/src/app/api/__tests__/dramas-channel.test.ts` | 新增 | 覆盖成功、非法参数、空频道、超大页码、内部错误，以及基于 repository registry 的 route 注入测试 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 增加剧场 Feed schema parse 和错误包装测试 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 增加稳定顺序、分页、空频道、`heat` 数值语义测试 |

> 本期仅撰写方案，不修改上述实现文件；表中列出 coding 阶段需要落地的核心改动面。

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/dramas/channel/route.ts` | `GET` | `/api/dramas/channel` | `withErrorHandler` + Route 内 Zod query 校验 | 返回剧场频道分页 Feed，匿名可访问 |
| `backend/src/app/api/dramas/route.ts` | `GET` | `/api/dramas` | `withErrorHandler` + Zod query 校验 | 现有首页 Feed，保持不变 |
| `backend/src/app/api/dramas/rankings/route.ts` | `GET` | `/api/dramas/rankings` | `withErrorHandler` + Zod query 校验 | 现有排行接口，保持不变 |
| `backend/src/app/api/dramas/tags/route.ts` | `GET` | `/api/dramas/tags` | `withErrorHandler` + Zod query 校验 | 现有分类接口，保持不变 |

### 3.2 路由分组策略

- 剧场频道仍属于 `dramas` 资源域中的一种“内容分发视图”，因此接口放在 `app/api/dramas/channel/` 下，而不是新增 `/api/theater` 顶级资源。
- `channel` 作为 query 参数，而非 path segment，可与现有分页 query 组合，并与移动端子频道切换状态天然对应。
- Route 层只负责：
  - 读取 query
  - 使用 Zod 校验
  - 通过既有 repository registry 获取 repository，并创建 service
  - 返回 `NextResponse.json(result)`
- 业务上的“固定顺序”“空频道返回 200 + []”“heat 为数值”全部在 service / repository 层保证，不让 route 层承担业务分支。
- 为与现有可测试性方向收口，新增 `channel` route 设计应优先复用 `repository-registry.ts` 的 `getDramaRepository()`，而不是在 route 中直接 `new DramaMockRepository()`；这样既与仓库已有注入点一致，也便于 route tests / 未来数据源切换。

### 3.3 参数校验

```typescript
import { z } from 'zod';

export const TheaterChannelSchema = z.enum([
  'all',
  'real',
  'anime',
  'movie',
  'audio',
  'novel',
  'comic',
  'bigscreen',
]);

export const TheaterFeedQuerySchema = z.object({
  channel: TheaterChannelSchema.default('all'),
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(20),
});
```

参数约束结论：

| 参数 | 规则 | 说明 |
|------|------|------|
| `channel` | `all \| real \| anime \| movie \| audio \| novel \| comic \| bigscreen` | 剧场子频道，默认 `all` |
| `page` | `int >= 1` | 与现有 dramas 列表接口保持一致 |
| `pageSize` | `1 <= int <= 100` | 默认 20，最大 100 |
| Request Body | 无 | 首版只读接口 |

### 3.4 响应契约

成功响应：

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
      "updated_at": "2026-07-25T00:00:00Z",
      "heat": 98210
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 12,
    "total_pages": 1
  }
}
```

错误响应：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed"
  }
}
```

### 3.5 频道分流与分页规则

| 场景 | 规则 | Backend 落地方式 |
|------|------|-----------------|
| `channel=all` | 返回真实内容 | 通过固定 theater seed 顺序切片 |
| 非 `all` 频道 | 返回空列表 | 统一返回 `data=[]` + 合法 pagination |
| 默认分页 | `page=1&pageSize=20` | query schema 默认值 |
| 超大页码 | 合法但超范围 | 返回 `200 + data=[]` |
| 稳定顺序 | 所有页共享同一顺序源 | 先构造完整有序数组，再统一 paginate |
| `heat` 语义 | 原始整数值 | 由 repository 在数据映射阶段填充，不格式化成字符串 |

### 3.6 Zod Schema 与 design.md 对齐说明

```typescript
export const TheaterDramaSchema = DramaSchema.extend({
  heat: z.number().int().min(0),
});

export const TheaterFeedResponseSchema = z.object({
  data: z.array(TheaterDramaSchema),
  pagination: PaginationSchema,
});
```

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
请求
  -> [withErrorHandler]
  -> [Route 内 query parse]
  -> [getDramaRepository()]
  -> DramaService.listTheaterFeed
  -> DramaRepository.listTheaterFeed
  -> NextResponse.json({ data, pagination })
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 路由级 | 统一处理 `AppError`、`ZodError` 和未知异常 |
| CORS / logger 等 Next.js 全局机制 | 全局 | 复用现有工程配置，本期不新增剧场专属 middleware |

### 4.3 错误传播方式

- query 参数异常由 `TheaterFeedQuerySchema.parse()` 抛出 `ZodError`，最终由 `withErrorHandler` 转为 400 + `VALIDATION_ERROR`。
- repository / service 映射结果不合法时，由 service 捕获并转成 `Errors.internal('Invalid theater feed result')`。
- 本期不新增专属业务异常枚举，避免为单一只读接口引入新的错误码复杂度。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `DramaService.listTheaterFeed` | 编排剧场频道查询、执行 schema parse、统一错误包装 | `TheaterFeedParams` | `PaginatedResult<TheaterDrama>` | `DramaRepositoryInterface` |
| `DramaService.listDramas` | 现有首页列表能力 | `PaginationParams` | `PaginatedResult<Drama>` | `DramaRepositoryInterface` |
| `DramaService.listRankings` | 现有排行能力 | `RankingParams` | `PaginatedResult<RankingDrama>` | `DramaRepositoryInterface` |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| `listTheaterFeed` 只读查询 | 无事务 | 不适用 |

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| `ZodError` | query 参数非法 | 400 | `VALIDATION_ERROR` |
| `AppError(INTERNAL_ERROR)` | repository 返回结果不满足 schema | 500 | `INTERNAL_ERROR` |
| `AppError(SERVICE_UNAVAILABLE)` | 预留给未来真实数据源不可用 | 503 | `SERVICE_UNAVAILABLE` |

### 5.4 关键实现草图

```typescript
async listTheaterFeed(params: TheaterFeedParams): Promise<PaginatedResult<TheaterDrama>> {
  try {
    return TheaterFeedResponseSchema.parse(await this.dramaRepository.listTheaterFeed(params));
  } catch (error) {
    if (isAppError(error)) {
      throw error;
    }
    throw Errors.internal('Invalid theater feed result');
  }
}
```

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| 无 | 无 | 本期首版不要求真实数据库变更 |

### 6.2 DDL

```sql
-- 本期无 migration。
-- 首版 theater feed 使用 mock seed / in-memory 数据源。
```

### 6.3 字段详情

| 表名 | 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| 无 | — | — | — | — | 本期不新增真实表字段 |

### 6.4 索引策略

| 表名 | 索引名 | 类型（UNIQUE/INDEX） | 字段 | 用途 |
|------|--------|---------------------|------|------|
| 无 | — | — | — | 本期不涉及 |

### 6.5 回滚策略

- 无 migration，则无回滚步骤。
- 若后续从 mock seed 演进到 Supabase 查询，应单独新增 migration 文件，不修改本期已完成代码和已执行 migration。

---

## 7. 后台任务/队列设计

### 7.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| 无 | — | — | — | — | — |

### 7.2 任务生命周期

```text
本期无后台任务 / 队列。
```

### 7.3 失败处理与死信队列

- 剧场频道首版为同步只读查询，不需要异步任务、消息队列或死信处理。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| Supabase URL | `SUPABASE_URL` | 现有配置 | 现有配置 | 未来真实数据源接入时复用 |
| Supabase anon key | `SUPABASE_ANON_KEY` | 现有配置 | 现有配置 | 未来真实数据源接入时复用 |
| Redis URL | `REDIS_URL` | 现有配置 | 现有配置 | 本期不使用 |

> ⚠️ 禁止硬编码任何常量。所有环境相关配置继续通过现有 `config` 模块或既有工程配置获取。本期剧场接口本身不新增环境变量。

---

## 9. API 调用清单（调用外部服务）

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| 无 | — | — | — | — |

- 本期剧场频道接口不调用第三方或下游服务。
- 数据直接来自本地 mock repository / seed。

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| 默认子频道 | 首次固定 `channel=all` | `TheaterFeedQuerySchema.default('all')` |
| 默认分页 | `page=1&pageSize=20` | query schema 默认值 |
| 空态策略 | 非 `all` 频道返回空列表 | repository 分支直接返回 `paginate([], params)` |
| 稳定顺序 | `channel=all` 所有页共用固定顺序 | repository 先生成完整有序数组，再统一切片 |
| `heat` 数值语义 | 服务端返回原始整数 | 在 repository 映射阶段给出 `heat: number` |
| 路由复用 | 搜索/分类/排行/新剧继续复用现有能力 | Backend 无需额外改动；维持既有 search / rankings / tags 接口不变 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | `withErrorHandler` | 统一输出错误体 |
| Query parse | `TheaterFeedQuerySchema.parse()` | 非法 query 直接抛 ZodError |
| Service | schema parse + `Errors.internal(...)` | 防止 repository 返回脏结构 |
| Repository | 固定 seed + 安全切片 | 空频道、超大页码都返回合法结果而不是异常 |
| 日志 | `console.error`（未知异常） | 沿用当前工程默认行为 |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `VALIDATION_ERROR` | 400 | `channel` / `page` / `pageSize` 非法 | `{ "error": { "code": "VALIDATION_ERROR", "message": "Validation failed" } }` |
| `INTERNAL_ERROR` | 500 | service / repository 结构不合法 | `{ "error": { "code": "INTERNAL_ERROR", "message": "Internal server error" } }` |
| `SERVICE_UNAVAILABLE` | 503 | 预留给未来真实数据源不可用 | `{ "error": { "code": "SERVICE_UNAVAILABLE", "message": "Service unavailable: theater-feed" } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 非法频道值 | `channel=unknown` | 返回 400 | 使用枚举白名单校验 |
| 缺省频道 | 不传 `channel` | 等价 `all` | 减少端侧分支 |
| 非 `all` 频道首版无数据 | `real/anime/...` | 返回 200 + 空数组 | 由客户端展示空态 |
| 超大页码 | `page` 合法但超出总页数 | 返回 200 + 空数组 | 与现有分页语义一致 |
| `pageSize` 超过上限 | `pageSize=1000` | 返回 400 | 保护接口与端侧渲染 |
| 剧场 seed 含重复 ID | 数据配置错误 | 测试失败 / schema 级校验或仓储断言 | coding 阶段需测试保证唯一性 |
| `heat` 缺失或负值 | 映射错误 | service schema parse 失败 -> 500 | 防止客户端收到脏字段 |

### 11.4 错误日志与监控

- 首版以自动化测试与本地日志为主，不新增专属埋点或监控平台接入。
- 后续若接入真实数据源，可按 `channel`、`page`、`pageSize` 记录请求日志，并对持续 5xx 或数据为空异常峰值做告警。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| 单元测试 | `DramaService.listTheaterFeed` 的 schema parse / error wrapping | vitest |
| Repository 测试 | mock repository 的稳定顺序、分页、空频道、`heat` 语义 | vitest |
| 集成测试 | `/api/dramas/channel` 的 query 校验与 JSON 响应 | Next.js route tests / vitest |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| BE-01 | 默认请求返回 `all` 第一页 | 无 query | 200，`data.length > 0`，`page=1`，`page_size=20` | 集成 |
| BE-02 | 非 `all` 频道返回空结果 | `channel=real` | 200，`data=[]`，`pagination.total=0` | 集成 |
| BE-03 | 非法频道值被拒绝 | `channel=foo` | 400，`VALIDATION_ERROR` | 集成 |
| BE-04 | 超大页码返回空列表 | `channel=all&page=999` | 200，`data=[]` | Repository / 集成 |
| BE-05 | 排序稳定 | 连续请求 `page=1/2` | 两页无重复，顺序固定 | Repository |
| BE-06 | `heat` 为整数 | `channel=all` | 所有项 `heat` 均为 `int >= 0` | Repository / Service |
| BE-07 | 仓储返回脏数据时 service 包装为 internal error | mock invalid result | 抛 `INTERNAL_ERROR` | 单元 |

### 12.3 不做项

- 本期不做真实 Supabase / Redis 集成测试。
- 本期不做性能压测；只要求保证 mock 环境下接口行为稳定、可预测、可自动化验证。

---
