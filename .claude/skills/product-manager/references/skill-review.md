# Skill Self-Review

## 概述

对 product-manager skill 本身进行全面审查，确保 skill 设计的一致性和完整性。这是一个 meta-review：审查 skill 文件（SKILL.md、references、assets）之间是否存在矛盾、遗漏或不一致。

## Subagent 定义

Subagent：
  description: "审查：产品经理-Skill-Self-Review"
  prompt: |
    你是一个 skill 设计审查专家。你的任务是审查 product-manager skill 的全部文件，检查设计的一致性和完整性。

    ## 审查输入

    读取以下全部文件：

    1. `.claude/skills/product-manager/SKILL.md`（主入口）
    2. `.claude/skills/product-manager/references/roadmap-planning.md`（路线规划）
    3. `.claude/skills/product-manager/references/prd-writing.md`（PRD 撰写）
    4. `.claude/skills/product-manager/references/prd-review.md`（PRD Review）
    5. `.claude/skills/product-manager/references/progress-management.md`（进展管理）
    6. `.claude/skills/product-manager/assets/prd-review-template.md`（PRD Review 模板）
    7. `.claude/skills/product-manager/assets/prd-template.md`（PRD 模板）
    8. `.claude/skills/product-manager/assets/subtasks-template.md`（子任务模板）
    9. `.claude/skills/product-manager/assets/progress-template.md`（进展模板）
    10. `.claude/skills/product-manager/assets/roadmap-template.md`（路线图模板）
    11. `.claude/skills/product-manager/assets/backlog-template.md`（待办池模板）
    12. `.claude/skills/product-manager/assets/decision-record-template.md`（决策记录模板）
    13. `docs/product_manager/roadmap.md`（实际路线图）
    14. `docs/product_manager/backlog.md`（实际待办池）
    15. `docs/product_manager/progress.md`（实际进展）

    > 注意：模板文件（assets/）中的占位符 `{{...}}` 和 `<!-- ... -->` 注释不是遗漏，是模板的预期行为。审查时区分「模板占位符」和「真正的信息缺失」。

    ## 审查维度

    ### 1. 路径一致性

    检查 SKILL.md 中声明的路径是否在 reference 和 template 文件中被正确引用：

    | 检查项 | 方法 |
    |--------|------|
    | PRD 产物路径 | SKILL.md 说 `prd/YYYY-MM-DD-<slug>/`，references 中的 subagent prompt 和模板是否一致？ |
    | progress.md 字段 | SKILL.md 和 progress-management.md 定义的表格列是否与 progress-template.md 一致？ |
    | 状态名称 | SKILL.md 说的 `planned/building/done` 是否在 progress-management.md 和 progress-template.md 中一致？ |
    | 文档引用 | references 中引用的模板路径（如 `assets/prd-template.md`）是否真实存在？ |

    ### 2. 信息一致性

    检查同一概念在不同文件中的表述是否一致：

    | 检查项 | 方法 |
    |--------|------|
    | 状态定义 | progress-management.md 定义的状态与 SKILL.md 能力线表格中提到的阶段是否一致？ |
    | PRD 精简原则 | SKILL.md 说 PRD 是「精简版」，prd-writing.md 说的是否一致？prd-template.md 是否真的精简了？ |
    | 工时约束 | 5 人日/端的约束在 SKILL.md、prd-writing.md、prd-review.md 中是否一致？ |
    | 上下游衔接 | SKILL.md 对 product-research 和 feature-workflow 的关系描述是否准确？ |

    ### 3. 完整性

    | 检查项 | 方法 |
    |--------|------|
    | 能力线覆盖 | SKILL.md 声明的 4 条能力线是否都有对应的 reference 文件？ |
    | 模板覆盖 | SKILL.md assets 表格中列出的模板是否都存在？ |
    | 流程闭环 | 从「用户提出想法」到「feature-workflow 完成后回写状态」，每个环节是否有对应的流程描述？ |
    | 边界情况 | 是否考虑了：功能被取消、功能跨多个迭代、PRD Review 循环终止条件（通过/仅剩人工决策/到达最大轮数）、用户中途改变主意？ |

    ### 4. 实际文档对齐

    检查 `docs/product_manager/` 下的实际文档是否与 skill 规范一致：

    | 检查项 | 方法 |
    |--------|------|
    | progress.md | 实际字段是否与 progress-template.md 一致？状态模型是否正确？ |
    | backlog.md | 是否包含必要的回溯信息（来源、优先级）？ |
    | roadmap.md | 是否引用了竞品分析结果？ |

    ### 5. 可用性

    | 检查项 | 方法 |
    |--------|------|
    | 触发描述是否准确 | description 中的触发词是否覆盖了典型使用场景？ |
    | 引用路径是否可达 | SKILL.md 中 `[references/xxx.md](references/xxx.md)` 的链接是否有效？ |
    | 文档能否独立理解 | 每个 reference 在不读 SKILL.md 的情况下，是否包含足够的上下文？ |

    ## 输出格式

    输出审查报告到 `.claude/skills/product-manager-workspace/skill-review.md`：

    ```markdown
    # Product Manager Skill Self-Review

    - 审查日期：YYYY-MM-DD
    - 审查结论：✅ 通过 / ⚠️ 有条件通过（需修正 N 项）/ ❌ 需重写

    ## 审查摘要

    | 维度 | 问题数 |
    |------|--------|
    | 路径一致性 | N |
    | 信息一致性 | N |
    | 完整性 | N |
    | 实际文档对齐 | N |
    | 可用性 | N |

    ## 发现的问题

    | 编号 | 维度 | 严重程度 | 位置 | 问题描述 | 建议修正 |
    |------|------|---------|------|---------|---------|
    | S-01 | | 🔴 阻塞 / 🟡 建议 | SKILL.md L42 | | |

    ## 逐维度详细审查

    ### 路径一致性
    ### 信息一致性
    ### 完整性
    ### 实际文档对齐
    ### 可用性

    ## 修正建议汇总

    按严重程度排序：
    1. [S-01] 🔴 <问题> → <具体修改方案>
    2. [S-02] 🟡 <问题> → <具体修改方案>
    ```

    ## 审查原则

    - **标注严重程度**：
      - 🔴 阻塞：路径错误、核心概念矛盾——会导致 agent 执行出错
      - 🟡 建议：措辞不一致、可优化但不会出错
    - **给出精确位置**：每个问题必须标注出在哪个文件的哪一行或哪个章节
    - **给出修改方案**：不只说「有问题」，必须给出明确的新内容建议
