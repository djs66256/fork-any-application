# 测试规范 — Web

> 本文档定义 Web 端的测试策略、框架选型与编写规范。

---

## 1. 测试金字塔

| 层级 | 框架 | 占比 | 目标 | 运行速度 | 执行频率 |
|------|------|------|------|---------|---------|
| Unit | Vitest | ~70% | 工具函数、Hooks 逻辑、Zod Schema | 毫秒级 | 每次提交 |
| Component | React Testing Library | ~20% | 组件交互行为验证 | 秒级 | 每次提交 |
| E2E | Playwright | ~10% | 关键用户路径完整性 | 分钟级 | 合并前 / 发布前 |

**原则：**

- 越底层越容易写、运行越快，尽量将逻辑下沉到单元测试
- E2E 只覆盖核心流程（注册、登录、剧集播放、购买），不追求覆盖率
- 所有测试必须在 CI 中通过后才允许合并

---

## 2. 单元测试

### 2.1 Vitest

```bash
npm install -D vitest @vitest/coverage-v8
```

配置文件 `vitest.config.ts`：

```typescript
import { defineConfig } from 'vitest/config';
import path from 'path';

export default defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    exclude: ['node_modules', '.next'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.{test,spec}.{ts,tsx}',
        'src/test/**',
        'src/**/*.d.ts',
      ],
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
```

测试文件 Placement：

- 与源文件同目录：`src/lib/utils/formatDate.test.ts`（相邻放置，推荐）
- 或统一放在 `src/__tests__/` 按模块组织

**基本结构：**

```typescript
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('formatDuration', () => {
  it('should format seconds to mm:ss when less than an hour', () => {
    const result = formatDuration(65);
    expect(result).toBe('1:05');
  });

  it('should format seconds to h:mm:ss when over an hour', () => {
    const result = formatDuration(3700);
    expect(result).toBe('1:01:40');
  });

  it('should handle zero seconds', () => {
    const result = formatDuration(0);
    expect(result).toBe('0:00');
  });
});
```

### 2.2 纯函数测试

纯函数测试是最高性价比的测试。应重点覆盖：

- **工具函数**：日期格式化、数值转换、数据转换
- **Zod Schema**：验证合法数据通过、非法数据拒绝、边界值

```typescript
// 测试 Zod Schema
import { describe, it, expect } from 'vitest';
import { videoSearchParamsSchema } from '@/lib/validation/video.schema';

describe('videoSearchParamsSchema', () => {
  it('should parse valid search params', () => {
    const result = videoSearchParamsSchema.safeParse({
      q: '短剧',
      page: 2,
      pageSize: 10,
      sort: 'popular',
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(2);
      expect(result.data.sort).toBe('popular');
    }
  });

  it('should apply default values for missing fields', () => {
    const result = videoSearchParamsSchema.safeParse({});
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(1);
      expect(result.data.pageSize).toBe(20);
      expect(result.data.sort).toBe('latest');
    }
  });

  it('should reject invalid sort value', () => {
    const result = videoSearchParamsSchema.safeParse({ sort: 'invalid' });
    expect(result.success).toBe(false);
  });

  it('should reject pageSize greater than 50', () => {
    const result = videoSearchParamsSchema.safeParse({ pageSize: 100 });
    expect(result.success).toBe(false);
  });

  it('should coerce string page number to integer', () => {
    const result = videoSearchParamsSchema.safeParse({ page: '3' });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.page).toBe(3);
    }
  });
});
```

### 2.3 Hook 测试

使用 `@testing-library/react` 的 `renderHook` 测试自定义 Hook。

```bash
npm install -D @testing-library/react
```

```typescript
// src/hooks/useDebounce.test.ts
import { describe, it, expect, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useDebounce } from './useDebounce';

describe('useDebounce', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should return initial value immediately', () => {
    const { result } = renderHook(() => useDebounce('hello', 500));
    expect(result.current).toBe('hello');
  });

  it('should debounce value changes', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'hello', delay: 500 } }
    );

    // 立即改变值
    rerender({ value: 'world', delay: 500 });

    // 值不应立即改变
    expect(result.current).toBe('hello');

    // 快进 500ms
    act(() => {
      vi.advanceTimersByTime(500);
    });

    expect(result.current).toBe('world');
  });
});
```

测试复杂 Hook（带 API 调用）时使用 `vi.mock` 模拟：

```typescript
// src/hooks/useVideoList.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useVideoList } from './useVideoList';
import * as videoApi from '@/lib/api/video';

vi.mock('@/lib/api/video');

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('useVideoList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return loading state initially', () => {
    vi.mocked(videoApi.fetchVideoList).mockResolvedValue({
      data: [], total: 0, page: 1, pageSize: 20, hasMore: false,
    });

    const { result } = renderHook(() => useVideoList('trending'), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(true);
  });

  it('should return data after successful fetch', async () => {
    const mockData = {
      data: [{ id: '1', title: 'Test', /* ... */ }],
      total: 1, page: 1, pageSize: 20, hasMore: false,
    };

    vi.mocked(videoApi.fetchVideoList).mockResolvedValue(mockData);

    const { result } = renderHook(() => useVideoList('trending'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.data).toEqual(mockData);
  });

  it('should return error state on failure', async () => {
    vi.mocked(videoApi.fetchVideoList).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useVideoList('trending'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});
```

---

## 3. 组件测试

### 3.1 React Testing Library

```bash
npm install -D @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

```typescript
// src/test/setup.ts — Vitest 全局 Setup
import '@testing-library/jest-dom/vitest';
```

基本测试模式：

```typescript
// src/components/VideoCard/VideoCard.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { VideoCard } from './VideoCard';

const mockVideo = {
  id: '00000000-0000-0000-0000-000000000001',
  title: '霸道总裁的甜蜜陷阱',
  coverUrl: 'https://cdn.shortdrama.com/cover.jpg',
  episodeCount: 20,
  duration: 180,
  tags: ['都市', '爱情'],
  playCount: 10000,
  likeCount: 5000,
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-23T00:00:00Z',
};

describe('VideoCard', () => {
  it('should render title and episode count', () => {
    render(<VideoCard video={mockVideo} />);

    expect(screen.getByText(mockVideo.title)).toBeInTheDocument();
    expect(screen.getByText(/20集/)).toBeInTheDocument();
  });

  it('should call onPlay when play button clicked', async () => {
    const user = userEvent.setup();
    const onPlay = vi.fn();

    render(<VideoCard video={mockVideo} onPlay={onPlay} />);

    const playButton = screen.getByRole('button', { name: /播放/ });
    await user.click(playButton);

    expect(onPlay).toHaveBeenCalledWith(mockVideo.id);
  });

  it('should show formatted play count', () => {
    render(<VideoCard video={mockVideo} />);

    expect(screen.getByText('1万')).toBeInTheDocument();
  });
});
```

### 3.2 查询策略

按优先级使用以下查询方法：

| 优先级 | 方法 | 说明 |
|--------|------|------|
| 最高 | `getByRole` | 最接近用户体验，兼顾无障碍 |
| 高 | `getByLabelText` | 表单控件首选 |
| 中 | `getByPlaceholderText` | 输入框备选 |
| 中 | `getByText` | 非交互性文本 |
| 低 | `getByTestId` | 无其他查询方式时的兜底方案 |

```typescript
// ✅ 优先
screen.getByRole('button', { name: '播放第1集' });
screen.getByRole('heading', { name: '热门短剧', level: 2 });
screen.getByLabelText('手机号');

// ✅ 备选
screen.getByPlaceholderText('搜索短剧');
screen.getByText('暂无更多内容');

// ❌ 避免过度使用（仅兜底）
screen.getByTestId('video-card-1');
```

使用 `queryBy*` 验元素不存在：

```typescript
expect(screen.queryByRole('alert')).not.toBeInTheDocument();
```

### 3.3 异步组件

涉及异步操作的组件测试：

```typescript
import { render, screen, waitFor } from '@testing-library/react';

describe('VideoSearch', () => {
  it('should show loading skeleton while searching', async () => {
    // 模拟慢速 API
    vi.mocked(fetchVideoList).mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 100))
    );

    render(<VideoSearch />);

    // 等待 loading 出现
    expect(screen.getByRole('status')).toBeInTheDocument();

    // 等待 loading 消失
    await waitFor(() => {
      expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });
  });

  it('should show results after search', async () => {
    const mockResults = { /* ... */ };
    vi.mocked(fetchVideoList).mockResolvedValue(mockResults);

    render(<VideoSearch />);

    // findBy 自带 waitFor
    expect(await screen.findByText('霸道总裁的甜蜜陷阱')).toBeInTheDocument();
  });

  it('should show error on failed search', async () => {
    vi.mocked(fetchVideoList).mockRejectedValue(new Error('搜索失败'));

    render(<VideoSearch />);

    expect(await screen.findByRole('alert')).toHaveTextContent('搜索失败');
  });
});
```

### 3.4 Provider 包裹

测试需要 Provider 的组件时，封装 render 函数：

```typescript
// src/test/test-utils.tsx
import { render, type RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,       // 测试中不重试
        gcTime: 0,          // 测试中不缓存
      },
      mutations: {
        retry: false,
      },
    },
  });
}

interface AllTheProvidersProps {
  children: React.ReactNode;
}

export function AllTheProviders({ children }: AllTheProvidersProps) {
  const queryClient = createTestQueryClient();

  return (
    <QueryClientProvider client={queryClient}>
      {/* 添加其他 Provider：Router、Theme、i18n 等 */}
      {children}
    </QueryClientProvider>
  );
}

export function renderWithProviders(
  ui: React.ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>,
) {
  return render(ui, { wrapper: AllTheProviders, ...options });
}

// 使用
import { renderWithProviders } from '@/test/test-utils';

it('should render with providers', () => {
  renderWithProviders(<VideoList category="trending" />);
  // ...
});
```

---

## 4. E2E 测试

### 4.1 Playwright

```bash
npm install -D @playwright/test
npx playwright install
```

配置文件 `playwright.config.ts`：

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html'],
    ['list'],
  ],
  use: {
    baseURL: process.env.NEXT_PUBLIC_SITE_URL || 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'mobile-chrome',
      use: {
        ...devices['Pixel 7'],
        locale: 'zh-CN',
      },
    },
    {
      name: 'mobile-safari',
      use: {
        ...devices['iPhone 14 Pro'],
        locale: 'zh-CN',
      },
    },
    {
      name: 'desktop-chrome',
      use: {
        ...devices['Desktop Chrome'],
        locale: 'zh-CN',
      },
    },
  ],
  webServer: process.env.CI
    ? {
        command: 'npm run build && npm run start',
        port: 3000,
        reuseExistingServer: true,
      }
    : undefined,
});
```

基本测试结构：

```typescript
// e2e/home.spec.ts
import { test, expect } from '@playwright/test';

test.describe('首页', () => {
  test('应该显示热门短剧列表', async ({ page }) => {
    await page.goto('/');

    // 等待标题出现
    await expect(page.getByRole('heading', { name: '热门短剧' })).toBeVisible();

    // 等待视频列表加载
    await page.waitForResponse(response =>
      response.url().includes('/api/videos') && response.status() === 200
    );

    // 至少有一个剧集卡片
    const cards = page.getByTestId('video-card');
    await expect(cards.first()).toBeVisible();
  });

  test('点击剧集卡片应跳转到详情页', async ({ page }) => {
    await page.goto('/');

    const card = page.getByTestId('video-card').first();
    await card.click();

    await expect(page).toHaveURL(/\/video\/.+/);
  });
});
```

### 4.2 测试场景

**关键路径必须覆盖：**

| 路径 | 场景 | 优先级 |
|------|------|--------|
| 浏览 | 首页加载 → 瀑布流浏览 → 点击进入详情 | P0 |
| 搜索 | 输入关键词 → 查看搜索结果 → 无结果提示 | P0 |
| 播放 | 点击剧集 → 视频播放 → 暂停/继续 → 下一集 | P0 |
| 注册 | 打开注册页 → 输入手机号 → 获取验证码 → 完成注册 | P0 |
| 登录 | 打开登录页 → 短信登录 → 进入首页 | P0 |
| 个人 | 进入个人中心 → 查看历史 → 切换Tab | P1 |
| 分享 | 点击分享 → 选择平台 → 分享成功 | P1 |

**E2E 测试命名约定：**

- 文件名：`{feature}.spec.ts`（如 `home.spec.ts`、`player.spec.ts`）
- 测试目录：`e2e/`（与 `src/` 同级）
- 使用 `test.describe` 分组，`test('应该...')` 命名

### 4.3 CI 集成

```yaml
# .github/workflows/playwright.yml
name: Playwright Tests

on:
  push:
    branches: [main, master]
  pull_request:
    branches: [main, master]

jobs:
  e2e:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        shard: [1, 2, 3, 4]

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20

      - name: Install dependencies
        run: npm ci

      - name: Install Playwright browsers
        run: npx playwright install --with-deps chromium

      - name: Build the app
        run: npm run build

      - name: Run Playwright tests (shard ${{ matrix.shard }})
        run: npx playwright test --shard=${{ matrix.shard }}/${{ strategy.job-total }}

      - name: Upload test results
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report-${{ matrix.shard }}
          path: |
            playwright-report/
            test-results/
```

**CI 中的关键配置：**

- `retries: 2`：失败重试 2 次（避免 flaky test 误报）
- `trace: 'on-first-retry'`：首次失败时保存 trace
- `screenshot: 'only-on-failure'`：失败时截图
- `video: 'retain-on-failure'`：失败时保留录像
- 使用 shard 并行执行加速

---

## 5. 视觉回归测试

### 5.1 Playwright Screenshot

```typescript
// e2e/visual/home.spec.ts
import { test, expect } from '@playwright/test';

test.describe('视觉回归 — 首页', () => {
  test('首页完整截图对比', async ({ page }) => {
    await page.goto('/');

    // 等待关键内容渲染
    await expect(page.getByRole('heading', { name: '热门短剧' })).toBeVisible();
    await page.waitForTimeout(1000); // 等待动画完成

    await expect(page).toHaveScreenshot('home-page.png', {
      fullPage: true,
      maxDiffPixelRatio: 0.01,  // 最多 1% 像素差异
    });
  });

  test('剧集卡片截图对比', async ({ page }) => {
    await page.goto('/');

    const card = page.getByTestId('video-card').first();
    await expect(card).toHaveScreenshot('video-card.png', {
      threshold: 0.1, // 最多 10% 颜色差异容忍度
    });
  });
});
```

**更新基准截图：**

```bash
npx playwright test --update-snapshots
```

### 5.2 Storybook + Chromatic

Storybook 暂未强制引入，团队需要时可按以下步骤接入：

```bash
npx storybook@latest init
npm install -D chromatic
```

- 每个组件编写 `.stories.tsx`
- 提交到 Git → Chromatic 自动截图 → 对比主分支
- 差异由人工审核接受/拒绝

---

## 6. 测试覆盖率

### 6.1 工具

Vitest 内置 Istanbul（通过 `@vitest/coverage-v8`），在 `vitest.config.ts` 中配置：

```bash
npm run test -- --coverage
# 生成 coverage/index.html 可视化报告
```

### 6.2 最低覆盖率

| 指标 | 最低 | 理想 |
|------|------|------|
| Statements（语句） | 70% | 85% |
| Branches（分支） | 60% | 80% |
| Functions（函数） | 70% | 85% |
| Lines（行） | 70% | 85% |

在 `vitest.config.ts` 中配置阈值：

```typescript
// vitest.config.ts
coverage: {
  provider: 'v8',
  reporter: ['text', 'lcov', 'html'],
  thresholds: {
    statements: 70,
    branches: 60,
    functions: 70,
    lines: 70,
  },
},
```

**不追求覆盖率场景：**

- 纯 UI 展示组件（无逻辑分支）
- 第三方库的 wrapper
- 配置文件

**必须覆盖场景：**

- 所有 Zod Schema 定义
- 数据转换/格式化函数
- 自定义 Hook 的核心逻辑
- 错误处理路径
