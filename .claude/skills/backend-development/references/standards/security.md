# 安全 — Backend

> 本文档定义 Backend 端的安全规范。

---

## 1. 输入校验

所有从外部进入的数据（用户请求、第三方回调、Webhook）都视为不可信输入，必须经过 Zod schema 校验。

### 1.1 请求参数校验

每个 API Route Handler 使用 Zod schema 的 `safeParse` 对所有输入进行校验：

```typescript
// 校验 Body
const parsed = createVideoSchema.safeParse(await request.json());
if (!parsed.success) {
  return NextResponse.json(
    { error: { code: 'VALIDATION_ERROR', details: parsed.error.flatten() } },
    { status: 400 },
  );
}
// parsed.data 是类型安全的，后续代码直接使用

// 校验 Query 参数（URL 中均为字符串，使用 z.coerce 转换）
const queryParams = Object.fromEntries(request.nextUrl.searchParams);
const parsed = videoQuerySchema.safeParse(queryParams);

// 校验 Path 参数
const idSchema = z.string().uuid();
const idResult = idSchema.safeParse(params.id);
if (!idResult.success) {
  return NextResponse.json(
    { error: { code: 'VALIDATION_ERROR', message: 'ID 格式无效' } },
    { status: 400 },
  );
}
```

校验规范：
- 使用 `.strict()` 拒绝未声明的字段，防止客户端注入意外数据。
- 使用 `.max()` 限制字符串长度，防止过长的输入（如标题最长 200 字）。
- 使用 `.url()`、`.email()`、`.uuid()` 等 Zod 内置校验方法做格式验证。
- 使用 `.min()` / `.max()` 限制数组长度（如标签最多 10 个）和数字范围。
- Schema 文件统一放在 `backend/shared/schemas/` 目录下。

### 1.2 SQL 注入防护

项目通过以下方式彻底防止 SQL 注入：

- **使用 Supabase JS Client 的参数化查询**：`supabase.from('videos').select().eq('id', id)` 自动参数化，无需手动转义。
- **使用 Prisma ORM**：`prisma.video.findUnique({ where: { id } })` 同样是参数化的。
- **严禁拼接 SQL 字符串**：禁止 `supabase.rpc('fn', {})` 中使用字符串拼接，禁止 `prisma.$queryRawUnsafe('SELECT * FROM videos WHERE id = ' + id)`（`$queryRawUnsafe` 只在 Migration 等受控脚本中使用）。

```typescript
// 正确：参数化查询
const { data } = await supabase.from('videos').select().eq('id', userInput);

// 错误：字符串拼接 — 绝对禁止
const { data } = await supabase.rpc('search_videos', { keyword: `'${userInput}'` });

// 如必须使用原生 SQL，用参数化模板
const result = await prisma.$queryRaw<Video[]>`
  SELECT * FROM videos WHERE title ILIKE ${'%' + keyword + '%'}
`;
```

### 1.3 类型安全

TypeScript + Zod 形成双重保障：

- **编译时**：TypeScript 编译器检查类型一致性，防止类型错误。
- **运行时**：Zod 在应用运行时校验实际输入数据，防止类型系统被绕过（如客户端发送的非预期格式）。
- 所有从 `z.infer` 派生的类型与 Zod schema 保持同步——修改 schema 后 TypeScript 编译器会报告所有受影响的位置。

---

## 2. API 安全

### 2.1 Rate Limiting

使用 `rate-limiter-flexible` 实现多层限流：

```typescript
// backend/shared/rate-limiter.ts
import { RateLimiterMemory, RateLimiterRedis } from 'rate-limiter-flexible';

// 开发环境使用内存限流，生产环境使用 Redis 支持集群
const opts = { points: 100, duration: 60 }; // 60秒内100次请求

export const globalLimiter = process.env.NODE_ENV === 'production'
  ? new RateLimiterRedis({ storeClient: redisClient, ...opts })
  : new RateLimiterMemory(opts);

export const authLimiter = new RateLimiterMemory({
  points: 5,        // 60秒内最多5次
  duration: 60,
  blockDuration: 300, // 超限后封禁5分钟
});

export const createVideoLimiter = new RateLimiterMemory({
  points: 10,       // 每用户每小时最多创建10个视频
  duration: 3600,
});
```

在 Route Handler 中使用：

```typescript
import { globalLimiter, createVideoLimiter } from '@/shared/rate-limiter';

export async function POST(request: NextRequest) {
  const userId = request.headers.get('x-user-id') ?? 'anonymous';
  const ip = request.headers.get('x-forwarded-for') ?? 'unknown';

  try {
    await globalLimiter.consume(`${ip}_${userId}`);
    await createVideoLimiter.consume(userId);
  } catch {
    return NextResponse.json(
      { error: { code: 'RATE_LIMITED', message: '请求过于频繁，请稍后再试' } },
      { status: 429 },
    );
  }
  // ...
}
```

**限流维度**：
- IP 限流：防止单一 IP 暴力请求，100 次/分钟。
- 用户限流：防止被窃 Token 的滥用，按操作类型分解（如登录 5 次/分钟、上传视频 10 次/小时）。
- API Key 限流：对第三方 API Key 按套餐配额限流。

**限流响应 Header**：
```
RateLimit-Limit: 100
RateLimit-Remaining: 45
RateLimit-Reset: 1714000000
Retry-After: 60
```

### 2.2 CORS

在 `backend/middleware.ts` 中配置 CORS：

```typescript
const ALLOWED_ORIGINS = process.env.CORS_ORIGINS?.split(',') ?? [];

export async function middleware(request: NextRequest) {
  const origin = request.headers.get('origin') ?? '';

  // OPTIONS 预检请求
  if (request.method === 'OPTIONS') {
    const isAllowed = ALLOWED_ORIGINS.includes(origin);
    return new NextResponse(null, {
      headers: {
        'Access-Control-Allow-Origin': isAllowed ? origin : '',
        'Access-Control-Allow-Methods': 'GET,POST,PUT,PATCH,DELETE',
        'Access-Control-Allow-Headers': 'Content-Type,Authorization,X-Client-Version',
        'Access-Control-Max-Age': '86400',
      },
    });
  }

  // 非预检请求在响应中追加 CORS Header
  const response = NextResponse.next();
  if (ALLOWED_ORIGINS.includes(origin)) {
    response.headers.set('Access-Control-Allow-Origin', origin);
    response.headers.set('Access-Control-Allow-Credentials', 'true');
  }

  return response;
}
```

- **不要**使用 `Access-Control-Allow-Origin: *`——始终使用白名单。
- 生产环境 `CORS_ORIGINS` 应只包含 App 端和 Web 管理后台的实际域名。
- 预检请求的 `Max-Age` 设为一天（86400 秒）以减少 OPTIONS 请求。

### 2.3 API Key 管理

- 第三方/合作方接入使用 API Key 认证，非 Supabase Auth。
- API Key 生成方式：`crypto.randomBytes(32).toString('hex')`，生成 64 字符的 Hex 串。
- API Key 存储：使用 SHA-256 哈希后存入数据库，原始 Key 仅在生成时展示一次。

```typescript
import crypto from 'crypto';

export function generateApiKey(): { raw: string; hashed: string; prefix: string } {
  const raw = crypto.randomBytes(32).toString('hex');     // 64字符
  const hashed = crypto.createHash('sha256').update(raw).digest('hex');
  const prefix = raw.substring(0, 8);                      // 用于在 UI 中展示
  return { raw, hashed, prefix };
}

// 验证时
function verifyApiKey(rawKey: string, storedHash: string): boolean {
  const hash = crypto.createHash('sha256').update(rawKey).digest('hex');
  return crypto.timingSafeEqual(Buffer.from(hash), Buffer.from(storedHash));
}
```

- 使用 `timingSafeEqual` 比较哈希值，防止时序攻击。
- API Key 支持设置过期时间和权限范围（如只读/读写）。

### 2.4 请求大小限制

- JSON Body 限制：100KB（通过 Next.js 的 `bodySizeLimit` 或中间件检查）。
- 文件上传限制：视频 500MB、图片 10MB（在 Route Handler 中校验）。

```typescript
export async function POST(request: NextRequest) {
  const contentLength = parseInt(request.headers.get('content-length') ?? '0', 10);

  const MAX_BODY_SIZE = 100 * 1024; // 100KB
  if (contentLength > MAX_BODY_SIZE) {
    return NextResponse.json(
      { error: { code: 'PAYLOAD_TOO_LARGE', message: '请求体过大' } },
      { status: 413 },
    );
  }

  // 或者使用 Next.js 配置
  // export const maxDuration = 30; // 最大执行时间
}
```

Next.js 配置（`next.config.js`）：

```javascript
module.exports = {
  experimental: {
    serverActions: { bodySizeLimit: '100kb' },
  },
};
```

---

## 3. 敏感数据

### 3.1 环境变量

- 服务端密钥（`SUPABASE_SERVICE_ROLE_KEY`、`DATABASE_URL` 等）**只能**配置在 `.env.local` 和部署平台的环境变量中。
- 禁止使用 `NEXT_PUBLIC_` 前缀暴露密钥到前端。
- `.env` 文件已在 `.gitignore` 中排除——最基础的防护。
- 生产环境密钥通过 Vercel Environment Variables 或 CI/CD 的 Secret 管理注入。

```bash
# .gitignore 中必须有
.env
.env.local
.env.*.local
```

- 密钥分级：
  - **L1 - 公开**（`NEXT_PUBLIC_APP_URL`）：可提交到代码仓库。
  - **L2 - 内部**（`DATABASE_URL`、`REDIS_URL`）：仅限服务端，通过环境变量注入。
  - **L3 - 高危**（`SUPABASE_SERVICE_ROLE_KEY`、JWT 签名密钥）：除环境变量外，还应限制可访问人员范围（如 Supabase Dashboard 权限管控）。

### 3.2 数据脱敏

日志中不记录敏感信息：

```typescript
// 脱敏函数
function maskPhone(phone: string): string {
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
  // 138****8000
}

function maskToken(token: string): string {
  if (token.length <= 8) return '***';
  return token.substring(0, 4) + '...' + token.substring(token.length - 4);
}

function maskEmail(email: string): string {
  const [name, domain] = email.split('@');
  const masked = name!.substring(0, 2) + '***' + name!.substring(name!.length - 1);
  return `${masked}@${domain}`;
}
```

**禁止在日志中输出的内容**：
- 密码、Token、API Key 的原始值。
- 手机号、邮箱、身份证号等 PII（个人可识别信息）的原始值。
- 完整的请求 Body（用户上传的内容可能包含隐私数据）。
- 第三方的 Secret（支付、短信等服务的密钥）。

**日志脱敏规范**：
```typescript
logger.info({
  userId: user.id,
  phone: maskPhone(user.phone),
  action: 'user.login',
}, 'User logged in'); // ✓ 正确

logger.info({ user }); // ✗ 错误：可能暴露手机号、邮箱
```

### 3.3 加密存储

- 密码由 Supabase Auth 管理，使用 bcrypt/scrypt 哈希，无需手动处理。
- 如果需要存储额外的敏感字段（如身份证号、支付信息），使用 AES-256-GCM 加密：

```typescript
import crypto from 'crypto';

const ENCRYPTION_KEY = Buffer.from(process.env.SECRET_ENCRYPTION_KEY!, 'hex'); // 32字节

export function encrypt(plaintext: string): { encrypted: string; iv: string; tag: string } {
  const iv = crypto.randomBytes(16);
  const cipher = crypto.createCipheriv('aes-256-gcm', ENCRYPTION_KEY, iv);
  const encrypted = Buffer.concat([cipher.update(plaintext, 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();
  return {
    encrypted: encrypted.toString('hex'),
    iv: iv.toString('hex'),
    tag: tag.toString('hex'),
  };
}

export function decrypt(encrypted: string, iv: string, tag: string): string {
  const decipher = crypto.createDecipheriv('aes-256-gcm', ENCRYPTION_KEY, Buffer.from(iv, 'hex'));
  decipher.setAuthTag(Buffer.from(tag, 'hex'));
  return decipher.update(Buffer.from(encrypted, 'hex')) + decipher.final('utf8');
}
```

- 加密密钥存储在环境变量 `SECRET_ENCRYPTION_KEY` 中，不写入数据库。
- 数据库层面使用 PostgreSQL 的 `pgcrypto` 扩展作为补充，但主要加密逻辑在应用层完成（减少对数据库功能的耦合）。

---

## 4. 依赖安全

### 4.1 依赖审计

```bash
# 检查已知漏洞
npm audit

# 查看详细漏洞报告
npm audit --json | jq '.vulnerabilities | to_entries | map({package: .key, severity: .value.severity, via: .value.via})'

# 自动修复（注意：可能引入 breaking changes）
npm audit fix

# 仅修复生产依赖
npm audit fix --only=prod

# CI 中集成审计（发现高风险漏洞时阻塞构建）
npm audit --audit-level=high && echo "No high vulnerabilities" || (echo "High vulnerability found" && exit 1)
```

定期审计节奏：
- 每次 PR 合并前运行 `npm audit`（在 CI 中自动执行）。
- 每周运行一次 `npm audit` 全量检查。
- 发现 Critical/High 漏洞时在 24 小时内处理。

### 4.2 漏洞扫描

**GitHub Dependabot**（推荐，与 GitHub 原生集成）：

`.github/dependabot.yml`：

```yaml
version: 2
updates:
  - package-ecosystem: "npm"
    directory: "/backend"
    schedule:
      interval: "weekly"
      day: "monday"
    open-pull-requests-limit: 10
    labels:
      - "dependencies"
      - "security"
    reviewers:
      - "team-backend"
    versioning-strategy: increase

  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
```

**Snyk 集成**（备选）：

```bash
# Snyk CLI 安装后扫描
npx snyk test

# 持续监控
npx snyk monitor

# CI 集成
npx snyk test --severity-threshold=high
```

### 4.3 版本锁定

- `package-lock.json` **必须**提交到仓库（锁定依赖树的确切版本）。
- 不要使用 `^` 或 `~` 范围的依赖（在 `package.json` 中锁定具体版本），除非是内部包。
- 使用 `npm ci`（而非 `npm install`）在 CI/CD 和部署时安装依赖，确保与 lock 文件完全一致。

```json
{
  "dependencies": {
    "zod": "3.23.8",        // 精确版本
    "next": "14.2.5",
    "@supabase/supabase-js": "2.45.1"
  }
}
```

- 定期检查过时的依赖：

```bash
npm outdated
npm outdated --json | jq 'to_entries | map({package: .key, current: .value.current, latest: .value.latest})'
```
