# 代码 Review：Android — PRD-08 用户登录与注册

> Review 日期：2026-07-28

## 审查结果

| 维度 | 状态 | 说明 |
|------|------|------|
| 实现与 design 一致性 | ✅ | review-coding 后已对齐 `design-android.md` 中的恢复链：`read -> me -> refresh -> clear`。 |
| 无硬编码常量 | ✅ | 本轮修复未引入新的环境地址、固定 token 或硬编码认证常量。 |
| 代码风格符合平台规范 | ✅ | Kotlin / Compose / ViewModel / StateFlow 写法与现有 Android 代码风格保持一致。 |
| 错误处理完备 | ✅ | refresh 失败与恢复失败均已统一收口到 `Expired -> clearSession() -> Anonymous`。 |
| 性能无明显问题 | ✅ | 前台恢复采用 `MainActivity.onStart()` 最小接入，未引入额外轮询或重复 refresh 机制。 |
| API 调用一致性 | ✅ | `AuthBootstrapper`、`AuthRefreshCoordinator`、`AuthAuthenticator` 的状态迁移与计划语义一致。 |
| 所有测试通过 | ❌ | 当前环境缺少 Java Runtime，未实际执行 Android Gradle test / assemble / detekt。 |
| 生命周期恢复覆盖 | ✅ | 已由仅冷启动恢复调整为 `MainActivity.onStart()` 覆盖冷启动与前台恢复。 |
| 认证状态机闭环 | ✅ | 登录恢复、401 refresh、refresh 失败清 session、logout 本地优先语义已形成闭环。 |

## 变更文件审查

| 文件 | 审查结果 | 问题数 |
|------|---------|--------|
| `android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthBootstrapper.kt` | ✅ | 1 |
| `android/app/src/main/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinator.kt` | ✅ | 1 |
| `android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt` | ✅ | 1 |
| `android/app/src/test/java/com/djs66256/short_drama/core/auth/AuthBootstrapperTest.kt` | ✅ | 2 |
| `android/app/src/test/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinatorTest.kt` | ✅ | 1 |

## 发现的问题

### 问题 1：恢复链在 refresh 失败时一度保留旧 session，与设计语义冲突

- **严重程度**：🔴 高
- **文件**：`android/app/src/main/java/com/djs66256/short_drama/core/auth/AuthBootstrapper.kt`
- **类型**：逻辑错误
- **描述**：在 `getCurrentUserUseCase()` 返回 `AUTH_UNAUTHORIZED` 后，若 `refreshSessionUseCase()` 返回 `ApiResult.Exception` 或服务端错误，旧实现会保留本地 session，导致启动恢复链偏离 `design-android.md` / `plan-android.md` 约定的 `me -> refresh -> clear`。
- **建议修复**：refresh 只要不是 `Success`，就统一执行 `markExpired()` 与 `clearSession()`，避免 UI 继续暴露伪登录态。
- **修复状态**：✅ 已修复
- **修复方案**：`AuthBootstrapper.restoreIfNeeded()` 已改为在 refresh 返回 `ApiResult.Error` 或 `ApiResult.Exception` 时统一清空本地 session，并回到匿名态。

### 问题 2：401 single-flight refresh 在异常路径一度保留旧 session，与状态机闭环冲突

- **严重程度**：🔴 高
- **文件**：`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinator.kt`
- **类型**：逻辑错误
- **描述**：此前为了区分 transient failure，一度尝试在 refresh 的 `HttpException` / `Throwable` 分支恢复旧 session，这会让 `AuthAuthenticator` 与 `AuthBootstrapper` 对 refresh 失败的语义不一致，也会破坏 `Expired -> Anonymous` 的统一状态迁移。
- **建议修复**：让 coordinator 成为 refresh 失败语义的单一出口：`markExpired()` 后 `clearSession()`，并把错误继续上抛为 `ApiResult.Error` 或 `ApiResult.Exception`。
- **修复状态**：✅ 已修复
- **修复方案**：`performRefresh()` 已在 `HttpException` 与普通异常分支统一执行 `markExpired()` + `clearSession()`。

### 问题 3：认证恢复仅在冷启动触发，未覆盖 App 回前台场景

- **严重程度**：🟡 中
- **文件**：`android/app/src/main/java/com/djs66256/short_drama/MainActivity.kt`
- **类型**：可维护性
- **描述**：恢复逻辑原先只在 `onCreate()` 触发，App 从后台回到前台时不会重新校验本地 session 与服务端状态，无法满足 design 中“冷启动 / 前后台恢复”共用恢复链的要求。
- **建议修复**：把恢复触发点上移到 `MainActivity.onStart()`，以最小改动覆盖冷启动进入前台与后台回前台两种路径。
- **修复状态**：✅ 已修复
- **修复方案**：已删除 `onCreate()` 中的 restore 调用，改为在 `onStart()` 中执行 `authBootstrapper.restoreIfNeeded()`。

### 问题 4：回滚实现后，相关测试仍停留在错误语义

- **严重程度**：🟡 中
- **文件**：`android/app/src/test/java/com/djs66256/short_drama/core/auth/AuthBootstrapperTest.kt`、`android/app/src/test/java/com/djs66256/short_drama/core/network/AuthRefreshCoordinatorTest.kt`
- **类型**：验证-修复不完整
- **描述**：实现已回到“refresh 失败清 session”的语义，但测试仍断言 transient exception / `HTTP_503` 会保留 `Authenticated(localSession)`，导致测试预期与真实设计、实现相互背离。
- **建议修复**：把相关测试用例改成断言最终状态为 `Anonymous`，并验证本地 session 已被清理。
- **修复状态**：✅ 已修复
- **修复方案**：已把 `AuthBootstrapperTest` 与 `AuthRefreshCoordinatorTest` 中相关 case 更新为“refresh 失败后清 session”的预期。

## 修复记录

| 轮次 | 修复项 |
|------|--------|
| 1 | 回滚 `AuthBootstrapper` / `AuthRefreshCoordinator` 中“refresh 失败保留旧 session”的错误收敛，恢复为 `Expired -> clearSession() -> Anonymous`。 |
| 1 | 将认证恢复入口从 `MainActivity.onCreate()` 调整到 `MainActivity.onStart()`，补足前台恢复覆盖。 |
| 1 | 同步修正 `AuthBootstrapperTest`、`AuthRefreshCoordinatorTest`，使测试语义与当前实现重新一致。 |

## 上一轮问题修复验证

| 问题编号 | 原问题摘要 | 原修复状态 | 验证结果 | 说明 |
|---------|-----------|-----------|---------|------|
| #1 | `me=401` 后 refresh 失败仍保留旧 session | ✅ 已修复 | ✅ 已验证修复 | 当前 `AuthBootstrapper` 已统一在 refresh 非成功时清 session。 |
| #2 | 认证恢复只在 `onCreate()` 触发 | ✅ 已修复 | ✅ 已验证修复 | 当前恢复入口已迁移到 `MainActivity.onStart()`。 |

## 遗留问题（需人工决策）

无。

## 结论

- [x] ✅ 所有静态审查发现的问题已修复，Android 代码已收口到当前 design / plan 语义
- [ ] ⚠️ 存在遗留问题，需人工确认

补充说明：

- 当前环境执行 `./android/gradlew -version` 仍返回 `Unable to locate a Java Runtime.`，因此 Android 的 test / build / detekt 尚未完成真实验证。
- 现阶段可以如实认定“代码与 review 产物已对齐”，但不能宣称“所有 Android 验证已通过”或进入 merge。