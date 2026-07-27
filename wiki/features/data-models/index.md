# 数据模型 (Data Models)

> 最后更新：2026-07-27

## 功能概述

项目核心数据模型定义，包含 `Drama`（短剧）、`Episode`（剧集）、`UserProfile`（用户）以及 PRD-05 新增的排行与预约模型、PRD-06 新增的分类标签模型约束。当前 Backend 仍是权威来源：`Drama` 提供首页、搜索与排行共享的基础卡片字段；`RankingDrama` 在其上扩展内容类型、热度、预约数、推荐值与用户预约态；`ClassificationTagsResponse` 则承载分类页固定三维度标签矩阵，并同步驱动 Android / iOS 分类页渲染。

- **覆盖端**：Backend（权威来源）、Android、iOS、Web（仅消费首页壳 / 搜索占位页，不消费真实分类模型）
- **核心价值**：统一首页卡片、搜索结果、排行列表、预约结果与分类标签契约，避免不同端对发现链路字段和维度含义的理解分叉
- **当前状态**：Backend / Android / iOS 已围绕首页 Feed、搜索发现、排行页、分类页对齐 `Drama + RankingDrama + RankingQuery + BookDramaResponse + ClassificationTagsResponse`；Web 本期不消费真实分类数据

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
| tags | string[] | default [] | 标签列表；搜索与分类链路都会复用 |
| rating | number \| null | min(0), max(10), nullable, default null | 评分 |
| created_at | string | — | 创建时间（ISO 8601 字符串） |
| updated_at | string | — | 更新时间（ISO 8601 字符串） |

相关列表响应定义在 `backend/src/lib/schemas.ts:61-80`：

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
| content_type | enum | `live_action` / `ai` | 榜单一级 Tab 过滤维度 |
| play_count | number | int, min(0) | 热榜排序与展示的热度代理值 |
| booking_count | number | int, min(0) | 预约榜排序与展示的预约数 |
| recommendation_score | number | min(0) | 推荐榜排序与展示的推荐值 |
| is_booked | boolean | default false | 当前用户是否已预约 |

> `RankingListResponseSchema` 复用统一分页结构，意味着首页 Feed、搜索结果与排行页只是在 `data` 元素类型上不同，而非分页协议不同（`backend/src/lib/schemas.ts:75-80`）。

### RankingQuery 与榜单维度模型

排行查询模型定义在 `backend/src/lib/schemas.ts:90-97`：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | `hot \| recommend \| booking` | `hot` | 榜单类型 |
| `contentType` | `all \| live_action \| ai` | `all` | 内容类型 |
| `page` | number | 1 | 页码 |
| `pageSize` | number | 10 | 每页条数 |

这些维度同时在客户端以强类型 enum / value object 形式存在：

- Android：`RankingContentType`、`RankingType`、`RankingQuery`、`RankingPage`（`android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingQuery.kt:3-54`）
- iOS：`RankingContentType`、`RankingType`、`RankingQuery`（`ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift:3-21` 及相关 enum 定义）

### ClassificationTags 数据模型

PRD-06 新增分类页标签矩阵契约，定义于 `backend/src/lib/schemas.ts:99-152`：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `ClassificationGender` | enum | `all` / `male` / `female` | 分类页顶部性别 Tab |
| `CLASSIFICATION_DIMENSION_KEYS` | tuple | 固定长度 3 | 维度顺序：`era_background` → `theme_plot` → `character_setting` |
| `ClassificationDimension.key` | enum | 固定 key | 分类维度标识 |
| `ClassificationDimension.name` | string | trim, min 1 | 维度展示名 |
| `ClassificationDimension.tags` | string[] | 默认 `[]` | 当前维度下的标签列表，可为空 |
| `ClassificationTagsResult.gender` | enum | 必填 | 当前返回的性别视图 |
| `ClassificationTagsResult.dimensions` | array | 长度必须为 3 | 固定三维度标签矩阵 |
| `ClassificationTagsResponse.data` | object | 必填 | 分类页完整返回体 |

> `ClassificationDimensionsSchema` 会逐位校验 key 顺序，因此即使某维度为空，也不能省略该项（`backend/src/lib/schemas.ts:121-139`）。

### SearchDramaQuery 模型

搜索查询模型定义在 `backend/src/lib/schemas.ts:82-88`：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `q` | string | — | `trim` 后长度 1~50；分类标签点击后也复用此 query |
| `page` | number | 1 | 页码 |
| `pageSize` | number | 10 | 每页条数 |

该模型与 `Drama.tags` 共同保证“分类标签 -> 搜索结果页”这条链路无需新增独立结果页契约。

### BookDramaResponse / 预约结果模型

Backend 返回定义于 `backend/src/lib/schemas.ts:154-160`：

| 字段 | 类型 | 说明 |
|------|------|------|
| drama_id | string | 被预约的短剧 UUID |
| booked | literal `true` | 当前仅支持预约成功 / 已预约语义 |
| booking_count | number | 预约后的最新预约数 |

客户端对应模型：

- Android 通过 `BookDramaResponseDto -> Domain` 映射供 ViewModel 局部更新预约数与按钮状态（相关 API 调用入口见 `android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:47-48`）
- iOS 使用 `BookDramaResult { dramaID, booked, bookingCount }`（`ios/ShortDrama/Sources/Domain/Entities/BookDramaResult.swift:3-8`）

### 数据关系

```text
Drama ──基础字段──▶ RankingDrama
Drama.tags ──被搜索复用──▶ SearchDramaQuery(q)
ClassificationTagsResponse ──标签点击──▶ SearchDramaQuery(q)
Drama ──1:N──▶ Episode
User ──1:N──▶ Booking（当前仅在 mock repository Set 中体现）
```

## 多端实现

### Backend
- 源文件：`backend/src/lib/schemas.ts:1-202`
- 依赖：zod
- 角色：Schema 权威来源；`DramaService` 用 `DramaListResponseSchema` / `RankingListResponseSchema` / `ClassificationTagsResponseSchema` / `BookDramaResponseSchema` 校验 repository 输出（`backend/src/services/drama/drama.service.ts:32-92`）
- 当前分类与排行运行时数据源：`DramaMockRepository` 预置榜单与分类 seed 数据，并在运行时以内存 Set 维护预约态（`backend/src/repositories/mock/drama.mock.repository.ts:234-468`）

### Android
- Ranking Domain Entity：`android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingDrama.kt:3-19`
- Ranking Query / Page / Enum：`android/app/src/main/java/com/djs66256/short_drama/domain/model/RankingQuery.kt:3-54`
- Ranking DTO：`android/app/src/main/java/com/djs66256/short_drama/data/dto/RankingDramaDto.kt:8-52`
- Classification DTO：`android/app/src/main/java/com/djs66256/short_drama/data/dto/ClassificationTagsResponseDto.kt`
- Classification Query / Entity：`android/app/src/main/java/com/djs66256/short_drama/domain/model/ClassificationTagModels.kt`
- 对齐方式：DTO 解码 backend snake_case JSON，并把 `content_type` / `gender` / `dimension key` 映射为强类型模型；分类页额外通过 `ClassificationDimensionKey.entries` 保证空维度也被完整建模（`android/app/src/main/java/com/djs66256/short_drama/feature/classification/viewmodel/ClassificationViewModel.kt:237-245`）
- UI 派生模型：`RankingDramaItemUiModel` 根据榜单维度派生“热度 / 推荐值 / 预约数”等展示字段；分类页直接消费 domain 维度模型构造 rail 与 section list

### iOS
- Ranking Domain Entity：`ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift:3-82`
- Ranking Query：`ios/ShortDrama/Sources/Domain/Entities/RankingQuery.swift:3-21`
- 预约结果：`ios/ShortDrama/Sources/Domain/Entities/BookDramaResult.swift:3-8`
- Classification DTO：`ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift:4-48`
- Classification Entity：`ios/ShortDrama/Sources/Domain/Entities/ClassificationModels.swift`
- 对齐方式：`ClassificationTagsPayloadDTO.normalizedDimensions()` 会按 `ClassificationDimensionKey.allCases` 兜底补齐缺失维度；`RankingDrama` 额外显式建模 `isBookingSubmitting` 作为客户端局部 UI 状态，不属于 Backend 返回字段（`ios/ShortDrama/Sources/Data/DTOs/ClassificationTagsResponseDTO.swift:25-41`、`ios/ShortDrama/Sources/Domain/Entities/RankingDrama.swift:19-20,58-81`）
- 使用场景：`RankingViewModel` 与 `ClassificationViewModel` 直接消费这些实体，并对目标项执行局部 booking state 更新或分类维度同步（`ios/ShortDrama/Sources/Features/Ranking/ViewModels/RankingViewModel.swift:114-157,219-223`、`ios/ShortDrama/Sources/Features/Classification/ViewModels/ClassificationViewModel.swift:17-104`）

### Web
- Web 首页与 `/search` 页面当前都不消费真实 `RankingDrama` 或 `ClassificationTagsResponse` 数据，仍展示应用壳 / 占位页（`web/src/features/home/HomeScreen.tsx:12-55`, `web/src/app/search/page.tsx:1-10`）
- 因此本期无需同步一份 Web 端等价排行 / 分类 schema

## 依赖关系

- 依赖 `zod` 做 Backend 运行时类型校验
- Backend 为 `Drama` / `RankingDrama` / `SearchDramaQuery` / `ClassificationTagsResponse` / `BookDramaResponse` 的权威来源，Android / iOS 各自通过 DTO + Domain Entity 做手动对齐
- 首页 Feed、搜索结果页、排行页、分类页和播放页入口都依赖 `Drama.id` 作为统一业务标识
- `GET /api/dramas`、`GET /api/dramas/search` 与 `GET /api/dramas/rankings` 共享基础 `Drama` 字段集；`GET /api/dramas/tags` 提供分类浏览矩阵；`POST /api/dramas/:id/book` 则消费 `BookDramaResponse`

## 已知限制

- 各端仍为手动同步 schema / entity，缺少真正共享的数据模型包，存在演进分叉风险。
- 当前 `ClassificationTagsResponse` 只覆盖分类页首版固定三维度；如果后续需要运营后台动态扩充维度，需再次同步 Backend 与客户端模型。
- Backend `is_booked` 依赖进程内 `bookings` Set 和骨架态 userId，不具备持久化或真实身份校验能力。
- Web 本期不消费真实搜索发现 / 分类数据，因此没有形成 Web 侧等价 `ClassificationTagsResponse` schema。
- 分类页标签矩阵当前来自代码内 seed，不是从真实剧库 tags 自动聚合生成。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-27 | 更新：新增 `ClassificationGender`、固定维度 key、`ClassificationTagsResponse` 等分类体系相关模型，并补充 Android / iOS DTO、Entity 与空维度兜底对齐方式 |
| 2026-07-27 | 更新：新增 `RankingDrama`、`RankingQuery`、`BookDramaResponse` 等排行体系相关模型，补充 Android / iOS 的 DTO、Entity 与榜单展示派生模型对齐方式 |
| 2026-07-26 | 更新：按 PRD-02 首页信息流实现，将 `Drama` 模型修正为首页卡片字段集，补充 Android/iOS DTO 与 Entity 的实际对齐方式，并记录列表响应与测试约束 |
| 2026-07-24 | 扩展：Backend 端新增 EpisodeSchema、UserProfileSchema、PlayerStartRequestSchema、PlayerStopRequestSchema、DramaListResponseSchema |
| 2026-07-22 | 从代码提取，初始创建 |

---

*本文档由 llm-wiki skill 自动维护。*