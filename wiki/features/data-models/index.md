# 数据模型 (Data Models)

> 最后更新：2026-07-24

## 功能概述

项目核心数据模型定义，包含 Drama（短剧）、Episode（剧集）、UserProfile（用户）的 Zod Schema 及对应的 TypeScript 类型。Backend 端为权威来源，Web 端手动同步。

- **覆盖端**：Backend（权威来源）、Web
- **核心价值**：统一数据结构约束，确保各端数据输入输出一致性

## 核心逻辑

### Drama 数据模型

定义在 `backend/src/lib/schemas.ts:15-28`（Backend 端，权威来源）：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | string | uuid() | 短剧 UUID |
| title | string | min(1) | 短剧标题 |
| description | string | optional, nullable | 短剧描述 |
| cover_url | string | url(), optional, nullable | 封面图 URL |
| category | string | optional, nullable | 分类标签 |
| total_episodes | number | int, min(0) | 总集数 |
| release_year | number | int, optional, nullable | 发行年份 |
| rating | number | min(0), max(10), optional, nullable | 评分（0-10） |
| status | enum | 'ongoing'/'completed'/'announced' | 连载状态 |
| created_at | string | — | 创建时间（ISO 8601） |
| updated_at | string | — | 更新时间（ISO 8601） |
| play_count | number | int, min(0), default 0 | 播放次数 |

### Episode 数据模型

定义在 `backend/src/lib/schemas.ts:32-43`：

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

### UserProfile 数据模型

定义在 `backend/src/lib/schemas.ts:76-84`：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | string | uuid() | 用户 UUID（关联 Supabase auth.users） |
| display_name | string | min(1), optional, nullable | 显示名称 |
| email | string | email(), optional, nullable | 邮箱 |
| avatar_url | string | url(), optional, nullable | 头像 URL |
| created_at | string | — | 创建时间 |
| updated_at | string | — | 更新时间 |

### Player 请求模型

- `PlayerStartRequest`（`backend/src/lib/schemas.ts:59-63`）：drama_id + episode_id + progress(默认0)
- `PlayerStopRequest`（`backend/src/lib/schemas.ts:67-72`）：drama_id + episode_id + progress + duration

### 数据关系

```
Drama ──1:N──▶ Episode
User ──1:N──▶ 播放记录（后续 PRD）
```

## 多端实现

### Backend
- 源文件：`backend/src/lib/schemas.ts:1-86`
- 依赖：zod ^4
- 导出：DramaSchema, EpisodeSchema, DramaListResponseSchema, PlayerStartRequestSchema, PlayerStopRequestSchema, UserProfileSchema, HealthResponseSchema
- 数据类型：`type Drama = z.infer<typeof DramaSchema>` 等

### Web
- 源文件：`web/src/lib/schemas.ts`
- 同步方式：手动对齐 Backend Schema 结构
- 依赖：zod

### iOS
- 源文件：`ios/ShortDrama/Sources/Domain/Entities/Drama.swift`, `Episode.swift`
- 对齐方式：Swift struct 字段名和类型与 Backend Schema 一致，使用 Codable + snake_case 策略

### Android
- 源文件：`android/app/src/main/java/com/djs66256/short_drama/domain/model/Drama.kt`, `Episode.kt`
- 对齐方式：Kotlin data class，使用 `@SerializedName` 注解对齐 JSON key

## 依赖关系

- 依赖 `zod` 库做运行时类型校验
- Backend 为 Schema 权威来源
- 各端手动同步 Schema 结构（长期可评估抽取共享 Schema package）

## 已知限制

- 各端手动同步 Schema，不共享同一份定义文件，存在不一致风险
- 当前 Drama/Episode 的字段较多使用 optional/nullable，后续业务 PRD 应明确必填字段
- 缺少播放历史、收藏、评论等业务实体定义

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 扩展：Backend 端新增 EpisodeSchema、UserProfileSchema、PlayerStartRequestSchema、PlayerStopRequestSchema、DramaListResponseSchema；DramaSchema 字段从 6 个扩展到 12 个（新增 status、release_year、play_count、total_episodes 等） |
| 2026-07-22 | 从代码提取，初始创建（仅 Web 端 6 字段 DramaSchema + Backend 端 HealthResponseSchema） |

---

*本文档由 llm-wiki skill 自动维护。*
