# 技术方案 Review：PRD-08 用户登录与注册

> Review 日期：2026-07-28
> Review 循环：第 2 轮
> 审查者：AI Agent

## 审查结果总览

### Shared 设计 (design.md)

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 与 Spec 一致性 | 6 | 6 | 0 | 1 |
| 功能完整性 | 5 | 5 | 0 | 0 |
| API 完整性 | 5 | 5 | 0 | 1 |
| 数据模型一致性 | 4 | 4 | 0 | 0 |
| 边界与错误处理 | 6 | 6 | 0 | 1 |
| 安全考虑 | 4 | 4 | 0 | 0 |
| 性能考虑 | 3 | 3 | 0 | 0 |

### 平台设计 (design-{platform}.md)

| 平台 | 与 Spec 一致性 | 功能完整性 | 架构 | 文件变更 | API 调用 | 状态管理 | 测试策略 | 总体 |
|------|--------------|----------|------|---------|---------|---------|---------|------|
| Backend | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| iOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Android | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Web | N/A（本期不涉及） | N/A（本期不涉及） | N/A（本期不涉及） | N/A（本期不涉及） | N/A（本期不涉及） | N/A（本期不涉及） | N/A（本期不涉及） | N/A |

## 发现的问题

> 第 2 轮 review 未发现新增 blocker 或 concern。上一轮 4 个阻塞问题已完成文档修复，并通过本轮复核。

| 编号 | 问题 | 严重程度 | 状态 | 说明 |
|------|------|---------|------|------|
| — | — | — | — | — |

## 跨端一致性检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| API 调用与 Shared 设计一致 | ✅ | shared / backend / iOS / Android 已统一采用细粒度 auth 错误码 contract；logout 幂等语义一致。 |
| 数据模型各端一致 | ✅ | `AuthSession`、`AuthUser`、`AuthStatus`、`LoginInterceptionContext` 的主体字段与状态机语义保持一致。 |
| 共享逻辑覆盖 | ✅ | 主动登录入口、排行登录拦截、single-flight refresh、冷启动恢复、退出登录本地优先等主链路均有设计承接。 |
| 错误处理策略一致 | ✅ | iOS 已补齐业务错误码透传链路；Android 已明确安全存储前提与依赖约束；Backend contract 与 Shared 对齐。 |

## 上一轮问题修复验证

| 编号 | 验证结果 | 验证说明 |
|------|---------|---------|
| S-1 | ✅ 已修复 | `design.md` 已把 OTP 发送、登录、refresh、`me`、logout 的错误码与边界表统一收敛为 `AUTH_INVALID_PHONE`、`AUTH_INVALID_CODE`、`AUTH_UNAUTHORIZED`、`AUTH_REFRESH_EXPIRED`、`AUTH_CODE_COOLDOWN`、`AUTH_CODE_EXPIRED`、`AUTH_RATE_LIMITED` 与 `SERVICE_UNAVAILABLE`。 |
| backend-1 | ✅ 已修复 | `design-backend.md` 已将 `DELETE /api/auth/session` 收敛为 `withErrorHandler` + 可选 access token 解析，并明确 token 缺失/失效时仍返回 `200 + code=0` 的幂等退出语义。 |
| ios-1 | ✅ 已修复 | `design-ios.md` 已补齐 `APIClient` 解析 `{ error: { code, message } }`、透传为 `APIError.business(statusCode:businessCode:message:)`，并由 `AuthRemoteDataSource` / `AuthRepository` 基于 `businessCode` 映射到登录态分支。 |
| android-1 | ✅ 已修复 | `design-android.md` 已将敏感会话持久化收敛为 `EncryptedSharedPreferences`，并将 DataStore 限定为 OTP cooldown 等非敏感辅助状态；新增 Jetpack Security 依赖需在 coding 前征得用户同意。 |

## 遗留问题（需人工决策）

> 本轮无新增 design blocker。Android 在 coding 阶段若接入 Jetpack Security，仍需按仓库约束先征得用户同意，但这属于实现前置条件，不再构成当前 design 阶段阻塞。

| 编号 | 问题 | 平台 | 建议 | 状态 |
|------|------|------|------|------|
| — | — | — | — | — |

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（design-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进

补充结论：
- 第 2 轮 review 确认 shared / backend / iOS / Android 已在认证错误码 contract、logout 幂等语义、iOS 业务错误码透传、Android 安全存储方案上完成收敛。
- 当前 design 产物已满足进入 `design-human-review` 的条件，可继续推进后续计划与实现阶段。
