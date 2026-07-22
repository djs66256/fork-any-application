---
name: llm-wiki
description: >
  LLM-wiki 是 agent 自身维护的项目全功能文档。本 skill 提供 llm-wiki 的完整规范，
  覆盖文档生成、增量更新、查阅三个核心场景。
  触发场景：用户提到"wiki"、"项目文档"、"功能文档"、"查一下XX功能"、"这个功能怎么实现的"、"XX模块是什么"；需求启动前需要了解现有功能时；
  技术方案设计时需要查阅现有能力时；任何涉及项目功能逻辑的问题询问；
  开发完成后自动触发增量更新。只要用户想了解当前项目已有功能、逻辑、技术方案，
  都应优先查阅 llm-wiki。
---

# LLM-wiki

## 定位

llm-wiki 是 agent 自行维护的、随项目演进持续更新的全功能文档。它不是面向人类的一次性交付物，而是 agent 的**长期记忆**——让每次新会话都能快速理解项目「当前有什么功能、各功能如何实现、各端之间如何协作」。

没有 llm-wiki 时，agent 每次都要从零阅读代码推断逻辑；有了 llm-wiki 之后，agent 可以直接定位到目标文档，获取结构化、可验证的项目功能信息。

## 能力线

| 能力线 | 职责 | 触发时机 | 执行方式 | 规范 |
|--------|------|---------|---------|------|
| **查阅** | 按用户意图定位并读取相关 wiki 文档 | 需求启动前、方案设计、功能咨询 | 主 agent 直接执行 | [references/query-flow.md](references/query-flow.md) |
| **生成** | 从代码工程中提取信息，生成首版 wiki | 工程首次接入 llm-wiki | subagent | [references/generate-and-update.md](references/generate-and-update.md) |
| **更新** | 开发完成后增量同步变更到 wiki | 每次完成开发任务后自动触发 | subagent | [references/generate-and-update.md](references/generate-and-update.md) |

## 目录结构与文档格式

Wiki 的完整目录结构、功能域名约定、功能文档章节规范、拆分策略、索引维护、各端状态标识，详见 [references/wiki-standards.md](references/wiki-standards.md)。

## 查阅

何时查阅、查阅流程、查阅原则，详见 [references/query-flow.md](references/query-flow.md)。

## 模板文件

| 模板 | 路径 | 用途 |
|------|------|------|
| 功能文档模板 | `assets/feature-template.md` | 新建功能文档时参照 |
| API 文档模板 | `assets/api-template.md` | 新建 API 文档时参照 |
| 索引模板 | `assets/index-template.md` | 新建 wiki/index.md 时参照 |
| 架构文档模板 | `assets/architecture-template.md` | 新建架构文档时参照 |
| 技术决策模板 | `assets/decisions-template.md` | 新建决策记录时参照 |
