# 播放器 API 文档

> 最后更新：2026-07-25

---

## POST /api/player/start

### 功能简介

开始播放接口的预留入口。当前代码未解析请求体，也未调用 service / repository，只会统一抛出 `Errors.notImplemented('POST /api/player/start not implemented')`，因此该端点处于明确的 501 占位状态。

### 代码文件路径

`backend/src/app/api/player/start/route.ts:1-6`

### path / method

`POST /api/player/start`

### 当前实现行为

| 项 | 现状 |
|----|------|
| 请求体解析 | 未实现 |
| 参数校验 | 未实现 |
| 业务逻辑 | 未实现 |
| 成功响应 | 未实现 |
| 错误响应 | 501 Not Implemented |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |

### 备注

- 现阶段无法从代码确认任何请求体字段要求，因此旧文档中的 `drama_id`、`episode_id`、`progress` 等字段说明已移除。
- 如后续实现请求体 Schema，应以 route 层实际引入的 Zod/Schema 定义为准重新补充。

---

## POST /api/player/stop

### 功能简介

停止播放/上报播放进度接口的预留入口。当前代码未解析请求体，也未写入任何播放状态，只会统一抛出 `Errors.notImplemented('POST /api/player/stop not implemented')`。

### 代码文件路径

`backend/src/app/api/player/stop/route.ts:1-6`

### path / method

`POST /api/player/stop`

### 当前实现行为

| 项 | 现状 |
|----|------|
| 请求体解析 | 未实现 |
| 参数校验 | 未实现 |
| 业务逻辑 | 未实现 |
| 成功响应 | 未实现 |
| 错误响应 | 501 Not Implemented |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |

### 备注

- 现阶段无法从代码确认 `progress`、`duration` 或任何其它字段的必填性，因此不保留未落地的参数表。
- 后续若接入真实播放进度上报，应同时更新功能文档与 API 文档，保持与代码一致。

---

## 参数变更记录

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-25 | 移除未被代码实现的请求体字段说明，改为按真实 501 占位行为记录接口现状 |

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-25 | 更新：按真实代码将播放器 API 文档修正为“仅返回 501 的占位接口”，移除未落地的 body 参数定义 |
| 2026-07-24 | 更新：请求 body 参数对齐实际 Zod Schema（drama_id / episode_id / progress），新增 Valid PlayerStartRequestSchema / PlayerStopRequestSchema 定义 |
| 2026-07-22 | 初始创建：迁移 `/api/video/play` → `/api/player/start`，新增 `/api/player/stop` |

---

*本文档由 llm-wiki skill 自动维护。*
