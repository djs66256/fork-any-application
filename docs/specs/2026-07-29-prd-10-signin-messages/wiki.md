# PRD-10《签到与消息系统》wiki 收录报告

## 收录范围

本次基于当前 worktree 中的真实代码与 PRD-10 文档，对以下能力完成 wiki 收录与增量更新：

- 认证体系中的 PRD-10 新鉴权边界：签到可选登录 + installationId、互动消息强制登录、消息页登录回流
- 首页菜单中的消息预览入口与独立消息中心
- 首页首屏后的签到浮层与 7 日签到板
- 消息系统的系统消息 / 互动消息双分区
- 系统总览与 API 索引中的 PRD-10 contract 补齐

主要依据来自：

- PRD 文档：`docs/specs/2026-07-29-prd-10-signin-messages/spec.md`、`docs/specs/2026-07-29-prd-10-signin-messages/design.md`、`docs/specs/2026-07-29-prd-10-signin-messages/design-backend.md`、`docs/specs/2026-07-29-prd-10-signin-messages/design-android.md`、`docs/specs/2026-07-29-prd-10-signin-messages/design-ios.md`、`docs/specs/2026-07-29-prd-10-signin-messages/qa-test.md`
- Backend 实现：`backend/src/app/api/check-ins/**`、`backend/src/app/api/messages/**`、`backend/src/services/check-in/check-in.service.ts`、`backend/src/services/message/message.service.ts`、`backend/src/lib/schemas.ts`、`backend/src/middleware/auth.ts`、`backend/src/repositories/repository-registry.ts`、`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql`
- Android 实现：`android/app/src/main/java/com/djs66256/short_drama/feature/home/**`、`android/app/src/main/java/com/djs66256/short_drama/feature/menu/**`、`android/app/src/main/java/com/djs66256/short_drama/feature/messages/**`、`android/app/src/main/java/com/djs66256/short_drama/navigation/**`、`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt`、`android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt`
- iOS 实现：`ios/ShortDrama/Sources/Features/Home/**`、`ios/ShortDrama/Sources/Features/MenuPanel/**`、`ios/ShortDrama/Sources/Features/Messages/**`、`ios/ShortDrama/Sources/App/**`、`ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift`、`ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift`

## 本次变更清单

### 新增文档

1. `wiki/features/check-in/index.md`
   - 新增签到能力文档。
   - 收录首页冷启动签到浮层、7 日签到板、服务端业务日、匿名 installationId、账号优先主体规则、同日幂等与第 8 天新一轮规则。
   - 补充 Android / iOS 本地关闭态、评论 / 登录模态互斥、Web 不实现等边界。

2. `wiki/features/messages/index.md`
   - 新增消息系统文档。
   - 收录菜单消息预览、消息中心、系统消息匿名可看、互动消息登录门槛、登录后留在消息页、preview 204 空态与 mock interaction repository 现状。

3. `wiki/api/check-ins.md`
   - 新增签到 API 文档。
   - 收录 `GET /api/check-ins/status` 与 `POST /api/check-ins` 的 request/response、错误码、installationId header、`server_date` 权威值、幂等规则与客户端接线关系。

4. `wiki/api/messages.md`
   - 新增消息 API 文档。
   - 收录 `GET /api/messages/preview`、`GET /api/messages/system`、`GET /api/messages/interactions` 的鉴权差异、204 空态、分页结构、interaction 强制登录与客户端解码方式。

5. `wiki/revision/2026-07-29-prd-10-signin-messages.md`
   - 新增 PRD-10 对 wiki 的 revision 记录。
   - 逐项记录新增 / 更新的 wiki 文件、摘要和主要代码来源。

### 增量更新文档

1. `wiki/features/index.md`
   - 新增“签到能力 (Check-In)”与“消息系统 (Messages)”入口。
   - 同步修正应用壳 / 认证 / 首页信息流在索引中的 PRD-10 摘要。

2. `wiki/api/index.md`
   - 新增 Check-Ins API 与 Messages API 入口。

3. `wiki/features/auth/index.md`
   - 补充 PRD-10 的新认证边界：
     - `messages/interactions` 强制登录
     - `messages` 登录回流语义
     - `check-ins` 可选登录 + installationId 兜底
   - 同步补入 Android / iOS / Backend 的对应实现和状态承载。

4. `wiki/features/app-shell/index.md`
   - 补充首页壳对签到浮层与消息系统的承载关系。
   - 同步更新 `menu/messages` / `.messages` route、先关菜单再导航时序、首页 overlay 宿主与消息页登录回流。

5. `wiki/features/homepage-feed/index.md`
   - 补充首页首屏加载完成后才评估签到浮层的时序。
   - 明确签到浮层与首页评论 / 登录模态互斥，以及签到失败不影响首页主内容。

6. `wiki/architecture/overview.md`
   - 把系统总览从“发现 + 登录 + 评论”扩展到“发现 + 登录 + 评论 + 签到 + 消息”。
   - 新增 check-ins/messages 接口、首页签到流程、菜单消息流程、installationId 主体策略与 mixed repository 运行现状。

## 关键收录结论

### 签到能力

- `GET /api/check-ins/status` 与 `POST /api/check-ins` 都是可选登录接口，登录态按 `userId` 记账，匿名态要求 `X-Installation-Id`（`backend/src/app/api/check-ins/status/route.ts:8-15`、`backend/src/services/check-in/check-in.service.ts:70-86`）。
- `server_date` 是客户端本地关闭态唯一权威日期，不能使用设备本地日期替代（`backend/src/services/check-in/check-in.service.ts:88-90,140-149`）。
- 首页签到浮层只在首页首屏内容成功后才评估，且会被评论 / 登录模态抑制（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:177-232`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:197-249`）。
- 同一业务日签到提交幂等；第 7 天后下一业务日自动重开新一轮（`backend/src/services/check-in/check-in.service.ts:92-109`）。

### 消息系统

- 菜单消息预览只展示最新 1 条系统消息摘要；空态是 `204 No Content`，不是 `200 + null`（`backend/src/app/api/messages/preview/route.ts:14-18`）。
- 系统消息匿名可读，互动消息必须登录（`backend/src/app/api/messages/system/route.ts:10-23`、`backend/src/app/api/messages/interactions/route.ts:11-30`）。
- Android 与 iOS 都是“先关菜单，再进入消息中心”，而不是在菜单仍打开时直接 push 新页面（`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt:70-102`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:71-113`）。
- 登录后消息页要保留当前上下文，不回首页占位页（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:471-479`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:183-203`）。
- interaction messages 首版仍固定为 mock 数据，没有真实持久化表；system messages 已有 Supabase 表结构（`backend/src/repositories/repository-registry.ts:67-69,136-142`、`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:24-40`）。

## 未覆盖 / 保留缺口

以下内容已在 wiki 中标注为限制或范围外，未被误收录为“已完成能力”：

1. Web 用户端不实现签到浮层或消息中心。
2. interaction messages 仍为 mock / fixture 数据，尚未接通真实评论 / 点赞事件流。
3. 签到奖励只做视觉表达，不落真实金币或收益系统。
4. 设备级黑盒验证仍未执行，当前证据以代码、自动化测试与 QA 文档为主。
5. 消息系统没有详情页，只到摘要列表层。

## 收录结果

本次已完成：

- PRD-10 的 wiki 基础功能文档新增
- PRD-10 的 API 文档新增
- 认证、应用壳、首页信息流、系统总览与索引的增量同步
- PRD-10 revision 记录补齐

所有新增 / 更新文档均按要求使用 `path:line` 形式标注来源。