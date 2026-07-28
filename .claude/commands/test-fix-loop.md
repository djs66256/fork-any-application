---
description: 测试-修复循环：派发 admin-e2e-test subagent 测试 admin web，修复发现的 bug，最多3轮
---

## 流程概述

本命令执行 admin web 的测试-修复循环：

1. **确认服务状态** — Backend (3001) 和 Web (3000) 均在运行
2. **Round N** (最多 3 轮)：
   a. 派发 admin-e2e-test subagent 执行 Chrome DevTools 测试
   b. 收集 subagent 输出的 bug 报告
   c. 如果无 bug 或已是第 3 轮 → 退出循环
   d. 修复所有报告的 bug（仅修改 web/ 和 backend/ 下的文件）
   e. 进入 Round N+1
3. **汇总报告** — 展示每轮发现和修复的 bug 数量

## 执行步骤

### 0. 前置检查

确认服务运行状态：

```bash
curl -s http://localhost:3001/api/health | head -1
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/admin/login
```

如果 Backend (3001) 未运行，启动它：
```bash
cd backend && npm run dev &
```

如果 Web (3000) 未运行，启动它：
```bash
cd web && npm run dev &
```

等待两个服务就绪后再继续。

### 1. Round 1: 首次测试

加载 admin-e2e-test skill，获取子 agent prompt：

```
Skill("admin-e2e-test")
```

从 skill 的「测试 subagent」定义中提取 prompt。

**派发测试 subagent：**

使用 Agent 工具派发 subagent：
- `subagent_type`: `claude`
- `description`: `Admin E2E 测试 Round 1`
- `prompt`: admin-e2e-test skill 中的 subagent prompt，加上测试账号信息

Subagent 需要的测试账号信息从环境变量 `.env.local` 或 Supabase 本地环境中获取。如果找不到测试账号，先通过 Supabase admin API 创建一个 admin 用户。

Subagent 执行完成后读取其输出，解析其中的 bug 报告。

**判定规则：**
- 如果 bug 报告为空或无 P0/P1 严重 bug → 测试完成，跳到汇总
- 如果有 bug → 进入修复步骤

**修复所有报告的 bug：**

按 bug 严重程度排序（P0 → P1 → P2），逐一修复：
- CSS/样式 bug → 修改 `web/src/` 下的 CSS module 文件
- 逻辑 bug → 修改 `web/src/` 或 `backend/src/` 下的 TypeScript 文件
- 确保所有颜色使用 CSS 变量而非硬编码值

修复完成后验证编译通过：
```bash
cd web && npm run build 2>&1 | tail -5
cd backend && npm run build 2>&1 | tail -5
```

### 2. Round 2: 验证修复

重新派发测试 subagent（同 Round 1），要求其：
- 重新检查上一轮发现的所有 bug 是否已修复
- 继续完整的测试流程

**判定规则：**
- 所有 P0/P1 bug 已修复且无新 bug → 测试完成
- 仍有 bug → 继续修复

### 3. Round 3: 最终验证

重新派发测试 subagent，做最终的完整回归测试。

**无论是否有 bug，Round 3 后都停止。** 记录仍存在的 bug 为「已知问题」。

### 4. 汇总报告

向用户展示：

```
# Test-Fix-Loop 汇总

## Round 1
- 发现 bug: <N> (P0: <N>, P1: <N>, P2: <N>)
- 已修复: <N>
- 未修复: <N>

## Round 2
- 发现 bug: <N>
- 已修复: <N>

## Round 3
- 发现 bug: <N>
- 已修复: <N>

## 遗留问题
- <BUG-ID>: <标题> — <原因>
```

## 约束

- 只修改 `web/` 和 `backend/` 下的文件
- 不修改第三方库或 node_modules
- 修复后运行 `npm run build` 确认编译通过
- 每轮测试前确认 dev server 仍在运行
- 测试 subagent 可能超时（复杂页面操作），设置合理的 timeout
- 如果 subagent 崩溃或超时，记录失败并重试一次
