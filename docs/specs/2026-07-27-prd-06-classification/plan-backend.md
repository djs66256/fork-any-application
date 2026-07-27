# 实现计划：Backend — PRD-06 分类浏览

> 创建日期：2026-07-27
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 需要在现有 `DramaService + DramaRepository + Next.js Route Handler` 四层结构上，补齐分类标签只读接口 `GET /api/dramas/tags`，并把既有搜索能力从 `title + category` 扩展到 `title + category + tags`，打通「分类页 → 标签点击 → 搜索结果页」闭环。

实现顺序采用轻量 TDD：先补测试锁定 contract，再按 `schema/interface → repository(mock 优先) → service → route → supabase 收口` 推进；默认运行链路继续以当前 route 中已使用的 mock repository 为准，Supabase 路径是否真正满足 tags 搜索闭环，以 coding 当轮是否补 migration 为准，计划中需显式收口，不能写成隐含已支持。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 分类 query schema 解析默认值与合法参数 | `{}`、`{ gender: "male" }`、`{ gender: "female" }` | 默认解析为 `gender=all`；合法值正确透传 | 单元测试 | P0 |
| T-02 | 分类 query schema 拦截非法参数 | `gender=unknown`、`gender=1` | 抛出校验错误，route 最终返回 `400 + VALIDATION_ERROR` | 单元测试 | P0 |
| T-03 | 分类标签 contract 固定三维度且保留空维度 | repository 返回某维度 `tags=[]` 或三维度全空 | 成功响应始终包含 `era_background / theme_plot / character_setting` 三个分组，不省略空维度 | 单元测试 | P0 |
| T-04 | `all` 标签集按固定顺序去重合并 | male / female 存在重复标签 | `all` 返回稳定去重并集，保留首次出现顺序 | 单元测试 | P0 |
| T-05 | 分类 route 返回 canonical 响应并覆盖默认值 | `GET /api/dramas/tags`、`GET /api/dramas/tags?gender=male`、`GET /api/dramas/tags?gender=female` | `200 + { data: { gender, dimensions } }`；默认请求等价 `all` | 路由测试 | P0 |
| T-06 | 搜索从 `title + category` 扩展到 `title + category + tags` | `q=萌宝`、`q=都市`、`q=逆袭` | tags 可命中对应短剧；title/category 旧行为保持不回归 | 单元测试 | P0 |
| T-07 | 搜索 route 继续保持分页与非法参数语义 | `GET /api/dramas/search?q=萌宝&page=1&pageSize=10`、`page=0` | 合法请求 `200` 且分页结构不变；非法参数返回 `400 + VALIDATION_ERROR` | 路由测试 | P0 |
| T-08 | service 对分类标签与搜索的非法 repository 输出做内部错误包装 | repository 返回缺维度、维度数不为 3 或分页结构非法 | service 返回 `500 + INTERNAL_ERROR`，不把服务端问题误报成 400 | 单元测试 | P0 |
| T-09 | mock 路径完整闭合分类与 tags 搜索链路 | route 默认实例化 mock repository | 分类接口与标签搜索在本地默认链路均可直接闭环 | 单元测试 | P0 |
| T-10 | Supabase 路径在未做 migration 时如实暴露限制 | 当前 `dramas` 表无 tags 列、repository 仍仅支持 `title/category` | 分类接口可继续通过仓库内种子返回；搜索不宣称已支持 tags 命中，并用测试/注释锁定限制 | 单元测试 | P1 |
| T-11 | Supabase 路径在补 migration 后满足 tags 搜索闭环 | 为 `dramas` 增加可查询 tags 存储后执行搜索 | repository 可在真实查询中匹配 tags；migration 可通过 `npx supabase db push` 应用 | 单元测试 | P1 |

## 实现步骤

### Step 1：先补测试，锁定分类标签与 tags 搜索的行为边界

- **关联测试**：T-01、T-02、T-03、T-04、T-05、T-06、T-07、T-08、T-09、T-10、T-11
- **目标文件**：`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/repositories/__tests__/drama.mock.repository.test.ts`、`backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`、`backend/src/services/drama/drama.service.test.ts`、`backend/src/app/api/__tests__/dramas-tags.test.ts`、`backend/src/app/api/__tests__/dramas-search.test.ts`
- **实现内容**：
  1. 先补 classification 相关测试，锁定 `GET /api/dramas/tags` 的默认 `gender=all`、合法 `male/female`、非法参数 `400`、固定三维度、空维度保留、`all` 去重合并等核心 contract。
  2. 在搜索相关测试中补上 `q=萌宝` 一类 tags 命中场景，同时保留 `q=都市`、`q=逆袭` 等 title/category 既有命中场景，避免 PRD-04 能力回归。
  3. 在 service 测试中先构造 repository 坏数据，锁定 service 对非法输出统一包装成 `Errors.internal(...)` 的语义。
  4. 在 Supabase repository 测试中先把“两条路径”写清楚：如果当轮不做 migration，则测试应明确记录 tags 搜索限制；如果当轮补 migration，则补上 tags 查询表达式与映射测试。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-tags.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-search.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加 classification query / response schema 的正反向测试 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 增加固定三维度、空维度、`all` 去重合并、tags 搜索命中测试 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 补齐 Supabase tags 搜索限制或 migration 后闭环测试 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 增加分类标签输出校验与内部错误包装测试 |
| `backend/src/app/api/__tests__/dramas-tags.test.ts` | 新增 | 覆盖分类接口成功、默认值、非法参数、空维度、内部错误 |
| `backend/src/app/api/__tests__/dramas-search.test.ts` | 修改 | 补充 tags 命中搜索场景，保持分页与错误语义不变 |

### Step 2：补齐 shared schema 与 repository interface，先把 contract 收口

- **关联测试**：T-01、T-02、T-03、T-04、T-08
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/repositories/interfaces/drama.repository.interface.ts`
- **实现内容**：
  1. 在 `schemas.ts` 中新增 `ClassificationGenderSchema`、`ClassificationTagsQuerySchema`、`ClassificationDimensionSchema`、`ClassificationTagsResponseSchema`，并用 `.length(3)` 锁定三维度长度。
  2. 明确三个稳定维度 key：`era_background`、`theme_plot`、`character_setting`，避免客户端与测试各自硬编码散落值。
  3. 在 repository interface 中新增 `ClassificationTagsQuery`、`ClassificationDimension`、`ClassificationTagsResult` 与 `listClassificationTags()` 方法定义，同时保持 `search(params)` 签名不变，避免 route contract 漂移。
  4. 将“空维度允许但不可省略”作为共享 contract 写进 schema / type 约束，而不是留给 route 层临时补空。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增 classification query、固定三维度、响应 schema |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 扩展分类标签查询契约与返回类型 |

### Step 3：优先实现 mock repository，先闭合默认运行链路

- **关联测试**：T-03、T-04、T-06、T-09
- **目标文件**：`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/repositories/__tests__/drama.mock.repository.test.ts`
- **实现内容**：
  1. 在 mock repository 内维护男频、女频两套 canonical 分类种子，并固定输出三维度顺序；即使某维度暂无标签，也返回空数组而非裁剪分组。
  2. 实现 `listClassificationTags(params)`：`male` / `female` 返回各自标签集，`all` 按固定顺序合并男频、女频并去重，保留首次出现顺序。
  3. 扩展 `search(params)`：在原有 `title + category` 匹配基础上增加 `tags.some(...)` 判断，保证分类标签点击后默认 mock 路径可命中结果。
  4. 校验分类种子与现有 mock drama 数据的可命中性，必要时微调种子文案，使每个对外返回的标签至少能在 `title/category/tags` 中命中一个 drama。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-search.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 新增分类种子、`all` 去重合并、固定三维度与 tags 搜索匹配 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 锁定默认 mock 路径的分类与搜索闭环行为 |

### Step 4：补齐 service 编排，统一做 schema 防御与错误包装

- **关联测试**：T-06、T-08、T-09
- **目标文件**：`backend/src/services/drama/drama.service.ts`、`backend/src/services/drama/drama.service.test.ts`
- **实现内容**：
  1. 在 `DramaService` 中新增 `listClassificationTags(params)`，调用 repository 后使用 `ClassificationTagsResponseSchema` 做输出守卫。
  2. 保持 `searchDramas(params)` 的输入输出结构不变，只让 repository 承担 tags 匹配扩展；service 继续负责统一把非法 repository 输出包装成 `Errors.internal(...)`。
  3. 明确分类 contract 的补空与三维度完整性由 repository 保证，service 只做编排和最终 schema 校验，避免职责漂移。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增分类标签 service，并保持搜索 service 兼容扩展 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 覆盖成功路径、坏数据包装与 tags 搜索兼容性 |

### Step 5：接入 route 层，补齐分类接口并保持搜索 route 语义稳定

- **关联测试**：T-05、T-07、T-09
- **目标文件**：`backend/src/app/api/dramas/tags/route.ts`、`backend/src/app/api/dramas/search/route.ts`、`backend/src/app/api/__tests__/dramas-tags.test.ts`、`backend/src/app/api/__tests__/dramas-search.test.ts`
- **实现内容**：
  1. 新增 `GET /api/dramas/tags` route，解析 query 后调用 `DramaService.listClassificationTags()`，通过 `withErrorHandler` 输出统一错误结构。
  2. 搜索 route 继续保持 `GET /api/dramas/search?q=...&page=...&pageSize=...`、成功体 `{ data, pagination }` 与非法参数 `400` 语义不变，不新增 `tag-search` 或额外 query 字段。
  3. 当前默认运行链路继续实例化 `DramaMockRepository`，优先保证本地与现有 harness 环境即可闭合分类标签点击搜索链路。
  4. 若后续引入 repository factory 或环境开关，不在本轮额外扩 scope；本轮只保证新增 route 与现有 route 一致风格落地。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-tags.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-search.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/dramas/tags/route.ts` | 新增 | 新增分类标签接口，处理 query 默认值与错误出口 |
| `backend/src/app/api/dramas/search/route.ts` | 修改 | 保持 route contract 不变，复用扩展后的搜索 service |
| `backend/src/app/api/__tests__/dramas-tags.test.ts` | 新增 | 覆盖分类接口核心路由语义 |
| `backend/src/app/api/__tests__/dramas-search.test.ts` | 修改 | 确认 tags 搜索命中与既有分页语义不回归 |

### Step 6：处理 Supabase 路径与 migration 收口，明确“支持”与“限制”边界

- **关联测试**：T-10、T-11
- **目标文件**：`backend/src/repositories/supabase/drama.supabase.repository.ts`、`backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`、`backend/supabase/migrations/<timestamp>_add_drama_tags_support.sql`（如当轮决定做 migration）
- **实现内容**：
  1. 先补 `DramaSupabaseRepository.listClassificationTags()`，其返回值可与 mock 路径共享同一套仓库内种子，从而保证分类接口 contract 不依赖真实数据库新表。
  2. 针对搜索闭环做两条路径收口：
     - 若当轮决定补 migration：新增全新 migration，为 `dramas` 增加可查询的 tags 存储结构，并扩展 Supabase `search()` 查询到 `title/category/tags`。
     - 若当轮不做 migration：保留当前 Supabase 搜索仅支持 `title/category` 的限制，在 repository 测试、代码注释与文档中如实写明“默认 mock 路径已闭环，Supabase 路径暂不承诺 tags 搜索命中”，禁止文档口径超前。
  3. 无论走哪条路径，都不得修改既有 migration 文件，只能新增 migration；同时保持现有 create/update 对 tags 的限制语义与实际存储能力一致。
  4. 完成后做 backend 全量回归，确认分类接口、搜索接口、lint、build 均无新增回归。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`
  - 运行 `cd backend && npm run test`
  - 运行 `cd backend && npm run lint`
  - 运行 `cd backend && npm run build`
  - 若新增 migration，运行 `cd backend && npx supabase db push`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 补齐分类标签查询，并按当轮 migration 决策处理 tags 搜索闭环 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 锁定 Supabase 路径的真实能力边界或 migration 后闭环行为 |
| `backend/supabase/migrations/<timestamp>_add_drama_tags_support.sql` | 新增（条件性） | 仅在当轮决定让 Supabase 真实支持 tags 搜索时新增 |

## 依赖关系

```text
Step 1（先补测试）
  └──▶ Step 2（Schema + Interface）
          └──▶ Step 3（Mock Repository）
                  └──▶ Step 4（Service）
                          └──▶ Step 5（Route）
Step 2 ──▶ Step 6（Supabase 路径与 migration 收口）
Step 3 ──▶ Step 6（默认 mock 闭环已成立后，再判断 Supabase 是否补齐）
Step 4 ──▶ Step 6
Step 5 ──▶ Step 6
```

## 验证总览

- [ ] 分类与搜索相关测试全部通过（`cd backend && npm run test`）
- [ ] Build 成功（`cd backend && npm run build`）
- [ ] 无新增 lint 错误（`cd backend && npm run lint`）
- [ ] `GET /api/dramas/tags` 的默认值、合法值、非法值均被覆盖
- [ ] 固定三维度、空维度保留、`all` 去重合并已被自动化测试锁定
- [ ] `GET /api/dramas/search` 已覆盖 `title + category + tags` 搜索扩展且不回归既有分页语义
- [ ] 默认 mock 运行链路可闭合「分类页标签点击 → 搜索结果页」
- [ ] Supabase 路径是否支持 tags 搜索与 migration 状态口径一致，不出现“代码未支持、文档已宣称支持”的偏差
- [ ] 若新增 migration，本地可通过 `cd backend && npx supabase db push` 应用

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增 classification query / response schema 与固定维度约束 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 覆盖默认值、合法值、非法值与响应 schema 测试 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 扩展分类标签查询契约 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 实现分类种子、`all` 去重合并、空维度保留与 tags 搜索扩展 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 覆盖 mock 路径分类与搜索闭环测试 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增分类标签 service，保持搜索 service 兼容扩展 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 覆盖 service 成功路径与内部错误包装 |
| `backend/src/app/api/dramas/tags/route.ts` | 新增 | 新增分类标签只读接口 |
| `backend/src/app/api/dramas/search/route.ts` | 修改 | 保持既有路由 contract，接入 tags 搜索扩展 |
| `backend/src/app/api/__tests__/dramas-tags.test.ts` | 新增 | 分类接口路由测试 |
| `backend/src/app/api/__tests__/dramas-search.test.ts` | 修改 | 搜索路由补充 tags 命中测试 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 补齐 Supabase 分类标签能力，并视 migration 决策处理 tags 搜索 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 修改 | 覆盖 Supabase 路径能力边界或 migration 后闭环 |
| `backend/supabase/migrations/<timestamp>_add_drama_tags_support.sql` | 新增（条件性） | 仅在当轮决定补齐 Supabase tags 搜索闭环时创建 |