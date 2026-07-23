# 代码规范 — Backend

> 本文档定义 Backend 端 TypeScript + Next.js 服务端的完整编码规范。

---

## 1. TypeScript 编码规范

本规范适用于所有 `backend/` 下的 TypeScript 代码。编码时优先遵守本规范，其次参考 `tsconfig.json` 中的 compilerOptions。

### 1.1 严格模式

`tsconfig.json` 必须开启以下严格检查：

```json
{
  "compilerOptions": {
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "exactOptionalPropertyTypes": false
  }
}
```

- `strict: true` 会同时开启 `strictNullChecks`、`noImplicitAny`、`strictFunctionTypes` 等子选项，是类型安全的基础。
- `noUncheckedIndexedAccess` 强制对索引访问（如 `arr[0]`、`obj['key']`）的结果追加 `| undefined`，避免遗漏空值检查。
- `exactOptionalPropertyTypes` 保持关闭，因为在 Zod schema 中常见 `z.string().optional()` 对应的 `field?: string` 在传递时可能与 `{ field: undefined }` 冲突。

### 1.2 类型定义

**类型文件组织**：

- 数据库模型类型定义在 `backend/shared/types/db.ts`，使用 `z.infer<typeof schema>` 从 Zod schema 派生。
- API 请求/响应类型定义在 `backend/shared/types/api.ts`。
- 通用业务类型定义在 `backend/shared/types/common.ts`。
- 每类类型使用独立的 namespace 或前缀以避免命名冲突，例如 `DbVideo`、`ApiCreateVideoRequest`。

**Zod schema 与 TypeScript 类型映射**：

```typescript
// backend/shared/schemas/video.ts
import { z } from 'zod';

export const videoSchema = z.object({
  id: z.string().uuid(),
  title: z.string().min(1).max(200),
  category: z.enum(['romance', 'revenge', 'comedy', 'action']),
  duration: z.number().int().positive(),
  createdAt: z.date(),
});

// 自动推导 TypeScript 类型，禁止手写重复类型
export type Video = z.infer<typeof videoSchema>;
```

- 所有数据模型的唯一真相来源（Single Source of Truth）是 Zod schema，TypeScript 类型一律通过 `z.infer` 派生，禁止手写 interface 后再定义 Zod schema。
- Schema 文件统一放在 `backend/shared/schemas/` 目录，按领域分文件。

### 1.3 命名约定

| 元素 | 规范 | 示例 |
|------|------|------|
| 变量 | camelCase | `videoList`, `totalCount` |
| 函数 | camelCase | `getVideoById`, `createPlaylist` |
| 类 | PascalCase | `VideoService`, `UserRepository` |
| 类型/接口 | PascalCase | `Video`, `PaginatedResponse<T>` |
| 常量 | UPPER_SNAKE_CASE | `MAX_VIDEO_DURATION`, `DEFAULT_PAGE_SIZE` |
| 枚举 | PascalCase | `VideoCategory`, `UserRole` |
| 文件名 | kebab-case | `video-service.ts`, `user-repository.ts` |
| 布尔变量 | is/has/can 前缀 | `isPublished`, `hasAccess`, `canEdit` |
| 私有属性 | 不使用 `_` 前缀，用 `private` 关键字 | `private readonly client: SupabaseClient` |

### 1.4 禁止 any

- **严禁使用 `any` 类型**，包括 `as any` 断言。代码审查中发现 `any` 即为阻塞项。
- 对类型不确定的场景使用 `unknown`，并配合类型守卫（type guard）收窄：

```typescript
function parseApiResponse(raw: unknown): Video {
  if (typeof raw !== 'object' || raw === null) {
    throw new Error('Invalid response');
  }
  const result = videoSchema.safeParse(raw);
  if (!result.success) {
    throw new ValidationError(result.error);
  }
  return result.data;
}
```

- 第三方库类型不完整时，在 `backend/shared/types/external.d.ts` 中补充 `.d.ts` 声明，不得用 `any` 绕过。
- ESLint 中配置 `@typescript-eslint/no-explicit-any: 'error'` 强制拦截。

---

## 2. API 路由编码规范

Next.js App Router 下的 Route Handler 是 API 入口，必须遵循本规范。

### 2.1 文件约定

- 路由文件统一命名为 `route.ts`，放在 `backend/app/api/` 下的对应路径。
- 每个 `route.ts` 导出对应 HTTP 方法的具名函数：`export async function GET(...)`、`POST`、`PUT`、`PATCH`、`DELETE`。
- 不要在一个 `route.ts` 中混合不相关的 HTTP 方法。如果某个资源的某个方法逻辑复杂，可抽取到 `service` 层。
- 路由文件夹使用动态段 `[id]` 表示路径参数。

```
backend/app/api/
├── videos/
│   ├── route.ts          → GET /api/videos, POST /api/videos
│   └── [id]/
│       └── route.ts      → GET /api/videos/:id, PUT /api/videos/:id, DELETE /api/videos/:id
```

### 2.2 请求处理

每个 Route Handler 按以下模板编写：

```typescript
import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';
import { createVideoSchema } from '@/shared/schemas/video';

export async function POST(request: NextRequest) {
  try {
    // 1. 解析请求体
    const body: unknown = await request.json();

    // 2. Zod 校验，使用 safeParse 以获取结构化错误
    const parsed = createVideoSchema.safeParse(body);
    if (!parsed.success) {
      return NextResponse.json(
        { error: { code: 'VALIDATION_ERROR', details: parsed.error.flatten() } },
        { status: 400 },
      );
    }

    // 3. 调用 Service 层
    const video = await videoService.create(parsed.data);

    // 4. 返回成功响应
    return NextResponse.json({ data: video }, { status: 201 });
  } catch (error) {
    // 5. 统一错误处理（由全局 error handler 或 middleware 兜底）
    return NextResponse.json(
      { error: { code: 'INTERNAL_ERROR', message: 'Unexpected error' } },
      { status: 500 },
    );
  }
}
```

- **动态参数**通过 `params` 访问：`params: { id: string }`。
- **Query 参数**通过 `request.nextUrl.searchParams` 获取。
- **Header** 通过 `request.headers.get('Authorization')` 获取。
- 请求体只能调用一次 `await request.json()`，如需多次读取应提前保存。

### 2.3 响应格式

所有 API 响应必须使用统一的 JSON 结构：

```typescript
// 成功响应
{ "data": T }

// 列表响应（含分页信息）
{ "data": T[], "meta": { "cursor": string | null, "hasMore": boolean } }

// 错误响应
{ "error": { "code": string, "message": string, "details"?: unknown } }
```

- 使用 `NextResponse.json()` 构造响应，不要手动 `new Response()`。
- 创建资源成功统一返回 201，不需返回内容时返回 204（body 为空）。
- 不要直接在 Route Handler 中拼接 SQL 或操作 Prisma——这些逻辑应在 Repository 中完成。

---

## 3. 命名规范

### 3.1 路由命名

- 资源名称使用**名词复数**，例如 `/api/videos`、`/api/users`、`/api/playlists`。
- 路径段使用 **kebab-case**，例如 `/api/video-comments` 而非 `/api/videoComments`。
- 嵌套资源用层级结构表示：`/api/playlists/{playlistId}/videos`。
- 非 CRUD 动作使用子资源路径：`/api/videos/{id}/like`，在 body 中指定动作而非放在 URL 里。
- 避免深层嵌套超过 3 层，深层关联使用查询参数：`/api/videos?playlistId=xxx`。

### 3.2 数据库命名

- 表名使用 **snake_case 复数**，例如 `videos`、`user_watch_history`。
- 字段名使用 **snake_case**，例如 `created_at`、`updated_at`、`cover_image_url`。
- 主键统一命名为 `id`，类型为 `uuid`。
- 外键命名为 `<关联表单数>_id`，例如 `playlist_id`、`uploader_id`。
- 时间戳字段统一使用 `created_at` 和 `updated_at`，类型为 `timestamptz`。
- 布尔字段使用 `is_` 前缀，例如 `is_published`、`is_deleted`（软删除）。
- 枚举字段使用 `snake_case` 值，例如 `'romance'`、`'revenge'`。

### 3.3 环境变量

- 环境变量按用途分组并使用前缀：

| 前缀 | 用途 | 示例 |
|------|------|------|
| `NEXT_PUBLIC_` | 仅前端可读 | `NEXT_PUBLIC_APP_URL` |
| `SUPABASE_` | Supabase 相关 | `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` |
| `DATABASE_` | 数据库连接 | `DATABASE_URL` |
| `REDIS_` | Redis 连接 | `REDIS_URL` |
| `STORAGE_` | 对象存储 | `STORAGE_BUCKET_NAME` |
| `SECRET_` | 密钥/Token | `SECRET_JWT_SIGNING_KEY` |
| `API_` | 外部 API Key | `API_RESEND_KEY` |

- 服务端专用变量**严禁**使用 `NEXT_PUBLIC_` 前缀，否则会被打包暴露到前端。

---

## 4. 代码审查清单

每次 CR 必须通过以下检查项，全部通过方可合并：

- [ ] **类型安全**：无 `any` 类型，无 `as` 强制类型断言，无 `@ts-ignore` 注释。
- [ ] **输入校验**：所有 API 输入（body、query、params）均经过 Zod schema 的 `safeParse` 校验。
- [ ] **错误处理**：每个 Route Handler 有 try-catch 或由全局 error handler 兜底，不向客户端暴露堆栈信息。
- [ ] **分层正确**：Route Handler 不直接操作数据库或调用第三方 API，必须通过 Service 层。
- [ ] **响应格式**：使用统一的 `{ data }` / `{ error }` JSON 结构。
- [ ] **SQL 安全**：所有数据库查询使用参数化查询（通过 Supabase Client 或 Prisma），禁止拼接 SQL 字符串。
- [ ] **无硬编码**：不得出现硬编码的 URL、Token、连接字符串，一律从环境变量读取。
- [ ] **命名规范**：文件 kebab-case、变量 camelCase、类型 PascalCase、常量 UPPER_SNAKE_CASE。
- [ ] **测试覆盖**：新增业务逻辑、参数校验规则有对应的单元测试，新增 API 路由有集成测试。
- [ ] **无 console.log**：禁止提交 `console.log`，调试日志使用 pino 的 `logger.debug()`。
- [ ] **迁移文件**：数据库 Schema 变更必须通过 Supabase Migration 管理，附带 up/down 脚本。
