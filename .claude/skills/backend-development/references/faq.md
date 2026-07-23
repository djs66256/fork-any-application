# 常见问题 — Backend

> 本文档收集 Backend 开发中的常见问题与解决方案。

---

## 构建问题

### Q1: `npm run build` 报错 "Module not found: Can't resolve 'pino'"

**错误现象**：
```
Module not found: Can't resolve 'pino' in '/backend/shared'
```

**原因**：Next.js 在构建时会分析所有 import 并将模块打包。`pino` 依赖 Node.js 原生模块（`fs`、`stream` 等），不应被打包到客户端 bundle 中。

**解决方案**：

1. 确保 pino 仅在服务端代码中使用（Route Handler、Service、Repository）。
2. 在 `next.config.js` 中将 pino 标记为 external：

```javascript
module.exports = {
  experimental: {
    serverComponentsExternalPackages: ['pino', 'pino-pretty'],
  },
};
```

3. 如果是 import 路径问题，检查是否有组件从共享模块间接引用了 pino。使用 `import type` 替代 `import` 当只需要类型时。

---

### Q2: `supabase db reset` 执行失败，报 "relation xxx does not exist"

**错误现象**：
```
ERROR: relation "public.videos" does not exist (SQLSTATE 42P01)
```

**原因**：迁移文件中引用了尚不存在的表。可能是迁移文件顺序错误，或者某个迁移的 `up` 脚本依赖了后续迁移才会创建的对象。

**解决方案**：

1. 检查迁移文件的时间戳顺序：`supabase migration list`。
2. 确保每个迁移文件是自包含的——不依赖其他迁移创建的表，除非该迁移时间戳更早。
3. 如果确实需要跨迁移依赖，使用 `CREATE TABLE IF NOT EXISTS` 或合并迁移文件。
4. 如果问题持续，手动修复：清除 `supabase/migrations/` 中不正确的 SQL，重新执行 `supabase db reset`。

---

### Q3: Prisma Client 报 "Can't reach database server"

**错误现象**：
```
PrismaClientInitializationError: Can't reach database server at `localhost:54322`
```

**原因**：Supabase 本地服务未启动，或 `DATABASE_URL` 配置不正确。

**解决方案**：

```bash
# 确认 Supabase 本地服务正在运行
supabase status

# 如果未运行，启动它
supabase start

# 验证连接字符串
supabase status -o env | grep DB_URL

# 确保 backend/.env.local 中的 DATABASE_URL 与 supabase status 输出一致
# 注意：Supabase 本地使用的是 postgresql://postgres:postgres@localhost:54322/postgres
```

---

### Q4: `npm run dev` 后 API 返回 401 Unauthorized

**错误现象**：
```
HTTP 401: { "error": { "code": "UNAUTHORIZED", "message": "未登录" } }
```

**原因**：middleware 的 Auth 验证未通过。可能是 Supabase 本地 Auth 服务的问题，或 Token 生成方式不正确。

**解决方案**：

1. 检查 Supabase 本地服务是否运行且 Auth 服务正常：
```bash
curl http://localhost:54321/auth/v1/health
```

2. 验证 Token 是否有效：
```bash
# 通过 Supabase 本地服务直接获取测试 Token
curl -X POST 'http://localhost:54321/auth/v1/token?grant_type=password' \
  -H 'apikey: <ANON_KEY>' \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"test123456"}'
```

3. 在 middleware 中添加调试日志，确认 Token 验证失败的具体原因：
```typescript
const { data: { session }, error } = await supabase.auth.getSession();
if (error) console.error('Auth error in middleware:', error);
```

---

## 数据库问题

### Q5: Zod 校验通过但数据库写入报 "duplicate key value violates unique constraint"

**错误现象**：
```
error: duplicate key value violates unique constraint "videos_title_key"
```

**原因**：唯一约束冲突，应用层未检测到重复数据。可能是并发写入导致——两个请求同时通过了"不重复"检查。

**解决方案**：

1. 在 Service 层捕获 `23505`（PostgreSQL 唯一约束错误码）：
```typescript
try {
  await videoRepo.create(data);
} catch (error) {
  if (error.code === 'P2002' || error.message?.includes('23505')) {
    throw new ConflictError('该标题已存在');
  }
  throw error;
}
```

2. 在数据库层面确保唯一约束存在（这是最后一道防线）：
```sql
ALTER TABLE videos ADD CONSTRAINT videos_title_unique UNIQUE (title, uploader_id);
```

3. 对于高并发场景，使用 `INSERT ... ON CONFLICT DO NOTHING` 或 `ON CONFLICT DO UPDATE`（Upsert）。

---

### Q6: RLS Policy 生效后查询返回空数组

**错误现象**：使用 `supabase.from('videos').select('*')` 返回空数组 `[]`，但表中确实有数据。

**原因**：RLS Policy 过滤了当前用户无权读取的行。最常见的原因是：
1. 未传入认证 Token，Supabase Client 以匿名身份请求。
2. Policy 的 `USING` 条件不符合当前用户的属性。

**解决方案**：

1. 确认 Supabase Client 传入了有效的 `Authorization: Bearer <token>` Header。
2. 使用 Service Role Key 验证表中确实有数据（Service Role 绕过 RLS）：
```bash
curl "http://localhost:54321/rest/v1/videos?select=count" \
  -H "apikey: <SERVICE_ROLE_KEY>" \
  -H "Authorization: Bearer <SERVICE_ROLE_KEY>"
```

3. 检查 RLS Policy 条件是否匹配：
```sql
-- 查看当前表的所有 Policy
SELECT policyname, cmd, qual, with_check FROM pg_policies WHERE tablename = 'videos';

-- 以特定用户身份测试 Policy
SET LOCAL role authenticated;
SET LOCAL request.jwt.claim.sub = 'user-uuid-here';
SELECT * FROM videos;
```

4. 如果 Policy 引用 `auth.uid()`，确保请求的 JWT Token 包含正确的 `sub` 字段。

---

### Q7: Migration 推送到远程后本地开发不匹配

**错误现象**：远程数据库有新表，但本地 `supabase db reset` 后缺少这些表。

**原因**：迁移文件未同步。可能是其他开发者在远程创建了迁移，但你本地没有 pull。

**解决方案**：

```bash
# 从远程拉取最新的 schema 差异
supabase db pull --linked

# 对比本地与远程的差异
supabase db diff --linked

# 如果需要，从远程 dump 完整 schema
supabase db dump --linked > supabase/dump.sql
```

规范做法：所有 schema 变更通过 Git 管理的 migration 文件进行，禁止直接在 Supabase Dashboard 上修改生产表结构。

---

## API 问题

### Q8: Next.js Route Handler 返回 405 Method Not Allowed

**错误现象**：
```
HTTP 405: Method Not Allowed
```

**原因**：请求使用了 `route.ts` 中未导出的 HTTP 方法。Next.js 只会响应已导出的方法函数。

**解决方案**：

1. 检查 `route.ts` 是否导出了对应的方法函数：
```typescript
// 只支持 GET 和 POST
export async function GET(request: NextRequest) { ... }
export async function POST(request: NextRequest) { ... }
// PUT 和 DELETE 未导出 → 返回 405
```

2. 如果该路由不支持某个方法，可显式导出并返回 405 以提供更清晰的错误信息：
```typescript
export async function PUT() {
  return NextResponse.json(
    { error: { code: 'METHOD_NOT_ALLOWED', message: '此资源不支持 PUT 操作' } },
    { status: 405 },
  );
}
```

---

### Q9: `request.json()` 报错 "Body is already read"

**错误现象**：
```
TypeError: Body is already read
```

**原因**：在同一个请求处理流程中多次调用了 `request.json()`。Next.js 的 Request Body 是 ReadableStream，只能读取一次。

**解决方案**：

1. 确保只在 Route Handler 中调用一次 `request.json()`，解析后传给下层：
```typescript
export async function POST(request: NextRequest) {
  const body = await request.json(); // 只读一次

  // 传给 Service 层时传解析后的对象，让 Service 不要再读 request
  return videoService.create(body);
}
```

2. 不要在 middleware 中读取 Body，这会导致 Route Handler 无法再次读取。

3. 如果需要多次访问，先 clone：
```typescript
const clonedRequest = request.clone();
const body = await clonedRequest.json();
```

---

### Q10: 请求体校验通过但 Zod 类型推断不正确

**错误现象**：`safeParse` 通过（`result.success === true`），但后续代码中类型报错。

**原因**：使用了 `z.coerce` 或 `transform` 后，`z.input` 与 `z.output` 类型不同。例如 `z.coerce.number()` 的 input 是 `string`，output 是 `number`。

**解决方案**：

```typescript
// 正确：使用 z.output 获取经过 coerce/transform 后的类型
const querySchema = z.object({
  limit: z.coerce.number().int().min(1).max(100),
});
type QueryOutput = z.output<typeof querySchema>; // { limit: number }
type QueryInput = z.input<typeof querySchema>;    // { limit: string | number }

// Route Handler 中使用 output 类型
const parsed = querySchema.safeParse(params);
if (parsed.success) {
  // parsed.data 是 z.output 类型
  const result = await service.list(parsed.data); // limit 是 number
}
```

---

## 性能问题

### Q11: API 响应时间过长（>2秒）

**错误现象**：单个 API 请求耗时超过 2000ms，用户体验差。

**常见原因与排查**：

1. **缺少索引**：检查是否全表扫描。
```sql
EXPLAIN ANALYZE SELECT * FROM videos WHERE category = 'romance' ORDER BY created_at DESC LIMIT 20;
```
如果看到 `Seq Scan on videos`，需要添加索引：
```sql
CREATE INDEX idx_videos_category_published ON videos (category, is_published) WHERE deleted_at IS NULL;
```

2. **N+1 查询**：检查 Prisma 查询是否在循环中查询关联数据。
```typescript
// 错误：N+1
const videos = await prisma.video.findMany();
for (const v of videos) {
  const uploader = await prisma.user.findUnique({ where: { id: v.uploaderId } });
}

// 正确：使用 include 预加载
const videos = await prisma.video.findMany({ include: { uploader: true } });
```

3. **COUNT(*) 在大表上的性能问题**：避免在每次列表请求中返回 `total` 计数。如果必须返回，使用估算值：
```sql
SELECT reltuples::bigint AS estimated_count FROM pg_class WHERE relname = 'videos';
```

---

### Q12: Supabase 本地服务内存占用过高

**错误现象**：Docker Desktop 内存使用超过 8GB，系统变慢。

**原因**：Supabase 本地服务启动了一整套容器（PostgreSQL、GoTrue、Kong、Storage 等），默认会占用较多内存。

**解决方案**：

1. 限制 Docker Desktop 的内存上限（Settings → Resources → Memory → 4GB）。
2. 仅启动需要的服务。如果不需要 Storage 或实时功能，可以停止对应容器：
```bash
# 查看所有 Supabase 容器
docker ps --filter "label=com.supabase.cli.project"

# 停止不需要的容器（如 imgproxy、storage）
docker stop supabase_imgproxy_local_short-drama supabase_storage_local_short-drama
```

3. 如果只需要数据库，可以只用 `supabase db start`（如果 CLI 版本支持）或直接启动 PostgreSQL 容器。

---

### Q13: Supabase JS Client 查询慢且返回大量数据

**错误现象**：`supabase.from('videos').select('*')` 返回所有列和所有行，响应体积巨大。

**解决方案**：

1. 使用 `.select()` 限定需要的列：
```typescript
const { data } = await supabase
  .from('videos')
  .select('id, title, cover_image_url, duration, view_count')
  .limit(20);
```

2. 始终使用 `.limit()` 控制返回行数（Supabase 默认最多返回 1000 行，但仍会造成不必要的数据传输）。

3. 大字段（如 `description` 文本）在列表查询中不返回，仅在详情查询中返回。

4. 使用 `.range()` 做分页或使用 cursor 分页。

---

## 部署问题

### Q14: Vercel 部署后 Supabase 连接失败

**错误现象**：部署到 Vercel 后，API 返回 500，日志显示 "Connection refused" 或 "Can't reach database server"。

**原因**：Vercel 环境中的环境变量未配置或配置不正确。Supabase 可能限制了连接 IP。

**解决方案**：

1. 在 Vercel Project Settings → Environment Variables 中配置：
   - `SUPABASE_URL`（使用 Supabase 项目的 URL，非本地 `localhost`）
   - `SUPABASE_ANON_KEY`
   - `SUPABASE_SERVICE_ROLE_KEY`
   - `DATABASE_URL`（使用 Supabase Dashboard → Settings → Database → Connection string，勾选 "Use connection pooling"）

2. Supabase 使用 PgBouncer 连接池（端口 6543），在 Vercel 的 serverless 环境中必须使用 Session 模式：
```
DATABASE_URL=postgresql://postgres.xxx:[PASSWORD]@aws-0-region.pooler.supabase.com:6543/postgres?pgbouncer=true
```

3. 如果使用 Prisma，设置 `pgbouncer=true` 并配置 `pgbouncer` 模式：
```prisma
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
  directUrl = env("DIRECT_DATABASE_URL")  # 用于 Migration
}
```

---

### Q15: `npm run build` 在 CI 中失败但本地成功

**错误现象**：GitHub Actions 中 `npm run build` 失败，提示 TypeScript 类型错误，但本地 `npx tsc --noEmit` 无问题。

**原因**：CI 环境与本地环境的 TypeScript 版本不一致，或 `node_modules` 缓存导致类型定义不匹配。

**解决方案**：

1. 在 CI 中使用 `npm ci`（而非 `npm install`）确保与 lock 文件一致：
```yaml
- uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'npm'
    cache-dependency-path: backend/package-lock.json
- run: npm ci
- run: npm run build
```

2. 在 CI 中额外执行 `npx tsc --noEmit` 确保类型检查完整（Next.js 构建的类型检查可能不覆盖所有文件）。

3. 检查 `.gitignore` 是否误忽略了 `next-env.d.ts` 或类型声明文件。

---

### Q16: Supabase CLI 报 "Cannot find linked project"

**错误现象**：
```
Cannot find linked project. Run supabase link first.
```

**原因**：当前项目未关联到远程 Supabase 项目。

**解决方案**：

```bash
# 登录 Supabase（如果未登录）
supabase login

# 生成 access token
supabase login --no-browser  # 如果无法打开浏览器

# 列出你的项目，获取 project ref
supabase projects list

# 关联到目标项目
supabase link --project-ref abcdefghijklmnop

# 验证关联成功
supabase status
```
