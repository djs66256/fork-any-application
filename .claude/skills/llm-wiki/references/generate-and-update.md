# 生成与更新流程

生成与更新操作**一律通过 subagent 执行**。主 agent 只负责识别时机和收集输入。

## 主 Agent 职责

1. **识别触发时机** — 判断是否需要生成或更新 wiki（参考下方触发条件）
2. **收集输入材料** — 整理用户描述、spec 文档路径、git diff 摘要等
3. **启动 subagent** — 按模板发起
4. **报告结果** — subagent 完成后，向用户简述变更

**主 agent 不直接读写 wiki 文件。**

## 触发机制

### 何时生成

当 `wiki/` 为空或缺少某功能域的文档时：

- 首次接入项目时，按功能域逐个生成
- 新增功能模块后，生成对应功能文档
- 用户明确要求「生成 wiki」

### 何时更新

**每次开发任务完成后自动触发**。这是核心约束，不是可选的。

开发完成的标志：用户说「做完了」「OK」「可以」等收尾语句，或代码已修改并验证通过。

## Subagent 模板

```
Subagent（通用型）：
  description: "维护 wiki：[功能域/变更简述]"
  prompt: |
    首先调用 Skill("llm-wiki") 加载完整规范。

    ## 任务

    维护当前项目的 wiki 文档。

    ## 输入

    - 用户需求：<用户原始描述>
    - 参考文档：<spec、设计文档等路径，如有>
    - 变更上下文：<git diff 摘要或变更文件列表，如有>

    ## 执行

    根据输入自行判断是新建还是增量更新，然后：

    1. 如为新建：扫描各端代码，按 assets/feature-template.md 模板填充各章节
    2. 如为更新：根据变更文件映射受影响文档，增量更新对应章节（不重写全文）
    3. 自行判断是否需要拆分（不询问），按 references/wiki-standards.md 中的拆分策略执行
    4. 如有 API 变更，按 assets/api-template.md 同步更新 wiki/api/
    5. 如有架构变更，按 assets/architecture-template.md 创建或更新 wiki/architecture/
    6. 如有技术决策，按 assets/decisions-template.md 追加到 wiki/decisions/
    7. 更新各层 index.md 的时间戳
    8. 创建 wiki/revision/<YYYY-MM-DD>-<简述>.md 修订记录
    9. 按 references/validation.md 启动验证 subagent 并输出变更摘要

    ## 原则

    - 真实来源必须是代码。可参考 spec 等文档，但 spec 与代码不一致时以代码为准
    - 每个关键信息标注源文件路径（格式：`web/src/xxx.ts:L42`）
    - 无法从代码确认的信息标注 `[待确认]`
    - 增量优先：只更新变化章节，保留未变化部分
    - 新旧共存：实现被替换时记录「旧→新」演进，而非直接删除
    - 更新后在文档头部记录本次更新时间，在修订历史中追加记录

    ## 修订记录格式

    每次完整更新一个修订文件。文件名：`wiki/revision/<YYYY-MM-DD>-<简述>.md`。

    以每个被修改的 wiki 文件为 section：

    ```markdown
    # <YYYY-MM-DD> — <简述>
    > 触发来源：用户需求 / 开发完成自动更新 / 查阅发现不一致

    ## wiki/features/xxx/index.md
    - **变更章节**：「业务逻辑」「代码架构」
    - **变更摘要**：一句话描述

    ## wiki/api/xxx.md
    - **变更章节**：「POST /api/xxx」
    - **变更摘要**：一句话描述
    ```

    ## 变更映射参考

    | 变更类型 | 影响 |
    |---------|------|
    | 新增 API 接口 | 功能文档 API 引用 + wiki/api/ |
    | 修改业务逻辑 | 功能文档业务逻辑、代码架构 |
    | 新增/修改页面 | 功能文档入口与路由、代码架构 |
    | 状态管理变更 | 功能文档状态管理 |
    | 新增功能模块 | 新建功能目录 + 各层索引 |
    | 架构调整 | architecture/ 相关文档 |
```
