# API 文档分析

## 项目 API 接口总览

当前项目（ShortDrama 短剧平台）的 API 文档位于 `docs/api/` 目录下，目前只有一份 API 文档。结合 wiki 中已文档化的功能模块，项目共有以下接口：

| 方法 | 路径 | 说明 | 源文件 |
|------|------|------|--------|
| GET | `/api/health` | 健康检查，返回服务状态、时间戳和版本信息 | `backend/src/app/api/health/route.ts` |
| POST | `/api/player/start` | 启动播放（旧路径 `/api/video/play`） | `docs/api/player.md` |
| POST | `/api/player/stop` | 停止播放并上报播放进度 | `docs/api/player.md` |

---

## 播放器相关 API

播放器模块是 ShortDrama 的核心功能模块，负责短剧内容的播放、控制与交互。共有 **2 个接口**，均定义在 `docs/api/player.md` 和 `wiki/features/video-player.md` 中。

### 1. POST /api/player/start

启动播放。

- **旧路径**：`POST /api/video/play`（已于 2026-07-22 变更）
- **请求体关键字段**：`{ videoId }`（待最终确认）
- **响应体关键字段**：`{ streamUrl, duration }`（待最终确认）
- **说明**：用户点击视频卡片，跳转播放器页面后，前端向 Backend 发起此请求。Backend 校验参数后返回视频流信息，前端据此初始化播放器并开始播放。

### 2. POST /api/player/stop

停止播放。

- **请求体关键字段**：`{ videoId, progress, duration }`（待最终确认）
- **响应体关键字段**：`{ success }`（待最终确认）
- **说明**：用户退出播放或切换视频时，前端发送此请求上报播放结束状态和播放进度。

### 通用约定

- 所有播放器接口均遵循 RESTful 风格。
- 请求与响应格式均为 JSON。
- 参数校验使用 Zod。
- 播放接口需要用户身份校验（鉴权依赖）。

### 变更记录

| 日期 | 变更内容 |
| ---- | ---- |
| 2026-07-22 | 接口路径从 `/api/video/play` 变更为 `/api/player/start` |
| 2026-07-22 | 新增 `/api/player/stop` 接口 |

### 当前状态

- 播放器相关 API 的请求参数和响应结构目前标注为「待定」，需由业务需求进一步定义。
- 播放器各端代码（Web/Android/iOS）尚未初始化，当前仅完成 API 接口设计。
- Backend 端播放器路由代码（`backend/src/app/api/player/`）尚未创建，目录待确认。
