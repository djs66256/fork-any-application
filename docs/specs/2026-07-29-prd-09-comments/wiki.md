# Wiki 收录报告

> 需求：PRD-09 评论系统
> 收录日期：2026-07-29
> 执行人：Claude Code

---

## 一、收录范围

本轮根据当前 worktree 中的真实 Backend / Android / iOS 代码，对 PRD-09 评论系统相关 wiki 进行增量同步，重点覆盖：

- 系统总览
- 首页信息流
- 播放器
- 评论能力
- 认证体系
- 排行体系
- 功能索引
- 修订记录

## 二、收录结论

### 2.1 已更新文档

| 文档 | 变更类型 | 变更摘要 |
|------|---------|---------|
| `wiki/architecture/overview.md` | 更新 | 将评论能力从“未实现”修正为“Backend + Android + iOS 首版已落地”，同步首页/播放器评论入口、comments API、migration、skeleton auth 与恢复语义 |
| `wiki/features/homepage-feed/index.md` | 更新 | 将首页 Feed 卡片从“仅观看/详情”修正为“观看/评论/详情”，补充首页评论容器与登录恢复上下文 |
| `wiki/features/video-player/index.md` | 更新 | 将播放器评论入口从“视觉占位”修正为“真实入口”，补充播放器 comments sheet / bottom sheet、登录恢复语义与 comments API |
| `wiki/features/comments/index.md` | 更新 | 将评论能力从“仅 spec 已定稿”重写为“首版已落地”，补充 comments routes / repository / service / migration / 多端入口与状态机 |
| `wiki/features/index.md` | 更新 | 修正首页信息流、播放器、评论能力索引摘要，避免继续描述为未实现 |
| `wiki/revision/2026-07-29-prd-09-comments-wiki-sync.md` | 更新 | 回写本轮 wiki 同步记录，说明各文档的变更摘要与主要代码来源 |

### 2.2 复核后保持有效的文档

| 文档 | 结论 | 说明 |
|------|------|------|
| `wiki/features/auth/index.md` | 保持有效 | Admin 真实 JWT + role 校验、用户侧 skeleton auth 的分层描述与当前评论实现仍一致 |
| `wiki/features/ranking/index.md` | 保持有效 | 排行预约的登录拦截模式仍可作为评论恢复上下文的参考，且用户侧认证仍是 skeleton auth |

## 三、代码依据

### 3.1 Backend

- `backend/src/app/api/dramas/[id]/comments/route.ts`
- `backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts`
- `backend/src/services/comment/comment.service.ts`
- `backend/src/repositories/repository-registry.ts`
- `backend/src/lib/config.ts`
- `backend/supabase/migrations/20260729000100_add_comments_tables.sql`
- `backend/src/middleware/auth.ts`

### 3.2 Android

- `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`
- `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt`
- `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/PlayerScreen.kt`
- `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`
- `android/app/src/main/java/com/djs66256/short_drama/feature/comments/`

### 3.3 iOS

- `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift`
- `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`
- `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`
- `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift`
- `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`
- `ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift`
- `ios/ShortDrama/Sources/Features/Comments/`

## 四、关键修正点

1. **评论能力不再是“未实现”**
   - Backend 已有 comments routes / schema / service / repository / migration
   - Android / iOS 首页与播放器都已接通评论入口与评论容器

2. **首页与播放器的旧结论已失效**
   - 首页不再是“无评论入口”
   - 播放器不再是“评论仅视觉占位”

3. **认证基线需要继续如实表述**
   - 评论写接口沿用用户侧 skeleton auth
   - 登录恢复只恢复评论上下文，不自动重放原发送或点赞动作

## 五、未纳入本轮的事项

| 事项 | 原因 |
|------|------|
| Web 评论能力 | PRD-09 本期范围不含 Web |
| 真机 / 模拟器黑盒表现 | 当前仓库无设备 testing skill，本轮 QA 以自动化验证与代码核对为主 |
| comments migration 真实 `supabase db push` 成功记录 | 仍被历史 migration 幂等性问题阻塞 |

## 六、结果摘要

- Wiki 已与当前 worktree 中 PRD-09 评论系统的真实实现重新对齐
- 旧的“评论未实现 / 首页无评论入口 / 播放器评论占位”表述已被移除
- 认证体系与排行体系文档经复核后仍可作为评论能力的真实依赖基线

---
*本报告由 feature-workflow 的 wiki-inclusion 阶段产出。*