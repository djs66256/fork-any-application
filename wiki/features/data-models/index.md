# 数据模型 (Data Models)

> 最后更新：2026-07-27

## 功能概述

项目核心数据模型定义，包含 Drama（短剧）、Episode（剧集）、UserProfile（用户）以及 PRD-05 新增的排行与预约相关模型约束。当前 Backend 仍是权威来源：`Drama` 提供首页与排行共享的基础卡片字段，`RankingDrama` 在其上扩展内容类型、热度、预约数、推荐值与用户预约态，并同步驱动 Android / iOS 排行页渲染。

- **覆盖端**：Backend（权威来源）、Android、iOS、Web（仅读取首页壳 / 排行占位页，不消费真实榜单）
- **核心价值**：统一首页卡片、排行列表、预约结果与移动端域模型，避免不同端对榜单字段和预约状态的理解分叉
- **当前状态**：Backend / Android / iOS 已围绕首页 Feed 与排行页对齐 `Drama + RankingDrama + RankingQuery + BookDramaResponse`；Web 本期不消费真实排行数据

## 核心逻辑

### Drama 数据模型

权威来源定义在 `backend/src/lib/schemas.ts:15-28`：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | string | uuid() | 短剧 UUID |
| title | string | min(1) | 短剧标题 |
| description | string | default `""` | 卡片描述 |
| cover_url | string \| null | url(), nullable, default null | 封面图 URL |
| category | string | default `""` | 分类标签 |
| episode_count | number | int, min(0) | 集数 |
| tags | string[] | default [] | 标签列表 |
| rating | number \| null | min(0), max(10), nullable, default null | 评分 |
| created_at | string | — | 创建时间（ISO 8601 字符串） |
| updated_at | string | — | 更新时间（ISO 8601 字符串） |

相关列表响应定义在 `backend/src/lib/schemas.ts:61-79`：

| 字段 | 类型 | 说明 |
|------|------|------|
| data | `Drama[]` / `RankingDrama[]` | 列表数据 |
| pagination.page | number | 当前页 |
| pagination.page_size | number | 每页条数（snake_case） |
| pagination.total | number | 总数 |
| pagination.total_pages | number | 总页数 |

### RankingDrama 数据模型

PRD-05 在 `Drama` 的基础上扩展榜单专用字段，定义于 `backend/src/lib/schemas.ts:30-44`：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| content_type | enum | `all` 之外的具体项由 `live_action` / `ai` 构成 | 榜单一级 Tab 过滤维度 |
| play_count | number | int, min(0) | 热榜排序与展示的热度代理值 |
| booking_count | number | int, min(0) | 预约榜排序与展示的预约数 |
| recommendation_score | number | min(0) | 推荐榜排序与展示的推荐值 |
| is_booked | boolean | default false | 当前用户是否已预约 |

> `RankingListResponseSchema` 复用统一分页结构，意味着首页 Feed 与排行页只是在 `data` 元素类型上不同，而非分页协议不同（`backend/src/lib/schemas.ts:75-79`）。

### RankingQuery 与榜单维度模型

排行查询模型定义在 `backend/src/lib/schemas.ts:30-34,90-97`：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | `hot \| recommend \| booking` | `hot` | 榜单类型 |
| `contentType` | `all \| live_action \| ai` | `all` | 内容类型 |
| `page` | number | 1 | 页码 |
| `pageSize` | number | 10 | 每页条数 |

这些维度同时在客户端以强类型 enum / value object 形式存在：

- Android：`RankingContentType`、`RankingType`、`RankingQuery`、`RankingPage`（`android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingQuery.kt:3-54`）
- iOS：`RankingContentType`、`RankingType`、`RankingQuery`（`ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift:3-21` 及相关 enum 定义）[待确认：iOS enum 定义文件未在本轮逐一展开，但已从 ViewModel / DataSource 使用处确认存在]

### BookDramaResponse / 预约结果模型

Backend 返回定义于 `backend/src/lib/schemas.ts:99-105`：

| 字段 | 类型 | 说明 |
|------|------|------|
| drama_id | string | 被预约的短剧 UUID |
| booked | literal `true` | 当前仅支持预约成功 / 已预约语义 |
| booking_count | number | 预约后的最新预约数 |

客户端对应模型：

- Android 通过 `BookDramaResponseDto -> Domain` 映射供 ViewModel 局部更新预约数与按钮状态（相关 API 调用入口见 `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:47-48`）[待确认：Android 预约 DTO 文件未在本轮逐行展开]
- iOS 使用 `BookDramaResult { dramaID, booked, bookingCount }`（`ios/ShortDrama/Sources/Domain/Entities/BookDramaResult.swift:3-8`）

### 数据关系

```text
Drama ──基础字段──▶ RankingDrama
Drama ──1:N──▶ Episode
User ──1:N──▶ Booking（当前仅在 mock repository Set 中体现）
```

## 多端实现

### Backend
- 源文件：`backend/src/lib/schemas.ts:1-140`
- 依赖：zod
- 角色：Schema 权威来源；`DramaService` 用 `DramaListResponseSchema` / `RankingListResponseSchema` / `BookDramaResponseSchema` 校验 repository 输出（`backend/src/services/drama/drama.service.ts:29-78`）
- 当前排行数据源：`DramaMockRepository` 预置 12 条 `RankingDrama` 种子数据，并在运行时以内存 Set 维护预约态（`backend/src/repositories/mock/drama.mock.repository.ts:22-227,316-395`）

### Android
- Domain Entity：`android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingDrama.kt:3-19`
- Query / Page / Enum：`android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingQuery.kt:3-54`
- DTO：`android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingDramaDto.kt:8-52`
- 对齐方式：DTO 解码 backend snake_case JSON，并把 `content_type` 转为 `RankingContentType`；`cover_url = null` 时降级为空字符串（`android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingDramaDto.kt:13-17,35-51`）
- UI 派生模型：`RankingDramaItemUiModel` 根据榜单维度派生“热度 / 推荐值 / 预约数”等展示字段（`android/app/src/main/java/com/djs66256/short_drama/feature/ranking/model/RankingUiModel.kt:6-71`）

### iOS
- Domain Entity：`ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift:3-82`
- Query：`ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift:3-21`
- 预约结果：`ios/ShortDrama/Sources/Domain/Entities/BookDramaResult.swift:3-8`
- 对齐方式：`RankingDrama` 额外显式建模 `isBookingSubmitting`，用于端内按钮提交态；这是客户端局部 UI 状态，不属于 Backend 返回字段（`ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift:19-20,58-81`）
- 使用场景：`RankingViewModel` 直接消费这些实体，并对目标项执行局部 booking state 更新（`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:114-157,219-223`）

### Web
- Web 首页与 `/rankings` 页面当前都不消费真实 `RankingDrama` 数据，仍展示应用壳 / 占位页（`web/src/features/home/HomeScreen.tsx:12-55`, `web/src/app/rankings/page.tsx:1-9`）
- 因此本期无需同步一份 Web 端榜单 schema

## 依赖关系

- 依赖 `zod` 做 Backend 运行时类型校验
- Backend 为 `Drama` / `RankingDrama` / `RankingQuery` / `BookDramaResponse` 的权威来源，Android / iOS 各自通过 DTO + Domain Entity 做手动对齐
- 首页 Feed、排行页、播放页入口都依赖 `Drama.id` 作为统一业务标识
- `GET /api/dramas` 与 `GET /api/dramas/rankings` 共享基础 `Drama` 字段集；`POST /api/dramas/:id/book` 则消费 `BookDramaResponse`

## 已知限制

- 各端仍为手动同步 schema / entity，缺少真正共享的数据模型包，存在演进分叉风险。
- 当前 `RankingDrama` 只覆盖排行页首版展示所需字段；如果后续需要更新时间、作者、专题标签等扩展字段，需要再次同步 Backend 与客户端模型。
- Backend `is_booked` 依赖进程内 `bookings` Set 和骨架态 userId，不具备持久化或真实身份校验能力（`backend/src/repositories/mock/drama.mock.repository.ts:316-395`, `backend/src/middleware/auth.ts:16-32`）。
- Web 本期不消费真实榜单数据，因此没有形成 Web 侧等价 `RankingDrama` schema。
- iOS 部分自动化测试样例仍使用非 UUID 字符串作为 mock `id`（如 `ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift:8-40`），这与 Backend 运行时 UUID 约束不完全一致，但不影响当前单测语义。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-27 | 更新：新增 `RankingDrama`、`RankingQuery`、`BookDramaResponse` 等排行体系相关模型，补充 Android / iOS 的 DTO、Entity 与榜单展示派生模型对齐方式 |
| 2026-07-26 | 更新：按 PRD-02 首页信息流实现，将 `Drama` 模型从旧的详情型字段集修正为首页卡片字段集，补充 Android/iOS DTO 与 Entity 的实际对齐方式，并记录列表响应与测试约束 |
| 2026-07-24 | 扩展：Backend 端新增 EpisodeSchema、UserProfileSchema、PlayerStartRequestSchema、PlayerStopRequestSchema、DramaListResponseSchema；DramaSchema 字段从 6 个扩展到 12 个（新增 status、release_year、play_count、total_episodes 等） |
| 2026-07-22 | 从代码提取，初始创建（仅 Web 端 6 字段 DramaSchema + Backend 端 HealthResponseSchema） |

---

*本文档由 llm-wiki skill 自动维护。*