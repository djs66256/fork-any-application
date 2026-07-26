# Backend 端技术方案：PRD-03 完整观看播放器

> 创建日期：2026-07-26
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Backend 端沿用现有 Next.js App Router Route Handlers + Service + Repository 的轻量分层，在不破坏首页 Feed 既有 `/api/dramas` 契约的前提下，新增播放器 bootstrap 所需的读接口，并将 `player/start`、`player/stop` 从 501 占位演进为首版可用能力。

```
HTTP Request
  -> Route Handler
  -> withErrorHandler / Zod parse
  -> Service
      -> DramaService / EpisodeService / PlayerService
  -> Repository
      -> DramaRepository / EpisodeRepository / PlaybackHistoryRepository
  -> Mock / Supabase persistence
  -> JSON Response
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/dramas/route.ts` | 不变 | 继续承载首页 Feed 列表接口 |
| `backend/src/app/api/dramas/[id]/route.ts` | 不变 | 详情接口仍保留未实现，占位状态不受本期影响 |
| `backend/src/app/api/player/start/route.ts` | 扩展 | 从 501 占位改为真实起播确认接口 |
| `backend/src/app/api/player/stop/route.ts` | 扩展 | 从 501 占位改为进度保存接口 |
| `backend/src/app/api/dramas/[id]/episodes/route.ts` | 新增 | 提供当前短剧的剧集列表 |
| `backend/src/app/api/player/progress/route.ts` | 新增 | 提供续播 bootstrap 查询 |
| `backend/src/lib/schemas.ts` | 扩展 | 增补播放进度 query / response / episode list response schema |
| `backend/src/lib/errors.ts` | 扩展 | 新增播放器域错误码与 message 约定 |
| `backend/src/services/episode/episode.service.ts` | 扩展 | 从单个 episode 查询扩展到 drama 维度列表查询 |
| `backend/src/services/player/player.service.ts` | 扩展 | 引入 progress / start / stop 的真实业务逻辑 |
| `backend/src/repositories/interfaces/episode.repository.interface.ts` | 不变 | 已有 `findByDramaId` / `findById`，继续复用 |
| `backend/src/repositories/mock/episode.mock.repository.ts` | 扩展 | 补齐播放器首版所需 Episode 种子数据 |
| `PlaybackHistoryRepository` 相关文件 | 新增 | 为匿名续播记录提供 repository 抽象与 mock 实现 |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/dramas/[id]/episodes/route.ts` | 新增 | 新增剧集列表接口 route |
| `backend/src/app/api/player/progress/route.ts` | 新增 | 新增续播 bootstrap 查询接口 |
| `backend/src/app/api/player/start/route.ts` | 修改 | 接入请求体解析、header 校验、service 调用 |
| `backend/src/app/api/player/stop/route.ts` | 修改 | 接入请求体解析、header 校验、progress 保存 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 progress / episode list / response schemas |
| `backend/src/lib/errors.ts` | 修改 | 新增播放器域错误码 |
| `backend/src/services/player/player.service.ts` | 修改 | 实现 start / stop / getProgress 逻辑 |
| `backend/src/services/episode/episode.service.ts` | 修改 | 新增 `listEpisodesByDramaId` |
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | 新增 | 抽象续播记录查询与 upsert 能力 |
| `backend/src/repositories/mock/playback-history.mock.repository.ts` | 新增 | 开发 / 测试默认实现 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | 新增 | 预留真实持久化实现 |
| `backend/src/app/api/__tests__/player.progress.test.ts` | 新增 | 覆盖 progress 接口主路径与边界 |
| `backend/src/app/api/__tests__/player.start.test.ts` | 新增 | 覆盖起播确认接口 |
| `backend/src/app/api/__tests__/player.stop.test.ts` | 新增 | 覆盖进度保存接口 |
| `backend/src/app/api/__tests__/drama-episodes.test.ts` | 新增 | 覆盖剧集列表接口 |
| `backend/src/services/player/player.service.test.ts` | 修改 | 从 not implemented 测试改为真实业务逻辑测试 |
| `backend/src/services/episode/episode.service.test.ts` | 修改 | 增补 drama 维度列表测试 |
| `backend/src/repositories/__tests__/playback-history.mock.repository.test.ts` | 新增 | 覆盖 upsert / 查询逻辑 |

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/dramas/[id]/episodes/route.ts` | GET | `/api/dramas/:id/episodes` | `withErrorHandler` → path param parse → `EpisodeService.listEpisodesByDramaId` | 返回指定 drama 的剧集列表 |
| `backend/src/app/api/player/progress/route.ts` | GET | `/api/player/progress` | `withErrorHandler` → query parse → header parse → `PlayerService.getPlaybackProgress` | 返回匿名身份在某 drama 下最近一次续播记录 |
| `backend/src/app/api/player/start/route.ts` | POST | `/api/player/start` | `withErrorHandler` → body parse → header parse → `PlayerService.startPlayback` | 已知 episode 下的起播确认 |
| `backend/src/app/api/player/stop/route.ts` | POST | `/api/player/stop` | `withErrorHandler` → body parse → header parse → `PlayerService.stopPlayback` | 保存当前进度 |

### 3.2 路由分组策略

- 沿用现有资源分组，不新增 `/v2` 或播放器专属路由前缀。
- 剧集列表归属 `dramas` 资源：`/api/dramas/:id/episodes`。
- 播放状态归属 `player` 资源：
  - `GET /api/player/progress`
  - `POST /api/player/start`
  - `POST /api/player/stop`
- `GET /api/episodes/[id]` 继续保持未实现；本期不把“单集详情”作为播放器 bootstrap 的依赖。

### 3.3 参数校验

```typescript
const PlaybackSessionIdHeaderSchema = z.string().uuid();

const DramaIdPathSchema = z.object({
  id: z.string().uuid(),
});

const PlayerProgressQuerySchema = z.object({
  dramaId: z.string().uuid(),
});

const PlayerStartRequestSchema = z.object({
  drama_id: z.string().uuid(),
  episode_id: z.string().uuid(),
  progress: z.number().min(0).default(0),
});

const PlayerStopRequestSchema = z.object({
  drama_id: z.string().uuid(),
  episode_id: z.string().uuid(),
  progress: z.number().min(0),
  duration: z.number().min(1),
});
```

校验规则：

- `dramaId`、`id`、`episode_id`、`X-Playback-Session-Id` 全部按 UUID 校验。
- `GET /api/player/progress` 的“无历史记录”不视为错误，返回 `200 + has_history=false`。
- `POST /api/player/start`、`POST /api/player/stop` 必须验证 `episode_id` 属于 `drama_id`。
- `POST /api/player/stop` 在 service 层对 `progress` 进行 `clamp(0, duration)`。

---

## 4. Middleware 链设计

### 4.1 请求流水线

```
请求
  -> withErrorHandler
  -> route-level zod parse (path/query/body/header)
  -> service 调用
  -> repository 调用
  -> NextResponse.json(success body)
```

### 4.2 Middleware 清单

| Middleware | 作用域 | 说明 |
|-----------|--------|------|
| `withErrorHandler` | 路由级 | 继续作为统一异常捕获入口 |
| Zod parse | 路由级 | 在 route handler 中显式解析 path/query/body/header |
| header helper（新增函数即可，不单独 middleware） | 路由级 | 读取并校验 `X-Playback-Session-Id` |

> 当前仓库没有显式的全局鉴权 / 限流链。播放器首版继续沿用现有轻量模式，不额外引入复杂 middleware 层。

### 4.3 错误传播方式

- route 层只负责参数解析与 service 编排。
- 参数错误统一抛 `AppError(ErrorCode.INVALID_PARAMS | INVALID_PLAYBACK_SESSION, ...)`。
- 业务错误由 service 抛出领域化 `AppError`：
  - `DRAMA_NOT_FOUND`
  - `EPISODE_NOT_FOUND`
  - `EPISODE_NOT_PLAYABLE`
- `withErrorHandler` 负责序列化为统一错误响应。
- ZodError 仅作为兜底；平台实现优先在 route 中把 header / query 语义化成业务错误码，而不是全部落到笼统的 `VALIDATION_ERROR`。

---

## 5. Service 层设计

### 5.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `EpisodeService.listEpisodesByDramaId` | 获取并校验剧集列表，并区分“短剧不存在”与“短剧存在但无剧集” | `dramaId` | `Episode[]` | `EpisodeRepositoryInterface`, `DramaRepositoryInterface` |
| `PlayerService.getPlaybackProgress` | 查询某匿名身份在 drama 下的最近续播记录 | `playbackSessionId`, `dramaId` | progress DTO | `PlaybackHistoryRepository`, `EpisodeRepositoryInterface`, `DramaRepositoryInterface` |
| `PlayerService.startPlayback` | 验证 episode 归属与可播放性，确认本次起播 | `playbackSessionId`, `dramaId`, `episodeId`, `progress` | start response DTO | `EpisodeRepositoryInterface`, `DramaRepositoryInterface` |
| `PlayerService.stopPlayback` | 保存最近一次播放进度 | `playbackSessionId`, `dramaId`, `episodeId`, `progress`, `duration` | stop response DTO | `EpisodeRepositoryInterface`, `DramaRepositoryInterface`, `PlaybackHistoryRepository` |

### 5.2 事务边界

| 操作组合 | 事务隔离级别 | 回滚策略 |
|---------|------------|---------|
| `stopPlayback` 验证 episode + upsert progress | 单次 repository 写入即可 | upsert 失败则整体失败，不写部分数据 |
| `startPlayback` | 无事务 | 仅做只读校验，不写播放历史 |
| `getPlaybackProgress` | 无事务 | 只读查询 |

> 当前首版不要求 `startPlayback` 写入播放历史，因此无需跨多表事务。

### 5.3 业务异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| 参数非法 | path/query/body 不合法 | 400 | `INVALID_PARAMS` |
| 播放身份非法 | 缺失或非法 `X-Playback-Session-Id` | 400 | `INVALID_PLAYBACK_SESSION` |
| 短剧不存在 | `dramaId` 未命中 | 404 | `DRAMA_NOT_FOUND` |
| 剧集不存在 | `episodeId` 未命中或不属于该 drama | 404 | `EPISODE_NOT_FOUND` |
| 剧集不可播放 | `video_url` 为空或非法 | 409 | `EPISODE_NOT_PLAYABLE` |
| 服务异常 | repository / 未知错误 | 500 | `INTERNAL_ERROR` |

### 5.4 关键业务流程

#### `getPlaybackProgress(playbackSessionId, dramaId)`

1. 校验 `dramaId` 存在。
2. 查询 `PlaybackHistoryRepository.findLatest(playbackSessionId, dramaId)`。
3. 若无记录：返回 `has_history=false`、`episode_id=null`、`start_time=0`。
4. 若有记录：
   - 校验 `episode_id` 仍存在且属于该 drama；
   - 若 episode 已不存在，则仍返回 `has_history=true` 的 raw 记录会误导客户端，因此这里直接返回 `has_history=false`，并记录 warning log；
   - 若 episode 存在，则返回 `episode_id + progress(start_time)`。

#### `startPlayback(playbackSessionId, dramaId, episodeId, progress)`

1. 校验 `dramaId` 存在。
2. 查询 `episodeId`，验证归属。
3. 校验 `video_url` 为合法可播放资源。
4. 归一化 `accepted_progress = max(progress, 0)`。
5. 返回起播确认响应，不写 `playback_history`。

#### `stopPlayback(playbackSessionId, dramaId, episodeId, progress, duration)`

1. 校验 `dramaId`、`episodeId` 归属、`duration > 0`。
2. 归一化 `savedProgress = clamp(progress, 0, duration)`。
3. upsert `playback_history`：
   - key: `(playback_session_id, drama_id)`
   - value: `episode_id`, `progress=savedProgress`, `duration`, `updated_at`
4. 返回保存结果。

---

## 6. 数据库 Migration 计划

### 6.1 变更概述

| 表名 | 操作（新建/修改/删除） | 说明 |
|------|----------------------|------|
| `playback_history` | 新建 | 存储匿名续播记录 |
| `episodes` | 不变 | 继续沿用现有 Episode 模型 |

### 6.2 DDL

```sql
CREATE TABLE playback_history (
  playback_session_id UUID NOT NULL,
  drama_id UUID NOT NULL,
  episode_id UUID NOT NULL,
  progress DOUBLE PRECISION NOT NULL DEFAULT 0,
  duration DOUBLE PRECISION,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  PRIMARY KEY (playback_session_id, drama_id)
);
```

### 6.3 字段详情

| 表名 | 字段 | 类型 | 约束 | 默认值 | 说明 |
|------|------|------|------|--------|------|
| `playback_history` | `playback_session_id` | UUID | NOT NULL | — | 匿名续播身份 |
| `playback_history` | `drama_id` | UUID | NOT NULL | — | 短剧维度主键之一 |
| `playback_history` | `episode_id` | UUID | NOT NULL | — | 最近一次观看剧集 |
| `playback_history` | `progress` | DOUBLE PRECISION | NOT NULL | `0` | 最近一次播放进度（秒） |
| `playback_history` | `duration` | DOUBLE PRECISION | NULL | `NULL` | 当前剧集总时长 |
| `playback_history` | `updated_at` | TIMESTAMP | NOT NULL | `NOW()` | 最近更新时间 |

### 6.4 索引策略

| 表名 | 索引名 | 类型（UNIQUE/INDEX） | 字段 | 用途 |
|------|--------|---------------------|------|------|
| `playback_history` | `pk_playback_history` | UNIQUE | `(playback_session_id, drama_id)` | 保证单 drama 只保留最近一条续播记录 |
| `playback_history` | `idx_playback_history_updated_at` | INDEX | `updated_at` | 后续清理历史或调试查询 |

### 6.5 回滚策略

- Mock 阶段：删除 `PlaybackHistoryMockRepository` 内存数据即可。
- 真实数据库阶段：`DROP TABLE playback_history;`。
- 若后续引入外键，可在 migration 中增加约束；首版设计不把数据库外键作为阻塞条件。

---

## 7. 后台任务/队列设计

本期不引入后台任务、异步队列或定时清理任务。

| 任务名称 | 触发条件 | 执行频率 | 队列/调度方式 | 重试策略 | 超时 |
|---------|---------|---------|-------------|---------|------|
| — | — | — | — | — | — |

原因：播放器首版只有同步查询与同步 upsert，不涉及异步转码、回调或批处理。

---

## 8. 配置与环境

| 配置项 | 环境变量 Key | 开发环境值 | 生产环境值 | 说明 |
|--------|-------------|----------|-----------|------|
| API 监听端口 | `PORT` | `3001`（沿用当前 `.env.example`） | 环境注入 | 与现有 backend 配置保持一致 |
| Supabase URL | `SUPABASE_URL` | 环境注入 | 环境注入 | 真实 repository 接入时使用 |
| Supabase Anon Key | `SUPABASE_ANON_KEY` | 环境注入 | 环境注入 | 保持与当前 config 模块一致 |
| Supabase Service Role Key | `SUPABASE_SERVICE_ROLE_KEY` | 环境注入 | 环境注入 | 管理端或绕过 RLS 场景使用 |
| Redis URL | `REDIS_URL` | `redis://localhost:6379` 或环境注入 | 环境注入 | 沿用现有 backend 配置 |
| 播放历史 repository 模式 | `PLAYER_HISTORY_REPOSITORY` | `mock` | `supabase` / 环境注入 | 设计期用于切换 mock 与真实实现，落地时需同步更新 `src/lib/config.ts` |

> ⚠️ 不新增硬编码常量；首版开发与测试默认使用 mock repository 即可，不强依赖真实数据库环境。

---

## 9. API 调用清单（调用外部服务）

本期 Backend 不调用第三方外部服务。

| 外部服务 | API 端点 | 调用时机 | 超时 | 降级策略 |
|---------|---------|---------|------|---------|
| — | — | — | — | — |

---

## 10. 跨端共享逻辑落地

| 共享逻辑 | design.md 定义 | Backend 实现方式 |
|---------|---------------|-----------------|
| Bootstrap 顺序 | `progress -> episodes -> start` | 提供两个读接口和两个事件接口；不在 `start` 中隐式做 bootstrap 决策 |
| 默认集选择 | 客户端选第一条可播放集 | Backend 只返回完整 `Episode[]` 列表，不替客户端决定默认集 |
| 恢复集选择 | 客户端优先恢复 `episode_id + start_time` | `GET /api/player/progress` 只返回最近记录；不做自动回退选择 |
| 匿名续播身份 | 通过 `X-Playback-Session-Id` 透传 UUID | route 层显式读取 header 并校验；repository 以该字段作为主查询键 |
| 切集规则 | 切集从 0 秒开始 | `start` 接口接收调用方给定的 `progress=0`，不额外查历史 |
| 无历史不是错误 | `has_history=false` | `progress` 接口 200 返回空历史结构 |
| 续播写入策略 | 退出 / 切集 / 切后台 best-effort 保存 | `stop` 接口同步 upsert 最近一次记录 |

---

## 11. 边界与错误处理

### 11.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | Zod parse + domain helper | 尽早拦截 path/query/body/header 错误 |
| Service | `AppError` | 抛出领域错误，不返回魔法值 |
| Middleware | `withErrorHandler` | 统一序列化为错误响应 |
| 日志 | `console.error` + warning 日志 | 当前仓库先沿用轻量日志，后续可接 observability |

### 11.2 错误码定义

| 业务错误码 | HTTP 状态码 | 说明 | 响应示例 |
|-----------|------------|------|---------|
| `INVALID_PARAMS` | 400 | 参数校验失败 | `{ "error": { "code": "INVALID_PARAMS", "message": "Invalid dramaId" } }` |
| `INVALID_PLAYBACK_SESSION` | 400 | header 缺失或非法 | `{ "error": { "code": "INVALID_PLAYBACK_SESSION", "message": "Missing X-Playback-Session-Id" } }` |
| `DRAMA_NOT_FOUND` | 404 | drama 不存在 | `{ "error": { "code": "DRAMA_NOT_FOUND", "message": "Drama not found" } }` |
| `EPISODE_NOT_FOUND` | 404 | episode 不存在或不归属 drama | `{ "error": { "code": "EPISODE_NOT_FOUND", "message": "Episode not found" } }` |
| `EPISODE_NOT_PLAYABLE` | 409 | 剧集无可播放资源 | `{ "error": { "code": "EPISODE_NOT_PLAYABLE", "message": "Episode has no playable resource" } }` |
| `INTERNAL_ERROR` | 500 | 服务内部错误 | `{ "error": { "code": "INTERNAL_ERROR", "message": "Internal server error" } }` |

### 11.3 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 空/非法 `dramaId` | path/query 不是 UUID | 返回 400 | route 层校验 |
| 缺失 header | 未传 `X-Playback-Session-Id` | 返回 400 | 仅 progress/start/stop 要求 |
| drama 不存在 | `dramaId` 未命中 | 返回 404 `DRAMA_NOT_FOUND` | 与空列表场景区分 |
| drama 存在但无剧集 | `EpisodeRepository.findByDramaId` 返回空列表 | `GET /api/dramas/:id/episodes` 返回 `200 + items=[]` | 由客户端进入 no-resource |
| 无历史记录 | 第一次进入某 drama | 200 + `has_history=false` | 不使用 404 |
| 恢复集已删除 | 历史记录存在，但 episode 不存在 | 200 + `has_history=false` | 记录 warning，客户端回退默认集 |
| episode 不归属 drama | 请求体 `episode_id` 与 `drama_id` 不匹配 | 404 | 避免跨剧串集 |
| episode 无资源 | `video_url` 为空/非法 | 409 | 客户端切回 no-resource / 提示 |
| stop 进度大于时长 | `progress > duration` | 服务端 clamp 后成功保存 | 避免无效进度 |
| stop 上报重复 | 多次保存同 drama 进度 | upsert 覆盖 | 首版无冲突错误 |
| repository 异常 | mock/supabase 抛错 | 500 | 交给 `withErrorHandler` 统一响应 |

### 11.4 错误日志与监控

- 记录 `playback_session_id + drama_id` 命中但 episode 缺失的 warning。
- 记录 `EPISODE_NOT_PLAYABLE` 的 episodeId，方便后续排查种子数据问题。
- 记录 Zod parse 细节，便于前端联调定位 header / body 契约错误。

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| 单元测试 | `PlayerService` / `EpisodeService` 业务逻辑 | Vitest |
| 集成测试 | Route Handler 参数解析、状态码、响应结构 | Vitest |
| Repository 测试 | `PlaybackHistoryMockRepository` upsert / query | Vitest |
| Schema 测试 | 新增 response schema、header/query/body schema | Vitest |

### 12.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| B-01 | progress 无历史 | 合法 `dramaId` + 合法 header，repo 无记录 | 200 + `has_history=false` | 集成 |
| B-02 | progress 有历史 | 合法 `dramaId` + 合法 header，repo 有记录 | 200 + `episode_id + start_time` | 集成 |
| B-03 | progress 缺失 header | 无 `X-Playback-Session-Id` | 400 + `INVALID_PLAYBACK_SESSION` | 集成 |
| B-04 | episodes 正序返回 | drama 下有多集乱序种子 | 返回按 `episode_number` 正序 | 单元 / 集成 |
| B-05 | drama 存在但无剧集 | 合法 `dramaId`，drama 存在且 episode 列表为空 | 200 + `items=[]` | 单元 / 集成 |
| B-06 | start 成功 | 合法 body + 可播放 episode | 200 + `accepted_progress` | 集成 |
| B-07 | start 剧集不归属 drama | body 中跨剧集组合 | 404 + `EPISODE_NOT_FOUND` | 集成 |
| B-08 | start 资源不可用 | `video_url=null` | 409 + `EPISODE_NOT_PLAYABLE` | 单元 / 集成 |
| B-09 | stop 保存进度 | 合法 body | 200 + `saved_progress`，repo 有更新 | 单元 / 集成 |
| B-10 | stop clamp 进度 | `progress > duration` | `saved_progress == duration` | 单元 |
| B-11 | 历史引用失效 episode | repo 中有旧历史但 episode 已删 | `has_history=false` | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| EpisodeRepository | `EpisodeMockRepository` | 提供可播放 / 不可播放 / 缺失等多类种子数据 |
| DramaRepository | `DramaMockRepository` | 校验 drama 是否存在 |
| PlaybackHistoryRepository | 新增 mock repository | 覆盖无历史 / 有历史 / upsert 更新 |
| Route Request | `NextRequest` 测试构造 | 覆盖 path/query/header/body 组合 |

---

## 13. 安全考虑

- **认证与授权**：首版不要求登录；`X-Playback-Session-Id` 不是鉴权令牌，只是续播归属键。
- **输入校验**：所有 ID 与 header 按 UUID 校验；body 使用 Zod schema。
- **敏感数据处理**：不在日志中打印完整 `playback_session_id`；必要时只打印前缀。
- **注入防护**：当前 repository 层继续使用参数化 / SDK 封装，不拼接原始 SQL 字符串。
- **资源校验**：对 `video_url` 做合法性检查，不把非法值原样透传为可播放资源。

---

## 14. 性能考虑

- **预期 QPS**：开发 / mock 阶段为主，暂无生产压测目标。
- **缓存策略**：首版不在 Backend 额外引入缓存；剧集数据量小，可直接通过 repository 查询。
- **数据库优化**：`playback_history` 以 `(playback_session_id, drama_id)` 为主键，查询复杂度稳定。
- **连接池配置**：沿用现有 Next.js / repository 默认配置。

---

## 15. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| 无 | — | — | 首版沿用现有 Next.js、zod、Vitest 能力即可 |

---

## 16. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| 现有错误码体系与 shared design 漂移 | API 契约、一致性 | 🟡 | 中 | 在 `errors.ts` 明确新增播放器域错误码 | 若来不及细分，至少先保证 message 明确 |
| mock repository 与未来真实 DB 行为不一致 | 测试可信度 | 🟡 | 中 | repository 接口先收口，再分别实现 mock / supabase | 首版先以接口测试保证语义 |
| `start` 写历史与 `stop` 写历史职责混淆 | 续播逻辑 | 🟡 | 中 | 明确只有 `stop` 写入历史，`start` 只确认起播 | 如需扩展，后续 PRD 再增量调整 |
| 失效历史记录导致客户端误恢复 | 播放主路径 | 🔴 | 中 | progress 查询阶段就过滤失效 episode | 返回 `has_history=false` |
| Episode 种子数据不足 | 端到端联调 | 🟡 | 高 | 在 mock repo 中为首页可达 drama 补 3~5 集稳定数据 | 用测试 seed 保底 |

---

## 17. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| `wiki/architecture/overview.md` | 整体架构 / 技术栈总览 | Backend 使用 Next.js Route Handlers，播放器接口当前仍为 501 |
| `wiki/features/app-shell/index.md` | Backend 端 / API 引用 | `/api/dramas` 已是首页数据源，播放器接口仍占位 |
| `wiki/features/video-player/index.md` | Backend / API 引用 | 当前 `/api/player/start`、`/api/player/stop` 未实现，Episode 是最接近的复用入口 |
| `wiki/features/data-models/index.md` | Episode / Player 请求模型 | 已有 `EpisodeSchema`、`PlayerStartRequestSchema`、`PlayerStopRequestSchema` |
| `wiki/api/player.md` | 当前实现行为 | 现有播放器 API 文档按 501 占位记录，需要本期改造 |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-26-prd-03-full-player/design.md` | shared 层定义了 bootstrap 链路、header 契约、错误码与状态机 |
| `backend/src/app/api/player/start/route.ts` | 当前起播 route 仅抛 `notImplemented` |
| `backend/src/app/api/player/stop/route.ts` | 当前 stop route 仅抛 `notImplemented` |
| `backend/src/app/api/dramas/route.ts` | 已有 route + zod parse + service 调用模式可复用 |
| `backend/src/app/api/dramas/[id]/route.ts` | 详情路由仍未实现，不纳入本期依赖 |
| `backend/src/app/api/episodes/[id]/route.ts` | episode 单条接口未实现，本期不复用 |
| `backend/src/services/drama/drama.service.ts` | 已有 service 分层模式 |
| `backend/src/services/episode/episode.service.ts` | 当前仅单集查询占位，需扩展列表查询 |
| `backend/src/services/player/player.service.ts` | 当前 start/stop service 仍未实现 |
| `backend/src/repositories/interfaces/episode.repository.interface.ts` | 已有 `findByDramaId` / `findById`，适合作为剧集查询基础 |
| `backend/src/repositories/mock/episode.mock.repository.ts` | 已有 mock repo，可承接播放器种子数据 |
| `backend/src/lib/schemas.ts` | 当前 schema 已含 Episode、PlayerStartRequest、PlayerStopRequest |
| `backend/src/lib/errors.ts` | 现有错误码体系需要扩展为播放器域语义 |
| `backend/src/middleware/error-handler.ts` | 统一错误响应出口 |
| `backend/src/services/player/player.service.test.ts` | 当前测试仍验证 not implemented，需改造为真实业务测试 |
| `backend/src/services/episode/episode.service.test.ts` | 当前 episode service 测试范围有限，需扩展 |
