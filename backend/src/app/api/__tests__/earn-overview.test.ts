import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { clearLocalAuthSessions, createLocalAuthSession } from '@/services/auth/local-auth-session.store';
import { resetRepositoryRegistry, setEarnRepository } from '@/repositories/repository-registry';
import type { EarnRepositoryInterface } from '@/repositories/interfaces/earn.repository.interface';
import type { CompleteEarnTaskResponse, EarnOverviewResponse } from '@/lib/schemas';

const mockGetUser = vi.fn();

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: () => ({
    auth: {
      getUser: mockGetUser,
    },
  }),
}));

const { GET } = await import('../earn/overview/route');

class ThrowingEarnRepository implements EarnRepositoryInterface {
  async getOverview(): Promise<EarnOverviewResponse> {
    throw new Error('boom');
  }

  async completeTask(): Promise<CompleteEarnTaskResponse> {
    throw new Error('unused');
  }
}

describe('GET /api/earn/overview', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearLocalAuthSessions();
    resetRepositoryRegistry();
  });

  it('should return anonymous overview without bearer token', async () => {
    const request = new NextRequest('https://example.com/api/earn/overview');

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.is_logged_in).toBe(false);
    expect(body.coins).toBe(0);
    expect(body.daily_rewards).toHaveLength(7);
  });

  it('should return logged-in overview for valid bearer token', async () => {
    const session = createLocalAuthSession({
      userId: '550e8400-e29b-41d4-a716-446655440099',
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 2592000,
    });

    const request = new NextRequest('https://example.com/api/earn/overview', {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.is_logged_in).toBe(true);
    expect(body.coins).toBe(1200);
  });

  it('should downgrade invalid bearer token to anonymous overview', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: { user: null },
      error: { message: 'invalid token' },
    });

    const request = new NextRequest('https://example.com/api/earn/overview', {
      headers: {
        Authorization: 'Bearer invalid-token',
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.is_logged_in).toBe(false);
    expect(body.coins).toBe(0);
  });

  it('should return internal error when service throws unexpectedly', async () => {
    setEarnRepository(new ThrowingEarnRepository());
    const request = new NextRequest('https://example.com/api/earn/overview');

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
