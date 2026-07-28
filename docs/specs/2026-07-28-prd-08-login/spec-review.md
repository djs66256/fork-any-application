# 需求 Review：PRD-08 用户登录与注册

> Review 日期：2026-07-28
> Review 循环：第 1 轮
> 审查者：AI Agent

## 审查结果总览

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 完整性 | 14 | 14 | 0 | 3 |
| 边界与错误处理 | 12 | 12 | 0 | 0 |
| 一致性（与 wiki） | 4 | 4 | 0 | 2 |
| 可行性 | 3 | 3 | 0 | 1 |
| 平台覆盖 | 2 | 2 | 0 | 0 |
| 术语与范围 | 3 | 3 | 0 | 1 |

## 发现的问题

### 问题 1：会话刷新归属与生命周期契约缺失，P0 的自动恢复链路无法直接落地

- **严重程度**：🔴 高
- **维度**：可行性
- **描述**：当前 spec 同时要求 Backend 提供统一认证 API（`send-code`、`verify-code`、`me`、`logout`），并在 US-03 / US-06 中要求客户端支持 session 恢复、refresh 失败回匿名、401 后统一退登录。但文档没有定义“refresh 由谁负责、何时触发、成功后如何换发 access/refresh token、失败后如何收敛到匿名态”的 canonical 契约。结合现状代码与 wiki：Android 只有 skeleton `AuthInterceptor` 与 `AuthSessionProvider`，iOS 只有自建 `APIClient`，两端都不是现成的 Supabase Auth SDK 直连方案；如果仍坚持 Backend 统一认证入口，就必须补齐 refresh contract，否则“重启恢复登录态”“长时间后台后恢复”“401 后重新登录引导”都无法按同一实现路径落地。
- **修复状态**：✅ 已修复
- **修复说明**：`spec.md` 已新增“6.2 会话生命周期契约”，明确移动端统一通过 Backend REST API 接入 Supabase Auth，补齐 `accessToken` / `refreshToken` / `expiresAt` 持久化规则、冷启动恢复、前后台切换、401 触发 single-flight refresh、refresh 失败回匿名，以及 `GET /api/users/me` / `POST /api/auth/session-refreshes` / `DELETE /api/auth/session` 的职责边界。

### 问题 2：认证 API 只写了能力名，缺少可实现的 canonical contract，且命名未对齐 RESTful 约束

- **严重程度**：🟡 中
- **维度**：完整性
- **描述**：spec 目前仅在目标与数据概览中列出 `send-code`、`verify-code`、`me`、`logout` 这组能力名，但没有给出规范化的 path / method / request body / response schema / auth header / 错误码约定，也没有说明是否沿用 backend 现有 `code / data / message` 响应包裹格式。根目录规则要求 API 采用 RESTful 设计，而现文档中的能力名更像临时接口昵称，不足以支撑 Backend、Android、iOS 对齐实现与测试用例编写。
- **修复状态**：✅ 已修复
- **修复说明**：`spec.md` 已新增“6.3 认证 API 概览”，给出 RESTful canonical contract：`POST /api/auth/otp-requests`、`POST /api/auth/sessions`、`POST /api/auth/session-refreshes`、`GET /api/users/me`、`DELETE /api/auth/session`，同时补齐请求/响应示例、统一 `{ code, data, message }` envelope、错误码映射，以及 Authorization / refresh token 传递规则。

### 问题 3：外部短信能力与开发验证方案的阻塞定义不一致，影响排期与验收边界

- **严重程度**：🟡 中
- **维度**：一致性
- **描述**：spec 一方面把“生产环境短信供应商的商务配置细节”列为范围外，并在 Q-04 中说明本地 / CI 可通过 mock auth service / fixture 完成验证；另一方面又在依赖表中把 `Supabase Auth` 标记为“🚧 待接入 / 阻塞：是”。当前写法没有明确“开发与 CI 的最小可执行依赖”到底是本地 Supabase + mock，还是必须等真实短信配置 ready 后才能启动实现。该不一致会直接影响 Backend 自动化测试、移动端联调和 QA 对“无真实短信通道”场景的验收口径。
- **修复状态**：✅ 已修复
- **修复说明**：`spec.md` 已将依赖拆分为“9.1 开发 / CI 阻塞依赖”和“9.2 生产 / 上线依赖”，明确本地 Supabase 栈或测试 OTP fixture 属于本期开发阻塞项，而真实短信供应商配置属于上线前依赖，不阻塞本期开发与 CI 验证，成功指标与依赖口径已对齐。

### 问题 4：“登录与注册”术语未定义，首登即注册的语义仍然模糊

- **严重程度**：🟡 中
- **维度**：术语与范围
- **描述**：文档标题是“用户登录与注册”，但范围、用户故事和功能详述都只描述了手机号验证码登录、会话恢复与登出，没有明确“注册”是否是独立流程，还是“首次验证码验证成功即自动创建账号”。这会影响 Backend 的用户记录创建语义、`AuthUser` 默认字段、QA 用例命名，以及后续 PRD 对“首登用户”和“已存在用户”的状态区分。
- **修复状态**：✅ 已修复
- **修复说明**：`spec.md` 已新增“6.1 登录与注册语义”，明确本期无独立注册页，首次验证码校验成功即自动创建用户并返回会话，后续同手机号仅创建新 session；同时补齐 `AuthUser.isNewUser` 用途以及首登/登录成功的 QA 与埋点口径。

## 上一轮问题修复验证

| 问题 | 结论 | 验证说明 |
|------|------|---------|
| 问题 1：会话生命周期 / refresh contract 缺失 | ✅ VERIFIED | 已新增会话生命周期契约、刷新时机、single-flight 与回退规则 |
| 问题 2：认证 API canonical contract 缺失 | ✅ VERIFIED | 已新增 RESTful API 列表、请求响应示例、错误码与 Authorization 规则 |
| 问题 3：开发 / CI 与生产短信依赖分层不一致 | ✅ VERIFIED | 已拆分开发 / CI 与生产 / 上线依赖，并统一阻塞口径 |
| 问题 4：登录与注册语义模糊 | ✅ VERIFIED | 已明确首次验证码通过即自动注册，无独立注册页 |

## 遗留问题（需人工决策）

> 本轮未发现必须人工决策的问题。

| 编号 | 问题 | 建议 | 状态 |
|------|------|------|------|
| — | 无 | 可直接进入 `spec-human-review` | — |

## 修改记录

| 轮次 | 修改项 | 修改内容 |
|------|--------|---------|
| 1 | 首轮审查输出 | 新增 4 个待修复问题（1 个高优先级、3 个中优先级），本轮无人工决策项 |
| 1 | 已收敛项记录 | 相比 PM 阶段，当前 spec 已收敛“我的”频道最小登录入口、排行预约登录拦截、协议勾选、退出登录入口、session 失效回匿名、骨架态认证下线目标等关键范围 |
| 2 | 主 agent 修复完成 | 已补齐会话生命周期、认证 API contract、开发/生产依赖分层与自动注册语义 |
| 2 | 第 2 轮复核结论 | 无新增问题，上一轮 4 个问题均已验证修复，可进入 `spec-human-review` |

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（spec-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进
- 第 2 轮复核未发现新增问题；上一轮 4 个问题均已完成修复并验证通过。
- 本轮无遗留人工决策项，建议直接进入 `spec-human-review`。