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
});
