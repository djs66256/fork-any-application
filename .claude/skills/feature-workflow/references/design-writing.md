# 技术方案撰写规范

技术方案分两层：shared（`design.md`）和各平台方案（`design-{platform}.md`）。先写 shared，再写各平台。

## 执行者

- `design-shared`：主 agent 直接执行
- `design-platforms`：主 agent 派发 subagent（各端可并行）

## 前置条件

- `spec-human-review` 阶段已通过（已 `workflow.py human-review spec-human-review --approve`）

## design-shared（共享部分）

### 模式检测

在开始工作前，先判断当前是「首次撰写」还是「修复轮次」：

1. 检查产物文件 `docs/specs/<YYYY-MM-dd>-<name>/design.md` 是否已存在
2. 检查 review 报告 `docs/specs/<YYYY-MM-dd>-<name>/design-review.md` 是否已存在

**🔧 修复轮次（两个文件都已存在）**：

design-review 发现了问题，本次任务是**只修复** review 报告中指出的 shared 层面的问题，而非重新撰写。

修复流程：
1. 读取现有的 `design.md`，保留其整体结构和已有内容
2. 读取 `design-review.md`，找出针对 Shared 设计（design.md）的问题（🔴 阻塞 和 🟡 关注）
3. **只修改/补充** review 报告中指出的具体问题，不重写整个方案文件
4. 不修改未被 review 报告指出的章节和内容
5. 修复完成后，在 `design-review.md` 中对应问题的描述后追加 `✅ 已修复于第 N 轮（Shared）`

**🆕 首次撰写（产物文件不存在）**：按下方「主 agent 执行」节描述，从零开始完整撰写。

### 主 agent 执行

1. **了解现状**：调用 `Skill("llm-wiki")` 查阅相关 wiki 文档，理解现有架构和 API 设计
2. **读取代码**：读取 `backend/` 下的现有路由、middleware、数据模型
3. **撰写 design.md**：按 `assets/design-template.md` 模板

核心内容：

- **API 设计**：每个接口需要提供完整的请求/响应格式和 Zod schema
- **数据模型**：涉及新增或变更的数据表、schema 定义
- **跨端共享逻辑**：状态机、缓存策略、推送行为等各端应保持一致的部分
- **安全考虑**：认证授权、数据校验、敏感数据处理
- **性能考虑**：预期 QPS、缓存策略、数据库优化

### API 设计原则

- API 使用 RESTful 设计
- 禁止硬编码常量（localhost、固定 token 等）
- 响应格式统一使用 `{ code, data, message }` 结构
- 错误码统一管理，不与 HTTP 状态码混淆
- 提供 Zod schema 用于前后端共享校验

## design-platforms（各端方案）

### 目录结构

各平台设计 agent 定义在 `references/<platform>-design/*.md` 中：

```
references/
├── backend-design/            # Backend 专属
│   └── service-design.md      # 路由、middleware、service 层、migration、后台任务
├── ios-design/                # iOS 专属
│   └── arch-design.md         # SwiftUI/UIKit、ViewModel、Navigation、持久化
├── android-design/            # Android 专属
│   └── arch-design.md         # Compose/View、ViewModel、Navigation、持久化
├── web-design/                # Web 专属
│   └── frontend-design.md     # React/Vue 组件、状态管理、路由、SSR/CSR
```

### 执行流程

#### 修复轮次判断

在派发 design-platforms subagent 前，主 agent 先判断各平台是否处于修复轮次：

- 如果 `design-{platform}.md` 和 `design-review.md` 都已存在 → 该平台处于修复轮次
- Subagent 的 prompt 中已内置「模式检测」逻辑（见各 `references/<platform>-design/*.md`），会自动识别修复轮次并执行靶向修复
- 主 agent 无需额外传递参数或修改 prompt

**注意**：design-review 可能针对 shared（design.md）而非某具体平台。如果 review 中只涉及 shared 层问题而无某平台的问题，该平台的 subagent 会在模式检测中识别到「无本端问题」并直接跳过，不会重写方案文件。

#### 第一步：加载 agent 定义

扫描 `references/<platform>-design/` 目录，读取所有 `*.md` 文件中的 `Subagent：` 定义块。
将 `<YYYY-MM-dd>-<name>`、`<feature-name>` 占位符替换为实际值。

仅加载涉及平台的目录，跳过不涉及的端。

#### 第二步：并行派发所有 agent

将加载的所有 agent **并行派发**。

**优先使用 agent team 模式派发**，详见 [references/agent-team.md](agent-team.md)。
如 agent team 不可用，回退到独立 subagent 模式，功能和行为保持一致。

各平台 subagent 可同时派发，互不依赖——它们各自独立读取 `spec.md` 和 `design.md`，独立写入 `design-{platform}.md`。

#### 第三步：标记完成并推进

全部 agent 完成后，主 agent 为每个平台调用：

```
workflow.py mark-platform design-platforms <platform> --status completed
```

全部标记完成后调用：

```
workflow.py advance
```

推进到 design-review。

## 注意事项

- 先查阅 wiki 和代码，不要凭空设计
- 跨平台方案必须与 shared design.md 中的 API 设计和数据模型保持一致
- 各端方案需遵循对应端的开发约束
- 如某端不涉及，跳过该端的 agent 加载和派发
