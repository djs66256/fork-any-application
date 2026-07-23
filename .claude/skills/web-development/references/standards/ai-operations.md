# AI 操作与自动化 — Web

> 本文档定义 Web 端 AI agent 可执行的浏览器操作与自动化能力。

---

## 1. Playwright / Puppeteer

本项目使用 **Playwright** 作为浏览器自动化工具（与 E2E 测试保持一致）。以下为 AI agent 可直接使用的 Playwright 操作模式。

### 1.1 启动与连接

```typescript
// lib/automation/browser.ts
import { chromium, Browser, BrowserContext, Page } from '@playwright/test';

export interface BrowserSession {
  browser: Browser;
  context: BrowserContext;
  page: Page;
}

/**
 * 启动浏览器并创建全新会话
 */
export async function launchBrowser(
  options?: {
    headless?: boolean;
    viewport?: { width: number; height: number };
  }
): Promise<BrowserSession> {
  const { headless = true, viewport = { width: 390, height: 844 } } = options ?? {};

  const browser = await chromium.launch({
    headless,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
    ],
  });

  const context = await browser.newContext({
    viewport,
    userAgent:
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15',
    locale: 'zh-CN',
    // 模拟移动端
    hasTouch: true,
    isMobile: true,
  });

  const page = await context.newPage();
  return { browser, context, page };
}

/**
 * 连接到已有浏览器实例（调试模式）
 * 前提：浏览器以 --remote-debugging-port=9222 启动
 */
export async function connectToExisting(): Promise<BrowserSession> {
  const browser = await chromium.connectOverCDP('http://localhost:9222');
  const defaultContext = browser.contexts()[0]!;
  const page = defaultContext.pages()[0] ?? (await defaultContext.newPage());
  return { browser, context: defaultContext, page };
}

/**
 * 关闭浏览器会话
 */
export async function closeBrowser(session: BrowserSession): Promise<void> {
  await session.context.close();
  await session.browser.close();
}
```

### 1.2 页面导航

```typescript
// lib/automation/navigation.ts
import type { Page } from '@playwright/test';

/**
 * 导航到指定 URL，等待页面加载完成
 */
export async function navigateTo(
  page: Page,
  url: string,
  options?: {
    waitUntil?: 'load' | 'domcontentloaded' | 'networkidle';
    timeout?: number;
  }
): Promise<void> {
  const { waitUntil = 'networkidle', timeout = 30_000 } = options ?? {};

  await page.goto(url, { waitUntil, timeout });
}

/**
 * 刷新页面
 */
export async function reload(page: Page): Promise<void> {
  await page.reload({ waitUntil: 'networkidle' });
}

/**
 * 返回上一页
 */
export async function goBack(page: Page): Promise<void> {
  await page.goBack({ waitUntil: 'networkidle' });
}

/**
 * 前进到下一页
 */
export async function goForward(page: Page): Promise<void> {
  await page.goForward({ waitUntil: 'networkidle' });
}
```

### 1.3 元素交互

```typescript
// lib/automation/interaction.ts
import type { Page, Locator } from '@playwright/test';

/**
 * 点击元素
 * 支持多种定位方式：text、role、selector、testId
 */
export async function clickElement(
  page: Page,
  options: {
    text?: string;
    role?: { name: string; role: string };
    selector?: string;
    testId?: string;
    force?: boolean;
  }
): Promise<void> {
  let locator: Locator;

  if (options.testId) {
    locator = page.getByTestId(options.testId);
  } else if (options.role) {
    locator = page.getByRole(options.role.role as 'button' | 'link' | 'textbox', {
      name: options.role.name,
    });
  } else if (options.text) {
    locator = page.getByText(options.text, { exact: true });
  } else if (options.selector) {
    locator = page.locator(options.selector);
  } else {
    throw new Error('Must provide one of: text, role, selector, testId');
  }

  await locator.click({ force: options.force });
}

/**
 * 在输入框中填入文本
 */
export async function fillInput(
  page: Page,
  options: {
    placeholder?: string;
    label?: string;
    selector?: string;
    testId?: string;
    value: string;
    clear?: boolean; // 是否先清空
  }
): Promise<void> {
  let locator: Locator;

  if (options.testId) {
    locator = page.getByTestId(options.testId);
  } else if (options.label) {
    locator = page.getByLabel(options.label);
  } else if (options.placeholder) {
    locator = page.getByPlaceholder(options.placeholder);
  } else if (options.selector) {
    locator = page.locator(options.selector);
  } else {
    throw new Error('Must provide one of: placeholder, label, selector, testId');
  }

  if (options.clear) {
    await locator.clear();
  }
  await locator.fill(options.value);
}

/**
 * 选择下拉选项
 */
export async function selectOption(
  page: Page,
  options: {
    selector?: string;
    testId?: string;
    label?: string;
    value?: string;
    text?: string;
  }
): Promise<void> {
  let locator: Locator;

  if (options.testId) {
    locator = page.getByTestId(options.testId);
  } else if (options.label) {
    locator = page.getByLabel(options.label);
  } else if (options.selector) {
    locator = page.locator(options.selector);
  } else {
    throw new Error('Must provide one of: selector, testId, label');
  }

  if (options.value) {
    await locator.selectOption(options.value);
  } else if (options.text) {
    await locator.selectOption({ label: options.text });
  }
}

/**
 * 悬停在元素上
 */
export async function hoverElement(
  page: Page,
  options: { selector?: string; testId?: string; text?: string }
): Promise<void> {
  let locator: Locator;

  if (options.testId) {
    locator = page.getByTestId(options.testId);
  } else if (options.text) {
    locator = page.getByText(options.text);
  } else if (options.selector) {
    locator = page.locator(options.selector);
  } else {
    throw new Error('Must provide one of: selector, testId, text');
  }

  await locator.hover();
}

/**
 * 滑动操作（移动端常用：上下滑动、左右滑动）
 */
export async function swipe(
  page: Page,
  direction: 'up' | 'down' | 'left' | 'right',
  options?: { selector?: string; distance?: number; duration?: number }
): Promise<void> {
  const { selector, distance = 300, duration = 500 } = options ?? {};

  const target = selector ? page.locator(selector) : page.locator('body');

  // 先获取元素中心坐标
  const box = await target.boundingBox();
  if (!box) throw new Error('Element not found for swipe');

  const centerX = box.x + box.width / 2;
  const centerY = box.y + box.height / 2;

  let endX = centerX;
  let endY = centerY;

  switch (direction) {
    case 'up':
      endY = centerY - distance;
      break;
    case 'down':
      endY = centerY + distance;
      break;
    case 'left':
      endX = centerX - distance;
      break;
    case 'right':
      endX = centerX + distance;
      break;
  }

  // 触摸滑动
  await page.mouse.move(centerX, centerY);
  await page.mouse.down();
  await page.mouse.move(endX, endY, { steps: 10 });
  await page.mouse.up();
}

/**
 * 滚动到指定位置
 */
export async function scrollTo(
  page: Page,
  options: { selector?: string; x?: number; y?: number }
): Promise<void> {
  const { selector, x = 0, y = 0 } = options;

  if (selector) {
    await page.locator(selector).scrollIntoViewIfNeeded();
  }

  await page.evaluate(({ scrollX, scrollY }) => {
    window.scrollTo(scrollX, scrollY);
  }, { scrollX: x, scrollY: y });
}

/**
 * 拖动元素
 */
export async function dragElement(
  page: Page,
  sourceSelector: string,
  targetSelector: string,
): Promise<void> {
  const source = page.locator(sourceSelector);
  const target = page.locator(targetSelector);

  await source.dragTo(target);
}
```

### 1.4 等待策略

```typescript
// lib/automation/wait.ts
import type { Page, Locator } from '@playwright/test';

/**
 * 等待选择器出现
 */
export async function waitForSelector(
  page: Page,
  selector: string,
  options?: { state?: 'attached' | 'detached' | 'visible' | 'hidden'; timeout?: number }
): Promise<Locator> {
  return page.waitForSelector(selector, {
    state: options?.state ?? 'visible',
    timeout: options?.timeout ?? 10_000,
  });
}

/**
 * 等待网络请求完成
 */
export async function waitForResponse(
  page: Page,
  urlPattern: string | RegExp,
  options?: { timeout?: number }
): Promise<void> {
  const response = await page.waitForResponse(
    (response) => {
      if (typeof urlPattern === 'string') {
        return response.url().includes(urlPattern);
      }
      return urlPattern.test(response.url());
    },
    { timeout: options?.timeout ?? 15_000 }
  );
}

/**
 * 等待页面导航完成
 */
export async function waitForNavigation(
  page: Page,
  options?: { timeout?: number }
): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout: options?.timeout ?? 30_000 });
}

/**
 * 等待页面无网络请求（适合 SPA 中的页面跳转）
 */
export async function waitForNetworkIdle(
  page: Page,
  options?: { timeout?: number; idleTime?: number }
): Promise<void> {
  const { timeout = 30_000, idleTime = 500 } = options ?? {};

  await page.waitForLoadState('networkidle', { timeout });
  // 额外等待确保异步请求也完成
  await page.waitForTimeout(idleTime);
}

/**
 * 等待文案出现
 */
export async function waitForText(
  page: Page,
  text: string,
  options?: { timeout?: number }
): Promise<void> {
  await page.getByText(text, { exact: false }).waitFor({
    state: 'visible',
    timeout: options?.timeout ?? 10_000,
  });
}

/**
 * 等待元素可用（可交互）
 */
export async function waitForEnabled(
  page: Page,
  selector: string,
  options?: { timeout?: number }
): Promise<Locator> {
  const locator = page.locator(selector);
  await locator.waitFor({ state: 'visible', timeout: options?.timeout ?? 10_000 });
  return locator;
}
```

---

## 2. 截图与视觉比对

### 2.1 全页截图

```typescript
// lib/automation/screenshot.ts
import type { Page } from '@playwright/test';
import path from 'path';

/**
 * 全页截图（包含滚动区域外的内容）
 */
export async function fullPageScreenshot(
  page: Page,
  options?: { outputPath?: string; quality?: number }
): Promise<Buffer> {
  const { outputPath, quality = 80 } = options ?? {};

  return page.screenshot({
    path: outputPath,
    fullPage: true,
    type: 'png',
    ...(quality && { quality }),
  });
}

/**
 * 视口截图（仅可视区域）
 */
export async function viewportScreenshot(
  page: Page,
  options?: { outputPath?: string }
): Promise<Buffer> {
  return page.screenshot({
    path: options?.outputPath,
    fullPage: false,
    type: 'png',
  });
}
```

### 2.2 元素截图

```typescript
/**
 * 单个元素截图
 */
export async function elementScreenshot(
  page: Page,
  selector: string,
  options?: { outputPath?: string; padding?: number }
): Promise<Buffer> {
  const element = page.locator(selector);
  const { padding = 0 } = options ?? {};

  const box = await element.boundingBox();
  if (!box) throw new Error(`Element not found: ${selector}`);

  // 可裁剪到元素区域
  const clip = {
    x: box.x - padding,
    y: box.y - padding,
    width: box.width + padding * 2,
    height: box.height + padding * 2,
  };

  return page.screenshot({
    path: options?.outputPath,
    clip,
    type: 'png',
  });
}

/**
 * 批量截图页面中的多个元素
 * 用于采集页面中所有剧集卡片、图标等
 */
export async function batchElementScreenshot(
  page: Page,
  selector: string,
  outputDir: string,
): Promise<string[]> {
  const elements = page.locator(selector);
  const count = await elements.count();
  const paths: string[] = [];

  for (let i = 0; i < count; i++) {
    const element = elements.nth(i);
    const screenshot = await element.screenshot({ type: 'png' });
    const filePath = path.join(outputDir, `element-${i}.png`);
    const fs = await import('fs/promises');
    await fs.writeFile(filePath, screenshot);
    paths.push(filePath);
  }

  return paths;
}
```

### 2.3 视觉回归

```typescript
import { expect } from '@playwright/test';

/**
 * Playwright 内置视觉回归（toHaveScreenshot）
 * 首次运行时生成基准截图，后续运行对比差异
 */
export async function visualRegression(
  page: Page,
  snapshotName: string,
  options?: {
    fullPage?: boolean;
    selector?: string;
    maxDiffPixelRatio?: number;
    threshold?: number;
  }
): Promise<void> {
  const { fullPage = false, selector, maxDiffPixelRatio = 0.01, threshold = 0.2 } = options ?? {};

  let target = page;

  if (selector) {
    const element = page.locator(selector);
    await expect(element).toHaveScreenshot(`${snapshotName}.png`, {
      fullPage: false,
      maxDiffPixelRatio,
      threshold,
    });
  } else {
    await expect(target).toHaveScreenshot(`${snapshotName}.png`, {
      fullPage,
      maxDiffPixelRatio,
      threshold,
    });
  }
}

/**
 * 对比两张截图的差异（不使用 Playwright test runner 时）
 */
export async function compareScreenshots(
  actual: Buffer,
  expected: Buffer,
  options?: { threshold?: number }
): Promise<{ match: boolean; diffPercentage: number }> {
  const { threshold = 0.01 } = options ?? {};

  // 使用 pixelmatch 进行像素级对比
  const { default: pixelmatch } = await import('pixelmatch');
  const { PNG } = await import('pngjs');

  const actualPng = PNG.sync.read(actual);
  const expectedPng = PNG.sync.read(expected);

  if (actualPng.width !== expectedPng.width || actualPng.height !== expectedPng.height) {
    return { match: false, diffPercentage: 1 };
  }

  const { width, height } = actualPng;
  const diff = new PNG({ width, height });
  const mismatchedPixels = pixelmatch(
    actualPng.data,
    expectedPng.data,
    diff.data,
    width,
    height,
    { threshold },
  );

  const totalPixels = width * height;
  const diffPercentage = mismatchedPixels / totalPixels;

  return {
    match: diffPercentage <= threshold,
    diffPercentage,
  };
}
```

---

## 3. 网络请求拦截

### 3.1 API Mock

```typescript
// lib/automation/mock.ts
import type { Page, Route } from '@playwright/test';

interface MockConfig {
  url: string | RegExp;
  method?: string;
  status?: number;
  headers?: Record<string, string>;
  body: unknown;
}

/**
 * 拦截并模拟 API 响应
 */
export async function mockApi(page: Page, config: MockConfig): Promise<void> {
  const { url, method = '**', status = 200, headers = {}, body } = config;

  await page.route(
    (route: Route) => {
      const methodMatch = method === '**' || route.request().method() === method;
      const urlMatch = typeof url === 'string'
        ? route.request().url().includes(url)
        : url.test(route.request().url());
      return methodMatch && urlMatch;
    },
    async (route: Route) => {
      await route.fulfill({
        status,
        contentType: 'application/json',
        headers: {
          'Access-Control-Allow-Origin': '*',
          ...headers,
        },
        body: JSON.stringify(body),
      });
    }
  );
}

/**
 * Mock 视频列表接口（模拟空数据）
 */
export async function mockEmptyVideoList(page: Page): Promise<void> {
  await mockApi(page, {
    url: '/api/videos',
    body: { data: [], total: 0, page: 1, pageSize: 20, hasMore: false },
  });
}

/**
 * Mock 视频详情接口
 */
export async function mockVideoDetail(page: Page, overrides?: Partial<Record<string, unknown>>): Promise<void> {
  await mockApi(page, {
    url: /\/api\/videos\/[a-f0-9-]+$/,
    body: {
      id: '00000000-0000-0000-0000-000000000001',
      title: 'Mock 短剧',
      coverUrl: 'https://cdn.shortdrama.com/covers/mock-cover.jpg',
      episodeCount: 20,
      duration: 180,
      tags: ['都市', '爱情'],
      playCount: 10000,
      likeCount: 5000,
      description: '这是一部 mock 短剧，用于自动化测试。',
      episodes: [
        { id: 'ep-1', title: '第1集', duration: 180, videoUrl: 'https://cdn.shortdrama.com/mock.mp4', episodeNumber: 1 },
      ],
      relatedVideos: [],
      createdAt: '2026-07-01T00:00:00Z',
      updatedAt: '2026-07-23T00:00:00Z',
      ...overrides,
    },
  });
}

/**
 * 清除所有 mock 规则
 */
export async function clearAllMocks(page: Page): Promise<void> {
  await page.unrouteAll({ behavior: 'ignoreErrors' });
}
```

### 3.2 请求监听

```typescript
import type { Page, Request, Response } from '@playwright/test';

/**
 * 监听所有 API 请求并记录日志
 */
export function logApiRequests(page: Page): void {
  const requests: Array<{ url: string; method: string; timestamp: number }> = [];

  page.on('request', (request: Request) => {
    if (request.url().includes('/api/')) {
      requests.push({
        url: request.url(),
        method: request.method(),
        timestamp: Date.now(),
      });
    }
  });

  page.on('response', (response: Response) => {
    if (response.url().includes('/api/')) {
      const entry = requests.find(r => r.url === response.url());
      if (entry) {
        console.log(`[API] ${response.status()} ${entry.method} ${entry.url}`);
      }
    }
  });
}

/**
 * 等待特定请求发生
 */
export async function waitForRequest(
  page: Page,
  urlPattern: string | RegExp,
  options?: { timeout?: number }
): Promise<Request> {
  const pattern = typeof urlPattern === 'string'
    ? (url: string) => url.includes(urlPattern)
    : (url: string) => urlPattern.test(url);

  return page.waitForRequest(
    (request) => pattern(request.url()),
    { timeout: options?.timeout ?? 10_000 }
  );
}

/**
 * 捕获所有请求的 URL 列表（用于分析页面加载了哪些资源）
 */
export function collectRequestUrls(page: Page): { stop: () => string[] } {
  const urls: string[] = [];

  const handler = (request: Request) => {
    urls.push(request.url());
  };

  page.on('request', handler);

  return {
    stop: () => {
      page.off('request', handler);
      return [...urls];
    },
  };
}
```

### 3.3 请求修改

```typescript
import type { Page, Route } from '@playwright/test';

/**
 * 修改请求 Header（如注入 Authorization）
 */
export async function injectAuthHeader(page: Page, token: string): Promise<void> {
  await page.route('**/api/**', async (route: Route) => {
    const headers = {
      ...route.request().headers(),
      'Authorization': `Bearer ${token}`,
    };
    await route.continue({ headers });
  });
}

/**
 * 修改请求 Body
 */
export async function modifyRequestBody(
  page: Page,
  urlPattern: string | RegExp,
  modifier: (body: unknown) => unknown
): Promise<void> {
  await page.route(
    (route: Route) => {
      const match = typeof urlPattern === 'string'
        ? route.request().url().includes(urlPattern)
        : urlPattern.test(route.request().url());
      return match && route.request().method() === 'POST';
    },
    async (route: Route) => {
      const originalBody = route.request().postDataJSON() ?? {};
      const modifiedBody = modifier(originalBody);
      await route.continue({
        postData: JSON.stringify(modifiedBody),
      });
    }
  );
}

/**
 * 阻断指定域名的请求（模拟离线场景）
 */
export async function blockRequests(page: Page, domains: string[]): Promise<void> {
  await page.route('**/*', (route: Route) => {
    const url = route.request().url();
    if (domains.some(domain => url.includes(domain))) {
      route.abort('aborted');
    } else {
      route.continue();
    }
  });
}

/**
 * 模拟慢网络
 */
export async function simulateSlowNetwork(page: Page, delayMs: number = 2000): Promise<void> {
  await page.route('**/*', async (route: Route) => {
    await new Promise(resolve => setTimeout(resolve, delayMs));
    await route.continue();
  });
}
```

---

## 4. 日志与性能采集

### 4.1 Console 日志

```typescript
import type { Page, ConsoleMessage } from '@playwright/test';

interface ConsoleEntry {
  type: string;
  text: string;
  timestamp: number;
  location?: string;
}

/**
 * 收集浏览器 Console 日志
 */
export function collectConsoleLogs(page: Page): { logs: ConsoleEntry[]; stop: () => ConsoleEntry[] } {
  const logs: ConsoleEntry[] = [];

  const handler = (msg: ConsoleMessage) => {
    logs.push({
      type: msg.type(),
      text: msg.text(),
      timestamp: Date.now(),
      location: msg.location().url
        ? `${msg.location().url}:${msg.location().lineNumber}`
        : undefined,
    });
  };

  page.on('console', handler);

  return {
    logs,
    stop: () => {
      page.off('console', handler);
      return [...logs];
    },
  };
}

/**
 * 过滤出错误日志
 */
export function getConsoleErrors(logs: ConsoleEntry[]): ConsoleEntry[] {
  return logs.filter(log => log.type === 'error' || log.type === 'warning');
}

/**
 * 监听页面未捕获异常
 */
export function listenForPageErrors(page: Page): { errors: Error[]; stop: () => Error[] } {
  const errors: Error[] = [];

  const handler = (error: Error) => {
    errors.push(error);
  };

  page.on('pageerror', handler);

  return {
    errors,
    stop: () => {
      page.off('pageerror', handler);
      return [...errors];
    },
  };
}
```

### 4.2 Performance 数据

```typescript
import type { Page } from '@playwright/test';

interface WebVitalsResult {
  FCP?: number;    // First Contentful Paint (ms)
  LCP?: number;    // Largest Contentful Paint (ms)
  FID?: number;    // First Input Delay (ms)
  CLS?: number;    // Cumulative Layout Shift
  TTFB?: number;   // Time to First Byte (ms)
  INP?: number;    // Interaction to Next Paint (ms)
}

/**
 * 采集 Core Web Vitals
 */
export async function collectWebVitals(page: Page): Promise<WebVitalsResult> {
  return page.evaluate(async () => {
    const result: Record<string, number> = {};

    // 获取 Paint Timing API
    const paintEntries = performance.getEntriesByType('paint');
    const fcpEntry = paintEntries.find(e => e.name === 'first-contentful-paint');
    if (fcpEntry) result.FCP = Math.round(fcpEntry.startTime);

    // 获取 Navigation Timing
    const navEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
    if (navEntry) result.TTFB = Math.round(navEntry.responseStart - navEntry.requestStart);

    // 通过 PerformanceObserver 获取 LCP
    try {
      const lcpValue = await new Promise<number | undefined>((resolve) => {
        let lcp: number | undefined;
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          if (entries.length > 0) {
            lcp = entries[entries.length - 1]!.startTime;
          }
        });
        observer.observe({ type: 'largest-contentful-paint', buffered: true });
        setTimeout(() => {
          observer.disconnect();
          resolve(lcp);
        }, 1000);
      });
      if (lcpValue) result.LCP = Math.round(lcpValue);
    } catch { /* LCP 不可用时忽略 */ }

    // 获取 CLS
    try {
      let clsValue = 0;
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          const layoutShift = entry as LayoutShift;
          if (!layoutShift.hadRecentInput) {
            clsValue += layoutShift.value;
          }
        }
      });
      observer.observe({ type: 'layout-shift', buffered: true });
      await new Promise(resolve => setTimeout(resolve, 1000));
      observer.disconnect();
      result.CLS = Math.round(clsValue * 1000) / 1000;
    } catch { /* CLS 不可用时忽略 */ }

    return result;
  });
}

/**
 * 获取页面资源加载性能数据
 */
export async function getResourceTiming(page: Page): Promise<
  Array<{
    name: string;
    type: string;
    duration: number;
    size: number;
  }>
> {
  return page.evaluate(() => {
    return performance.getEntriesByType('resource').map((entry) => {
      const res = entry as PerformanceResourceTiming;
      return {
        name: res.name,
        type: res.initiatorType,
        duration: Math.round(res.duration),
        size: res.transferSize || 0,
      };
    });
  });
}

/**
 * 测量页面中 JavaScript 堆内存使用情况
 */
export async function getMemoryUsage(page: Page): Promise<{
  jsHeapSizeLimit: number;
  totalJSHeapSize: number;
  usedJSHeapSize: number;
} | null> {
  return page.evaluate(() => {
    const memory = (performance as Performance & { memory?: {
      jsHeapSizeLimit: number;
      totalJSHeapSize: number;
      usedJSHeapSize: number;
    } }).memory;

    return memory ? {
      jsHeapSizeLimit: memory.jsHeapSizeLimit,
      totalJSHeapSize: memory.totalJSHeapSize,
      usedJSHeapSize: memory.usedJSHeapSize,
    } : null;
  });
}
```

### 4.3 网络日志

```typescript
import type { Page } from '@playwright/test';
import fs from 'fs/promises';

/**
 * 导出 HAR 文件（HTTP Archive）
 * 记录完整请求/响应信息，用于离线分析
 */
export async function exportHar(
  page: Page,
  outputPath: string,
): Promise<void> {
  // Playwright 通过 BrowserContext 的 HAR 功能导出
  // 注意：需要在创建 context 时启用 recordHar
  // 或者使用 page.route 手动收集请求/响应

  const entries = await page.evaluate(async () => {
    const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[];
    return resources.map(r => ({
      name: r.name,
      initiatorType: r.initiatorType,
      duration: Math.round(r.duration),
      startTime: Math.round(r.startTime),
      transferSize: r.transferSize,
      encodedBodySize: r.encodedBodySize,
      decodedBodySize: r.decodedBodySize,
    }));
  });

  const har = {
    log: {
      version: '1.2',
      creator: { name: 'ai-operator', version: '1.0' },
      entries,
    },
  };

  await fs.writeFile(outputPath, JSON.stringify(har, null, 2), 'utf-8');
}

/**
 * 创建带 HAR 录制的浏览器会话（用于完整的网络流量记录）
 */
import { type BrowserContext } from '@playwright/test';

export async function createHarRecording(
  browser: import('@playwright/test').Browser,
  harPath: string,
): Promise<BrowserContext> {
  const context = await browser.newContext({
    recordHar: {
      path: harPath,
      mode: 'full', // 记录完整请求和响应体
    },
  });
  return context;
}
```
