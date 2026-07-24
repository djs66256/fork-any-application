# 健康检查 API 文档

> 最后更新：2026-07-24

---

## GET /api/health

### 功能简介

后端健康检查接口，用于监控服务运行状态。返回服务整体状态、时间戳、版本号以及 Supabase 数据库和 Redis 的连通性状态。

### 代码文件路径

`backend/src/app/api/health/route.ts:L5`

### path / method

`GET /api/health`

### Headers

无必填 Header。

### Query 参数

无。

### Body 参数

无。

### Response

```json
{
  "status": "ok",
  "version": "0.1.0",
  "timestamp": "2026-07-24T00:00:00.000Z",
  "services": {
    "database": "connected",
    "redis": "connected"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | string | 服务整体状态：`"ok"` \| `"degraded"` \| `"error"` |
| `version` | string | 应用版本号，来自 `APP_VERSION` 环境变量 |
| `timestamp` | string | 响应生成时间（ISO 8601 格式） |
| `services.database` | string | Supabase PostgreSQL 连接状态：`"connected"` \| `"disconnected"` \| `"unknown"` |
| `services.redis` | string | Redis 连接状态：`"connected"` \| `"disconnected"` \| `"unknown"` |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（即使基础设施不可用也返回 200，status 字段区分降级/错误） |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

### 实现说明

响应通过 `HealthService.check()`（`backend/src/services/health/health.service.ts:L1`）构造，使用 `Promise.all` 并行检查 Supabase DB 和 Redis 连通性：

- Supabase 健康检查：通过 `supabase.rpc('version')` 检测连通性（`backend/src/infrastructure/supabase.ts:L38`）
- Redis 健康检查：通过 `redis.ping()` 检测连通性（`backend/src/infrastructure/redis.ts:L15`）
- 所有基础设施不可用时仍返回 200，status 降级为 `"degraded"`，实现优雅降级

Route handler 使用 `withErrorHandler` wrapper 统一错误处理。

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 扩展：新增 `services.database` 和 `services.redis` 字段，status 从 literal `"ok"` 扩展为 `"ok"/"degraded"/"error"`，新增 Supabase DB + Redis 连通性检查 |
| 2026-07-22 | 从后端代码提取，初始创建 |

---

*本文档由 llm-wiki skill 自动维护。*
