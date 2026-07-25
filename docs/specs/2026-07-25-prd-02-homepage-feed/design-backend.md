# Backend 端技术方案：PRD-02 首页信息流

> 创建日期：2026-07-25
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Backend 端不新增资源路由，不引入数据库 migration，也不接入真实推荐服务；仅在现有 `GET /api/dramas` 骨架上补齐**首页 Feed canonical contract**、**首页可用 mock 数据**、**分页校验**和**自动化测试**。整体仍严格遵循当前四层结构：Route → Service → Repository → Shared。

```text
GET /api/dramas
  -> Route Handler (`app/api/dramas/route.ts`)
     -> Zod Query 校验（page / pageSize）
     -> DramaService.listDramas(params)
        -> DramaMockRepository.findMany(params)
           -> 预置首页 mock dramas 数据集
           -> slice 分页
     -> 返回 { data, pagination }
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/dramas/route.ts` | 修改 | 继续作为唯一列表入口，保持 `/api/dramas` 路径不变，收口 query 与返回字段契约 |
| `backend/src/lib/schemas.ts` | 修改 | 将首页卡片字段集统一到 `episode_count`、`tags` 等需求定义，保证响应校验与共享方案一致 |
| `backend/src/services/drama/drama.service.ts` | 轻微扩展 | 继续保持轻量委托，可增加首页列表字段一致性保障但不引入复杂业务逻辑 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 从空仓库演进为内置 10~20 条 mock 数据，并提供稳定分页结果 |
| `backend/src/app/api/__tests__/dramas.test.ts` | 修改 | 从“空列表骨架验证”扩展为首页列表、分页、参数边界、多页结果验证 |
| `backend/src/app/api/dramas/[id]/route.ts` | 不变 | 详情接口仍维持 501，占位状态，不纳入本期实现 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/dramas/route.ts` | 修改 | 保持 `/api/dramas` 路径与 `page/pageSize` query，明确默认值和参数边界 |
| `backend/src/lib/schemas.ts` | 修改 | 统一 `DramaSchema` 为首页卡片字段集，补充 `tags` 并将 `total_episodes` 收口为 `episode_count` |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 预置稳定 mock 数据集，支持第一页与多页分页切片 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 保持薄 Service，作为 Route 与 Repository 间的稳定编排层 |
| `backend/src/app/api/__tests__/dramas.test.ts` | 修改 | 增加成功态、空态、默认分页、边界页码、非法参数测试 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 不变 | 继续复用现有 `PaginatedResult<T>` 与 `PaginationParams` 抽象 |

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/dramas/route.ts` | `GET` | `/api/dramas` | `withErrorHandler` + Zod query 校验 | 首页 Feed 唯一列表接口 |
| `backend/src/app/api/dramas/route.ts` | `POST` | `/api/dramas` | `withErrorHandler` | 保持 501，不在本期实现 |
| `backend/src/app/api/dramas/[id]/route.ts` | `GET` | `/api/dramas/[id]` | `withErrorHandler` | 保持 501，不在本期实现 |

### 3.2 路由分组策略

- 继续按资源维度分组在 `app/api/dramas/` 下，不引入 `/api/v1` 新前缀。
- `/api/dramas` 作为首页 Feed 的 canonical contract，不新增并行别名路由。
- 详情接口保持占位，有助于与 PRD-02 首页范围隔离。

### 3.3 参数校验

```typescript
const PaginationQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(10),
});

const DramaSchema = z.object({
  id: z.string().uuid(),
  title: z.string().min(1),
  description: z.string().default(''),
  cover_url: z.string().url().nullable().default(null),
  category: z.string().default(''),
  episode_count: z.number().int().min(0),
  tags: z.array(z.string()).default([]),
  rating: z.number().min(0).max(10).nullable().default(null),
  created_at: z.string(),
  updated_at: z.string(),
});
```

参数约束结论：

| 参数 | 规则 | 说明 |
|------|------|------|
| `page` | `int >= 1` | 本期客户端固定请求第一页，但服务端保留通用分页能力 |
| `pageSize` | `int >= 1 && <= 100` | 默认 10，用于首页首屏 |
| `id` | `uuid` | 响应主键保持 UUID 形态，与当前代码事实一致 |

---

## 4. Middleware 链设计

### 4.1 请求流水线

```text
请求
  -> withErrorHandler
  -> Route 内解析 searchParams
  -> Zod Query 校验
  -> DramaService.listDramas
  -> Repository.findMany
  -> JSON 响应
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 路由级 | 捕获参数校验、服务异常并输出统一错误结构 |

### 4.3 错误传播方式

- Query 参数非法：由 Zod 抛错，交给 `withErrorHandler` 输出 4xx。
- Repository / Service 异常：统一交给 `withErrorHandler` 输出 500。
- 未实现详情接口：继续通过 `Errors.notImplemented` 输出 501。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `DramaService.listDramas` | 编排首页列表读取 | `PaginationParams` | `PaginatedResult<Drama>` | `DramaRepositoryInterface` |
| `DramaService.getDramaById` | 保持未实现 | `id` | `Drama` | `DramaRepositoryInterface` |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| 首页列表查询 | 不涉及事务 | 无，纯内存只读查询 |

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| 参数校验异常 | `page` / `pageSize` 非法 | 400 | `INVALID_PARAMS` |
| 未实现异常 | 访问详情/创建链路 | 501 | `NOT_IMPLEMENTED` |
| 内部异常 | 仓库或序列化异常 | 500 | `INTERNAL_ERROR` |

设计原则：
- Service 层继续保持薄，不在本期引入推荐排序、个性化、缓存、多数据源聚合。
- 字段统一优先在 Shared Schema 与 Mock Repository 层完成，避免 Service 变成映射堆栈。

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| — | 无 | 本期不引入数据库，不新增 migration |

### 6.2 DDL

```sql
-- 无 migration：PRD-02 Backend 仅使用内存 mock repository
```

### 6.3 字段详情

| 表名 | 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| — | — | — | — | — | 无数据库表变更 |

### 6.4 索引策略

| 表名 | 索引名 | 类型（UNIQUE/INDEX） | 字段 | 用途 |
|------|--------|---------------------|------|------|
| — | — | — | — | 无 |

### 6.5 回滚策略

- 由于不涉及 migration，回滚即回退代码中的 mock 数据与 schema 改动。

---

## 7. 后台任务/队列设计

### 7.1 任务清单

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| — | — | — | — | — | — |

### 7.2 任务生命周期

```text
无后台任务：本期首页列表数据为同步内存读取
```

### 7.3 失败处理与死信队列

- 本期不涉及队列、异步任务或死信策略。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| 应用名 | `APP_NAME` | 现有配置 | 现有配置 | 不新增 |
| 监听端口 | `PORT` | 现有配置 | 现有配置 | 不新增 |
| Supabase | `SUPABASE_URL` 等 | 现有配置 | 现有配置 | 本期不使用 |
| Redis | `REDIS_URL` | 现有配置 | 现有配置 | 本期不使用 |

> ⚠️ 不新增任何环境变量；首页 mock 数据直接在代码中维护，不通过环境变量注入结构化业务数据。

---

## 9. API 调用清单（调用外部服务）

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| — | — | — | — | — |

本期 Backend 不调用第三方服务、Supabase 或 Redis。

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| canonical contract | `/api/dramas?page&pageSize` + `{ data, pagination }` | 保持既有 Route 结构，修正 schema 字段集 |
| 首页卡片字段集 | `cover_url` / `episode_count` / `tags` 等 | 在 `DramaSchema` 与 mock 数据中统一提供 |
| 首屏第一页范围 | 客户端只消费第一页 | Backend 仍实现通用分页，但测试重点覆盖第一页与边界页 |
| UUID 主键 | 首页卡片 `id` 使用 UUID | mock 数据使用稳定 UUID，避免示例与 schema 脱节 |
| 页面承载边界 | mall / earn 不纳入 Feed | Backend 不为商城/赚钱新增首页列表专用字段 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | `withErrorHandler` | 统一格式化错误输出 |
| Query 校验 | Zod | 拦截非法分页参数 |
| Repository | 纯内存切片 | 不应抛出业务歧义错误 |
| 日志 | 现有 Next.js / test 日志 | 当前以测试断言为主，不额外引入日志基础设施 |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `INVALID_PARAMS` | 400 | `page` / `pageSize` 参数非法 | `{ "error": { "code": "INVALID_PARAMS", "message": "..." } }` |
| `NOT_IMPLEMENTED` | 501 | 详情接口未实现 | `{ "error": { "code": "NOT_IMPLEMENTED", "message": "..." } }` |
| `INTERNAL_ERROR` | 500 | 服务内部错误 | `{ "error": { "code": "INTERNAL_ERROR", "message": "..." } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 空仓库 | mock 数据为空 | 返回 `data=[]` 与分页元信息 | 保留兼容测试能力 |
| 首页成功态 | 至少存在 1 条数据 | 返回第一页 1~10 条卡片 | 供客户端首屏展示 |
| 多页分页 | 数据量 > `pageSize` | 返回正确 `total_pages` 与切片结果 | 供后续扩展与测试 |
| 大页码 | `page > total_pages` | 返回空数组 + 正确 pagination | 不抛 500 |
| 非法参数 | `page=0`、`pageSize=101` | 返回 400 | 自动化测试覆盖 |
| 缺字段 mock | 单条数据字段缺失 | 在 schema 层阻断 | 避免脏数据透出到客户端 |

### 11.4 错误日志与监控

- 当前阶段以单元测试 / 接口测试验证为主，不新增监控埋点。
- 若 Repository 构造数据不满足 schema，应在测试中第一时间暴露，而不是运行时兜底忽略。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| 接口测试 | `GET /api/dramas` 成功态、默认分页、非法参数 | Vitest + `NextRequest` |
| Repository 行为测试 | mock 数据分页切片与边界页 | 可复用 Vitest 或新增 repository 测试 |
| 回归测试 | `POST /api/dramas`、`GET /api/dramas/[id]` 仍为未实现 | Vitest |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| B-01 | 默认分页 | `/api/dramas` | `page=1`, `page_size=10` | 接口测试 |
| B-02 | 首页成功态第一页 | `/api/dramas?page=1&pageSize=10` | 返回至少 1 条卡片，字段完整 | 接口测试 |
| B-03 | 多页分页 | `/api/dramas?page=2&pageSize=10` | 正确返回第二页切片与 `total_pages>=2` | 接口测试 |
| B-04 | 大页码 | `/api/dramas?page=999&pageSize=10` | 返回空数组，不报错 | 接口测试 |
| B-05 | 非法 page | `/api/dramas?page=0&pageSize=10` | 400 + `INVALID_PARAMS` | 接口测试 |
| B-06 | 非法 pageSize | `/api/dramas?page=1&pageSize=101` | 400 + `INVALID_PARAMS` | 接口测试 |
| B-07 | 未实现 POST | `POST /api/dramas` | 501 + `NOT_IMPLEMENTED` | 回归测试 |

### 12.3 Mock 数据策略

| 项目 | 设计 |
|------|------|
| 数据量 | 10~20 条，至少形成 2 页 |
| 主键 | 使用稳定 UUID 常量 |
| 内容分布 | 覆盖不同分类、封面缺失、空标签、不同评分 |
| 排序 | 使用稳定固定顺序，避免测试抖动 |

---

## 13. 风险与取舍

| 风险 / 取舍 | 说明 | 对应策略 |
|------------|------|---------|
| 不接真实数据源 | 无法验证真实推荐逻辑 | 本期只验证首页主链路与契约稳定性 |
| 字段从 `total_episodes` 收口到 `episode_count` | 需要同步客户端 DTO / Entity 适配 | 在 shared design 与平台 design 中明确迁移 |
| 不兼容 `/api/v1/dramas` | iOS 需主动迁移 | 在 iOS 设计中作为明确改造项处理 |
