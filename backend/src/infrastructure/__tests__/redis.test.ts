import { describe, it, expect, beforeEach, vi } from 'vitest';

const mockPing = vi.fn();
const mockDisconnect = vi.fn();

const MockRedis = vi.fn().mockImplementation(() => ({
  ping: mockPing,
  disconnect: mockDisconnect,
}));

vi.mock('ioredis', () => ({
  default: MockRedis,
}));

vi.mock('@/lib/config', () => ({
  config: {
    redis: {
      url: 'redis://localhost:6379',
    },
  },
}));

describe('redis', () => {
  beforeEach(() => {
    vi.resetModules();
    MockRedis.mockClear();
    mockPing.mockClear();
    mockDisconnect.mockClear();
  });

  it('should create redis client with config url', async () => {
    const { getRedis } = await import('../redis');
    getRedis();
    expect(MockRedis).toHaveBeenCalledWith(
      'redis://localhost:6379',
      expect.objectContaining({
        lazyConnect: true,
      }),
    );
  });

  it('should return same instance on repeated getRedis calls', async () => {
    const { getRedis } = await import('../redis');
    const r1 = getRedis();
    const r2 = getRedis();
    expect(MockRedis).toHaveBeenCalledTimes(1);
    expect(r1).toBe(r2);
  });

  it('checkRedisHealth should return true when ping returns PONG', async () => {
    mockPing.mockResolvedValueOnce('PONG');
    const { checkRedisHealth } = await import('../redis');
    const result = await checkRedisHealth();
    expect(result).toBe(true);
  });

  it('checkRedisHealth should return false when ping fails', async () => {
    mockPing.mockRejectedValueOnce(new Error('Connection refused'));
    const { checkRedisHealth } = await import('../redis');
    const result = await checkRedisHealth();
    expect(result).toBe(false);
  });

  it('closeRedis should disconnect and reset singleton', async () => {
    const { getRedis, closeRedis } = await import('../redis');
    getRedis();
    expect(MockRedis).toHaveBeenCalledTimes(1);
    closeRedis();
    expect(mockDisconnect).toHaveBeenCalled();
    getRedis();
    expect(MockRedis).toHaveBeenCalledTimes(2);
  });
});
