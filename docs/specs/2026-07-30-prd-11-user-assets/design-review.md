# 技术方案 Review：PRD-11 个人资产管理

> Review 日期：2026-07-30
> Review 循环：第 1 轮
> 审查者：AI Agent

## 审查结果总览

### Shared 设计 (design.md)

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 与 Spec 一致性 | 4 | 4 | 0 | 0 |
| 功能完整性 | 4 | 4 | 0 | 0 |
| API 完整性 | 4 | 4 | 0 | 0 |
| 数据模型一致性 | 4 | 4 | 0 | 0 |
| 边界与错误处理 | 4 | 4 | 0 | 0 |
| 安全考虑 | 3 | 3 | 0 | 0 |
| 性能考虑 | 3 | 3 | 0 | 0 |

### 平台设计 (design-{platform}.md)

| 平台 | 与 Spec 一致性 | 功能完整性 | 架构 | 文件变更 | API 调用 | 状态管理 | 测试策略 | 总体 |
|------|--------------|----------|------|---------|---------|---------|---------|------|
| Backend | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| iOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Android | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Web | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

## 发现的问题

### Shared 设计问题

#### 问题 S-1：未知 `dramas.status` 的跨端口径未收口

- **严重程度**：🔴 阻塞
- **维度**：数据模型
- **描述**：`design.md` 一度保留“未知 `dramas.status` 由 backend 方案进一步定稿”的未定表述，会导致 shared 层对过滤策略仍不闭合。
- **修复状态**：✅ 已修复
- **修复说明**：已在 `design.md` 明确为“服务端过滤并记录 warning，不返回给客户端，也不计入 `summary`”。

### 平台设计问题

#### 问题 backend-1：429 能力边界表述过强

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：边界与错误处理
- **描述**：初版 backend 文档一边写本期不新增限流 middleware，一边把 429 表述成像是本期已主动实现的能力，容易让后续 coding 把限流误判为必做。
- **修复状态**：✅ 已修复
- **修复说明**：已统一改为“429 仅为 contract 预留”；本期不新增主动 429 生成逻辑，也不把它作为主动实现或必测路径。

#### 问题 ios-1：`BookDramaResponseDTO` 历史漂移被混入本期主链路

- **严重程度**：🟡 关注
- **平台**：ios
- **维度**：文件变更
- **描述**：初版 iOS 文档同时出现“非阻塞背景说明”和“coding 一并收口”的矛盾表述，容易把旧预约写链路 DTO 修复误纳入 PRD-11 booking assets 主路径。
- **修复状态**：✅ 已修复
- **修复说明**：已统一改为“不在本期主链路”；仅保留历史漂移背景，不作为 booking assets 主实现阻塞项。

#### 问题 ios-2：booking route / login 回流闭环需要写硬

- **严重程度**：🔴 阻塞
- **平台**：ios
- **维度**：功能完整性
- **描述**：初版 iOS 文档对 `AppRoute.bookingAssets`、`LoginInterceptionContext.Source.bookingAssets`、`LoginView` 文案、`NavigationRouter.completeLogin()`、`MenuPanelContainerView` 的闭环表述不够硬，存在被误实现为局部导航补丁的风险。
- **修复状态**：✅ 已修复
- **修复说明**：文档已明确这些点属于同一闭环必改项，并补充 `completeLogin()` 的 booking 回流与不重复 push 约束。

#### 问题 android-1：booking 接口鉴权白名单遗漏

- **严重程度**：🔴 阻塞
- **平台**：android
- **维度**：API 调用
- **描述**：现有 `AuthInterceptor.requiresAuth()` 未覆盖 `users/me/bookings`，若文档不明确修改点，按原方案实现会导致 booking 请求不带 bearer token。
- **修复状态**：✅ 已修复
- **修复说明**：已在 `design-android.md` 明确把 `AuthInterceptor.kt` 纳入必改文件，并要求将 `users/me/bookings` 纳入白名单。

#### 问题 android-2：登录成功回 booking route 的现状说明缺失

- **严重程度**：🟡 关注
- **平台**：android
- **维度**：状态管理
- **描述**：初版 Android 文档未明确 `LoginViewModel.resolveSuccessRoute()` 已允许 `menu/booking` 透传，容易让后续实现重复加分支。
- **修复状态**：✅ 已修复
- **修复说明**：已补充“无需新增特殊分支，但需加回归测试确保该约束稳定”的定稿说明。

#### 问题 web-1：skipped 边界护栏偏弱

- **严重程度**：🟡 关注
- **平台**：web
- **维度**：文件变更
- **描述**：初版 Web 文档虽已标 skipped，但对“不新增 route / api-client / schema / tests”的边界说明不够硬，后续 plan/coding 仍可能误接 Web。
- **修复状态**：✅ 已修复
- **修复说明**：已强化 Web skipped 边界，明确无任何 booking assets Web route、api-client、schema、页面交互与测试进入本期范围。

## 跨端一致性检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| API 调用与 Shared 设计一致 | ✅ | `GET /api/users/me/bookings` 的 query、响应和 401/400/503 主路径已在 shared/backend/mobile 对齐 |
| 数据模型各端一致 | ✅ | `BookingAsset` / `summary` / `pagination` 命名与口径一致；未知 `dramas.status` 统一过滤 |
| 共享逻辑覆盖 | ✅ | booking 独立 route、登录承接、summary 只读服务端、downloads 保持 placeholder 都已收口 |
| 错误处理策略一致 | ✅ | 401 回登录承接、追加失败不清空已有列表、Web skipped 不新增错误处理逻辑已对齐 |

## 遗留问题（需人工决策）

> 以下问题 agent 无法自行解决，需要人工确认。

| 编号 | 问题 | 平台 | 建议 | 状态 |
|------|------|------|------|------|
| 无 | — | — | — | — |

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（design-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进
