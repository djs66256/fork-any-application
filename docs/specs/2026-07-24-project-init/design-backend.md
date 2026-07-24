# Backend 端技术方案

> 特性：项目初始化与架构设计
> 日期：2026-07-24
> 对应 PRD：US-02（Backend 工程初始化）
> 关联设计：design.md（跨端共享设计）

---

## 1. 概述

### 1.1 方案范围

本方案覆盖 Backend 端基于 **Supabase** 的完整技术架构设计，包括：

- **四层架构**：Route → Service → Repository → Infrastructure 分层设计
- **基础服务**：Supabase 作为核心 BaaS，提供 PostgreSQL 数据库、Auth 认证、Storage 存储、Realtime 实时推送
- **API 路由**：7 个端点（`health` + `dramas` + `dramas/[id]` + `episodes/[id]` + `player/start` + `player/stop`）
- **Service 层**：HealthService（含 Supabase DB + Redis 连通性检查）、DramaService/EpisodeService/PlayerService（骨架）
- **Repository 层**：Interface + Supabase 实现 + Mock 实现（基于 Supabase JS Client）
- **Infrastructure 层**：Supabase Client 单例、Redis 客户端
- **Middleware 链**：CORS、请求日志、统一错误处理、Supabase Auth（骨架）
- **数据库 Migration**：Supabase CLI 管理 migration
- **配置与环境变量**：全部通过 `.env` 注入

### 1.2 当前状态

Backend 工程从零开始初始化。

### 1.3 Supabase 选型理由

| 能力 | Supabase 提供 | 传统方案 | 选型理由 |
|------|-------------|---------|---------|
| 数据库 | 托管 PostgreSQL 15 | 自建 pg + Docker | 免运维、自带 Dashboard、内置 Row Level Security |
| 认证 | Supabase Auth（邮箱/手机/OAuth） | 自建 JWT + 用户表 | 开箱即用、支持多种登录方式、自动管理 JWT |
| 存储 | Supabase Storage（S3 兼容） | 自建 MinIO/对接 CDN | 统一管理、内置 CDN、支持公开/私有 bucket |
| 实时 | Supabase Realtime（WebSocket） | 自建 Socket.io/WebSocket | 基于 PostgreSQL 逻辑复制、自动广播变更 |
| API | 自动生成 REST API（PostgREST） | 手写所有 CRUD 接口 | 加速开发、但本方案仍手写 API 以获得完整控制 |
| Migration | Supabase CLI (`supabase migration`) | Drizzle/Prisma | 原生 SQL migration、与 Supabase 平台深度集成 |
| 本地开发 | `supabase start`（Docker） | 手动 docker compose | 一键启动完整 Supabase 栈 |

---

## 2. 分层架构

### 2.1 四层架构模型

```
┌──────────────────────────────────────────────────┐
│  Route Layer (src/app/api/**/route.ts)           │  ← HTTP 入/出，参数校验，错误响应
├──────────────────────────────────────────────────┤
│  Service Layer (src/services/)                   │  ← 业务逻辑，跨 Repository 编排
├──────────────────────────────────────────────────┤
│  Repository Layer (src/repositories/)            │  ← 数据访问抽象，Interface + Supabase 实现
├──────────────────────────────────────────────────┤
│  Infrastructure (src/infrastructure/)            │  ← Supabase Client、Redis 客户端
│  Shared (src/lib/)                               │  ← 跨层共享（config、schemas、errors）
└──────────────────────────────────────────────────┘
```

### 2.2 层间依赖约束

| 约束 | 规则 |
|------|------|
| Route → Service | Route 只调用 Service，不直接访问 Repository 或 Infrastructure |
| Service → Repository | Service 依赖 Repository Interface（构造函数注入），不依赖具体实现 |
| Repository → Infrastructure | Repository 的 Supabase 实现依赖 `infrastructure/supabase.ts` 提供的 client |
| Service → lib | Service 可使用 `lib/` 中的 schema、config、errors |
| Route → lib | Route 可使用 `lib/` 中的 schema、errors |
| Supabase Client 单一入口 | 整个应用通过 `infrastructure/supabase.ts` 导出单例 client，所有 Repository 注入该 client |
| 跨层禁止 | 上层不允许反向依赖下层 |

### 2.3 完整文件清单

```
backend/src/
├── app/
│   ├── layout.tsx                                     # 根布局
│   ├── page.tsx                                       # 管理首页
│   ├── globals.css                                    # 全局样式
│   └── api/
│       ├── health/
│       │   └── route.ts                               # GET /api/health
│       ├── dramas/
│       │   ├── route.ts                               # GET /api/dramas, POST /api/dramas
│       │   └── [id]/
│       │       └── route.ts                           # GET /api/dramas/[id]
│       ├── episodes/
│       │   └── [id]/
│       │       └── route.ts                           # GET /api/episodes/[id]
│       └── player/
│           ├── start/
│           │   └── route.ts                           # POST /api/player/start
│           └── stop/
│               └── route.ts                           # POST /api/player/stop
├── services/
│   ├── health/
│   │   ├── health.service.ts                          # 健康检查（Supabase DB + Redis）
│   │   └── health.service.test.ts
│   ├── drama/
│   │   ├── drama.service.ts                           # 短剧业务（骨架）
│   │   └── drama.service.test.ts
│   ├── episode/
│   │   ├── episode.service.ts                         # 剧集业务（骨架）
│   │   └── episode.service.test.ts
│   └── player/
│       ├── player.service.ts                          # 播放业务（骨架）
│       └── player.service.test.ts
├── repositories/
│   ├── interfaces/
│   │   ├── drama.repository.interface.ts              # Drama 数据访问接口
│   │   └── episode.repository.interface.ts            # Episode 数据访问接口
│   ├── supabase/
│   │   ├── drama.supabase.repository.ts               # Drama Supabase 实现
│   │   └── episode.supabase.repository.ts             # Episode Supabase 实现
│   └── mock/
│       ├── drama.mock.repository.ts                   # Drama Mock 实现
│       └── episode.mock.repository.ts                 # Episode Mock 实现
├── infrastructure/
│   ├── supabase.ts                                    # Supabase Client 单例
│   │   ├── client.ts                                  # 通用 client（anon key，RLS 控制权限）
│   │   └── admin.ts                                   # 服务端 client（service role key，绕过 RLS）
│   └── redis.ts                                       # Redis 客户端（ioredis）
├── middleware/
│   ├── cors.ts                                        # CORS 中间件
│   ├── logger.ts                                      # 请求日志中间件
│   ├── auth.ts                                        # Supabase Auth 验证中间件（骨架）
│   └── error-handler.ts                               # 统一错误处理 wrapper
├── lib/
│   ├── config.ts                                      # 环境变量注入
│   ├── schemas.ts                                     # Zod schema（Drama/Episode/User/Health）
│   ├── errors.ts                                      # 错误定义与格式化
│   └── types.ts                                       # 共享 TypeScript 类型
├── supabase/
│   └── migrations/                                    # Supabase CLI 管理的 migration 文件
│       └── .gitkeep
├── .env.example                                       # 环境变量模板
└── supabase/
    └── config.toml                                     # Supabase CLI 本地开发配置
```

---

## 3. 配置与环境变量

### 3.1 环境变量清单

所有配置项通过 `.env` 注入，禁止硬编码常量。

```bash
# .env (Backend 应用层)
APP_NAME=ShortDrama Backend
APP_VERSION=0.1.0
PORT=3001

# Supabase
SUPABASE_URL=http://127.0.0.1:54321              # 本地 Supabase API URL
SUPABASE_ANON_KEY=your-anon-key-here              # 客户端 anon key（用于前端和 RLS）
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key-here  # 服务端 key（绕过 RLS，仅 Backend 使用）

# Redis（独立于 Supabase，用于缓存和会话）
REDIS_URL=redis://localhost:6379
```

### 3.2 `config.ts`

```typescript
// src/lib/config.ts
export const config = {
  app: {
    name: process.env.APP_NAME ?? 'ShortDrama Backend',
    version: process.env.APP_VERSION ?? '0.1.0',
    env: process.env.NODE_ENV ?? 'development',
  },
  supabase: {
    url: process.env.SUPABASE_URL ?? 'http://127.0.0.1:54321',
    anonKey: process.env.SUPABASE_ANON_KEY ?? '',
    serviceRoleKey: process.env.SUPABASE_SERVICE_ROLE_KEY ?? '',
  },
  redis: {
    url: process.env.REDIS_URL ?? 'redis://localhost:6379',
  },
} as const;
```

> ⚠️ `??` 回退值仅限本地开发便利。生产环境必须通过环境变量显式注入所有配置。

### 3.3 `.env.example`

```bash
# Backend 配置
APP_NAME=ShortDrama Backend
APP_VERSION=0.1.0
PORT=3001

# Supabase — 本地开发使用 supabase start 输出值
SUPABASE_URL=http://127.0.0.1:54321
SUPABASE_ANON_KEY=your-anon-key-here
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key-here

# Redis
REDIS_URL=redis://localhost:6379
```

### 3.4 Supabase 本地开发环境

使用 Supabase CLI 替代手写 Docker Compose：

```bash
# 初始化 Supabase 项目
npx supabase init

# 启动本地 Supabase 栈（PostgreSQL + Auth + Storage + Realtime + Studio）
npx supabase start
```

`supabase start` 自动启动：
| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL | 54322 | 数据库 |
| Supabase Studio | 54323 | Web 管理界面 |
| Supabase API (Kong) | 54321 | REST API + Auth 端点 |
| Inbucket | 54324 | 本地邮件测试 |

---

## 4. Infrastructure 层设计

### 4.1 `supabase.ts` — Supabase Client 双实例

Backend 需要两种 Supabase Client：

- **`client`**：使用 `anonKey`，受 Row Level Security 约束（用于模拟前端请求的权限模型）
- **`admin`**：使用 `serviceRoleKey`，绕过 RLS（用于服务端内部操作：migration、管理任务、跨用户查询）

```typescript
// src/infrastructure/supabase.ts
import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { config } from '@/lib/config';

// 通用 client（anon key，受 RLS 限制）
let supabaseClient: SupabaseClient | null = null;

export function getSupabaseClient(): SupabaseClient {
  if (!supabaseClient) {
    supabaseClient = createClient(
      config.supabase.url,
      config.supabase.anonKey,
      {
        auth: {
          autoRefreshToken: false,
          persistSession: false,
        },
      },
    );
  }
  return supabaseClient;
}

// 管理 client（service role key，绕过 RLS — 仅限 Backend 服务端使用）
let supabaseAdmin: SupabaseClient | null = null;

export function getSupabaseAdmin(): SupabaseClient {
  if (!supabaseAdmin) {
    supabaseAdmin = createClient(
      config.supabase.url,
      config.supabase.serviceRoleKey,
      {
        auth: {
          autoRefreshToken: false,
          persistSession: false,
        },
      },
    );
  }
  return supabaseAdmin;
}

// 健康检查：检测 Supabase 数据库连通性
export async function checkSupabaseHealth(): Promise<boolean> {
  try {
    const client = getSupabaseAdmin();
    const { error } = await client.from('_health').select('*').limit(1).maybeSingle();
    // 如果 _health 表不存在，改用 raw SQL
    if (error?.code === '42P01') {
      const { error: rawError } = await client.rpc('version');
      return !rawError;
    }
    return !error;
  } catch {
    return false;
  }
}

// 释放 Supabase 连接
export async function closeSupabase(): Promise<void> {
  // Supabase JS client 不需要显式关闭，连接由底层 HTTP 管理
  supabaseClient = null;
  supabaseAdmin = null;
}
```

### 4.2 `redis.ts` — Redis 客户端

```typescript
// src/infrastructure/redis.ts
import Redis from 'ioredis';
import { config } from '@/lib/config';

let redis: Redis | null = null;

export function getRedis(): Redis {
  if (!redis) {
    redis = new Redis(config.redis.url, {
      maxRetriesPerRequest: 3,
      retryStrategy(times) {
        if (times > 3) return null;  // 停止重试
        return Math.min(times * 200, 2000);
      },
      lazyConnect: true,
    });
  }
  return redis;
}

export async function checkRedisHealth(): Promise<boolean> {
  try {
    const r = getRedis();
    await r.ping();
    return true;
  } catch {
    return false;
  }
}

export async function closeRedis(): Promise<void> {
  if (redis) {
    await redis.quit();
    redis = null;
  }
}
```

---

## 5. Shared / lib 层设计

### 5.1 `schemas.ts` — 完整 Zod Schema

所有 Zod Schema 定义在此，与 `design.md` 保持一致：

```typescript
// src/lib/schemas.ts
import { z } from 'zod';

// ===== Health =====
export const HealthResponseSchema = z.object({
  status: z.enum(['ok', 'degraded', 'error']),
  version: z.string(),
  timestamp: z.string().datetime(),
  services: z.object({
    database: z.enum(['connected', 'disconnected']),
    redis: z.enum(['connected', 'disconnected']),
  }),
});

export type HealthResponse = z.infer<typeof HealthResponseSchema>;

// ===== Drama =====
export const DramaSchema = z.object({
  id: z.string().uuid(),
  title: z.string().min(1).max(200),
  description: z.string().max(2000),
  cover_url: z.string().url(),
  category: z.string().min(1).max(50),
  episode_count: z.number().int().positive(),
  tags: z.array(z.string().max(30)).max(10).optional(),
  rating: z.number().min(0).max(10).optional(),
  status: z.enum(['draft', 'published', 'archived']).default('draft'),
  created_at: z.string().datetime(),
  updated_at: z.string().datetime(),
});

export type Drama = z.infer<typeof DramaSchema>;

export const DramaListResponseSchema = z.object({
  data: z.array(DramaSchema),
  pagination: z.object({
    page: z.number().int().positive(),
    page_size: z.number().int().positive(),
    total: z.number().int().nonnegative(),
    total_pages: z.number().int().nonnegative(),
  }),
});

export type DramaListResponse = z.infer<typeof DramaListResponseSchema>;

// ===== Episode =====
export const EpisodeSchema = z.object({
  id: z.string().uuid(),
  drama_id: z.string().uuid(),
  title: z.string().min(1).max(200),
  episode_number: z.number().int().positive(),
  video_url: z.string().url(),
  duration: z.number().int().positive(),  // 秒
  thumbnail_url: z.string().url(),
  status: z.enum(['draft', 'published', 'processing']).default('draft'),
  created_at: z.string().datetime(),
  updated_at: z.string().datetime(),
});

export type Episode = z.infer<typeof EpisodeSchema>;

// ===== Player =====
export const PlayerStartRequestSchema = z.object({
  episode_id: z.string().uuid(),
  position: z.number().int().nonnegative().optional().default(0),
});

export type PlayerStartRequest = z.infer<typeof PlayerStartRequestSchema>;

export const PlayerStopRequestSchema = z.object({
  episode_id: z.string().uuid(),
  position: z.number().int().nonnegative(),
  duration: z.number().int().positive(),
});

export type PlayerStopRequest = z.infer<typeof PlayerStopRequestSchema>;

// ===== User (Supabase Auth) =====
export const UserProfileSchema = z.object({
  id: z.string().uuid(),
  nickname: z.string().min(1).max(50),
  avatar_url: z.string().url().optional(),
  created_at: z.string().datetime(),
  updated_at: z.string().datetime(),
});

export type UserProfile = z.infer<typeof UserProfileSchema>;
```

### 5.2 `errors.ts` — 错误定义

```typescript
// src/lib/errors.ts

export const ErrorCode = {
  NOT_FOUND: 'NOT_FOUND',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  CONFLICT: 'CONFLICT',
  TOO_MANY_REQUESTS: 'TOO_MANY_REQUESTS',
  INTERNAL_ERROR: 'INTERNAL_ERROR',
  NOT_IMPLEMENTED: 'NOT_IMPLEMENTED',
  SERVICE_UNAVAILABLE: 'SERVICE_UNAVAILABLE',
} as const;

export type ErrorCode = (typeof ErrorCode)[keyof typeof ErrorCode];

export class AppError extends Error {
  constructor(
    public readonly code: ErrorCode,
    message: string,
    public readonly statusCode: number = 500,
    public readonly details?: unknown,
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export const Errors = {
  notFound: (resource: string, id?: string) =>
    new AppError(ErrorCode.NOT_FOUND, `${resource}${id ? ` (${id})` : ''} not found`, 404),

  validationError: (message: string, details?: unknown) =>
    new AppError(ErrorCode.VALIDATION_ERROR, message, 400, details),

  unauthorized: (message = 'Authentication required') =>
    new AppError(ErrorCode.UNAUTHORIZED, message, 401),

  forbidden: (message = 'Access denied') =>
    new AppError(ErrorCode.FORBIDDEN, message, 403),

  conflict: (message: string) =>
    new AppError(ErrorCode.CONFLICT, message, 409),

  tooManyRequests: (message = 'Too many requests') =>
    new AppError(ErrorCode.TOO_MANY_REQUESTS, message, 429),

  internal: (message = 'Internal server error') =>
    new AppError(ErrorCode.INTERNAL_ERROR, message, 500),

  notImplemented: (endpoint?: string) =>
    new AppError(ErrorCode.NOT_IMPLEMENTED, endpoint ? `${endpoint} is not yet implemented` : 'Not implemented', 501),

  serviceUnavailable: (service: string) =>
    new AppError(ErrorCode.SERVICE_UNAVAILABLE, `Service unavailable: ${service}`, 503),
};

export function formatErrorResponse(error: AppError) {
  return {
    error: {
      code: error.code,
      message: error.message,
      ...(error.details ? { details: error.details } : {}),
    },
  };
}
```

---

## 6. Middleware 链设计

### 6.1 请求流水线

```
请求 → [logger] → [cors] → [auth(可选)] → [参数校验] → Route Handler → 响应
         │          │          │              │                │
         ▼          ▼          ▼              ▼                ▼
      访问日志   跨域处理  Supabase JWT    Zod Schema    withErrorHandler
                         验证(骨架)       校验          统一 try-catch
```

### 6.2 Middleware 清单

| Middleware | 作用域 | 实现方式 | 说明 |
|-----------|--------|---------|------|
| CORS | 全局 | wrapper 函数，设置响应头 | 开发阶段 `Access-Control-Allow-Origin: *` |
| 请求日志 | 全局 | wrapper 函数，记录 method + path + duration | 使用 `console.log`，后续接入日志系统 |
| Auth 验证 | 路由组 | wrapper 函数，验证 Supabase JWT | 骨架，后续业务 PRD 启用 |
| 错误处理 | 全局 | `withErrorHandler` wrapper | 统一 `try-catch` → `formatErrorResponse` |

> **不使用 Next.js `middleware.ts`**：因为 Next.js App Router 的 `middleware.ts` 运行在 Edge Runtime，无法直接访问 Node.js 环境中的 Supabase Client 和 Redis Client。改用 wrapper 函数模式，在 Route Handler 内部组合使用。

### 6.3 错误处理 wrapper

```typescript
// src/middleware/error-handler.ts
import { NextRequest, NextResponse } from 'next/server';
import { AppError } from '@/lib/errors';
import { ZodError } from 'zod';
import { Errors } from '@/lib/errors';

type Handler = (req: NextRequest, context: { params: Promise<Record<string, string>> }) => Promise<NextResponse>;

export function withErrorHandler(handler: Handler): Handler {
  return async (req, context) => {
    try {
      return await handler(req, context);
    } catch (error) {
      if (error instanceof AppError) {
        return NextResponse.json(
          { error: { code: error.code, message: error.message, ...(error.details ? { details: error.details } : {}) } },
          { status: error.statusCode },
        );
      }
      if (error instanceof ZodError) {
        return NextResponse.json(
          { error: { code: 'VALIDATION_ERROR', message: 'Validation failed', details: error.errors } },
          { status: 400 },
        );
      }
      console.error('Unhandled error:', error);
      return NextResponse.json(
        { error: { code: 'INTERNAL_ERROR', message: 'Internal server error' } },
        { status: 500 },
      );
    }
  };
}
```

### 6.4 Supabase Auth 中间件（骨架）

```typescript
// src/middleware/auth.ts
import { NextRequest, NextResponse } from 'next/server';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';

type Handler = (req: NextRequest, context: { params: Promise<Record<string, string>> }) => Promise<NextResponse>;

// requireAuth: 骨架中间件，验证请求中的 Supabase JWT
// 当前阶段不强制启用（所有 API 公开访问），后续业务 PRD 中按路由组启用
export function requireAuth(handler: Handler): Handler {
  return async (req, context) => {
    const authHeader = req.headers.get('authorization');
    if (!authHeader?.startsWith('Bearer ')) {
      return NextResponse.json(
        { error: { code: 'UNAUTHORIZED', message: 'Authentication required' } },
        { status: 401 },
      );
    }
    const token = authHeader.split(' ')[1];
    const supabase = getSupabaseAdmin();
    const { data: { user }, error } = await supabase.auth.getUser(token);
    if (error || !user) {
      return NextResponse.json(
        { error: { code: 'UNAUTHORIZED', message: 'Invalid or expired token' } },
        { status: 401 },
      );
    }
    // 将 user 信息附加到 request context（通过 header 传递，Next.js 不支持直接修改 request）
    req.headers.set('x-user-id', user.id);
    return handler(req, context);
  };
}
```

---

## 7. API 路由设计

### 7.1 路由总览

| 路由文件 | HTTP 方法 | URL 路径 | Auth | 实现状态 |
|---------|----------|---------|------|---------|
| `api/health/route.ts` | GET | `/api/health` | 否 | ✅ 完整实现 |
| `api/dramas/route.ts` | GET | `/api/dramas` | 否 | ✅ 返回空列表 |
| `api/dramas/route.ts` | POST | `/api/dramas` | 否 | 🚧 501 骨架 |
| `api/dramas/[id]/route.ts` | GET | `/api/dramas/[id]` | 否 | 🚧 501 骨架 |
| `api/episodes/[id]/route.ts` | GET | `/api/episodes/[id]` | 否 | 🚧 501 骨架 |
| `api/player/start/route.ts` | POST | `/api/player/start` | 否 | 🚧 501 骨架 |
| `api/player/stop/route.ts` | POST | `/api/player/stop` | 否 | 🚧 501 骨架 |

### 7.2 统一响应格式

**成功响应**：
```json
{
  "data": { ... },
  "pagination": { "page": 1, "page_size": 20, "total": 0, "total_pages": 0 }
}
```

**错误响应**：
```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Drama (abc-123) not found"
  }
}
```

### 7.3 路由实现

#### 7.3.1 `GET /api/health`

```typescript
// src/app/api/health/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { config } from '@/lib/config';
import { withErrorHandler } from '@/middleware/error-handler';
import { checkSupabaseHealth } from '@/infrastructure/supabase';
import { checkRedisHealth } from '@/infrastructure/redis';

export const GET = withErrorHandler(async (_req: NextRequest) => {
  const [dbConnected, redisConnected] = await Promise.all([
    checkSupabaseHealth(),
    checkRedisHealth(),
  ]);

  const allHealthy = dbConnected && redisConnected;

  return NextResponse.json({
    status: allHealthy ? 'ok' : 'degraded',
    version: config.app.version,
    timestamp: new Date().toISOString(),
    services: {
      database: dbConnected ? 'connected' : 'disconnected',
      redis: redisConnected ? 'connected' : 'disconnected',
    },
  });
});
```

#### 7.3.2 `GET /api/dramas`（空列表）

```typescript
// src/app/api/dramas/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';

export const GET = withErrorHandler(async (req: NextRequest) => {
  const { searchParams } = new URL(req.url);
  const page = Math.max(1, parseInt(searchParams.get('page') ?? '1', 10));
  const pageSize = Math.min(100, Math.max(1, parseInt(searchParams.get('pageSize') ?? '20', 10)));

  // 骨架：返回空列表 + 分页元数据
  return NextResponse.json({
    data: [],
    pagination: {
      page,
      page_size: pageSize,
      total: 0,
      total_pages: 0,
    },
  });
});
```

#### 7.3.3 骨架路由（5 个 501 端点）

```typescript
// 示例：src/app/api/dramas/[id]/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';

export const GET = withErrorHandler(async (_req: NextRequest) => {
  return NextResponse.json(
    { error: { code: 'NOT_IMPLEMENTED', message: 'This endpoint is not yet implemented' } },
    { status: 501 },
  );
});
```

> 其余 4 个骨架端点（`POST /api/dramas`、`GET /api/episodes/[id]`、`POST /api/player/start`、`POST /api/player/stop`）遵循相同模式。

---

## 8. Service 层设计

### 8.1 Service 清单

| Service | 职责 | 依赖 |
|---------|------|------|
| HealthService | 检查 Supabase DB + Redis 连通性 | `checkSupabaseHealth()`, `checkRedisHealth()` |
| DramaService | 短剧 CRUD 业务逻辑（骨架） | `DramaRepositoryInterface` |
| EpisodeService | 剧集查询业务逻辑（骨架） | `EpisodeRepositoryInterface` |
| PlayerService | 播放控制业务逻辑（骨架） | `DramaRepositoryInterface`, `EpisodeRepositoryInterface` |

### 8.2 HealthService

```typescript
// src/services/health/health.service.ts
import { checkSupabaseHealth } from '@/infrastructure/supabase';
import { checkRedisHealth } from '@/infrastructure/redis';
import { config } from '@/lib/config';

export interface HealthStatus {
  status: 'ok' | 'degraded' | 'error';
  version: string;
  timestamp: string;
  services: {
    database: 'connected' | 'disconnected';
    redis: 'connected' | 'disconnected';
  };
}

export class HealthService {
  async check(): Promise<HealthStatus> {
    const [dbConnected, redisConnected] = await Promise.all([
      checkSupabaseHealth(),
      checkRedisHealth(),
    ]);

    const allHealthy = dbConnected && redisConnected;

    return {
      status: allHealthy ? 'ok' : 'degraded',
      version: config.app.version,
      timestamp: new Date().toISOString(),
      services: {
        database: dbConnected ? 'connected' : 'disconnected',
        redis: redisConnected ? 'connected' : 'disconnected',
      },
    };
  }
}
```

### 8.3 骨架 Service

```typescript
// src/services/drama/drama.service.ts
import { DramaRepositoryInterface } from '@/repositories/interfaces/drama.repository.interface';
import { Errors } from '@/lib/errors';

export class DramaService {
  constructor(private readonly dramaRepo: DramaRepositoryInterface) {}

  async listDramas(page: number, pageSize: number) {
    // 骨架：返回空列表
    return { data: [], pagination: { page, pageSize, total: 0, totalPages: 0 } };
  }

  async getDramaById(id: string) {
    throw Errors.notImplemented('getDramaById');
  }

  async createDrama(input: unknown) {
    throw Errors.notImplemented('createDrama');
  }
}
```

### 8.4 Service 依赖注入

所有 Service 通过构造函数注入 Repository Interface，解耦具体实现：

```typescript
// 生产环境
const dramaRepo = new DramaSupabaseRepository(getSupabaseAdmin());
export const dramaService = new DramaService(dramaRepo);

// 测试环境
const mockRepo = new DramaMockRepository();
export const testDramaService = new DramaService(mockRepo);
```

---

## 9. Repository 层设计

### 9.1 Interface 定义

```typescript
// src/repositories/interfaces/drama.repository.interface.ts
import type { Drama } from '@/lib/schemas';

export interface PaginationParams {
  page: number;
  pageSize: number;
}

export interface PaginatedResult<T> {
  data: T[];
  pagination: {
    page: number;
    pageSize: number;
    total: number;
    totalPages: number;
  };
}

export interface DramaRepositoryInterface {
  findMany(params: PaginationParams): Promise<PaginatedResult<Drama>>;
  findById(id: string): Promise<Drama | null>;
  create(input: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama>;
  update(id: string, input: Partial<Drama>): Promise<Drama>;
  delete(id: string): Promise<void>;
}
```

### 9.2 Supabase 实现（骨架）

```typescript
// src/repositories/supabase/drama.supabase.repository.ts
import { SupabaseClient } from '@supabase/supabase-js';
import {
  DramaRepositoryInterface,
  PaginationParams,
  PaginatedResult,
} from '@/repositories/interfaces/drama.repository.interface';
import type { Drama } from '@/lib/schemas';
import { Errors } from '@/lib/errors';

export class DramaSupabaseRepository implements DramaRepositoryInterface {
  private readonly table = 'dramas';

  constructor(private readonly supabase: SupabaseClient) {}

  async findMany(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    // 骨架：表尚未创建，返回空列表
    const { data, error, count } = await this.supabase
      .from(this.table)
      .select('*', { count: 'exact' })
      .range((params.page - 1) * params.pageSize, params.page * params.pageSize - 1)
      .order('created_at', { ascending: false });

    if (error) throw Errors.internal(`Database error: ${error.message}`);

    return {
      data: (data as Drama[]) ?? [],
      pagination: {
        page: params.page,
        pageSize: params.pageSize,
        total: count ?? 0,
        totalPages: Math.ceil((count ?? 0) / params.pageSize),
      },
    };
  }

  async findById(id: string): Promise<Drama | null> {
    const { data, error } = await this.supabase
      .from(this.table)
      .select('*')
      .eq('id', id)
      .single();

    if (error) {
      if (error.code === 'PGRST116') return null; // 0 rows
      throw Errors.internal(`Database error: ${error.message}`);
    }
    return data as Drama;
  }

  async create(input: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama> {
    const { data, error } = await this.supabase
      .from(this.table)
      .insert(input)
      .select()
      .single();

    if (error) throw Errors.internal(`Database error: ${error.message}`);
    return data as Drama;
  }

  async update(id: string, input: Partial<Drama>): Promise<Drama> {
    const { data, error } = await this.supabase
      .from(this.table)
      .update(input)
      .eq('id', id)
      .select()
      .single();

    if (error) throw Errors.internal(`Database error: ${error.message}`);
    return data as Drama;
  }

  async delete(id: string): Promise<void> {
    const { error } = await this.supabase
      .from(this.table)
      .delete()
      .eq('id', id);

    if (error) throw Errors.internal(`Database error: ${error.message}`);
  }
}
```

### 9.3 Mock 实现

```typescript
// src/repositories/mock/drama.mock.repository.ts
import {
  DramaRepositoryInterface,
  PaginationParams,
  PaginatedResult,
} from '@/repositories/interfaces/drama.repository.interface';
import type { Drama } from '@/lib/schemas';

export class DramaMockRepository implements DramaRepositoryInterface {
  private items: Map<string, Drama> = new Map();

  async findMany(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    const all = Array.from(this.items.values());
    return {
      data: all.slice((params.page - 1) * params.pageSize, params.page * params.pageSize),
      pagination: {
        page: params.page,
        pageSize: params.pageSize,
        total: all.length,
        totalPages: Math.ceil(all.length / params.pageSize),
      },
    };
  }

  async findById(id: string): Promise<Drama | null> {
    return this.items.get(id) ?? null;
  }

  async create(input: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama> {
    const drama: Drama = {
      ...input,
      id: crypto.randomUUID(),
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    } as Drama;
    this.items.set(drama.id, drama);
    return drama;
  }

  async update(id: string, input: Partial<Drama>): Promise<Drama> {
    const existing = this.items.get(id);
    if (!existing) throw new Error('Not found');
    const updated = { ...existing, ...input, updated_at: new Date().toISOString() };
    this.items.set(id, updated);
    return updated;
  }

  async delete(id: string): Promise<void> {
    this.items.delete(id);
  }
}
```

---

## 10. 数据库 Migration（Supabase CLI）

### 10.1 Migration 管理方式

使用 Supabase CLI 管理数据库 schema 变更：

```bash
# 创建新 migration
npx supabase migration new init_tables

# 应用 migration 到本地数据库
npx supabase db reset

# 查看 migration 状态
npx supabase migration list
```

### 10.2 初始 Migration（骨架）

```sql
-- supabase/migrations/00000000000001_init_tables.sql

-- 短剧表
CREATE TABLE IF NOT EXISTS dramas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR(200) NOT NULL,
  description TEXT DEFAULT '',
  cover_url TEXT NOT NULL,
  category VARCHAR(50) NOT NULL,
  episode_count INTEGER NOT NULL DEFAULT 0 CHECK (episode_count >= 0),
  tags TEXT[] DEFAULT '{}',
  rating REAL CHECK (rating >= 0 AND rating <= 10),
  status VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'published', 'archived')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 剧集表
CREATE TABLE IF NOT EXISTS episodes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drama_id UUID NOT NULL REFERENCES dramas(id) ON DELETE CASCADE,
  title VARCHAR(200) NOT NULL,
  episode_number INTEGER NOT NULL,
  video_url TEXT NOT NULL,
  duration INTEGER NOT NULL CHECK (duration > 0),
  thumbnail_url TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'published', 'processing')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(drama_id, episode_number)
);

-- 用户 Profile 表（扩展 Supabase Auth 的 auth.users）
CREATE TABLE IF NOT EXISTS profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  nickname VARCHAR(50) NOT NULL,
  avatar_url TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_dramas_category ON dramas(category);
CREATE INDEX IF NOT EXISTS idx_dramas_status ON dramas(status);
CREATE INDEX IF NOT EXISTS idx_dramas_created_at ON dramas(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_episodes_drama_id ON episodes(drama_id);
CREATE INDEX IF NOT EXISTS idx_episodes_drama_number ON episodes(drama_id, episode_number);
CREATE INDEX IF NOT EXISTS idx_profiles_nickname ON profiles(nickname);

-- 自动更新 updated_at 的触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_dramas_updated_at
  BEFORE UPDATE ON dramas FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER set_episodes_updated_at
  BEFORE UPDATE ON episodes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER set_profiles_updated_at
  BEFORE UPDATE ON profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 10.3 Row Level Security (RLS) 策略（骨架）

后续业务 PRD 中启用 RLS 策略以保护数据访问：

```sql
-- supabase/migrations/00000000000002_rls_policies.sql (后续 PRD 创建)

-- 启用 RLS
ALTER TABLE dramas ENABLE ROW LEVEL SECURITY;
ALTER TABLE episodes ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- 公开读取（当前阶段）
CREATE POLICY "Public read access" ON dramas FOR SELECT USING (true);
CREATE POLICY "Public read access" ON episodes FOR SELECT USING (true);

-- 用户只能修改自己的 profile（后续 Auth PRD 启用）
-- CREATE POLICY "Users can update own profile" ON profiles
--   FOR UPDATE USING (auth.uid() = id);
```

---

## 11. 测试策略

### 11.1 测试框架

| 工具 | 用途 |
|------|------|
| vitest | 单元测试 + 集成测试运行器 |
| Mock Repository | Service 层单元测试 |

### 11.2 HealthService 测试

```typescript
// src/services/health/health.service.test.ts
import { describe, it, expect, vi } from 'vitest';
import { HealthService } from './health.service';

describe('HealthService', () => {
  it('should return ok when all services are connected', async () => {
    vi.mock('@/infrastructure/supabase', () => ({
      checkSupabaseHealth: () => Promise.resolve(true),
    }));
    vi.mock('@/infrastructure/redis', () => ({
      checkRedisHealth: () => Promise.resolve(true),
    }));

    const service = new HealthService();
    const result = await service.check();

    expect(result.status).toBe('ok');
    expect(result.services.database).toBe('connected');
    expect(result.services.redis).toBe('connected');
  });

  it('should return degraded when database is disconnected', async () => {
    vi.mock('@/infrastructure/supabase', () => ({
      checkSupabaseHealth: () => Promise.resolve(false),
    }));
    vi.mock('@/infrastructure/redis', () => ({
      checkRedisHealth: () => Promise.resolve(true),
    }));

    const service = new HealthService();
    const result = await service.check();

    expect(result.status).toBe('degraded');
    expect(result.services.database).toBe('disconnected');
  });
});
```

### 11.3 Mock Repository 测试（骨架）

```typescript
// src/services/drama/drama.service.test.ts
import { describe, it, expect } from 'vitest';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { DramaService } from './drama.service';

describe('DramaService', () => {
  it('should return empty list when no dramas exist', async () => {
    const mockRepo = new DramaMockRepository();
    const service = new DramaService(mockRepo);

    const result = await service.listDramas(1, 20);

    expect(result.data).toEqual([]);
    expect(result.pagination.total).toBe(0);
  });
});
```

---

## 12. 依赖汇总

### 12.1 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 | 审批 |
|---------|------|------|---------|------|
| `@supabase/supabase-js` | ^2.x | Supabase JS Client SDK | 官方 SDK，统一访问 DB/Auth/Storage/Realtime | ✅ 已批准 |
| `ioredis` | ^5.x | Redis 客户端 | 高性能、完整 Redis 协议支持 | ✅ 已批准 |
| `vitest` | ^2.x | 单元测试框架 | Vite 生态、快速、与 Next.js 兼容 | ✅ 已批准 |

> Supabase CLI (`supabase`) 作为开发依赖（devDependency），用于本地环境管理和 migration。

### 12.2 已有依赖

| 依赖名称 | 用途 |
|---------|------|
| `next` | Next.js 16 App Router |
| `react` / `react-dom` | SSR 页面渲染 |
| `zod` | 数据校验 |
| `typescript` | 类型系统 |

---

## 13. 文件变更清单

### 13.1 新建文件（30 个）

| 文件 | 说明 |
|------|------|
| `src/infrastructure/supabase.ts` | Supabase Client 双实例 + 健康检查 |
| `src/infrastructure/redis.ts` | Redis 客户端 + 健康检查 |
| `src/middleware/cors.ts` | CORS 中间件 |
| `src/middleware/logger.ts` | 请求日志 |
| `src/middleware/auth.ts` | Supabase Auth 验证（骨架） |
| `src/middleware/error-handler.ts` | 统一错误处理 |
| `src/lib/errors.ts` | 错误码 + AppError 类 + 错误工厂 |
| `src/lib/types.ts` | 共享 TypeScript 类型 |
| `src/services/health/health.service.ts` | HealthService |
| `src/services/health/health.service.test.ts` | HealthService 测试 |
| `src/services/drama/drama.service.ts` | DramaService（骨架） |
| `src/services/drama/drama.service.test.ts` | DramaService 测试 |
| `src/services/episode/episode.service.ts` | EpisodeService（骨架） |
| `src/services/episode/episode.service.test.ts` | EpisodeService 测试 |
| `src/services/player/player.service.ts` | PlayerService（骨架） |
| `src/services/player/player.service.test.ts` | PlayerService 测试 |
| `src/repositories/interfaces/drama.repository.interface.ts` | Drama Repository Interface |
| `src/repositories/interfaces/episode.repository.interface.ts` | Episode Repository Interface |
| `src/repositories/supabase/drama.supabase.repository.ts` | Drama Supabase 实现 |
| `src/repositories/supabase/episode.supabase.repository.ts` | Episode Supabase 实现 |
| `src/repositories/mock/drama.mock.repository.ts` | Drama Mock 实现 |
| `src/repositories/mock/episode.mock.repository.ts` | Episode Mock 实现 |
| `src/app/api/dramas/route.ts` | GET/POST /api/dramas |
| `src/app/api/dramas/[id]/route.ts` | GET /api/dramas/[id] |
| `src/app/api/episodes/[id]/route.ts` | GET /api/episodes/[id] |
| `src/app/api/player/start/route.ts` | POST /api/player/start |
| `src/app/api/player/stop/route.ts` | POST /api/player/stop |
| `supabase/migrations/00000000000001_init_tables.sql` | 初始 schema migration |
| `supabase/config.toml` | Supabase CLI 配置 |
| `src/db/seed.ts` | 种子数据脚本（骨架） |

### 13.2 修改文件（5 个）

| 文件 | 变更 |
|------|------|
| `src/lib/config.ts` | 扩展：新增 supabase.url/anonKey/serviceRoleKey、redis.url |
| `src/lib/schemas.ts` | 扩展：新增 DramaSchema、EpisodeSchema、PlayerSchemas、UserProfileSchema |
| `src/app/api/health/route.ts` | 扩展：新增 Supabase DB + Redis 连通性检查 |
| `src/app/page.tsx` | 扩展：展示 Supabase 和 Redis 连接状态 |
| `package.json` | 新增：`@supabase/supabase-js`、`ioredis`、`vitest` |
| `CLAUDE.md` | 扩展：新增 Supabase 开发规范 |

---

## 14. 架构决策记录 (ADR)

### ADR-1：Supabase 作为基础 BaaS 平台

- **日期**：2026-07-24
- **状态**：已批准
- **决策**：使用 Supabase 作为项目的核心后端基础设施，替代自建 PostgreSQL + 自建 Auth 的方案
- **原因**：
  1. Supabase 提供完整的 BaaS 能力（数据库、认证、存储、实时），减少自建基础设施的工作量
  2. 本地开发通过 `supabase start` 一键启动完整栈，开发体验优于手动 Docker Compose
  3. Supabase CLI 管理 migration，原生 SQL 方式，与 PostgreSQL 完全兼容
  4. Row Level Security 提供数据库级别的权限控制，而非仅在应用层实现
  5. Supabase Studio 提供 Web 管理界面，方便开发和调试
- **影响**：
  - Infrastructure 层使用 `@supabase/supabase-js` SDK，而非原始 `pg` 驱动
  - Repository 层使用 Supabase Client 的 `from().select().insert()` API，而非 raw SQL
  - Migration 使用 Supabase CLI (`supabase migration`)，而非 Drizzle/Prisma
  - RLS 策略在后续 PRD 中定义，当前阶段表公开可读
  - Auth 集成后续 PRD 中启用 Supabase Auth

### ADR-2：Interface + 实现模式

- **日期**：2026-07-24
- **状态**：已批准
- **决策**：Repository 层采用 Interface（定义在 `repositories/interfaces/`）+ 多实现（Supabase 在 `repositories/supabase/`、Mock 在 `repositories/mock/`）
- **原因**：
  1. 依赖倒置：Service 层只依赖 Interface，不依赖具体实现
  2. 可测试性：单元测试通过注入 Mock Repository 隔离数据库
  3. 可替换性：未来如需切换数据源（如从 Supabase 迁移到其他服务），只需新增一个实现
- **影响**：
  - Service 层通过构造函数注入 Repository Interface
  - 每个实体需维护 Interface + Supabase 实现 + Mock 实现三份代码

### ADR-3：Wrapper 函数模式替代 Next.js middleware.ts

- **日期**：2026-07-24
- **状态**：已批准
- **决策**：使用 wrapper 函数（`withErrorHandler`、`requireAuth`）组合 middleware，而非 Next.js App Router 的 `middleware.ts`
- **原因**：
  1. Next.js `middleware.ts` 运行在 Edge Runtime，无法访问 Node.js 环境中的 Supabase Client 和 Redis Client
  2. Wrapper 函数在 Route Handler 内执行，有完整的 Node.js 环境
  3. 组合模式更灵活，可按路由粒度选择性地应用不同 middleware
- **影响**：
  - 每个 Route Handler 需要显式包裹 `withErrorHandler`
  - 中间件代码与业务代码在同一运行时，调试更方便

---

## 15. API 与 design.md 对齐

### 15.1 数据模型对齐

| 实体 | design.md Zod Schema | Backend 实现 | 对齐 |
|------|---------------------|-------------|------|
| Drama | 12 字段（id, title, description, coverUrl, category, episodeCount, tags, rating, status, createdAt, updatedAt） | schemas.ts 中使用 snake_case 字段名（`cover_url`、`episode_count`） | ✅ 字段语义一致，命名风格对齐 Supabase/PostgreSQL 惯例 |
| Episode | 10 字段（id, dramaId, title, episodeNumber, videoUrl, duration, thumbnailUrl, status, createdAt, updatedAt） | 同上，使用 snake_case | ✅ |
| HealthResponse | status, version, timestamp, services.{database, redis} | 同 | ✅ |

### 15.2 端点对齐

所有 7 个 API 端点与 `design.md` 严格一一对应，每个端点的请求/响应格式一致。

---

## 16. 变更历史

| 日期 | 变更内容 | 变更原因 |
|------|---------|---------|
| 2026-07-24 | 初始版本 | 项目初始化 Backend 方案 |
| 2026-07-24 | 重构为 Supabase 基础服务 | 用户决策：以 Supabase 为 BaaS 平台 |
| 2026-07-24 | spec-review 修复 | 统一 episodeCount 约束、修复 Supabase demo keys、补充错误码定义 |
