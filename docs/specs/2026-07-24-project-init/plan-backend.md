# 实现计划：Backend — 项目初始化与架构设计

> 创建日期：2026-07-24
> 对应技术方案：design-backend.md
> 对应需求：spec.md

## 概述

从当前已初始化的 Next.js 16 项目骨架出发，构建完整的四层架构（Route → Service → Repository → Infrastructure + Shared），接入 Supabase 和 Redis 基础设施，提供 7 个 API 端点（含 health + 6 个业务骨架），并编写数据库 migration 和单元测试。实现后 Backend 可通过 `npm run dev` 启动，`GET /api/health` 返回服务及基础设施连通性状态。

## 测试场景列表

> 遵循轻量 TDD：先定义测试场景，再进入实现步骤。

| 编号 | 测试场景 | 输入 | 预期输出 | 类型 | 优先级 |
|------|---------|------|---------|------|--------|
| T-01 | config 模块导出完整配置 | 设置 `SUPABASE_URL`、`REDIS_URL` 等环境变量 | config.supabase.url / config.redis.url 返回对应值；缺失时使用默认值 | 单元测试 | P0 |
| T-02 | HealthResponseSchema 校验有效数据 | `{ status: "ok", version: "0.1.0", timestamp: "...", services: { database: "connected", redis: "connected" } }` | 解析成功，返回结构化对象 | 单元测试 | P0 |
| T-03 | DramaSchema 校验有效数据 | 合法 Drama 对象（12 字段全量） | 解析成功，snake_case 字段无报错 | 单元测试 | P1 |
| T-04 | DramaSchema 拒绝无效数据 | `{ title: "" }`（title 为空字符串） | ZodError，提示 title min(1) | 单元测试 | P1 |
| T-05 | AppError 工厂函数创建正确错误 | `Errors.notFound("Drama", "abc")` | `AppError { code: "NOT_FOUND", message: "Drama (abc) not found", statusCode: 404 }` | 单元测试 | P0 |
| T-06 | formatErrorResponse 序列化 AppError | 传入 `Errors.validationError("Bad input")` | `{ error: { code: "VALIDATION_ERROR", message: "Bad input" } }` | 单元测试 | P1 |
| T-07 | withErrorHandler 正常传递 handler 返回值 | handler 返回 `NextResponse.json({ ok: true })` | 返回 `{ ok: true }` + HTTP 200 | 单元测试 | P0 |
| T-08 | withErrorHandler 捕获 AppError 并格式化 | handler 内 `throw Errors.notFound("Drama", "x")` | `{ error: { code: "NOT_FOUND", message: "Drama (x) not found" } }` + HTTP 404 | 单元测试 | P0 |
| T-09 | withErrorHandler 捕获 ZodError | handler 内 `DramaSchema.parse({ title: "" })` | `{ error: { code: "VALIDATION_ERROR" } }` + HTTP 400 | 单元测试 | P1 |
| T-10 | DramaMockRepository 增删改查 | 多次调用 create / findById / findMany / delete | 数据一致性：增后能查、找到后能删、删后查不到 | 单元测试 | P0 |
| T-11 | HealthService.check 全部连接正常 | mock `checkSupabaseHealth=true`，`checkRedisHealth=true` | `{ status: "ok", services: { database: "connected", redis: "connected" } }` | 单元测试 | P0 |
| T-12 | HealthService.check 数据库断开 | mock `checkSupabaseHealth=false`，`checkRedisHealth=true` | `{ status: "degraded", services: { database: "disconnected", redis: "connected" } }` | 单元测试 | P0 |
| T-13 | GET /api/health 返回连接状态 | 发送 GET 请求（本地无 Docker 时） | HTTP 200，含 `status`、`version`、`timestamp`、`services` 字段 | 集成测试 | P0 |
| T-14 | GET /api/dramas 返回空列表 + 分页 | `GET /api/dramas?page=1&pageSize=10` | `{ data: [], pagination: { page: 1, page_size: 10, total: 0, total_pages: 0 } }` | 集成测试 | P0 |
| T-15 | 骨架端点返回 501 | GET/POST 任意未实现端点 | `{ error: { code: "NOT_IMPLEMENTED" } }` + HTTP 501 | 集成测试 | P0 |

## 实现步骤

### Step 1：项目基础配置 — 依赖、环境变量、Supabase CLI

- **关联测试**：T-01
- **目标文件**：`backend/package.json`、`backend/.env.example`、`backend/supabase/config.toml`、`backend/vitest.config.ts`
- **实现内容**：
  1. 在 `package.json` 中新增依赖：`@supabase/supabase-js`（^2.x）、`ioredis`（^5.x）、`vitest`（^2.x），并新增 `"test": "vitest run"` script
  2. 创建 `.env.example`，定义 `APP_NAME`、`APP_VERSION`、`PORT`、`SUPABASE_URL`、`SUPABASE_ANON_KEY`、`SUPABASE_SERVICE_ROLE_KEY`、`REDIS_URL`
  3. 执行 `npx supabase init` 生成 `supabase/config.toml`
  4. 创建 `vitest.config.ts`，配置 `@` alias 指向 `./src/*`
  5. 安装依赖：`npm install`
- **验证方式**：
  - ✅ 已完成：运行 `npm run test` 确认 vitest 可执行
  - ✅ 已完成：检查 `package.json` 中含 `@supabase/supabase-js`、`ioredis`、`vitest`
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/package.json` | 修改 | 新增 3 个依赖（supabase-js、ioredis、vitest）+ test script |
| `backend/.env.example` | 新增 | 7 个环境变量模板（APP_NAME 等） |
| `backend/vitest.config.ts` | 新增 | vitest 配置 + @ alias |
| `backend/supabase/config.toml` | 新增 | Supabase CLI 本地开发配置 |

### Step 2：Config + Shared 层 — 配置扩展、Schema、错误类型

- **关联测试**：T-01、T-02、T-03、T-04、T-05、T-06
- **目标文件**：`backend/src/lib/config.ts`、`backend/src/lib/schemas.ts`、`backend/src/lib/errors.ts`、`backend/src/lib/types.ts`
- **实现内容**：
  1. 扩展 `config.ts`：新增 `supabase`（url、anonKey、serviceRoleKey）和 `redis`（url）配置项，使用 `??` 提供本地开发默认值
  2. 扩展 `schemas.ts`：保留 `HealthResponseSchema` 并新增 `services` 字段（database、redis 连通状态）；新增 `DramaSchema`（12 字段，snake_case）、`EpisodeSchema`（10 字段）、`DramaListResponseSchema`、`PlayerStartRequestSchema`、`PlayerStopRequestSchema`、`UserProfileSchema`
  3. 新建 `errors.ts`：定义 `ErrorCode` 枚举（9 种错误码）、`AppError` 类（code + message + statusCode + details）、`Errors` 工厂对象（notFound / validationError / unauthorized / forbidden / conflict / tooManyRequests / internal / notImplemented / serviceUnavailable）、`formatErrorResponse` 函数
  4. 新建 `types.ts`：共享类型，如 `Nullable<T>`、`DeepPartial<T>` 等基础工具类型
  5. 编写 `src/lib/__tests__/config.test.ts`：验证 config 字段完整性、默认值
  6. 编写 `src/lib/__tests__/schemas.test.ts`：验证 Drama/Episode/HealthResponse schema 的有效/无效数据
  7. 编写 `src/lib/__tests__/errors.test.ts`：验证每个 Errors 工厂方法的 code、message、statusCode
- **验证方式**：
  - ✅ 已完成：运行 `npm run test` 确认 T-01～T-06 全部通过（33 tests passed）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/lib/config.ts` | 修改 | 新增 supabase + redis 配置项 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 Drama/Episode/Player/UserProfile Zod Schema |
| `backend/src/lib/errors.ts` | 新增 | ErrorCode 枚举 + AppError 类 + Errors 工厂 + formatErrorResponse |
| `backend/src/lib/types.ts` | 新增 | 共享基础工具类型 |
| `backend/src/lib/__tests__/config.test.ts` | 新增 | config 模块单元测试 |
| `backend/src/lib/__tests__/schemas.test.ts` | 新增 | Zod Schema 校验单元测试 |
| `backend/src/lib/__tests__/errors.test.ts` | 新增 | AppError 工厂方法单元测试 |

### Step 3：Infrastructure 层 — Supabase Client + Redis 客户端

- **关联测试**：T-11、T-12（HealthService 测试依赖本层）
- **目标文件**：`backend/src/infrastructure/supabase.ts`、`backend/src/infrastructure/redis.ts`
- **实现内容**：
  1. 新建 `src/infrastructure/supabase.ts`：实现 `getSupabaseClient()`（anon key，单例）、`getSupabaseAdmin()`（service role key，单例）、`checkSupabaseHealth()`（通过 `rpc('version')` 检测连通性）、`closeSupabase()` 释放
  2. 新建 `src/infrastructure/redis.ts`：实现 `getRedis()`（ioredis 单例，lazyConnect + 重试策略）、`checkRedisHealth()`（ping 检测）、`closeRedis()` 释放
  3. 新建 `src/infrastructure/__tests__/supabase.test.ts`：单元测试验证 client 创建逻辑（mock `createClient`）
  4. 新建 `src/infrastructure/__tests__/redis.test.ts`：单元测试验证 Redis 客户端初始化逻辑（mock ioredis）
- **验证方式**：
  - ✅ 已完成：运行 `npm run test` 确认 infrastructure 测试通过（12 tests passed）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/infrastructure/supabase.ts` | 新增 | Supabase Client 双实例（anon + service_role）+ 健康检查 |
| `backend/src/infrastructure/redis.ts` | 新增 | ioredis 客户端 + 健康检查 |
| `backend/src/infrastructure/__tests__/supabase.test.ts` | 新增 | Supabase client 单元测试 |
| `backend/src/infrastructure/__tests__/redis.test.ts` | 新增 | Redis client 单元测试 |

### Step 4：Middleware 层 — 错误处理、Auth 骨架、CORS、日志

- **关联测试**：T-07、T-08、T-09
- **目标文件**：`backend/src/middleware/` 下 4 个文件
- **实现内容**：
  1. 新建 `src/middleware/error-handler.ts`：实现 `withErrorHandler` wrapper 函数 — 正常返回透传、捕获 `AppError` 返回统一错误 JSON、捕获 `ZodError` 返回 400、未知错误返回 500
  2. 新建 `src/middleware/auth.ts`：实现 `requireAuth` wrapper 函数 — 验证 `Authorization: Bearer <token>` header，通过 `getSupabaseAdmin().auth.getUser(token)` 校验，校验失败返回 401，成功则将 `x-user-id` 注入 request header（骨架阶段不强制启用）
  3. 新建 `src/middleware/cors.ts`：实现 `withCors` wrapper — 设置 `Access-Control-Allow-Origin`、`Access-Control-Allow-Methods`、`Access-Control-Allow-Headers`，OPTIONS 预检返回 204
  4. 新建 `src/middleware/logger.ts`：实现 `withLogger` wrapper — 记录 `[method] [path] [duration]ms`
  5. 编写 `src/middleware/__tests__/error-handler.test.ts`：验证正常透传、AppError 捕获、ZodError 捕获、未知异常 500
- **验证方式**：
  - ✅ 已完成：运行 `npm run test -- src/middleware` 确认 T-07～T-09 通过（5 tests passed）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/middleware/error-handler.ts` | 新增 | withErrorHandler wrapper（AppError / ZodError / 通用异常） |
| `backend/src/middleware/auth.ts` | 新增 | requireAuth wrapper（Supabase JWT 验证，骨架） |
| `backend/src/middleware/cors.ts` | 新增 | withCors wrapper（跨域头 + OPTIONS 预检） |
| `backend/src/middleware/logger.ts` | 新增 | withLogger wrapper（method + path + duration） |
| `backend/src/middleware/__tests__/error-handler.test.ts` | 新增 | 错误处理中间件单元测试 |

### Step 5：Repository 层 — Interface 定义 + Mock 实现

- **关联测试**：T-10
- **目标文件**：`backend/src/repositories/interfaces/` 下 2 个 interface + `backend/src/repositories/mock/` 下 2 个 mock
- **实现内容**：
  1. 新建 `src/repositories/interfaces/drama.repository.interface.ts`：定义 `PaginationParams`、`PaginatedResult<T>`、`DramaRepositoryInterface`（findMany / findById / create / update / delete）
  2. 新建 `src/repositories/interfaces/episode.repository.interface.ts`：定义 `EpisodeRepositoryInterface`（findByDramaId / findById）
  3. 新建 `src/repositories/mock/drama.mock.repository.ts`：内存 `Map` 实现完整 CRUD
  4. 新建 `src/repositories/mock/episode.mock.repository.ts`：内存 `Map` 实现 `findByDramaId` / `findById`
  5. 编写 `src/repositories/__tests__/drama.mock.repository.test.ts`：验证增删改查全流程，含分页正确性
  6. 编写 `src/repositories/__tests__/episode.mock.repository.test.ts`：验证按 dramaId 查询 + 按 id 查询
- **验证方式**：
  - ✅ 已完成：运行 `npm run test -- src/repositories/__tests__` 确认 T-10 通过（14 tests passed）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 新增 | Drama Repository 接口 + 分页类型 |
| `backend/src/repositories/interfaces/episode.repository.interface.ts` | 新增 | Episode Repository 接口 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 新增 | Drama 内存 Mock 实现 |
| `backend/src/repositories/mock/episode.mock.repository.ts` | 新增 | Episode 内存 Mock 实现 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 新增 | Mock Repository CRUD 测试 |
| `backend/src/repositories/__tests__/episode.mock.repository.test.ts` | 新增 | Mock Repository 查询测试 |

### Step 6：Repository 层 — Supabase 实现

- **关联测试**：T-10（Mock 通过，Supabase 实现遵循相同 interface）
- **目标文件**：`backend/src/repositories/supabase/drama.supabase.repository.ts`、`backend/src/repositories/supabase/episode.supabase.repository.ts`
- **实现内容**：
  1. 新建 `src/repositories/supabase/drama.supabase.repository.ts`：实现 `DramaRepositoryInterface`，通过 `supabase.from('dramas').select/insert/update/delete` 操作数据，含分页（range + count），错误处理统一抛 `AppError`
  2. 新建 `src/repositories/supabase/episode.supabase.repository.ts`：实现 `EpisodeRepositoryInterface`，通过 `supabase.from('episodes')` 查询
  3. 新建 `src/repositories/supabase/__tests__/drama.supabase.repository.test.ts`：mock Supabase client 的 `from().select().range()` 链式调用，验证分页逻辑和错误处理路径（如 `PGRST116` → 返回 null）
  4. 新建 `src/repositories/supabase/__tests__/episode.supabase.repository.test.ts`：mock Supabase client，验证 episode 查询
- **验证方式**：
  - ✅ 已完成：运行 `npm run test -- src/repositories/supabase` 确认 Supabase 实现测试通过（8 tests passed）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 新增 | Drama Supabase CRUD 实现 |
| `backend/src/repositories/supabase/episode.supabase.repository.ts` | 新增 | Episode Supabase 查询实现 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 新增 | Supabase Repository 单元测试（mock client） |
| `backend/src/repositories/supabase/__tests__/episode.supabase.repository.test.ts` | 新增 | Supabase Repository 单元测试（mock client） |

### Step 7：Service 层 — 业务逻辑封装

- **关联测试**：T-11、T-12
- **目标文件**：`backend/src/services/` 下 4 个 service + 4 个 test
- **实现内容**：
  1. 新建 `src/services/health/health.service.ts`：`HealthService.check()` — 并行调用 `checkSupabaseHealth()` + `checkRedisHealth()`，返回 `HealthStatus`
  2. 新建 `src/services/drama/drama.service.ts`：`DramaService` — 构造函数注入 `DramaRepositoryInterface`；`listDramas()` 返回空列表（骨架），`getDramaById()` / `createDrama()` 抛出 `Errors.notImplemented()`
  3. 新建 `src/services/episode/episode.service.ts`：`EpisodeService` — 构造函数注入 `EpisodeRepositoryInterface`；`getEpisodeById()` 抛出 `Errors.notImplemented()`
  4. 新建 `src/services/player/player.service.ts`：`PlayerService` — 构造函数注入 Repository；`startPlayback()` / `stopPlayback()` 抛出 `Errors.notImplemented()`
  5. 编写 `src/services/health/health.service.test.ts`：mock infrastructure 层的 check 函数，验证 ok / degraded 状态
  6. 编写 `src/services/drama/drama.service.test.ts`：注入 `DramaMockRepository`，验证 `listDramas` 返回空列表，验证骨架方法抛出 `Errors.notImplemented`
- **验证方式**：
  - ✅ 已完成：运行 `npm run test -- src/services` 确认 T-11、T-12 通过（11 tests passed）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/services/health/health.service.ts` | 新增 | HealthService（DB + Redis 连通性检查） |
| `backend/src/services/health/health.service.test.ts` | 新增 | HealthService 单元测试 |
| `backend/src/services/drama/drama.service.ts` | 新增 | DramaService 骨架（listDramas + 501 方法） |
| `backend/src/services/drama/drama.service.test.ts` | 新增 | DramaService 单元测试（注入 Mock Repository） |
| `backend/src/services/episode/episode.service.ts` | 新增 | EpisodeService 骨架 |
| `backend/src/services/episode/episode.service.test.ts` | 新增 | EpisodeService 单元测试 |
| `backend/src/services/player/player.service.ts` | 新增 | PlayerService 骨架 |
| `backend/src/services/player/player.service.test.ts` | 新增 | PlayerService 单元测试 |

### Step 8：Route 层 — API 端点 + 管理首页

- **关联测试**：T-13、T-14、T-15
- **目标文件**：`backend/src/app/api/` 下 7 个 route.ts + `backend/src/app/page.tsx`
- **实现内容**：
  1. 重构 `src/app/api/health/route.ts`：改为导入 `HealthService`，调用 `service.check()` 返回完整 health 响应（含 `services.database` / `services.redis` + `timestamp`）；包裹 `withErrorHandler`
  2. 新建 `src/app/api/dramas/route.ts`：`GET` 返回空列表 + 分页元数据；`POST` 返回 501 骨架
  3. 新建 `src/app/api/dramas/[id]/route.ts`：`GET` 返回 501 骨架
  4. 新建 `src/app/api/episodes/[id]/route.ts`：`GET` 返回 501 骨架
  5. 新建 `src/app/api/player/start/route.ts`：`POST` 返回 501 骨架
  6. 新建 `src/app/api/player/stop/route.ts`：`POST` 返回 501 骨架
  7. 更新 `src/app/page.tsx`：展示 Supabase 和 Redis 连接状态（通过 `/api/health` 数据渲染）
  8. 编写 `src/app/api/__tests__/health.test.ts`：发送 GET，验证 200 + 响应结构
  9. 编写 `src/app/api/__tests__/dramas.test.ts`：验证 GET 空列表分页 + POST 501
  10. 编写 `src/app/api/__tests__/skeleton-endpoints.test.ts`：验证其余 4 个骨架端点返回 501
- **验证方式**：
  - ✅ 已完成：运行 `npm run test` 确认 T-13～T-15 通过（9 tests passed）
  - 运行 `npm run dev`，手动访问 `http://localhost:3001/api/health` 确认 JSON 响应正确
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/src/app/api/health/route.ts` | 修改 | 重构为 HealthService 调用 + services 状态 |
| `backend/src/app/api/dramas/route.ts` | 新增 | GET（空列表+分页）+ POST（501） |
| `backend/src/app/api/dramas/[id]/route.ts` | 新增 | GET 501 骨架 |
| `backend/src/app/api/episodes/[id]/route.ts` | 新增 | GET 501 骨架 |
| `backend/src/app/api/player/start/route.ts` | 新增 | POST 501 骨架 |
| `backend/src/app/api/player/stop/route.ts` | 新增 | POST 501 骨架 |
| `backend/src/app/page.tsx` | 修改 | 展示 Supabase + Redis 连接状态 |
| `backend/src/app/api/__tests__/health.test.ts` | 新增 | health 端点集成测试 |
| `backend/src/app/api/__tests__/dramas.test.ts` | 新增 | dramas 端点集成测试 |
| `backend/src/app/api/__tests__/skeleton-endpoints.test.ts` | 新增 | 骨架端点 501 测试 |

### Step 9：数据库 Migration + Docker Compose + CLAUDE.md 更新

- **关联测试**：无独立测试（migration 为 SQL，CLAUDE.md 为文档）
- **目标文件**：`backend/supabase/migrations/00000000000001_init_tables.sql`、`backend/docker-compose.yml`、`backend/CLAUDE.md`
- **实现内容**：
  1. 新建 `supabase/migrations/00000000000001_init_tables.sql`：创建 `dramas` 表（12 列含 CHECK 约束）、`episodes` 表（10 列含外键 + UNIQUE）、`profiles` 表（关联 auth.users）；创建 6 个索引；创建 `update_updated_at_column` 触发器 + 3 个表级触发器
  2. 新建 `docker-compose.yml`：定义 Redis 7 服务（端口 6379）和 Supabase 服务配置说明（引导使用 `supabase start`）
  3. 更新 `backend/CLAUDE.md`：补充 Supabase 开发规范（migration 管理方式、RLS 策略、环境变量约定）、四层架构目录说明、测试命令
  4. 编写 `src/db/seed.ts`：种子数据脚本骨架（含 `insertSampleData()` 空函数 + TODO 注释）
- **验证方式**：
  - ✅ 已完成：检查 `supabase/migrations/` 目录含 SQL 文件
  - ✅ 已完成：检查 `backend/CLAUDE.md` 含 Supabase 规范章节
  - ⬜ 待验证：执行 `docker compose up -d`，确认 Redis 容器启动成功（需要 Docker 运行）
- **变更文件**：

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/supabase/migrations/00000000000001_init_tables.sql` | 新增 | dramas + episodes + profiles 建表 + 索引 + 触发器 |
| `backend/docker-compose.yml` | 新增 | Redis 服务 + Supabase 使用指引 |
| `backend/src/db/seed.ts` | 新增 | 种子数据脚本骨架 |
| `backend/CLAUDE.md` | 修改 | 补充 Supabase 规范 + 四层架构 + 测试命令 |

## 依赖关系

```
Step 1 (项目基础配置) ──▶ Step 2 (Config + Shared 层)
                              │
                              ▼
                        Step 3 (Infrastructure 层)
                              │
                              ▼
                        Step 4 (Middleware 层)
                              │
                              ▼
                        Step 5 (Repository Interface + Mock)
                              │
                              ├──────────────▶ Step 6 (Repository Supabase 实现)
                              │                        │
                              ▼                        ▼
                        Step 7 (Service 层) ◀──────────┘
                              │
                              ▼
                        Step 8 (Route 层)
                              │
                              ▼
                        Step 9 (Migration + Docker + CLAUDE.md)
```

- Step 3 依赖 Step 2（infrastructure 引用 config）
- Step 4 依赖 Step 2（middleware 引用 errors、schemas）
- Step 5 依赖 Step 2（Repository interface 引用 schemas types）
- Step 6 依赖 Step 3 + Step 5（Supabase 实现依赖 infrastructure supabase client + interface 定义）
- Step 7 依赖 Step 3 + Step 5（Service 依赖 infrastructure 健康检查 + Repository interface）
- Step 8 依赖 Step 4 + Step 7（Route 依赖 middleware wrapper + Service）
- Step 9 可与 Step 8 并行（migration 和 docker compose 不依赖 route 实现）

## 验证总览

- [ ] 所有单元测试 + 集成测试通过（`npm run test`）
- [ ] Build 成功（`npm run build`）
- [ ] Lint 无新增错误（`npm run lint`）
- [ ] `npm run dev` 启动成功，`GET http://localhost:3001/api/health` 返回 200
- [ ] `GET http://localhost:3001/api/dramas` 返回空列表 + 分页元数据
- [ ] 其余 5 个骨架端点均返回 501
- [ ] `docker compose up -d` Redis 启动成功
- [ ] 管理首页 `/` 正确展示应用名、版本号、API 链接

## 变更文件汇总

| 文件 | 操作 | 内容简介 |
|------|------|---------|
| `backend/package.json` | 修改 | 新增 3 个依赖（@supabase/supabase-js、ioredis、vitest）+ test script |
| `backend/.env.example` | 新增 | 7 个环境变量模板 |
| `backend/vitest.config.ts` | 新增 | Vitest 配置 + @ alias |
| `backend/docker-compose.yml` | 新增 | Redis 7 服务 + Supabase 使用指引 |
| `backend/supabase/config.toml` | 新增 | Supabase CLI 本地开发配置 |
| `backend/supabase/migrations/00000000000001_init_tables.sql` | 新增 | dramas + episodes + profiles 建表 SQL |
| `backend/src/lib/config.ts` | 修改 | 新增 supabase + redis 配置项 |
| `backend/src/lib/schemas.ts` | 修改 | 新增 Drama/Episode/Player/UserProfile Zod Schema |
| `backend/src/lib/errors.ts` | 新增 | ErrorCode + AppError + Errors 工厂 + formatErrorResponse |
| `backend/src/lib/types.ts` | 新增 | 共享基础工具类型 |
| `backend/src/lib/__tests__/config.test.ts` | 新增 | config 模块单元测试 |
| `backend/src/lib/__tests__/schemas.test.ts` | 新增 | Schema 校验单元测试 |
| `backend/src/lib/__tests__/errors.test.ts` | 新增 | AppError 工厂单元测试 |
| `backend/src/infrastructure/supabase.ts` | 新增 | Supabase Client 双实例 + 健康检查 |
| `backend/src/infrastructure/redis.ts` | 新增 | ioredis 客户端 + 健康检查 |
| `backend/src/infrastructure/__tests__/supabase.test.ts` | 新增 | Supabase client 单元测试 |
| `backend/src/infrastructure/__tests__/redis.test.ts` | 新增 | Redis client 单元测试 |
| `backend/src/middleware/error-handler.ts` | 新增 | withErrorHandler wrapper |
| `backend/src/middleware/auth.ts` | 新增 | requireAuth wrapper（骨架） |
| `backend/src/middleware/cors.ts` | 新增 | withCors wrapper |
| `backend/src/middleware/logger.ts` | 新增 | withLogger wrapper |
| `backend/src/middleware/__tests__/error-handler.test.ts` | 新增 | 错误处理中间件单元测试 |
| `backend/src/repositories/interfaces/drama.repository.interface.ts` | 新增 | Drama Repository 接口 |
| `backend/src/repositories/interfaces/episode.repository.interface.ts` | 新增 | Episode Repository 接口 |
| `backend/src/repositories/mock/drama.mock.repository.ts` | 新增 | Drama 内存 Mock 实现 |
| `backend/src/repositories/mock/episode.mock.repository.ts` | 新增 | Episode 内存 Mock 实现 |
| `backend/src/repositories/__tests__/drama.mock.repository.test.ts` | 新增 | Mock Repository CRUD 测试 |
| `backend/src/repositories/__tests__/episode.mock.repository.test.ts` | 新增 | Mock Repository 查询测试 |
| `backend/src/repositories/supabase/drama.supabase.repository.ts` | 新增 | Drama Supabase CRUD 实现 |
| `backend/src/repositories/supabase/episode.supabase.repository.ts` | 新增 | Episode Supabase 查询实现 |
| `backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts` | 新增 | Supabase Repository 单元测试 |
| `backend/src/repositories/supabase/__tests__/episode.supabase.repository.test.ts` | 新增 | Supabase Repository 单元测试 |
| `backend/src/services/health/health.service.ts` | 新增 | HealthService |
| `backend/src/services/health/health.service.test.ts` | 新增 | HealthService 单元测试 |
| `backend/src/services/drama/drama.service.ts` | 新增 | DramaService 骨架 |
| `backend/src/services/drama/drama.service.test.ts` | 新增 | DramaService 单元测试 |
| `backend/src/services/episode/episode.service.ts` | 新增 | EpisodeService 骨架 |
| `backend/src/services/episode/episode.service.test.ts` | 新增 | EpisodeService 单元测试 |
| `backend/src/services/player/player.service.ts` | 新增 | PlayerService 骨架 |
| `backend/src/services/player/player.service.test.ts` | 新增 | PlayerService 单元测试 |
| `backend/src/app/api/health/route.ts` | 修改 | 重构为 HealthService + services 连通性 |
| `backend/src/app/api/dramas/route.ts` | 新增 | GET（空列表）+ POST（501） |
| `backend/src/app/api/dramas/[id]/route.ts` | 新增 | GET 501 骨架 |
| `backend/src/app/api/episodes/[id]/route.ts` | 新增 | GET 501 骨架 |
| `backend/src/app/api/player/start/route.ts` | 新增 | POST 501 骨架 |
| `backend/src/app/api/player/stop/route.ts` | 新增 | POST 501 骨架 |
| `backend/src/app/page.tsx` | 修改 | 展示 Supabase + Redis 连接状态 |
| `backend/src/app/api/__tests__/health.test.ts` | 新增 | health 端点集成测试 |
| `backend/src/app/api/__tests__/dramas.test.ts` | 新增 | dramas 端点集成测试 |
| `backend/src/app/api/__tests__/skeleton-endpoints.test.ts` | 新增 | 骨架端点 501 测试 |
| `backend/src/db/seed.ts` | 新增 | 种子数据脚本骨架 |
| `backend/CLAUDE.md` | 修改 | 补充 Supabase 规范 + 四层架构 + 测试命令 |

总计：**31 个新增文件** + **5 个修改文件**
