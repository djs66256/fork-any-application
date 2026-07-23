# QA 黑盒测试

qa-blackbox-testing 阶段在代码通过人工确认后执行，作为合入主干前的最后一道质量防线。该阶段根据 spec 撰写黑盒测试文档，并派发 subagent 执行测试。

## 定位

QA 黑盒测试是一个**半自动**阶段：

- 测试文档由 subagent 根据 spec 自动撰写
- 测试执行由 subagent 派发执行
- 设备/模拟器操作方案预留为独立 skill（待定义），若不可用则跳过设备执行步骤
- 测试失败时上报人工

## 前置条件

- code-human-review 阶段已通过
- spec.md 和 design 文档已就绪（用于理解功能边界）
- 各端代码变更已完成

## 执行流程

### 第一步：阅读 spec 和 design

主 agent 阅读当前 feature 的 spec.md 和 design.md（包括各端 design 文档），理解：

- 功能的正常流程和交互边界
- 各端的验收标准
- 异常路径和错误处理逻辑
- 跨端交互场景

### 第二步：撰写 QA 测试文档

派发 **qa-test-writing subagent**，根据 spec 撰写 QA 测试文档。

产物：`docs/specs/<YYYY-MM-dd>-<name>/qa-test.md`

Subagent 定义：

```
Subagent：
  description: "QA 测试文档撰写：<feature-name>"
  prompt: |
    你是一个 QA 黑盒测试文档撰写 agent。你的职责是根据需求文档撰写全面的黑盒测试用例。

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill，获取 QA 测试模板和规范上下文。
    2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`。
    3. 读取共享技术方案 `docs/specs/<YYYY-MM-dd>-<name>/design.md`。
    4. 读取各端技术方案（按需）：
       - `docs/specs/<YYYY-MM-dd>-<name>/design-backend.md`
       - `docs/specs/<YYYY-MM-dd>-<name>/design-ios.md`
       - `docs/specs/<YYYY-MM-dd>-<name>/design-android.md`
       - `docs/specs/<YYYY-MM-dd>-<name>/design-web.md`
    5. 通过 Skill 工具加载 `llm-wiki` skill，读取相关 wiki 文档了解现有功能。

    ## 撰写要求

    按 `assets/qa-test-template.md` 模板格式撰写 `docs/specs/<YYYY-MM-dd>-<name>/qa-test.md`。

    测试用例需覆盖以下维度：

    ### 1. 功能测试
    - 核心功能正向流程（Happy Path）
    - 各功能入口和路径
    - 用户操作的完整链路
    - 各端特有的交互方式

    ### 2. 边界测试
    - 输入边界值（最小/最大/空值）
    - 状态边界（首次/最后一次/中间状态）
    - 数据边界（空列表/单条/大量数据）
    - UI 边界（长文本/特殊字符/极值显示）

    ### 3. 异常测试
    - 网络异常（断网/弱网/超时）
    - 服务端错误（4xx/5xx）
    - 权限不足
    - 并发冲突
    - 资源不足（存储满/内存低）

    ### 4. 兼容性测试
    - 跨端数据一致性
    - 新旧版本兼容（如有）
    - 不同设备/屏幕尺寸

    每个测试用例必须包含：
    - 用例编号（如 QA-001）
    - 测试标题
    - 前置条件
    - 测试步骤
    - 预期结果
    - 涉及平台（backend/ios/android/web）
    - 优先级（P0/P1/P2）

    ## 完成标准

    1. 所有 spec 中定义的功能点都有对应的测试用例
    2. 边界和异常场景覆盖充分
    3. 每个用例测试步骤可操作、可复现，预期结果明确、可验证
    4. 每个用例下方预留了「执行结果」区块，待测试执行 agent 填写
```

### 第三步：执行测试

派发 **qa-test-execution subagent**，按测试文档执行测试。

**设备/模拟器方案预留**：

- 设备/模拟器操作由独立 skill 提供（如 `device-testing` skill）
- 主 agent 在执行测试前检查该 skill 是否已定义
- 如果 skill 已定义：派发 subagent，通过该 skill 操作设备/模拟器执行测试
- 如果 skill 未定义：提示「设备/模拟器测试 skill 尚未定义，跳过设备执行步骤」，仅产出测试文档

产物：`docs/specs/<YYYY-MM-dd>-<name>/qa-test-report.md`

Subagent 定义（设备/模拟器可用时）：

```
Subagent：
  description: "QA 测试执行：<feature-name>"
  prompt: |
    你是一个 QA 黑盒测试执行 agent。你的职责是按测试文档执行黑盒测试并记录结果。

    ## 准备

    1. 读取测试文档 `docs/specs/<YYYY-MM-dd>-<name>/qa-test.md`。
    2. 读取 spec 和 design 了解功能预期行为。
    3. 加载设备/模拟器操作 skill（由主 agent 指定 skill 名称）。

    ## 执行

    1. 按测试文档中的用例顺序逐一执行。
    2. 对每个用例：
       - 按测试步骤操作设备/模拟器
       - 观察实际结果
       - 与预期结果对比
       - 记录测试状态：✅ 通过 / ❌ 失败 / ⏭️ 跳过 / ⚠️ 阻塞
       - 将实际结果、截图/日志等填入用例的「执行结果」区块
    3. 失败的用例需截图或记录详细日志。

    ## 输出

    直接在 qa-test.md 中每个用例下方填写「执行结果」区块，同时在「测试结果汇总」和「未通过用例详细记录」中汇总。

    ## 完成标准

    1. 所有可执行的测试用例已被执行
    2. 每个用例的「执行结果」区块已填写（状态、实际结果、备注）
    3. 「测试结果汇总」和「未通过用例详细记录」已更新
    4. 失败和阻塞的用例有详细记录
```

### 第四步：向用户报告结果并等待决策

主 agent 读取 `qa-test.md` 中的执行结果，汇总并向用户展示：

1. **汇总测试结果**：统计通过/失败/跳过/阻塞数量和比例
2. **列出未通过用例**：展示每个失败/阻塞用例的编号、标题、优先级、失败原因
3. **等待用户决策**，不自动推进。向用户展示决策选项：

> ⚠️ **QA 测试完成，发现 X 个未通过的用例：**
>
> | 用例 | 优先级 | 状态 | 原因 |
> |------|--------|------|------|
> | QA-F-003 | P0 | ❌ 失败 | ... |
> | QA-B-002 | P1 | ⚠️ 阻塞 | ... |
>
> **请决定下一步：**
> - **继续推进** — 问题可接受，进入 worktree-merge
> - **驳回修复** — 回到 coding-platforms 修复后重新测试
> - **记录并推进** — 记录为已知问题，后续版本修复

注意：
- **全部通过**时，仍然向用户展示摘要，由用户确认后推进
- **有未通过**时，必须等用户明确决策后才能继续
- 用户选择「驳回修复」时，回到 coding-platforms，修复完成后重新走 code-human-review → qa-blackbox-testing

## 产物

| 文件 | 说明 |
|------|------|
| `qa-test.md` | QA 黑盒测试文档，包含测试用例定义和每个用例的执行结果 |

## 推进命令

- 全部通过且用户确认：`python3 scripts/workflow.py advance`
- 跳过（无设备 skill 或纯后端改动）：`python3 scripts/workflow.py advance --skip qa-blackbox-testing`
- 用户驳回修复：回到 coding-platforms 阶段

## 完成标准

- ✅ **达成**：测试文档已撰写，测试执行完成（或合理跳过），用户确认推进
- 👤 **等待决策**：存在未通过用例，等待用户选择：继续推进 / 驳回修复 / 记录并推进
- ⏭️ **跳过**：设备/模拟器 skill 未定义，仅产出测试文档后跳过

## 不适用场景

- **纯后端需求**（不涉及 UI/设备交互）：整个阶段可 skip
- **设备/模拟器 skill 未定义**：仅产出测试文档（qa-test.md），设备执行步骤跳过并在报告中注明
- **所有平台均被 skip**：qa-blackbox-testing 阶段也可 skip

## 设备/模拟器方案预留说明

设备/模拟器的操作方案将作为独立 skill 定义（如 `device-testing`），提供以下能力：

- 连接和管理模拟器/真机
- 安装/启动 App
- 模拟用户操作（点击/滑动/输入）
- 截屏和日志采集
- 多设备并行测试

该 skill 的具体实现不在 feature-workflow 的范围内。当该 skill 就绪后，只需在 qa-test-execution subagent 的 prompt 中指定 skill 名称即可集成。

当前如果该 skill 不可用，qa-blackbox-testing 阶段会自动降级：只产出测试文档，不执行设备测试。
