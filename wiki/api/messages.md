# Messages API 文档

> 最后更新：2026-07-29

---

## 概述

PRD-10 新增的消息 API 覆盖菜单消息预览、系统消息列表与互动消息列表三类资源。与 Auth API 的 envelope 结构不同，消息接口使用两种资源级响应：

- preview：返回单对象，空态返回 `204 No Content`
- list：返回 `{ data, pagination }`

这三条接口共同支撑 Native 端菜单消息入口与独立消息中心页（`backend/src/app/api/messages/preview/route.ts:9-20`、`backend/src/app/api/messages/system/route.ts:10-23`、`backend/src/app/api/messages/interactions/route.ts:11-30`、`docs/specs/2026-07-29-prd-10-signin-messages/spec.md:24-35`）。

当前消息 contract 的关键特点：

- `GET /api/messages/preview` 匿名可访问，空态返回 204（`backend/src/app/api/messages/preview/route.ts:14-18`）。
- `GET /api/messages/system` 匿名可访问，分页参数统一为 `page/pageSize`（`backend/src/app/api/messages/system/route.ts:10-23`）。
- `GET /api/messages/interactions` 通过 `requireAuthContext()` 强制登录，并从 `request.auth.userId` 读取当前用户（`backend/src/app/api/messages/interactions/route.ts:11-29`、`backend/src/middleware/auth.ts:97-138`）。
- preview 的 `relative_time` 由服务端按 `sent_at` 实时格式化为“x分钟前 / x小时前”（`backend/src/services/message/message.service.ts:17-27,47-54`）。
- 互动消息首版固定由 mock repository 提供，不存在持久化表；系统消息支持 mock / supabase 切换（`backend/src/repositories/repository-registry.ts:59-69`、`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:24-40`）。

---

## GET /api/messages/preview

### 功能简介

返回菜单抽屉使用的最新 1 条系统消息摘要，供 Native 端“我的消息”模块展示标题、摘要和相对时间（`backend/src/app/api/messages/preview/route.ts:9-20`、`backend/src/services/message/message.service.ts:40-61`）。

### 代码文件路径

- Route：`backend/src/app/api/messages/preview/route.ts:9-20`
- Service：`backend/src/services/message/message.service.ts:17-27,40-61`
- Schema：`backend/src/lib/schemas.ts:125-130`

### path / method

`GET /api/messages/preview`

### Request

无 query、无 body、无认证要求。

### Success Response

```json
{
  "title": "系统通知",
  "summary": "你关注的剧集已更新第 12 集。",
  "relative_time": "2小时前"
}
```

### 当前行为说明

- Route 初始化 `MessageService` 后直接调用 `service.getPreview()`；返回 `null` 时输出 `204 No Content`（`backend/src/app/api/messages/preview/route.ts:9-18`）。
- Service 从 system message repository 取最新一条消息；如果有数据，会把 `sent_at` 转成 `relative_time` 字段（`backend/src/services/message/message.service.ts:40-54`）。
- `relative_time` 的当前格式只有“x分钟前 / x小时前”两档，不返回绝对时间（`backend/src/services/message/message.service.ts:17-27`）。
- repository 或 schema 异常会统一映射为 `SERVICE_UNAVAILABLE`（`backend/src/services/message/message.service.ts:55-60`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功返回最新 1 条摘要 |
| 204 | — | 当前无系统消息 |
| 503 | `SERVICE_UNAVAILABLE` | 数据源不可用 |

---

## GET /api/messages/system

### 功能简介

分页获取系统消息列表，供消息中心页“系统消息”分区使用；匿名用户也可访问（`backend/src/app/api/messages/system/route.ts:10-23`、`backend/src/services/message/message.service.ts:63-72`）。

### 代码文件路径

- Route：`backend/src/app/api/messages/system/route.ts:10-23`
- Service：`backend/src/services/message/message.service.ts:63-72`
- Schema：`backend/src/lib/schemas.ts:132-159`

### path / method

`GET /api/messages/system`

### Query Parameters

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | 1 | 页码，必须 `>= 1` |
| `pageSize` | number | 否 | 20 | 每页条数，`1 <= pageSize <= 20` |

### Success Response

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "title": "系统通知",
      "summary": "你关注的剧集已更新第 12 集。",
      "sent_at": "2026-07-29T08:00:00.000Z"
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 1,
    "total_pages": 1
  }
}
```

### 当前行为说明

- Route 使用 `MessageListQuerySchema` 校验 `page/pageSize`，并对缺失参数应用默认值（`backend/src/app/api/messages/system/route.ts:10-15`）。
- Service 直接调用 `systemMessageRepository.list(input)`，再用 `SystemMessageListResponseSchema` 做最终 parse（`backend/src/services/message/message.service.ts:63-72`）。
- `SystemMessageSchema` 当前字段只有 `id/title/summary/sent_at`，不包含消息详情、跳转链接或已读状态（`backend/src/lib/schemas.ts:132-153`）。
- 当 system message repository 切到 Supabase 实现时，列表按 `sent_at DESC` 排序（对应表索引见 migration，`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:24-33`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `VALIDATION_ERROR` | `page` 或 `pageSize` 非法 |
| 503 | `SERVICE_UNAVAILABLE` | 数据源不可用 |

---

## GET /api/messages/interactions

### 功能简介

分页获取登录用户的互动消息列表，供消息中心页“互动消息”分区使用。该接口是 PRD-10 中唯一必须登录的消息接口（`backend/src/app/api/messages/interactions/route.ts:11-30`、`backend/src/services/message/message.service.ts:74-97`）。

### 代码文件路径

- Route：`backend/src/app/api/messages/interactions/route.ts:11-30`
- Auth Middleware：`backend/src/middleware/auth.ts:69-103,132-138`
- Service：`backend/src/services/message/message.service.ts:74-97`
- Schema：`backend/src/lib/schemas.ts:140-159`

### path / method

`GET /api/messages/interactions`

### Headers

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Authorization` | string | 是 | `Bearer <token>` |

### Query Parameters

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | 1 | 页码，必须 `>= 1` |
| `pageSize` | number | 否 | 20 | 每页条数，`1 <= pageSize <= 20` |

### Success Response

```json
{
  "data": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440010",
      "type": "comment_reply",
      "title": "有人回复了你的评论",
      "summary": "“这集反转真不错” 收到一条新回复。",
      "sent_at": "2026-07-29T09:00:00.000Z"
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 1,
    "total_pages": 1
  }
}
```

### 当前行为说明

- Route 先由 `requireAuthContext()` 验证 bearer token，再通过 `getAuth(request)` 读取 `userId`（`backend/src/app/api/messages/interactions/route.ts:11-24`、`backend/src/middleware/auth.ts:97-138`）。
- Service 在 `userId` 缺失时会主动抛 `Errors.authUnauthorized('请先登录后查看互动消息')`（`backend/src/services/message/message.service.ts:74-82`）。
- 当前互动消息实体包含 `type/title/summary/sent_at`，`type` 允许 `comment_reply / comment_like / system_hint`（`backend/src/lib/schemas.ts:140-147`）。
- 互动消息 repository 当前固定为 mock 实现，不随 config 切换，也没有对应 Supabase 表（`backend/src/repositories/repository-registry.ts:67-69,136-142`、`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:24-40`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `VALIDATION_ERROR` | `page` 或 `pageSize` 非法 |
| 401 | `AUTH_UNAUTHORIZED` | 未登录或 token 无效 |
| 503 | `SERVICE_UNAVAILABLE` | 数据源不可用 |

---

## 数据结构补充

### `MessagePreview`

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | string | 标题 |
| `summary` | string | 摘要 |
| `relative_time` | string | 相对时间，如 `2小时前` |

### `SystemMessage`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string(UUID) | 消息 ID |
| `title` | string | 标题 |
| `summary` | string | 摘要 |
| `sent_at` | string | 发送时间 |

### `InteractionMessage`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string(UUID) | 消息 ID |
| `type` | enum | `comment_reply` / `comment_like` / `system_hint` |
| `title` | string | 标题 |
| `summary` | string | 摘要 |
| `sent_at` | string | 发送时间 |

---

## 与客户端实现的关系

- Android `MessageRemoteDataSource` 会把 preview 的 204 明确解码为 `ApiResult.Success(null)`，系统消息和互动消息则统一走列表 contract（`android/app/src/main/java/com/djs66256/short_drama/data/datasource/MessageRemoteDataSource.kt:20-52`）。
- Android `AuthInterceptor` 只为 `messages/interactions` 自动补 `Authorization`，preview 与 system 不要求登录（`android/app/src/main/java/com/djs66256/short_drama/core/network/AuthInterceptor.kt:38-50`）。
- iOS 菜单消息预览会把 503 降级成“暂无消息”或静态兜底文案，以保证入口仍可点击（`ios/ShortDrama/Sources/Features/MenuPanel/ViewModels/MenuPanelViewModel.swift:142-154`）。
- iOS 消息中心在登录成功后继续停留在 `.messages`，并基于 `authStore.$status` 刷新互动消息区（`ios/ShortDrama/Sources/Features/Messages/Views/MessageCenterView.swift:27-35`、`ios/ShortDrama/Sources/App/NavigationRouter.swift:183-203`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录 PRD-10 `messages/preview`、`messages/system`、`messages/interactions` 的匿名 / 登录分流、204 空态与双列表 contract |

---
*本文档由 llm-wiki skill 自动维护。*