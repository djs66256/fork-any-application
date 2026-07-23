# 测试规范 — Backend

> 本文档定义 Backend 端的测试策略、框架选型与编写规范。

---

## 1. 测试金字塔

测试策略遵循测试金字塔模型：大量单元测试覆盖业务逻辑，适量集成测试验证 API 交互，少量 E2E 测试覆盖关键用户流程。

| 层级 | 框架 | 占比 | 目标 |
|------|------|------|------|
| Unit | Vitest | ~60% | 纯函数、工具逻辑、Service 逻辑、数据转换 |
| Integration | Vitest + Supertest | ~30% | API 路由、数据库交互、Auth 流程 |
| E2E | Playwright / 手动 | ~10% | 跨系统完整用户流程 |

- **Unit**：不依赖外部服务（数据库、网络），通过 Mock 隔离依赖。
- **Integration**：连接真实测试数据库，验证 HTTP 请求-响应完整链路。
- **E2E**：在 staging 环境运行，验证真实用户场景。

---

## 2. 单元测试

### 2.1 Vitest

测试文件放在与源码同目录的 `__tests__/` 下，或集中在 `backend/__tests__/` 按源码结构镜像布局：

```
backend/
├── services/
│   ├── video-service.ts
│   └── __tests__/
│       └── video-service.test.ts
├── shared/
│   ├── schemas/
│   │   ├── video.ts
│   │   └── __tests__/
│   │       └── video-schema.test.ts
│   └── utils/
│       ├── pagination.ts
│       └── __tests__/
│           └── pagination.test.ts
└── __tests__/
    ├── setup.ts              # 全局 test setup
    └── integration/           # 集成测试
        └── api/
            └── videos.test.ts
```

基本测试结构：

```typescript
import { describe, it, expect, beforeAll, afterAll, beforeEach, afterEach, vi } from 'vitest';

describe('VideoService', () => {
  // beforeAll: 在所有测试开始前执行一次，如准备测试数据
  beforeAll(async () => {
    // 初始化外部服务 Mock 等
  });

  // afterAll: 在所有测试结束后执行一次，如清理资源
  afterAll(async () => {
    // 清理
  });

  // beforeEach: 每个测试前执行，重置状态
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // afterEach: 每个测试后执行
  afterEach(() => {
    // 清理副作用
  });

  it('should create a video with valid input', async () => {
    // Arrange（准备）
    const input = {
      title: '测试短剧',
      category: 'romance' as const,
      coverImageUrl: 'https://example.com/cover.jpg',
    };

    // Act（执行）
    const result = await videoService.create(input);

    // Assert（验证）
    expect(result).toBeDefined();
    expect(result.title).toBe(input.title);
    expect(result.id).toBeDefined();
  });

  it('should throw error when title is empty', async () => {
    // 对于预期抛出错误的情况
    await expect(
      videoService.create({ title: '', category: 'romance', coverImageUrl: 'https://...' })
    ).rejects.toThrow('标题不能为空');
  });
});
```

### 2.2 Service 层测试

Service 层测试的核心原则：Mock Repository 依赖，专注测试业务逻辑。

```typescript
import { describe, it, expect, vi } from 'vitest';
import { VideoService } from '@/services/video-service';

// 使用 vi.mock 模拟整个模块
vi.mock('@/repositories/video-repository', () => ({
  VideoRepository: vi.fn().mockImplementation(() => ({
    findById: vi.fn(),
    create: vi.fn(),
    list: vi.fn(),
  })),
}));

describe('VideoService', () => {
  const mockRepo = {
    findById: vi.fn(),
    create: vi.fn(),
    list: vi.fn(),
  };

  const videoService = new VideoService(mockRepo as any);

  it('should return video by id', async () => {
    const mockVideo = { id: 'uuid-1', title: '测试短剧', category: 'romance' };
    mockRepo.findById.mockResolvedValue(mockVideo);

    const result = await videoService.getById('uuid-1');

    expect(result).toEqual(mockVideo);
    expect(mockRepo.findById).toHaveBeenCalledWith('uuid-1');
  });

  it('should throw NotFoundError when video does not exist', async () => {
    mockRepo.findById.mockResolvedValue(null);

    await expect(videoService.getById('not-found')).rejects.toThrow('视频不存在');
  });

  it('should filter out deleted videos from list', async () => {
    mockRepo.list.mockResolvedValue({
      data: [
        { id: '1', deletedAt: null },
        { id: '2', deletedAt: new Date() },
      ],
    });

    const result = await videoService.list({});

    expect(result.data).toHaveLength(1);
    expect(result.data[0].id).toBe('1');
  });
});
```

**测试规范**：
- 每个 `it` 测试一个行为，描述使用中文。
- 使用 AAA 模式（Arrange-Act-Assert）组织测试代码。
- Service 测试不应启动真实数据库——所有 Repository 方法通过 Mock 模拟。
- Mock 返回值要反映真实数据结构，不要只返回空对象。

### 2.3 工具函数测试

Zod Schema 测试：

```typescript
import { describe, it, expect } from 'vitest';
import { createVideoSchema } from '@/shared/schemas/video';

describe('createVideoSchema', () => {
  it('should pass with valid input', () => {
    const result = createVideoSchema.safeParse({
      title: '测试短剧',
      category: 'romance',
      coverImageUrl: 'https://example.com/cover.jpg',
    });
    expect(result.success).toBe(true);
  });

  it('should fail when title is empty', () => {
    const result = createVideoSchema.safeParse({
      title: '',
      category: 'romance',
      coverImageUrl: 'https://example.com/cover.jpg',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.flatten().fieldErrors.title).toBeDefined();
    }
  });

  it('should fail when category is invalid', () => {
    const result = createVideoSchema.safeParse({
      title: 'Test',
      category: 'sci-fi',  // 不在枚举中
      coverImageUrl: 'https://example.com/cover.jpg',
    });
    expect(result.success).toBe(false);
  });

  it('should reject unknown fields due to .strict()', () => {
    const result = createVideoSchema.safeParse({
      title: 'Test',
      category: 'romance',
      coverImageUrl: 'https://example.com/cover.jpg',
      maliciousField: 'should be rejected',
    });
    expect(result.success).toBe(false);
  });

  it('should coerce numeric strings in query params', () => {
    const { videoQuerySchema } = require('@/shared/schemas/video');
    const result = videoQuerySchema.safeParse({ limit: '50' });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(typeof result.data.limit).toBe('number');
      expect(result.data.limit).toBe(50);
    }
  });
});
```

数据转换 / 格式化函数测试：

```typescript
import { describe, it, expect } from 'vitest';
import { toCamelCase, cursorEncode, cursorDecode, formatDuration } from '@/shared/utils';

describe('cursorEncode / cursorDecode', () => {
  it('should encode and decode cursor correctly', () => {
    const payload = { lastId: 'uuid-123', lastCreatedAt: '2026-01-01T00:00:00Z' };
    const encoded = cursorEncode(payload);
    const decoded = cursorDecode(encoded);
    expect(decoded).toEqual(payload);
  });
});

describe('formatDuration', () => {
  it('should format seconds to mm:ss', () => {
    expect(formatDuration(65)).toBe('01:05');
    expect(formatDuration(3600)).toBe('60:00');
  });
});
```

---

## 3. API 集成测试

### 3.1 Route Handler 测试

使用 Next.js 的测试工具配合 Supertest 测试完整的 HTTP 请求链路：

```typescript
import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { createServer } from 'http';
import { parse } from 'url';
import next from 'next';
import request from 'supertest';

// vitest.config.ts 中设置 testTimeout，集成测试通常需要更长超时
// export default defineConfig({ test: { testTimeout: 30000 } })

describe('GET /api/videos', () => {
  it('should return video list with pagination', async () => {
    const res = await request(app)
      .get('/api/videos?limit=5')
      .set('Authorization', `Bearer ${testAccessToken}`)
      .expect('Content-Type', /json/)
      .expect(200);

    expect(res.body).toHaveProperty('data');
    expect(res.body).toHaveProperty('meta');
    expect(Array.isArray(res.body.data)).toBe(true);
    expect(res.body.data.length).toBeLessThanOrEqual(5);
    expect(res.body.meta).toHaveProperty('hasMore');
  });

  it('should return 400 for invalid limit parameter', async () => {
    const res = await request(app)
      .get('/api/videos?limit=999')
      .set('Authorization', `Bearer ${testAccessToken}`)
      .expect(400);

    expect(res.body.error.code).toBe('VALIDATION_ERROR');
    expect(res.body.error.details).toBeDefined();
  });

  it('should return 401 without auth token', async () => {
    await request(app)
      .get('/api/videos')
      .expect(401);
  });
});

describe('POST /api/videos', () => {
  it('should create a video and return 201', async () => {
    const res = await request(app)
      .post('/api/videos')
      .set('Authorization', `Bearer ${testAccessToken}`)
      .send({
        title: '集成测试短剧',
        category: 'comedy',
        coverImageUrl: 'https://example.com/cover.jpg',
      })
      .expect(201);

    expect(res.body.data.id).toBeDefined();
    expect(res.body.data.title).toBe('集成测试短剧');
  });

  it('should return 400 when required fields are missing', async () => {
    const res = await request(app)
      .post('/api/videos')
      .set('Authorization', `Bearer ${testAccessToken}`)
      .send({ title: '缺少必要字段' })
      .expect(400);

    expect(res.body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return 409 when creating duplicate unique resource', async () => {
    // 先创建一个
    await request(app)
      .post('/api/videos')
      .set('Authorization', `Bearer ${testAccessToken}`)
      .send({ title: '唯一标题', category: 'romance', coverImageUrl: 'https://...' });

    // 再创建同标题的
    const res = await request(app)
      .post('/api/videos')
      .set('Authorization', `Bearer ${testAccessToken}`)
      .send({ title: '唯一标题', category: 'romance', coverImageUrl: 'https://...' })
      .expect(409);

    expect(res.body.error.code).toBe('CONFLICT');
  });
});
```

### 3.2 认证测试

```typescript
describe('Auth: protected routes', () => {
  it('should return 401 when token is expired', async () => {
    const expiredToken = 'eyJhbGciOiJIUzI1NiIs...'; // 已过期的 JWT

    await request(app)
      .get('/api/videos')
      .set('Authorization', `Bearer ${expiredToken}`)
      .expect(401);
  });

  it('should return 401 when token is malformed', async () => {
    await request(app)
      .get('/api/videos')
      .set('Authorization', 'Bearer not-a-valid-token')
      .expect(401);
  });

  it('should return 403 when user does not have required role', async () => {
    // 使用普通用户 Token 访问管理员接口
    const res = await request(app)
      .delete('/api/admin/users/some-user-id')
      .set('Authorization', `Bearer ${normalUserToken}`)
      .expect(403);

    expect(res.body.error.code).toBe('FORBIDDEN');
  });
});

describe('Auth: public routes', () => {
  it('should allow unauthenticated access to health check', async () => {
    await request(app)
      .get('/api/health')
      .expect(200);
  });

  it('should allow unauthenticated access to login', async () => {
    await request(app)
      .post('/api/auth/login')
      .send({ phone: '13800138000', code: '123456' })
      .expect(200);
  });
});
```

### 3.3 错误场景

确保对所有 HTTP 错误状态码有测试覆盖：

| 状态码 | 必须测试的场景 |
|--------|--------------|
| 400 | 缺少必填字段、字段类型错误、数值超出范围 |
| 401 | 无 Token、过期 Token、伪造 Token |
| 403 | 普通用户操作管理接口、非资源所有者修改资源 |
| 404 | 查询不存在的 ID、更新已删除的资源 |
| 409 | 唯一索引冲突、版本冲突（乐观锁失败）|
| 422 | 业务规则不满足（如余额不足仍尝试购买）|
| 429 | 短时间内发送过多请求 |
| 500 | 数据库连接失败时的降级行为 |

```typescript
describe('Error scenarios', () => {
  it('should return 404 for non-existent video', async () => {
    const res = await request(app)
      .get('/api/videos/00000000-0000-0000-0000-000000000000')
      .set('Authorization', `Bearer ${testAccessToken}`)
      .expect(404);

    expect(res.body.error.code).toBe('VIDEO_NOT_FOUND');
  });

  it('should return 422 when business rule is violated', async () => {
    const res = await request(app)
      .post('/api/playlists/playlist-id/videos')
      .set('Authorization', `Bearer ${testAccessToken}`)
      .send({ videoId: 'video-id' })
      .expect(422);

    expect(res.body.error.code).toBe('PLAYLIST_FULL');
  });
});
```

---

## 4. 数据库测试

### 4.1 测试数据库

- 使用 Supabase Local（`supabase start`）作为测试数据库。
- 测试数据库独立于开发数据库，通过 `DATABASE_URL` 环境变量区分。
- 在 CI 环境中使用 Supabase CLI 启动本地实例或使用 Testcontainers。

vitest 的 global setup 中确保数据库就绪：

```typescript
// backend/__tests__/setup.ts
import { execSync } from 'child_process';

beforeAll(async () => {
  // 确保 Supabase 本地服务运行
  try {
    execSync('supabase status', { stdio: 'ignore' });
  } catch {
    // 如果未运行则启动
    execSync('supabase start', { stdio: 'inherit' });
    // 等待服务就绪
    await new Promise(resolve => setTimeout(resolve, 5000));
  }

  // 重置数据库到最新迁移（清空测试残留数据）
  execSync('supabase db reset', { stdio: 'inherit' });
});

afterAll(async () => {
  // 集成测试结束后不停止 Supabase（开发期间可能需继续使用）
});
```

### 4.2 Seed 数据

在测试文件中通过 Supabase Client 插入测试所需的 seed 数据：

```typescript
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!,
);

async function seedTestData() {
  // 插入测试用户
  const { data: user } = await supabase.auth.admin.createUser({
    email: 'test@example.com',
    password: 'password123',
    email_confirm: true,
  });

  // 插入测试视频
  const { data: videos } = await supabase
    .from('videos')
    .insert([
      { title: '已发布视频 A', category: 'romance', cover_image_url: 'https://...', is_published: true, uploader_id: user.user?.id },
      { title: '已发布视频 B', category: 'comedy', cover_image_url: 'https://...', is_published: true, uploader_id: user.user?.id },
      { title: '草稿视频 C', category: 'action', cover_image_url: 'https://...', is_published: false, uploader_id: user.user?.id },
    ])
    .select();

  return { user, videos };
}
```

### 4.3 事务回滚

每个测试用例之间保持数据隔离，有两种策略：

**策略 1：每个测试后清理**（推荐用于集成测试）：

```typescript
beforeEach(async () => {
  // 插入测试所需数据
  await seedTestData();
});

afterEach(async () => {
  // 使用 Service Role Client 清理，绕过 RLS
  await supabase.from('videos').delete().neq('id', 'nonexistent');
  await supabase.from('users').delete().neq('id', 'nonexistent');
});
```

**策略 2：Database Template**（更快、更干净）：

```sql
-- 在测试开始前创建 template 数据库
CREATE DATABASE test_template WITH TEMPLATE postgres;

-- 应用所有迁移到 template
-- 每个测试用例从 template 创建临时数据库
CREATE DATABASE test_isolated_1 WITH TEMPLATE test_template;
-- 测试完成后 DROP DATABASE test_isolated_1;
```

### 4.4 Testcontainers

对于不需要 Supabase 完整服务的纯数据库测试，使用 Testcontainers 启动临时 PostgreSQL：

```typescript
import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { PostgreSqlContainer } from '@testcontainers/postgresql';
import { PrismaClient } from '@prisma/client';

describe('Database integration tests', () => {
  let container: PostgreSqlContainer;
  let prisma: PrismaClient;

  beforeAll(async () => {
    container = await new PostgreSqlContainer('postgres:15')
      .withDatabase('test_db')
      .withUsername('test')
      .withPassword('test')
      .start();

    prisma = new PrismaClient({
      datasources: { db: { url: container.getConnectionUri() } },
    });

    // 运行 Prisma 迁移
    await prisma.$executeRawUnsafe('CREATE TABLE ...');
  }, 60000); // 容器启动需要较长时间

  afterAll(async () => {
    await prisma.$disconnect();
    await container.stop();
  });

  it('should insert and query a video', async () => {
    const video = await prisma.video.create({
      data: {
        title: 'Testcontainers 测试',
        category: 'romance',
        coverImageUrl: 'https://example.com/cover.jpg',
        duration: 120,
        uploaderId: 'uuid-1',
      },
    });

    expect(video.id).toBeDefined();

    const found = await prisma.video.findUnique({ where: { id: video.id } });
    expect(found?.title).toBe('Testcontainers 测试');
  });
});
```

---

## 5. 测试覆盖率

### 5.1 工具

使用 Vitest 内置的 v8 覆盖率引擎：

```bash
# 生成覆盖率报告
npx vitest run --coverage

# 指定报告格式
npx vitest run --coverage --coverage.reporter=text,html,json-summary

# 覆盖率报告输出到 coverage/ 目录
# 查看 HTML 报告
open backend/coverage/index.html
```

`vitest.config.ts` 配置：

```typescript
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'node',
    include: ['**/__tests__/**/*.test.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json-summary'],
      exclude: [
        'node_modules/',
        '__tests__/',
        'supabase/',
        '*.config.*',
        'shared/types/',        // 纯类型文件不需要测试
        'shared/supabase/',     // Client 初始化不需要测试
      ],
    },
  },
});
```

### 5.2 最低覆盖率

| 指标 | 最低要求 | 建议目标 |
|------|---------|---------|
| 行覆盖率 (Lines) | 70% | 80% |
| 分支覆盖率 (Branches) | 60% | 75% |
| 函数覆盖率 (Functions) | 70% | 85% |
| 语句覆盖率 (Statements) | 70% | 80% |

**重点关注目录**：

| 目录 | 覆盖率要求 | 原因 |
|------|-----------|------|
| `services/` | >= 85% | 核心业务逻辑 |
| `shared/schemas/` | >= 95% | 数据校验，漏洞防护关键 |
| `shared/utils/` | >= 90% | 纯函数，容易测试 | 
| `repositories/` | >= 60% | 数据访问层，主要通过集成测试覆盖 |
| `app/api/` | 不强制，但关键端点在集成测试中覆盖 | Route Handler 主要是编排逻辑 |

**CI 中的覆盖率检查**：

```yaml
# .github/workflows/test.yml 中的覆盖率门禁
- name: Run tests with coverage
  run: npx vitest run --coverage

- name: Check coverage thresholds
  run: |
    COVERAGE=$(node -e "const r=require('./coverage/coverage-summary.json'); console.log(r.total.lines.pct)")
    if (( $(echo "$COVERAGE < 70" | bc -l) )); then
      echo "Line coverage $COVERAGE% is below 70% threshold"
      exit 1
    fi
```
