## 端说明

Backend 端代码与说明统一维护在当前目录。
如无额外说明，当前目录仅承载服务端相关实现，不处理前端或移动端界面逻辑。

## 技术约束

- 使用 TypeScript、Next.js、Supabase、Zod。
- 数据结构、请求参数、响应数据优先使用 Zod 做校验与约束。
- 如接入 Supabase，需保持数据访问逻辑与接口逻辑的职责边界清晰。

## 架构约束

- 服务端代码使用 TypeScript 开发，避免回退到无类型或弱类型实现。
- 接口层（Route）、业务逻辑层（Service）、数据访问层（Repository）、基础设施层（Infrastructure）应严格分层。
- 共享层（Shared）位于 `src/lib/`，提供跨层复用的 Schema、Error、Config、Types。
- API 设计遵循 RESTful 风格。
- 优先保证核心接口行为、参数校验与数据转换逻辑具备良好的可测试性。

## 四层架构

```
src/
├── lib/                    # Shared layer: config, schemas, errors, types
├── infrastructure/         # Infrastructure: Supabase client, Redis client
├── middleware/              # Middleware: error handler, auth, CORS, logger
├── repositories/
│   ├── interfaces/         # Repository interfaces
│   ├── mock/               # In-memory mock implementations (for testing)
│   └── supabase/           # Supabase implementations
├── services/               # Business logic layer
│   ├── health/
│   ├── drama/
│   ├── episode/
│   └── player/
└── app/
    └── api/                # Route layer (Next.js App Router)
        ├── health/
        ├── dramas/
        ├── episodes/
        └── player/
```

分层依赖关系：Route → Service → Repository → Infrastructure + Shared

## Supabase 开发规范

### Migration 管理

- SQL migration 文件放在 `supabase/migrations/` 目录。
- 文件命名格式：`<timestamp>_<description>.sql`（如 `00000000000001_init_tables.sql`）。
- 使用 `npx supabase db push` 应用 migration 到本地 Supabase 实例。
- 不要手动修改 `supabase/migrations/` 中已执行的 migration 文件，始终新建 migration。

### RLS 策略

- 所有表启用 Row Level Security（RLS）。
- 开发阶段使用宽松策略（authenticated 用户可读可写）。
- 生产环境需收紧策略（如：用户只能更新自己的 profile，dramas 只读）。

### 环境变量约定

| 变量 | 用途 | 必需 |
|------|------|------|
| `APP_NAME` | 应用名称 | 否 |
| `APP_VERSION` | 应用版本 | 否 |
| `PORT` | 服务端口 | 否 |
| `SUPABASE_URL` | Supabase 项目 URL | 是 |
| `SUPABASE_ANON_KEY` | Supabase 匿名密钥 | 是 |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase 服务角色密钥（仅 admin 操作） | 是 |
| `REDIS_URL` | Redis 连接地址 | 否 |

### Client 双实例策略

- `getSupabaseClient()` — 使用 anon key，遵循 RLS 策略，用于用户端操作。
- `getSupabaseAdmin()` — 使用 service role key，绕过 RLS，用于服务端管理操作（如 auth 验证、health check）。

## 测试要求

- 需要编写单元测试。
- 涉及业务逻辑、参数校验、数据转换的改动，应同步补齐对应测试。
- 新增接口场景时，应优先保证核心行为可通过自动化测试验证。
- Repository 测试使用 mock client（不依赖真实 Supabase/Redis 连接）。
- Service 测试通过依赖注入 mock repository。

## 命令约定

- **安装依赖**：`npm install`
- **开发服务器**：`npm run dev`（默认端口 3001）
- **构建**：`npm run build`
- **Lint**：`npm run lint`
- **测试**：`npm run test`（vitest）
- **运行单个测试文件**：`npm run test -- src/lib/__tests__/errors.test.ts`
- **启动 Supabase 本地**：`npx supabase start`
- **应用 migration**：`npx supabase db push`
- **启动 Redis（Docker）**：`docker compose up -d`

## 开发约定

- 仅修改 `backend/` 目录下的文件。
- API 设计需遵循仓库根目录中的 RESTful 约束。
- 禁止硬编码环境地址、数据库连接信息、token、密钥或其他环境相关常量。
- 所有环境相关值通过 `config` 模块获取，提供 `??` 默认值。
- 统一错误处理：业务错误抛出 `AppError`（携带 `ErrorCode` 枚举），由 `withErrorHandler` middleware 捕获并格式化为 `{ error: { code, message } }` JSON。
