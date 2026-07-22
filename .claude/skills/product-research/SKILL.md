---
name: product-research
description: >
  竞品业务功能文档化 skill。
  触发场景：用户提到"竞品分析"、"分析竞品"、"product research"、"对比竞品"、
  "看下XX产品"、"调研XX功能"、"XX是怎么做的"、"爬一下XX的业务逻辑"、
  "整理XX的完整交互流程"等。只要涉及操作竞品应用并产出分析文档，都应使用本 skill。
---

# 竞品业务功能文档化

## 定位

本 skill 不是做一次性的「操作录屏 + 简单记录」，而是系统化地将竞品应用的**业务逻辑、交互行为、UI 样式**爬取并整理为完整的结构化文档，供自身产品团队持续参考。

## 能力线

本 skill 整合四类能力，各司其职：

| 能力线 | 职责 | 落地方式 |
|--------|------|---------|
| **规划** | 分析范围确认、子任务拆分、采集方案制定 | `references/planning.md` |
| **文档工程** | 目录规范、文档模板、命名约定、索引维护 | `references/doc-standards.md` |
| **应用操作** | 操控竞品应用，截屏、录屏、模拟用户事件 | `references/mobile-adb.md`（后续补充更多方案） |
| **产物分析** | 通过单个多模态 subagent 分析截图/录屏，提取业务逻辑 | `references/analysis.md` |
| **成文** | 通过 subagent 将分析结果合并到功能文档，更新索引，记录修订 | `references/compilation-subagent.md` |
| **Review** | 通过 subagent 对照 checklist 逐项检查并修正，输出审查报告 | `references/review.md` |

## 工作流总览

```
  [1. 规划]          [2. 采集]           [3. 分析]            [4. 成文]          [5. Review]
  明确分析范围    操控竞品应用       派发单个 subagent    派发 subagent      对照标准检查
  划分子任务      截图 + 录屏       读取产物多维度分析   合并分析到功能文档   补充遗漏维度
  制定采集方案    每步标注上下文    输出 analysis.md      更新索引/修订历史   输出最终文档
```

## 阶段 1：规划 —— 预制采集文档，等待审批

确认分析范围、拆分子任务、制定采集方案。详细指南见 `references/planning.md`。

核心步骤：
1. 从用户描述中提取目标竞品、功能模块、分析深度、目标平台
2. 按交互链路将大范围分析拆分为独立可执行的子任务
3. 对每个子任务，按 `assets/capture-template.md` 预制采集文档，填入采集信息和操作序列，保存到 `captures/<日期>-<描述>/capture.md`
4. 向用户呈现分析计划和采集文档，确认后进入采集

## 阶段 2：采集 —— 派发 subagent，传文档路径即可

采集文档已在阶段 1 预制完成。按 `references/capture-subagent.md` 中的 subagent 定义，传入采集文档路径即可派发。

**采集不可并发**：所有采集共享同一设备/模拟器，必须逐个串行。

## 阶段 3：分析 —— 派发 subagent，传文档路径即可

采集完成后，对每个采集文档，按 `references/analysis.md` 中的 subagent 定义各派发一个分析 subagent，传入采集文档路径。subagent 会加载 `product-research` skill，自行读取采集文档和同目录 `assets/` 下的产物，产出分析文档。

## 阶段 4：成文 —— 派发 subagent，传文档路径即可

分析完成后，按 `references/compilation-subagent.md` 中的 subagent 定义派发成文 subagent，传入采集文档路径。subagent 会加载 `product-research` skill，自行读取采集文档、分析文档和功能文档，完成合并。

**同模块必须串行，不同模块可并行**：多个采集对应同一功能模块时，成文 subagent 必须逐个串行执行（功能文档写入冲突），不同模块间可并发。

成文 subagent 负责：
- 首次创建或增量更新功能文档
- 截图引用标注采集源
- 更新模块和频道索引
- 创建修订历史

## 阶段 5：Review —— 派发 subagent，传文档路径即可

成文完成后，按 `references/review.md` 中的 subagent 定义派发 Review subagent，传入采集文档路径。subagent 会加载 `product-research` skill，自行读取采集文档、分析文档、功能文档、索引和修订历史，逐项检查并输出审查报告。

发现问题后 subagent 直接修正，修正后自检，直到全部通过。

## 完整示例

用户：「爬一下红果的播放器，从点击视频卡片到退出播放器的完整流程，包括所有的交互细节和 UI 样式」

执行流程：

**阶段 1 — 规划**
- 范围：播放器完整交互流程 + UI 样式
- 按路径拆分为 5 个子任务，每个独立预制采集文档：
  1. 点击视频卡片 → 进入播放器 `captures/2026-07-22-player-enter/capture.md`
  2. 播放页 UI 首屏（静态布局）`captures/2026-07-22-player-layout/capture.md`
  3. 控制栏交互（唤出/隐藏 + 暂停/播放 + 进度 + 倍速）`captures/2026-07-22-player-controls/capture.md`
  4. 横竖屏切换 `captures/2026-07-22-player-orientation/capture.md`
  5. 播放器 → 退出回到首页 `captures/2026-07-22-player-exit/capture.md`
- 平台：mobile
- 向用户呈现分析计划和各采集文档，确认后逐条执行

**阶段 2 — 采集**
- 按 `references/capture-subagent.md` 逐个派发采集 subagent，只传采集文档路径
- 以子任务 1 为例：subagent 读取 `captures/2026-07-22-player-enter/capture.md`，按操作序列执行并回填
- 全部 subagent 返回后确认：各采集文档已回填观察/日志/产物清单，截图录屏在各自 `assets/` 中

**阶段 3 — 分析**
- 按 `references/analysis.md` 逐个派发分析 subagent，只传采集文档路径
- subagent 加载 product-research skill，读取采集文档和同目录产物，按模板产出 `analysis.md`

**阶段 4 — 成文**
- 所有采集均为同一功能模块 `video-player`，成文 subagent 必须串行
- 按 `references/compilation-subagent.md` 逐个派发成文 subagent，只传采集文档路径
- subagent 加载 product-research skill，自行读取采集文档和分析文档
- 首次成文：创建 `mobile/video-player/video-player.md`（功能文档）
- 后续成文：增量更新同一功能文档，追加截图、合并发现、更新附录
- 每次成文后更新模块 `index.md` 并创建修订历史

**阶段 5 — Review**
- 按 `references/review.md` 逐个派发 Review subagent，只传采集文档路径
- subagent 加载 product-research skill，自行读取所有相关文档
- 逐项检查采集文档、分析文档、功能文档、修订历史、索引
- 输出审查报告，发现问题直接修正并自检
