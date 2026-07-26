# 播放器 API 文档

> 最后更新：2026-07-26

---

## GET /api/player/progress

### 功能简介

查询指定 `dramaId` 在当前匿名播放会话下的最近续播信息。该接口是播放器 bootstrap 的第一步，返回是否命中历史、恢复 `episode_id` 与 `start_time`，供 Android / iOS 决定默认播放集或续播集（`backend/src/app/api/player/progress/route.ts:26-45`, `backend/src/services/player/player.service.ts:25-72`）。

### 代码文件路径

- Route：`backend/src/app/api/player/progress/route.ts:1-45`
- Service：`backend/src/services/player/player.service.ts:25-72`
- Schema：`backend/src/lib/schemas.ts:65-127`
- Error：`backend/src/lib/errors.ts:1-107`
- 测试：`backend/src/services/player/player.service.test.ts:27-118`

### path / method

`GET /api/player/progress`

### Request

#### Query 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `dramaId` | string (UUID) | 是 | 当前播放页对应的短剧 ID |

#### Headers

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `X-Playback-Session-Id` | string (UUID) | 是 | 匿名播放会话 ID，仅该接口族使用 |

### Success Response

```json
{
  "code": 0,
  "data": {
    "drama_id": "550e8400-e29b-41d4-a716-446655440001",
    "has_history": true,
    "episode_id": "660e8400-e29b-41d4-a716-446655440001",
    "start_time": 120,
    "updated_at": "2026-07-26T00:00:00Z"
  },
  "message": "ok"
}
```

### 当前行为说明

- 如果当前会话从未保存过该 drama 的播放历史，接口返回 `has_history=false`、`episode_id=null`、`start_time=0`（`backend/src/services/player/player.service.ts:31-44`）。
- 如果历史记录引用的 episode 已被删除或不再属于当前 drama，也会回退为 `has_history=false`，避免客户端恢复到无效资源（`backend/src/services/player/player.service.ts:46-59`）。
- 该接口会先校验 `dramaId` 与 `X-Playback-Session-Id` 都是合法 UUID，否则直接返回 400（`backend/src/app/api/player/progress/route.ts:12-24,27-36`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功，可能返回 `has_history=false` |
| 400 | `INVALID_PARAMS` | `dramaId` 非法 |
| 400 | `INVALID_PLAYBACK_SESSION` | header 缺失或不是合法 UUID |
| 404 | `DRAMA_NOT_FOUND` | 指定 drama 不存在 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## GET /api/dramas/:id/episodes

### 功能简介

按 drama 列出播放器所需的剧集列表。该接口是播放器 bootstrap 的第二步，也被选集面板直接复用；当前不消费 `X-Playback-Session-Id`，只返回通用剧集与资源可用性数据（`backend/src/app/api/dramas/[id]/episodes/route.ts:13-19`, `backend/src/services/episode/episode.service.ts:16-35`）。

### 代码文件路径

- Route：`backend/src/app/api/dramas/[id]/episodes/route.ts:1-20`
- Service：`backend/src/services/episode/episode.service.ts:1-45`
- Schema：`backend/src/lib/schemas.ts:32-45,103-113`
- Mock 数据：`backend/src/repositories/mock/episode.mock.repository.ts:4-106`

### path / method

`GET /api/dramas/:id/episodes`

### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string (UUID) | 是 | 短剧 UUID |

### Success Response

```json
{
  "code": 0,
  "data": {
    "drama_id": "550e8400-e29b-41d4-a716-446655440001",
    "series_status": "completed",
    "items": [
      {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "drama_id": "550e8400-e29b-41d4-a716-446655440001",
        "title": "第 1 集",
        "episode_number": 1,
        "duration": 180,
        "video_url": "https://example.com/dramas/001/episode-1.mp4",
        "thumbnail_url": "https://example.com/dramas/001/episode-1.jpg",
        "description": "第一集简介",
        "created_at": "2026-07-26T00:00:00Z",
        "updated_at": "2026-07-26T00:00:00Z"
      }
    ]
  },
  "message": "ok"
}
```

### 当前行为说明

- 返回值会先按 `episode_number` 升序排序，保证客户端选集面板与默认集解析顺序稳定（`backend/src/services/episode/episode.service.ts:22-24`）。
- `series_status` 目前由集数推导：`episode_count > 0` 返回 `completed`，否则返回 `ongoing`（`backend/src/services/episode/episode.service.ts:6-8,26-34`）。
- mock 数据中同时包含可播放 episode 与 `video_url=null` 的“暂无资源”episode，客户端据此决定 fallback 或禁用态（`backend/src/repositories/mock/episode.mock.repository.ts:4-65`）。
- 该接口不读取 `X-Playback-Session-Id`，移动端网络层也没有为它注入该 header（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:39-42`, `ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift:18-20,72-79`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `VALIDATION_ERROR` | path `id` 不是合法 UUID |
| 404 | `DRAMA_NOT_FOUND` | 指定 drama 不存在 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## POST /api/player/start

### 功能简介

在客户端已经确定目标集后开始当前集播放。该接口不再承担“为客户端决定恢复哪一集”的 bootstrap 责任，而是接收已解析好的 `drama_id + episode_id + progress`，返回服务端接受的起播信息（`backend/src/app/api/player/start/route.ts:26-47`, `backend/src/services/player/player.service.ts:74-95`）。

### 代码文件路径

- Route：`backend/src/app/api/player/start/route.ts:1-47`
- Service：`backend/src/services/player/player.service.ts:74-95`
- Schema：`backend/src/lib/schemas.ts:65-90,129-141`
- Error：`backend/src/lib/errors.ts:1-107`
- 测试：`backend/src/services/player/player.service.test.ts:67-91`

### path / method

`POST /api/player/start`

### Request

#### Headers

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `X-Playback-Session-Id` | string (UUID) | 是 | 匿名播放会话 ID |

#### Body

```json
{
  "drama_id": "550e8400-e29b-41d4-a716-446655440001",
  "episode_id": "660e8400-e29b-41d4-a716-446655440001",
  "progress": 30
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `drama_id` | string (UUID) | 是 | 当前短剧 ID |
| `episode_id` | string (UUID) | 是 | 目标剧集 ID |
| `progress` | number | 否 | 起播进度，缺省时按 0 处理 |

### Success Response

```json
{
  "code": 0,
  "data": {
    "drama_id": "550e8400-e29b-41d4-a716-446655440001",
    "episode_id": "660e8400-e29b-41d4-a716-446655440001",
    "accepted_progress": 30,
    "playback_session_id": "770e8400-e29b-41d4-a716-446655440000",
    "started_at": "2026-07-26T00:00:00.000Z"
  },
  "message": "ok"
}
```

### 当前行为说明

- `progress` 会被 `Math.max(progress, 0)` 归一化后回传为 `accepted_progress`（`backend/src/services/player/player.service.ts:84-93`）。
- 若 episode 不属于指定 drama，返回 `EPISODE_NOT_FOUND`；若 episode 没有 `video_url`，返回 `EPISODE_NOT_PLAYABLE`（`backend/src/services/player/player.service.ts:80-83,136-149`）。
- 路由层会同时校验 body 与 `X-Playback-Session-Id`，header 缺失或非法都会返回 `INVALID_PLAYBACK_SESSION`（`backend/src/app/api/player/start/route.ts:12-24,26-33`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `INVALID_PARAMS` | body 字段缺失或类型非法 |
| 400 | `INVALID_PLAYBACK_SESSION` | header 缺失或不是合法 UUID |
| 404 | `DRAMA_NOT_FOUND` | 指定 drama 不存在 |
| 404 | `EPISODE_NOT_FOUND` | 指定 episode 不存在，或不属于该 drama |
| 409 | `EPISODE_NOT_PLAYABLE` | episode 无可播放资源 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## POST /api/player/stop

### 功能简介

停止当前集播放并保存当前播放进度。播放器在返回、页面消失或切后台时都以 best-effort 方式调用该接口；服务端会把 `progress` clamp 到 `[0, duration]` 区间并持久化为最近历史（`backend/src/app/api/player/stop/route.ts:26-48`, `backend/src/services/player/player.service.ts:97-127`）。

### 代码文件路径

- Route：`backend/src/app/api/player/stop/route.ts:1-48`
- Service：`backend/src/services/player/player.service.ts:97-127`
- Schema：`backend/src/lib/schemas.ts:65-90,143-155`
- Error：`backend/src/lib/errors.ts:1-107`
- 测试：`backend/src/services/player/player.service.test.ts:93-118`

### path / method

`POST /api/player/stop`

### Request

#### Headers

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `X-Playback-Session-Id` | string (UUID) | 是 | 匿名播放会话 ID |

#### Body

```json
{
  "drama_id": "550e8400-e29b-41d4-a716-446655440001",
  "episode_id": "660e8400-e29b-41d4-a716-446655440001",
  "progress": 120,
  "duration": 180
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `drama_id` | string (UUID) | 是 | 当前短剧 ID |
| `episode_id` | string (UUID) | 是 | 当前剧集 ID |
| `progress` | number | 是 | 当前进度 |
| `duration` | number | 是 | 当前集总时长，最小值为 1 |

### Success Response

```json
{
  "code": 0,
  "data": {
    "drama_id": "550e8400-e29b-41d4-a716-446655440001",
    "episode_id": "660e8400-e29b-41d4-a716-446655440001",
    "saved_progress": 120,
    "duration": 180,
    "updated_at": "2026-07-26T00:00:00.000Z"
  },
  "message": "ok"
}
```

### 当前行为说明

- 服务端会把 `progress` clamp 到 `[0, duration]`，因此传入超长进度时会按 `duration` 保存（`backend/src/services/player/player.service.ts:14-16,107-125`）。
- 同一 `playbackSessionId + dramaId` 会覆盖之前保存的最近记录，供下次 `GET /api/player/progress` 命中（`backend/src/services/player/player.service.test.ts:103-111`）。
- 该接口本身不区分“正常退出”和“best-effort 上报”，是否忽略失败由客户端自行决定（Android / iOS 都在 ViewModel 内吞掉异常；`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt:360-377`, `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift:259-296`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `INVALID_PARAMS` | body 字段缺失或类型非法 |
| 400 | `INVALID_PLAYBACK_SESSION` | header 缺失或不是合法 UUID |
| 404 | `DRAMA_NOT_FOUND` | 指定 drama 不存在 |
| 404 | `EPISODE_NOT_FOUND` | 指定 episode 不存在，或不属于该 drama |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## 参数变更记录

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-26 | 新增 `GET /api/player/progress` 与 `GET /api/dramas/:id/episodes` 收录；`start/stop` 从 501 占位更新为可用接口；明确 `X-Playback-Session-Id` 仅用于 `progress/start/stop`，不用于 `episodes` |
| 2026-07-25 | 移除未被代码实现的请求体字段说明，改为按真实 501 占位行为记录接口现状 |

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-26 | 更新：按 PRD-03 实际代码补齐 `GET /api/player/progress`、`GET /api/dramas/:id/episodes`、`POST /api/player/start`、`POST /api/player/stop` 的首版可用契约，并明确 header 透传范围与主要错误码 |
| 2026-07-25 | 更新：按真实代码将播放器 API 文档修正为“仅返回 501 的占位接口”，移除未落地的 body 参数定义 |
| 2026-07-24 | 更新：请求 body 参数对齐实际 Zod Schema（drama_id / episode_id / progress），新增 Valid PlayerStartRequestSchema / PlayerStopRequestSchema 定义 |
| 2026-07-22 | 初始创建：迁移 `/api/video/play` → `/api/player/start`，新增 `/api/player/stop` |

---

*本文档由 llm-wiki skill 自动维护。*