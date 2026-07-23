# API 文档概览

## Wiki 中的 API 文档状态

当前项目 wiki 中没有独立的 API 文档目录（`wiki/api/index.md` 不存在），API 接口信息分散在各功能文档中。以下是所有已记录的 API 接口：

### 全部 API 接口

| 方法 | 路径 | 说明 | 所属功能 | 源文件 | 实际代码状态 |
|------|------|------|---------|--------|------------|
| GET | `/api/health` | 健康检查，返回服务状态、时间戳、版本号 | 健康检查 | `backend/src/app/api/health/route.ts` | ✅ 已实现 |
| POST | `/api/player/start` | 开始播放视频，返回视频流信息 | 播放器 | 未实现 | ❌ 仅文档设计 |
| POST | `/api/player/stop` | 停止播放并上报播放进度 | 播放器 | 未实现 | ❌ 仅文档设计 |

---

## 播放器相关的 API

播放器相关 API 定义在 `wiki/features/video-player.md` 中，共 2 个接口：

### 1. POST `/api/player/start`

- **说明**：开始播放视频
- **旧路径**：`/api/video/play`（已于 2026-07-22 变更）
- **请求体关键字段**：`{ videoId }`
- **响应体关键字段**：`{ streamUrl, duration }`
- **代码实现状态**：❌ 未实现。后端 `backend/src/app/api/` 下没有 player 相关路由文件。

### 2. POST `/api/player/stop`

- **说明**：停止播放并上报播放进度
- **请求体关键字段**：`{ videoId, progress, duration }`
- **响应体关键字段**：`{ success }`
- **代码实现状态**：❌ 未实现。后端 `backend/src/app/api/` 下没有 player 相关路由文件。

---

## 关键发现

1. **API 文档不完整**：`wiki/api/index.md` 不存在，API 信息散落在功能文档中而非集中管理。
2. **播放器 API 仅有设计文档，无代码实现**：wiki 中描述的两个播放器接口（start/stop）在后端代码中均未落地。后端实际仅有一个 API 路由文件：`backend/src/app/api/health/route.ts`。
3. **播放器各端均处于规划状态**：wiki index 中播放器功能域在 Web、Android、iOS、Backend 四端均标记为 📅 规划中。
4. **存在路径变更记录**：播放器 API 曾从 `/api/video/play` 变更为 `/api/player/start`，但 wiki 中未标记旧路径是否仍兼容。
