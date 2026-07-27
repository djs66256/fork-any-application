# Wiki 收录报告：PRD-06 分类浏览

> 收录日期：2026-07-27
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/features/classification/index.md` | 新建 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 新增 PRD-06 分类浏览功能文档，收录搜索发现页“分类”入口、固定三维度标签矩阵、标签点击复用搜索结果页、`GET /api/dramas/tags` 与 tags 搜索扩展 |
| `wiki/api/dramas.md` | 更新 | `GET /api/dramas` / `GET /api/dramas/search` / `GET /api/dramas/hot-search` / `GET /api/dramas/tags` / 修订历史 | 补充首页/搜索共享 `tags` 字段事实；新增搜索、热搜、分类 tags 文档；将搜索命中规则扩展为 `title + category + tags` |
| `wiki/api/index.md` | 更新 | API 文档索引 | 将 Dramas API 描述扩展为首页 Feed + 搜索 + 热搜 + 排行 + 分类 tags + 预约接口均已落地 |
| `wiki/features/index.md` | 更新 | 功能域索引 | 新增“分类浏览”功能入口，并同步更新应用壳、数据模型、深链摘要中的 classification 事实 |
| `wiki/features/app-shell/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 同步首页频道从“承载 Feed + 排行”扩展为“承载 Feed + 搜索发现 + 排行 + 分类”，补充 classification 子路由与 Backend `GET /api/dramas/tags` |
| `wiki/features/deeplink/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 修订历史 | 把 `classification` 从占位 deeplink 承接修正为真实分类页入口，并同步 search/ranking/classification 三条发现链路 |
| `wiki/features/data-models/index.md` | 更新 | 功能概述 / 核心逻辑 / 多端实现 / 依赖关系 / 已知限制 / 修订历史 | 新增 classification 相关数据模型约束，包括 `ClassificationGender`、固定维度 key、`ClassificationTagsResponse`，并记录 Android / iOS DTO / Entity 对齐方式 |
| `wiki/architecture/overview.md` | 更新 | 概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史 | 将系统总览扩展到 PRD-06 分类浏览，补充搜索发现到分类页、分类标签到搜索结果页的主链路与 Native / Web 范围边界 |

## 修订记录

- `wiki/revision/2026-07-27-prd-06-classification.md` 已创建

## 校验说明

- 本轮收录以代码为准、设计/PRD 为辅，已按源文件 `path:line` 引用 Backend / Android / iOS / Web 的当前实现事实。
- 本轮新增 1 个功能文档（`wiki/features/classification/index.md`），并对 API、索引、应用壳、深链、数据模型、架构文档做增量同步。
- 已检查本次新增/更新文档中的相对链接均指向当前 worktree 内实际存在的 wiki 路径；未按主仓旧结构补建不存在的 `search-discovery` 文档，避免把分叉结构当成当前事实。
- 文档中无法从本轮代码直接确认的内容未写成已落地事实；设备级结论统一以 `qa-test.md` 的“待补黑盒验证”限制为准。

## 收录结论

- [x] ✅ 所有本次 feature 直接影响的 wiki 内容已同步
- [x] ✅ 本轮无因信息不足而故意留空的直接影响项；未落地部分均已按当前代码事实记录为限制

当前仍保留的实现限制：

- Web 端未实现真实分类页；这不是收录缺失，而是当前代码事实。
- Backend 运行时分类标签与 tags 搜索当前仍直接使用 `DramaMockRepository`；Supabase repository 已补齐能力，但 route 还未切换。
- 分类标签集合仍是代码内固定 seed，不是由真实内容后台动态生成。
- 设备/模拟器黑盒测试在本轮 workflow 中被跳过，因此 wiki 只能记录“待补测”的限制，不能把真实滑动/点击体验写成已验证结论（见 `qa-test.md` 与 `workflow.json`）。
