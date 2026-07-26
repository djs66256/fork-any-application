# 技术方案撰写规范

技术方案分两层：shared（`design.md`）和各平台方案（`design-{platform}.md`）。先写 shared，再写各平台。

## 执行者

- `design-shared`：主 agent 直接执行
- `design-platforms`：主 agent 直接执行（不再使用 subagent，按平台顺序撰写）

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

### 执行者

**主 agent 直接执行**（不使用 subagent）。主 agent 逐平台顺序撰写方案文件，充分利用上下文连贯性。

### 目录结构

各平台设计参考指南位于 `references/<platform>-design/` 目录中：

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

在开始撰写各端方案前，主 agent 必须根据 `design-review.md` 中的问题归属来决定哪些平台需要修复：

1. 读取 `design-review.md`，找出各 🔴 阻塞 / 🟡 关注 问题的归属平台
2. **只对有问题的平台进行修复**，无问题的平台不修改（其方案文件保持不变）
3. 如果有跨端一致性问题，修复所有涉及的平台方案

例如：review 只指出 `design-ios.md` 和 `design-backend.md` 有缺陷，则只修复 iOS 和 Backend 两个端，Android 和 Web 不修改。

#### 第一步：了解各端现状

对于每个涉及的平台：
1. 读取 `references/<platform>-design/` 下的参考指南，了解该端设计维度和要求
2. 显式读取对应端的 `CLAUDE.md`，确保该端的开发约束已加载到上下文中
3. 调用 `Skill("llm-wiki")` 了解该端已有功能架构
4. 读取对应端的源代码，理解当前实现

#### 第二步：逐平台撰写方案

按各平台的 `assets/design-{platform}-template.md` 模板，逐平台撰写方案文件。

各平台方案之间能够保证一致性（因为都由同一个主 agent 撰写），但可以二次检查。

**撰写顺序建议**：先写 Backend（数据模型和 API 是其他端的基础），再写 Web/iOS/Android。

#### 第三步：标记完成并推进

全部平台方案完成后，主 agent 为每个涉及的平台调用：

```
workflow.py mark-platform design-platforms <platform> --status completed
```

不涉及的平台标记为 skipped：

```
workflow.py mark-platform design-platforms <platform> --status skipped
```

全部标记完成后调用：

```
workflow.py advance
```

推进到 design-review。

### 修复模式

当 design-review 发现问题后回到 design-platforms 修复时：

1. 读取 `design-review.md`，找出各平台归属的问题
2. 对于有问题的平台，读取现有的 `design-{platform}.md`，**只修改/补充** review 指出的问题
3. 不修改未被指出的章节和内容
4. 修复完成后，在 `design-review.md` 中对应问题的描述后追加 `✅ 已修复于第 N 轮（{Platform}）`

## 注意事项

- 先查阅 wiki 和代码，不要凭空设计
- 跨平台方案必须与 shared design.md 中的 API 设计和数据模型保持一致
- 各端方案需遵循对应端的开发约束
- 如某端不涉及，跳过该端的方案撰写，标记为 skipped
