# 技术方案 Review：PRD-10 签到与消息系统

> Review 日期：2026-07-29
> Review 循环：第 1 轮
> 审查者：AI Agent

## 审查结果总览

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 设计完整性 | 12 | 8 | 4 | 0 |
| 与 spec 一致性 | 8 | 6 | 2 | 0 |
| 与真实代码一致性 | 10 | 5 | 5 | 0 |
| 架构可行性 | 6 | 4 | 2 | 0 |
| 导航与状态机边界 | 6 | 4 | 2 | 0 |
| 错误处理与 contract | 6 | 3 | 3 | 0 |

## 发现的问题

### 问题 1：消息预览空态 contract 仍未定稿，shared/backend/mobile 设计口径不一致

- **严重程度**：🔴 高
- **维度**：设计完整性 / API contract
- **描述**：
  当前 `design.md` 与 `design-backend.md` 对 `GET /api/messages/preview` 的空态仍保留两种实现：`204 No Content` 或 `200 + null`。这会直接影响 Backend route handler、iOS/Android DTO、空态分支和自动化测试断言，不符合 design 阶段应有的单一 contract。`spec.md` 已要求 preview 返回单个 `MessagePreview` 或空态，设计阶段不应继续保留双解。
- **修复状态**：❌ 未修复
- **修复说明**：
  需要在 shared/backend/iOS/Android 设计中统一定稿唯一返回方式，并同步更新测试策略与客户端空态处理说明。

### 问题 2：分页参数与 installationId 非法场景的错误码设计，与当前 Backend 默认行为不一致

- **严重程度**：🔴 高
- **维度**：错误处理 / Backend 一致性
- **描述**：
  当前设计文档多处把分页参数非法、匿名签到缺少/非法 `X-Installation-Id` 统一定义为 `400 INVALID_PARAMS`。但现有 Backend Route Handler 基线在 Zod 校验失败时默认返回 `VALIDATION_ERROR`，不是 `INVALID_PARAMS`。如果不补充 route helper 或手动映射逻辑，design 文档与真实实现将出现偏差。
- **修复状态**：❌ 未修复
- **修复说明**：
  需要二选一收敛：
  1. 明确新增 route helper / 手动 parse，将这类场景统一映射为 `INVALID_PARAMS`；
  2. 或设计文档改为对齐当前默认 `VALIDATION_ERROR`。

### 问题 3：Backend 数据模型与配置方案仍有多个开放决策，尚未达到 design 阶段收口要求

- **严重程度**：🔴 高
- **维度**：设计完整性 / Backend 架构
- **描述**：
  当前 shared/backend 设计仍保留多个关键开放项：
  1. `check_in_subjects` 是否单独建表；
  2. `interaction_messages` 首版是否真实落 Supabase，还是 mock-only；
  3. `BUSINESS_TIMEZONE` 是否新增独立配置；
  4. preview 空态 contract 的最终取值。
  这些都直接影响 migration、repository、config、测试和跨端 DTO，不应继续停留在“评估后收敛”的状态。
- **修复状态**：❌ 未修复
- **修复说明**：
  需要在 `design.md` 与 `design-backend.md` 中把所有开放分支压缩成唯一实现口径，再进入下一阶段。

### 问题 4：iOS 消息页真实路由与登录回流设计未完全收敛，和当前代码基线存在缺口

- **严重程度**：🔴 高
- **维度**：iOS 架构一致性 / 导航
- **描述**：
  当前 iOS 设计方向正确，但关键实现前提尚未完全定稿：
  - 当前真实代码里 `AppRoute.swift` 还没有真实 `messages` route；
  - 菜单入口仍跳转 `.menuPlaceholder(kind: .messages)`；
  - `LoginInterceptionContext.Source` 目前也没有 messages 场景；
  - `design-ios.md` 里还保留 `"menu/messages" 或 "messages"` 这类开放表述。
  这会导致路由标识、登录成功回流语义和测试断言都出现分叉。
- **修复状态**：❌ 未修复
- **修复说明**：
  需要在 iOS 设计中明确：
  1. 消息页真实 route 的唯一命名；
  2. 登录回流是否新增 `.messages` source；
  3. 菜单入口替换 placeholder 的具体落点。

### 问题 5：Android 互动消息请求依赖 bearer token 注入，但设计文档未写清需要扩展 AuthInterceptor 路径判定

- **严重程度**：🔴 高
- **维度**：Android 架构一致性 / 网络层
- **描述**：
  `design-android.md` 中写“互动消息请求复用既有 bearer token 注入”，方向正确，但当前 Android 真正自动注入 bearer 的前提，是 `AuthInterceptor.requiresAuth()` 已将该路径视为受保护接口。如果不在设计里明确把 `/messages/interactions` 纳入受保护路径白名单，后续实现容易漏掉 token 注入。
- **修复状态**：❌ 未修复
- **修复说明**：
  需要在 Android 设计中显式补充：新增互动消息接口时，必须同步更新 `AuthInterceptor` 的鉴权路径判定逻辑。

### 问题 6：首页签到浮层与评论容器的冲突策略仍保留开放表述，跨端状态机没有定稿

- **严重程度**：🟡 中
- **维度**：导航与状态机边界
- **描述**：
  Android 与 iOS 设计都识别到了首页已有评论容器（Android `CommentBottomSheet` / iOS `CommentSheetView`），但当前文档仍写成“可等待评论容器关闭后再评估是否显示，或首版只在首页空闲时显示一次”等开放选项，没有明确统一策略。这会导致双端对以下行为理解不一致：
  - 评论容器展示中是否放弃本次签到弹出；
  - 评论关闭后是否补弹；
  - 冷启动资格检查的生命周期边界；
  - tab restore 或 feed reload 是否重复评估。
- **修复状态**：❌ 未修复
- **修复说明**：
  需要在 shared design 中统一定稿一个跨端一致的模态冲突策略，并要求 iOS/Android 方案跟随该口径。

### 问题 7：iOS 网络层关于 Authorization 注入的表述过于绝对，与当前真实接线方式不完全一致

- **严重程度**：🟡 中
- **维度**：iOS 网络层一致性
- **描述**：
  `design-ios.md` 中写“`Authorization` 由 `APIClient` 注入；`X-Installation-Id` 由 endpoint 注入”，但当前 iOS 的真实网络层并不是一个天然全局 bearer 自动注入 client，认证 header 仍依赖现有 auth/network 接线方式。该表述会让设计看起来像已有统一自动注入机制，和实际情况不完全一致。
- **修复状态**：❌ 未修复
- **修复说明**：
  建议把设计文档改为更贴近当前代码的说法：`Authorization` 继续复用现有 auth/network 接线机制，`X-Installation-Id` 由签到 endpoint 定向注入。

### 问题 8：多份设计文档仍保留探索性措辞，不符合进入 design-human-review 前的成熟度要求

- **严重程度**：🟡 中
- **维度**：设计成熟度
- **描述**：
  当前多份文档仍存在“如适用”“可拆分或复用”“新增/修改视复杂度决定”“首版可先不开放”“新建/可合并”等开放措辞。这说明设计还停留在方案探索态，而不是实现收敛态。以当前阶段目标看，这会把 human review 变成替设计补决策，而不是审阅已收口的技术方案。
- **修复状态**：❌ 未修复
- **修复说明**：
  进入 `design-human-review` 前，需要先做一轮 design cleanup，把所有开放分支压缩为唯一实现口径。

## 上一轮问题修复验证

> 本轮为首次 design 审查，无上一轮 design-review 修复项。

补充说明：
- `spec-review.md` 中此前发现的需求层问题已基本完成收敛；
- 本轮新发现的问题，主要集中在 design 与真实工程基线之间的落地细节、开放决策未收口、以及错误处理 / 导航 / 状态机层面的实现前提未定稿。

## 遗留问题（需人工决策）

> 当前仍有若干关键决策未在设计中完全定稿，但这些问题大多可以由主 agent 结合现有代码与合理默认值继续收口；本轮暂不要求用户介入。

| 编号 | 问题 | 建议 | 状态 |
|------|------|------|------|
| H-01 | `GET /api/messages/preview` 空态返回 `204` 还是 `200 + null` | 在 design 中定稿唯一 contract | ⏳ 待收敛 |
| H-02 | 分页/header 非法参数是复用 `VALIDATION_ERROR` 还是新增映射为 `INVALID_PARAMS` | 设计需对齐真实 route 处理策略 | ⏳ 待收敛 |
| H-03 | 首页签到浮层与评论容器冲突时的统一策略 | 在 shared design 中定稿跨端一致状态机 | ⏳ 待收敛 |

## 修改记录

| 轮次 | 修改项 | 修改内容 |
|------|--------|---------|
| 1 | 首轮审查 | 基于 spec、shared/backend/iOS/Android design、真实代码与 wiki 输出首轮 design review，发现 8 个待修复问题 |

## 结论

- [ ] ✅ 所有问题已修复，可进入下一阶段（design-human-review）
- [x] ⚠️ 存在遗留问题，需要先修订 design 文档再继续推进

补充结论：
- 当前设计方向与产品范围总体一致，且已基本对齐现有首页、菜单、认证与分层基线；
- 但 API contract、错误码口径、Backend 数据模型、iOS 登录回流、Android auth 注入前提与首页模态冲突策略仍未完全收口；
- 在这些问题修复前，**暂不建议直接进入 `design-human-review`**。
