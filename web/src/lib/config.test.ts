import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('config', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
  });

  it('should return default app name when env var is not set', async () => {
    const { config } = await import('@/lib/config');
    expect(config.app.name).toBe('ShortDrama');
  });

  it('should return default version when env var is not set', async () => {
    const { config } = await import('@/lib/config');
    expect(config.app.version).toBe('0.1.0');
  });

  it('should use NEXT_PUBLIC_APP_NAME from environment', async () => {
    vi.stubEnv('NEXT_PUBLIC_APP_NAME', 'TestApp');
    const { config } = await import('@/lib/config');
    expect(config.app.name).toBe('TestApp');
  });

  it('should use NEXT_PUBLIC_APP_VERSION from environment', async () => {
    vi.stubEnv('NEXT_PUBLIC_APP_VERSION', '2.0.0');
    const { config } = await import('@/lib/config');
    expect(config.app.version).toBe('2.0.0');
  });

  it('should return NODE_ENV as env', async () => {
    const { config } = await import('@/lib/config');
    expect(config.app.env).toBeDefined();
  });

  it('should expose mall defaults', async () => {
    const { config } = await import('@/lib/config');
    expect(config.mall.route).toBe('/mall');
    expect(config.mall.searchFallbackRoute).toBe('/search');
    expect(config.mall.pageSize).toBe(20);
    expect(config.mall.bridgeEnabled).toBe(true);
  });

  it('should honor mall bridge env override', async () => {
    vi.stubEnv('NEXT_PUBLIC_MALL_BRIDGE_ENABLED', 'false');
    const { config } = await import('@/lib/config');
    expect(config.mall.bridgeEnabled).toBe(false);
  });

  it('should expose earn defaults', async () => {
    const { config } = await import('@/lib/config');
    expect(config.earn.route).toBe('/earn');
    expect(config.earn.bridgeEnabled).toBe(true);
    expect(config.earn.browserFeedback.loginUnavailable).toBe('暂时无法打开登录，请稍后再试');
    expect(config.earn.browserFeedback.taskRequiresApp).toBe('请在 App 内完成该任务');
    expect(config.earn.browserFeedback.taskUnavailable).toBe('当前任务暂不可用，请稍后重试');
    expect(config.earn.browserFeedback.taskInDevelopment).toBe('该任务开发中，敬请期待');
    expect(config.earn.browserFeedback.reloginRequired).toBe('请先登录后再领取奖励');
    expect(config.earn.browserFeedback.completionFailed).toBe('奖励领取失败，请稍后重试');
  });

  it('should honor earn bridge env override', async () => {
    vi.stubEnv('NEXT_PUBLIC_EARN_BRIDGE_ENABLED', 'false');
    const { config } = await import('@/lib/config');
    expect(config.earn.bridgeEnabled).toBe(false);
  });
});
