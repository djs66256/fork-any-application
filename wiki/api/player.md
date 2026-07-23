# 播放器 API 文档

> 最后更新：2026-07-22

---

## POST /api/player/start

### 功能简介

开始播放视频。该接口替代了旧的 `/api/video/play` 路径。

### 代码文件路径

`backend/src/app/api/player/start/route.ts` [待确认]

### path / method

`POST /api/player/start`

### Body 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `videoId` | string | 是 | 视频唯一标识 |

**示例：**

```json
{
  "videoId": "vid_12345"
}
```

### Response

```json
{
  "streamUrl": "https://cdn.example.com/videos/vid_12345/master.m3u8",
  "duration": 180
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `streamUrl` | string | 视频流地址 |
| `duration` | number | 视频时长（秒） |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `INVALID_PARAMS` | 参数校验失败 |
| 401 | `UNAUTHORIZED` | 未登录 |
| 404 | `NOT_FOUND` | 视频不存在 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

### 路径变更记录

| 旧路径 | 新路径 | 变更时间 |
|--------|--------|---------|
| `/api/video/play` | `/api/player/start` | 2026-07-22 |

---

## POST /api/player/stop

### 功能简介

停止播放并上报播放进度。

### 代码文件路径

`backend/src/app/api/player/stop/route.ts` [待确认]

### path / method

`POST /api/player/stop`

### Body 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `videoId` | string | 是 | 视频唯一标识 |
| `progress` | number | 是 | 当前播放进度（秒） |
| `duration` | number | 是 | 视频总时长（秒） |

**示例：**

```json
{
  "videoId": "vid_12345",
  "progress": 45.5,
  "duration": 180
}
```

### Response

```json
{
  "success": true
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | boolean | 操作是否成功 |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `INVALID_PARAMS` | 参数校验失败 |
| 401 | `UNAUTHORIZED` | 未登录 |
| 404 | `NOT_FOUND` | 视频不存在 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-22 | 初始创建：迁移 `/api/video/play` → `/api/player/start`，新增 `/api/player/stop` |

---

*本文档由 llm-wiki skill 自动维护。*
