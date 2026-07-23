---
name: feature-workflow
description: >
  需求到落地的全流程管理 skill。覆盖从 worktree 创建、需求撰写与 review、
  技术方案设计与 review、各端 plan 与 coding（含 code review），到 worktree 合并与 wiki 收录的 14 个阶段。
  触发场景：用户提出新功能开发需求、说"开始一个新需求"、"创建一个功能分支"、"推进 XX 需求"、
  "做 XX 功能"、"实现 XX"、"开发 XX"、"加一个 XX 功能"。只要涉及从需求到代码落地的完整开发流程，
  都应使用本 skill。当用户表达了开发意图但未明确说明流程时，也应主动使用本 skill 引导。
---

# Feature Workflow

## 定位

feature-workflow 是需求从「想法」到「代码落地」的完整编排层。它不替代任何已有 skill（如 llm-wiki），而是在它们之上提供阶段编排、状态管理和人机协作边界。

核心设计理念：**agent 高度自主推进流程，人只在 3 个固定确认点和必要时介入**。每个需要 review 的阶段都内置了「执行→审查→修复」循环，agent 会自行修复能解决的问题，仅将无法判断的问题上报给人。

## 能力线

| 能力线 | 职责 | 执行方式 | 规范 |
|--------|------|---------|------|
| **Worktree 管理** | 创建/合并/清理 git worktree | 主 agent + Bash | [references/worktree.md](references/worktree.md) |
| **需求撰写** | 查阅 wiki + 代码，撰写 spec | 主 agent | [references/spec-writing.md](references/spec-writing.md) |
| **需求 Review** | 审查需求完整性/一致性/可行性 | subagent（循环修复） | [references/spec-review.md](references/spec-review.md) |
| **技术方案设计** | Shared 设计 + 各端方案 | 主 agent + subagent（并行） | [references/design-writing.md](references/design-writing.md) |
| **技术方案 Review** | 审查设计完整性/一致性/跨端对齐 | subagent（循环修复） | [references/design-review.md](references/design-review.md) |
| **Plan 撰写** | 轻量 TDD 实现计划 | subagent（并行） | [references/plan-writing.md](references/plan-writing.md) |
| **Coding** | 按 plan 逐步骤实现 | subagent（并行，内部含 code review） | [references/coding.md](references/coding.md) |
| **Code Review** | 审查代码质量、规范、一致性 | subagent（在 coding 内部创建） | [references/code-review.md](references/code-review.md) |
| **Wiki 收录** | 汇总全流程产物 + git diff，委托 llm-wiki 更新 wiki | subagent | [references/wiki-inclusion.md](references/wiki-inclusion.md) |

## 工作流总览

```mermaid
flowchart TD
    A["1. worktree-setup<br/>创建分支和工作区"] --> B["2. spec-writing<br/>需求撰写"]
    B --> C["3. spec-review 🔄<br/>需求审查(自动循环)"]
    C --> D["4. spec-human-review 👤<br/>人确认需求"]
    D --> E["5. design-shared<br/>共享技术方案"]
    E --> F["6. design-platforms ⚡<br/>各端方案(并行)"]
    F --> G["7. design-review 🔄<br/>方案审查(自动循环)"]
    G --> H["8. design-human-review 👤<br/>人确认方案"]
    H --> I["9. plan-platforms ⚡<br/>各端实现计划(并行)"]
    I --> J["10. coding-platforms ⚡🔄<br/>各端编码+审查(并行)"]
    J --> K["11. code-human-review 👤<br/>人确认代码"]
    K --> L["12. worktree-merge<br/>合回主干"]
    L --> M["13. wiki-inclusion<br/>wiki 收录"]
    M --> N["14. completed<br/>完成"]

    ⚡["⚡ 可并行"] --- F
    🔄["🔄 含review循环"] --- C
    👤["👤 需人确认"] --- D
```

| 图例 | 含义 |
|------|------|
| 🔄 | 含 review 循环（执行→审查→修复→再审查，直到 agent 无法解决的问题） |
| 👤 | 需要人工确认 |
| ⚡ | 各端可并行执行 |

## 阶段说明

### 阶段 1：worktree-setup

**执行者**：主 agent + Bash

**前置条件**：无

1. 确认需求名称（kebab-case），如 `add-player-speed-control`
2. 执行 `python3 scripts/workflow.py init <name>` 创建 spec 目录和 workflow.json
3. 执行 `git worktree add .worktree/<YYYY-MM-dd>-<name> -b feature/<YYYY-MM-dd>-<name>` 创建工作区
4. 进入 worktree 目录：`cd .worktree/<YYYY-MM-dd>-<name>`

**产物**：`docs/specs/<YYYY-MM-dd>-<name>/workflow.json`，worktree 就绪

**完成标志**：worktree 创建成功，已进入 worktree 目录

**下一阶段提示**：「worktree 已就绪。是否开始需求撰写？」

### 阶段 2：spec-writing

**执行者**：主 agent

**前置条件**：worktree-setup 完成

**执行规范**：详见 [references/spec-writing.md](references/spec-writing.md)

核心流程：
1. 调用 `Skill("llm-wiki")` 查阅现有功能文档
2. 读取各端代码了解当前实现
3. 按 `assets/spec-template.md` 撰写 `spec.md`

**产物**：`docs/specs/<YYYY-MM-dd>-<name>/spec.md`

**完成标志**：spec.md 已写入，所有章节已填充

完成后：`python3 scripts/workflow.py advance`

**下一阶段提示**：「需求文档已完成。是否开始需求审查？」

### 阶段 3：spec-review 🔄

**执行者**：subagent（自动循环修复）

**前置条件**：spec-writing 完成

**执行规范**：详见 [references/spec-review.md](references/spec-review.md)

核心流程：
1. 派发 spec-review subagent（prompt 见 reference 文件）
2. Subagent 审查完整性/一致性/可行性，发现问题直接修复
3. Subagent 输出 `spec-review.md`
4. 主 agent 检查遗留问题：无遗留 → 推进；有遗留 → 询问用户 → 重新派发

**产物**：`docs/specs/<YYYY-MM-dd>-<name>/spec-review.md`

**review 循环**：每次循环调用 `python3 scripts/workflow.py review-loop spec-review --increment`

**下一阶段提示**：「需求审查完成。请确认需求文档，确认后进入技术方案设计。」

### 阶段 4：spec-human-review 👤

**执行者**：人 + 主 agent

**前置条件**：spec-review 完成，无遗留问题

1. 向用户展示需求文档关键内容摘要
2. 用户确认后：`python3 scripts/workflow.py human-review spec-human-review --approve`
3. 用户驳回后：`python3 scripts/workflow.py human-review spec-human-review --reject`，回到阶段 2

**下一阶段提示**：「需求已确认。是否开始技术方案设计？」

### 阶段 5：design-shared

**执行者**：主 agent

**前置条件**：spec-human-review 通过

**执行规范**：详见 [references/design-writing.md](references/design-writing.md)

1. 调用 `Skill("llm-wiki")` 查阅现有架构和 API
2. 读取 backend 代码了解现有 API 设计
3. 按 `assets/design-template.md` 撰写 `design.md`（API 设计、数据模型、跨端共享逻辑）

**产物**：`docs/specs/<YYYY-MM-dd>-<name>/design.md`

**下一阶段提示**：「共享技术方案已完成。是否开始各端方案设计？」

### 阶段 6：design-platforms ⚡

**执行者**：subagent（各端并行）

**前置条件**：design-shared 完成

**执行规范**：详见 [references/design-writing.md](references/design-writing.md)

1. 判断涉及哪些平台
2. 为每个涉及平台并行派发 design subagent
3. Subagent 按 `assets/design-platform-template.md` 输出 `design-{platform}.md`
4. 每个平台完成后调用 `python3 scripts/workflow.py mark-platform design-platforms <platform> --status completed`
5. 全部完成后 `python3 scripts/workflow.py advance`

**产物**：`design-backend.md`, `design-ios.md`, `design-android.md`, `design-web.md`（按需）

**下一阶段提示**：「各端方案已完成。是否开始方案审查？」

### 阶段 7：design-review 🔄

**执行者**：subagent（两步：先 shared，再各端并行）

**前置条件**：design-platforms 完成

**执行规范**：详见 [references/design-review.md](references/design-review.md)

1. 派发 shared design review subagent → 审查 `design.md`
2. Shared 通过后，并行派发各平台 design review subagent
3. Subagent 发现问题直接修复，输出 `design-review.md`
4. 主 agent 检查遗留问题，有遗留则循环

**产物**：`docs/specs/<YYYY-MM-dd>-<name>/design-review.md`

**review 循环**：`python3 scripts/workflow.py review-loop design-review --increment`

**下一阶段提示**：「方案审查完成。请确认技术方案，确认后进入实现计划。」

### 阶段 8：design-human-review 👤

**执行者**：人 + 主 agent

**前置条件**：design-review 完成，无遗留问题

- 通过：`python3 scripts/workflow.py human-review design-human-review --approve`
- 驳回：`python3 scripts/workflow.py human-review design-human-review --reject`，回到阶段 5

**下一阶段提示**：「技术方案已确认。是否开始编写各端实现计划？」

### 阶段 9：plan-platforms ⚡

**执行者**：subagent（各端并行）

**前置条件**：design-human-review 通过

**执行规范**：详见 [references/plan-writing.md](references/plan-writing.md)

1. 为各涉及平台并行派发 plan subagent
2. Subagent 按 `assets/plan-template.md` 输出 `plan-{platform}.md`
3. 每个平台完成后 `python3 scripts/workflow.py mark-platform plan-platforms <platform> --status completed`
4. 全部完成后 `python3 scripts/workflow.py advance`

**产物**：`plan-backend.md`, `plan-ios.md`, `plan-android.md`, `plan-web.md`（按需）

**下一阶段提示**：「实现计划已完成。是否开始编码？」

### 阶段 10：coding-platforms ⚡🔄

**执行者**：subagent（各端并行，每个内部含 code review 循环）

**前置条件**：plan-platforms 完成

**执行规范**：详见 [references/coding.md](references/coding.md)

1. 为各涉及平台并行派发 coding subagent
2. 每个 subagent 按 plan 步骤执行：写测试 → 写代码 → 验证 → 补充测试
3. 完成后在内部创建 code-review subagent 进行审查
4. 审查发现问题 → 修复 → 重新审查（最多 3 轮）
5. 每个平台完成后 `python3 scripts/workflow.py mark-platform coding-platforms <platform> --status completed`
6. 全部完成后 `python3 scripts/workflow.py advance`

**产物**：各端代码变更 + `code-{platform}-review.md`（按需）

**review 循环**：`python3 scripts/workflow.py review-loop coding-platforms --platform <platform> --increment`

**下一阶段提示**：「编码完成。请审查代码变更，确认后合回主干。」

### 阶段 11：code-human-review 👤

**执行者**：人 + 主 agent

**前置条件**：coding-platforms 完成

1. 向用户展示代码变更摘要和 review 结论
2. 通过：`python3 scripts/workflow.py human-review code-human-review --approve`
3. 驳回：`python3 scripts/workflow.py human-review code-human-review --reject`，对应平台回到 coding

**下一阶段提示**：「代码已确认。是否合回主干？」

### 阶段 12：worktree-merge

**执行者**：主 agent + Bash

**前置条件**：code-human-review 通过

**执行规范**：详见 [references/worktree.md](references/worktree.md)

1. 推送分支：`git push origin feature/<YYYY-MM-dd>-<name>`
2. 切回主仓库：`cd <project-root> && git checkout main && git pull origin main`
3. 合并：`git merge --no-ff feature/<YYYY-MM-dd>-<name>`
4. 推送主干：`git push origin main`
5. 清理：`git worktree remove .worktree/<YYYY-MM-dd>-<name>`

**下一阶段提示**：「主干已合并。是否进行 wiki 收录？」

### 阶段 13：wiki-inclusion

**执行者**：subagent

**前置条件**：worktree-merge 完成

**执行规范**：详见 [references/wiki-inclusion.md](references/wiki-inclusion.md)

1. 派发 wiki-inclusion subagent
2. Subagent 收集 spec 目录下所有文档 + `git diff` 变更文件列表
3. 委托 llm-wiki skill 完成 wiki 文档维护
4. 输出 `wiki.md` 收录报告

**产物**：`docs/specs/<YYYY-MM-dd>-<name>/wiki.md`，wiki 各文档已更新

**下一阶段提示**：「wiki 收录完成。需求全流程结束！」

### 阶段 14：completed

调用 `python3 scripts/workflow.py advance` 标记完成。向用户报告全流程总结。

## 人机协作边界

| 阶段 | 自动化程度 | 人介入条件 |
|------|-----------|-----------|
| worktree-setup | ✅ 全自动 | — |
| spec-writing | ✅ 全自动 | — |
| spec-review 🔄 | 🟡 半自动 | review subagent 无法判断的问题 |
| spec-human-review 👤 | 🔴 必须人确认 | 每次 |
| design-shared | ✅ 全自动 | — |
| design-platforms ⚡ | ✅ 全自动 | — |
| design-review 🔄 | 🟡 半自动 | review subagent 无法判断的问题 |
| design-human-review 👤 | 🔴 必须人确认 | 每次 |
| plan-platforms ⚡ | ✅ 全自动 | — |
| coding-platforms ⚡🔄 | 🟡 半自动 | code review 无法判断的问题 |
| code-human-review 👤 | 🔴 必须人确认 | 每次 |
| worktree-merge | 🟡 半自动 | 合并冲突时 |
| wiki-inclusion | ✅ 全自动 | — |

人在 3 个必经确认点之外的介入条件：
- Review 循环发现 agent 无法解决的问题（如产品策略、架构权衡、外部依赖选择）
- 用户主动中断流程提出修改
- 代码合并冲突需要手动解决

## Review 循环机制

所有 review 阶段（spec-review、design-review、coding-platforms）遵循相同的循环模式：

```
执行 → 派发 review subagent → subagent 检查并修复 → 输出 review 报告
                                                              ↓
                                    无遗留 ← 主 agent 检查 ← 
                                      ↓                      ↓
                                 推进到下一阶段            有遗留 → 询问用户
                                                              ↓
                                                         用户回复后 → 回到执行
```

关键规则：
- Subagent 自行修复能修复的问题，不等待主 agent 确认
- 每轮 review 后调用 `workflow.py review-loop` 递增计数
- Agent 应自我限制：**3 轮 review 仍未收敛时，无论如何上报给人工**
- 遗留问题记录到 review 文档的「遗留问题」章节

## 各阶段完成后的提示

每个阶段完成后，主 agent 必须明确提示：

> 「✅ 阶段 {N} ({stage-name}) 已完成。」
> 「📁 产物：{文件列表}」
> 「⏭️ 下一阶段：阶段 {N+1} ({next-stage-name})」
> 「是否继续？」

这样确保人对流程进展始终有清晰感知。

## 快速开始示例

用户：「做一个倍速播放功能，支持 0.5x/1.0x/1.5x/2.0x 四个速度档位」

执行流程：

**阶段 1 — worktree-setup**
```bash
python3 scripts/workflow.py init add-playback-speed
git worktree add .worktree/$(date +%Y-%m-%d)-add-playback-speed -b feature/$(date +%Y-%m-%d)-add-playback-speed
cd .worktree/$(date +%Y-%m-%d)-add-playback-speed
```

**阶段 2 — spec-writing**
- 调用 `Skill("llm-wiki")` 查阅播放器功能文档 → 了解现有播放器架构
- 读取 `ios/`、`android/` 下的播放器源码 → 确认当前不支持倍速
- 按模板撰写 spec.md，定义 4 档速度的交互流程和验收标准

**阶段 3 — spec-review 🔄**
- 派发 spec-review subagent → 发现「缺少控制栏 UI 变更说明」→ 补充 → 通过

**阶段 4 — spec-human-review 👤**
- 向用户展示需求摘要 → 用户确认

**阶段 5-8 — design + review**
- 撰写 design.md（API: `POST /api/player/speed`）
- 并行派发 iOS/Android/Backend design subagent
- Review 循环修复

**阶段 9-11 — plan + coding + review**
- 各端 plan 并行
- Coding subagent 并行 → 内部 code review 循环
- 用户确认代码

**阶段 12-14 — merge + wiki**
- 合回主干 → wiki 收录 → 完成

## 资源索引

### Scripts

| 脚本 | 用途 |
|------|------|
| `scripts/workflow.py` | 流程状态管理（init/status/advance/review-loop/human-review/mark-platform） |

### References

| 文件 | 用途 | 包含 subagent 定义 |
|------|------|--------------------|
| [references/worktree.md](references/worktree.md) | worktree 创建/合并/清理规范 | — |
| [references/spec-writing.md](references/spec-writing.md) | 需求撰写流程和注意事项 | — |
| [references/spec-review.md](references/spec-review.md) | 需求 review 流程 | ✅ |
| [references/design-writing.md](references/design-writing.md) | 技术方案设计流程和 subagent 定义 | ✅ |
| [references/design-review.md](references/design-review.md) | 技术方案 review 流程 | ✅ |
| [references/plan-writing.md](references/plan-writing.md) | 轻量 TDD plan 撰写规范 | ✅ |
| [references/coding.md](references/coding.md) | coding subagent 派发规范 | ✅ |
| [references/code-review.md](references/code-review.md) | code review subagent 规范 | ✅ |
| [references/wiki-inclusion.md](references/wiki-inclusion.md) | wiki 收录流程 | ✅ |

### Assets（模板）

| 模板 | 用途 |
|------|------|
| `assets/spec-template.md` | 需求文档模板 |
| `assets/spec-review-template.md` | 需求 review 报告模板 |
| `assets/design-template.md` | 共享技术方案模板 |
| `assets/design-platform-template.md` | 各端技术方案模板 |
| `assets/design-review-template.md` | 技术方案 review 报告模板 |
| `assets/plan-template.md` | 实现计划模板（轻量 TDD） |
| `assets/code-review-template.md` | 代码 review 报告模板 |
| `assets/wiki-inclusion-template.md` | wiki 收录报告模板 |

## 关键约束

- **流程脚本优先**：状态变更通过 `scripts/workflow.py` 执行，不直接修改 `workflow.json`
- **wiki 操作委托**：wiki 维护通过 `llm-wiki` skill 执行，feature-workflow 不直接操作 wiki
- **产品信息引用**：涉及产品名、竞品名时引用 `PRODUCT.md`，不硬编码
- **已有 skill 配合**：
  - 编写 spec/design 前必须调用 `Skill("llm-wiki")` 了解现状
  - wiki 收录通过 `llm-wiki` skill 的子流程执行
  - 如需竞品分析，先通过 `product-research` skill 完成后再走 feature-workflow
