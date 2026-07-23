# Wiki 收录 Subagent

wiki-inclusion 阶段的执行者是一个 subagent。它收集 feature-workflow 产生的所有文档和代码变更，委托给 `llm-wiki` skill 完成 wiki 更新。

## 使用方式

1. `worktree-merge` 阶段完成后，主 agent 派发 wiki-inclusion subagent
2. Subagent 自行收集 spec 目录下所有文档 + git diff 变更
3. Subagent 委托 llm-wiki 完成 wiki 文档维护
4. Subagent 输出 wiki.md 收录报告

## Subagent 定义

```
Subagent：
  description: "Wiki 收录：<feature-name>"
  prompt: |
    你是一个功能文档收录 agent。负责将 feature-workflow 的完整成果同步到项目 wiki。

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill，获取收录模板和规范
    2. 通过 Skill 工具加载 `llm-wiki` skill，获取 wiki 维护规范

    ## 收集输入材料

    从 spec 目录读取所有产出文档（均相对于项目根目录）：

    读取 `docs/specs/<YYYY-MM-dd>-<name>/` 下的所有文件：
    - `spec.md`（需求文档）→ 提取功能描述、用户故事、业务逻辑
    - `spec-review.md`（需求 review）→ 提取关键设计决策
    - `design.md`（共享技术方案）→ 提取 API 设计、数据模型、架构决策
    - `design-review.md`（技术方案 review）→ 提取关键架构决策
    - `design-backend.md`, `design-ios.md`, `design-android.md`, `design-web.md`（各端方案）
    - `plan-backend.md`, `plan-ios.md`, `plan-android.md`, `plan-web.md`（各端实现计划）
    - `code-backend-review.md`, `code-ios-review.md`, `code-android-review.md`, `code-web-review.md`（各端 review 结果）

    使用 `git diff feature/<YYYY-MM-dd>-<name> main --name-only` 获取所有变更文件列表。

    ## 执行 wiki 维护

    按 llm-wiki skill 中 `references/generate-and-update.md` 的规范执行：

    1. **判断影响范围**：根据变更文件列表和 spec/design 文档，确定哪些 wiki 文档需要新建或更新
    2. **更新功能文档**：
       - 如为全新功能 → 按 llm-wiki skill 的 `assets/feature-template.md` 模板创建 `wiki/features/<name>/index.md`
       - 如为已有功能变更 → 增量更新对应章节
    3. **更新 API 文档**：如涉及 API 变更 → 按 llm-wiki skill 的 `assets/api-template.md` 更新 `wiki/api/<domain>.md`
    4. **更新架构文档**：如涉及架构调整 → 按 llm-wiki skill 的 `assets/architecture-template.md` 创建或更新 `wiki/architecture/<topic>.md`
    5. **追加技术决策**：如涉及重要技术选型 → 按 llm-wiki skill 的 `assets/decisions-template.md` 创建 `wiki/decisions/<YYYY-MM-DD>-<title>.md`
    6. **更新索引**：按 llm-wiki skill 的 `assets/index-template.md` 更新 `wiki/index.md`、`wiki/features/index.md`、`wiki/api/index.md` 等
    7. **创建修订记录**：创建 `wiki/revision/<YYYY-MM-DD>-<feature-name>.md`
    8. **验证**：按 llm-wiki skill 的 `references/validation.md` 规范验证 Mermaid 语法和交叉引用

    ## 收录原则

    - **代码是唯一真实来源**：spec/design 是设计意图，代码是实际结果，wiki 以代码为准
    - **增量优先**：只更新变化章节，保留未变化部分
    - **标注来源**：每个关键信息标注源文件路径（`path/to/file.ts:L42`）
    - **新旧共存**：实现被替换时记录「旧 → 新」演进
    - **不确定标注**：无法从代码确认的信息标注 `[待确认]`
    - **产品信息引用**：涉及产品名时引用 PRODUCT.md，不硬编码

    ## 输出收录报告

    按 `assets/wiki-inclusion-template.md` 模板输出到：
    `docs/specs/<YYYY-MM-dd>-<name>/wiki.md`

    ## 完成标志

    - wiki 中各受影响文档已更新或创建
    - wiki.md 收录报告已输出
    - 修订记录已创建
    - wiki 验证通过
```

## 主 agent 后续操作

Subagent 完成后，主 agent：

1. 读取 wiki.md 确认收录结果
2. 调用 `workflow.py advance` 推进到 completed
3. 向用户报告：需求全流程已完成

## 与 llm-wiki 的关系

wiki-inclusion subagent **wrap** llm-wiki skill，不重复实现 wiki 维护逻辑。它负责：

- **收集阶段**：汇总 feature-workflow 全流程的产出
- **委托阶段**：调用 llm-wiki 的子流程完成 wiki 文档操作
- **报告阶段**：向主 agent 汇报收录结果

这样设计的原因是：wiki 维护逻辑（模板填充、拆分决策、验证流程等）已在 llm-wiki 中完整定义，feature-workflow 不应重复。
