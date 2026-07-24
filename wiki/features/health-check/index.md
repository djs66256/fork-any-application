# 健康检查 (Health Check)

> 最后更新：2026-07-24

## 功能概述

后端健康检查接口，用于监控服务运行状态。检查 Supabase PostgreSQL 数据库和 Redis 缓存服务的连通性，返回整体状态、版本号和各项基础设施的连接状态。

- **覆盖端**：Backend
- **核心价值**：为运维监控、负载均衡健康探测、CI/CD 验证提供标准检查端点；支持优雅降级（基础设施不可用时仍返回 200）

## 入口与路由

- API 端点：`GET /api/health`
- 源文件：`backend/src/app/api/health/route.ts:5`
- 在 Backend 首页（`/`）中有链接指向该端点

## 核心逻辑

1. Route handler（包裹 `withErrorHandler`）创建 `HealthService` 实例
2. `HealthService.check()` 使用 `Promise.all` 并行检查 Supabase DB 和 Redis 连通性
3. Supabase 健康检查：通过 `client.rpc('version')` 探测数据库连接
4. Redis 健康检查：通过 `redis.ping()` 探测 Redis 连接
5. 根据两服务状态计算整体 status：全通 = `"ok"`，部分不通 = `"degraded"`
6. 返回 JSON 响应（200 OK，即使基础设施不可用也返回 200）

流程：
```
GET /api/health → HealthService.check() → Promise.all([
  checkSupabaseHealth() → supabase.rpc('version'),
  checkRedisHealth() → redis.ping()
]) → NextResponse.json({ status, version, timestamp, services })
```

## 多端实现

### Backend

- 源文件：`backend/src/app/api/health/route.ts:1-9`
- Service 层：`backend/src/services/health/health.service.ts:1` — 封装连通性检查逻辑
- Infrastructure 层：
  - `backend/src/infrastructure/supabase.ts:1` — Supabase Client 双实例 + `checkSupabaseHealth()`
  - `backend/src/infrastructure/redis.ts:1` — ioredis 客户端 + `checkRedisHealth()`
- Zod Schema：`backend/src/lib/schemas.ts:3-13`（`HealthResponseSchema`，status 为 `ok/degraded/error`）
- 配置：`backend/src/lib/config.ts`（版本号通过 `APP_VERSION` 环境变量注入）
- 响应格式：
  ```json
  {
    "status": "ok",
    "version": "0.1.0",
    "timestamp": "2026-07-24T...",
    "services": {
      "database": "connected",
      "redis": "connected"
    }
  }
  ```
- 错误处理：通过 `withErrorHandler` wrapper 统一捕获异常，参考 `backend/src/middleware/error-handler.ts:1`

## API 引用

| 接口 | API 文档 | 说明 |
|------|---------|------|
| `GET /api/health` | [api/health.md](../../api/health.md) | 健康检查，返回服务状态、版本号及基础设施连通性 |

## 依赖关系

- 依赖 `@/services/health/health.service`（HealthService）
- 依赖 `@/infrastructure/supabase`（checkSupabaseHealth）
- 依赖 `@/infrastructure/redis`（checkRedisHealth）
- 依赖 `@/lib/config`（应用版本号）
- 依赖 `@/middleware/error-handler`（withErrorHandler 统一错误处理）
- 外部依赖：`@supabase/supabase-js`、`ioredis`

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 扩展：新增 Supabase DB 和 Redis 连通性检查，status 字段扩展为 ok/degraded/error，新增 services 字段 |
| 2026-07-22 | 从后端代码提取，初始创建（仅检查进程存活） |

---

*本文档由 llm-wiki skill 自动维护。*
