# 评论能力 (Comments)

> 最后更新：2026-07-29
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

PRD-09 评论系统已经在当前 worktree 中完成首版落地：Backend 已提供评论列表、发表评论、点赞/取消点赞三条 RESTful API；Android 与 iOS 都已把评论入口接到首页 Feed 卡片与播放器页内，并以内嵌 bottom sheet / sheet 的方式承载评论列表、排序、分页、发表评论、点赞，以及未登录写操作的拦截与恢复。评论首版继续遵守现有用户侧 skeleton auth 基线：匿名可读、登录可写；登录成功后**只恢复评论抽屉上下文，不自动重放原发送或点赞动作**。

- **核心价值**：为首页 Feed 与播放器补齐不离开当前上下文的互动能力
- **覆盖范围**：Backend comments routes / schema / repository / service / migration，Android 评论抽屉，iOS 评论 sheet，评论登录恢复上下文
- **当前状态**：Android / iOS / Backend 已实现；Web 本期不实现；真实短信登录服务和真机黑盒验证仍未纳入本轮范围

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android 首页 Feed | 卡片 action row「评论」按钮 | 无独立 route；点击后在首页上下文内打开 `CommentBottomSheet` | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` |
| Android 播放器 | 右侧「评论」操作位 | 无独立 route；点击后由 `PlayerViewModel.openComments()` 打开播放器内评论抽屉 | `android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` |
| iOS 首页 Feed | 卡片 action row `Button("评论")` | 无独立 route；点击后在首页上下文内打开 `.sheet` 承载的 `CommentSheetView` | `ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift` |
| iOS 播放器 | 右侧 `message` action button | 无独立 route；点击后由 `PlayerViewModel.openComments()` 打开播放器内 comments sheet | `ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift` |
| Backend | 评论列表 | `GET /api/dramas/:id/comments?page&pageSize&sort` | `backend/src/app/api/dramas/[id]/comments/route.ts` |
| Backend | 发表评论 | `POST /api/dramas/:id/comments` | `backend/src/app/api/dramas/[id]/comments/route.ts` |
| Backend | 点赞 / 取消点赞 | `POST /api/dramas/:id/comments/:commentId/like` | `backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts` |
| Web | N/A | 本期不实现 comments UI / route | `PRODUCT.md`、`docs/specs/2026-07-29-prd-09-comments/spec.md` |

## 当前现状

### Android

1. 已新增独立 comments 模块，包含 `CommentBottomSheet`、`CommentSheetViewModel`、`CommentUiState` 与登录恢复上下文模型。
   - 源文件：`android/app/src/main/java/com/djs66256/short_drama/feature/comments/`
2. 首页 `HomeScreen` 已在卡片上接入 `onComment`，并以 `activeCommentDramaId` + `pendingCommentLoginContext` 承载评论抽屉与登录恢复状态。
   - 源文件：`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt`
3. 播放器右侧评论按钮已从占位改为真实回调；`PlayerViewModel` 维护 `commentSheetState` 与 `pendingCommentLoginContext`，支持播放器内评论抽屉恢复。
   - 源文件：`android/app/src/main/java/com/djs66256/short_drama/feature/player/ui/components/PlayerComponents.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt`
4. 评论写操作未登录时会发出 `CommentEffect.RequireLogin(CommentLoginContext)`；当前宿主层以 Toast + placeholder dialog 承接登录提示。
   - 源文件：`android/app/src/main/java/com/djs66256/short_drama/feature/comments/model/CommentLoginContext.kt`、`android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt`
5. SearchResultScreen 仅补齐 `HomeDramaCard(..., onComment = {})` 以兼容共享卡片签名，不扩展为本期搜索结果页评论入口。
   - 源文件：`android/app/src/main/java/com/djs66256/short_drama/feature/search/ui/SearchResultScreen.kt`

### iOS

1. 已新增独立 comments 模块，包含 `CommentSheetView`、`CommentSheetViewModel`、`CommentLoginContext` 与对应 UseCase / Repository / DTO。
   - 源文件：`ios/ShortDrama/Sources/Features/Comments/`、`ios/ShortDrama/Sources/Data/DataSources/CommentRemoteDataSource.swift`
2. 首页 `HomeDramaCardView` 已增加评论按钮；`HomeViewModel` 维护 `activeCommentSheet` 与 `pendingCommentLoginContext`，首页通过 `.sheet` 承载评论能力。
   - 源文件：`ios/ShortDrama/Sources/Features/Home/Views/Components/HomeDramaCardView.swift`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`
3. 播放器右侧评论按钮已从静态 `staticButton` 改成真实 action button；`PlayerViewModel` 维护 `isCommentSheetPresented`、`pendingCommentLoginContext` 与 `routeEffect`。
   - 源文件：`ios/ShortDrama/Sources/Features/Player/Views/Components/PlayerRightActionBar.swift`、`ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift`
4. 评论写操作未登录时，`CommentSheetViewModel` 发出 `.requireLogin(CommentLoginContext)`；首页与播放器当前都以 alert / 恢复上下文方式承接，不自动重放原写操作。
   - 源文件：`ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift`、`ios/ShortDrama/Sources/Features/Player/Views/PlayerView.swift`
5. 当前 iOS 构建、测试与 lint 已通过，但 `PlayerViewModel.swift` 仍有既有 `type_body_length` warning，不阻塞本次功能合入。
   - 记录文件：`docs/specs/2026-07-29-prd-09-comments/code-ios-review.md`

### Backend

1. 已新增 comments route handlers、Zod schema、service、repository interface，以及 mock / supabase 双实现。
   - 源文件：`backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts`、`backend/src/services/comment/comment.service.ts`、`backend/src/repositories/interfaces/comment.repository.interface.ts`、`backend/src/repositories/repository-registry.ts`
2. `CommentService` 会先校验 drama 存在，再对 repository 返回值做 schema 校验，保证列表、创建、点赞三条链路的 contract 稳定。
   - 源文件：`backend/src/services/comment/comment.service.ts`
3. 评论列表允许匿名读取；发表评论和点赞继续使用 `getAuthenticatedUserId()`，沿用当前 skeleton auth 基线。
   - 源文件：`backend/src/middleware/auth.ts`、`backend/src/app/api/dramas/[id]/comments/route.ts`、`backend/src/app/api/dramas/[id]/comments/[commentId]/like/route.ts`
4. `config.comments.repository` + `createDefaultCommentRepository()` 允许在 `mock` 和 `supabase` 两种实现间切换。
   - 源文件：`backend/src/lib/config.ts`、`backend/src/repositories/repository-registry.ts`
5. 已新增 Supabase migration，创建 `comments` 与 `comment_likes` 表、索引及 RLS。
   - 源文件：`backend/supabase/migrations/20260729000100_add_comments_tables.sql`

## 核心逻辑

### 流程：评论列表加载、排序与分页

1. 首页或播放器中的评论入口打开评论抽屉 / sheet。
   - Android：首页通过 `activeCommentDramaId`，播放器通过 `commentSheetState.isVisible` 承载可见性。
   - iOS：首页通过 `activeCommentSheet`，播放器通过 `isCommentSheetPresented` 承载可见性。
2. 评论 ViewModel 首屏默认请求 `page=1`、`pageSize=20`、`sort=latest`。
   - Android：`CommentSheetViewModel.open()` -> `loadPage(FIRST_PAGE, append = false)`。
   - iOS：`loadIfNeeded()` -> `reloadFirstPage()`。
3. 切换排序时会重置第一页并重新拉取；追加分页只在已有列表、且仍有下一页时触发。
4. Backend `GET /api/dramas/:id/comments` 解析 `page/pageSize/sort`，并把可选 `userId` 透传到 repository，用于补充 `liked` 等用户态字段。
5. 返回 contract 为 `{ data, pagination }`，其中 `sort` 仅支持 `latest` 与 `hot`。
   - 源文件：`backend/src/lib/schemas.ts`

### 流程：发表评论与点赞切换

1. 评论输入会先做本地校验，只允许 1~500 字。
   - Android：`EMPTY_COMMENT_ERROR_MESSAGE` / `COMMENT_TOO_LONG_ERROR_MESSAGE`
   - iOS：`validateInput()`
2. 若用户未登录，客户端不会直接发出写请求，而是生成结构化 `CommentLoginContext`：
   - Android：`CommentEffect.RequireLogin(buildCommentLoginContext(...))`
   - iOS：`routeEffect = .requireLogin(CommentLoginContext(...))`
3. 已登录时：
   - 发表评论调用 `POST /api/dramas/:id/comments`
   - 点赞调用 `POST /api/dramas/:id/comments/:commentId/like`
4. Backend 对写请求统一要求 `getAuthenticatedUserId()`，匿名写操作返回 401。
5. 创建成功后，客户端会把新评论插入列表顶部；点赞成功后，只局部更新目标评论的 `liked` 与 `likeCount`。

### 流程：登录恢复只恢复上下文，不自动重放写操作

1. `CommentLoginContext` 会记录 `source`、`dramaId` 与待恢复动作元信息。
   - Android 额外记录 `returnRoute` 与 `PendingCommentAction(type, commentId)`。
   - iOS 记录 `source` 与 `PendingCommentAction(kind, commentId)`。
2. 用户在评论内触发未登录写操作时，宿主页面先关闭或保留评论容器，并缓存 pending context。
3. 当前登录承接仍是占位方案：
   - Android：Toast + placeholder dialog
   - iOS：alert
4. 登录成功后只调用 `restoreCommentContext(...)` 或重新打开评论抽屉 / sheet，不会自动再次发送评论或点赞。
5. 该语义与 PRD-09 spec / design 保持一致。
   - 源文件：`docs/specs/2026-07-29-prd-09-comments/spec.md`、`docs/specs/2026-07-29-prd-09-comments/design-ios.md`、`docs/specs/2026-07-29-prd-09-comments/design-android.md`

## 状态管理落点

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `CommentUiState` | `MutableStateFlow` | 页面级 / 组件级 | 聚合列表态、分页态、排序、输入内容、发送中、点赞中 | `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt` |
| Android `CommentEffect.RequireLogin` | `SharedFlow` | 页面级 | 未登录写操作时发出登录恢复上下文 | `android/app/src/main/java/com/djs66256/short_drama/feature/comments/viewmodel/CommentSheetViewModel.kt` |
| Android `activeCommentDramaId` / `pendingCommentLoginContext` | Compose state | 首页级 | 首页评论抽屉与登录恢复上下文 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt` |
| Android `commentSheetState` / `pendingCommentLoginContext` | `StateFlow<PlayerUiState>` | 播放器级 | 播放器评论抽屉与登录恢复上下文 | `android/app/src/main/java/com/djs66256/short_drama/feature/player/viewmodel/PlayerViewModel.kt` |
| iOS `CommentSheetViewModel` published state | `@Published` | 页面级 / 组件级 | 聚合 `listState`、`appendState`、`comments`、`selectedSort`、`inputText` 等 | `ios/ShortDrama/Sources/Features/Comments/ViewModels/CommentSheetViewModel.swift` |
| iOS `activeCommentSheet` / `pendingCommentLoginContext` | `@Published` | 首页级 | 首页 comments sheet 与登录恢复上下文 | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift` |
| iOS `isCommentSheetPresented` / `pendingCommentLoginContext` | `@Published` | 播放器级 | 播放器 comments sheet 与登录恢复上下文 | `ios/ShortDrama/Sources/Features/Player/ViewModels/PlayerViewModel.swift` |
| Backend `CommentListQuerySchema` / `CommentSchema` | Zod schema | 请求级 | 约束评论查询参数与返回结构 | `backend/src/lib/schemas.ts` |
| Backend `comments.repository` | config + registry | 应用级 | 决定运行时走 mock 还是 supabase comments repository | `backend/src/lib/config.ts`、`backend/src/repositories/repository-registry.ts` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 首页信息流 | 入口挂载 | 首页卡片 action row 已扩展评论按钮 |
| 播放器 | 入口挂载 | 播放器右侧操作区已扩展评论按钮与容器承载 |
| 认证体系 | 写接口基线 | 评论写接口沿用 skeleton auth / `x-user-id` / `Bearer <user-id>` |
| 排行体系 | 登录拦截模式参考 | 排行预约的登录拦截思路被评论上下文恢复机制吸收，但评论现已独立实现自己的 context 模型 |
| 数据模型 | 用户摘要来源 | 评论用户摘要对齐 `id / display_name / avatar_url` |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Next.js Route Handlers | Backend comments API 承载 | `backend/src/app/api/dramas/[id]/comments/*` |
| Supabase Postgres | 评论 / 点赞持久化 | `comments`、`comment_likes` 表与 RLS migration |
| Retrofit | Android 评论列表 / 发评论 / 点赞请求 | `ApiService` + `CommentRemoteDataSource` |
| URLSession + APIClient | iOS 评论列表 / 发评论 / 点赞请求 | `CommentRemoteDataSource` |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| Web 本期不实现评论能力 | 无法在 Web 端验证 comments UI / route | 2026-07-29 | 范围明确为 Native 优先 |
| 登录承接仍是占位方案 | Android / iOS 只能验证“拦截 + 恢复上下文”语义，不能验证真实登录回流 | 2026-07-29 | Android placeholder dialog，iOS alert |
| 登录恢复不自动重放写操作 | 用户登录后需要自行再次点击发送或点赞 | 2026-07-29 | 这是已定设计，不是 bug |
| Backend comments migration 的本地 `supabase db push` 仍受历史 migration 阻塞 | 无法在本轮完成 comments migration 的真实推送验证 | 2026-07-29 | 见 `docs/specs/2026-07-29-prd-09-comments/qa-test.md`、`code-backend-review.md` |
| iOS `PlayerViewModel.swift` 仍有 `type_body_length` warning | 不影响功能，但需后续单独治理 | 2026-07-29 | 已在 code review 中记录 |
| 设备级黑盒测试未自动执行 | 当前结论以代码、自动化测试、构建与 QA 文档为主 | 2026-07-29 | 仓库当前无 device-testing skill |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 更新：根据当前 worktree 真实代码，将评论能力从“未实现”修正为“Backend + Android + iOS 首版已落地”，补充首页/播放器入口、评论抽屉、登录恢复上下文、comments routes、repository registry 与 migration 现状 |

---
*本文档由 llm-wiki skill 自动维护。*