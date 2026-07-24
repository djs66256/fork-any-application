# Code Review：Backend — project-init

> 审查日期：2026-07-24
> 审查范围：backend/ 下所有变更文件（31 新增 + 5 修改）
> 审查轮次：第 1 轮（首轮编码）

## 审查维度与结论

### 维度一：通用-代码规范（硬编码 + 代码风格）

| 审查项 | 结果 |
|--------|------|
| URL/API 地址硬编码 | 无问题。所有 URL 通过 config 模块获取，使用 `??` 提供默认值。 |
| Token/密钥硬编码 | 无问题。SUPABASE_URL、ANON_KEY、SERVICE_ROLE_KEY 均通过 `process.env` 获取。 |
| 环境相关常量硬编码 | 已修复。发现 `config.ts` 中 `APP_NAME` 默认值为 `'ShortDrama Backend'`（嵌入产品名），将其改为 `'Backend'`；`docker-compose.yml` 中 container_name 从 `shortdrama-redis` 改为 `app-redis`。 |
| 配置值硬编码 | 无问题。超时、重试等配置均内聚在相关模块中，CORS 的 `*` 和 `86400` 作为合理的中间件默认值，在骨架阶段可接受。 |
| 命名规范 | 无问题。变量/函数/类/文件命名符合 TypeScript 规范，snake_case 用于数据库列映射。 |
| 缩进和格式 | 无问题。统一 2 空格缩进，ESLint 配置一致。 |
| 注释清晰度 | 无问题。关键模块均有 JSDoc 注释（auth.ts、seed.ts、migration 等）。 |
| 函数复杂度 | 无问题。各函数职责单一，嵌套不超过 3 层。 |

### 维度二：通用-设计与 API 一致性

| 审查项 | 结果 |
|--------|------|
| 组件/模块拆分 | 与 design 一致。四层架构完整：Route → Service → Repository → Infrastructure + Shared。 |
| 数据流 | 与 design 一致。Health 端点：Route → HealthService → checkSupabaseHealth/checkRedisHealth。Dramas 端点：Route → DramaService → DramaRepository。 |
| 接口调用 | 与 design 一致。所有 endpoint 均包裹 `withErrorHandler`，预期后续叠加 `withCors`/`withLogger`/`requireAuth`。 |
| API 路径 | 与 design 一致。`/api/health`、`/api/dramas`、`/api/dramas/[id]`、`/api/episodes/[id]`、`/api/player/start`、`/api/player/stop` 共 6 个业务端点。 |
| HTTP 方法 | 无问题。GET/POST 使用正确。 |

### 维度三：通用-代码质量（错误处理 + 性能 + 测试）

| 审查项 | 结果 |
|--------|------|
| 错误处理 | 无问题。统一使用 AppError + ErrorCode 枚举，withErrorHandler 统一捕获 AppError/ZodError/Unknown。 |
| 边界条件 | 无问题。Repository 层处理 PGRST116（Not Found）、23505（Conflict），分页含空数据场景。 |
| 吞异常 | 无问题。health check 的 try/catch 返回 false 而非吞异常，这是符合设计意图的降级策略。 |
| 性能 | 无问题。HealthService 并行检查 DB+Redis（Promise.all），Supabase client 单例模式。 |
| 内存泄漏 | 无问题。Repository 使用 `closeSupabase()`/`closeRedis()` 提供显式释放。 |
| 测试通过 | 92/92 tests pass。 |
| 测试覆盖 | Repository Interface + Mock、Supabase 实现、Service 层、Middleware、Route 层、Config/Schema/Errors 共享层均有测试。关键场景覆盖：正常流程、边界条件（空列表、null 查询）、错误路径（PGRST116、冲突、未实现）、分页正确性。 |

### 维度四：Backend-API 标准规范

| 审查项 | 结果 |
|--------|------|
| RESTful 路径 | 无问题。资源路径使用名词复数（dramas、episodes），小写+连字符（player/start、player/stop 为动作端点，可接受）。 |
| 响应格式 | 无问题。统一 `{ error: { code, message } }` 错误格式，`/api/dramas` 使用 `{ data, pagination }` 结构。 |
| 分页规范 | 无问题。page/page_size/total/total_pages 字段命名一致。 |
| 参数校验 | 已改进。修复前 `/api/dramas` 使用 `parseInt` 裸解析（NaN/负数可绕过），已改为 Zod `PaginationQuerySchema`（coerce + min/max）。其余骨架端点暂未接受请求体参数。 |

### 维度五：Backend-数据库与测试质量

| 审查项 | 结果 |
|--------|------|
| SQL 注入 | 无问题。所有数据库操作通过 Supabase JS Client 的参数化查询实现。 |
| 索引设计 | 无问题。migration 含 6 个索引（status、category、created_at、drama_id、drama_id+episode_number、email）。 |
| RLS 策略 | 无问题。三表均启用 RLS，开发阶段使用宽松策略（authenticated 全权限）。 |
| N+1 查询 | 无问题。骨架阶段无复杂关联查询，Episode 查询通过 `eq('drama_id')` 单次获取。 |
| Repository 分离 | 无问题。Interface 定义 + Mock 实现 + Supabase 实现，依赖注入到 Service 层。 |
| 测试 mock | 无问题。所有测试 mock 外部依赖（Supabase client、ioredis、config），可独立运行。 |

## 发现与修复摘要

### 审查中发现的问题（已全部修复）

| # | 严重度 | 文件 | 问题 | 修复方案 |
|---|--------|------|------|----------|
| 1 | high | `backend/src/lib/config.ts:3` | APP_NAME 默认值 `'ShortDrama Backend'` 包含产品名 | 改为 `'Backend'` |
| 2 | high | `backend/docker-compose.yml:6` | container_name `shortdrama-redis` 包含产品名 | 改为 `app-redis` |
| 3 | medium | `backend/src/app/api/dramas/route.ts:8-9` | 分页参数使用 `parseInt` 裸解析，缺少校验 | 改为 Zod PaginationQuerySchema（coerce + min/max） |
| 4 | low | `backend/src/app/page.tsx:10` | 混用 `<a>` 和 `<Link>` 组件 | 统一改为 `<Link>` |
| 5 | low | `backend/.env.example:2` | APP_NAME 示例值包含产品名 | 改为 `Backend` |

### 后续建议（非阻塞，可在迭代中改进）

1. CORS 中间件的 `Allow-Origin: *` 和 `Max-Age: 86400` 在骨架阶段可接受，后续应通过 config 模块配置。
2. DramaMockRepository 在路由中每次请求新建实例（`new DramaMockRepository()`），后续应改为真正 DI 或 Supabase 实现。
3. Auth 中间件当前为骨架（接受任意 Bearer token），后续需接入 Supabase JWT 验证。

## 验证结果

- [x] Build 成功（`npm run build` exit 0）
- [x] Lint 无新增错误（0 errors, 10 warnings -- 均为 skeleton 方法 `_` 前缀参数）
- [x] 所有测试通过（92/92 pass）
- [x] 审查发现问题已全部修复
- [x] 四层架构完整符合 plan-backend.md 设计
- [x] 无产品名硬编码残留
