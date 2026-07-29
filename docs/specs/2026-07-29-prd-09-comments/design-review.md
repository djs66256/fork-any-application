# 技术方案 Review：PRD-09 评论系统

> Review 日期：2026-07-29
> Review 循环：第 2 轮
> 审查者：AI Agent

## 审查结果总览

### Shared 设计 (design.md)

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 与 Spec 一致性 | — | ✅ | 0 | 0 |
| 功能完整性 | — | ✅ | 0 | 0 |
| API 完整性 | — | ✅ | 0 | 0 |
| 数据模型一致性 | — | ✅ | 0 | 0 |
| 边界与错误处理 | — | ✅ | 0 | 0 |
| 安全考虑 | — | ✅ | 0 | 0 |
| 性能考虑 | — | ✅ | 0 | 0 |

### 平台设计 (design-{platform}.md)

| 平台 | 与 Spec 一致性 | 功能完整性 | 架构 | 文件变更 | API 调用 | 状态管理 | 测试策略 | 总体 |
|------|--------------|----------|------|---------|---------|---------|---------|------|
| Backend | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| iOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Android | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Web | —（本期不涉及） | —（本期不涉及） | —（本期不涉及） | —（本期不涉及） | —（本期不涉及） | —（本期不涉及） | —（本期不涉及） | — |

## 发现的问题

本轮未发现确认成立的问题。

## 跨端一致性检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| API 调用与 Shared 设计一致 | ✅ | Shared / Backend 继续以 `{ error: { code, message } }` 为错误 envelope；iOS 方案已明确当前 `APIClient` 只稳定暴露 HTTP status + message，Android 方案也不再假设不存在的导航 helper。 |
| 数据模型各端一致 | ✅ | `PendingCommentAction`、`CommentLoginContext`、`pagination.total`、评论实体与点赞返回结构在 shared / iOS / Android / backend 文档中保持一致。 |
| 共享逻辑覆盖 | ✅ | 登录恢复策略在各端都收敛为“恢复来源页与评论抽屉上下文，不自动重放写操作”。 |
| 错误处理策略一致 | ✅ | iOS 方案按 HTTP status + message 建模；Android 方案按结构化上下文建模登录恢复；两端都与当前代码基线和 backend 错误 envelope 对齐。 |

## 上一轮问题修复验证

### 已验证修复：ios-1 iOS 错误码映射方案与当前 APIClient 能力不一致

- **验证结果**：✅ 已修复
- **验证说明**：`design-ios.md` 已明确当前 `APIClient` 只向上暴露 HTTP status + message，不暴露 nested `error.code`；错误处理章节也已改为按 HTTP status 与 message 做分流，不再要求 comments 方案依赖当前不存在的业务错误码透传能力。这与真实代码基线一致：`ios/ShortDrama/Sources/Core/Network/APIClient.swift` 仅抛出 `APIError.server(code: Int, message: String)`，内部 `ErrorResponse` 虽可解析 nested `error.message`，但不会把 `error.code` 向上暴露。

### 已验证修复：android-1 Android 登录恢复上下文设计丢失 pending action 信息，且依赖不存在的导航 helper

- **验证结果**：✅ 已修复
- **验证说明**：`design-android.md` 已补齐 `CommentLoginContext` 与 `PendingCommentAction`，结构中包含 `source`、`dramaId`、`action`、可选 `commentId` 与 `returnRoute`；同时文档已明确当前不存在 `AppDestination.homeWithComment(dramaId)`，首页恢复路径只能先回到现有 `home` route，再由首页页面级状态重新打开对应 `dramaId` 的评论抽屉。这与真实导航基线一致：`android/app/src/main/java/com/djs66256/short_drama/navigation/AppDestination.kt` 仅提供 `home`、`play/{videoId}` 等现有 route/helper，并无 `homeWithComment`；`MainNavigationViewModel` 当前也只承接 `PendingRoute` 级别的来源页恢复。

## 与代码 / wiki 一致性复核

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 移动端业务写接口认证基线 | ✅ | design/spec 继续以 skeleton auth 为准，和 `backend/src/middleware/auth.ts` 及 `wiki/features/auth/index.md` 一致。 |
| 首页 Feed / 播放器评论现状引用 | ✅ | 文档继续基于“首页无评论入口、播放器仅有评论视觉位”的现状推进，和 `wiki/features/homepage-feed/index.md`、`wiki/features/video-player/index.md`、`wiki/features/comments/index.md` 一致。 |
| 登录拦截参考基线 | ✅ | iOS 参考 `RankingLoginContext`、Android 参考 `RequireLogin(returnRoute)` / `PendingRoute` 的表述，和 `wiki/features/ranking/index.md` 一致，且没有再把这些局部模式误写成已存在的通用 comments 基础设施。 |

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（design-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进

所有问题已修复，可进入下一阶段（design-human-review）。
