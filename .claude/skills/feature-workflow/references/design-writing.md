# 技术方案撰写规范

技术方案分两层：shared（`design.md`）和各平台方案（`design-{platform}.md`）。先写 shared，再写各平台。

## 执行者

- `design-shared`：主 agent 直接执行
- `design-platforms`：主 agent 派发 subagent（各端可并行）

## 前置条件

- `spec-human-review` 阶段已通过（已 `workflow.py human-review spec-human-review --approve`）

## design-shared（共享部分）

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
