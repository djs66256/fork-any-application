# Wiki 收录报告：PRD-01 底部导航与应用路由

> 收录日期：2026-07-25
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/features/app-shell/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 同步移动端 5 Tab 容器、Web 路由骨架、Android 多 back stack、iOS 独立 `NavigationPath` 与 Backend 仍无新增导航接口的现状 |
| `wiki/features/deeplink/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 修正 Android deeplink 从骨架到已接入解析+消费的状态，补充 `player` 兼容与容器未就绪排队机制 |
| `wiki/architecture/overview.md` | 更新 | 概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史 | 将系统总览更新为当前导航骨架架构 |
| `wiki/features/index.md` | 更新 | 功能域索引 | 刷新应用壳、深链、播放器三个功能域摘要 |
| `wiki/features/video-player/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 按代码事实修正为“跨端播放页占位已落地，真实播放能力未实现” |
| `wiki/api/player.md` | 更新 | `POST /api/player/start` / `POST /api/player/stop` / 参数变更记录 / 修订历史 | 移除未落地的请求体字段说明，改为记录真实 501 占位行为 |

## 修订记录

- `wiki/revision/2026-07-25-prd-01-bottom-nav.md` 已创建

## 校验说明

- 已按代码实际状态完成增量收录，重点覆盖：底部导航、多端路由、deeplink 兼容、Web 路由骨架。
- 本次变更未新增 Mermaid 图；已检查本次新增/更新文档中的相对链接均指向现有 wiki 文档路径。
- 本次收录以代码为准，未从 spec/design 反推任何尚未落地的接口行为。

## 收录结论

- [x] ✅ 所有本次 feature 直接影响的 wiki 内容已同步
- [ ] ⚠️ 部分内容因信息不足未收录（见下方说明）

当前未收录为新能力的内容：
- 设备/模拟器层面的真实黑盒导航表现仍未补测，因此 wiki 中仅能记录自动化验证现状，不能把设备级体验写成已确认结论（见 `qa-test.md`）。
- iOS 端未实现 `djsdrama://player/{id}` 历史 host 兼容；该行为并非信息缺失，而是当前代码事实，已在 deeplink 文档中明确记录。
