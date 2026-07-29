# PRD-14 技术方案审查报告：赚钱中心

> 审查日期：2026-07-29
> 审查范围：design.md / design-backend.md / design-web.md / design-ios.md / design-android.md

---

## 结论

PRD-14 赚钱中心方案已完成本轮回修，`design.md` 与 backend/web/iOS/Android 各端设计现已在以下关键闭环上对齐：

- 赚钱首页由 H5 承载，Native 负责容器、登录承接、播放返回恢复；
- earn 保持独立命名空间，没有明显把 mall 当成“通用能力”直接复用；
- backend 与 web 的基础接口形态对齐，围绕 `GET /api/earn/overview` 与 `POST /api/earn/complete-task`；
- `complete-task` 的 Bearer-only 方案、Native → H5 token 快照同步方式已写实；
- Native → H5 host sync 已统一锁定为 `CustomEvent('earn.hostMessage', { detail })`；
- player → earn 的结果对象、产出方、完成判定与回传顺序已明确。

因此当前版本**可以进入下一阶段（design-human-review）**。

---

## 问题清单

本轮未发现继续阻断 `design-human-review` 的高优先级问题。

### 已关闭问题 1：认证链路未闭环

- shared design 已固定为 **backend Bearer-only + Native 通过 `earn.syncAuthState` 下发 `apiAccessToken` 快照 + H5 仅内存持有**。
- backend/web/iOS/Android 文档均已补充 token 来源、失效回退与不下发 refresh token 的约束。
- `web` 方案已明确 `complete-task` 需显式附带 `Authorization` header，并在 `401` 时重新走登录引导。

### 已关闭问题 2：host sync transport 未统一

- shared design 已把 Native → H5 唯一协议锁定为 `CustomEvent('earn.hostMessage', { detail })`。
- web 文档已改为只监听该事件；iOS/Android 文档已同步为统一注入方式。
- earn 首版不再保留 `window.message` fallback，避免重蹈 mall 历史分叉。

### 已关闭问题 3：player → earn result contract 未定义

- shared design 已新增 `EarnTaskPlayerResult` / `EarnTaskCompletionSchema`，明确 `taskId / videoId / completed / reason`。
- iOS/Android 文档已写明由 player / router / handoff 层统一产出结果对象。
- `completed=true` 的来源、异常退出/后台/中断路径，以及 `earn.completeTask` 与 `earn.restoreContext` 的发送顺序都已落盘。

---

## 结论补充

当前技术方案的主方向可继续保留，且三处阻断性缺口已在文档层完成收口，可进入下一阶段。