# 2026-07-25 — PRD-01 底部导航与应用路由 wiki 收录

> 触发来源：PRD-01 底部导航与应用路由

## wiki/features/app-shell/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：同步移动端应用壳从单页骨架演进为 5 Tab 导航容器的实现现状，补充 Web 路由骨架、Android 多 back stack、iOS 独立 `NavigationPath` 与 Backend 仍无新增导航接口的说明。

## wiki/features/deeplink/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：修正 Android deeplink 从“骨架”到“已接入解析+待执行路由消费”的状态，补充 `player` 历史别名兼容、iOS/Android 容器未就绪排队机制与跨端 canonical naming。

## wiki/architecture/overview.md
- **变更类型**：更新
- **变更章节**：概述 / 架构设计 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史
- **变更摘要**：将系统总览从早期单页骨架描述更新为当前导航骨架架构，明确移动端 5 Tab 容器、Web 路由补齐与 Backend 保持不变。

## wiki/features/index.md
- **变更类型**：更新
- **变更章节**：功能域索引
- **变更摘要**：同步应用壳、深链、播放器三个功能域的最新描述，突出 PRD-01 带来的导航骨架变化。

## wiki/features/video-player/index.md
- **变更类型**：更新
- **变更章节**：功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史
- **变更摘要**：按代码事实将播放器文档修正为“跨端播放页占位已落地、真实播放能力未实现、Backend 接口仍为 501”。

## wiki/api/player.md
- **变更类型**：更新
- **变更章节**：POST /api/player/start / POST /api/player/stop / 参数变更记录 / 修订历史
- **变更摘要**：移除未在代码中实现的请求体字段说明，改为按 Route Handler 的真实 501 占位行为记录接口现状。
