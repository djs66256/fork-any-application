# Admin API 文档

> 最后更新：2026-07-27

管理平台所有 API 使用 `/api/admin/*` 前缀，需携带 `Authorization: Bearer <JWT>` header。JWT 由 Supabase Auth 签发，包含 `app_metadata.role`（admin / editor / viewer）。

统一响应格式：

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

错误码体系：

| 业务错误码 | HTTP 状态码 | 说明 |
|-----------|------------|------|
| `INVALID_PARAMS` | 400 | 参数校验失败 |
| `INVALID_CREDENTIALS` | 401 | 邮箱或密码错误 |
| `UNAUTHORIZED` | 401 | 未登录或 Token 过期 |
| `FORBIDDEN` | 403 | 权限不足 |
| `NOT_FOUND` | 404 | 资源不存在 |
| `CONFLICT` | 409 | 资源冲突（如剧集号重复） |
| `CANNOT_MODIFY_SELF` | 400 | 尝试修改自己的角色 |
| `INTERNAL_ERROR` | 500 | 服务内部错误 |

---

## POST /api/admin/auth/login

### 功能简介

管理员登录，使用 Supabase Auth 邮箱+密码认证。

### 代码文件路径

`backend/src/app/api/admin/auth/login/route.ts`

### path / method

`POST /api/admin/auth/login`

### Body 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | string | 是 | 管理员邮箱 |
| `password` | string | 是 | 密码 |

**示例：**

```json
{
  "email": "admin@example.com",
  "password": "securepassword"
}
```

### Response

```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOi...",
    "user": { "id": "uuid", "email": "admin@example.com", "role": "admin" }
  },
  "message": "ok"
}
```

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `INVALID_PARAMS` | 参数校验失败 |
| 401 | `INVALID_CREDENTIALS` | 邮箱或密码错误 |

---

## POST /api/admin/auth/logout

### 功能简介

管理员登出。

### 代码文件路径

`backend/src/app/api/admin/auth/logout/route.ts`

### path / method

`POST /api/admin/auth/logout`

### Response

```json
{
  "code": 0,
  "data": null,
  "message": "ok"
}
```

---

## GET /api/admin/stats

### 功能简介

获取仪表盘统计数据（总短剧数、总剧集数、总用户数）。

### 代码文件路径

`backend/src/app/api/admin/stats/route.ts`

### path / method

`GET /api/admin/stats`

### Response

```json
{
  "code": 0,
  "data": {
    "total_dramas": 42,
    "total_episodes": 256,
    "total_users": 15
  },
  "message": "ok"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `total_dramas` | number | 短剧总数 |
| `total_episodes` | number | 剧集总数 |
| `total_users` | number | 用户总数 |

### 权限

任意角色（admin / editor / viewer）

---

## GET /api/admin/dramas

### 功能简介

获取短剧列表（分页）。

### 代码文件路径

`backend/src/app/api/admin/dramas/route.ts`

### path / method

`GET /api/admin/dramas`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | 1 | 页码 |
| `pageSize` | number | 否 | 20 | 每页条数（1-100） |

### Response

```json
{
  "code": 0,
  "data": {
    "data": [
      {
        "id": "uuid",
        "title": "...",
        "cover_url": "...",
        "category": "...",
        "episode_count": 24,
        "rating": 8.5,
        "tags": ["tag1"],
        "created_at": "...",
        "updated_at": "..."
      }
    ],
    "pagination": {
      "page": 1,
      "page_size": 20,
      "total": 42,
      "total_pages": 3
    }
  },
  "message": "ok"
}
```

### 权限

任意角色

---

## POST /api/admin/dramas

### 功能简介

新建短剧。

### 代码文件路径

`backend/src/app/api/admin/dramas/route.ts`

### path / method

`POST /api/admin/dramas`

### Body 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `title` | string | 是 | 标题（1-200 字符） |
| `description` | string | 否 | 描述（默认 ""） |
| `cover_url` | string \| null | 否 | 封面图 URL |
| `category` | string | 否 | 分类（默认 ""） |
| `episode_count` | number | 否 | 集数（默认 0） |
| `tags` | string[] | 否 | 标签列表（默认 []） |
| `rating` | number \| null | 否 | 评分（0-10） |

### Response

```json
{
  "code": 0,
  "data": { "id": "uuid", "title": "...", "..." : "..." },
  "message": "ok"
}
```

### 权限

admin / editor

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 201 | — | 创建成功 |
| 400 | `INVALID_PARAMS` | 参数校验失败 |
| 401 | `UNAUTHORIZED` | 未登录 |
| 403 | `FORBIDDEN` | 权限不足 |

---

## GET /api/admin/dramas/:id

### 功能简介

获取短剧详情。

### 代码文件路径

`backend/src/app/api/admin/dramas/[id]/route.ts`

### path / method

`GET /api/admin/dramas/:id`

### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 是 | 短剧 UUID |

### 权限

任意角色

---

## PUT /api/admin/dramas/:id

### 功能简介

编辑短剧。

### 代码文件路径

`backend/src/app/api/admin/dramas/[id]/route.ts`

### path / method

`PUT /api/admin/dramas/:id`

### Body 参数

同 POST /api/admin/dramas，所有字段可选（partial update）。

### 权限

admin / editor

---

## DELETE /api/admin/dramas/:id

### 功能简介

删除短剧（级联删除关联剧集）。

### 代码文件路径

`backend/src/app/api/admin/dramas/[id]/route.ts`

### path / method

`DELETE /api/admin/dramas/:id`

### Response

```json
{
  "code": 0,
  "data": { "deleted": true },
  "message": "ok"
}
```

### 权限

admin / editor

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 删除成功 |
| 404 | `NOT_FOUND` | 短剧不存在 |

---

## GET /api/admin/dramas/:id/episodes

### 功能简介

获取某短剧的剧集列表。

### 代码文件路径

`backend/src/app/api/admin/dramas/[id]/episodes/route.ts`

### path / method

`GET /api/admin/dramas/:id/episodes`

### Response

```json
{
  "code": 0,
  "data": {
    "drama_id": "uuid",
    "items": [
      {
        "id": "uuid",
        "drama_id": "uuid",
        "title": "...",
        "episode_number": 1,
        "duration": 120,
        "video_url": "...",
        "thumbnail_url": "...",
        "description": "..."
      }
    ]
  },
  "message": "ok"
}
```

### 权限

任意角色

---

## POST /api/admin/dramas/:id/episodes

### 功能简介

新建剧集。

### 代码文件路径

`backend/src/app/api/admin/dramas/[id]/episodes/route.ts`

### path / method

`POST /api/admin/dramas/:id/episodes`

### Body 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `title` | string | 是 | 剧集标题 |
| `episode_number` | number | 是 | 剧集号（>=1） |
| `duration` | number \| null | 否 | 时长（秒） |
| `video_url` | string \| null | 否 | 视频 URL |
| `thumbnail_url` | string \| null | 否 | 缩略图 URL |
| `description` | string \| null | 否 | 描述 |

### 权限

admin / editor

---

## PUT /api/admin/episodes/:id

### 功能简介

编辑剧集。

### 代码文件路径

`backend/src/app/api/admin/episodes/[id]/route.ts`

### path / method

`PUT /api/admin/episodes/:id`

### Body 参数

同 POST /api/admin/dramas/:id/episodes，所有字段可选（partial update）。

### 权限

admin / editor

---

## DELETE /api/admin/episodes/:id

### 功能简介

删除剧集。

### 代码文件路径

`backend/src/app/api/admin/episodes/[id]/route.ts`

### path / method

`DELETE /api/admin/episodes/:id`

### 权限

admin / editor

---

## GET /api/admin/users

### 功能简介

获取用户列表（分页）。仅 admin 可访问。

### 代码文件路径

`backend/src/app/api/admin/users/route.ts`

### path / method

`GET /api/admin/users`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | 1 | 页码 |
| `pageSize` | number | 否 | 20 | 每页条数 |

### 权限

admin only

---

## PUT /api/admin/users/:id/role

### 功能简介

修改用户角色。

### 代码文件路径

`backend/src/app/api/admin/users/[id]/role/route.ts`

### path / method

`PUT /api/admin/users/:id/role`

### Body 参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `role` | enum | 是 | `"admin"` / `"editor"` / `"viewer"` |

**示例：**

```json
{
  "role": "editor"
}
```

### 权限

admin only

### 约束

- 不可修改自己的角色（返回 `CANNOT_MODIFY_SELF` 错误）
- 目标用户不存在时返回 `NOT_FOUND`

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `CANNOT_MODIFY_SELF` | 不可修改自己的角色 |
| 400 | `INVALID_PARAMS` | 参数校验失败 |
| 403 | `FORBIDDEN` | 权限不足 |
| 404 | `NOT_FOUND` | 用户不存在 |

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-27 | 初始创建，收录 14 个管理 API 端点 |

---
*本文档由 llm-wiki skill 自动维护。*