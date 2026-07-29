# 签到能力 (Check-In)

> 最后更新：2026-07-29
> 覆盖端：Android / iOS / Backend（Web 本期不实现）

## 功能概述

PRD-10 在现有首页 Feed、菜单抽屉与登录体系之上，新增了首页冷启动签到浮层与 7 日签到板。当前实现以 Backend 返回的 `server_date`、`today_signed`、`current_streak` 和 `should_show_popup` 作为权威签到状态，Android 与 iOS 再叠加首页冷启动、页面上下文、本地关闭态与模态冲突判断，决定是否真正展示浮层（`backend/src/services/check-in/check-in.service.ts:43-68`、`backend/src/services/check-in/check-in.service.ts:111-155`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:195-232`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:230-268`）。

- 核心价值：为首页冷启动补齐轻量回访激励，并复用现有首页容器与登录状态
- 覆盖范围：签到状态查询、当日签到提交、7 日签到板展示、本地当日关闭态、安装级匿名身份
- 当前状态：Android / iOS / Backend 已落地；Web 端明确不在 PRD-10 范围内（`docs/specs/2026-07-29-prd-10-signin-messages/spec.md:36-45`）

## 入口与路由

| 端 | 入口 | 路由 / 触发方式 | 源文件 |
|----|------|----------------|--------|
| Android | 首页冷启动后 Feed 加载完成 | `HomeScreen` 首次组合触发 `viewModel.loadIfNeeded()`，成功加载后内部继续查询签到状态 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:67-69`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:56-61,177-205` |
| iOS | 首页冷启动后 Feed 加载完成 | `HomeView.task` 调用 `loadIfNeeded()`，ViewModel 在首屏加载后评估签到弹层 | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:85-91`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:86-97,197-249` |
| Android | 首页弹层交互 | `CheckInPopup` 通过 `onClose/onSubmit` 驱动关闭与签到提交 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/CheckInPopup.kt:37-149` |
| iOS | 首页弹层交互 | `CheckInPopupView` 由 `HomeView` 叠加在首页之上，关闭与提交都留在当前页面上下文 | `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:50-63` |
| Backend | 状态查询接口 | `GET /api/check-ins/status` | `backend/src/app/api/check-ins/status/route.ts:8-18` |
| Backend | 签到提交接口 | `POST /api/check-ins` | `backend/src/app/api/check-ins/route.ts:8-18` |
| Web | N/A | 不实现签到浮层与签到页 | `docs/specs/2026-07-29-prd-10-signin-messages/spec.md:71-74` |

## 核心逻辑

### 流程：首页冷启动评估是否展示签到浮层

1. 客户端先完成首页首屏 Feed 加载，再评估签到弹层，避免签到请求阻塞首页内容。
   - Android 在 `loadDramas()` 结束后调用 `loadCheckInStatusIfNeeded()`（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:138-205`）。
   - iOS 在 `performLoad()` 成功后调用 `evaluateCheckInPopupIfNeeded()`（`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:197-249`）。
2. Backend route 使用 `resolveOptionalAuthContext()` 解析登录态；匿名态通过 `X-Installation-Id` 兜底，header 非法时直接抛 `VALIDATION_ERROR`（`backend/src/app/api/check-ins/status/route.ts:8-15`、`backend/src/app/api/check-ins/parse-installation-id.ts:5-17`、`backend/src/middleware/auth.ts:65-76`）。
3. `CheckInService.resolveSubject()` 统一按“账号优先、安装标识兜底”的顺序确定签到主体；两者都缺失时返回 `Missing X-Installation-Id`（`backend/src/services/check-in/check-in.service.ts:70-86`）。
4. Service 基于服务端业务日与最近 30 条签到记录计算当前周期：
   - 连续签到则推进 `streak_day`
   - 中断或第 8 天则重置为第 1 天
   - `should_show_popup` 当前等于 `!todaySigned`（`backend/src/services/check-in/check-in.service.ts:92-109,111-155`）。
5. Android / iOS 再叠加客户端最终展示条件：
   - 同一 `server_date` 已本地关闭则不再弹出
   - 评论抽屉 / 登录模态等阻塞 UI 在前时不展示
   - 查询失败时直接降级为“不弹窗但首页继续可用”（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:110-123`、`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:195-232`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:50-62`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:230-247`）。

### 流程：提交当日签到

1. 用户点击“立即签到”后，客户端把当前弹层切为 submitting/loading 状态（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:71-84`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:144-156`）。
2. Backend `POST /api/check-ins` 与状态查询接口复用同一主体解析逻辑；若当日已签到，则直接按幂等成功返回最新状态（`backend/src/app/api/check-ins/route.ts:8-18`、`backend/src/services/check-in/check-in.service.ts:50-68`）。
3. 服务端仅在当天不存在签到记录时写入 `check_in_records`，并使用 `(subject_type, subject_id, business_date)` 唯一索引确保同日幂等（`backend/src/services/check-in/check-in.service.ts:53-64`、`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:1-14`）。
4. 客户端提交成功后：
   - 记录当前 `server_date` 为本地已关闭 / 已处理日期
   - 将按钮更新为“今日已签到”
   - 当日不再重复弹出（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:87-91,207-232`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:157-183`）。
5. 提交失败时仅在浮层内展示轻量失败提示，不影响首页主内容（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:92-111`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:163-182`）。

### 规则：安装标识与服务端业务日

- `X-Installation-Id` 使用 UUID，并只在签到接口中作为匿名主体 header 透传，不扩散到消息接口（`backend/src/app/api/check-ins/parse-installation-id.ts:5-17`、`backend/src/lib/schemas.ts:103-123`、`docs/specs/2026-07-29-prd-10-signin-messages/design.md:32-36`）。
- Android 使用 DataStore 持久化安装标识与最近一次 dismissed `server_date`（`android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt:13-55`）。
- iOS 使用 Keychain 持久化安装标识、用 `UserDefaults` 记录按 `serverDate` 维度的关闭态（`ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift:3-69`）。
- 登录态下两端都允许同时发送 bearer token 与 installationId，但服务端始终优先以账号记账（`ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift:42-50`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:251-256`、`backend/src/services/check-in/check-in.service.ts:70-86`）。

### 边界与异常处理

| 场景 | 处理方式 | 源文件 |
|------|---------|--------|
| 匿名请求缺少安装标识 | Backend 返回 `VALIDATION_ERROR`；客户端需重新生成 installationId 或降级不展示 | `backend/src/services/check-in/check-in.service.ts:84-86`、`backend/src/app/api/check-ins/parse-installation-id.ts:11-14` |
| 当日重复签到 | Service 不重复写库，直接返回当前最新状态 | `backend/src/services/check-in/check-in.service.ts:54-68` |
| 第 7 天后进入下一业务日 | `computeNextStreakDay()` 把第 8 天重置为 1 | `backend/src/services/check-in/check-in.service.ts:100-109` |
| 评论 / 登录模态阻塞首页 | Android 放弃本次会话弹层；iOS 在阻塞 overlay 存在时直接不评估或清空弹层 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:110-123`、`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:50-53`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:127-133,233-245` |
| 状态查询失败 | 首页继续展示主内容，不白屏、不弹全局错误 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:200-204`、`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:246-247` |

## 多端实现

### Android

- 状态源：`HomeUiState.checkInPopup` 与 `CheckInPopupUiState` 聚合可见性、提交中、`serverDate`、`todaySigned`、`currentStreak`、`rewardCopy`、`days` 与错误信息（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:22-40`）
- 浮层宿主：`HomeScreen` 在首页根容器上方叠加 `CheckInPopup`，并和评论 bottom sheet 做互斥（`android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:73-123`）
- 本地持久化：`DataStoreCheckInLocalStore` 负责 installationId 与 dismissed `server_date`（`android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt:20-55`）
- 请求认证：`AuthInterceptor` 仅为 `check-ins/status` 与 `check-ins` 在存在 access token 时自动补 `Authorization`（`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt:38-50`）
- 自动化证据：QA 文档确认 contract 一致性已通过自动化验证，但设备黑盒仍待补测（`docs/specs/2026-07-29-prd-10-signin-messages/qa-test.md:266-283,303-312`）

### iOS

- 状态源：`HomeViewModel.CheckInPopupState` 使用单一可推导状态表达弹层展示与提交反馈（`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:16-35`）
- 浮层宿主：`HomeView` 只在没有评论 sheet 与登录 alert 时渲染 `CheckInPopupView`（`ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:50-63,98-120`）
- 本地持久化：Keychain installationId + UserDefaults dismiss store（`ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift:3-69`）
- 请求头策略：签到 remote data source 同时支持 `X-Installation-Id` 与 `Authorization`（`ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift:20-52`）
- 自动化证据：QA 文档说明 iOS 数据层与状态机测试已覆盖 check-ins contract，但未执行设备黑盒（`docs/specs/2026-07-29-prd-10-signin-messages/qa-test.md:281-283,303-312`）

### Backend

- Route：`/api/check-ins/status` 与 `/api/check-ins` 都使用 `withErrorHandler` + `resolveOptionalAuthContext()`（`backend/src/app/api/check-ins/status/route.ts:8-18`、`backend/src/app/api/check-ins/route.ts:8-18`）
- Schema：`InstallationIdHeaderSchema`、`SignInDaySchema`、`SignInStatusSchema` 定义了安装标识和签到板 contract（`backend/src/lib/schemas.ts:103-123`）
- Service：`CheckInService` 负责主体解析、业务日计算、7 日轮次与幂等提交（`backend/src/services/check-in/check-in.service.ts:37-205`）
- 持久化：`check_in_records` 表和唯一索引是当前幂等约束来源（`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:1-23`）
- Repository 运行时：签到仓储支持 mock / supabase 切换（`backend/src/repositories/repository-registry.ts:51-57,120-126`）

### Web

- Web 用户端不实现签到浮层、签到页或签到 API 调用（`docs/specs/2026-07-29-prd-10-signin-messages/spec.md:36-45,71-74`）

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/check-ins/status` | [../../api/check-ins.md](../../api/check-ins.md) | 查询当前账号或匿名安装在当前业务日的签到状态 |
| `POST /api/check-ins` | [../../api/check-ins.md](../../api/check-ins.md) | 提交当日签到并返回最新 7 日板状态 |

## 状态管理

| 状态 | 存储方式 | 作用域 | 说明 | 源文件 |
|------|---------|--------|------|--------|
| Android `HomeUiState.checkInPopup` | `MutableStateFlow` | 页面级 | 首页签到弹层唯一状态源 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:33-40,50-52` |
| Android `checkInPopupAbandoned` | ViewModel 私有字段 | 页面级 | 当前会话是否放弃再弹签到浮层 | `android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:53-55,129-136,195-197` |
| Android dismissed `server_date` | DataStore Preferences | 设备级 | 按服务端业务日记录当日关闭 / 已处理状态 | `android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt:40-54` |
| iOS `checkInPopupState` | `@Published` | 页面级 | 首页签到弹层是否展示及提交状态 | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:42-47` |
| iOS `hasEvaluatedCheckInPopup` | ViewModel 私有字段 | 页面级 | 保证首页首屏后只评估一次签到弹层 | `ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:60-63,230-233` |
| iOS dismissed `serverDate` | `UserDefaults` | 设备级 | 按 `serverDate` 记录当日关闭态 | `ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift:44-69` |
| Backend `server_date` | JSON 响应字段 | 请求级 | 当前服务端业务日，是客户端本地 dismiss 判断的唯一权威日期 | `backend/src/services/check-in/check-in.service.ts:88-90,140-149` |

## 依赖关系

### 内部依赖

| 功能 | 依赖方式 | 说明 |
|------|---------|------|
| 首页信息流 | 首页首屏后的附加状态查询 | 签到弹层必须挂在首页上下文之上，不新增独立 route |
| 认证体系 | 可选登录的签到主体解析 | 登录态时账号优先，匿名态时 installationId 兜底 |
| 评论能力 | 首页模态互斥 | 评论抽屉 / 登录拦截存在时，本次会话不强插签到浮层 |
| 应用壳 | 首页容器承载 | 签到浮层只在首页冷启动场景触发 |

### 外部依赖

| 服务 / 框架 | 用途 | 接入方式 |
|-------------|------|---------|
| Backend Check-In API | 状态查询与签到提交 | Android Retrofit / iOS URLSession 调用 `/api/check-ins/*` |
| Supabase / Mock Check-In Repository | 持久化签到记录 | Backend `CheckInService` 通过 repository registry 获取 |
| DataStore / Keychain / UserDefaults | 设备级安装标识与关闭态存储 | Android 与 iOS 各自实现 |

## 已知限制

| 问题 | 影响 | 记录时间 | 备注 |
|------|------|---------|------|
| 浮层最终展示仍依赖客户端上下文 | Backend 只能返回服务端资格，无法知道评论 / 登录模态冲突 | 2026-07-29 | 这是有意设计的前后端分层边界 |
| 奖励只做视觉表达 | 不会产生真实金币余额或收益写入 | 2026-07-29 | 明确不依赖赚钱中心 |
| 匿名签到历史不自动迁移到账号 | 匿名后再登录时不做历史合并 | 2026-07-29 | PRD-10 首版范围外 |
| Web 端不实现签到体验 | 只有 Native 端有签到浮层 | 2026-07-29 | `spec.md` 已明确排除 Web |
| 设备级黑盒验证仍待补测 | 当前证据主要来自代码、自动化测试和 QA 文档 | 2026-07-29 | `qa-test.md` 中 10 条设备黑盒用例均为跳过 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录 PRD-10 首页签到浮层、服务端业务日、安装标识、当日幂等签到与 7 日轮次规则 |

---
*本文档由 llm-wiki skill 自动维护。*