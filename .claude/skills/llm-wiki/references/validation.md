# 验证流程

验证由维护 subagent 在完成 wiki 变更后自行执行（按 generate-and-update.md 中的「自我验证」步骤）。也可由主 agent 单独触发以下 subagent 进行独立验证。

## 验证触发

以下情况触发验证：

- 生成了新的 wiki 文档
- 更新了已有 wiki 文档
- 用户明确要求「验证 wiki」

## 验证项

### 1. Mermaid 语法验证

检查所有变更的 `.md` 文件中是否包含 Mermaid 图表，若有则逐一验证语法合法性。

使用 `mermaid-validate` CLI 工具：

```bash
mermaid-validate <file>
```

验证文件列表为本次生成/更新的所有 `.md` 文件。

### 2. 交叉引用验证

检查文档中的所有内部链接是否正确：

- `wiki/` 内部链接（如 `[xxx](../api/xxx.md)`）→ 目标文件是否存在
- 源代码文件引用（如 `` `web/src/xxx.ts:L42` ``）→ 文件路径是否存在（不校验行号）

## Subagent 编排

两项验证并行执行，各用一个 subagent。模板如下：

### Mermaid 验证 Subagent

```
Subagent（通用型）：
  description: "验证 wiki Mermaid 语法：[文档名称]"
  subagent_type: "general-purpose"
  prompt: |
    首先调用 Skill("llm-wiki") 加载 llm-wiki 验证规范。

    对以下 wiki 文件执行 Mermaid 语法验证：

    - <wiki/features/xxx/index.md>
    - <wiki/features/xxx/sub.md>

    1. 逐一读取每个文件，检查是否包含 Mermaid 代码块（```mermaid ... ```）
    2. 对包含 Mermaid 的文件，执行 mermaid-validate <文件路径>
    3. 输出验证报告：每个文件的 Mermaid 数量、合法/非法数量，非法图表的错误信息
```

### 交叉引用验证 Subagent

```
Subagent（通用型）：
  description: "验证 wiki 交叉引用：[文档名称]"
  subagent_type: "general-purpose"
  prompt: |
    首先调用 Skill("llm-wiki") 加载 llm-wiki 验证规范。

    对以下 wiki 文件执行交叉引用验证：

    - <wiki/features/xxx/index.md>
    - <wiki/features/xxx/sub.md>

    1. 逐一读取每个文件，提取所有内部链接：
       - Markdown 链接格式：[text](path) → 提取 path
       - 代码文件引用格式：`path:LNN` → 提取 path
    2. 对每个 path，检查目标文件是否存在（不作行号校验）
    3. 忽略外部 URL（http/https 开头）
    4. 输出验证报告：每个链接的路径、是否存在、不存在的链接明细
```

## 验证报告格式

两个 subagent 的输出合并为统一验证报告：

```markdown
## Wiki 验证报告

> 验证时间：YYYY-MM-DD
> 验证范围：N 个文件

### Mermaid 语法验证

| 文件 | Mermaid 图表数 | 合法 | 非法 |
|------|---------------|------|------|
| xxx.md | 2 | 2 | 0 |

非法详情（如有）：
- `xxx.md` 第 3 个图表：语法错误 ...

### 交叉引用验证

| 文件 | 引用数 | 有效 | 无效 |
|------|--------|------|------|
| xxx.md | 5 | 4 | 1 |

无效引用详情（如有）：
- `xxx.md` → `../api/missing.md`：目标文件不存在
- `xxx.md` → `web/src/nonexistent.ts:L42`：源文件不存在

### 验证结论

- Mermaid 语法：✅ 全部通过 / ❌ 有 N 个错误
- 交叉引用：✅ 全部有效 / ❌ 有 N 个无效引用
```

## 验证失败处理

- **Mermaid 语法错误**：直接修正图表语法，重新验证
- **内部链接失效**：检查目标文件是否路径写错、是否尚未创建、是否已删除。路径写错的修正；目标尚未创建的先标注 `[待创建]`；已删除的移除引用
- 修正后重新执行验证，直到全部通过
