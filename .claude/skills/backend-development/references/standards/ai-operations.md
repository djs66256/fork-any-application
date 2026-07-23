# AI 操作与自动化 — Backend

> 本文档定义 Backend 端 AI agent 可执行的自动化操作能力。

---

## 1. API 测试

### 1.1 请求发送

## 1. API 测试

### 1.1 请求发送

使用 curl 对本地和远程 API 进行测试：

**基本请求**：

```bash
# GET 请求（获取视频列表，带分页）
curl -X GET "http://localhost:3000/api/videos?category=romance&limit=10" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/json" \
  -s | jq .

# POST 请求（创建视频）
curl -X POST "http://localhost:3000/api/videos" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "我的短剧",
    "category": "romance",
    "coverImageUrl": "https://example.com/cover.jpg"
  }' \
  -s | jq .

# PUT 请求（更新视频）
curl -X PUT "http://localhost:3000/api/videos/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "更新后的标题"}' \
  -s | jq .

# DELETE 请求（删除视频）
curl -X DELETE "http://localhost:3000/api/videos/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -s -w "\nHTTP Status: %{http_code}\n"
```

**带 Cursor 分页的请求**：

```bash
# 首次请求
curl -X GET "http://localhost:3000/api/videos?limit=20" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -s | jq '.meta'

# 下一页（使用返回的 cursor）
curl -X GET "http://localhost:3000/api/videos?limit=20&cursor=eyJsYXN0SWQiOiJ1dWlkLTEyMyJ9" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -s | jq '{hasMore: .meta.hasMore, count: (.data | length)}'
```

**获取 Access Token**（用于后续请求）：

```bash
# 通过 Supabase Auth 的 Token 端点获取 Session
curl -X POST "https://xxxxx.supabase.co/auth/v1/token?grant_type=password" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "test123456"}' \
  -s | jq -r '.access_token'

# 或通过本地登录 API
curl -X POST "http://localhost:3000/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"phone": "13800138000", "code": "123456"}' \
  -s | jq -r '.data.accessToken'
```

**Admin 操作**（使用 Service Role Key）：

```bash
# 使用 Service Role Key 绕过 RLS 查询所有用户
curl -X GET "http://localhost:3000/api/admin/users?limit=50" \
  -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
  -s | jq '.data | length'
```

### 1.2 响应验证

使用 curl 和 jq 验证响应：

```bash
# 验证 201 创建成功
curl -X POST "http://localhost:3000/api/videos" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"测试短剧","category":"comedy","coverImageUrl":"https://example.com/cover.jpg"}' \
  -s -w "\nHTTP Status: %{http_code}\n" \
  | jq '{status: 201, hasData: (.data.id != null), title: .data.title}'

# 验证 400 校验失败
curl -X POST "http://localhost:3000/api/videos" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":""}' \
  -s -w "\nHTTP Status: %{http_code}\n" \
  | jq '{status: 400, code: .error.code, fields: (.error.details.fieldErrors | keys)}'

# 验证 401 未认证
curl -X GET "http://localhost:3000/api/videos" \
  -s -w "\nHTTP Status: %{http_code}\n" \
  | jq '{status: 401, code: .error.code}'

# 验证 404 资源不存在
curl -X GET "http://localhost:3000/api/videos/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -s -w "\nHTTP Status: %{http_code}\n" \
  | jq '{status: 404, code: .error.code}'

# 验证 409 冲突（重复创建）
curl -X POST "http://localhost:3000/api/videos" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d @- <<'EOF' | jq .
{
  "title": "重复短剧",
  "category": "romance",
  "coverImageUrl": "https://example.com/cover.jpg"
}
EOF

# 验证 Response Header
curl -X GET "http://localhost:3000/api/videos?limit=5" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -s -D - -o /dev/null | head -20
```

### 1.3 API 文档生成

从代码自动生成 OpenAPI 文档：

```bash
# 使用 next-swagger-doc 生成 OpenAPI Spec JSON
curl -X GET "http://localhost:3000/api/docs" \
  -s | jq . > openapi.json

# 如果有独立的文档端点
curl -X GET "http://localhost:3000/api/docs/openapi.json" \
  -s | jq '.paths | keys'
```

在 OpenAPI Spec 中注册 API 路由的注释：

```typescript
/**
 * @swagger
 * /api/videos:
 *   get:
 *     summary: 获取视频列表
 *     parameters:
 *       - name: category
 *         in: query
 *         schema:
 *           type: string
 *           enum: [romance, revenge, comedy, action]
 *       - name: limit
 *         in: query
 *         schema:
 *           type: integer
 *           default: 20
 *     responses:
 *       200:
 *         description: 视频列表
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/VideoListResponse'
 */
export async function GET(request: NextRequest) { ... }
```

---

## 2. 数据库操作

### 2.1 Supabase CLI

**日常开发命令**：

```bash
# 启动本地 Supabase 服务
supabase start

# 查看本地 Supabase 服务状态
supabase status

# 停止本地 Supabase 服务
supabase stop

# 查看本地服务的所有连接信息
supabase status -o env

# 链接到远程 Supabase 项目（从 Dashboard → Settings → API 获取 project ref）
supabase link --project-ref abcdefghijklmnop

# 查看当前链接的项目
supabase projects list
```

**迁移管理**：

```bash
# 创建新迁移文件（文件生成在 supabase/migrations/ 下）
supabase migration new add_video_bookmarks_table

# 查看迁移状态
supabase migration list

# 应用新的迁移到本地数据库（不重置，保留数据）
supabase db push

# 重置本地数据库（清空所有数据，重新应用所有迁移）
supabase db reset

# 从远程 Supabase 项目 pull 最新的 schema 差异
supabase db diff --linked -f fix_videos_index

# 从远程拉取完整 schema 为初始迁移
supabase db pull
```

**数据操作**：

```bash
# 导出远程数据库的数据（生成 seed.sql）
supabase db dump --linked --data-only > supabase/seed.sql

# 导入 seed 数据到本地（在 supabase/seed.sql 存在时 reset 会自动执行）
supabase db reset

# 直接连接本地 PostgreSQL 执行 SQL
psql postgresql://postgres:postgres@localhost:54322/postgres

# 连接后执行 SQL
psql postgresql://postgres:postgres@localhost:54322/postgres -c "
  SELECT tablename, indexname FROM pg_indexes WHERE schemaname = 'public';
"
```

**类型生成**：

```bash
# 从本地数据库生成 TypeScript 类型
supabase gen types typescript --local > backend/shared/types/supabase-db.ts

# 从远程项目生成
supabase gen types typescript --linked > backend/shared/types/supabase-db.ts
```

### 2.2 数据查询

**通过 Supabase JS Client 查询**：

```bash
# 使用 Node.js 脚本直接查询（需要 tsx 或 node）
cat <<'SCRIPT' | npx tsx -
import { createClient } from '@supabase/supabase-js';
const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!,
);

async function main() {
  // 查询视频列表
  const { data, error } = await supabase
    .from('videos')
    .select('id, title, category, view_count, created_at')
    .eq('is_published', true)
    .order('created_at', { ascending: false })
    .limit(10);

  if (error) { console.error(error); return; }
  console.log(JSON.stringify(data, null, 2));
}
main();
SCRIPT
```

**通过 psql 直接执行 SQL**：

```bash
# 查询视频总数（按分类）
psql postgresql://postgres:postgres@localhost:54322/postgres -c "
  SELECT category, COUNT(*) as count
  FROM videos
  WHERE is_published = true AND deleted_at IS NULL
  GROUP BY category
  ORDER BY count DESC;
"

# 查询慢查询（pg_stat_statements 扩展需要先启用）
psql postgresql://postgres:postgres@localhost:54322/postgres -c "
  SELECT query, calls, mean_exec_time, total_exec_time
  FROM pg_stat_statements
  ORDER BY mean_exec_time DESC
  LIMIT 10;
"

# 查询表大小
psql postgresql://postgres:postgres@localhost:54322/postgres -c "
  SELECT
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
  FROM pg_tables
  WHERE schemaname = 'public'
  ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
"

# 查看索引使用情况
psql postgresql://postgres:postgres@localhost:54322/postgres -c "
  SELECT
    indexrelname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
  FROM pg_stat_user_indexes
  WHERE schemaname = 'public'
  ORDER BY idx_scan DESC;
"

# 检查 RLS 策略
psql postgresql://postgres:postgres@localhost:54322/postgres -c "
  SELECT
    tablename,
    policyname,
    cmd,
    permissive,
    qual
  FROM pg_policies
  WHERE schemaname = 'public'
  ORDER BY tablename, cmd;
"
```

### 2.3 数据导入导出

```bash
# 导出完整数据库（schema + data）
supabase db dump --linked > supabase/dump.sql

# 仅导出数据（不含 schema）
supabase db dump --linked --data-only > supabase/data.sql

# 从本地导出 CSV（通过 psql）
psql postgresql://postgres:postgres@localhost:54322/postgres -c "\
  COPY (SELECT * FROM videos WHERE deleted_at IS NULL) \
  TO STDOUT WITH CSV HEADER" > videos_export.csv

# 从 CSV 导入（通过 psql）
psql postgresql://postgres:postgres@localhost:54322/postgres -c "\
  COPY videos (id, title, category, cover_image_url, duration, is_published, uploader_id) \
  FROM STDIN WITH CSV HEADER" < videos_import.csv

# 导出特定表为 JSON
psql postgresql://postgres:postgres@localhost:54322/postgres -c "
  SELECT json_agg(row_to_json(t))
  FROM (SELECT id, title, category, view_count FROM videos LIMIT 100) t;
" > videos_sample.json

# 从远程 Supabase 拉取数据到本地
supabase db dump --linked --data-only > supabase/seed.sql
# 重置本地数据库并导入 seed
supabase db reset
```

### 2.4 Seed 数据

使用 seed.sql 提供开发测试数据（`supabase/seed.sql`）：

```sql
-- 插入测试用户
INSERT INTO auth.users (id, email, phone, raw_user_meta_data)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'test1@example.com', '13800138001', '{"nickname": "测试用户1"}'),
  ('00000000-0000-0000-0000-000000000002', 'test2@example.com', '13800138002', '{"nickname": "测试用户2"}');

-- 插入公开用户资料
INSERT INTO public.users (id, phone, nickname, role)
VALUES
  ('00000000-0000-0000-0000-000000000001', '13800138001', '测试用户1', 'creator'),
  ('00000000-0000-0000-0000-000000000002', '13800138002', '测试用户2', 'user');

-- 插入测试视频
INSERT INTO public.videos (id, title, description, category, cover_image_url, duration, view_count, is_published, uploader_id)
VALUES
  ('10000000-0000-0000-0000-000000000001', '甜宠短剧：总裁的契约新娘', '一部甜蜜的短剧', 'romance', 'https://picsum.photos/seed/v1/720/1080', 120, 1000, true, '00000000-0000-0000-0000-000000000001'),
  ('10000000-0000-0000-0000-000000000002', '复仇短剧：逆袭人生', '爽文改编', 'revenge', 'https://picsum.photos/seed/v2/720/1080', 90, 2000, true, '00000000-0000-0000-0000-000000000001'),
  ('10000000-0000-0000-0000-000000000003', '搞笑短剧：办公室日常', '轻松搞笑', 'comedy', 'https://picsum.photos/seed/v3/720/1080', 60, 1500, true, '00000000-0000-0000-0000-000000000001');
```

使用 Faker.js 生成批量测试数据（脚本方式）：

```bash
cat <<'SCRIPT' | npx tsx -
import { faker } from '@faker-js/faker';
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!,
);

const videos = Array.from({ length: 50 }, () => ({
  title: faker.lorem.sentence({ min: 2, max: 5 }),
  description: faker.lorem.paragraph(),
  category: faker.helpers.arrayElement(['romance', 'revenge', 'comedy', 'action']),
  cover_image_url: faker.image.urlPicsumPhotos({ width: 720, height: 1080 }),
  duration: faker.number.int({ min: 30, max: 300 }),
  view_count: faker.number.int({ min: 0, max: 100000 }),
  is_published: faker.datatype.boolean(0.8),
  uploader_id: '00000000-0000-0000-0000-000000000001',
}));

const { error } = await supabase.from('videos').insert(videos);
if (error) { console.error(error); process.exit(1); }
console.log(`Inserted ${videos.length} videos`);
SCRIPT
```

---

## 3. 日志分析

### 3.1 日志查询

通过 pino 的 JSON 日志进行过滤和查询：

```bash
# 按日志级别过滤（开发环境的 pino-pretty 输出）
npm run dev 2>&1 | grep '"level":50'    # error
npm run dev 2>&1 | grep '"level":40'    # warn
npm run dev 2>&1 | grep '"level":30'    # info

# 按 traceId 追踪一个请求的完整日志链
npm run dev 2>&1 | grep '"traceId":"abc-123-def"'

# 按 action 过滤特定业务操作
npm run dev 2>&1 | grep '"action":"video.create"'

# 按 userId 过滤特定用户的请求
npm run dev 2>&1 | grep '"userId":"00000000-0000-0000-0000-000000000001"'

# 使用 jq 处理 JSON 日志（生产环境）
npm start 2>&1 | jq 'select(.level == 50)'                    # 只看 error
npm start 2>&1 | jq 'select(.duration > 1000)'                 # 超过 1 秒的请求
npm start 2>&1 | jq '{time, action, duration, msg}'            # 关键字段提取
npm start 2>&1 | jq 'select(.err != null) | {msg, err: .err.message}'  # 错误摘要

# 统计最近 100 条日志的级别分布
cat app.log | tail -100 | jq -r '.level' | sort | uniq -c | sort -rn
```

### 3.2 聚合统计

```bash
# 统计 API 端点的请求量和平均响应时间
cat app.log | jq -r 'select(.action != null) | "\(.action) \(.duration)"' \
  | awk '{sum[$1]+=$2; count[$1]++} END {for (k in sum) printf "%s: %.0fms avg (%d calls)\n", k, sum[k]/count[k], count[k]}' \
  | sort -t: -k2 -rn

# 统计各状态码分布
cat app.log | jq -r 'select(.statusCode != null) | .statusCode' \
  | sort | uniq -c | sort -rn

# 统计 Top 10 慢请求
cat app.log | jq -r 'select(.duration != null) | "\(.action) \(.duration)ms"' \
  | sort -k2 -rn | head -10

# 按小时统计请求量
cat app.log | jq -r 'select(.time != null) | .time[:13]' \
  | sort | uniq -c | sort -k2
```

### 3.3 异常检测

```bash
# 检测错误率突增（最近 5 分钟内 error 数量）
cat app.log | jq -r 'select(.level >= 50 and .time > "'$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S)'Z'")' \
  | wc -l

# 检测同一 traceId 的重复错误（可能是重试风暴）
cat app.log | jq -r 'select(.level >= 40) | .traceId' \
  | sort | uniq -c | sort -rn | head -10

# 检测特定错误码的频次
cat app.log | jq -r 'select(.code != null) | .code' \
  | sort | uniq -c | sort -rn

# 监控 Supabase 数据库日志（通过 Supabase CLI）
supabase logs --project-ref abcdefghijklmnop

# 监控特定 Supabase 服务的日志
supabase logs --project-ref abcdefghijklmnop --service auth     # Auth 日志
supabase logs --project-ref abcdefghijklmnop --service db       # 数据库日志
supabase logs --project-ref abcdefghijklmnop --service storage  # Storage 日志
```

---

## 4. Mock 服务

### 4.1 API Mock

使用 MSW (Mock Service Worker) 在开发阶段 Mock API：

```typescript
// backend/__mocks__/handlers.ts
import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('http://localhost:3000/api/videos', () => {
    return HttpResponse.json({
      data: [
        { id: 'mock-1', title: 'Mock 短剧', category: 'romance' },
      ],
      meta: { cursor: null, hasMore: false },
    });
  }),

  http.post('http://localhost:3000/api/videos', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json(
      { data: { id: 'mock-new', ...body } },
      { status: 201 },
    );
  }),
];
```

使用 Faker.js 生成真实感测试数据：

```bash
cat <<'SCRIPT' | npx tsx -
import { faker } from '@faker-js/faker';

// 生成 10 个模拟视频数据
const mockVideos = Array.from({ length: 10 }, () => ({
  id: faker.string.uuid(),
  title: faker.lorem.sentence({ min: 2, max: 5 }),
  category: faker.helpers.arrayElement(['romance', 'revenge', 'comedy', 'action']),
  coverImageUrl: faker.image.urlPicsumPhotos({ width: 720, height: 1080 }),
  duration: faker.number.int({ min: 30, max: 300 }),
  viewCount: faker.number.int({ min: 0, max: 500000 }),
  createdAt: faker.date.past().toISOString(),
}));

console.log(JSON.stringify(mockVideos, null, 2));
SCRIPT
```

### 4.2 外部服务 Mock

**Supabase Auth Mock**（用于单元测试，不依赖真实 Supabase 服务）：

```typescript
import { vi } from 'vitest';

// Mock supabase.auth.getSession
vi.mock('@supabase/ssr', () => ({
  createServerClient: () => ({
    auth: {
      getSession: vi.fn().mockResolvedValue({
        data: {
          session: {
            user: { id: 'test-user-id', role: 'user' },
            access_token: 'mock-token',
          },
        },
        error: null,
      }),
    },
  }),
}));
```

**第三方 API Mock**（使用 MSW 拦截外部请求）：

```bash
# 用 http-server 或 json-server 快速搭建 Mock 外部服务
npx json-server --watch backend/__mocks__/external-api.json --port 4000

# external-api.json 示例
cat > backend/__mocks__/external-api.json <<'JSON'
{
  "payments": [
    { "id": "pay-1", "status": "success", "amount": 990, "createdAt": "2026-01-01T00:00:00Z" }
  ],
  "sms": [
    { "phone": "13800138000", "sent": true, "code": "123456" }
  ]
}
JSON
```

**在测试中配置 Mock 外部请求**：

```typescript
// backend/__tests__/setup.ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

export const server = setupServer(
  // Mock 支付回调
  http.post('https://api.payment-gateway.com/callback', () => {
    return HttpResponse.json({ status: 'ok' });
  }),

  // Mock 短信发送
  http.post('https://api.sms-provider.com/send', () => {
    return HttpResponse.json({ code: 0, message: '发送成功' });
  }),

  // Mock Supabase 远程端点
  http.get('https://*.supabase.co/rest/v1/*', ({ request }) => {
    // 返回空数据或 mock 数据
    return HttpResponse.json([]);
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

**使用 Supabase Local 作为真实 Mock 环境**：

```bash
# 启动本地 Supabase（包含完整的 PostgreSQL、Auth、Storage）
supabase start

# 验证本地服务可用
curl http://localhost:54321/rest/v1/videos \
  -H "apikey: $(supabase status -o env | grep ANON_KEY | cut -d= -f2)" \
  -H "Authorization: Bearer $(supabase status -o env | grep ANON_KEY | cut -d= -f2)"

# 在本地 Supabase 中创建 Mock 用户
curl -X POST 'http://localhost:54321/auth/v1/admin/users' \
  -H "apikey: $(supabase status -o env | grep SERVICE_ROLE_KEY | cut -d= -f2)" \
  -H "Authorization: Bearer $(supabase status -o env | grep SERVICE_ROLE_KEY | cut -d= -f2)" \
  -H "Content-Type: application/json" \
  -d '{"email": "mock@test.com", "password": "mock123456", "email_confirm": true}'
```
