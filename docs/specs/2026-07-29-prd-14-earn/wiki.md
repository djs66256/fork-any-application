# Wiki 收录报告：PRD-14 赚钱中心

> 收录日期：2026-07-29
> 对应需求：spec.md

## 收录内容

| wiki 文档 | 操作 | 变更章节 | 说明 |
|-----------|------|---------|------|
| `wiki/features/earn/index.md` | 新建 | 功能概述 / 入口与路由 / 核心逻辑 / 边界与异常处理 / 多端实现 / API 引用 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 新增赚钱中心主功能文档，收录 H5 `/earn` 页面、Android / iOS Native 容器接入、earn 专属 bridge / host sync、登录承接、任务播放器承接与奖励结算闭环 |
| `wiki/api/earn.md` | 新建 | `GET /api/earn/overview` / `POST /api/earn/complete-task` / 修订历史 | 新增赚钱中心 API 文档，收录 overview 可选鉴权、complete-task Bearer-only、代表性任务限制与幂等完成语义 |
| `wiki/features/index.md` | 更新 | 功能域索引 | 新增“赚钱中心 (Earn Center)”入口，并同步修正 app-shell / auth / player 摘要 |
| `wiki/api/index.md` | 更新 | API 文档索引 | 新增赚钱中心 API 入口 |
| `wiki/features/app-shell/index.md` | 更新 | 功能概述 / 已知限制 / 修订历史 | 将赚钱频道从 placeholder 描述修正为 Native 容器承载的 H5 页面，并明确当前只有商城仍保持 placeholder |
| `wiki/features/auth/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 多端实现 / API 引用 / 状态管理 / 已知限制 / 修订历史 | 补充赚钱中心对统一认证体系的复用，记录 `earn.requestLogin`、`earn.syncAuthState` 与 H5 memory-only token 约束 |
| `wiki/features/video-player/index.md` | 更新 | 功能概述 / 入口与路由 / 核心逻辑 / 边界与异常处理 / 多端实现 / 状态管理 / 依赖关系 / 已知限制 / 修订历史 | 补充赚钱任务复用原生播放器承接的路径、自然播放结束才算完成，以及任务结果回流给赚钱容器的语义 |
| `wiki/architecture/overview.md` | 更新 | 概述 / 架构设计 / 当前首页发现与账号承载结构 / 核心流程调用栈 / 设计决策 / 跨端涉及 / 技术栈总览 / 已知限制 / 修订历史 | 将系统总览更新到 PRD-14 口径，补充 earn H5 容器接入、Earn API、host message transport、登录 / 播放承接与奖励结算顺序 |

## 修订记录

- `wiki/revision/2026-07-29-prd-14-earn.md` 已创建

## 校验说明

- 本轮收录以当前 worktree 中的 Web / Android / iOS / Backend 真实代码为准，以 PRD 与 QA 文档为辅；当文档与代码存在口径差异时，一律以代码实现为准。
- 已按 llm-wiki 规范完成 Mermaid 与交叉引用校验：本轮变更文档均不包含 Mermaid 图表，因此 Mermaid 校验结果为 0 图表 / 0 错误；交叉引用已按 llm-wiki 规范核对 Markdown 内部链接与显式源码文件引用，当前均可解析到有效目标。
- 已重点校对以下 PRD-14 关键事实并在文档中回链源码：赚钱页为 H5 承载但由 Native 容器接入、Native -> H5 唯一 transport 为 `CustomEvent('earn.hostMessage', { detail })`、H5 仅以内存态持有 `apiAccessToken`、`POST /api/earn/complete-task` 保持 Bearer-only、代表性任务只有在 Native 自然播放结束时才会产出 `completed=true`、完成回调顺序固定为 `earn.completeTask` 后 `earn.restoreContext(reason='task-return')`。
- 未创建 earn 专属 decision 文档：当前变更仍可由 feature / api / architecture / revision 四类文档充分覆盖，未发现需要额外沉淀为长期独立决策记录的新分歧。

## Wiki 验证报告

> 验证时间：2026-07-29
> 验证范围：10 个文件

### Mermaid 语法验证

| 文件 | Mermaid 图表数 | 合法 | 非法 |
|------|---------------|------|------|
| `wiki/features/earn/index.md` | 0 | 0 | 0 |
| `wiki/api/earn.md` | 0 | 0 | 0 |
| `wiki/features/index.md` | 0 | 0 | 0 |
| `wiki/api/index.md` | 0 | 0 | 0 |
| `wiki/features/app-shell/index.md` | 0 | 0 | 0 |
| `wiki/features/auth/index.md` | 0 | 0 | 0 |
| `wiki/features/video-player/index.md` | 0 | 0 | 0 |
| `wiki/architecture/overview.md` | 0 | 0 | 0 |
| `wiki/revision/2026-07-29-prd-14-earn.md` | 0 | 0 | 0 |
| `docs/specs/2026-07-29-prd-14-earn/wiki.md` | 0 | 0 | 0 |

非法详情：无。

### 交叉引用验证

| 文件 | 引用数 | 有效 | 无效 |
|------|--------|------|------|
| `wiki/features/earn/index.md` | 127 | 127 | 0 |
| `wiki/api/earn.md` | 30 | 30 | 0 |
| `wiki/features/index.md` | 13 | 13 | 0 |
| `wiki/api/index.md` | 7 | 7 | 0 |
| `wiki/features/app-shell/index.md` | 123 | 123 | 0 |
| `wiki/features/auth/index.md` | 127 | 127 | 0 |
| `wiki/features/video-player/index.md` | 121 | 121 | 0 |
| `wiki/architecture/overview.md` | 77 | 77 | 0 |
| `wiki/revision/2026-07-29-prd-14-earn.md` | 35 | 35 | 0 |
| `docs/specs/2026-07-29-prd-14-earn/wiki.md` | 29 | 29 | 0 |

无效引用详情：无。

### 验证结论

- Mermaid 语法：✅ 全部通过
- 交叉引用：✅ 全部有效（按 llm-wiki 规范校验 Markdown 内部链接与显式源码文件引用）

## 收录结论

- [x] ✅ 所有 PRD-14 赚钱中心直接影响的 wiki 内容已同步
- [x] ✅ 赚钱中心 feature / api 文档、相关增量更新、revision 记录与收录报告已建立
- [x] ✅ 系统总览、应用壳、认证体系与播放器文档已从旧的 placeholder / 未接入口径同步到当前真实实现

当前仍保留的实现限制：

- 商城（mall）仍按 H5 策略保留 placeholder tab，尚未像赚钱一样接入真实 Native 容器。
- 赚钱 H5 的 `apiAccessToken` 只存在于内存态；页面重建或刷新后必须等待宿主再次发送 `earn.syncAuthState`。
- 赚钱奖励当前只允许代表性任务完成，且必须依赖 Native 播放承接层自然播放结束；真实提现、账本、连续看剧发奖、多任务并发与风控均未实现。
- Android 与 iOS 对 `earn.restoreContext(reason='task-return')` 的 `preserveScroll` 语义尚未统一：Android 为 `false`，iOS 为 `true`。
- 设备 / 模拟器黑盒测试在本轮 workflow 中按规范降级，当前 wiki 证据仍主要来自源码、自动化测试与 QA 文档。