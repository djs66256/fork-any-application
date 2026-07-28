# 技术方案 Review：PRD-12 剧场频道

> Review 日期：2026-07-28
> Review 循环：第 1 轮
> 审查者：AI Agent

## 审查结果总览

### Shared 设计 (design.md)

| 维度 | 检查项数 | 通过 | 问题 | 已修复 |
|------|---------|------|------|--------|
| 与 Spec 一致性 | 5 | 4 | 1 | 1 |
| 功能完整性 | 5 | 5 | 0 | 0 |
| API 完整性 | 4 | 4 | 0 | 0 |
| 数据模型一致性 | 4 | 3 | 1 | 1 |
| 边界与错误处理 | 4 | 4 | 0 | 0 |
| 安全考虑 | 4 | 4 | 0 | 0 |
| 性能考虑 | 3 | 3 | 0 | 0 |

### 平台设计 (design-{platform}.md)

| 平台 | 与 Spec 一致性 | 功能完整性 | 架构 | 文件变更 | API 调用 | 状态管理 | 测试策略 | 总体 |
|------|--------------|----------|------|---------|---------|---------|---------|------|
| Backend | ✅ | ✅ | ⚠️ | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| iOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Android | ⚠️ | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ | ⚠️ |
| Web | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

## 发现的问题

### Shared 设计问题

#### 问题 S-1：剧场卡片进入播放页的承接归属未在 shared 设计中显式收口

- **严重程度**：🟡 关注
- **维度**：与 Spec 一致性 / 数据模型
- **描述**：`design.md` 已明确搜索、分类、排行、新剧复用 home-owned 路由，但对剧场卡片进入 `play` 的 tab / graph 归属没有同等级别的显式说明。现有代码里 iOS `.player(videoId:)` 归属 `.home`，`NavigationRouter.navigate(to:)` 会切 `selectedTab`（`ios/ShortDrama/Sources/App/AppRoute.swift:26`, `ios/ShortDrama/Sources/App/NavigationRouter.swift:49`）；Android 的 `PlayerScreen` 也注册在 `home_graph`（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:168`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:260`）。如果 shared 层不显式收口，平台文档可能各自解释，导致 theater → play 返回语义分叉。
- **修复状态**：✅ 已修复
- **修复说明**：已在 `design.md` 和 `spec.md` 中补充：剧场卡片继续复用既有 home-owned canonical `play` 承接语义；若现有播放器承接归属于 `home` 容器，则从剧场进入时允许切换到底部 `home` Tab / home graph，不要求在 `theater` 内维持独立播放器副本。

#### 问题 S-2：`spec.md` 示例数据中的 `category` 语义与 shared 数据模型冲突

- **严重程度**：🟡 关注
- **维度**：数据模型
- **描述**：shared 设计已明确 `category` 是题材展示文案，`channel` 才是子频道枚举（`design.md:188`）。但 `spec.md` 的接口示例仍写为 `"category": "real"`，会把题材字段和频道枚举混用，影响后续 DTO / schema 设计和测试断言。
- **修复状态**：✅ 已修复
- **修复说明**：已将 `spec.md` 示例中的 `category` 修正为题材值 `"都市"`，与 shared / backend 方案保持一致。

### 平台设计问题

#### 问题 backend-1：新增 theater route 的装配策略未与既有 repository registry 收口

- **严重程度**：🟡 关注
- **平台**：backend
- **维度**：架构
- **描述**：`design-backend.md` 先前把 `GET /api/dramas/channel` 描述为 route 中直接创建 `DramaMockRepository`。但代码库已有 `repository-registry.ts` 的 `getDramaRepository()` / `setDramaRepository()` / `resetRepositoryRegistry()` 注入点（`backend/src/repositories/repository-registry.ts:10`, `backend/src/repositories/repository-registry.ts:30`, `backend/src/repositories/repository-registry.ts:54`）。同时现有 rankings route 的测试风格也偏向 mock `DramaService` 以提升可测性（`backend/src/app/api/__tests__/dramas-rankings.test.ts:4`）。如果 theater route 继续固化 `new DramaMockRepository()`，会让设计与未来的数据源切换、route 注入测试方向不一致。
- **修复状态**：✅ 已修复
- **修复说明**：已在 `design-backend.md` 中统一改为通过既有 `getDramaRepository()` 获取仓储，并把 route tests 补充为覆盖基于 repository registry 的注入场景，避免把 `DramaMockRepository` 直接写死在新增 route 设计里。

#### 问题 android-1：home-owned 承接页与播放页的跳转语义没有像 shared / iOS 一样写清“先回 home 容器再承接”

- **严重程度**：🟡 关注
- **平台**：android
- **维度**：与 Spec 一致性 / API 调用
- **描述**：Android 文档原先写法以 `navController.navigate(...)` 直接从 `theater_graph` 跳转到搜索 / 分类 / 排行 / 新剧 / 播放页面，但没有像 shared / iOS 那样明确“这些页面属于 home-owned 承接，允许切到底部 `home` tab”。结合当前导航结构，相关页面与 `PlayerScreen` 都注册在 `home_graph`（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:168`, `android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:260`），且项目已有 `navigateToTopLevelTab(...)` 可复用（`android/app/src/main/java/com/djs66256/short_drama/navigation/NavGraph.kt:406`）。若文档不写清楚，会留下“仍停留 theater tab”与“切到 home 容器”两种实现解释空间。
- **修复状态**：✅ 已修复
- **修复说明**：已在 `design-android.md` 中把搜索 / 分类 / 排行 / 预约 / 新剧 / 播放全部统一为先 `navigateToTopLevelTab(navController, TopLevelTab.HOME)`，再执行对应 `navigate(...)`；同时在路由清单与跨端共享逻辑表中补充“复用 home-owned 承接”的明确说明。

## 跨端一致性检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| API 调用与 Shared 设计一致 | ✅ | `GET /api/dramas/channel`、ranking 初始化、classification/search/new-releases 复用路径已收口 |
| 数据模型各端一致 | ✅ | `channel` 与 `category` 语义已拆清；`heat` 继续保持服务端原始整数 |
| 共享逻辑覆盖 | ✅ | 默认频道、分页、空态、booking 直达、play 承接、home-owned 跳转均已在 shared / iOS / Android / backend 对齐 |
| 错误处理策略一致 | ✅ | 首屏错误、分页错误、validation/internal/service unavailable 等口径保持一致 |

## 上一轮问题修复验证

> 仅非首轮 review 时填写。验证上一轮 review 报告中标记为 `✅ 已修复` 的问题是否真正被修改到位。

首轮 review，无上一轮问题需要验证。

## 遗留问题（需人工决策）

> 以下问题 agent 无法自行解决，需要人工确认。

| 编号 | 问题 | 平台 | 建议 | 状态 |
|------|------|------|------|------|
| H-01 | 无 | all | 本轮问题均可由设计文档自行收口，无需额外人工决策 | 已收敛 |

## 结论

- [x] ✅ 所有问题已修复，可进入下一阶段（design-human-review）
- [ ] ⚠️ 存在遗留问题，需要人工确认后再推进
