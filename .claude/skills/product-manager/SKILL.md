---
name: product-manager
description: >
  产品经理 skill，负责需求分析与功能规划。覆盖三大核心能力：功能路线规划（根据用户输入、竞品调研、现有 wiki 决定下一步做什么）、
  需求精密化（将模糊需求描述为精确 PRD，将大型需求拆分为每端 ≤5 人日的子任务）、
  项目进展管理（跟踪功能层面的规划/构建/完成状态，确保迭代上升、避免冲突）。
  触发场景：用户提到"下一步做什么"、"规划功能"、"分析需求"、"产品路线"、"排优先级"、"拆需求"、"写 PRD"、
  "这个功能怎么做"、"评估工作量"、"功能规划"、"产品规划"、"roadmap"、"backlog"、"需求分析"、"feature planning"。
  也应在用户提出模糊的功能想法时主动介入，帮助澄清和拆解。本 skill 处于 product-research（竞品调研）和 feature-workflow（开发落地）之间。
---

# Product Manager

## 定位

product-manager 是「产品决策层」——在竞品调研（product-research）和开发落地（feature-workflow）之间，负责回答「下一步做什么、怎么做、做到哪了」。

核心设计理念：**事件驱动、数据支撑、可落地优先**。每次用户提出想法或项目状态变化时，综合竞品、wiki、当前进展做出产品决策，确保每个进入开发的需求都是精确的、可拆解的、可追踪的。

```
product-research          product-manager           feature-workflow
(竞品调研)         →      (产品决策)        →       (开发落地)
                           ↑
                      llm-wiki (现有能力)
```

## 能力线

| 能力线 | 职责 | 触发时机 | 执行方式 | 规范 |
|--------|------|---------|---------|------|
| **路线规划** | 综合分析，输出功能优先级建议 | 用户提出新想法、阶段性复盘 | 主 agent | [references/roadmap-planning.md](references/roadmap-planning.md) |
| **需求拆解** | 模糊需求 → 精简 PRD → 子任务拆分 | 确定要做某个功能后 | 主 agent + subagent | [references/prd-writing.md](references/prd-writing.md) |
| **进展管理** | 维护功能全景图，跟踪状态 | 每次 feature-workflow 完成后、用户主动查询 | 主 agent | [references/progress-management.md](references/progress-management.md) |
| **PRD Review** | 审查 PRD 完整性、可落地性、跨端一致性，循环修正直至仅剩人工决策问题 | PRD 撰写完成后 | subagent（循环） | [references/prd-review.md](references/prd-review.md) |

## 文档结构

所有产物位于 `docs/product_manager/` 下：

```
docs/product_manager/
├── roadmap.md              # 产品路线图：按主题分组的长期功能规划
├── backlog.md              # 功能待办池：所有待评估/待排期的功能想法
├── progress.md             # 进展跟踪：每个功能当前状态与链接
├── decisions/              # 分析决策记录
│   └── YYYY-MM-DD-<topic>.md
├── prd/                    # PRD 文档（按日期+功能组织）
│   └── YYYY-MM-DD-<feature-slug>/
│       ├── prd.md          # PRD 主文档（精简版，只关注做什么、为什么、给谁用）
│       └── subtasks.md     # 子任务拆分与工时估算
└── revisions/              # 变更记录
    └── revisions.md
```

### 文档职责

| 文档 | 职责 | 更新时机 |
|------|------|---------|
| `roadmap.md` | 按主题/阶段组织的功能路线图，体现产品大方向 | 每次路线规划分析后 |
| `backlog.md` | 所有功能想法的扁平列表，含优先级、状态、来源 | 新想法提出时、PRD 开始撰写时 |
| `progress.md` | 每个功能当前所处阶段（planned→building→done），含链接 | PRD 完成后、feature-workflow 启动/完成后 |
| `decisions/` | 每次「下一步做什么」分析的决策过程和依据 | 每次路线规划分析后 |
| `prd/YYYY-MM-DD-<slug>/prd.md` | 单个功能的精简需求文档（做什么、为什么、给谁用） | 需求拆解阶段 |
| `prd/YYYY-MM-DD-<slug>/subtasks.md` | 子任务拆分、每端工时估算、迭代计划 | PRD 完成后 |
| `revisions/revisions.md` | product_manager 目录下所有文档的变更摘要 | 每次文档变更后 |

### PRD 定位

PM 阶段的 PRD 是**精简版**，只聚焦核心决策信息：

| PRD 包含 | PRD 不包含（留到 spec 阶段） |
|----------|---------------------------|
| 需求背景（问题/痛点/目标/竞品） | 详细数据模型 |
| 目标用户 | 非功能性需求（性能/安全指标） |
| 用户故事（验收标准） | 边界与异常处理细节 |
| 核心流程（用户旅程 + 流程图） | 技术实现细节 |
| 范围定义（做什么/不做什么） | API 设计 |
| 涉及平台 | 兼容性与迁移方案 |
| 依赖项 | |
| 元信息（现有功能影响、待澄清、参考资料、变更历史） | |

## 工作流总览

```
  [A. 信息收集]       [B. 路线规划]        [C. 需求拆解]        [D. PRD Review]           [E. 交付跟踪]
  用户提出想法     综合分析竞品+wiki    撰写精简 PRD        review-writing 循环       feature-workflow
  读取竞品调研     给出优先级建议       拆分子任务           agent 自行修正问题         启动开发
  读取 wiki          记录决策过程       估算每端工时         人工决策问题上报          跟踪进展
  读取当前进展       更新 backlog       更新 progress         审查通过后更新 progress    更新 progress
```

### 触发路径

product-manager 根据用户意图的不同，进入不同的处理路径：

| 用户意图 | 进入路径 | 产出 |
|---------|---------|------|
| "下一步做什么" / "分析下优先级" / "规划功能" | A → B | 决策记录 + 更新 roadmap/backlog |
| "做 XX 功能" / "帮我拆一下 XX" / "写 XX 的 PRD" | A → C → D | PRD + subtasks |
| "项目进展怎么样" / "还有哪些没做完" | 仅 E（查询） | 进展摘要 |
| "XX 功能做完了"（feature-workflow 完成后触发） | 仅 E（更新） | 更新 progress.md |

## 关键约束

- **竞品调研优先**：分析功能方向前，必须确认竞品调研是否充分。如 `docs/product_research/` 中无相关模块的竞品资料，应先建议用户通过 `product-research` skill 补充，或基于现有信息做出标注。
- **wiki 必读**：撰写 PRD 前必须调用 `Skill("llm-wiki")` 了解现有功能，避免重复规划或与现有架构冲突。
- **工时约束**：子任务拆分后，每端口（Backend/iOS/Android/Web）的单个子任务工时不得超过 5 人日。超过则必须继续拆分。
- **PRD 精简原则**：PM 阶段 PRD 只关注「做什么、为什么、给谁用」。技术细节、非功能性需求、异常处理细节留给 feature-workflow spec 阶段补充。
- **产物目录**：所有 product-manager 产物写入 `docs/product_manager/` 下，不散落其他目录。
- **交付衔接**：PRD 完成后，引导用户通过 `feature-workflow` skill 启动开发。PRD 中的用户故事和核心流程可直接作为 feature-workflow spec 的输入。
- **产品信息引用**：涉及产品名、竞品名时引用 `PRODUCT.md`，不硬编码。
- **状态同步**：feature-workflow 的 wiki-inclusion 阶段（阶段 14）完成后，应回到 product-manager 更新 progress.md 中对应功能的状态。

## 资源索引

### References

| 文件 | 用途 |
|------|------|
| [references/roadmap-planning.md](references/roadmap-planning.md) | 路线规划分析流程：信息收集、决策记录格式、用户确认 |
| [references/prd-writing.md](references/prd-writing.md) | PRD 撰写规范与流程 |
| [references/prd-review.md](references/prd-review.md) | PRD Review 流程与 subagent 定义 |
| [references/progress-management.md](references/progress-management.md) | 进展管理：状态定义、子需求跟踪、冲突检测 |

### Assets（模板）

| 模板 | 用途 |
|------|------|
| `assets/prd-template.md` | PRD 主文档模板（精简版） |
| `assets/subtasks-template.md` | 子任务拆分与工时估算模板 |
| `assets/decision-record-template.md` | 决策记录模板 |
| `assets/roadmap-template.md` | 路线图模板 |
| `assets/backlog-template.md` | 功能待办池模板 |
| `assets/prd-review-template.md` | PRD Review 输出模板 |
| `assets/progress-template.md` | 进展跟踪模板 |
