# 编译、运行与调试 — Backend

> 本文档定义 Backend 端的构建、运行与调试规范。

---

## 1. 环境配置

所有环境特定配置通过 `.env` 文件管理，严禁在代码中硬编码环境地址、密钥或连接信息。

### 1.1 环境变量

| 文件 | 用途 | 提交到 Git |
|------|------|-----------|
| `.env.example` | 环境变量模板（不含真实值） | 是 |
| `.env.local` | 本地开发覆盖 | 否 |
| `.env.development` | 开发环境公共变量 | 否 |
| `.env.production` | 生产环境变量 | 否 |
| `.env.test` | 测试环境变量（如测试数据库 URL） | 否 |

核心环境变量声明（`.env.example`）：

```bash
# Supabase
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIs...
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJIUzI1NiIs...

# Database (Prisma)
DATABASE_URL=postgresql://postgres:password@localhost:54322/postgres

# Redis
REDIS_URL=redis://localhost:6379

# App
NEXT_PUBLIC_APP_URL=http://localhost:3000

# Storage
STORAGE_BUCKET_NAME=media

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:3001

# Logging
LOG_LEVEL=debug
```

- `NEXT_PUBLIC_` 前缀的变量会被打包到前端，**只能**存放公开信息（如 APP URL），密钥类变量**严禁**使用此前缀。
- 启动前确保 `.env.local` 存在——可执行 `cp .env.example .env.local` 后填入真实值。

### 1.2 多环境管理

| 环境 | Supabase 项目 | 数据库 | Redis | 用途 |
|------|-------------|--------|-------|------|
| development | 本地（`supabase start`） | `localhost:54322` | 本地/禁用 | 日常开发 |
| staging | Supabase Staging 项目 | 共享实例 | 共享实例 | 预发布验证 |
| production | Supabase Production 项目 | 独立实例 | 独立实例 | 线上服务 |

切换环境时通过 `SUPABASE_URL`、`DATABASE_URL` 等环境变量区分，不要修改代码。

### 1.3 Secret 管理

- **所有密钥**（API Key、数据库密码、JWT Secret、Service Role Key）只存在于 `.env.local` 和平台的环境变量配置中。
- `.env` 文件已加入 `.gitignore`，**绝不允许**提交到仓库。
- `.env.example` 是唯一应提交的环境变量文件，只包含变量名和占位说明。
- 生产环境密钥通过部署平台（Vercel Environment Variables）注入，不经过文件系统。
- Service Role Key 仅在后端使用，**不能**配置为 `NEXT_PUBLIC_*` 或暴露到前端。

---

## 2. 常用命令

### 2.1 开发

```bash
# 启动 Supabase 本地服务（首次使用需要 supabase init 和 supabase start）
supabase start

# 启动 Next.js 开发服务器（需要先配置好 .env.local）
cd backend && npm run dev

# 同时启动 Supabase + Next.js
supabase start && cd backend && npm run dev
```

### 2.2 构建

```bash
# 生产构建（会运行 lint 和 TypeScript 类型检查）
cd backend && npm run build

# 仅类型检查（不构建）
npx tsc --noEmit

# 启动生产服务器
npm run start
```

### 2.3 数据库

```bash
# 创建新的迁移文件
supabase migration new add_video_tags_table

# 应用迁移到本地数据库（会重置数据库，丢失所有本地数据）
supabase db reset

# 仅应用新的迁移（不重置，保留数据）
supabase db push

# 将本地 schema 变更推送到链接的远程 Supabase 项目
supabase db push --linked

# 从远程拉取 schema 变更
supabase db pull

# 查看迁移列表
supabase migration list

# 为本地数据库生成 seed 数据（从远程拉取）
supabase db dump --local --data-only > supabase/seed.sql

# 使用 Prisma 生成迁移（如使用 Prisma 管理 schema）
npx prisma migrate dev --name add_video_tags

# 推送 Prisma schema 到数据库
npx prisma db push

# 查看 Prisma Studio（数据库 GUI）
npx prisma studio
```

### 2.4 Lint

```bash
# ESLint 检查
cd backend && npm run lint

# ESLint 自动修复
npm run lint -- --fix

# 格式化检查（Prettier）
npx prettier --check .

# 格式化修复
npx prettier --write .

# 类型检查
npx tsc --noEmit
```

### 2.5 测试

```bash
# 运行所有测试
cd backend && npm run test

# 运行测试并生成覆盖率报告
npm run test -- --coverage

# 监听模式（文件变化时自动运行）
npm run test -- --watch

# 运行特定测试文件
npm run test -- src/__tests__/services/video-service.test.ts

# 运行特定测试用例（按名称匹配）
npm run test -- -t "should create a video"

# 运行 E2E 测试（如配置了 Playwright）
npm run test:e2e
```

---

## 3. 调试

### 3.1 Node.js Inspector

使用 Node.js 内置的 Inspector 进行调试：

```bash
# 以 Inspector 模式启动开发服务器
NODE_OPTIONS='--inspect' npm run dev

# 指定非默认端口
NODE_OPTIONS='--inspect=9229' npm run dev
```

然后通过以下任一方式连接：
- 打开 Chrome，访问 `chrome://inspect`，在 "Remote Target" 中找到进程并点击 "inspect"。
- 在 VS Code 中配置 `launch.json` 附加到进程（见 3.3）。

### 3.2 日志调试

- **开发环境**：`LOG_LEVEL=debug`，在关键节点使用 `logger.debug()` 输出中间状态、SQL 参数、请求上下文。
- **生产环境**：`LOG_LEVEL=info`，仅输出 `info` 及以上级别的日志。
- 通过 `traceId` 追踪单个请求的完整调用链——在 middleware 中生成 `traceId`，所有该请求内的日志都带上此字段。
- 使用 `logger.child({ traceId, userId })` 创建带上下文的子 Logger，避免每次都手动传入。

```typescript
// 在 Route Handler 中创建子 Logger
const traceId = request.headers.get('x-trace-id') ?? crypto.randomUUID();
const reqLogger = logger.child({ traceId, userId });
reqLogger.info({ action: 'video.list.request' }, 'Fetching video list');
```

### 3.3 断点调试

**VS Code 配置**（`.vscode/launch.json`）：

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Next.js: Debug Server",
      "type": "node",
      "request": "launch",
      "runtimeExecutable": "npm",
      "runtimeArgs": ["run", "dev"],
      "cwd": "${workspaceFolder}/backend",
      "console": "integratedTerminal",
      "skipFiles": ["<node_internals>/**"]
    },
    {
      "name": "Next.js: Attach to Server",
      "type": "node",
      "request": "attach",
      "port": 9229,
      "skipFiles": ["<node_internals>/**"]
    },
    {
      "name": "Vitest: Current File",
      "type": "node",
      "request": "launch",
      "runtimeExecutable": "npm",
      "runtimeArgs": ["run", "test", "--", "--run", "${relativeFile}"],
      "cwd": "${workspaceFolder}/backend",
      "console": "integratedTerminal"
    }
  ]
}
```

- 在代码中插入 `debugger;` 语句作为断点，在 Inspector 模式下程序会在此暂停。
- 使用 `launch` 配置直接启动调试，`attach` 配置附加到已运行的 `--inspect` 进程。

---

## 4. 性能分析

### 4.1 API 响应时间

- Middleware 中记录每个请求的耗时：`response.headers.set('x-response-time', `${duration}ms`)`。
- 使用 pino 记录每个 Route Handler 的耗时：

```typescript
const start = Date.now();
const result = await videoService.list(params);
logger.info({ action: 'video.list', duration: Date.now() - start }, 'Query completed');
```

- 在 Supabase Dashboard 中查看 API 请求的平均响应时间和 P95/P99。
- 对超过 500ms 的 API 请求在日志中使用 `warn` 级别标记。

### 4.2 数据库性能

**分析查询计划**：

```sql
-- 在 Supabase SQL Editor 中执行
EXPLAIN ANALYZE
SELECT * FROM videos
WHERE category = 'romance' AND is_published = true
ORDER BY created_at DESC
LIMIT 20;
```

关键指标：
- `Seq Scan`（全表扫描）→ 考虑添加索引。
- `Index Scan` 或 `Index Only Scan` → 索引被正确使用。
- `cost` 和 `actual time` → 预估成本与实际执行时间的差距。

**索引使用检查**：

```sql
-- 查看表上的索引使用统计
SELECT
  schemaname, tablename, indexrelname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'videos'
ORDER BY idx_scan DESC;
```

- `idx_scan` 接近 0 的索引可能是无效索引，考虑删除。

**慢查询识别**：
- Supabase Dashboard → Database → Query Performance → 查看慢查询列表。
- 启用 `auto_explain` 扩展自动记录超过阈值的查询。
- 使用 Prisma 时，开发环境开启 `log: ['query']`，观察 SQL 输出。

### 4.3 内存/CPU

**Node.js Profiler**：

```bash
# 启动时启用 CPU Profiling
NODE_OPTIONS='--cpu-prof' npm run dev

# 生成 .cpuprofile 文件后，在 Chrome DevTools → Performance → Load Profile 中分析
```

**Heap Snapshot**（排查内存泄漏）：

```typescript
import { writeHeapSnapshot } from 'v8';
import { createWriteStream } from 'fs';

// 在可疑位置生成 heapdump
function dumpHeap() {
  const filename = `heap-${Date.now()}.heapsnapshot`;
  writeHeapSnapshot(filename);
  logger.warn({ filename }, 'Heap snapshot created');
}
```

- 在 Chrome DevTools → Memory → Load 中分析 `.heapsnapshot` 文件。
- 关注 `Shallow Size` 和 `Retained Size` 来定位内存泄漏的根因。

**运行时监控**：

```typescript
// 定期输出内存使用情况
setInterval(() => {
  const mem = process.memoryUsage();
  logger.debug({
    heapUsedMB: Math.round(mem.heapUsed / 1024 / 1024),
    heapTotalMB: Math.round(mem.heapTotal / 1024 / 1024),
    rssMB: Math.round(mem.rss / 1024 / 1024),
  }, 'Memory usage');
}, 60000);
```
