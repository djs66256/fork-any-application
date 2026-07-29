import { afterEach, describe, expect, it, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllEnvs();
  vi.resetModules();
});

describe('config', () => {
  it('should have app config with default values when env vars are not set', async () => {
    const { config } = await import('../config');

    expect(config.app.name).toBeDefined();
    expect(config.app.version).toBeDefined();
    expect(config.app.env).toBeDefined();
  });

  it('should have supabase config with empty string defaults', async () => {
    const { config } = await import('../config');

    expect(config.supabase).toBeDefined();
    expect(typeof config.supabase.url).toBe('string');
    expect(typeof config.supabase.anonKey).toBe('string');
    expect(typeof config.supabase.serviceRoleKey).toBe('string');
  });

  it('should have redis config with default url', async () => {
    const { config } = await import('../config');

    expect(config.redis).toBeDefined();
    expect(config.redis.url).toBe('redis://localhost:6379');
  });

  it('should default player history repository to mock', async () => {
    const { config } = await import('../config');

    expect(config.player.historyRepository).toBe('mock');
  });

  it('should default comments repository to mock', async () => {
    const { config } = await import('../config');

    expect(config.comments.repository).toBe('mock');
  });

  it('should expose environment-backed supabase keys', async () => {
    const { config } = await import('../config');

    expect(config.supabase.url).toBeDefined();
    expect(config.supabase.anonKey).toBeDefined();
    expect(config.supabase.serviceRoleKey).toBeDefined();
  });

  it('should read auth bypass settings from current env', async () => {
    vi.stubEnv('AUTH_ALLOW_TEST_OTP_BYPASS', 'true');
    vi.stubEnv('AUTH_TEST_PHONE', '13900139000');
    vi.stubEnv('AUTH_TEST_OTP_CODE', '654321');
    vi.stubEnv('AUTH_ACCESS_TOKEN_TTL_SECONDS', '120');
    vi.stubEnv('AUTH_REFRESH_TOKEN_TTL_SECONDS', '3600');

    const { config } = await import('../config');

    expect(config.auth.allowTestOtpBypass).toBe(true);
    expect(config.auth.testPhone).toBe('13900139000');
    expect(config.auth.testOtpCode).toBe('654321');
    expect(config.auth.accessTokenTtlSeconds).toBe(120);
    expect(config.auth.refreshTokenTtlSeconds).toBe(3600);
  });
});
