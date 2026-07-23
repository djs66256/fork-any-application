# 架构设计 — Backend

> 本文档定义 Backend 端的整体架构设计规范。

---

## 1. 整体架构

<!-- TODO: 补充架构图与分层说明 -->

```
Route Handler (API Layer)
    ↓
Service (Business Logic)
    ↓
Repository (Data Access)
    ↓
Database (Supabase / PostgreSQL)
```

<!-- TODO: 各层职责详细说明 -->

---

## 2. API 设计规范

<!-- TODO: 补充 RESTful API 设计规范 -->

### 2.1 路由设计

<!-- TODO: 资源命名、层级关系、集合 vs 单资源 -->

### 2.2 请求格式

<!-- TODO: Query 参数、Path 参数、Request Body -->

### 2.3 响应格式

<!-- TODO: 统一 { data, error, meta } 响应结构 -->

### 2.4 状态码

<!-- TODO: 200/201/204/400/401/403/404/409/422/500 -->

### 2.5 错误码体系

<!-- TODO: 业务错误码枚举、国际化错误信息 -->

### 2.6 分页

<!-- TODO: 基于 cursor 的分页、page/limit 分页 -->

### 2.7 版本管理

<!-- TODO: API 版本策略（URL 前缀 / Header）-->

---

## 3. 认证与授权

<!-- TODO: 补充 Auth 方案 -->

### 3.1 Supabase Auth

<!-- TODO: 用户注册/登录、Session 管理、MFA -->

### 3.2 Middleware 鉴权

<!-- TODO: Next.js middleware 验证 Token、路由保护 -->

### 3.3 RBAC

<!-- TODO: 角色定义、权限检查、Row Level Security -->

---

## 4. 数据库设计

<!-- TODO: 补充数据库设计规范 -->

### 4.1 Schema 设计

<!-- TODO: 范式、字段类型选择、默认值、约束 -->

### 4.2 索引策略

<!-- TODO: 索引类型、复合索引、部分索引 -->

### 4.3 迁移管理

<!-- TODO: Supabase Migration、版本化、回滚策略 -->

### 4.4 Row Level Security (RLS)

<!-- TODO: RLS Policy 设计、测试 -->

---

## 5. 缓存策略

<!-- TODO: 补充缓存方案 -->

### 5.1 应用缓存

<!-- TODO: Redis / Upstash、缓存 Key 设计 -->

### 5.2 HTTP 缓存

<!-- TODO: Cache-Control、ETag、Next.js fetch cache -->

### 5.3 数据库缓存

<!-- TODO: 物化视图、查询结果缓存 -->

### 5.4 缓存失效

<!-- TODO: TTL、主动失效、Cache-Aside 模式 -->

---

## 6. 错误处理

<!-- TODO: 补充错误处理策略 -->

### 6.1 错误分类

<!-- TODO: 输入错误、业务错误、系统错误、第三方错误 -->

### 6.2 全局错误处理

<!-- TODO: error boundary、统一错误响应 -->

### 6.3 日志与追踪

<!-- TODO: 错误上下文、Trace ID 注入 -->
