# 进化流程详解

## 触发标准

满足以下**任一条件**时启动：

1. **用户主动**：用户说"进化"、"优化 harness"、"改进一下 skill"、"开始进化"
2. **数量阈值**：同一改进目标下「待进化」记录 ≥ 3 条（主 agent 在记录完成后提示用户）
3. **严重性阈值**：存在 `blocker` 级别的记录（主 agent 提示用户）

## 进化分支

根据改进目标类型，走不同的进化分支：

| 改进目标 | 进化方式 | 理由 |
|---------|---------|------|
| `.claude/skills/` 下的 skill | **走 skill-creator evals 流程**：设计测试用例 → 基线对比 → 评估效果 → 迭代优化 | Skill 的触发和输出质量需要客观衡量，不能仅凭感觉改 |
| `CLAUDE.md`、`PRODUCT.md`、`docs/` 等文档 | **走直接修改流程**：分析 → 方案 → 用户审核 → 执行 | 文档规则的正确性由人判断，不需要 evals |
| `docs/short_memory.md` | 在 skill/文档进化时间步处理：提升为长期规则后清理 | 记忆是过渡态，不单独进化 |

## Skill 进化流程（走 skill-creator）

对 skill 的进化，通过 `skill-creator` skill 的标准流程进行：

```
[分析记录] → [设计测试用例] → [运行 evals] → [迭代优化] → [归档]
```

### 步骤 1：分析记录

- 读取 `docs/evolution/index.md` 中该 skill 相关的所有「待进化」记录
- 通读每条记录的问题描述和期望行为
- 确认改进方向（prompt 调整、流程变更、新增/删除能力线等）

### 步骤 2：设计测试用例

这是 skill 进化的关键步骤。根据记录中的问题，设计能验证改进效果的测试用例。

**用例设计原则**：
- 每条 record 中的问题，至少对应一个能复现该问题的测试用例
- 用例应该是真实用户会说的 prompt，不是抽象描述
- 同时设计正向用例（改了之后应该通过的）和回归用例（改了之后不应该破坏的）

用例格式参照 skill-creator 的 `evals/evals.json`：

```json
{
  "skill_name": "feature-workflow",
  "evals": [
    {
      "id": 1,
      "prompt": "用户可能说的实际 prompt",
      "expected_output": "预期输出描述",
      "files": []
    }
  ]
}
```

### 步骤 3：运行 evals

调用 `Skill("skill-creator")` 加载 skill-creator，然后按 skill-creator 的流程执行：

1. 在 `<skill-name>-workspace/` 下创建迭代目录
2. 运行 with-skill 和 baseline（旧版 skill 快照）对比
3. 收集 timing 和 token 数据
4. 运行 grader 评估每个 assertion
5. 生成 benchmark 和 eval viewer

详细步骤参见 skill-creator SKILL.md 中「Running and evaluating test cases」章节。

### 步骤 4：迭代优化

根据 evals 结果和用户反馈，改进 skill：
- 如果 evals 显示改善，应用改动
- 如果 evals 显示退步，回退并重新分析
- 反复直到用户满意

### 步骤 5：归档

- 更新 `docs/evolution/index.md` 中对应记录状态为「已进化」
- 在进化目录下创建 `evolution.md`（用 [assets/evolution-template.md](../assets/evolution-template.md)），记录：
  - 进化方案摘要
  - 关联记录
  - evals 结果摘要
  - 最终改动的 diff

## 文档进化流程（直接修改）

对 CLAUDE.md 等非 skill 文件的进化，走直接修改流程：

```
[分析] → [制定方案] → [用户审核] → [执行] → [归档]
```

### 步骤 1：分析

收集信息：
- 读取 `docs/evolution/index.md` 中相关「待进化」记录
- 读取要修改的文件当前内容
- 搜索是否有其他 CLAUDE.md 规则与拟议改动冲突
- 检查 `docs/short_memory.md` 中是否有相关短期记忆可一起提升

分析方法：

**根因分析**：追问「为什么会出现这个问题？」→ 设计疏忽？流程缺失？约定没文档化？

**影响范围分析**：
- 改动会影响哪些 skill 或流程？
- 是否有 pending 记录需要同步处理？

**聚类分析**：多条记录指向同一文件的不同位置时，判断是否共享同一根因。

### 步骤 2：制定方案

按 [assets/evolution-template.md](../assets/evolution-template.md) 中「文档进化」模板撰写进化方案，保存到 `docs/evolution/<YYYY-MM-dd>-<name>/evolution.md`。

### 步骤 3：用户审核

向用户展示方案摘要，逐项确认：
- **全部通过** → 进入执行
- **部分通过** → 调整被驳回的部分，重新确认
- **全部驳回** → 标记记录为「已驳回」，结束（记录保留但状态变更）

### 步骤 4：执行

按确认后的方案逐项实施，执行顺序：
1. 先改模板/asset
2. 再改 reference
3. 再改 SKILL.md
4. 再改 CLAUDE.md
5. 最后处理 short_memory.md（提升后清理）

### 步骤 5：归档

- [ ] 所有变更已写入对应文件
- [ ] `docs/evolution/index.md` 中对应记录状态已更新为「已进化」
- [ ] 在 evolution.md 末尾补充「执行报告」章节（格式见模板）
- [ ] 如在执行中方案有调整，执行报告中已记录差异及原因
- [ ] `docs/short_memory.md` 中已提升的内容已清理

## 进化后

进化不是终点。如果改动引入了新问题，开启新一轮记录→进化循环。
