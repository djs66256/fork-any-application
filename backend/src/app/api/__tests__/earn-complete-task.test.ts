import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { clearLocalAuthSessions, createLocalAuthSession } from '@/services/auth/local-auth-session.store';
import { resetRepositoryRegistry } from '@/repositories/repository-registry';

const mockGetUser = vi.fn();

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: () => ({
    auth: {
      getUser: mockGetUser,
    },
  }),
}));

const { POST } = await import('../earn/complete-task/route');

describe('POST /api/earn/complete-task', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearLocalAuthSessions();
    resetRepositoryRegistry();
  });

  it('should reject missing bearer token with auth unauthorized', async () => {
    const request = new NextRequest('https://example.com/api/earn/complete-task', {
      method: 'POST',
      body: JSON.stringify({
        task_id: '22222222-2222-4222-8222-222222222222',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(401);
    expect(body.error.code).toBe('AUTH_UNAUTHORIZED');
  });

  it('should reject invalid task_id with validation error', async () => {
    const session = createLocalAuthSession({
      userId: '550e8400-e29b-41d4-a716-446655440001',
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 2592000,
    });

    const request = new NextRequest('https://example.com/api/earn/complete-task', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
      body: JSON.stringify({
        task_id: 'invalid-task-id',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return not found for unknown task id', async () => {
    const session = createLocalAuthSession({
      userId: '550e8400-e29b-41d4-a716-446655440002',
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 2592000,
    });

    const request = new NextRequest('https://example.com/api/earn/complete-task', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
      body: JSON.stringify({
        task_id: '44444444-4444-4444-8444-444444444444',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error.code).toBe('NOT_FOUND');
  });

  it('should complete representative task with bearer token only', async () => {
    const session = createLocalAuthSession({
      userId: '550e8400-e29b-41d4-a716-446655440003',
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 2592000,
    });

    const request = new NextRequest('https://example.com/api/earn/complete-task', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
      body: JSON.stringify({
        task_id: '22222222-2222-4222-8222-222222222222',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      success: true,
      task_id: '22222222-2222-4222-8222-222222222222',
      coins_earned: 500,
      total_coins: 1700,
      task_status: 'completed',
    });
  });

  it('should return idempotent success for repeated completion', async () => {
    const session = createLocalAuthSession({
      userId: '550e8400-e29b-41d4-a716-446655440004',
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 2592000,
    });

    const firstRequest = new NextRequest('https://example.com/api/earn/complete-task', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
      body: JSON.stringify({
        task_id: '22222222-2222-4222-8222-222222222222',
      }),
    });

    const secondRequest = new NextRequest('https://example.com/api/earn/complete-task', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
      body: JSON.stringify({
        task_id: '22222222-2222-4222-8222-222222222222',
      }),
    });

    const firstResponse = await POST(firstRequest, undefined);
    const secondResponse = await POST(secondRequest, undefined);
    const firstBody = await firstResponse.json();
    const secondBody = await secondResponse.json();

    expect(firstResponse.status).toBe(200);
    expect(secondResponse.status).toBe(200);
    expect(firstBody.coins_earned).toBe(500);
    expect(secondBody).toEqual({
      success: true,
      task_id: '22222222-2222-4222-8222-222222222222',
      coins_earned: 0,
      total_coins: 1700,
      task_status: 'completed',
    });
  });

  it('should reject invalid bearer token with auth unauthorized', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: { user: null },
      error: { message: 'invalid token' },
    });

    const request = new NextRequest('https://example.com/api/earn/complete-task', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer invalid-token',
      },
      body: JSON.stringify({
        task_id: '22222222-2222-4222-8222-222222222222',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(401);
    expect(body.error.code).toBe('AUTH_UNAUTHORIZED');
  });
});
