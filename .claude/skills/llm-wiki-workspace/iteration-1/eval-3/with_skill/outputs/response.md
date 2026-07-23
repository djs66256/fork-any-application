# Wiki 更新完成：播放器 API 路径重命名

## 变更背景

后端播放器 API 接口路径从 `/api/video/play` 改为 `/api/player/start`，同时新增 `/api/player/stop` 接口。

## 更新的文件

### 新建文件

- `wiki/api/player.md` — 播放器 API 文档，按 `assets/api-template.md` 模板编写，包含 `POST /api/player/start`（替代旧路径 `/api/video/play`）和 `POST /api/player/stop`（新增）两个接口的完整定义
- `wiki/revision/2026-07-22-player-api-rename.md` — 本次修订记录

### 修改文件

- `wiki/features/video-player/index.md` — 将内联的「API 接口」章节改为「API 引用」章节，链接到 `wiki/api/player.md`；保留路径变更记录；新增修订历史
- `wiki/index.md` — 更新「最近更新」表格，追加本次变更记录

## 关键设计决策

- API 文档统一维护在 `wiki/api/` 下，功能文档通过链接引用，不在功能文档中重复定义 API
- 旧路径 `/api/video/play` 在 API 文档和功能文档中均保留路径变更记录，遵循「新旧共存」原则
- 由于 backend 中 player 路由代码实际尚未落地（`backend/src/app/api/player/` 目录不存在），源文件路径标注为 `[待确认]`，遵循「代码是真实来源」原则

## 注意事项

播放器的 Backend 端状态已在索引中从「📅 规划中」更新为「🚧 进行中」，反映 API 接口设计已完成的状态。
