# Wiki 收录报告：PRD-02 首页信息流

> 收录日期：2026-07-26
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/features/homepage-feed/index.md` | 新建 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 新增 PRD-02 首页信息流功能文档，收录移动端首页 Feed 状态机、Backend 首页列表接口、路由语义与范围边界 |
| `wiki/api/dramas.md` | 更新 | GET /api/dramas / POST /api/dramas / GET /api/dramas/[id] / 修订历史 | 将 Dramas API 从空骨架修正为首页 Feed 列表接口，补充分页行为、卡片字段和实际错误码 |
| `wiki/features/data-models/index.md` | 更新 | 功能概述 / 核心逻辑 / 多端实现 / 依赖关系 / 已知限制 / 修订历史 | 将 `Drama` 数据模型修正为首页卡片字段集，并补充 Android / iOS DTO 与 Entity 映射 |
| `wiki/features/app-shell/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 同步首页频道从占位页演进为 Native Feed，补充首页容器与 Backend 数据源关系 |
| `wiki/features/video-player/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 将播放器入口从首页示例按钮修正为首页 Feed 卡片动作，补充 `drama.id -> play/:id` 链路 |
| `wiki/features/index.md` | 更新 | 功能域索引 | 新增首页信息流功能入口，并同步相关功能描述 |
| `wiki/api/index.md` | 更新 | API 文档索引 | 将 Dramas API 描述修正为移动端首页 Feed 列表接口 |
| `wiki/architecture/overview.md` | 更新 | 概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史 | 将系统总览扩展到 PRD-02 首页 Feed 架构与多端范围边界 |

## 修订记录

- `wiki/revision/2026-07-26-prd-02-homepage-feed.md` 已创建

## 收录结论

- [x] ✅ 所有变更已同步到 wiki
- [ ] ⚠️ 部分内容因信息不足未收录（见下方说明）

本轮收录以代码为准、文档为辅，已同步 Backend / Android / iOS 的实现事实，并明确以下边界：

- Web 首页本期不实现 Feed，仍保持应用壳；
- `mall` / `earn` 继续由 H5 承载，不属于 Native 首页 Feed；
- 移动端设备/模拟器黑盒执行未开展，因此 wiki 已记录“待补测”的限制，但不影响本轮代码事实收录。
