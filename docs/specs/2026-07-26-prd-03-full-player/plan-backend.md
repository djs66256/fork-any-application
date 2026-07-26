# 实现计划：Backend — PRD-03 完整观看播放器

> 创建日期：2026-07-26
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

本期 Backend 端在现有 Next.js Route Handler + Service + Repository 四层结构内，补齐完整观看播放器首版所需的剧集列表、匿名续播查询、起播确认与进度保存能力。实现遵循轻量 TDD：先锁定 schema / route / repository / service 的测试场景，再逐步替换当前 `player/start`、`player/stop` 的 501 占位实现，并新增 `progress` 与 `episodes` 接口。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。
> 各端测试要求见对应 CLAUDE.md。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | Shared schema 与播放器错误码收口 | 合法 / 非法的 `episodes`、`progress`、`start`、`stop` 请求与响应对象 | 新增 Zod schema 可解析合法数据；非法 UUID / 缺失 header / 非法响应结构被拦截；错误码枚举可表达 `INVALID_PLAYBACK_SESSION`、`DRAMA_NOT_FOUND`、`EPISODE_NOT_FOUND`、`EPISODE_NOT_PLAYABLE` | 单元测试 | P0 |
| T-02 | `GET /api/dramas/:id/episodes` 返回播放器可消费剧集列表 | 合法 `dramaId`、存在 / 不存在短剧、短剧存在但剧集为空 | 返回 `200 + { code, data, message }`，`items` 按 `episode_number` 正序；不存在短剧时返回 `404`；空剧集返回 `200 + items=[]` | 集成测试 | P0 |
| T-03 | `GET /api/player/progress` 支持匿名续播查询 | 合法 `dramaId` + `X-Playback-Session-Id`，分别覆盖无历史 / 有历史 / 历史引用失效剧集 / 缺失 header | 无历史时返回 `has_history=false`；有历史时返回 `episode_id + start_time`；历史失效时回退 `has_history=false`；缺失或非法 header 返回 `400` | 集成测试 | P0 |
| T-04 | `POST /api/player/start` 校验剧集归属与资源可用性 | 合法 body、跨剧 `episode_id`、无资源 episode、负进度 | 成功返回 `accepted_progress`；跨剧或不存在剧集返回 `404`；资源不可用返回 `409`；负进度被 schema / service 归一化拦截 | 集成测试 | P0 |
| T-05 | `POST /api/player/stop` 保存并覆盖最近一次观看进度 | 合法 body、`progress > duration`、同一 `(playback_session_id, drama_id)` 重复上报、缺失 header | 成功返回 `saved_progress`；超时长进度被 clamp 到 `duration`；重复上报走 upsert 覆盖；缺失或非法 header 返回 `400` | 集成测试 | P0 |
| T-06 | Playback history repository 与配置切换可独立验证 | mock repository upsert / 查询样例、`PLAYER_HISTORY_REPOSITORY` 未配置 / 配置为 `mock` | `findLatest` 返回最近一条记录；同 key 写入被覆盖；config 暴露 repository mode 且默认值安全可回退到 `mock` | 单元测试 | P1 |

## 实现步骤

### Step 1：先收口 shared schema、错误码与测试基线

- **关联测试**：T-01
- **目标文件**：`backend/src/lib/schemas.ts`、`backend/src/lib/errors.ts`、`backend/src/lib/config.ts`、`backend/src/lib/__tests__/schemas.test.ts`、`backend/src/lib/__tests__/errors.test.ts`
- **实现内容**：
  1. 先在 `schemas.test.ts`、`errors.test.ts` 中补齐播放器首版 contract 的失败测试，锁定 `EpisodeListResponseSchema`、`PlayerProgressQuerySchema`、`PlayerProgressResponseSchema`、`PlayerStartResponseSchema`、`PlayerStopResponseSchema` 与 `PlaybackSessionIdHeaderSchema` 的输入输出语义。
  2. 在 `errors.ts` 中扩展播放器域错误码与 message 映射，保持继续走 `AppError + withErrorHandler` 的统一格式，不额外引入新的错误处理分支。
  3. 在 `config.ts` 中补充 `PLAYER_HISTORY_REPOSITORY` 的读取与默认值，为后续 mock / supabase repository 选择预留入口。
  4. 回写 `schemas.ts` 与错误码实现后，再补充一轮反向测试，覆盖非法 UUID、空历史结构、`saved_progress` / `accepted_progress` 的边界校验。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/lib/__tests__/schemas.test.ts` 确认 T-01 通过
  - 运行 `cd backend && npm run test -- src/lib/__tests__/errors.test.ts` 确认 T-01 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增播放器首版 query / response / header schema |
| `backend/src/lib/errors.ts` | 修改 | 扩展播放器域错误码与统一错误响应能力 |
| `backend/src/lib/config.ts` | 修改 | 增加 `PLAYER_HISTORY_REPOSITORY` 配置读取与默认值 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 新增播放器 schema 正反向断言 |
| `backend/src/lib/__tests__/errors.test.ts` | 修改 | 新增播放器错误码与响应格式回归测试 |

### Step 2：补齐剧集列表 route/service 主路径

- **关联测试**：T-02
- **目标文件**：`backend/src/app/api/dramas/[id]/episodes/route.ts`、`backend/src/services/episode/episode.service.ts`、`backend/src/repositories/mock/episode.mock.repository.ts`、`backend/src/app/api/__tests__/drama-episodes.test.ts`、`backend/src/services/episode/episode.service.test.ts`
- **实现内容**：
  1. 先新增 `drama-episodes.test.ts` 与 `episode.service.test.ts`，覆盖短剧存在且多集乱序、短剧存在但空列表、`dramaId` 不存在三类核心路径，让目标行为先以测试形式固定下来。
  2. 在 `EpisodeService` 中新增 `listEpisodesByDramaId`，通过注入 `DramaRepositoryInterface + EpisodeRepositoryInterface` 区分“短剧不存在”和“短剧存在但无剧集”，并在 service 层完成排序与响应 schema 收口。
  3. 扩展 `EpisodeMockRepository` 种子数据与 helper，让首页可达 drama 至少具备 3~5 集稳定测试数据，同时保留空列表与无资源 episode 的可构造能力。
  4. 新增 `backend/src/app/api/dramas/[id]/episodes/route.ts`，沿用现有 `withErrorHandler` 风格解析 path param，并以 `{ code, data, message }` 输出 canonical contract。
  5. 实现完成后补充边界测试，确保 route 不会把空列表误判成 404，且 `items` 始终按 `episode_number` 正序返回。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/services/episode/episode.service.test.ts` 确认 T-02 通过
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/drama-episodes.test.ts` 确认 T-02 通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/dramas/[id]/episodes/route.ts` | 新增 | 新增剧集列表接口 route |
| `backend/src/services/episode/episode.service.ts` | 修改 | 新增 `listEpisodesByDramaId` 与短剧存在性校验 |
| `backend/src/repositories/mock/episode.mock.repository.ts` | 修改 | 补齐播放器剧集种子与稳定排序输入 |
| `backend/src/app/api/__tests__/drama-episodes.test.ts` | 新增 | 覆盖剧集列表接口正常路径与边界 |
| `backend/src/services/episode/episode.service.test.ts` | 修改 | 覆盖剧集排序、空列表与短剧不存在场景 |

### Step 3：新增 playback history repository，并落地 progress 查询接口

- **关联测试**：T-03、T-06
- **目标文件**：`backend/src/repositories/interfaces/playback-history.repository.interface.ts`、`backend/src/repositories/mock/playback-history.mock.repository.ts`、`backend/src/repositories/supabase/playback-history.supabase.repository.ts`、`backend/src/repositories/__tests__/playback-history.mock.repository.test.ts`、`backend/src/services/player/player.service.ts`、`backend/src/app/api/player/progress/route.ts`、`backend/src/app/api/__tests__/player.progress.test.ts`
- **实现内容**：
  1. 先新增 `playback-history.mock.repository.test.ts` 与 `player.progress.test.ts`，锁定 repository 的 `findLatest/upsert` 语义，以及 progress 接口在“无历史 / 有历史 / 缺失 header / 失效历史”下的返回结构。
  2. 新增 playback history repository 抽象，定义以 `(playback_session_id, drama_id)` 为 key 的查询与 upsert 能力；mock 实现用于测试与默认开发场景，supabase 实现只负责与未来真实表结构对齐。
  3. 在 `PlayerService` 中新增 `getPlaybackProgress`，复用 drama / episode repository 做短剧存在性校验与失效历史过滤，避免把已删除 episode 返回给客户端。
  4. 新增 `backend/src/app/api/player/progress/route.ts`，在 route 层解析 query 与 `X-Playback-Session-Id` header，并把非法 header 语义化为 `INVALID_PLAYBACK_SESSION`，而不是完全依赖 Zod 默认错误。
  5. 同步补一份 `playback_history` 逻辑表对应的 repository 接入点，保证后续 `stop` 能直接复用本步骤的抽象与测试资产。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/repositories/__tests__/playback-history.mock.repository.test.ts` 确认 T-03/T-06 通过
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/player.progress.test.ts` 确认 T-03 通过
  - 运行 `cd backend && npm run test -- src/services/player/player.service.test.ts` 确认 T-03 相关 service 用例通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | 新增 | 定义续播历史查询与 upsert 抽象 |
| `backend/src/repositories/mock/playback-history.mock.repository.ts` | 新增 | 提供默认开发 / 测试使用的内存实现 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | 新增 | 预留真实持久化实现 |
| `backend/src/repositories/__tests__/playback-history.mock.repository.test.ts` | 新增 | 覆盖 repository 的查询与覆盖写入行为 |
| `backend/src/services/player/player.service.ts` | 修改 | 新增 `getPlaybackProgress` 与通用校验逻辑 |
| `backend/src/app/api/player/progress/route.ts` | 新增 | 新增匿名续播查询接口 |
| `backend/src/app/api/__tests__/player.progress.test.ts` | 新增 | 覆盖 progress 接口主路径与错误边界 |

### Step 4：把 `POST /api/player/start` 从 501 占位演进为真实起播确认接口

- **关联测试**：T-04
- **目标文件**：`backend/src/app/api/player/start/route.ts`、`backend/src/services/player/player.service.ts`、`backend/src/app/api/__tests__/player.start.test.ts`、`backend/src/services/player/player.service.test.ts`、`backend/src/app/api/__tests__/skeleton-endpoints.test.ts`
- **实现内容**：
  1. 先新增 `player.start.test.ts`，覆盖合法起播、跨剧集组合、无资源 episode、缺失 / 非法 `X-Playback-Session-Id` 四类断言，并把 `skeleton-endpoints.test.ts` 中对 `POST /api/player/start` 返回 501 的旧预期替换为“仅保留其他占位接口”的回归测试。
  2. 在 `PlayerService.startPlayback` 中补齐 `drama_id` 存在性、`episode_id` 归属、`video_url` 可播放性与 `accepted_progress` 归一化逻辑，复用 Step 3 中已经收口的通用校验能力，避免 route 层堆业务判断。
  3. 改造 `backend/src/app/api/player/start/route.ts`，解析 body + header，实例化所需 repository / service，并返回共享设计约定的 `{ code, data, message }` 响应。
  4. 完成主路径后补充 service 层测试，确保负值 progress 不会绕过 schema / service 双层保护，且 route 错误码与 design-backend 保持一致。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/player.start.test.ts` 确认 T-04 通过
  - 运行 `cd backend && npm run test -- src/services/player/player.service.test.ts` 确认 T-04 相关 service 用例通过
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/player/start/route.ts` | 修改 | 接入 body/header 解析与真实起播确认逻辑 |
| `backend/src/services/player/player.service.ts` | 修改 | 实现 `startPlayback` 的归属校验与资源校验 |
| `backend/src/app/api/__tests__/player.start.test.ts` | 新增 | 覆盖起播接口成功与失败路径 |
| `backend/src/services/player/player.service.test.ts` | 修改 | 将 not implemented 测试替换为真实起播逻辑测试 |
| `backend/src/app/api/__tests__/skeleton-endpoints.test.ts` | 修改 | 移除 `player/start` 的 501 旧断言，保留其他骨架接口回归 |

### Step 5：落地 `POST /api/player/stop`、补齐 migration，并完成全量回归

- **关联测试**：T-05、T-06
- **目标文件**：`backend/src/app/api/player/stop/route.ts`、`backend/src/services/player/player.service.ts`、`backend/supabase/migrations/<timestamp>_create_playback_history.sql`、`backend/src/app/api/__tests__/player.stop.test.ts`、`backend/src/services/player/player.service.test.ts`
- **实现内容**：
  1. 先新增 `player.stop.test.ts`，覆盖合法保存、`progress > duration` 的 clamp、重复保存覆盖、缺失 header 与 episode 不归属 drama 等路径。
  2. 在 `PlayerService.stopPlayback` 中实现 `clamp(progress, 0, duration)`、剧集归属校验与 playback history upsert，保证同一 `(playback_session_id, drama_id)` 只保留最近一次记录。
  3. 改造 `backend/src/app/api/player/stop/route.ts`，接入 body/header 解析与 service 调用，并输出 `saved_progress`、`duration`、`updated_at`。
  4. 新增 `backend/supabase/migrations/<timestamp>_create_playback_history.sql`，把逻辑表结构落到 Supabase migration 中；开发与测试默认仍走 mock repository，不把真实数据库作为本期自动化测试前置条件。
  5. 收尾时跑通所有播放器相关测试、全量 `npm run test`、`npm run build` 与 `npm run lint`，确认 `episodes/progress/start/stop` 四条主路径与现有 `/api/dramas` 不互相回归。
- **验证方式**：
  - 运行 `cd backend && npm run test -- src/app/api/__tests__/player.stop.test.ts` 确认 T-05 通过
  - 运行 `cd backend && npm run test -- src/services/player/player.service.test.ts` 确认 T-05 相关 service 用例通过
  - 运行 `cd backend && npm run test` 确认播放器相关回归全部通过
  - 运行 `cd backend && npm run build` 确认构建通过
  - 运行 `cd backend && npm run lint` 确认无新增 lint 错误
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/player/stop/route.ts` | 修改 | 接入进度保存逻辑与统一响应 |
| `backend/src/services/player/player.service.ts` | 修改 | 实现 `stopPlayback` 的 clamp 与 upsert 逻辑 |
| `backend/supabase/migrations/<timestamp>_create_playback_history.sql` | 新增 | 创建 `playback_history` 表与索引 |
| `backend/src/app/api/__tests__/player.stop.test.ts` | 新增 | 覆盖 stop 接口保存与边界行为 |
| `backend/src/services/player/player.service.test.ts` | 修改 | 补齐 stop 逻辑与 clamp/upsert 测试 |

## 依赖关系

```text
Step 1 ──▶ Step 2
   ├────▶ Step 3 ──▶ Step 4
   └────────────────▶ Step 5
Step 2 ─────────────────▶ Step 4
Step 3 ─────────────────▶ Step 5
```

## 验证总览

- [ ] 所有测试通过（`cd backend && npm run test`）
- [ ] Build 成功（`cd backend && npm run build`）
- [ ] 无新增 lint 错误（`cd backend && npm run lint`）
- [ ] `GET /api/dramas/:id/episodes` 返回按 `episode_number` 正序的 canonical contract
- [ ] `GET /api/player/progress` 能区分无历史、有历史与失效历史三类结果
- [ ] `POST /api/player/start` 不再返回 501，且正确校验 header、剧集归属与资源可用性
- [ ] `POST /api/player/stop` 能 clamp 进度并按 `(playback_session_id, drama_id)` 覆盖写入最近记录
- [ ] `PlaybackHistory` mock repository、Supabase migration 与 config 入口保持一致

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/schemas.ts` | 修改 | 新增播放器 query / response / header schema |
| `backend/src/lib/errors.ts` | 修改 | 扩展播放器域错误码 |
| `backend/src/lib/config.ts` | 修改 | 增加 playback history repository 配置 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | schema 正反向测试 |
| `backend/src/lib/__tests__/errors.test.ts` | 修改 | 错误码与错误响应测试 |
| `backend/src/app/api/dramas/[id]/episodes/route.ts` | 新增 | 剧集列表接口 |
| `backend/src/app/api/player/progress/route.ts` | 新增 | 匿名续播查询接口 |
| `backend/src/app/api/player/start/route.ts` | 修改 | 起播确认接口从 501 改为真实实现 |
| `backend/src/app/api/player/stop/route.ts` | 修改 | 进度保存接口从 501 改为真实实现 |
| `backend/src/app/api/__tests__/drama-episodes.test.ts` | 新增 | 剧集列表接口测试 |
| `backend/src/app/api/__tests__/player.progress.test.ts` | 新增 | progress 接口测试 |
| `backend/src/app/api/__tests__/player.start.test.ts` | 新增 | start 接口测试 |
| `backend/src/app/api/__tests__/player.stop.test.ts` | 新增 | stop 接口测试 |
| `backend/src/app/api/__tests__/skeleton-endpoints.test.ts` | 修改 | 清理 start/stop 的旧 501 回归断言 |
| `backend/src/services/episode/episode.service.ts` | 修改 | 新增剧集列表 service 逻辑 |
| `backend/src/services/episode/episode.service.test.ts` | 修改 | 剧集列表 service 测试 |
| `backend/src/services/player/player.service.ts` | 修改 | 新增 progress/start/stop 真实业务逻辑 |
| `backend/src/services/player/player.service.test.ts` | 修改 | 用真实业务测试替换 not implemented 测试 |
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | 新增 | 续播历史 repository 抽象 |
| `backend/src/repositories/mock/playback-history.mock.repository.ts` | 新增 | 续播历史 mock 实现 |
| `backend/src/repositories/mock/episode.mock.repository.ts` | 修改 | 补齐播放器剧集种子数据 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | 新增 | 续播历史 Supabase 实现 |
| `backend/src/repositories/__tests__/playback-history.mock.repository.test.ts` | 新增 | playback history repository 测试 |
| `backend/supabase/migrations/<timestamp>_create_playback_history.sql` | 新增 | `playback_history` 建表 migration |
