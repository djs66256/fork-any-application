# Backend 初始化规范

## 技术栈

| 组件 | 选型 |
|------|------|
| 运行时 | Node.js ≥ 20 |
| 框架 | Next.js 16 App Router (API Routes) |
| 语言 | TypeScript 5.x |
| 数据校验 | Zod ≥ 4 |
| BaaS | Supabase (PostgreSQL + Auth + Storage + Realtime) |
| 缓存 | Redis (ioredis) |
| 数据库 Migration | Supabase CLI (`supabase migration new`) |
| 测试 | Vitest |

## 标准目录结构

```
backend/
├── CLAUDE.md                    # 后端开发规范
├── package.json
├── tsconfig.json
├── .env.example
├── src/
│   ├── app/
│   │   ├── layout.tsx           # 管理后台根布局
│   │   ├── page.tsx             # 管理首页（名称+版本+环境+健康状态）
│   │   └── api/
│   │       ├── health/
│   │       │   └── route.ts     # GET /api/health
│   │       ├── dramas/
│   │       │   ├── route.ts     # GET/POST /api/dramas
│   │       │   └── [id]/
│   │       │       └── route.ts # GET /api/dramas/[id]
│   │       ├── episodes/
│   │       │   └── [id]/
│   │       │       └── route.ts # GET /api/episodes/[id]
│   │       └── player/
│   │           ├── start/
│   │           │   └── route.ts # POST /api/player/start
│   │           └── stop/
│   │               └── route.ts # POST /api/player/stop
│   ├── services/                # Service 层 — 按业务域独立目录
│   │   ├── health/
│   │   │   ├── health.service.ts
│   │   │   └── health.service.test.ts
│   │   ├── drama/
│   │   │   ├── drama.service.ts
│   │   │   └── drama.service.test.ts
│   │   ├── episode/
│   │   │   ├── episode.service.ts
│   │   │   └── episode.service.test.ts
│   │   └── player/
│   │       ├── player.service.ts
│   │       └── player.service.test.ts
│   ├── repositories/            # Repository 层 — 按实体独立目录
│   │   ├── interfaces/
│   │   │   ├── drama.repository.interface.ts
│   │   │   └── episode.repository.interface.ts
│   │   ├── supabase/
│   │   │   ├── drama.supabase.repository.ts
│   │   │   └── episode.supabase.repository.ts
│   │   └── mock/
│   │       ├── drama.mock.repository.ts
│   │       └── episode.mock.repository.ts
│   ├── infrastructure/          # 基础设施（外部依赖实例化）
│   │   ├── supabase.ts          # Supabase Client 双实例
│   │   └── redis.ts             # Redis 客户端
│   ├── middleware/
│   │   ├── cors.ts
│   │   ├── logger.ts
│   │   ├── auth.ts              # Supabase JWT 验证（骨架）
│   │   └── error-handler.ts
│   └── lib/                     # Shared 层
│       ├── schemas.ts           # Zod Schema（Drama, Episode, UserProfile, Health...）
│       ├── errors.ts            # AppError 类 + 错误码枚举 + 错误工厂
│       ├── types.ts             # 共享 TypeScript 类型
│       └── config.ts            # 环境变量注入
├── supabase/
│   ├── config.toml              # Supabase CLI 配置
│   └── migrations/
│       └── .gitkeep
└── README.md
```

## API 标准

- **URL 格式**：`/api/<resource>`
- **响应格式**：统一 `{"error":{"code":"...","message":"..."}}` 错误结构
- **错误码枚举**：NOT_FOUND、VALIDATION_ERROR、UNAUTHORIZED、FORBIDDEN、CONFLICT、TOO_MANY_REQUESTS、INTERNAL_ERROR、NOT_IMPLEMENTED、SERVICE_UNAVAILABLE
- **Middleware**：使用 wrapper 函数模式（非 `middleware.ts`），因为 Edge Runtime 无法访问 Supabase SDK
- **健康检查**：`GET /api/health` 返回 `{"status":"ok|degraded","version":"0.1.0","timestamp":"...","services":{"database":"connected|disconnected","redis":"connected|disconnected"}}`

## 标准 Base URL

- 生产环境：`SUPABASE_URL` + `SUPABASE_ANON_KEY`（通过环境变量注入）
- 本地开发：`http://127.0.0.1:54321`（`supabase start` 输出）
- 端口：默认 3001（与 Web 3000 错开）

## 约束

- Route → Service → Repository 单向依赖，不跳过层级
- 所有 Service 通过构造函数注入 Repository Interface
- 测试与源码同目录（`*.test.ts`）
- 禁止硬编码常量，所有配置通过 `.env` 注入
