# 进展管理

## 概述

进展管理是 product-manager 对功能层面「全貌」的持续跟踪。它不关注具体代码实现细节（那是 feature-workflow 的职责），而是回答：

- **规划了多少功能**：backlog 和 roadmap 中有多少待做项
- **正在做什么**：哪些功能正在 feature-workflow 中推进
- **已经完成了什么**：哪些功能已交付
- **下一步准备做什么**：优先级最高且未被阻塞的待启动项

## 功能状态定义

只用三个核心状态：

```
[planned] → [building] → [done]
  规划中       构建中      已完成
```

| 状态 | 含义 | 进入条件 | 退出条件 |
|------|------|---------|---------|
| `planned` | 功能已识别，记录在 backlog 或 roadmap 中，尚未开始开发 | 用户提出想法、路线规划分析输出 | PRD Review 通过，用户确认启动 feature-workflow |
| `building` | PRD 已就绪，正在 feature-workflow 中推进 | PRD Review 通过，用户确认启动 feature-workflow | feature-workflow wiki-inclusion 完成 |
| `done` | 已合入主干，wiki 已更新 | feature-workflow 完成 | — |

> 不再区分「PRD 撰写中」和「PRD 已确认」——这些是 building 状态的内部细节，不需要暴露在 progress.md 的顶层视图中。如需了解，直接看 `prd/` 目录下对应文档即可。
>
> **已取消的功能**：如用户明确表示放弃某功能，可在 `planned` 或 `building` 区对应行的「备注」列标注 `（已取消：原因）`，记录取消原因而不删除记录，保留历史信息。

## 子需求跟踪

当一个需求被拆分为多个子需求时（一个 PRD 对应一个 feature-workflow 迭代），progress.md 按以下方式记录：

### 规则

- **PRD 是跟踪的最小单位**：progress.md 的每一行对应一个 PRD（即一个功能），不是每个子任务
- 每个 PRD 产生一个 building entry，完成后变成 done entry
- 子需求（多个 PRD 对应同一个功能的不同迭代）在功能列标注，通过功能名分组

### 记录方式

```markdown
### 🚧 构建中

| 功能 | 迭代 | PRD | Spec | 启动日期 | 涉及端 | 备注 |
|------|------|-----|------|---------|--------|------|
| 首页信息流 | 1/2 | [链接](prd/2026-07-23-homepage-feed/prd.md) | [链接](../../docs/specs/xxx/spec.md) | 2026-07-25 | Backend/iOS/Android | 核心 Feed 流 |
| 首页信息流 | 2/2 | [链接](prd/2026-07-30-homepage-feed-v2/prd.md) | — | 2026-08-01 | iOS/Android | 下拉刷新 + 骨架屏 |

### 🟢 已完成

| 功能 | 迭代 | 完成日期 | 涉及端 | Spec | 备注 |
|------|------|---------|--------|------|------|
| 首页信息流 | 1/2 | 2026-07-30 | Backend/iOS/Android | [链接](../../docs/specs/xxx/spec.md) | 核心 Feed 流 |
```

- **迭代列**：`1/2` 表示第 1 个迭代，总共 2 个；`1/1` 表示一次性完成
- 同一个功能的多个迭代通过**功能名**分组，可以一眼看出哪些功能还在迭代中
- 每个迭代有自己独立的 PRD（路径带日期区分），可以独立启动 feature-workflow

### 粒度决策

是否拆分为多个迭代由 PM 在需求拆解阶段决定：

- **单迭代（1/1）**：功能足够小，一个 feature-workflow 可以完成
- **多迭代（1/N）**：功能较大，按 subtasks.md 中规划的迭代拆分，每个迭代独立走 feature-workflow

## 进展文档格式

`docs/product_manager/progress.md` 维护所有功能的当前状态：

```markdown
# 项目进展

> 最后更新：YYYY-MM-DD HH:mm

## 进展总览

| 状态 | 数量 |
|------|------|
| 🔵 规划中 (planned) | N |
| 🚧 构建中 (building) | N |
| 🟢 已完成 (done) | N |

> planned = 功能数；building/done = 迭代数。

## 功能列表

### 🚧 构建中

| 功能 | 迭代 | PRD | Spec | 启动日期 | 涉及端 | 备注 |
|------|------|-----|------|---------|--------|------|
| <名称> | N/M | [链接](prd/YYYY-MM-DD-<slug>/prd.md) | [链接](../../docs/specs/<spec-dir>/spec.md) | YYYY-MM-DD | Backend/iOS/Android | |

### 🔵 规划中

| 功能 | 来源 | 优先级 | 涉及端 | 备注 |
|------|------|--------|--------|------|
| <名称> | 用户提出 / 竞品分析 / 路线规划 | P0 / P1 / P2 | | |

### 🟢 已完成

| 功能 | 迭代 | 完成日期 | 涉及端 | Spec | 备注 |
|------|------|---------|--------|------|------|
| <名称> | N/N | YYYY-MM-DD | Backend/iOS/Android | [链接](../../docs/specs/<spec-dir>/spec.md) | |
```

## 更新时机与流程

### 时机 1：PRD Review 通过后

主 agent 更新 progress.md：
- 将本功能的记录从 `planned` 移至 `building`（PRD 已就绪，等待 feature-workflow 启动）
- 如果这是该功能的第 N 个迭代，填写迭代列（`N/M`）
- 填写 PRD 链接、涉及端

### 时机 2：feature-workflow 完成后

feature-workflow 的 wiki-inclusion（阶段 14）完成后，应通知用户回到 product-manager。主 agent 更新 progress.md：
- 将该迭代记录从 `building` 移至 `done`
- 填写完成日期
- 如果该功能还有后续迭代（`N < M`），问用户是否继续推进下一个迭代
- 更新进展总览计数

### 时机 3：用户主动查询

> 「📊 当前项目进展：」
> 「🟢 已完成：N 个迭代 | 🚧 构建中：N 个迭代 | 🔵 规划中：N 个功能」
> 「🚧 当前进行中：<功能名>（迭代 2/3）— 涉及 Backend/iOS/Android」
> 「⏭️ 下一步建议：<功能名>（PRD 已就绪，预估工时 X 人日）」

## 冲突检测

在以下时刻执行冲突检测：

### PRD 启动前

检查 `progress.md` 中 `building` 状态的条目：
- 是否有功能正在修改同一模块？（如两个功能都在改「播放器」）
- 是否有功能依赖本功能的产出？

如有冲突，向用户报告：

> 「⚠️ 潜在冲突：<功能名 A> 正在构建中，也涉及 <模块名> 模块。」
> 「建议：等 <功能名 A> 完成后再启动本功能，或明确分工边界。」

### 路线规划时

检查 `backlog.md` 的功能是否与 `progress.md` 中 `building` 状态的功能存在模块重叠。
如有重叠，在路线分析中标注「需等待 X 功能完成后评估」。
