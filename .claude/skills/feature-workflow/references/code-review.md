# Code Review 派发规范

Code review 在 coding subagent **内部**执行。本文件定义如何并行派发专项 code-review subagent，以及汇总结果和修复问题的流程。

## 触发时机

Coding subagent 完成所有 plan 步骤的实现后，在内部执行 code review。

## 审查维度来源

专项 subagent 从以下目录加载：

| 来源 | 路径 | 说明 |
|------|------|------|
| 通用维度 | `references/common-code-review/*.md` | 所有平台必须执行 |
| 平台专属维度 | `references/<platform>-code-review/*.md` | 各平台特有审查项 |
| 扩展维度 | `<platform>/CLAUDE.md` 中 `code review 额外增加 @path` 引用的路径 | 可选，平台额外定制 |

## 执行流程

### 第一步：加载子 subagent 定义

扫描 `references/common-code-review/` 和 `references/<platform>-code-review/` 目录，读取所有 `*.md` 文件中的 `Subagent：` 定义块。
将 `<platform>` 和 `<feature-name>` 替换为实际值。

### 第二步：检查扩展审查

检查 `<platform>/CLAUDE.md` 中是否包含 `code review 额外增加 @` 指令。
如有，从对应路径加载 subagent 定义文件。
注意：`<platform>/CLAUDE.md` 已通过 coding subagent 的 `@<platform>` 自动加载到上下文中。

### 第三步：并行派发所有专项 subagent

将第一步和第二步收集的所有 subagent **并行派发**。

### 第四步：等待并汇总

等待所有 subagent 完成后：

1. 合并所有 subagent 返回的问题列表
2. 按严重程度排序：高 → 中 → 低
3. 去重（同一文件同一行同一类型的问题合并）
4. 按 `assets/code-review-template.md` 模板格式写入 `docs/specs/<YYYY-MM-dd>-<name>/code-<platform>-review.md`

## 修复策略

汇总完成后，按优先级处理问题：

1. **高严重度**（逻辑错误、安全漏洞、测试失败）→ 立即修复代码，修复后重新运行测试
2. **中严重度**（规范违反、性能问题）→ 直接修复代码
3. **低严重度**（代码风格、可维护性）→ 直接修复代码
4. **需人工决策**（架构选择、权衡取舍）→ 不自动修复，记录到 review 文档的「遗留问题」

修复完成后，在 review 文档的「修复记录」中记录修复内容。

## Review 循环

修复完成后，重新运行所有专项 subagent 进行重新审查。
如果仍有新问题，继续修复循环。
如果 3 轮 review 后仍有问题，不再继续循环，记录遗留问题上交给主流程。

## 完成标志

- 所有专项 subagent 已完成
- `code-<platform>-review.md` 已按模板输出
- 所有可自动修复的问题已解决
- 遗留问题已清晰罗列在 review 文档中

## 子 subagent 规范

### 目录结构

```
references/
├── common-code-review/       # 通用维度（所有平台）
│   ├── design-api.md         # 设计一致性与 API 合规
│   ├── code-standards.md     # 硬编码与代码风格
│   └── quality.md            # 错误处理、性能与测试
├── ios-code-review/          # iOS 专属
│   ├── memory-threading.md   # 内存管理与线程安全
│   └── view-tests.md         # View 层级与单元测试
├── android-code-review/      # Android 专属
│   ├── memory-threading.md   # 内存泄漏与线程安全
│   └── lifecycle-tests.md    # Lifecycle 感知与单元测试
├── backend-code-review/      # Backend 专属
│   ├── api-standards.md      # RESTful 合规、响应格式与参数校验
│   └── database-quality.md   # 数据库安全与单元测试
└── web-code-review/          # Web 专属
    ├── ui-ux.md              # 响应式设计与可访问性
    └── quality-tests.md      # 状态管理、Bundle 大小与测试覆盖
```

### 子 subagent 定义格式

每个子 subagent 文件遵循项目 CLAUDE.md 中的 subagent 定义格式：

```
Subagent：
  description: "Code Review：<scope>-<维度>-<feature-name>"
  prompt: |
    ...
```

子 subagent 的 prompt 中：
- 使用 `<platform>`、`<YYYY-MM-dd>-<name>`、`<feature-name>` 作为占位符
- 由 coding subagent 在派发时替换为实际值
- 专注于单一审查维度，不越界审查其他维度
- 返回结构化 JSON 问题列表：`[{file, line?, severity: "high"|"medium"|"low", title, description, suggestion}]`

### 平台 CLAUDE.md 扩展机制

如果某端 `CLAUDE.md`（例如 `ios/CLAUDE.md`）中包含：

```
code review 额外增加 @references/custom-review/
```

则 coding subagent 在 code review 时会额外加载 `references/custom-review/*.md` 中的 subagent 定义并派发。

这使得各端可以在不修改 skill 核心文件的情况下，灵活扩展审查维度。
