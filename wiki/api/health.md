# 健康检查 API 文档

> 最后更新：2026-07-22

---

## GET /api/health

### 功能简介

后端健康检查接口，用于监控服务运行状态。返回服务状态、时间戳和版本信息。

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
  "timestamp": "2026-07-22T00:00:00.000Z",
  "version": "0.1.0"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | string (literal `"ok"`) | 服务状态，固定为 `"ok"` |
| `timestamp` | string | 响应生成时间（ISO 8601 格式） |
| `version` | string | 应用版本号，来自 `APP_VERSION` 环境变量 |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |

当前接口无错误分支，始终返回 200。

### 实现说明

响应数据在构造后通过 `HealthResponseSchema`（`backend/src/lib/schemas.ts:L3-8`）校验：

```typescript
// backend/src/lib/schemas.ts:L3-8
export const HealthResponseSchema = z.object({
  status: z.literal('ok'),
  timestamp: z.string(),
  version: z.string(),
});
```

版本号来自配置模块（`backend/src/lib/config.ts:L1-7`），通过 `APP_VERSION` 环境变量注入，默认为 `'0.1.0'`。

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-22 | 从后端代码提取，初始创建 |

---

*本文档由 llm-wiki skill 自动维护。*
