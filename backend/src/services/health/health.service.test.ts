import { describe, it, expect, vi, beforeEach } from 'vitest';

const { mockCheckSupabaseHealth, mockCheckRedisHealth } = vi.hoisted(() => ({
  mockCheckSupabaseHealth: vi.fn(),
  mockCheckRedisHealth: vi.fn(),
}));

vi.mock('@/infrastructure/supabase', () => ({
  checkSupabaseHealth: mockCheckSupabaseHealth,
}));

vi.mock('@/infrastructure/redis', () => ({
  checkRedisHealth: mockCheckRedisHealth,
}));

vi.mock('@/lib/config', () => ({
  config: {
    app: {
      name: 'Test Backend',
      version: '0.1.0',
      env: 'test',
    },
  },
}));

import { HealthService } from './health.service';

describe('HealthService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return ok when both services are healthy', async () => {
    mockCheckSupabaseHealth.mockResolvedValue(true);
    mockCheckRedisHealth.mockResolvedValue(true);

    const service = new HealthService();
    const result = await service.check();

    expect(result.status).toBe('ok');
    expect(result.services.database).toBe('connected');
    expect(result.services.redis).toBe('connected');
    expect(result.version).toBe('0.1.0');
    expect(result.timestamp).toBeDefined();
  });

  it('should return degraded when database is disconnected but redis is connected', async () => {
    mockCheckSupabaseHealth.mockResolvedValue(false);
    mockCheckRedisHealth.mockResolvedValue(true);

    const service = new HealthService();
    const result = await service.check();

    expect(result.status).toBe('degraded');
    expect(result.services.database).toBe('disconnected');
    expect(result.services.redis).toBe('connected');
  });

  it('should return degraded when redis is disconnected but database is connected', async () => {
    mockCheckSupabaseHealth.mockResolvedValue(true);
    mockCheckRedisHealth.mockResolvedValue(false);

    const service = new HealthService();
    const result = await service.check();

    expect(result.status).toBe('degraded');
    expect(result.services.database).toBe('connected');
    expect(result.services.redis).toBe('disconnected');
  });

  it('should return error when both services are disconnected', async () => {
    mockCheckSupabaseHealth.mockResolvedValue(false);
    mockCheckRedisHealth.mockResolvedValue(false);

    const service = new HealthService();
    const result = await service.check();

    expect(result.status).toBe('error');
    expect(result.services.database).toBe('disconnected');
    expect(result.services.redis).toBe('disconnected');
  });
});
