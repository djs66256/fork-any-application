# 实现计划：Backend — PRD-07 菜单面板

> 创建日期：2026-07-28
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 仅交付菜单面板“最近在看”只读链路：新增 `GET /api/player/recently-viewed`，复用现有匿名 `X-Playback-Session-Id`、`playback_history`、`drama`、`episode` 能力，为移动端返回最多 3 条续播摘要。

实现顺序遵循轻量 TDD：先锁定 schema、repository、service、route 的测试场景，再按四层架构逐层落地；本期只涉及 `backend/` 目录相关实现，不新增数据库 migration，不修改现有 `progress/start/stop` 接口语义。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | recently-viewed schema 解析 canonical 成功响应 | `code=0 + data.items[0..3] + message="ok"`，含 `cover_url=null` | schema 通过；允许空数组与空封面 | 单元测试 | P0 |
| T-02 | recently-viewed schema 拦截越界与脏字段 | `items.length=4`、非法 UUID、负进度、`episode_number=0` | schema 校验失败 | 单元测试 | P0 |
| T-03 | playback history mock repository 按 session 返回最近记录 | 同一 session 下多条历史、不同 `updated_at`、混入其它 session | 仅返回目标 session 记录，按 `updated_at desc` 排序并 obey `limit` | 单元测试 | P0 |
| T-04 | PlayerService 在无历史时返回空成功态 | 合法 `playbackSessionId`，repository 返回空数组 | `200` 语义的 `code=0 + data.items=[] + message="ok"` | 单元测试 | P0 |
| T-05 | PlayerService 过滤脏历史并截断到 3 条有效摘要 | 候选历史中包含缺失 drama / episode、跨 drama episode、不足 3 条有效项 | 过滤无效项；返回最多 3 条有效摘要；允许不足 3 条 | 单元测试 | P0 |
| T-06 | recently-viewed route 拦截缺失或非法 header | 缺失 `X-Playback-Session-Id`、非 UUID header | 返回 `400 + INVALID_PLAYBACK_SESSION` | 路由测试 | P0 |
| T-07 | recently-viewed route 返回 canonical 成功响应 | 合法 header，service 返回有效项或空数组 | 返回 `200 + { code, data: { items }, message }` | 路由测试 | P0 |
| T-08 | Supabase playback history repository 支持最近记录查询 | `playback_session_id + limit`、Supabase error | 生成 `eq + order(updated_at desc) + limit` 查询；基础设施异常被包装为内部错误 | 单元测试 | P1 |

## 实现步骤

### Step 1：先锁定 recently-viewed shared contract，再收口 schema 与接口定义

- **关联测试**：T-01、T-02
- **目标文件**：`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/lib/schemas.ts`、`backend/src/repositories/interfaces/playback-history.repository.interface.ts`
- **实现内容**：
  1. 先在 `schemas.test.ts` 中补 recently-viewed 正反向用例，锁定最多 3 条、`cover_url` 可空、`progress >= 0`、`episode_number >= 1` 等 contract。
  2. 在 `schemas.ts` 中新增 `RecentlyViewedItemSchema` 与 `RecentlyViewedResponseSchema`，继续沿用当前 player 域 `{ code, data, message }` 成功包裹风格。
  3. 在 `playback-history.repository.interface.ts` 中补充 `listRecentBySession(playbackSessionId, limit)` 契约，为后续 repository / service 提供统一入口。
  4. 明确本期只做只读聚合，不引入 query 参数、分页参数或 migration 相关接口变化。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts` 确认 T-01、T-02 通过 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 新增 recently-viewed schema 正反向测试 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 recently-viewed item / response schema |
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | 修改 | 扩展按 session 查询最近记录的方法契约 |

### Step 2：先补 repository 测试，再实现 mock 最近记录查询能力

- **关联测试**：T-03
- **目标文件**：`backend/src/repositories/__tests__/playback-history.mock.repository.test.ts`、`backend/src/repositories/mock/playback-history.mock.repository.ts`
- **实现内容**：
  1. 先补 `PlaybackHistoryMockRepository` 测试，覆盖同 session 多记录排序、跨 session 过滤、`limit` 截断、返回副本不污染内部状态等场景。
  2. 在 mock repository 中实现 `listRecentBySession`：遍历内存数据、筛选目标 `playback_session_id`、按 `updated_at desc` 排序、截断到指定 `limit`。
  3. 保持现有 `findLatest` / `upsert` 行为不变，避免影响 `progress` / `stop` 已有测试。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/playback-history.mock.repository.test.ts` 确认 T-03 通过 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/__tests__/playback-history.mock.repository.test.ts` | 修改 | 增加按 session 最近记录查询测试 |
| `backend/src/repositories/mock/playback-history.mock.repository.ts` | 修改 | 实现 `listRecentBySession` 的筛选、排序与 limit |

### Step 3：先补 PlayerService 聚合测试，再实现最近在看业务编排

- **关联测试**：T-04、T-05
- **目标文件**：`backend/src/services/player/player.service.test.ts`、`backend/src/services/player/player.service.ts`
- **实现内容**：
  1. 先在 `player.service.test.ts` 中补最近在看用例，覆盖空历史、有效 1~3 条、候选窗口内混入缺失 drama / episode 的脏数据过滤、超过 3 条时只取前 3 条。
  2. 在 `PlayerService` 中新增 `getRecentlyViewed(playbackSessionId)`，通过 `playbackHistoryRepository.listRecentBySession(...)` 拉取候选窗口，再聚合 `dramaRepository.findById` 与 `episodeRepository.findById`。
  3. 对缺失 drama、缺失 episode、`episode.drama_id !== history.drama_id` 的记录直接过滤，不向客户端下发脏数据。
  4. 最终使用 `RecentlyViewedResponseSchema.parse(...)` 收口输出，保持 `items=[]` 也返回成功态。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/services/player/player.service.test.ts` 确认 T-04、T-05 通过 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/player/player.service.test.ts` | 修改 | 增加最近在看聚合、过滤、截断、空态测试 |
| `backend/src/services/player/player.service.ts` | 修改 | 新增 `getRecentlyViewed` 业务编排逻辑 |

### Step 4：先补 route 测试，再新增 recently-viewed API 路由

- **关联测试**：T-06、T-07
- **目标文件**：`backend/src/app/api/__tests__/player.recently-viewed.test.ts`、`backend/src/app/api/player/recently-viewed/route.ts`
- **实现内容**：
  1. 先新增 route 测试，覆盖 header 缺失、header 非 UUID、成功返回有效项、成功返回空数组四类核心路径。
  2. 新增 `GET /api/player/recently-viewed` route，复用当前 `progress` 路由一致的 header 校验模式，解析 `X-Playback-Session-Id` 后调用 `PlayerService.getRecentlyViewed(...)`。
  3. route 仅处理请求解析与 service 调用，错误继续交给 `withErrorHandler` 输出统一 JSON 结构，不在 route 内做额外业务分支。
  4. 保持 `progress/start/stop` 现有 route 文件不变，避免为抽取公共 helper 扩大本期改动面。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/player.recently-viewed.test.ts` 确认 T-06、T-07 通过 ✅ 已完成
  - 运行 `cd backend && npm run test -- src/services/player/player.service.test.ts` 做 route 接入后的回归确认 ✅ 已完成
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/__tests__/player.recently-viewed.test.ts` | 新增 | 覆盖 recently-viewed route 的成功与 400 语义 |
| `backend/src/app/api/player/recently-viewed/route.ts` | 新增 | 新增最近在看只读接口 |

### Step 5：先补 Supabase 查询测试，再实现真实存储路径并做 Backend 回归

- **关联测试**：T-08
- **目标文件**：`backend/src/repositories/supabase/__tests__/playback-history.supabase.repository.test.ts`、`backend/src/repositories/supabase/playback-history.supabase.repository.ts`
- **实现内容**：
  1. 先新增 Supabase repository 测试，锁定 `listRecentBySession` 的查询形态：`.eq('playback_session_id', ...) + .order('updated_at', { ascending: false }) + .limit(limit)`。
  2. 在 `PlaybackHistorySupabaseRepository` 中补齐 `listRecentBySession`，继续复用现有 `PLAYBACK_HISTORY_SELECT_COLUMNS` 与 `PlaybackHistorySchema` 映射逻辑。
  3. 对 Supabase 返回错误统一包装为 `Errors.internal(...)`，与当前 repository 风格保持一致。
  4. 完成后执行 player 域定向回归与 backend 全量回归，确认新增接口不影响既有 `progress/start/stop`、build 与 lint。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/supabase/__tests__/playback-history.supabase.repository.test.ts` 确认 T-08 通过 ✅ 已完成
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/player.progress.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/player.start.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/player.stop.test.ts` ✅ 已完成
  - 运行 `cd backend && npm run test` ✅ 已完成
  - 运行 `cd backend && npm run build` ✅ 已完成
  - 运行 `cd backend && npm run lint` ✅ 已完成（存在 5 条仓库既有 warning，无新增 lint error）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/supabase/__tests__/playback-history.supabase.repository.test.ts` | 新增 | 覆盖 Supabase 最近记录查询与错误包装测试 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | 修改 | 实现真实存储下的 `listRecentBySession` |

## 依赖关系

```text
Step 1（Schema + Interface）
  └──▶ Step 2（Mock Repository）
          └──▶ Step 3（PlayerService）
                  └──▶ Step 4（Route）
                          └──▶ Step 5（Supabase + 全量回归）
```

## 验证总览

- [x] recently-viewed 相关测试全部通过（`cd backend && npm run test`）
- [x] Build 成功（`cd backend && npm run build`）
- [x] 无新增 lint 错误（`cd backend && npm run lint`）
- [x] `GET /api/player/recently-viewed` 的缺失 header、非法 header、空数组、成功返回 1~3 条均被自动化测试覆盖
- [x] service 已覆盖脏历史过滤、候选窗口截断、无历史成功态
- [x] mock / supabase 两条 playback history repository 路径都已支持按 session 查询最近记录
- [x] 不新增 migration，不改动既有 `GET /api/player/progress`、`POST /api/player/start`、`POST /api/player/stop` 的外部 contract

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增 recently-viewed item / response schema |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 补齐 recently-viewed schema 正反向测试 |
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | 修改 | 增加按 session 查询最近记录的方法契约 |
| `backend/src/repositories/mock/playback-history.mock.repository.ts` | 修改 | 实现 mock 最近记录查询 |
| `backend/src/repositories/__tests__/playback-history.mock.repository.test.ts` | 修改 | 覆盖 mock repository 排序、过滤、limit 测试 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | 修改 | 实现 Supabase 最近记录查询 |
| `backend/src/repositories/supabase/__tests__/playback-history.supabase.repository.test.ts` | 新增 | 覆盖 Supabase 查询链路测试 |
| `backend/src/services/player/player.service.ts` | 修改 | 新增最近在看聚合 service |
| `backend/src/services/player/player.service.test.ts` | 修改 | 覆盖最近在看空态、过滤、截断测试 |
| `backend/src/app/api/player/recently-viewed/route.ts` | 新增 | 新增 recently-viewed route |
| `backend/src/app/api/__tests__/player.recently-viewed.test.ts` | 新增 | 覆盖 recently-viewed route 测试 |