# 播放器 API 文档

> 最后更新：2026-07-24

---

## POST /api/player/start

### 功能简介

开始播放视频。当前骨架阶段返回 501 Not Implemented，后续 PRD 实现完整逻辑。

### 代码文件路径

`backend/src/app/api/player/start/route.ts:L1`

### path / method

`POST /api/player/start`

### Body 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `drama_id` | string (UUID) | 是 | 短剧唯一标识 |
| `episode_id` | string (UUID) | 是 | 剧集唯一标识 |
| `progress` | number | 否 | 续播位置（秒），默认 0 |

**示例：**

```json
{
  "drama_id": "550e8400-e29b-41d4-a716-446655440000",
  "episode_id": "660e8400-e29b-41d4-a716-446655440001",
  "progress": 0
}
```

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |
| 400 | `VALIDATION_ERROR` | 参数校验失败 |

---

## POST /api/player/stop

### 功能简介

停止播放并上报播放进度。当前骨架阶段返回 501。

### 代码文件路径

`backend/src/app/api/player/stop/route.ts:L1`

### path / method

`POST /api/player/stop`

### Body 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `drama_id` | string (UUID) | 是 | 短剧唯一标识 |
| `episode_id` | string (UUID) | 是 | 剧集唯一标识 |
| `progress` | number | 是 | 当前播放位置（秒） |
| `duration` | number | 是 | 视频总时长（秒） |

**示例：**

```json
{
  "drama_id": "550e8400-e29b-41d4-a716-446655440000",
  "episode_id": "660e8400-e29b-41d4-a716-446655440001",
  "progress": 45.5,
  "duration": 180
}
```

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |
| 400 | `VALIDATION_ERROR` | 参数校验失败 |

---

## 参数变更记录

| 旧字段 | 新字段 | 变更时间 | 说明 |
|--------|--------|---------|------|
| `videoId` | `drama_id` + `episode_id` | 2026-07-24 | 按数据模型拆分，同时关联短剧和剧集 |

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 更新：请求 body 参数对齐实际 Zod Schema（drama_id / episode_id / progress），新增 Valid PlayerStartRequestSchema / PlayerStopRequestSchema 定义 |
| 2026-07-22 | 初始创建：迁移 `/api/video/play` → `/api/player/start`，新增 `/api/player/stop` |

---

*本文档由 llm-wiki skill 自动维护。*
