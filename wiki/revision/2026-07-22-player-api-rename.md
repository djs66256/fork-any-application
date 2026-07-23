# 2026-07-22 — 播放器 API 路径重命名并新增 stop 接口

> 触发来源：用户需求

## wiki/api/player.md
- **变更类型**：新建
- **变更摘要**：创建播放器 API 文档，包含 `POST /api/player/start`（替代旧路径 `/api/video/play`）和 `POST /api/player/stop`（新增）

## wiki/api/index.md
- **变更类型**：新建
- **变更摘要**：创建 API 文档索引，添加播放器 API 域引用

## wiki/features/video-player/index.md
- **变更章节**：「API 引用」
- **变更摘要**：将内联 API 表格替换为链接引用到 `wiki/api/player.md`，新增修订历史

## wiki/index.md
- **变更章节**：「最近更新」、API 文档引用
- **变更摘要**：在全局索引中补充 API 文档区链接，更新最近更新时间戳
