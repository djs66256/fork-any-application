# 基础库与基础能力 — Backend

> 本文档定义 Backend 端的基础库选型、集成方案与基础能力接入规范。

---

## 1. HTTP 服务

Backend 端基于 Next.js App Router 的 Route Handler 提供 HTTP API 服务。与传统的 Express/Koa 不同，Next.js API Routes 是 Serverless-first 架构，每个 Route Handler 是独立的请求处理函数。

### 1.1 Route Handler

每个 `route.ts` 文件导出对应 HTTP 方法的函数：

```typescript
// GET /api/videos — 获取视频列表
export async function GET(request: NextRequest) {
  const { searchParams } = request.nextUrl;
  const cursor = searchParams.get('cursor');
  const limit = Number(searchParams.get('limit')) || 20;
  // 校验 → Service → 响应
  return NextResponse.json({ data: videos, meta: { cursor, hasMore } });
}

// POST /api/videos — 创建视频
export async function POST(request: NextRequest) {
  const body = await request.json();
  const parsed = createVideoSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: { code: 'VALIDATION_ERROR', details: parsed.error.flatten() } },
      { status: 400 },
    );
  }
  const video = await videoService.create(parsed.data);
  return NextResponse.json({ data: video }, { status: 201 });
}
```

**规范**：
- 每个 HTTP 方法函数只做三件事：校验输入、调用 Service、返回响应。
- 不在 Route Handler 中直接调用 Repository 或 Supabase Client。
- 不使用 `export const dynamic = 'force-dynamic'` 等段配置，除非确有无状态渲染需求。

**Route Segment Config**：

```typescript
// 默认使用 dynamic，确保 API 数据实时
export const dynamic = 'force-dynamic';

// 如果 API 响应可以缓存一段时间（如热门榜单），使用 revalidate
export const revalidate = 60; // 60 秒
```

### 1.2 中间件

`backend/middleware.ts` 处理跨切面逻辑（cross-cutting concerns）：

```typescript
import { NextRequest, NextResponse } from 'next/server';
import { createServerClient } from '@supabase/ssr';

export async function middleware(request: NextRequest) {
  const start = Date.now();
  const traceId = crypto.randomUUID();

  // 1. CORS 处理
  const origin = request.headers.get('origin') ?? '';
  const allowedOrigins = process.env.CORS_ORIGINS?.split(',') ?? [];
  if (request.method === 'OPTIONS') {
    return new NextResponse(null, {
      headers: {
        'Access-Control-Allow-Origin': allowedOrigins.includes(origin) ? origin : '',
        'Access-Control-Allow-Methods': 'GET,POST,PUT,PATCH,DELETE',
        'Access-Control-Allow-Headers': 'Content-Type,Authorization',
        'Access-Control-Max-Age': '86400',
      },
    });
  }

  // 2. Auth 验证（白名单路径跳过）
  const publicPaths = ['/api/auth/login', '/api/auth/register', '/api/health'];
  const isPublicPath = publicPaths.some(p => request.nextUrl.pathname.startsWith(p));

  let userId: string | null = null;
  if (!isPublicPath) {
    const supabase = createServerClient(
      process.env.SUPABASE_URL!, process.env.SUPABASE_ANON_KEY!,
      { cookies: () => request.cookies },
    );
    const { data: { session } } = await supabase.auth.getSession();
    if (!session) {
      return NextResponse.json(
        { error: { code: 'UNAUTHORIZED', message: '未登录' } },
        { status: 401 },
      );
    }
    userId = session.user.id;
  }

  // 3. 注入 traceId 和 userId 到 header
  const headers = new Headers(request.headers);
  headers.set('x-trace-id', traceId);
  if (userId) headers.set('x-user-id', userId);

  // 4. Rate Limiting
  // 使用 rate-limiter-flexible 或 Redis 实现

  const response = NextResponse.next({ request: { headers } });

  // 5. 记录请求日志
  const duration = Date.now() - start;
  response.headers.set('x-response-time', `${duration}ms`);
  response.headers.set('x-trace-id', traceId);

  return response;
}

export const config = { matcher: '/api/:path*' };
```

**中间件职责**：
- **Auth**：验证 JWT Token，注入 `x-user-id`。
- **Logging**：生成 `x-trace-id`，记录请求耗时。
- **CORS**：处理 OPTIONS 预检请求，限制允许的 Origin。
- **Rate Limiting**：基于 IP 或用户 ID 实现频率限制（接入 `rate-limiter-flexible`）。

### 1.3 Server Actions

Server Actions 仅用于与前端表单的紧密集成场景（如页面中的搜索表单），不作为主要 API 方案：

```typescript
'use server';

import { z } from 'zod';
import { videoService } from '@/services/video-service';

const searchSchema = z.object({ keyword: z.string().min(1).max(100) });

export async function searchVideos(formData: FormData) {
  const parsed = searchSchema.safeParse({ keyword: formData.get('keyword') });
  if (!parsed.success) {
    return { error: '关键词格式不正确' };
  }
  const results = await videoService.search(parsed.data.keyword);
  return { data: results };
}
```

- Server Actions 不是 RESTful API 的替代方案，RESTful API 路由仍是主要的对外接口。
- Server Actions 应有与 Route Handler 相同的输入校验标准。

---

## 2. 数据校验

Zod 是整个 Backend 数据校验的基石，所有外部输入（用户请求、第三方回调、环境变量）必须经过 Zod schema 校验。

### 2.1 输入校验

**Schema 文件组织**：`backend/shared/schemas/` 目录下按领域分文件。

```typescript
// backend/shared/schemas/video.ts
import { z } from 'zod';

// 创建视频的 Schema
export const createVideoSchema = z.object({
  title: z.string().min(1, '标题不能为空').max(200, '标题最长200字'),
  description: z.string().max(2000).optional(),
  category: z.enum(['romance', 'revenge', 'comedy', 'action']),
  coverImageUrl: z.string().url('封面图必须为有效的 URL'),
  tags: z.array(z.string().max(20)).max(10).default([]),
}).strict(); // .strict() 拒绝未知字段

// 更新视频的 Schema（所有字段可选）
export const updateVideoSchema = createVideoSchema.partial();

// 查询参数 Schema
export const videoQuerySchema = z.object({
  cursor: z.string().optional(),
  limit: z.coerce.number().int().min(1).max(100).default(20),
  category: z.enum(['romance', 'revenge', 'comedy', 'action']).optional(),
  sort: z.enum(['created_at', 'view_count', 'rating']).default('created_at'),
  order: z.enum(['asc', 'desc']).default('desc'),
});

// 使用 z.coerce 处理 Query 参数（URL 中均为字符串）
```

**在 Route Handler 中的使用模式**：

```typescript
export async function GET(request: NextRequest) {
  // 解析 query 参数
  const params = Object.fromEntries(request.nextUrl.searchParams);
  const parsed = videoQuerySchema.safeParse(params);
  if (!parsed.success) {
    return NextResponse.json(
      { error: { code: 'VALIDATION_ERROR', details: parsed.error.flatten() } },
      { status: 400 },
    );
  }
  // parsed.data 是类型安全的
  const result = await videoService.list(parsed.data);
  return NextResponse.json(result);
}
```

- 始终使用 `safeParse` 而非 `parse`，避免抛出未捕获的异常。
- 使用 `.flatten()` 将 Zod 错误格式化为 `{ fieldErrors: Record<string, string[]> }` 结构，便于前端展示。

### 2.2 业务校验

Zod 不仅做格式校验，也可以做跨字段的业务规则校验：

```typescript
// 使用 refine 做跨字段校验
export const createPlaylistSchema = z.object({
  name: z.string().min(1).max(50),
  videoIds: z.array(z.string().uuid()).min(1, '至少添加1个视频').max(100, '最多100个视频'),
  isPublic: z.boolean(),
  publishAt: z.date().optional(),
}).refine(
  (data) => {
    // 定时发布的播单必须公开
    if (data.publishAt && !data.isPublic) return false;
    return true;
  },
  { message: '定时发布的播单必须设置为公开', path: ['isPublic'] },
);

// 使用 superRefine 做更复杂的校验（如查数据库）
export const registerSchema = z.object({
  phone: z.string().regex(/^1[3-9]\d{9}$/, '手机号格式不正确'),
  nickname: z.string().min(1).max(20),
}).superRefine(async (data, ctx) => {
  // 检查手机号是否已注册（需要注入 Repository）
  const exists = await userRepo.findByPhone(data.phone);
  if (exists) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: '该手机号已注册',
      path: ['phone'],
    });
  }
});
```

- 格式校验（长度、格式、范围）写在 Schema 定义中。
- 需要查数据库的业务校验使用 `superRefine`，并在调用时注入依赖（避免 Schema 直接依赖 Repository）。
- 跨字段校验（如 `endDate` 必须大于 `startDate`）使用 `refine`。

### 2.3 类型生成

所有 TypeScript 类型从 Zod schema 通过 `z.infer` 派生：

```typescript
// Schema 文件同时导出 Schema 和派生类型
export const videoSchema = z.object({ ... });
export type Video = z.infer<typeof videoSchema>;

export const createVideoSchema = z.object({ ... });
export type CreateVideoInput = z.infer<typeof createVideoSchema>;

// 数据库模型类型
export type DbVideo = z.infer<typeof videoSchema>;

// API 响应类型
export type ApiVideoResponse = { data: Video };
export type ApiVideoListResponse = { data: Video[]; meta: PaginationMeta };
```

- **禁止**手写 interface 再单独定义 Zod schema——这会导致类型不一致。
- `z.input<typeof schema>` 和 `z.output<typeof schema>` 在使用了 `z.coerce` 或 `transform` 时类型会不同，注意区分使用场景。
- API 响应类型在 `backend/shared/types/api.ts` 中单独定义，避免与数据库模型耦合。

---

## 3. 数据库访问

### 3.1 Supabase Client

**服务端使用**：

```typescript
// backend/shared/supabase/server.ts
import { createServerClient } from '@supabase/ssr';
import { cookies } from 'next/headers';

export function createClient() {
  const cookieStore = cookies();

  return createServerClient(
    process.env.SUPABASE_URL!,
    process.env.SUPABASE_ANON_KEY!,
    {
      cookies: {
        get(name) { return cookieStore.get(name)?.value; },
        set(name, value, options) { cookieStore.set({ name, value, ...options }); },
        remove(name, options) { cookieStore.set({ name, value: '', ...options }); },
      },
    },
  );
}
```

**Service Role 客户端**（仅限后台任务/脚本）：

```typescript
// backend/shared/supabase/admin.ts
import { createClient } from '@supabase/supabase-js';

export const supabaseAdmin = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!,
  { auth: { autoRefreshToken: false, persistSession: false } },
);
```

- 普通 API 请求使用 `createClient()`（尊重 RLS），后台任务/迁移脚本使用 `supabaseAdmin`。
- **严禁**在 Route Handler 中使用 Service Role Client，否则 RLS 形同虚设。

### 3.2 ORM (Prisma)

项目推荐使用 Prisma 作为 ORM，其 Schema 定义、Migration 生成和类型安全查询与 Supabase 配合良好。

**Prisma Schema**（`backend/prisma/schema.prisma`）：

```prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

model Video {
  id          String   @id @default(uuid()) @db.Uuid
  title       String   @db.VarChar(200)
  description String?  @db.Text
  category    String   @db.VarChar(50)
  coverImageUrl String @db.Text
  duration    Int
  viewCount   Int      @default(0)
  isPublished Boolean  @default(false)
  uploaderId  String   @db.Uuid
  createdAt   DateTime @default(now()) @db.Timestamptz()
  updatedAt   DateTime @updatedAt @db.Timestamptz()
  deletedAt   DateTime? @db.Timestamptz()

  uploader    User     @relation(fields: [uploaderId], references: [id])

  @@index([category, isPublished])
  @@index([uploaderId])
  @@index([createdAt])
  @@map("videos")
}

model User {
  id        String   @id @default(uuid()) @db.Uuid
  phone     String   @unique @db.VarChar(20)
  nickname  String   @db.VarChar(50)
  role      String   @default("user") @db.VarChar(20)
  createdAt DateTime @default(now()) @db.Timestamptz()
  updatedAt DateTime @updatedAt @db.Timestamptz()

  videos    Video[]

  @@map("users")
}
```

**Repository 中的查询与事务**：

```typescript
// backend/repositories/video-repository.ts
import { PrismaClient, Prisma } from '@prisma/client';

const prisma = new PrismaClient();

export class VideoRepository {
  async findById(id: string) {
    return prisma.video.findUnique({
      where: { id, deletedAt: null },
    });
  }

  async list(params: VideoQueryParams) {
    const { cursor, limit, category, sort, order } = params;

    const where: Prisma.VideoWhereInput = {
      deletedAt: null,
      isPublished: true,
      ...(category ? { category } : {}),
    };

    const videos = await prisma.video.findMany({
      where,
      orderBy: { [sort]: order },
      take: limit + 1, // 多取一条判断 hasMore
      ...(cursor ? { cursor: { id: cursor }, skip: 1 } : {}),
    });

    const hasMore = videos.length > limit;
    if (hasMore) videos.pop();

    return {
      data: videos,
      cursor: hasMore ? videos[videos.length - 1]?.id : null,
      hasMore,
    };
  }

  async create(data: Prisma.VideoCreateInput) {
    return prisma.video.create({ data });
  }

  // 事务示例：创建视频同时更新用户统计
  async createWithStats(data: Prisma.VideoCreateInput, userId: string) {
    return prisma.$transaction([
      prisma.video.create({ data }),
      prisma.user.update({
        where: { id: userId },
        data: { videoCount: { increment: 1 } },
      }),
    ]);
  }
}
```

### 3.3 查询优化

**避免 N+1 查询**：
- 使用 Prisma 的 `include` 或 `select` 预加载关联数据。
- 批量查询使用 `findMany` + `where: { id: { in: ids } }` 替代循环单条查询。
- 使用 `prisma.$transaction` 将多个写操作合并为一个事务，减少往返。

**连接池管理**：
- 开发环境使用 `connection_limit=5`，生产环境根据 Supabase 套餐调整。
- 避免在循环中大量创建 Prisma Client 实例——将 `PrismaClient` 实例化为单例（`globalThis.prisma ??= new PrismaClient()`）。

```typescript
// backend/shared/prisma.ts
import { PrismaClient } from '@prisma/client';

const globalForPrisma = globalThis as unknown as { prisma: PrismaClient };

export const prisma = globalForPrisma.prisma ?? new PrismaClient({
  log: process.env.NODE_ENV === 'development' ? ['query', 'error', 'warn'] : ['error'],
});

if (process.env.NODE_ENV !== 'production') globalForPrisma.prisma = prisma;
```

**批量操作**：
- 批量插入使用 `prisma.video.createMany({ data: [...] })`。
- 批量更新使用 `prisma.video.updateMany({ where: ..., data: ... })`。
- 大量数据的导入导出使用 `COPY` 命令或 Supabase CLI 的 `db dump`/`db push`。

---

## 4. 文件存储

### 4.1 Supabase Storage

```typescript
// backend/shared/supabase/admin.ts
import { createClient } from '@supabase/supabase-js';

export const supabaseAdmin = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!,
);

// 上传文件示例
async function uploadVideoCover(file: File, videoId: string) {
  const ext = file.name.split('.').pop();
  const path = `videos/${videoId}/cover.${ext}`;

  const { data, error } = await supabaseAdmin.storage
    .from('media')
    .upload(path, file, {
      cacheControl: '3600',
      upsert: true,
      contentType: file.type,
    });

  if (error) throw new Error(`Upload failed: ${error.message}`);

  // 获取公开 URL
  const { data: { publicUrl } } = supabaseAdmin.storage
    .from('media')
    .getPublicUrl(path);

  return publicUrl;
}
```

**Bucket 管理**：
- 按资源类型创建 Bucket，如 `media`（图片/视频）、`avatars`（头像）、`exports`（导出文件）。
- Bucket 策略：公开可读的图片/视频设为 `public`，私有文件（如用户上传的原始文件）设为 `private`。

### 4.2 图片处理

使用 `sharp` 进行服务端图片处理：

```typescript
import sharp from 'sharp';

async function generateThumbnails(inputPath: string, videoId: string) {
  const sizes = [
    { name: 'small', width: 240, height: 360 },
    { name: 'medium', width: 480, height: 720 },
    { name: 'large', width: 720, height: 1080 },
  ];

  const results: Record<string, string> = {};

  for (const { name, width, height } of sizes) {
    const outputPath = `videos/${videoId}/cover_${name}.webp`;
    const buffer = await sharp(inputPath)
      .resize(width, height, { fit: 'cover' })
      .webp({ quality: 80 })
      .toBuffer();

    await supabaseAdmin.storage.from('media').upload(outputPath, buffer, {
      contentType: 'image/webp',
      upsert: true,
    });

    results[name] = supabaseAdmin.storage.from('media').getPublicUrl(outputPath).data.publicUrl;
  }

  return results;
}
```

### 4.3 访问控制

- **公开文件**：直接使用 Storage 的 `getPublicUrl()` 获取永久 URL。
- **私有文件**：使用 `createSignedUrl(expiresIn)` 生成临时访问 URL，有效期按需设置（如 1 小时）。
- **上传权限**：用户只能上传到自己的命名空间 `{bucket}/{userId}/...`，通过 RLS Policy 限制。
- **文件大小限制**：在 Route Handler 中校验文件大小，视频不超过 500MB，图片不超过 10MB。

---

## 5. 日志系统

### 5.1 日志框架

使用 pino 作为日志框架，以 JSON 格式输出，提供结构化日志能力：

```typescript
// backend/shared/logger.ts
import pino from 'pino';

export const logger = pino({
  level: process.env.LOG_LEVEL ?? (process.env.NODE_ENV === 'production' ? 'info' : 'debug'),
  // 开发环境使用 pino-pretty 美化输出
  transport: process.env.NODE_ENV === 'development'
    ? { target: 'pino-pretty', options: { colorize: true } }
    : undefined,
  // 生产环境使用 JSON 格式（默认）便于日志平台采集
  serializers: {
    err: pino.stdSerializers.err,
    req: pino.stdSerializers.req,
  },
  // 注入基础字段
  base: {
    env: process.env.NODE_ENV,
    service: 'short-drama-backend',
  },
});
```

### 5.2 结构化日志

所有日志输出为 JSON，包含统一的字段：

```typescript
logger.info({
  traceId: 'uuid-xxx',
  userId: 'user-xxx',
  action: 'video.create',
  videoId: 'video-xxx',
  duration: 120,
}, 'Video created successfully');
```

统一字段说明：

| 字段 | 类型 | 说明 |
|------|------|------|
| `timestamp` | string (ISO 8601) | 自动注入 |
| `level` | string | trace/debug/info/warn/error/fatal |
| `message` | string | 人类可读的日志描述 |
| `traceId` | string (UUID) | 请求全链路追踪 ID |
| `userId` | string | 当前操作用户 ID（如有） |
| `action` | string | 业务操作标识，如 `video.create`、`payment.charge` |
| `duration` | number (ms) | 操作耗时 |

### 5.3 日志级别

| 级别 | 用途 | 生产环境默认 |
|------|------|-------------|
| `trace` | 最细粒度的调试信息（如函数进入/退出） | 关闭 |
| `debug` | 开发调试信息（如 SQL 查询、变量值） | 关闭 |
| `info` | 关键业务事件（如用户注册、支付成功） | 开启 |
| `warn` | 预期内的异常（如参数校验失败、登录失败） | 开启 |
| `error` | 系统错误（如数据库连接失败、未知异常） | 开启 |
| `fatal` | 致命错误（进程即将退出） | 开启 |

### 5.4 日志上报

- **本地开发**：使用 `pino-pretty` 输出到 stdout。
- **生产环境**：输出 JSON 到 stdout，由部署平台（Vercel/Docker）的日志采集器收集。
- **集中式日志**：接入日志平台（如 Datadog、Logtail、Axiom），通过 pino 的 transport 将日志推送到对应服务。
- **Supabase 日志**：Supabase 自身提供数据库慢查询日志和 Auth 日志，通过 Supabase Dashboard 查看。

---

## 6. 任务队列

### 6.1 异步任务

对于耗时操作（视频转码、推送通知、批量数据处理），使用 BullMQ 异步处理：

```typescript
// backend/queues/video-queue.ts
import { Queue, Worker } from 'bullmq';
import Redis from 'ioredis';

const connection = new Redis(process.env.REDIS_URL!, { maxRetriesPerRequest: null });

// 定义队列
export const videoQueue = new Queue('video-processing', { connection });

// 添加任务
await videoQueue.add('transcode', {
  videoId: 'xxx',
  sourceUrl: 'https://...',
  formats: ['720p', '1080p'],
}, {
  attempts: 3,
  backoff: { type: 'exponential', delay: 5000 },
});

// 处理任务
const worker = new Worker('video-processing', async (job) => {
  switch (job.name) {
    case 'transcode':
      await transcodeVideo(job.data);
      break;
    case 'generate-thumbnail':
      await generateThumbnail(job.data);
      break;
  }
}, { connection });
```

### 6.2 定时任务

使用 Supabase 的 `pg_cron` 或 BullMQ 的 `repeatable` 任务：

**BullMQ 定时任务**（应用层）：

```typescript
await videoQueue.add('cleanup-expired',
  { olderThanDays: 30 },
  { repeat: { pattern: '0 3 * * *' } }, // 每天凌晨 3 点执行
);
```

**pg_cron 定时任务**（数据库层，适合纯 SQL 操作）：

```sql
-- 每天清理 30 天前软删除的记录
SELECT cron.schedule(
  'cleanup-deleted-records',
  '0 4 * * *',
  'DELETE FROM videos WHERE deleted_at < NOW() - INTERVAL ''30 days'''
);
```

### 6.3 重试策略

- 使用 BullMQ 的指数退避重试：`attempts: 5, backoff: { type: 'exponential', delay: 2000 }`。
- 区分可重试错误（网络超时、服务暂时不可用）和不可重试错误（参数错误、数据不存在）。
- 设置死信队列（DLQ）：超过最大重试次数的任务移入 `failed` 状态，通过 Bull Board 监控和手动重放。
- 重要操作（如支付回调）实现幂等性——通过唯一键（如 `orderId`）防止重复处理。

---

## 7. 国际化 (i18n)

### 7.1 错误信息

后端错误信息按语言分别维护文案映射：

```typescript
// backend/shared/i18n/errors.ts
export const errorMessages = {
  VALIDATION_ERROR: {
    zh: '参数校验失败',
    en: 'Validation failed',
  },
  VIDEO_NOT_FOUND: {
    zh: '视频不存在',
    en: 'Video not found',
  },
  INSUFFICIENT_BALANCE: {
    zh: '余额不足',
    en: 'Insufficient balance',
  },
} as const;

export function getErrorMessage(code: string, locale: 'zh' | 'en' = 'zh'): string {
  return errorMessages[code]?.[locale] ?? errorMessages.VALIDATION_ERROR[locale];
}
```

- 默认使用中文（`zh`），客户端通过 `Accept-Language` Header 或 `?locale=en` 参数指定语言。

### 7.2 内容国际化

数据库中需要多语言的字段采用以下两种方案之一：

**方案 A：JSON 字段**（适用于字段数量不确定的内容）：

```sql
CREATE TABLE categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name JSONB NOT NULL DEFAULT '{}',  -- { "zh": "甜宠", "en": "Romance" }
  created_at TIMESTAMPTZ DEFAULT now()
);
```

```typescript
const categorySchema = z.object({
  id: z.string().uuid(),
  name: z.record(z.string()),  // { zh: string, en: string }
});
```

**方案 B：翻译表**（适用于翻译量大、需要翻译工作流的场景）：

```sql
CREATE TABLE videos (
  id UUID PRIMARY KEY,
  title_en TEXT,
  title_zh TEXT NOT NULL,
  ...
);
```

- 对于竖屏短剧平台，字段数量有限，优先使用方案 A（JSONB 字段），简单灵活。
