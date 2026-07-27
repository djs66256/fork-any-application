# Admin API 规范

## 路由前缀

所有管理 API 使用 `/api/admin/*` 前缀，与 consumer API（`/api/dramas`、`/api/player/*`）完全隔离。

## API 端点总览

### Drama 管理

| 方法 | 路径 | 说明 | 所需角色 |
|------|------|------|---------|
| GET | `/api/admin/dramas` | 短剧列表（分页、搜索） | admin/editor/viewer |
| POST | `/api/admin/dramas` | 新建短剧 | admin/editor |
| PUT | `/api/admin/dramas/:id` | 更新短剧 | admin/editor |
| DELETE | `/api/admin/dramas/:id` | 删除短剧（级联删除 episodes） | admin/editor |

### Episode 管理

| 方法 | 路径 | 说明 | 所需角色 |
|------|------|------|---------|
| GET | `/api/admin/dramas/:dramaId/episodes` | 短剧下剧集列表 | admin/editor/viewer |
| POST | `/api/admin/dramas/:dramaId/episodes` | 新建剧集（校验 episode_number 唯一） | admin/editor |
| PUT | `/api/admin/episodes/:id` | 更新剧集（校验 episode_number 唯一） | admin/editor |
| DELETE | `/api/admin/episodes/:id` | 删除剧集 | admin/editor |

### User 管理

| 方法 | 路径 | 说明 | 所需角色 |
|------|------|------|---------|
| GET | `/api/admin/users` | 用户列表 | admin |
| PUT | `/api/admin/users/:id/role` | 更新用户角色 | admin |

### 统计

| 方法 | 路径 | 说明 | 所需角色 |
|------|------|------|---------|
| GET | `/api/admin/stats` | 仪表盘统计数据 | admin/editor/viewer |

## 响应格式

统一使用简洁格式，与现有 consumer API 风格保持一致。**不使用 `{ code, message }` 包装层。**

### 列表响应

```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 100,
    "total_pages": 5
  }
}
```

适用于：`GET /api/admin/dramas`、`GET /api/admin/dramas/:dramaId/episodes`、`GET /api/admin/users`

### 单条资源响应

```json
{
  "data": {
    "id": "uuid",
    "title": "...",
    ...
  }
}
```

适用于：`POST/PUT/DELETE` 所有端点（返回创建/更新后的资源对象）

### 统计响应

```json
{
  "data": {
    "dramas": 42,
    "episodes": 315,
    "users": 8
  }
}
```

适用于：`GET /api/admin/stats`

### 错误响应

```json
{
  "error": "Forbidden"
}
```

状态码：401（未认证）、403（无权限）、404（资源不存在）、409（冲突，如 episode_number 重复）、422（参数校验失败）

## 认证方式

所有 `/api/admin/*` 请求必须携带：

```
Authorization: Bearer <supabase-jwt>
```

由 Auth 中间件 `requireRole(...)` 统一验证。

## 约束

### episode_count 字段

- Drama `episode_count` 不由客户端直接写入
- 后端从 episodes 表实时 COUNT 计算
- 短剧新建/编辑表单中不暴露此字段

### episode_number 唯一性

- 同一 `drama_id` 下 `episode_number` 唯一
- `POST /api/admin/dramas/:dramaId/episodes` 和 `PUT /api/admin/episodes/:id` 需校验
- 重复时返回 409 Conflict

### 级联删除

- `DELETE /api/admin/dramas/:id` 时级联删除该短剧下所有 episodes
- 前端删除前显示确认弹窗：「删除短剧将同时删除所有关联剧集，不可恢复」

## 代码实现

Admin API 遵循 Backend 四层架构（Route → Service → Repository）：
- Route 层：`backend/src/app/api/admin/dramas/route.ts` 等
- Service 层：`backend/src/services/admin/` 下
- Repository 层：`backend/src/repositories/supabase/` 下（对接 Supabase 客户端）
- 中间件：`backend/src/middleware/admin-auth.ts`
