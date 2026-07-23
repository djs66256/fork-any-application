# 测试规范 — Backend

> 本文档定义 Backend 端的测试策略、框架选型与编写规范。

---

## 1. 测试金字塔

<!-- TODO: 补充测试分层策略 -->

| 层级 | 框架 | 占比 | 目标 |
|------|------|------|------|
| Unit | Vitest | ~60% | 纯函数、工具逻辑 |
| Integration | Vitest + Supertest | ~30% | API 路由、数据库交互 |
| E2E | Playwright / 手动 | ~10% | 跨系统完整流程 |

---

## 2. 单元测试

### 2.1 Vitest

<!-- TODO: describe/it/expect、beforeAll/afterAll -->

### 2.2 Service 层测试

<!-- TODO: 业务逻辑独立测试、Mock Repository -->

### 2.3 工具函数测试

<!-- TODO: Zod schema、数据转换、格式化 -->

---

## 3. API 集成测试

### 3.1 Route Handler 测试

<!-- TODO: Next.js test helper、request/response -->

### 3.2 认证测试

<!-- TODO: Mock Auth、测试不同角色 -->

### 3.3 错误场景

<!-- TODO: 400/401/403/404/500 覆盖 -->

---

## 4. 数据库测试

### 4.1 测试数据库

<!-- TODO: 独立测试数据库、Supabase local -->

### 4.2 Seed 数据

<!-- TODO: 测试前的数据准备 -->

### 4.3 事务回滚

<!-- TODO: 测试间数据隔离 -->

### 4.4 Testcontainers

<!-- TODO: Docker PostgreSQL 测试环境 -->

---

## 5. 测试覆盖率

<!-- TODO: 补充覆盖率目标 -->

### 5.1 工具

<!-- TODO: Vitest coverage（v8）-->

### 5.2 最低覆盖率

<!-- TODO: 行覆盖率、分支覆盖率目标 -->
