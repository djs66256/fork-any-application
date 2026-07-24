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

const { GET } = await import('../health/route');

describe('GET /api/health', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCheckSupabaseHealth.mockResolvedValue(true);
    mockCheckRedisHealth.mockResolvedValue(true);
  });

  it('should return 200 with health status', async () => {
    const response = await GET();
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.status).toBe('ok');
    expect(body.version).toBe('0.1.0');
    expect(body.timestamp).toBeDefined();
    expect(body.services).toBeDefined();
    expect(body.services.database).toBe('connected');
    expect(body.services.redis).toBe('connected');
  });

  it('should return degraded when db is down', async () => {
    mockCheckSupabaseHealth.mockResolvedValue(false);

    const response = await GET();
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.status).toBe('degraded');
    expect(body.services.database).toBe('disconnected');
    expect(body.services.redis).toBe('connected');
  });
});
