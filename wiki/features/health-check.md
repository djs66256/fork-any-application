# 健康检查 (Health Check)

> 最后更新：2026-07-22

## 功能概述

后端健康检查接口，用于监控服务运行状态。返回服务状态、时间戳和版本信息。

- **覆盖端**：Backend
- **核心价值**：为运维监控、负载均衡健康探测、CI/CD 验证提供标准检查端点

## 入口与路由

- API 端点：`GET /api/health`
- 源文件：`backend/src/app/api/health/route.ts:1-14`
- 在 Backend 首页 (`/`) 中有链接指向该端点

## 核心逻辑

1. 构造响应数据：`{ status: 'ok', timestamp: ISO 时间戳, version: 配置中的版本号 }`
2. 使用 Zod Schema (`HealthResponseSchema`) 校验响应结构
3. 返回 JSON 格式的 `NextResponse`

流程：
```
GET /api/health → 构造 data 对象 → HealthResponseSchema.parse(data) → NextResponse.json(parsed)
```

## 多端实现

### Backend
- 源文件：`backend/src/app/api/health/route.ts:1-14`
- Zod Schema：`backend/src/lib/schemas.ts:3-8`（`HealthResponseSchema`）
- 配置：`backend/src/lib/config.ts:1-7`（版本号通过 `APP_VERSION` 环境变量注入）
- 响应格式：
  ```json
  {
    "status": "ok",
    "timestamp": "2026-07-22T...",
    "version": "0.1.0"
  }
  ```

## API 接口

| 方法 | 路径 | 说明 | 响应 |
|------|------|------|------|
| GET | `/api/health` | 健康检查 | `{ status: "ok", timestamp: string, version: string }` |

## 依赖关系

- 依赖 `@/lib/config`（配置模块）获取应用版本号
- 依赖 `@/lib/schemas`（HealthResponseSchema）做响应校验
- 使用 Next.js API Route 和 NextResponse

## 已知限制

- 当前仅检查服务进程是否存活，未检查数据库连接、外部服务依赖等深度健康指标
- 无鉴权机制，端点公开可访问
