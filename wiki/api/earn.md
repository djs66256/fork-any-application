# 赚钱中心 API 文档

> 最后更新：2026-07-29

---

## GET /api/earn/overview

### 功能简介

返回赚钱首页首屏所需的收益总览、新手任务、连续看剧福利 7 宫格和现金任务列表。该接口允许匿名访问；如果请求携带有效 bearer token，则会按已登录视角返回金币余额与任务完成状态；无 token 或无效 token 都会降级为匿名视角（`backend/src/app/api/earn/overview/route.ts:7-12`, `backend/src/app/api/__tests__/earn-overview.test.ts:37-90`）。

### 代码文件路径

- Route：`backend/src/app/api/earn/overview/route.ts:1-13`
- Service：`backend/src/services/earn/earn.service.ts:15-30`
- Schema：`backend/src/lib/schemas.ts:122-182`
- Mock Repository：`backend/src/repositories/mock/earn.mock.repository.ts:18-136`
- 测试：`backend/src/app/api/__tests__/earn-overview.test.ts:30-101`

### path / method

`GET /api/earn/overview`

### Request

#### Headers

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Authorization` | string | 否 | 可选 bearer access token；有效时返回已登录视角，无效时回退匿名视角 |

### Success Response

```json
{
  "coins": 1200,
  "is_logged_in": true,
  "new_user_task": {
    "id": "11111111-1111-4111-8111-111111111111",
    "title": "新人7天保底6元",
    "description": "完成首次看剧任务即可领取金币奖励",
    "reward_coins": 600,
    "status": "available",
    "action": {
      "type": "play",
      "video_id": "drama-001-episode-01"
    }
  },
  "daily_rewards": [
    { "day": 1, "coins": 10, "status": "claimable" },
    { "day": 2, "coins": 20, "status": "locked" },
    { "day": 3, "coins": 30, "status": "locked" },
    { "day": 4, "coins": 40, "status": "locked" },
    { "day": 5, "coins": 50, "status": "locked" },
    { "day": 6, "coins": 60, "status": "locked" },
    { "day": 7, "coins": 70, "status": "locked" }
  ],
  "cash_tasks": [
    {
      "id": "22222222-2222-4222-8222-222222222222",
      "title": "看剧领现金",
      "description": "完整观看指定短剧可获得金币",
      "reward_coins": 500,
      "status": "available",
      "action": {
        "type": "play",
        "video_id": "drama-001-episode-01"
      },
      "is_representative": true
    }
  ]
}
```

### 当前行为说明

- `EarnOverviewResponseSchema` 约束返回结构必须包含 `coins`、`is_logged_in`、1 个 `new_user_task`、固定 7 项 `daily_rewards` 以及 `cash_tasks` 列表（`backend/src/lib/schemas.ts:161-182`）。
- 路由通过 `resolveOptionalAuthContext()` 解析可选鉴权，再交给 `EarnService.getOverview()`；无登录态也会返回 200（`backend/src/app/api/earn/overview/route.ts:7-12`, `backend/src/services/earn/earn.service.ts:18-30`）。
- 当前 mock 实现中，匿名视角固定返回 `coins=0`；已登录视角固定从 `LOGGED_IN_BASE_COINS = 1200` 起算，并把已完成代表性任务的奖励累加到 `coins`（`backend/src/repositories/mock/earn.mock.repository.ts:18-22`, `backend/src/repositories/mock/earn.mock.repository.ts:109-136`）。
- 当前 mock 数据固定包含：1 个新手任务、2 个现金任务（其中只有 `看剧领现金` 标记为 `is_representative=true`，另一个为 `placeholder` 锁定任务），以及 7 项连续看剧福利（`backend/src/repositories/mock/earn.mock.repository.ts:23-69`）。
- 自动化测试已覆盖匿名访问、有效 bearer 返回已登录视角、无效 bearer 降级为匿名视角，以及异常情况下返回 500 `INTERNAL_ERROR`（`backend/src/app/api/__tests__/earn-overview.test.ts:37-101`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功；匿名与已登录都返回同一结构 |
| 500 | `INTERNAL_ERROR` | Repository/Service 返回非预期结构或抛出未处理异常 |

---

## POST /api/earn/complete-task

### 功能简介

完成赚钱中心代表性任务并结算金币奖励。该接口必须携带 bearer access token，且当前只允许完成被标记为 `is_representative=true` 的任务；重复提交同一任务保持幂等成功，第二次开始 `coins_earned=0`（`backend/src/app/api/earn/complete-task/route.ts:8-15`, `backend/src/repositories/mock/earn.mock.repository.ts:138-164`, `backend/src/app/api/__tests__/earn-complete-task.test.ts:92-168`）。

### 代码文件路径

- Route：`backend/src/app/api/earn/complete-task/route.ts:1-16`
- Service：`backend/src/services/earn/earn.service.ts:32-47`
- Schema：`backend/src/lib/schemas.ts:170-182`
- Repository：`backend/src/repositories/mock/earn.mock.repository.ts:138-164`
- Error 定义：`backend/src/lib/errors.ts:1-136`
- 测试：`backend/src/app/api/__tests__/earn-complete-task.test.ts:18-192`

### path / method

`POST /api/earn/complete-task`

### Request

#### Headers

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Authorization` | string | 是 | bearer access token；缺失或无效时返回 `401 AUTH_UNAUTHORIZED` |

#### Body

```json
{
  "task_id": "22222222-2222-4222-8222-222222222222"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `task_id` | string (UUID) | 是 | 代表性任务 ID |

### Success Response

```json
{
  "success": true,
  "task_id": "22222222-2222-4222-8222-222222222222",
  "coins_earned": 500,
  "total_coins": 1700,
  "task_status": "completed"
}
```

### 当前行为说明

- 路由会先读取 JSON body，用 `CompleteEarnTaskRequestSchema` 校验 `task_id` 必须是 UUID，然后通过 `resolveRequiredAuthContext()` 强制要求登录态（`backend/src/app/api/earn/complete-task/route.ts:8-15`, `backend/src/lib/schemas.ts:170-182`）。
- `EarnService.completeTask()` 只把 `userId + taskId` 透传给 repository，并再次用 `CompleteEarnTaskResponseSchema` 校验返回值（`backend/src/services/earn/earn.service.ts:32-47`）。
- 当前 mock 实现会先按 `taskId` 查找现金任务或新手任务；找不到时抛 `NOT_FOUND`，不是代表性任务时抛 `CONFLICT`，只有 `is_representative=true` 的任务允许结算（`backend/src/repositories/mock/earn.mock.repository.ts:138-147`, `backend/src/lib/errors.ts:67-99`）。
- 首次完成代表性任务会把 `reward_coins` 计入总金币；重复完成只返回 `coins_earned=0`，但仍保持 `success=true` 与 `task_status='completed'`（`backend/src/repositories/mock/earn.mock.repository.ts:149-164`）。
- 自动化测试已覆盖：缺失 bearer 401、无效 `task_id` 400 `VALIDATION_ERROR`、未知任务 404 `NOT_FOUND`、有效 bearer 完成代表性任务、重复完成幂等成功，以及无效 bearer 401 `AUTH_UNAUTHORIZED`（`backend/src/app/api/__tests__/earn-complete-task.test.ts:25-190`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功；重复完成同样返回成功 |
| 400 | `VALIDATION_ERROR` | `task_id` 不是合法 UUID |
| 401 | `AUTH_UNAUTHORIZED` | 缺失 bearer token 或 bearer token 无效 |
| 404 | `NOT_FOUND` | `task_id` 不存在 |
| 409 | `CONFLICT` | 任务存在，但不是允许完成的代表性任务 |
| 500 | `INTERNAL_ERROR` | Service/Repository 返回结构异常或抛出未处理错误 |

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录赚钱中心 overview 与 complete-task 两个接口，覆盖可选鉴权首屏、代表性任务 Bearer-only 完成与幂等语义 |

---
*本文档由 llm-wiki skill 自动维护。*
