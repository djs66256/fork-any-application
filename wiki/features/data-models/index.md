# 数据模型 (Data Models)

> 最后更新：2026-07-22

## 功能概述

项目核心数据模型定义，当前仅包含短剧（Drama）实体的 Zod Schema 校验结构。

- **覆盖端**：Web（前端校验）、Backend（后端校验）
- **核心价值**：统一数据结构约束，确保各端数据输入输出一致性

## 核心逻辑

### Drama 数据模型

定义在 `web/src/lib/schemas.ts:3-12` 和 `backend/src/lib/schemas.ts:3-12` 中（当前二者结构不同——Web 端有 DramaSchema，Backend 端仅有 HealthResponseSchema）。

**Web 端 Drama 模型**（`web/src/lib/schemas.ts:3-12`）：
```typescript
DramaSchema = z.object({
  id: z.string(),
  title: z.string().min(1),
  description: z.string(),
  coverUrl: z.string().url(),
  category: z.string(),
  episodeCount: z.number().int().positive(),
})
```

字段说明：
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | string | — | 短剧唯一标识 |
| title | string | min(1) | 短剧标题 |
| description | string | — | 短剧描述 |
| coverUrl | string | url() | 封面图 URL |
| category | string | — | 分类 |
| episodeCount | number | int, positive | 集数 |

**Backend 端**仅有 `HealthResponseSchema`，尚未定义 Drama 相关 Schema。

## 多端实现

### Web
- 源文件：`web/src/lib/schemas.ts:1-12`
- 导出类型：`Drama`（从 `DramaSchema` 推断）
- 依赖：zod 库

### Backend
- 源文件：`backend/src/lib/schemas.ts:1-9`
- 当前仅定义 `HealthResponseSchema`，未定义业务数据模型
- 依赖：zod ^4.4.3

## 依赖关系

- 依赖 `zod` 库做运行时类型校验
- Web 和 Backend 各自独立维护 schemas.ts，当前未共享

## 已知限制

- Web 端和 Backend 端的 schemas 不一致：Web 有 Drama 模型，Backend 没有
- 两端未共享同一份 Schema 定义，存在重复定义风险
- Android/iOS 端无对应的数据模型定义（无类型安全的数据校验）
- 缺少用户、评论、剧集等其他业务实体的模型定义
