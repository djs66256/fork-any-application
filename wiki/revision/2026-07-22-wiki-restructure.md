# 2026-07-22 — 目录重构与完整 wiki 生成

> 触发来源：用户需求（从各端代码提取信息，生成完整项目 wiki）

## wiki/index.md
- **变更章节**：「API 文档」「架构专题」「技术决策」「最近更新」
- **变更摘要**：更新全局索引，补充 api/、architecture/、decisions/ 三个子系统的链接和详情表格

## wiki/features/index.md
- **变更类型**：新建
- **变更摘要**：创建功能域索引，列出全部 14 个功能域及各端实现状态

## wiki/features/app-shell/index.md
- **变更类型**：迁移（原 `wiki/features/app-shell.md`）
- **变更摘要**：按 wiki 标准将功能文档移入子目录

## wiki/features/data-models/index.md
- **变更类型**：迁移（原 `wiki/features/data-models.md`）
- **变更摘要**：按 wiki 标准将功能文档移入子目录

## wiki/features/health-check/index.md
- **变更类型**：迁移（原 `wiki/features/health-check.md`）
- **变更摘要**：按 wiki 标准将功能文档移入子目录

## wiki/features/video-player/index.md
- **变更类型**：迁移 + 更新（原 `wiki/features/video-player.md`）
- **变更章节**：「API 引用」
- **变更摘要**：按 wiki 标准将功能文档移入子目录，API 章节改为引用链接指向 wiki/api/player.md

## wiki/features/deeplink/index.md
- **变更类型**：迁移（原 `wiki/features/deeplink.md`）
- **变更摘要**：按 wiki 标准将功能文档移入子目录

## wiki/api/index.md
- **变更类型**：新建
- **变更摘要**：创建 API 文档索引，列出健康检查和播放器两个 API 域

## wiki/api/health.md
- **变更类型**：新建
- **变更摘要**：从 `backend/src/app/api/health/route.ts` 提取信息，创建健康检查 API 文档

## wiki/api/player.md
- **变更类型**：从 docs/api/player.md 迁移并补全
- **变更摘要**：按 API 模板格式创建播放器 API 文档，包含 POST /api/player/start 和 POST /api/player/stop

## wiki/architecture/index.md
- **变更类型**：新建
- **变更摘要**：创建架构文档索引

## wiki/architecture/overview.md
- **变更类型**：新建
- **变更摘要**：从各端代码提取技术栈、架构设计、核心流程调用栈，创建系统总览架构文档

## wiki/decisions/index.md
- **变更类型**：新建
- **变更摘要**：创建技术决策索引（当前无正式决策记录）
