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

### 执行流程

主 agent 为每个涉及平台并行派发设计 subagent：

```
Subagent：
  description: "设计：<platform>-<feature-name>"
  prompt: |
    你是一个 <platform> 端的技术方案设计 agent。

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill
    2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`
    3. 读取共享技术方案 `docs/specs/<YYYY-MM-dd>-<name>/design.md`
    4. 读取 `<platform>/CLAUDE.md` 了解该端开发规范
    5. 通过 Skill 工具加载 `llm-wiki` skill，查阅该端相关的功能文档

    ## 任务

    按 `assets/design-platform-template.md` 模板，撰写 <platform> 端技术方案，
    输出到 `docs/specs/<YYYY-MM-dd>-<name>/design-<platform>.md`。

    ## 各端特殊要求

    ### Backend
    - API 实现路由、middleware、service 层设计
    - 数据库 migration 计划
    - 后台任务/队列设计（如适用）

    ### iOS
    - SwiftUI / UIKit 组件设计
    - ViewModel / Presenter 设计
    - Navigation 路由设计
    - 数据持久化策略（CoreData / UserDefaults / Keychain）

    ### Android
    - Compose / View 组件设计
    - ViewModel 设计
    - Navigation 路由设计
    - 数据持久化策略（Room / DataStore / SharedPreferences）

    ### Web
    - React / Vue 组件设计
    - 状态管理方案（Zustand / Context / Redux）
    - 路由设计
    - SSR / CSR 策略

    ## 完成标志

    - design-<platform>.md 已写入
```

### 并行性

各平台 subagent 可同时派发，互不依赖。主 agent 在全部完成后调用 `workflow.py mark-platform design-platforms <platform> --status completed`，全部标记完成后调用 `workflow.py advance` 推进到 design-review。

## 注意事项

- 先查阅 wiki 和代码，不要凭空设计
- 跨平台方案必须与 shared design.md 中的 API 设计和数据模型保持一致
- 各端方案需遵循对应 `CLAUDE.md` 中的开发约束
- 如某端不涉及，跳过该端的 subagent 派发
