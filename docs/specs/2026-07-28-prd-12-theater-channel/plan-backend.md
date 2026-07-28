# 实现计划：Backend — PRD-12 剧场频道

> 创建日期：2026-07-28
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 需要在现有 `Route → Service → Repository → Shared` 四层结构上新增 `GET /api/dramas/channel`，为移动端剧场频道提供只读分页 Feed。

实现顺序采用轻量 TDD：先补测试锁定 query 校验、固定顺序分页、空频道和错误处理，再按 `schema/interface → repository → service/route → 回归验证` 收口，避免影响既有 `dramas`、`rankings`、`tags` 能力。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | 剧场 query schema 解析默认值与合法参数 | `{}`、`{ channel: "all", page: "2", pageSize: "20" }` | 默认解析为 `all/1/20`，合法输入被正确转换 | 单元测试 | P0 |
| T-02 | 剧场 query schema 拦截非法参数 | `channel=foo`、`page=0`、`pageSize=101` | 抛出校验错误，route 最终返回 `400 + VALIDATION_ERROR` | 单元测试 | P0 |
| T-03 | mock repository 对 `channel=all` 返回固定顺序第一页 | `channel=all&page=1&pageSize=20` | 返回稳定顺序的数据，字段包含 `heat:int>=0` | 单元测试 | P0 |
| T-04 | mock repository 对非 `all` 频道返回空结果 | `channel=real`、`channel=anime` 等 | 返回 `200` 语义对应的数据结构：`data=[]`，`pagination.total=0` | 单元测试 | P0 |
| T-05 | mock repository 在超大页码下保持合法分页结果 | `channel=all&page=999&pageSize=20` | 返回 `data=[]`，分页元信息与总量正确 | 单元测试 | P0 |
| T-06 | service 对非法 repository 输出做内部错误包装 | repository 返回缺失 `heat` 或分页结构非法 | 抛出 `INTERNAL_ERROR`，不把内部契约问题暴露成 400 | 单元测试 | P0 |
| T-07 | `/api/dramas/channel` 路由返回 canonical 响应并支持 registry 注入 | `GET /api/dramas/channel?channel=all&page=1&pageSize=20` | `200 + { data, pagination }`，route 通过 `getDramaRepository()` 取仓储 | 路由测试 | P0 |
| T-08 | `/api/dramas/channel` 路由覆盖空频道、非法参数与内部异常 | `channel=real`、`channel=foo`、service/repository 抛错 | 分别返回 `200 + []`、`400 + VALIDATION_ERROR`、`500 + INTERNAL_ERROR` | 路由测试 | P0 |

## 实现步骤

### Step 1：先补测试，锁定剧场频道 contract 与边界

- **关联测试**：T-01、T-02、T-03、T-04、T-05、T-06、T-07、T-08
- **目标文件**：`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/repositories/__tests__/drama.mock.repository.test.ts`、`backend/src/services/drama/drama.service.test.ts`、`backend/src/app/api/__tests__/dramas-channel.test.ts`
- **实现内容**：
  1. 先新增剧场频道相关测试，覆盖 query 默认值/非法值、`channel=all` 稳定顺序、非 `all` 空结果、超大页码、`heat` 数值语义、内部错误包装等核心行为。
  2. 在 repository 测试中固定 theater seed 顺序与分页切片规则，避免 coding 过程中用 `created_at` 或临时排序替代固定顺序源。
  3. 在 route 测试中复用 `repository-registry` 的注入方式，验证新 route 不直接 `new DramaMockRepository()`，并覆盖成功、空结果、400、500 路径。
  4. 保持既有 `/api/dramas`、`/api/dramas/rankings`、`/api/dramas/tags` 测试不回退，只新增本期剧场接口测试。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-channel.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加剧场 query / response schema 正反向测试 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 增加固定顺序、空频道、超大页码、`heat` 语义测试 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 增加剧场 feed 成功与内部错误包装测试 |
| `backend/src/app/api/__tests__/dramas-channel.test.ts` | 新增 | 覆盖路由成功、空结果、非法参数、内部错误与 registry 注入测试 |

### Step 2：补齐 shared schema 与 repository interface，先把 contract 收口

- **关联测试**：T-01、T-02、T-06
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/repositories/interfaces/drama.repository.interface.ts`
- **实现内容**：
  1. 在 `schemas.ts` 中新增 `TheaterChannelSchema`、`TheaterFeedQuerySchema`、`TheaterDramaSchema`、`TheaterFeedResponseSchema`，并复用现有 `DramaSchema` 与 `PaginationSchema`。
  2. 在 repository interface 中新增 `TheaterFeedParams`、`listTheaterFeed(...)` 等契约，明确 `channel` 枚举、默认分页和剧场卡片 `heat` 为服务端原始整数值。
  3. 保持成功响应继续沿用当前 Backend 的资源体直出风格，不新增新的 envelope，也不为只读接口引入额外写模型。
  4. 确保 interface 变更后 mock / supabase repository 都有明确的实现或占位，避免后续编译回退。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增剧场频道 query、实体、响应 schema |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 扩展剧场 Feed 查询契约与类型 |

### Step 3：实现 repository，先闭合固定顺序、频道分流与分页规则

- **关联测试**：T-03、T-04、T-05
- **目标文件**：`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/repositories/supabase/drama.supabase.repository.ts`
- **实现内容**：
  1. 在 `DramaMockRepository` 中声明独立的 theater seed 顺序，复用现有 drama 基础字段并映射 `heat`，确保 `channel=all` 所有分页都基于同一份有序数组切片。
  2. 对 `real / anime / movie / audio / novel / comic / bigscreen` 统一返回合法空结果，而不是伪造内容或抛业务异常。
  3. 保持 `heat` 在 repository 映射阶段直接输出为 `int >= 0`，不格式化为字符串，避免端侧失去原始数值。
  4. 对已有 `DramaSupabaseRepository` 补齐 `listTheaterFeed` 的接口兼容实现或显式占位，确保新增 interface 后编译通过；首版交付仍以 mock 路径为默认运行方式。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 实现 theater seed、稳定分页、空频道与 `heat` 映射 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 补齐剧场 Feed 接口兼容实现或占位 |

### Step 4：实现 service 与 route，完成 `/api/dramas/channel` 出口

- **关联测试**：T-06、T-07、T-08
- **目标文件**：`backend/src/services/drama/drama.service.ts`、`backend/src/app/api/dramas/channel/route.ts`
- **实现内容**：
  1. 在 `DramaService` 中新增 `listTheaterFeed(params)`，统一调用 repository 并使用 `TheaterFeedResponseSchema` 做输出校验。
  2. 当 repository 返回脏结构时，统一包装为 `Errors.internal('Invalid theater feed result')`，避免把内部问题误报成客户端参数错误。
  3. 新增 `/api/dramas/channel` route，使用 `withErrorHandler` + `TheaterFeedQuerySchema.parse()` 处理 query，并通过 `getDramaRepository()` 创建 `DramaService`。
  4. 保持 route 层只负责 query 解析、依赖获取和 JSON 返回，不把固定顺序、空频道、`heat` 语义等业务逻辑下沉到 route。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-channel.test.ts`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增剧场 Feed service，并统一做 schema parse / internal error 包装 |
| `backend/src/app/api/dramas/channel/route.ts` | 新增 | 新增剧场频道接口，解析 query 并通过 registry 注入 repository |

### Step 5：做 backend 回归验证，确认新增接口不破坏既有能力

- **关联测试**：T-01、T-02、T-03、T-04、T-05、T-06、T-07、T-08
- **目标文件**：`backend/src/app/api/dramas/channel/route.ts`、`backend/src/services/drama/drama.service.ts`、`backend/src/repositories/mock/drama.mock.repository.ts`、`backend/src/lib/schemas.ts`
- **实现内容**：
  1. 跑通剧场接口相关定向测试，确认 schema、repository、service、route 四层行为一致。
  2. 进行 backend 全量测试回归，确保现有 `dramas`、`search`、`rankings`、`tags`、`episodes` 能力未受接口扩展影响。
  3. 运行 build 与 lint，确认新增 route、interface 扩展和 supabase 占位实现不会引入类型或静态检查问题。
  4. 若回归中发现 contract 偏差，优先回到对应测试补断言，再修正实现，保持 TDD 闭环。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/dramas-channel.test.ts`
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/drama.mock.repository.test.ts`
  - 运行 `cd backend && npm run test -- src/services/drama/drama.service.test.ts`
  - 运行 `cd backend && npm run test`
  - 运行 `cd backend && npm run build`
  - 运行 `cd backend && npm run lint`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 剧场 Feed schema 最终收口 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 剧场数据规则最终收口 |
| `backend/src/services/drama/drama.service.ts` | 修改 | service 错误包装与输出校验最终收口 |
| `backend/src/app/api/dramas/channel/route.ts` | 新增 | 剧场接口最终出口 |

## 依赖关系

```text
Step 1（先补测试）
  └──▶ Step 2（Schema + Interface）
          └──▶ Step 3（Repository）
                  └──▶ Step 4（Service + Route）
                          └──▶ Step 5（全量回归验证）
```

## 验证总览

- [ ] 剧场频道定向测试全部通过（`cd backend && npm run test -- src/app/api/__tests__/dramas-channel.test.ts`）
- [ ] Backend 全量测试通过（`cd backend && npm run test`）
- [ ] Build 成功（`cd backend && npm run build`）
- [ ] 无新增 lint 错误（`cd backend && npm run lint`）
- [ ] `channel=all` 默认请求返回稳定顺序第一页
- [ ] 非 `all` 频道统一返回 `200 + data=[] + 合法 pagination`
- [ ] 超大页码返回空列表而非异常
- [ ] `heat` 始终为原始整数值
- [ ] route 使用 `getDramaRepository()`，未在 route 中直接实例化 mock repository

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增剧场频道 query、实体、响应 schema |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补齐剧场 schema 正反向测试 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 修改 | 扩展剧场 Feed 查询契约 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 修改 | 实现 theater seed、稳定顺序、空频道与分页 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 修改 | 补齐接口兼容实现或占位 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 修改 | 覆盖剧场 Feed 核心业务逻辑测试 |
| `backend/src/services/drama/drama.service.ts` | 修改 | 新增剧场 Feed service，并统一做输出校验 |
| `backend/src/services/drama/drama.service.test.ts` | 修改 | 覆盖 service 成功与内部错误路径 |
| `backend/src/app/api/dramas/channel/route.ts` | 新增 | 新增剧场频道接口 |
| `backend/src/app/api/__tests__/dramas-channel.test.ts` | 新增 | 剧场 route 测试 |
