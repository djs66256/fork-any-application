# Backend 端技术方案：PRD-07 菜单面板

> 创建日期：2026-07-27
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

本期 Backend 端在现有 `player` 资源域内新增只读接口 `GET /api/player/recently-viewed`，为移动端菜单面板提供“最近在看”摘要数据。实现继续沿用当前仓库已落地的四层结构：Route → Service → Repository → Infrastructure / Shared，不引入新依赖，不改动现有 `progress/start/stop` 路由与匿名播放会话语义。

```text
GET /api/player/recently-viewed
  -> Route Handler (`app/api/player/recently-viewed/route.ts`)
     -> withErrorHandler
     -> parsePlaybackSessionId(request)
     -> PlayerService.getRecentlyViewed(playbackSessionId)
        -> PlaybackHistoryRepository.listRecentBySession(playbackSessionId, fetchLimit)
           -> Mock: in-memory Map values -> filter/sort/take(fetchLimit)
           -> Supabase: playback_history by session -> order(updated_at desc) -> limit(fetchLimit)
        -> for each history:
             DramaRepository.findById(history.drama_id)
             EpisodeRepository.findById(history.episode_id)
        -> filter invalid drama/episode rows
        -> take first 3 valid items
        -> RecentlyViewedResponseSchema.parse({ code, data.items, message })
     -> JSON response
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| `backend/src/app/api/player/progress/route.ts` | 不变 / 参考复用 | 复用同样的 `parsePlaybackSessionId` 校验模式 |
| `backend/src/app/api/player/start/route.ts` | 不变 | 继续负责播放开始 |
| `backend/src/app/api/player/stop/route.ts` | 不变 | 继续负责播放停止与历史写入 |
| `backend/src/app/api/player/recently-viewed/route.ts` | 新增 | 新增菜单面板最近在看接口 |
| `backend/src/services/player/player.service.ts` | 扩展 | 新增 `getRecentlyViewed(playbackSessionId)`，在候选历史窗口内过滤脏数据后返回最近在看摘要 |
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | 扩展 | 增加 `listRecentBySession(playbackSessionId, limit)` |
| `backend/src/repositories/mock/playback-history.mock.repository.ts` | 扩展 | 支持按 session 列出最近记录 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | 扩展 | 支持 Supabase 路径按 session + updated_at desc 取最近记录 |
| `backend/src/lib/schemas.ts` | 扩展 | 新增 recently-viewed response schema |
| `backend/src/lib/errors.ts` | 不变 | 继续复用 `INVALID_PLAYBACK_SESSION` / `INTERNAL_ERROR` / `SERVICE_UNAVAILABLE` |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 不变 | 继续复用 `findById` |
| `backend/src/repositories/interfaces/episode.repository.interface.ts` | 不变 | 继续复用 `findById` |

### 1.2 设计原则

- 最近在看归属 `player` 领域，而非 `user` 领域。
- 成功响应沿用当前 `player` 域已使用的 `{ code, data, message }` 风格；失败响应继续统一为 `{ error: { code, message } }`。
- 不新增 migration；首版直接基于现有 `playback_history` 表与当前 `(playback_session_id, drama_id)` 唯一 latest 语义聚合。
- `PlaybackHistoryRepository` 只负责返回历史记录，不在仓储层直接 join drama / episode；聚合职责仍集中在 `PlayerService`。
- 缺失 drama / episode 的历史记录在 service 层过滤，不向客户端泄漏脏数据。

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `backend/src/app/api/player/recently-viewed/route.ts` | 新增 | 注册 `GET /api/player/recently-viewed`，解析 header 并调用 service |
| `backend/src/services/player/player.service.ts` | 修改 | 新增 `getRecentlyViewed(playbackSessionId)` |
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | 修改 | 增加 `listRecentBySession(playbackSessionId, limit)` 接口 |
| `backend/src/repositories/mock/playback-history.mock.repository.ts` | 修改 | 在内存数据上实现排序 + 截断 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | 修改 | 增加 `order('updated_at', { ascending: false }).limit(limit)` 查询 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 `RecentlyViewedItemSchema` / `RecentlyViewedResponseSchema` |
| `backend/src/app/api/__tests__/player-recently-viewed.test.ts` | 新增 | 覆盖 header 缺失/非法、成功、空数组、过滤无效记录 |
| `backend/src/services/player/player.service.test.ts` | 修改 | 覆盖最近在看聚合、排序、过滤、空态 |
| `backend/src/repositories/__tests__/playback-history.mock.repository.test.ts` | 新增 / 修改 | 覆盖 list recent 排序与 limit |
| `backend/src/repositories/supabase/__tests__/playback-history.supabase.repository.test.ts` | 新增 / 修改 | 覆盖 Supabase 查询参数与错误处理 |
| `backend/src/lib/__tests__/schemas.test.ts` | 修改 | 增加 recently-viewed schema 测试 |

> 当前阶段只输出设计文档，不直接修改实现文件。

---

## 3. API 路由设计

### 3.1 路由注册

| 路由文件 | HTTP 方法 | URL 路径 | 中间件链 | 说明 |
|---------|----------|---------|---------|------|
| `backend/src/app/api/player/recently-viewed/route.ts` | `GET` | `/api/player/recently-viewed` | `withErrorHandler` + Route 内 header 校验 | 返回菜单面板最近在看摘要 |
| `backend/src/app/api/player/progress/route.ts` | `GET` | `/api/player/progress` | 现有实现 | 卡片点击进入播放器后的续播点恢复 |
| `backend/src/app/api/player/start/route.ts` | `POST` | `/api/player/start` | 现有实现 | 播放开始 |
| `backend/src/app/api/player/stop/route.ts` | `POST` | `/api/player/stop` | 现有实现 | 播放停止并写入历史 |

### 3.2 路由样板

```typescript
import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { PlaybackSessionIdHeaderSchema } from '@/lib/schemas';
import { Errors } from '@/lib/errors';

function parsePlaybackSessionId(request: NextRequest): string {
  const playbackSessionId = request.headers.get('X-Playback-Session-Id');
  if (!playbackSessionId) {
    throw Errors.invalidPlaybackSession('Missing X-Playback-Session-Id');
  }

  const parsed = PlaybackSessionIdHeaderSchema.safeParse(playbackSessionId);
  if (!parsed.success) {
    throw Errors.invalidPlaybackSession('Invalid X-Playback-Session-Id');
  }

  return parsed.data;
}

export const GET = withErrorHandler(async function GET(request: NextRequest) {
  const playbackSessionId = parsePlaybackSessionId(request);
  const service = createPlayerService();
  const response = await service.getRecentlyViewed(playbackSessionId);
  return NextResponse.json(response);
});
```

### 3.3 响应契约

成功响应：

```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "drama_id": "550e8400-e29b-41d4-a716-446655440001",
        "title": "逆袭归来后我成了豪门团宠",
        "cover_url": "https://example.com/dramas/001.jpg",
        "episode_number": 12,
        "progress": 128.5,
        "updated_at": "2026-07-27T15:20:00.000Z"
      }
    ]
  },
  "message": "ok"
}
```

错误响应：

```json
{
  "error": {
    "code": "INVALID_PLAYBACK_SESSION",
    "message": "Invalid X-Playback-Session-Id"
  }
}
```

### 3.4 参数约束

| 参数 | 来源 | 规则 | 说明 |
|------|------|------|------|
| `X-Playback-Session-Id` | Header | UUID，必填 | 与当前 player 接口保持一致 |
| `RECENTLY_VIEWED_LIMIT` | 内部常量 | 固定 3 | 响应最多 3 条有效摘要 |
| `RECENTLY_VIEWED_FETCH_LIMIT` | 内部常量 | 固定大于 3（首版建议 10） | 读取候选历史窗口并过滤脏数据，不对外暴露 query |

---

## 4. Service 层设计

### 4.1 Service 清单

| Service | 职责 | 输入 | 输出 | 依赖 |
|---------|------|------|------|------|
| `PlayerService.getPlaybackProgress` | 既有续播点查询 | `playbackSessionId, dramaId` | `PlayerProgressResponse` | drama / episode / history repo |
| `PlayerService.startPlayback` | 既有开始播放 | `playbackSessionId, dramaId, episodeId, progress` | `PlayerStartResponse` | drama / episode / history repo |
| `PlayerService.stopPlayback` | 既有停止播放 | `playbackSessionId, dramaId, episodeId, progress, duration` | `PlayerStopResponse` | drama / episode / history repo |
| `PlayerService.getRecentlyViewed` | 新增最近在看聚合 | `playbackSessionId` | `RecentlyViewedResponse` | drama / episode / history repo |

### 4.2 方法设计

```typescript
const RECENTLY_VIEWED_LIMIT = 3;
const RECENTLY_VIEWED_FETCH_LIMIT = 10;

async getRecentlyViewed(playbackSessionId: string): Promise<RecentlyViewedResponse> {
  const histories = await this.playbackHistoryRepository.listRecentBySession(
    playbackSessionId,
    RECENTLY_VIEWED_FETCH_LIMIT,
  );

  const items = [];
  for (const history of histories) {
    const [drama, episode] = await Promise.all([
      this.dramaRepository.findById(history.drama_id),
      this.episodeRepository.findById(history.episode_id),
    ]);

    if (!drama || !episode || episode.drama_id !== history.drama_id) {
      continue;
    }

    items.push({
      drama_id: drama.id,
      title: drama.title,
      cover_url: drama.cover_url,
      episode_number: episode.episode_number,
      progress: history.progress,
      updated_at: history.updated_at,
    });

    if (items.length >= RECENTLY_VIEWED_LIMIT) {
      break;
    }
  }

  return RecentlyViewedResponseSchema.parse({
    code: 0,
    data: { items },
    message: 'ok',
  });
}
```

### 4.3 关键规则

| 规则 | 落地方式 |
|------|---------|
| 返回上限 3 条 | service 在过滤后截断到 `RECENTLY_VIEWED_LIMIT=3`，response schema `.max(3)` 兜底 |
| 候选窗口大于 3 | repository 先取 `RECENTLY_VIEWED_FETCH_LIMIT` 条原始 history，覆盖“最近几条里混有脏记录”的常见场景 |
| 排序按最近观看时间倒序 | repository 负责 `updated_at desc` |
| 同一 drama 不重复 | 依赖现有 `upsert onConflict(playback_session_id, drama_id)` 语义 |
| 失效记录不下发 | service 聚合时过滤 `drama` / `episode` 缺失或不匹配情况；过滤后允许少于 3 条 |
| 空态返回成功 | `items=[]` 仍返回 `code:0,message:'ok'` |

### 4.4 异常定义

| 异常类型 | 触发条件 | HTTP 状态码 | 错误码 |
|---------|---------|-----------|--------|
| Header 缺失 / 非法 | route 校验失败 | 400 | `INVALID_PLAYBACK_SESSION` |
| repository 异常 | 数据源查询失败 | 500 / 503 | `INTERNAL_ERROR` / `SERVICE_UNAVAILABLE` |
| schema 校验失败 | service 输出不合法 | 500 | `INTERNAL_ERROR` |

---

## 5. Repository 层设计

### 5.1 接口扩展

```typescript
export interface PlaybackHistoryRepositoryInterface {
  findLatest(playbackSessionId: string, dramaId: string): Promise<PlaybackHistory | null>;
  listRecentBySession(playbackSessionId: string, limit: number): Promise<PlaybackHistory[]>;
  upsert(input: UpsertPlaybackHistoryInput): Promise<PlaybackHistory>;
}
```

### 5.2 Mock Repository

实现策略：
- 继续使用 `Map<string, PlaybackHistory>`，key 为 `${playbackSessionId}:${dramaId}`；
- `listRecentBySession` 遍历 `Map.values()`，筛选同 session 记录，按 `updated_at desc` 排序，取前 `limit` 条；
- 返回 clone 后的数组，避免测试侧意外修改内部状态。

示意：

```typescript
async listRecentBySession(playbackSessionId: string, limit: number): Promise<PlaybackHistory[]> {
  return Array.from(this.data.values())
    .filter((item) => item.playback_session_id === playbackSessionId)
    .sort((a, b) => b.updated_at.localeCompare(a.updated_at))
    .slice(0, limit)
    .map(clonePlaybackHistory);
}
```

### 5.3 Supabase Repository

实现策略：
- 查询表：`playback_history`
- 过滤：`.eq('playback_session_id', playbackSessionId)`
- 排序：`.order('updated_at', { ascending: false })`
- 截断：`.limit(limit)`
- 继续使用 `PLAYBACK_HISTORY_SELECT_COLUMNS` 做字段选择，并通过 `PlaybackHistorySchema` 兜底校验。

示意：

```typescript
async listRecentBySession(playbackSessionId: string, limit: number): Promise<PlaybackHistory[]> {
  const supabase = getSupabaseAdmin();
  const { data, error } = await supabase
    .from('playback_history')
    .select(PLAYBACK_HISTORY_SELECT_COLUMNS)
    .eq('playback_session_id', playbackSessionId)
    .order('updated_at', { ascending: false })
    .limit(limit);

  if (error) {
    throw Errors.internal(`Failed to fetch recent playback history: ${error.message}`);
  }

  return (data ?? []).map(mapRowToPlaybackHistory);
}
```

### 5.4 不新增 migration 的依据

| 结论 | 说明 |
|------|------|
| 不新增表 | 最近在看直接消费现有 `playback_history` |
| 不新增索引设计文档 | 当前 `limit=3` 且按 session 查询，首版复杂度可控 |
| 不变更写入模型 | 继续复用 `stopPlayback -> upsert(playback_session_id, drama_id)` |

---

## 6. Schema 设计

在 `backend/src/lib/schemas.ts` 新增：

```typescript
export const RecentlyViewedItemSchema = z.object({
  drama_id: z.string().uuid(),
  title: z.string().min(1),
  cover_url: z.string().url().nullable().default(null),
  episode_number: z.number().int().min(1),
  progress: z.number().min(0),
  updated_at: z.string(),
});

export const RecentlyViewedResponseSchema = z.object({
  code: z.literal(0),
  data: z.object({
    items: z.array(RecentlyViewedItemSchema).max(3),
  }),
  message: z.string(),
});
```

说明：
- `cover_url` 允许为空，与当前 `DramaSchema` 保持一致；
- `updated_at` 继续沿用字符串时间戳，和现有 player response 对齐；
- `items` 为空数组时也合法。

---

## 7. 边界与错误处理

### 7.1 全局错误处理架构

| 层级 | 机制 | 说明 |
|------|------|------|
| Route | `withErrorHandler` + header 校验 | 统一输出 400 / 500 错误 |
| Service | schema parse + 过滤脏数据 | 防止无效历史污染响应 |
| Repository | `Errors.internal()` / `Errors.serviceUnavailable()` | 对基础设施异常做领域化包装 |

### 7.2 边界场景

| 场景 | 触发条件 | API 行为 | 说明 |
|------|---------|---------|------|
| 无历史 | 新 session | 200 + `items=[]` | 客户端展示空态 |
| 只有 1~2 条有效记录 | 候选窗口内有效项不足 3 条 | 返回现有条数 | 不补空项 |
| 最近若干条混有脏数据 | 固定候选窗口里存在失效 drama / episode | 过滤无效项后返回剩余有效摘要 | 不承诺跨窗口补足到 3 条 |
| header 缺失 | 客户端实现异常 | 400 + `INVALID_PLAYBACK_SESSION` | 与现有 progress 接口一致 |
| header 非 UUID | 非法 header | 400 + `INVALID_PLAYBACK_SESSION` | 不返回 Zod 细节 |
| drama 缺失 | 脏历史 | 过滤该项 | 其余项正常返回 |
| episode 缺失 / drama 不匹配 | 脏历史 | 过滤该项 | 其余项正常返回 |
| repository 故障 | Supabase 报错 | 500 / 503 | 客户端区块错误态 |

---

## 8. 测试策略

### 8.1 测试范围

| 测试类型 | 覆盖内容 | 框架/工具 |
|---------|---------|----------|
| Route 测试 | header 缺失/非法、成功、空态 | Vitest |
| Service 测试 | 排序、聚合、过滤脏数据、空数组 | Vitest |
| Repository 测试 | mock / supabase list recent 查询 | Vitest |
| Schema 测试 | recently-viewed response schema | Vitest |

### 8.2 关键测试场景

| 编号 | 测试场景 | 输入 | 预期输出 | 测试类型 |
|------|---------|------|---------|---------|
| BE-T01 | 合法 session 返回最近 3 条 | 4 条 history，更新时间不同 | 返回 3 条，按 `updated_at desc` | Service / Repository |
| BE-T02 | 无历史 | 空 session | `code=0,data.items=[]` | Route / Service |
| BE-T03 | 缺失 header | 无 `X-Playback-Session-Id` | 400 + `INVALID_PLAYBACK_SESSION` | Route |
| BE-T04 | 非法 header | `abc` | 400 + `INVALID_PLAYBACK_SESSION` | Route |
| BE-T05 | 最近几条混有脏数据 | 固定候选窗口内包含失效 drama/episode | 过滤后返回剩余有效摘要，允许不足 3 条 | Service |
| BE-T06 | drama 缺失 | history 指向不存在 drama | 该项被过滤 | Service |
| BE-T07 | episode 缺失 | history 指向不存在 episode | 该项被过滤 | Service |
| BE-T08 | `cover_url=null` | drama 无封面 | schema 通过，正常返回 | Schema / Service |
| BE-T09 | repository 异常 | Supabase error | 500 / `INTERNAL_ERROR` | Repository / Route |

### 8.3 不在本期测试范围

- 线上 Supabase 压测；
- 监控与埋点；
- 真实客户端联调黑盒验证（放到 workflow 后续 QA 阶段）。

---

## 9. 参考资料

| 文件 | 关键内容 |
|------|---------|
| `docs/specs/2026-07-27-prd-07-menu-panel/spec.md` | 最近在看接口边界与菜单面板行为 |
| `docs/specs/2026-07-27-prd-07-menu-panel/design.md` | shared contract、状态机、错误语义 |
| `backend/src/app/api/player/progress/route.ts` | header 校验与 route 样板 |
| `backend/src/services/player/player.service.ts` | 现有 player service 编排方式 |
| `backend/src/repositories/interfaces/playback-history.repository.interface.ts` | history repository 扩展位置 |
| `backend/src/repositories/mock/playback-history.mock.repository.ts` | mock repository 扩展位置 |
| `backend/src/repositories/supabase/playback-history.supabase.repository.ts` | Supabase repository 扩展位置 |
| `backend/src/lib/schemas.ts` | player response schema 风格与新增 schema 落点 |
| `backend/src/lib/errors.ts` | `INVALID_PLAYBACK_SESSION` / `INTERNAL_ERROR` 等错误码 |
