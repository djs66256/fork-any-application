# Wiki 收录报告：PRD-05 排行体系

> 收录日期：2026-07-27
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/features/ranking/index.md` | 新建 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 新增 PRD-05 排行体系功能文档，收录搜索发现入口、双层 Tab 榜单浏览、分页、预约拦截、`play` 路由复用与 Native/Web 范围边界 |
| `wiki/api/dramas.md` | 更新 | `GET /api/dramas/rankings` / `POST /api/dramas/[id]/book` / 修订历史 | 补充排行列表与预约接口的 canonical contract、认证、幂等行为与当前 mock runtime 事实 |
| `wiki/features/data-models/index.md` | 更新 | 功能概述 / 核心逻辑 / 多端实现 / 依赖关系 / 已知限制 / 修订历史 | 新增 `RankingDrama`、`RankingQuery`、`BookDramaResponse` 等排行体系模型，并补充 Android / iOS 对齐方式 |
| `wiki/features/app-shell/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 同步首页频道从承载首页 Feed 扩展为承载搜索发现与真实排行页 |
| `wiki/features/video-player/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 将播放器入口从“首页 Feed 卡片”扩展为“首页 Feed + 排行卡片” |
| `wiki/features/deeplink/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 修订历史 | 将 `ranking` 从占位 deeplink 入口修正为真实排行页入口，并补充发现链路 host |
| `wiki/features/index.md` | 更新 | 功能域索引 | 新增排行体系功能入口，并同步相关功能摘要 |
| `wiki/api/index.md` | 更新 | API 文档索引 | 将 Dramas API 描述扩展为首页 Feed + 排行 + 预约接口均已落地 |
| `wiki/architecture/overview.md` | 更新 | 概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史 | 将系统总览扩展到 PRD-05 排行体系与多端范围边界 |

## 修订记录

- `wiki/revision/2026-07-27-prd-05-ranking.md` 已创建

## 校验说明

- 本轮收录以代码为准、设计/PRD 为辅，已按源文件 `path:line` 引用 Backend / Android / iOS / Web 的当前实现事实。
- 本轮新增 2 个 Mermaid 流程图（均位于 `wiki/features/ranking/index.md`），内容与已落地代码一致，未引入超出实现范围的节点。
- 已检查本次新增/更新文档中的相对链接均指向现有 wiki 文档路径。
- 文档中无法从代码直接确认的细节已显式标记 `[待确认]`，未将推测写成已落地事实。

## 收录结论

- [x] ✅ 所有本次 feature 直接影响的 wiki 内容已同步
- [ ] ⚠️ 部分内容因信息不足未收录（见下方说明）

当前仍保留为限制或待后续 workflow 继续推进的内容：

- Web 端 `/rankings` 仍为占位页；这不是收录缺失，而是当前代码事实。
- Backend 排行与预约运行时仍直接使用 `DramaMockRepository`，Supabase migration 已存在但未接入 route。
- 预约认证仍是 skeleton auth，尚未接入真实登录 / JWT 校验；wiki 已按代码事实记录。
- 设备/模拟器黑盒测试在本轮 workflow 中被跳过，因此 wiki 仅能记录“待补测”的限制，不能把设备级体验写成已确认结论（见 `qa-test.md` 与 `workflow.json`）。
