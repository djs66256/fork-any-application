# 实现计划：Backend — PRD-04 搜索发现

> 创建日期：2026-07-26
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 在现有 `DramaService + DramaRepository` 四层结构上新增 `GET /api/dramas/search` 与 `GET /api/dramas/hot-search` 两个只读接口，继续复用当前 `DramaSchema` 列表契约、`withErrorHandler` 错误出口与 mock repository 开发路径。

实现顺序遵循轻量 TDD：先补测试场景并让新增断言落位，再按 `schema → repository → service → route` 收口实现，最终跑通接口级回归、全量测试、build 与 lint。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 搜索 query schema 校验合法参数 | `{ q: " 逆袭 ", page: "1", pageSize: "10" }` | 解析成功，输出 `q="逆袭"`、`page=1`、`pageSize=10` | 单元测试 | P0 |
| T-02 | 搜索 query schema 拦截非法参数 | `q="   "`、`q.length=51`、`page=0`、`pageSize=101` | 抛出校验错误，route 最终返回 `400 + VALIDATION_ERROR` | 单元测试 | P0 |
| T-03 | mock repository 按 `title + category` 做大小写不敏感包含匹配 | `q="逆袭"`、`q="都市"`、大小写变体关键词 | 返回命中结果；匹配顺序沿用现有数据顺序 | 单元测试 | P0 |
| T-04 | mock repository 搜索分页边界正确 | `q="剧" page=999 pageSize=10` | 返回 `200` 语义对应的数据结构：`data=[]`，`pagination` 保持正确 | 单元测试 | P0 |
| T-05 | hot search repository 返回 Top 10 以内的稳定结构 | 无输入 | 返回 `{ data: HotSearchItem[] }`，每项含 `rank/keyword/score`，总数 `<= 10` | 单元测试 | P0 |
| T-06 | service 校验 repository 输出并包装内部异常 | repository 返回非法 drama 或非法 hot search 数据 | 抛出 `INTERNAL_ERROR`，不把内部契约问题暴露成 400 | 单元测试 | P0 |
| T-07 | `GET /api/dramas/search` 路由返回 canonical 列表契约 | `GET /api/dramas/search?q=逆袭&page=1&pageSize=10` | `200`，响应为 `DramaListResponse`，字段与 `/api/dramas` 一致 | 路由测试 | P0 |
| T-08 | `GET /api/dramas/search` 路由覆盖空结果与非法参数 | `q=不存在的关键词`、`q=空白`、`page=0` | 空结果返回 `200 + data=[]`；非法参数返回 `400 + VALIDATION_ERROR` | 路由测试 | P0 |
| T-09 | `GET /api/dramas/hot-search` 路由返回热搜列表 | `GET /api/dramas/hot-search` | `200`，响应为 `{ data: HotSearchItem[] }`，长度 `<= 10` | 路由测试 | P0 |
| T-10 | 搜索与热搜路由处理内部异常 | repository / service 主动抛错 | 返回 `500 + INTERNAL_ERROR` | 路由测试 | P0 |

## 实现步骤

### Step 1：先补测试，锁定搜索与热搜的行为边界

- **关联测试**：T-01、T-02、T-03、T-04、T-05、T-06、T-07、T-08、T-09、T-10
- **目标文件**：`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/repositories/__tests__/drama.mock.repository.test.ts`、`backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`、`backend/src/services/drama/drama.service.test.ts`、`backend/src/app/api/__tests__/dramas-search.test.ts`、`backend/src/app/api/__tests__/dramas-hot-search.test.ts`
- **实现内容**：
  1. 先新增搜索与热搜相关测试文件，覆盖 query 校验、title/category 匹配、大小写不敏感、空结果、大页码、热搜数量边界、内部异常等场景。
  2. 在 service 测试中补齐 `searchDramas`、`listHotSearches` 的成功路径和内部契约错误路径，锁定 `INTERNAL_ERROR` 包装语义。
  3. 在 route 测试中以 `NextRequest` 驱动 `/api/dramas/search` 与 `/api/dramas/hot-search`，提前固定成功响应、400 参数错误、500 内部错误三类行为。
  4. 保持现有 `/api/dramas` 测试不回退，只新增本期路由测试，不改写旧列表接口的验收语义。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-search.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-hot-search.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加搜索 query schema 与 hot search response schema 校验测试 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 增加搜索匹配、分页边界、热搜返回测试 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 增加 Supabase repository 搜索与热搜契约测试 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 增加 `searchDramas` / `listHotSearches` 的成功与异常测试 |
| `backend/src/app/api/__tests__/dramas-search.test.ts` | 新增 | 搜索路由成功、空结果、参数非法、内部错误测试 |
| `backend/src/app/api/__tests__/dramas-hot-search.test.ts` | 新增 | 热搜路由成功与内部错误测试 |

### Step 2：补齐 shared schema 与 repository interface，先把契约收口

- **关联测试**：T-01、T-02、T-05
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/repositories/interfaces/drama.repository.interface.ts`
- **实现内容**：
  1. 在 `schemas.ts` 中新增 `SearchDramaQuerySchema`、`HotSearchItemSchema`、`HotSearchListResponseSchema`，并保持搜索结果继续复用现有 `DramaSchema` / `DramaListResponseSchema`。
  2. 在 repository interface 中新增 `SearchDramasParams`、`HotSearchItem` 类型，以及 `search(...)`、`listHotSearches()` 两个接口方法，确保 mock 与 supabase 实现共享同一契约。
  3. 保持搜索历史相关 schema 继续留在移动端语义范围内，不在 Backend 新增 history API 或 history schema。
  4. 不新增环境变量与 config 字段，遵循 design 中“首版无需额外配置”的约束。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增搜索 query 与 hot search 响应 schema |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 扩展搜索参数类型、热搜类型与 repository 新方法 |

### Step 3：实现 repository，先闭合数据源与分页/匹配规则

- **关联测试**：T-03、T-04、T-05
- **目标文件**：`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/repositories/supabase/drama.supabase.repository.ts`
- **实现内容**：
  1. 在 `DramaMockRepository` 中基于现有 `HOMEPAGE_DRAMAS` 实现 `search(params)`：对 `title` 与 `category` 做标准化后包含匹配，匹配顺序沿用现有数据顺序，再执行分页切片。
  2. 在 `DramaMockRepository` 中新增稳定热搜种子数据，实现 `listHotSearches()`，保证最多 10 条、结构满足 `rank/keyword/score`。
  3. 同步为 `DramaSupabaseRepository` 补齐 `search(params)` 与 `listHotSearches()`，至少满足接口编译与契约一致性；其中 `search` 走现有 `dramas` 表字段映射，`listHotSearches` 首版可返回静态热搜种子数据。
  4. 保持 repository 只负责查询、过滤、分页和数据映射，不把 HTTP 语义下沉到仓储层。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 实现 mock 搜索匹配、分页切片与热搜种子返回 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 补齐 supabase 搜索与热搜方法，满足统一接口契约 |

### Step 4：实现 service，统一做输出 schema 校验与内部错误包装

- **关联测试**：T-06
- **目标文件**：`backend/src/services/drama/drama.service.ts`
- **实现内容**：
  1. 在 `DramaService` 中新增 `searchDramas(params)` 与 `listHotSearches()`，分别调用 repository 对应方法。
  2. 对搜索结果使用 `DramaListResponseSchema` 校验，对热搜结果使用 `HotSearchListResponseSchema` 校验，确保 route 出口前的数据契约稳定。
  3. 当 repository 返回非法结构时，显式包装为 `Errors.internal(...)`，避免把内部数据问题暴露成客户端参数错误。
  4. 保持 `DramaService` 仍是薄编排层，不新增缓存、异步任务、真实搜索排序或历史持久化逻辑。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增搜索与热搜 service 方法，并统一做 schema parse / internal error 包装 |

### Step 5：接入 route，完成 `/api/dramas/search` 与 `/api/dramas/hot-search` 出口

- **关联测试**：T-07、T-08、T-09、T-10
- **目标文件**：`backend/src/app/api/dramas/search/route.ts`、`backend/src/app/api/dramas/hot-search/route.ts`
- **实现内容**：
  1. 新增 `/api/dramas/search` route，使用 `SearchDramaQuerySchema` 解析 `q/page/pageSize`，注入 `DramaMockRepository` 与 `DramaService`，返回 `DramaListResponse`。
  2. 新增 `/api/dramas/hot-search` route，复用 `withErrorHandler`，调用 `DramaService.listHotSearches()` 返回 `{ data }`。
  3. 保持错误响应完全走现有 `withErrorHandler`，让参数错误返回 `400 + VALIDATION_ERROR`，未知内部错误返回 `500 + INTERNAL_ERROR`。
  4. 不修改既有 `/api/dramas` 首页列表路由，只通过新增子路由补齐搜索发现能力。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-search.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-hot-search.test.ts`
  - 运行 `cd backend && npm run test`
  - 运行 `cd backend && npm run build`
  - 运行 `cd backend && npm run lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/dramas/search/route.ts` | 新增 | 搜索路由，解析 query 并返回 canonical drama 列表契约 |
| `backend/src/app/api/dramas/hot-search/route.ts` | 新增 | 热搜路由，返回 Top 10 以内热搜列表 |

## 依赖关系

```text
Step 1（先补测试）
  └──▶ Step 2（Schema + Interface）
          └──▶ Step 3（Repository）
                  └──▶ Step 4（Service）
                          └──▶ Step 5（Route）
```

## 验证总览

- [ ] 搜索与热搜测试全部通过（`cd backend && npm run test`）
- [ ] Build 成功（`cd backend && npm run build`）
- [ ] 无新增 lint 错误（`cd backend && npm run lint`）
- [ ] `/api/dramas/search` 返回 `DramaListResponse`，并覆盖空结果与非法参数
- [ ] `/api/dramas/hot-search` 返回 `{ data: HotSearchItem[] }`，长度不超过 10
- [ ] 搜索匹配规则符合 `title + category`、大小写不敏感包含匹配
- [ ] 大页码保持 `200 + data=[]`，不破坏分页元信息
- [ ] 内部契约错误统一返回 `INTERNAL_ERROR`

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增搜索 query 与 hot search 响应 schema |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补齐搜索 schema 正反向测试 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 扩展搜索参数、热搜类型与 repository 方法 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 实现搜索与热搜 mock 数据能力 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 覆盖搜索匹配、分页、热搜边界 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 补齐 supabase 搜索与热搜契约实现 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 覆盖 supabase 搜索与热搜契约测试 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增搜索与热搜 service，并统一做输出校验 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 覆盖 service 成功与内部错误路径 |
| `backend/src/app/api/dramas/search/route.ts` | 新增 | 新增搜索接口 |
| `backend/src/app/api/dramas/hot-search/route.ts` | 新增 | 新增热搜接口 |
| `backend/src/app/api/__tests__/dramas-search.test.ts` | 新增 | 搜索路由测试 |
| `backend/src/app/api/__tests__/dramas-hot-search.test.ts` | 新增 | 热搜路由测试 |
