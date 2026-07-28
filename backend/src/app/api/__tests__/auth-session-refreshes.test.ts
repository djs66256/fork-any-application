import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { Errors } from '@/lib/errors';

vi.mock('@/services/auth/auth.service', () => ({
  AuthService: vi.fn().mockImplementation(() => ({
    refreshSession: vi.fn().mockResolvedValue({
      access_token: 'new-access-token',
      refresh_token: 'new-refresh-token',
      expires_at: '2026-07-28T13:34:56Z',
      user: {
        id: 'user_xxx',
        phone: '138****8000',
        display_name: null,
        avatar_url: null,
        role: 'viewer',
        is_new_user: false,
      },
    }),
  })),
}));

const { POST } = await import('../auth/session-refreshes/route');
const { AuthService } = await import('@/services/auth/auth.service');

describe('POST /api/auth/session-refreshes', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return refreshed auth session payload', async () => {
    const request = new NextRequest('https://localhost:3001/api/auth/session-refreshes', {
      method: 'POST',
      body: JSON.stringify({
        refreshToken: 'refresh-token',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data.accessToken).toBe('new-access-token');
    expect(body.data.refreshToken).toBe('new-refresh-token');
    expect(body.data.user.isNewUser).toBe(false);
    expect(vi.mocked(AuthService).mock.results[0]?.value.refreshSession).toHaveBeenCalledWith({
      refreshToken: 'refresh-token',
    });
  });

  it('should reject blank refresh token', async () => {
    const request = new NextRequest('https://localhost:3001/api/auth/session-refreshes', {
      method: 'POST',
      body: JSON.stringify({
        refreshToken: '   ',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should propagate refresh expiration errors', async () => {
    vi.mocked(AuthService).mockImplementationOnce(() => ({
      refreshSession: vi.fn().mockRejectedValue(Errors.authRefreshExpired('登录态已失效，请重新登录')),
    }) as unknown as InstanceType<typeof AuthService>);

    const request = new NextRequest('https://localhost:3001/api/auth/session-refreshes', {
      method: 'POST',
      body: JSON.stringify({
        refreshToken: 'refresh-token',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(401);
    expect(body.error.code).toBe('AUTH_REFRESH_EXPIRED');
  });
});
