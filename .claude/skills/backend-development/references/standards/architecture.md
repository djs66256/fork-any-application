# 架构设计 — Backend

> 本文档定义 Backend 端的整体架构设计规范。

---

## 1. 整体架构

Backend 端采用经典的三层架构，职责清晰分层，确保代码可维护、可测试、可替换。

```
Route Handler (API Layer)
    ↓
Service (Business Logic)
    ↓
Repository (Data Access)
    ↓
Database (Supabase / PostgreSQL)
```

### 各层职责

**Route Handler（API 层 / 接口层）**

位置：`backend/app/api/` 下的 `route.ts` 文件。

职责：
- 解析 HTTP 请求（body、query、params、headers）。
- 使用 Zod schema 对输入进行校验，不通过则返回 400。
- 调用 Service 层方法，组装响应。
- 处理 HTTP 层面的关切（状态码、Header、CORS）。
- **禁止**包含业务逻辑、数据库查询、第三方 API 调用。

**Service（业务逻辑层）**

位置：`backend/services/`，按领域分文件，如 `video-service.ts`、`user-service.ts`。

职责：
- 实现所有业务规则和流程编排。
- 调用 Repository 层完成数据读写。
- 调用第三方服务（支付、推送、邮件）的 Client 封装。
- 处理事务边界（跨多个 Repository 操作时的数据一致性）。
- 抛出语义化的业务异常（如 `NotFoundError`、`ForbiddenError`、`ConflictError`）。

**Repository（数据访问层）**

位置：`backend/repositories/`，按表/领域分文件，如 `video-repository.ts`。

职责：
- 封装所有数据库查询（Supabase Client 或 Prisma）。
- 对外暴露类型安全的查询方法，方法签名只接受和返回业务类型（非原始 SQL 结果）。
- 处理数据映射（snake_case 数据库字段 ↔ camelCase 业务对象）。
- **禁止**包含业务逻辑或 HTTP 层面的概念。

**Database（数据库层）**

- 使用 Supabase 托管的 PostgreSQL。
- Schema 通过 Migration 管理，Migration 文件在 `backend/supabase/migrations/` 下。
- Row Level Security（RLS）策略定义在 Migration 中，不依赖应用层检查。

### 目录结构

```
backend/
├── app/api/               # Route Handler（API 层）
│   └── [resource]/route.ts
├── services/              # 业务逻辑层
│   └── [domain]-service.ts
├── repositories/          # 数据访问层
│   └── [domain]-repository.ts
├── shared/
│   ├── schemas/           # Zod schema（数据模型的唯一真相来源）
│   ├── types/             # TypeScript 类型（从 Zod 派生）
│   └── utils/             # 工具函数
├── supabase/
│   └── migrations/        # 数据库迁移文件
└── middleware.ts           # Next.js 中间件（Auth、Logging、CORS）
```

---

## 2. API 设计规范

### 2.1 路由设计

**资源命名**：
- 使用名词复数：`/api/videos`、`/api/playlists`、`/api/users`。
- 路径段使用 kebab-case：`/api/watch-history` 而非 `/api/watchHistory`。

**层级关系**：
- 一对多关系用嵌套路由：`/api/playlists/{playlistId}/videos`。
- 多对多关系用独立资源配合查询参数：`/api/video-tags?videoid=xxx`。
- 嵌套深度不超过 3 层，超出时拆分为顶层资源。

**集合 vs 单资源**：
- `GET /api/videos` → 获取视频列表（集合）。
- `GET /api/videos/{id}` → 获取单个视频（单资源）。
- 批量操作通过集合端点实现：`POST /api/videos/batch`（在 body 中指定操作和 ID 列表）。

### 2.2 请求格式

**Query 参数**（筛选、排序、分页）：
- 分页：`?cursor=xxx&limit=20`（基于 cursor）或 `?page=1&limit=20`（基于页码）。
- 排序：`?sort=created_at&order=desc`。
- 筛选：`?category=romance&is_published=true`。

**Path 参数**：
- 资源 ID 使用 UUID 格式：`/api/videos/550e8400-e29b-41d4-a716-446655440000`。
- 通过 `params` 对象在 Route Handler 中接收。

**Request Body**：
- 使用 JSON 格式，`Content-Type: application/json`。
- POST/PUT/PATCH 请求的 Body 由 Zod schema 校验，不接受非预期的字段（使用 `.strict()`）。
- 布尔值使用 `true`/`false`，不要使用 `0`/`1` 或字符串 `"true"`。

### 2.3 响应格式

所有响应使用统一的 JSON 结构：

```typescript
// 单资源响应
{
  "data": { "id": "uuid", "title": "...", ... }
}

// 列表响应（cursor 分页）
{
  "data": [ ... ],
  "meta": {
    "cursor": "next-page-cursor",
    "hasMore": true,
    "total"?: number  // 仅在需要总数时返回，注意 COUNT 查询的性能开销
  }
}

// 错误响应
{
  "error": {
    "code": "VALIDATION_ERROR",    // 机器可读的错误码
    "message": "标题不能为空",      // 人类可读的提示信息
    "details"?: { ... }           // 可选的结构化错误详情
  }
}
```

- 使用 `NextResponse.json()` 构造响应。
- 不要将 Supabase/Prisma 的内部错误直接返回给客户端。
- 错误响应中不要包含堆栈信息（生产环境）。

### 2.4 状态码

| 状态码 | 语义 | 使用场景 |
|--------|------|----------|
| 200 OK | 请求成功 | GET、PUT、PATCH 成功时 |
| 201 Created | 资源已创建 | POST 成功后，附带 Location Header |
| 204 No Content | 成功但无返回体 | DELETE 成功后 |
| 400 Bad Request | 请求参数错误 | Zod 校验失败、参数格式不正确 |
| 401 Unauthorized | 未认证 | 缺少 Token 或 Token 无效/过期 |
| 403 Forbidden | 无权限 | 已认证但权限不足（如非管理员操作） |
| 404 Not Found | 资源不存在 | 查询/更新/删除不存在的资源 |
| 409 Conflict | 资源冲突 | 唯一索引冲突（重复创建）、版本冲突 |
| 422 Unprocessable Entity | 语义有误 | 参数格式正确但业务规则不满足 |
| 429 Too Many Requests | 限流 | 超过频率限制 |
| 500 Internal Server Error | 服务端错误 | 未预期的运行时错误 |

### 2.5 错误码体系

业务错误码使用大写蛇形命名，格式为 `<模块>_<错误类型>`：

```typescript
// backend/shared/error-codes.ts
export const ErrorCode = {
  // 通用
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  NOT_FOUND: 'NOT_FOUND',
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  CONFLICT: 'CONFLICT',
  INTERNAL_ERROR: 'INTERNAL_ERROR',
  RATE_LIMITED: 'RATE_LIMITED',

  // 视频相关
  VIDEO_NOT_FOUND: 'VIDEO_NOT_FOUND',
  VIDEO_PROCESSING_FAILED: 'VIDEO_PROCESSING_FAILED',
  VIDEO_UPLOAD_TOO_LARGE: 'VIDEO_UPLOAD_TOO_LARGE',

  // 用户相关
  USER_NOT_FOUND: 'USER_NOT_FOUND',
  PHONE_ALREADY_REGISTERED: 'PHONE_ALREADY_REGISTERED',
  INVALID_VERIFICATION_CODE: 'INVALID_VERIFICATION_CODE',

  // 支付相关
  INSUFFICIENT_BALANCE: 'INSUFFICIENT_BALANCE',
  PAYMENT_FAILED: 'PAYMENT_FAILED',
} as const;
```

- 错误信息通过 locale 参数支持多语言：`message_zh`、`message_en`。
- 国际化文案映射在 `backend/shared/i18n/errors.ts` 中维护。

### 2.6 分页

项目优先使用 **cursor-based 分页**（适合无限滚动场景），必要时才使用 page-based 分页。

**Cursor-based 分页**（推荐，适用于时间线、Feed 流）：

```typescript
// 请求：GET /api/videos?cursor=xxx&limit=20
const cursorSchema = z.object({
  cursor: z.string().optional(),
  limit: z.coerce.number().int().min(1).max(100).default(20),
});

// 响应
{
  "data": [...],
  "meta": {
    "cursor": "eyJsYXN0SWQiOiJ1dWlkLTEyMyJ9",  // base64 编码的游标
    "hasMore": true
  }
}
```

- cursor 基于 `id` 或 `created_at` + `id` 组合生成，用 base64 编码。
- 后端始终多取一条记录（limit + 1）来判断 `hasMore`。

**Page-based 分页**（适用于管理后台）：

```typescript
// 请求：GET /api/admin/videos?page=1&limit=20
{
  "data": [...],
  "meta": {
    "page": 1,
    "limit": 20,
    "total": 156,
    "totalPages": 8
  }
}
```

- `page` 从 1 开始计数。
- `total` 需要 COUNT 查询，在数据量大的表上注意索引和性能。

### 2.7 版本管理

- API 版本通过 **URL 前缀** 管理：`/api/v1/videos`、`/api/v2/videos`。
- 初始阶段无需版本号（即 `/api/videos` 等同于 v1），在需要 breaking change 时引入 `/api/v2/`。
- 旧版本保持至少 3 个月的兼容期，通过 `Deprecation` Header 和文档告知客户端迁移。
- 不使用 Header-based 版本管理（如 `Accept: application/vnd.api+json;version=2`），因为调试和文档生成不便。

### 3. 认证与授权

项目使用 Supabase Auth 作为认证服务，结合 Next.js Middleware 和 RLS 实现完整的鉴权链路。

### 3.1 Supabase Auth

**用户注册/登录**：
- 支持手机号 + 验证码登录（主要方式，适用于竖屏短剧用户）。
- 支持第三方 OAuth（微信、Apple ID 等）。
- 注册流程：用户请求验证码 → Supabase 发送短信 → 用户提交验证码 → Supabase 验证并返回 JWT。
- JWT 包含 `sub`（用户 UUID）、`role`、`exp` 等标准字段。

**Session 管理**：
- 使用 Supabase 的 `supabase.auth.getSession()` 在服务端验证 Token。
- Token 通过 `Authorization: Bearer <jwt>` Header 传递。
- Access Token 有效期 1 小时，Refresh Token 有效期 30 天。

**MFA（多因素认证）**：
- 管理后台操作（如财务、内容审核）建议开启 MFA。
- 使用 TOTP（Time-based One-Time Password）方式。

### 3.2 Middleware 鉴权

`backend/middleware.ts` 是所有 API 请求的鉴权入口：

```typescript
import { createServerClient } from '@supabase/ssr';
import { NextRequest, NextResponse } from 'next/server';

export async function middleware(request: NextRequest) {
  const supabase = createServerClient(
    process.env.SUPABASE_URL!,
    process.env.SUPABASE_ANON_KEY!,
    { cookies: () => request.cookies }
  );

  const { data: { session } } = await supabase.auth.getSession();

  // 公开路由白名单（如登录、注册、health check）
  const publicPaths = ['/api/auth', '/api/health'];
  const isPublicPath = publicPaths.some(p => request.nextUrl.pathname.startsWith(p));

  if (!session && !isPublicPath) {
    return NextResponse.json(
      { error: { code: 'UNAUTHORIZED', message: '请先登录' } },
      { status: 401 },
    );
  }

  // 将用户信息注入 request header，方便下游 Route Handler 使用
  const requestHeaders = new Headers(request.headers);
  requestHeaders.set('x-user-id', session?.user.id ?? '');
  requestHeaders.set('x-user-role', session?.user.role ?? '');

  return NextResponse.next({ request: { headers: requestHeaders } });
}

export const config = {
  matcher: '/api/:path*',
};
```

**路由保护策略**：
- 所有 `/api/*` 路径默认需要认证，公开路由在白名单中声明。
- 从 middleware 注入 `x-user-id` 和 `x-user-role` header，下游 Route Handler 通过 `request.headers.get('x-user-id')` 获取当前用户，避免在每个 Handler 中重复验证 Token。

### 3.3 RBAC

**角色定义**：

| 角色 | 标识 | 权限范围 |
|------|------|----------|
| User（普通用户） | `user` | 浏览视频、点赞、评论、个人中心 |
| Creator（创作者） | `creator` | 上传视频、管理自己的内容 |
| Admin（管理员） | `admin` | 内容审核、用户管理、系统配置 |
| SuperAdmin（超级管理员） | `super_admin` | 管理员账号管理、敏感操作 |

**权限检查**：
- 在 Service 层做权限校验，而非在 Route Handler 或 Repository 中。
- 使用声明式 API：

```typescript
// backend/services/guards.ts
export function requireRole(user: AuthenticatedUser, ...roles: UserRole[]): void {
  if (!roles.includes(user.role)) {
    throw new ForbiddenError('权限不足');
  }
}

// 使用
async function deleteVideo(user: AuthenticatedUser, videoId: string) {
  const video = await videoRepo.findById(videoId);
  if (!video) throw new NotFoundError('视频不存在');

  requireRole(user, 'admin', 'super_admin');
  // 或检查所有权：if (video.uploaderId !== user.id) requireRole(user, 'admin');

  await videoRepo.softDelete(videoId);
}
```

**Row Level Security (RLS)**：
- 数据库层面使用 PostgreSQL RLS Policy 作为第二道防线。
- 示例：用户只能读取已发布的视频和自己的草稿。

```sql
ALTER TABLE videos ENABLE ROW LEVEL SECURITY;

CREATE POLICY "任何人可读已发布视频" ON videos
  FOR SELECT USING (is_published = true);

CREATE POLICY "创作者可读取自己的草稿" ON videos
  FOR SELECT USING (auth.uid() = uploader_id);

CREATE POLICY "创作者可更新自己的视频" ON videos
  FOR UPDATE USING (auth.uid() = uploader_id);
```

---

## 4. 数据库设计

### 4.1 Schema 设计

- 满足第三范式（3NF），除非有明确的性能原因才做反范式化（需在注释中说明）。
- 主键统一使用 `uuid` 类型，默认值 `gen_random_uuid()`。
- 所有表必须包含 `id`（主键）、`created_at`、`updated_at` 三个标准字段。
- 支持软删除的表添加 `deleted_at timestamptz` 字段，`NULL` 表示未删除。
- 金额字段使用 `integer` 类型存储分为单位的值，避免浮点精度问题。
- 枚举类型在数据库中存储为 `text`，值的约束通过应用层 Zod schema 和数据库 CHECK 约束双重保证。
- 外键必须显式声明 `FOREIGN KEY ... REFERENCES ...`。
- 不要使用数据库的级联删除（`ON DELETE CASCADE`），逻辑删除由应用层处理。

### 4.2 索引策略

**索引类型选择**：
- 主键/外键 → B-tree 索引（PostgreSQL 默认）。
- 全文搜索 → GIN 索引 + `tsvector`。
- 数组字段包含查询 → GIN 索引。
- 地理位置查询 → GiST 索引。

**复合索引原则**：
- 最常用的查询条件放在复合索引的最左侧。
- 对 `WHERE a = ? AND b = ? ORDER BY c` 创建 `(a, b, c)` 复合索引。
- 使用 `EXPLAIN ANALYZE` 验证索引是否被使用，再做优化决策。

**部分索引（Partial Index）**：
- 对软删除表只索引 `WHERE deleted_at IS NULL`，缩小索引体积。
- 对状态字段的少量值创建索引，如 `WHERE status = 'pending'`。

**索引命名**：
- `idx_<表名>_<字段名>`，如 `idx_videos_category`。
- 复合索引 `idx_<表名>_<字段1>_<字段2>`，如 `idx_watch_history_user_created`。

### 4.3 迁移管理

使用 Supabase CLI 进行数据库迁移管理：

```bash
# 创建新的迁移文件
supabase migration new add_video_tags_table

# 应用迁移到本地数据库
supabase db reset

# 推送到远程 Supabase 项目
supabase db push
```

**迁移文件规范**：
- 每个迁移文件包含 `-- up` 和 `-- down` 两部分（写在注释中），`up` 是正向变更，`down` 是回滚脚本。
- 迁移文件名格式：`YYYYMMDDHHmmss_descriptive_name.sql`。
- 每个迁移只做一件事：不要在一个迁移中同时加表和改索引。
- 迁移文件必须可重复执行（idempotent）：使用 `IF NOT EXISTS` / `IF EXISTS`。
- 禁止手动修改已推送到远程的迁移文件，如有问题创建新的迁移修复。

**回滚策略**：
- 开发期：`supabase db reset` 重置到最新 schema。
- 生产环境：不回滚已应用的迁移（避免数据丢失），通过新迁移修复问题。
- 重大变更前先在 staging 环境完整验证。

### 4.4 Row Level Security (RLS)

- 所有包含用户数据的表**必须**启用 RLS（`ALTER TABLE ... ENABLE ROW LEVEL SECURITY`）。
- RLS Policy 通过 Migration 管理，与应用代码一起版本化。
- Policy 命名格式：`<动作>_<条件>`，如 `select_published`、`update_own_videos`。
- 测试 RLS：在集成测试中使用不同角色的 Supabase Client 验证 Policy 是否生效。
- Service Role Key 绕过 RLS，因此**严禁**在 API Route Handler 中使用 Service Role Client——仅在后台任务、迁移脚本等受控场景使用。

---

## 5. 缓存策略

### 5.1 应用缓存

- 使用 Redis（通过 `ioredis` 或 `@upstash/redis`）作为应用层缓存。
- 缓存 Key 设计格式：`<服务>:<实体>:<标识>`，例如 `video:detail:uuid-123`、`user:profile:uuid-456`。
- 设置合理的 TTL：热点数据 TTL 较短（如首页推荐列表 5 分钟），冷数据 TTL 较长（如视频详情 1 小时）。
- 缓存序列化：使用 `JSON.stringify` / `JSON.parse`，避免存储不可序列化的对象。

### 5.2 HTTP 缓存

- 使用 `Cache-Control` Header 控制客户端缓存行为。
  - 公开不变资源：`Cache-Control: public, max-age=31536000, immutable`。
  - 用户相关数据：`Cache-Control: private, no-cache`。
- 使用 `ETag` 支持条件请求（`If-None-Match`），减少不必要的数据传输。
- 利用 Next.js 的 `fetch` 缓存（`next: { revalidate: 60 }`）对静态或低频变化数据进行服务端缓存。

### 5.3 数据库缓存

- 利用 PostgreSQL 的 `MATERIALIZED VIEW` 缓存复杂聚合查询结果（如排行榜、统计面板）。
- 使用 `pg_cron` 或应用层定时任务定期刷新物化视图。
- Supabase 自动缓存常用查询结果，但不要依赖此行为——在性能敏感场景显式设计缓存。

### 5.4 缓存失效

- **TTL 策略**：为所有缓存 Key 设置 TTL，通过惰性过期（访问时检查）+ 主动过期（TTL 到期自动删除）结合。
- **主动失效**：数据变更时，在 Service 层同步删除相关缓存 Key。
  ```typescript
  async function updateVideo(id: string, data: UpdateVideoInput) {
    const video = await videoRepo.update(id, data);
    await cache.del(`video:detail:${id}`);
    await cache.del('video:hot-list');
    return video;
  }
  ```
- **Cache-Aside 模式**：读数据时先查缓存，未命中则查数据库并回填缓存。
- **缓存穿透防护**：对不存在的数据也缓存一个空值标记（TTL 较短），防止恶意请求穿透到数据库。

---

## 6. 错误处理

### 6.1 错误分类

将错误分为四类，每类有明确的处理策略：

| 类别 | 示例 | HTTP 状态码 | 处理方式 |
|------|------|-------------|----------|
| 输入错误 | Zod 校验失败、参数格式错误 | 400/422 | 返回结构化错误详情给客户端 |
| 业务错误 | 余额不足、重复点赞、权限不足 | 409/403 | 返回业务错误码，提示用户如何修正 |
| 系统错误 | 数据库连接失败、Redis 超时 | 500 | 记录完整日志，返回通用错误信息 |
| 第三方错误 | 支付网关超时、短信发送失败 | 502/503 | 区分可重试和不可重试，实现降级策略 |

自定义错误类：

```typescript
// backend/shared/errors.ts
export class AppError extends Error {
  constructor(
    public readonly code: string,
    public readonly httpStatus: number,
    message: string,
    public readonly details?: unknown,
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export class NotFoundError extends AppError {
  constructor(resource: string, id?: string) {
    super('NOT_FOUND', 404, id ? `${resource} (${id}) 不存在` : `${resource} 不存在`);
  }
}

export class ForbiddenError extends AppError {
  constructor(message = '权限不足') { super('FORBIDDEN', 403, message); }
}

export class ConflictError extends AppError {
  constructor(message: string) { super('CONFLICT', 409, message); }
}

export class ValidationError extends AppError {
  constructor(details: unknown) { super('VALIDATION_ERROR', 400, '参数校验失败', details); }
}
```

### 6.2 全局错误处理

- 每个 Route Handler 使用统一的 try-catch 模式，捕获 `AppError` 并返回对应 HTTP 状态码。
- 未预期的错误在 catch 中记录日志后返回 500，不暴露堆栈信息。
- 可选使用 Next.js 的 `error.tsx` 模式——在 `app/api/error.ts` 中定义全局错误边界。
- 推荐在 `backend/shared/route-utils.ts` 中封装一个 `withErrorHandler` 工具函数：

```typescript
import { NextRequest, NextResponse } from 'next/server';
import { AppError } from '@/shared/errors';
import { logger } from '@/shared/logger';

type Handler = (req: NextRequest, ...args: unknown[]) => Promise<NextResponse>;

export function withErrorHandler(handler: Handler): Handler {
  return async (req, ...args) => {
    try {
      return await handler(req, ...args);
    } catch (error) {
      if (error instanceof AppError) {
        return NextResponse.json(
          { error: { code: error.code, message: error.message, details: error.details } },
          { status: error.httpStatus },
        );
      }
      logger.error({ err: error, path: req.nextUrl.pathname }, 'Unhandled error');
      return NextResponse.json(
        { error: { code: 'INTERNAL_ERROR', message: '服务器内部错误' } },
        { status: 500 },
      );
    }
  };
}
```

### 6.3 日志与追踪

- 使用 pino 记录结构化 JSON 日志。
- 每个请求在 middleware 中注入 `x-trace-id`（UUID v7），全链路透传。
- 错误日志必须包含 `traceId`、`userId`（如果已认证）、`path`、`method`、`error.stack`。
- 在 Service 层捕获业务异常时，使用 `logger.warn()` 记录（预期内的错误）；系统异常使用 `logger.error()` 记录。
- 生产环境使用 `logger.error()`，开发环境可同时使用 `logger.debug()` 输出详细上下文。
