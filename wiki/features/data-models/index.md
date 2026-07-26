# 数据模型 (Data Models)

> 最后更新：2026-07-26

## 功能概述

项目核心数据模型定义，包含 Drama（短剧）、Episode（剧集）、UserProfile（用户）的 Schema / Entity 约束。当前 Backend 仍是权威来源，但随着 PRD-02 首页信息流落地，`Drama` 已从早期内容详情型字段集收口为首页卡片所需字段集，并同步驱动 Android / iOS 首页 Feed 渲染。

- **覆盖端**：Backend（权威来源）、Android、iOS、Web（仅读取首页壳，不消费 Feed）
- **核心价值**：统一首页卡片、列表接口与移动端域模型，避免不同端对 Drama 字段的理解分叉
- **当前状态**：Backend / Android / iOS 已围绕首页 Feed 对齐 `episode_count + tags + rating` 字段；详情型扩展字段仍待后续 PRD 定义

## 核心逻辑

### Drama 数据模型

权威来源定义在 `backend/src/lib/schemas.ts:15-25`：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | string | uuid() | 短剧 UUID |
| title | string | min(1) | 短剧标题 |
| description | string | default `""` | 首页卡片描述 |
| cover_url | string \| null | url(), nullable, default null | 封面图 URL |
| category | string | default `""` | 分类标签 |
| episode_count | number | int, min(0) | 集数 |
| tags | string[] | default [] | 首页卡片标签列表 |
| rating | number \| null | min(0), max(10), nullable, default null | 评分 |
| created_at | string | — | 创建时间（ISO 8601 字符串） |
| updated_at | string | — | 更新时间（ISO 8601 字符串） |

相关列表响应定义在 `backend/src/lib/schemas.ts:28-39`：

| 字段 | 类型 | 说明 |
|------|------|------|
| data | `Drama[]` | 列表数据 |
| pagination.page | number | 当前页 |
| pagination.page_size | number | 每页条数（snake_case） |
| pagination.total | number | 总数 |
| pagination.total_pages | number | 总页数 |

### 变化说明：从旧详情字段收口到首页卡片字段

PRD-02 之后，`DramaSchema` 不再以 `total_episodes`、`release_year`、`status`、`play_count` 等旧字段作为当前事实来源，而是以首页信息流卡片最小字段集为准。Schema 测试也明确验证：旧的 `total_episodes` 形态不会再通过当前列表 schema（`backend/src/lib/__tests__/schemas.test.ts:134-167`）。

### Episode 数据模型

定义在 `backend/src/lib/schemas.ts:41-52`：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | string | uuid() | 剧集 UUID |
| drama_id | string | uuid() | 所属短剧 UUID |
| title | string | min(1) | 剧集标题 |
| episode_number | number | int, min(1) | 集数序号 |
| duration | number | int, min(0), optional, nullable | 时长（秒） |
| video_url | string | url(), optional, nullable | 视频 URL |
| thumbnail_url | string | url(), optional, nullable | 缩略图 URL |
| description | string | optional, nullable | 剧集描述 |
| created_at | string | — | 创建时间 |
| updated_at | string | — | 更新时间 |

> 说明：Episode 当前未直接参与 PRD-02 首页 Feed 渲染，但仍是播放器 / 详情能力后续扩展的基础实体。

### UserProfile 数据模型

定义在 `backend/src/lib/schemas.ts:85-93`：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | string | uuid() | 用户 UUID（关联 Supabase auth.users） |
| display_name | string | min(1), optional, nullable | 显示名称 |
| email | string | email(), optional, nullable | 邮箱 |
| avatar_url | string | url(), optional, nullable | 头像 URL |
| created_at | string | — | 创建时间 |
| updated_at | string | — | 更新时间 |

### Player 请求模型

- `PlayerStartRequest`（`backend/src/lib/schemas.ts:68-72`）：`drama_id + episode_id + progress(default 0)`
- `PlayerStopRequest`（`backend/src/lib/schemas.ts:76-81`）：`drama_id + episode_id + progress + duration`

### 数据关系

```text
Drama ──1:N──▶ Episode
User ──1:N──▶ 播放记录（后续 PRD）
```

## 多端实现

### Backend
- 源文件：`backend/src/lib/schemas.ts:1-95`
- 依赖：zod
- 角色：Schema 权威来源；`DramaService` 用 `DramaListResponseSchema` 校验 repository 输出（`backend/src/services/drama/drama.service.ts:5-10`）
- 首页数据源：`backend/src/repositories/mock/drama.mock.repository.ts:4-180` 预置 12 条 `Drama` mock 数据

### Android
- Domain Entity：`android/app/src/main/java/com/djs66256/short_drama/domain/model/Drama.kt:3-14`
- DTO：`android/app/src/main/java/com/djs66256/short_drama/data/dto/DramaDto.kt:6-27`
- 对齐方式：`@SerialName("cover_url")`、`@SerialName("episode_count")` 等映射 snake_case 字段，最终输出 `Drama(coverUrl, episodeCount, tags, rating, ...)`
- 使用场景：`HomeViewModel` / `HomeScreen` 直接消费该 Entity 作为首页卡片数据（`android/app/src/main/java/com/djs66256/short_drama/feature/home/viewmodel/HomeViewModel.kt:17-95`, `android/app/src/main/java/com/djs66256/short_drama/feature/home/ui/HomeScreen.kt:177-232`）

### iOS
- Domain Entity：`ios/ShortDrama/Sources/Domain/Entities/Drama.swift:3-14`
- DTO：`ios/ShortDrama/Sources/Data/DTOs/DramaDTO.swift:3-41`
- 对齐方式：DTO 解码 backend snake_case JSON，并通过 `toDomain()` 映射到 Swift 风格 Entity
- 使用场景：`HomeViewModel` / `HomeView` 直接消费 `Drama` 作为首页卡片数据（`ios/ShortDrama/Sources/Features/Home/ViewModels/HomeViewModel.swift:7-87`, `ios/ShortDrama/Sources/Features/Home/Views/HomeView.swift:73-224`）

### Web
- Web 首页当前不消费 `Drama` Feed，仍展示应用壳信息（`web/src/features/home/HomeScreen.tsx:12-55`）
- 因此本期无需同步一份 Web 端 `Drama` 首页卡片 schema

## 依赖关系

- 依赖 `zod` 做 Backend 运行时类型校验
- Backend 为 Schema 权威来源，Android / iOS 各自通过 DTO + Domain Entity 做手动对齐
- 首页 Feed、播放页入口、详情页入口都依赖 `Drama.id` 作为统一业务标识
- `GET /api/dramas` 的列表契约是当前 `Drama` 模型最主要的对外出口（见 `wiki/api/dramas.md`）

## 已知限制

- 各端仍为手动同步 schema / entity，缺少真正共享的数据模型包，存在演进分叉风险。
- 当前 `Drama` 仅覆盖首页卡片最小字段集；详情页若需要导演、主演、状态、播放量等字段，需在后续 PRD 重新扩展并同步各端。
- Web 首页本期不消费 Feed，因此没有形成 Web 侧等价首页卡片 schema。
- iOS 部分测试样例仍使用非 UUID 字符串作为 mock `id`（如 `ios/ShortDrama/Tests/DataTests/DramaRepositoryTests.swift:13-39`、`ios/ShortDrama/Tests/ViewModelTests/NavigationRouterTests.swift:8-19`），这与 Backend 运行时 UUID 约束不完全一致，但不影响当前单测语义。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-26 | 更新：按 PRD-02 首页信息流实现，将 `Drama` 模型从旧的详情型字段集修正为首页卡片字段集，补充 Android/iOS DTO 与 Entity 的实际对齐方式，并记录列表响应与测试约束 |
| 2026-07-24 | 扩展：Backend 端新增 EpisodeSchema、UserProfileSchema、PlayerStartRequestSchema、PlayerStopRequestSchema、DramaListResponseSchema；DramaSchema 字段从 6 个扩展到 12 个（新增 status、release_year、play_count、total_episodes 等） |
| 2026-07-22 | 从代码提取，初始创建（仅 Web 端 6 字段 DramaSchema + Backend 端 HealthResponseSchema） |

---

*本文档由 llm-wiki skill 自动维护。*