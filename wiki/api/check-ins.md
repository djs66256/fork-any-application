# Check-Ins API 文档

> 最后更新：2026-07-29

---

## 概述

PRD-10 新增的签到 API 负责承接首页冷启动签到浮层、当日签到提交与 7 日签到板状态恢复。与 Auth API 使用 `{ code, data, message }` envelope 不同，签到接口直接返回单个 `SignInStatus` 对象，不再套统一 envelope；客户端需按该资源自己的 contract 解码（`backend/src/app/api/check-ins/status/route.ts:8-18`、`backend/src/app/api/check-ins/route.ts:8-18`、`docs/specs/2026-07-29-prd-10-signin-messages/spec.md:28-35`）。

当前签到 contract 的关键特点：

- 可选登录：登录态优先按账号记账；匿名态必须带 `X-Installation-Id`（`backend/src/services/check-in/check-in.service.ts:70-86`）。
- 安装标识只用于签到接口，不扩散到消息接口（`docs/specs/2026-07-29-prd-10-signin-messages/design.md:32-36`）。
- 服务端返回 `server_date` 作为当前业务日权威值，客户端本地关闭态必须按该日期判断（`backend/src/services/check-in/check-in.service.ts:88-90,140-149`）。
- 同一业务日内签到提交幂等；第 8 天自动重新从第 1 天开始新一轮（`backend/src/services/check-in/check-in.service.ts:92-109`）。
- 唯一索引 `(subject_type, subject_id, business_date)` 是当前幂等基础（`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:1-14`）。

---

## GET /api/check-ins/status

### 功能简介

查询当前账号或匿名安装在当前服务端业务日下的签到状态，用于首页冷启动后判断是否展示签到浮层，并渲染 7 日签到板（`backend/src/app/api/check-ins/status/route.ts:8-18`、`backend/src/services/check-in/check-in.service.ts:43-48`）。

### 代码文件路径

- Route：`backend/src/app/api/check-ins/status/route.ts:8-18`
- Header 解析：`backend/src/app/api/check-ins/parse-installation-id.ts:5-17`
- Service：`backend/src/services/check-in/check-in.service.ts:43-48,70-155`
- Schema：`backend/src/lib/schemas.ts:103-123`

### path / method

`GET /api/check-ins/status`

### Request

#### Headers

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Authorization` | string | 否 | `Bearer <token>`；有登录态时可带 |
| `X-Installation-Id` | string(UUID) | 匿名态必填 | 匿名签到主体；登录态可同时透传，但服务端以账号优先 |

### Success Response

```json
{
  "server_date": "2026-07-29",
  "should_show_popup": true,
  "today_signed": false,
  "current_streak": 3,
  "reward_copy": "今日签到可领取第 4 天奖励",
  "days": [
    {
      "day": 1,
      "title": "第 1 天",
      "reward_label": "金币 x10",
      "status": "signed"
    },
    {
      "day": 4,
      "title": "第 4 天",
      "reward_label": "金币 x40",
      "status": "today"
    }
  ]
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `server_date` | string | 服务端业务日，格式 `YYYY-MM-DD` |
| `should_show_popup` | boolean | 服务端资格判断；客户端仍需叠加冷启动、本地关闭态与模态冲突 |
| `today_signed` | boolean | 当前业务日是否已签到 |
| `current_streak` | number | 当前连续签到进度，范围 0~7 |
| `reward_copy` | string | 浮层文案 |
| `days` | `SignInDay[]` | 固定 7 个元素的签到板状态 |
| `days[].status` | enum | `signed` / `today` / `locked` |

### 当前行为说明

- Route 会先用 `resolveOptionalAuthContext()` 解析 bearer token，再用 `parseInstallationId()` 解析安装标识（`backend/src/app/api/check-ins/status/route.ts:8-15`）。
- `parseInstallationId()` 只接受合法 UUID，非法 header 直接抛 `Errors.validationError('Invalid X-Installation-Id')`（`backend/src/app/api/check-ins/parse-installation-id.ts:5-17`）。
- Service 的主体解析规则是：`userId` 存在则直接使用账号；否则要求 installationId；两者都没有则报错（`backend/src/services/check-in/check-in.service.ts:70-86`）。
- `should_show_popup` 当前服务端实现仅等于 `!todaySigned`；客户端仍需结合本地 dismissed `server_date` 和 UI 上下文做最终展示决策（`backend/src/services/check-in/check-in.service.ts:140-149`、`docs/specs/2026-07-29-prd-10-signin-messages/design-backend.md:171-177`）。
- `days` 固定 7 格，已签格用历史 cycle 计算，未签到时仅下一格标记为 `today`（`backend/src/services/check-in/check-in.service.ts:111-149`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `VALIDATION_ERROR` | 匿名态缺少安装标识，或 `X-Installation-Id` 非法 |
| 503 | `SERVICE_UNAVAILABLE` | 仓储不可用或返回结构非法 |

---

## POST /api/check-ins

### 功能简介

提交当日签到；同一服务端业务日内幂等。成功后返回最新签到状态，响应结构与 `GET /api/check-ins/status` 完全一致（`backend/src/app/api/check-ins/route.ts:8-18`、`backend/src/services/check-in/check-in.service.ts:50-68`）。

### 代码文件路径

- Route：`backend/src/app/api/check-ins/route.ts:8-18`
- Header 解析：`backend/src/app/api/check-ins/parse-installation-id.ts:5-17`
- Service：`backend/src/services/check-in/check-in.service.ts:50-68,92-155`
- Migration：`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:1-23`

### path / method

`POST /api/check-ins`

### Request

#### Headers

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Authorization` | string | 否 | 登录态 bearer token |
| `X-Installation-Id` | string(UUID) | 匿名态必填 | 匿名签到主体 |

#### Body

无请求体。

### Success Response

```json
{
  "server_date": "2026-07-29",
  "should_show_popup": false,
  "today_signed": true,
  "current_streak": 4,
  "reward_copy": "已完成第 4 天签到",
  "days": [
    {
      "day": 4,
      "title": "第 4 天",
      "reward_label": "金币 x40",
      "status": "signed"
    }
  ]
}
```

### 当前行为说明

- Route 与状态查询接口共用 `resolveOptionalAuthContext()` 与 `parseInstallationId()` 逻辑（`backend/src/app/api/check-ins/route.ts:8-15`）。
- Service 会先读取最近签到记录，若当日还没有记录，则计算 `streak_day` 并调用 `repository.createIfAbsent(...)` 写入；如果今天已存在记录，则直接跳过写入（`backend/src/services/check-in/check-in.service.ts:50-68`）。
- `computeNextStreakDay()` 在以下场景返回 `1`：
  - 没有历史记录
  - 最近一次签到与今天不连续
  - 最近一条已是第 7 天，进入下一业务日（`backend/src/services/check-in/check-in.service.ts:92-109`）。
- 写入完成后会再次读取最近记录，并统一通过 `buildStatus()` 返回最新 7 日板，而不是仅返回“提交成功”标志（`backend/src/services/check-in/check-in.service.ts:66-68`）。
- 数据库层通过唯一索引约束同一主体在同一业务日只会有一条记录（`backend/supabase/migrations/20260729001000_create_signin_and_system_messages.sql:1-14`）。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 提交成功，或同日重复提交的幂等成功 |
| 400 | `VALIDATION_ERROR` | 登录态与安装标识都不可用，或安装标识非法 |
| 503 | `SERVICE_UNAVAILABLE` | 仓储或数据库不可用 |

---

## 数据结构补充

### `SignInDay`

| 字段 | 类型 | 说明 |
|------|------|------|
| `day` | number | 1~7 |
| `title` | string | 如 `第 1 天` |
| `reward_label` | string | 如 `金币 x10` |
| `status` | enum | `signed` / `today` / `locked` |

### `SignInStatus`

定义位于 `backend/src/lib/schemas.ts:103-123`，是 check-ins 资源的唯一响应结构。

---

## 与客户端实现的关系

- Android `ApiService.getCheckInStatus()` 与 `submitCheckIn()` 直接按该对象结构解码，并通过 `@Header("X-Installation-Id")` 透传安装标识（`android/app/src/main/java/com/djs66256/short_drama/core/network/ApiService.kt:134-142`）。
- iOS `CheckInRemoteDataSource` 使用 `CheckInEndpoints.makeHeaders(...)` 同时组装 `Authorization` 与 `X-Installation-Id`（`ios/ShortDrama/Sources/Data/DataSources/CheckInRemoteDataSource.swift:19-52`）。
- 两端都以 `server_date` 作为 dismissed state 的唯一日期依据，而不是本地设备日期（`android/app/src/main/java/com/djs66256/short_drama/core/storage/CheckInLocalStore.kt:40-54`、`ios/ShortDrama/Sources/Core/Storage/InstallationIdStore.swift:44-69`）。

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-29 | 初始创建：收录 PRD-10 `GET /api/check-ins/status` 与 `POST /api/check-ins` 的可选登录、安装标识、服务端业务日与幂等规则 |

---
*本文档由 llm-wiki skill 自动维护。*