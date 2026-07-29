# 需求 Review：PRD-10 签到与消息系统

> Review 日期：2026-07-29
> Review 循环：第 1 轮
> 审查者：AI Agent

## 审查结果总览

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 完整性 | 14 | 14 | 0 | 4 |
| 边界与错误处理 | 12 | 12 | 0 | 1 |
| 一致性（与 wiki） | 4 | 4 | 0 | 1 |
| 可行性 | 3 | 3 | 0 | 1 |
| 平台覆盖 | 2 | 2 | 0 | 0 |
| 术语与范围 | 4 | 4 | 0 | 1 |

## 发现的问题

### 问题 1：匿名签到的 installationId contract 与信任边界仍未定稿，当前不足以支撑跨端实现对齐

- **严重程度**：🔴 高
- **维度**：完整性
- **描述**：
  spec 已把匿名签到建立在 installationId 之上，但关键 contract 仍留在 Q-01 开放项里，没有定稿到可直接实现的程度。当前缺口包括：
  1. installationId 的透传位置仍在“header / body / query”三选一之间摇摆；
  2. 没有约定 installationId 的格式、长度、校验规则，以及客户端首次生成/持久化的统一方式；
  3. 没有把“登录态 + installationId 同时存在时，服务端始终以账号态为准”的优先级落到接口语义；
  4. 没有明确 installationId 是客户端可伪造标识，只能用于低风险签到展示与次数收口，不能被解释为可信用户身份。

  这会直接影响 Backend schema、Android/iOS 存储与请求封装，也会让安全边界变得模糊。现有代码里已经有匿名标识的真实先例：播放器链路统一使用 `X-Playback-Session-Id` header，并要求 UUID（`backend/src/lib/schemas.ts`、`backend/src/app/api/player/parse-playback-session-id.ts`、`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt`、`ios/ShortDrama/Sources/Data/DataSources/PlayerRemoteDataSource.swift`）。PRD-10 如果不跟这一模式对齐，后续 design 很容易分叉。
- **修复状态**：✅ 已修复
- **修复说明**：
  `spec.md` 已在术语表、US-01、数据概览与 API 约束中统一定稿：匿名签到统一使用 `X-Installation-Id` header，格式为安装级 UUID；客户端负责安装级持久化；登录态与 header 同时存在时服务端始终以账号态为主；并补充说明该标识仅用于低风险签到激励，不视为强身份或资产归属凭据。

### 问题 2：签到“同一自然日”与本地关闭态的判定口径仍不清，客户端本地时间可能与服务端幂等语义冲突

- **严重程度**：🟡 中
- **维度**：边界与错误处理
- **描述**：
  spec 多处要求“同一自然日最多签到一次”“当日关闭后不再弹出”，但没有定义这个“自然日”究竟由谁判定。当前文档一方面写了“设备日期进入下一自然日时允许重新触发资格检查”，另一方面又要求服务端对签到做幂等收口；如果不补齐判定基准，就会出现以下冲突：
  1. 用户改动本地时钟后，客户端可能重新触发弹窗资格检查，但服务端仍认定未跨天；
  2. 客户端本地记录的“今日关闭态”可能与服务端“今日已签到/未签到”不在同一个日界线上；
  3. 匿名安装态与登录账号态切换时，关闭态/已签到态的同步基准会不一致。

  这类问题在签到场景里属于高频边界，若不先写清，端上状态机会很容易和后端幂等逻辑出现错位。
- **修复状态**：✅ 已修复
- **修复说明**：
  `spec.md` 已统一收敛为“签到是否跨天、是否可再次弹出、是否允许再次提交”的权威判定全部以服务端业务日为准；客户端本地日期变化只用于触发重新查询，不直接决定成功与否；本地关闭态也改为按服务端返回的业务日键控。

### 问题 3：消息中心返回语义仍保留开放项，其中“返回时重新打开菜单”与现有菜单关闭后导航机制不一致

- **严重程度**：🔴 高
- **维度**：一致性（与 wiki）
- **描述**：
  Q-02 仍在讨论“消息中心返回时是否必须重新打开菜单抽屉”，但现有 Android/iOS App Shell 的真实机制都不是这样工作的：
  - Android 由 `MainNavigationViewModel.closeMenuThenNavigate(...)` 先关闭菜单，再把目标 route 放入 `pendingRoute`，返回时只会回到首页根上下文，不会恢复抽屉打开态（`android/app/src/main/java/com/djs66256/short_drama/navigation/MainNavigationViewModel.kt`）；
  - iOS 由 `NavigationRouter.closeMenuPanelThenNavigate(to:)` 和 `markMenuPanelDidClose()` 先关抽屉再 push route，回退同样是回到 home tab 的根上下文，而不是重开 overlay（`ios/ShortDrama/Sources/App/NavigationRouter.swift`）。

  因此，spec 里把“严格回到抽屉打开态”保留为可选答案，会误导后续 design 以为需要新增一套菜单状态恢复能力；这与现有 wiki 中明确记录的“先关菜单再导航”机制不一致（`wiki/features/app-shell/index.md`、`wiki/architecture/overview.md`）。
- **修复状态**：✅ 已修复
- **修复说明**：
  `spec.md` 已删除开放项，并在 US-04 / US-05、现有功能影响与待澄清问题章节中定稿：消息页进入与返回都沿用现有 close-then-navigate 机制；返回后回到首页根上下文，菜单保持关闭，不自动重开。

### 问题 4：互动消息首版仍存在与“真实代码现状”不完全一致的承诺，需进一步降级表述

- **严重程度**：🟡 中
- **维度**：可行性
- **描述**：
  spec 已经说明“首版不要求评论/点赞实时事件回灌”，这是正确方向；但文档里仍有几处表述会让读者自然理解成“当前要交付真实的、与用户评论/点赞行为关联的通知流”，例如：
  - 术语表把互动消息定义为“与当前登录用户相关的评论、点赞、互动提醒等消息分区”；
  - US-06 流程写成“Backend 基于当前 `AuthContext.userId` 返回当前用户的互动消息列表”；
  - 菜单与消息中心中多次出现 `has_unread`、互动提醒等词，但没有定义其真实来源和已读/未读边界。

  与此同时，当前真实代码里还没有任何 notification/message-center 基础设施，也没有评论事件到消息流的生成链路；现状只有评论 API、评论登录恢复和菜单占位入口（`wiki/features/auth/index.md`、`wiki/features/homepage-feed/index.md`、`wiki/features/app-shell/index.md`、`wiki/architecture/overview.md`）。如果 spec 不继续降级，很容易让后续 design/coding 默认需要补一条“评论/点赞事件 -> 互动消息”的新链路，超出首版范围。
- **修复状态**：✅ 已修复
- **修复说明**：
  `spec.md` 已将互动消息首版重新定义为“登录后可见的受保护消息分区”；正文明确允许 Backend 以独立 repository / seeded fixture / mock 数据源按登录态返回列表，不承诺评论/点赞事件已真实驱动消息生成，也不承诺完整未读体系。

### 问题 5：消息接口的资源边界与分页 contract 仍不统一，和现有 backend API 基线存在冲突

- **严重程度**：🔴 高
- **维度**：完整性
- **描述**：
  当前 spec 已明确要做“消息预览 + 系统消息列表 + 互动消息列表”，但接口 contract 还没有收口到可实现层面，主要问题有：
  1. 文档没有明确到底是“单独 preview 接口 + 单独 system list 接口 + 单独 interaction list 接口”，还是“消息页组合接口一次返回全部数据”；
  2. 数据概览里写了 `MessageCenterResponse` 含 `pagination / next_cursor`，而功能详述又写“使用分页或增量加载”，这等于把页码分页和游标分页同时保留为开放项；
  3. 现有 backend 列表接口和 wiki 文档已经统一到了 `page/pageSize` query + `pagination.page/page_size/total/total_pages` 响应结构（见 `wiki/api/dramas.md`、`backend/src/lib/schemas.ts`），PRD-10 如果继续保留 `next_cursor`，后续 DTO、Zod schema、移动端分页状态都会出现两套口径；
  4. 菜单预览和消息列表里的 `has_unread` 也还没有定义作用域：是全局消息属性、账号/安装维度状态，还是单纯 UI 标记。

  这会直接阻塞 route 设计、schema 命名、测试样例和客户端列表状态机。
- **修复状态**：✅ 已修复
- **修复说明**：
  `spec.md` 已在“首版 API 约束”中显式定稿为 5 个资源：`GET /api/check-ins/status`、`POST /api/check-ins`、`GET /api/messages/preview`、`GET /api/messages/system`、`GET /api/messages/interactions`；所有列表接口统一使用 `page/pageSize` 请求参数与 `pagination.page/page_size/total/total_pages` 响应结构，并移除 `next_cursor` 与未定稿的 `has_unread` 承诺。

## 上一轮问题修复验证

> 首轮 review，问题已在同一轮修订中完成修复与复核。

| 问题编号 | 原问题摘要 | 原修复状态 | 验证结果 | 说明 |
|---------|-----------|-----------|---------|------|
| 1 | installationId contract 未定稿 | ❌ 未修复 | ✅ 通过 | header / UUID / 优先级 / 信任边界均已写入 spec |
| 2 | 服务端业务日判定未定稿 | ❌ 未修复 | ✅ 通过 | 已统一为服务端业务日权威判定 |
| 3 | 消息返回语义与菜单机制不一致 | ❌ 未修复 | ✅ 通过 | 已删除自动重开菜单备选方案 |
| 4 | 互动消息首版承诺过重 | ❌ 未修复 | ✅ 通过 | 已降级为受保护分区 + fixture / mock 可行方案 |
| 5 | 消息资源与分页 contract 未统一 | ❌ 未修复 | ✅ 通过 | 已定稿 RESTful 资源拆分与 page/pageSize 分页 |

## 遗留问题（需人工决策）

> 本轮无遗留人工决策问题。原 H-01 已在 spec 中定稿。

| 编号 | 问题 | 建议 | 状态 |
|------|------|------|------|
| H-01 | 7 日签到完成后的下一周期规则 | 已定稿为“第 8 天重新从第 1 天开始新一轮 7 日签到” | ✅ 已解决 |

## 修改记录

| 轮次 | 修改项 | 修改内容 |
|------|--------|---------|
| 1 | 首轮审查 | 输出 spec-review 报告，记录 5 个主 agent 可修复问题和 1 个需人工决策问题 |
| 1 | 首轮修复复核 | 主 agent 修订 `spec.md`，完成问题闭环，并在本报告中更新修复状态与验证结果 |

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（spec-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进

补充结论：
- 首轮 review 发现的 5 个问题均已在 `spec.md` 中收敛并通过复核；
- 原人工决策项 H-01 已在 spec 中定稿，不再阻塞后续阶段；
- 当前 `spec.md` 已可进入 `spec-human-review`，并继续推进到 design 阶段。
