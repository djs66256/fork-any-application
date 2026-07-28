import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { Errors } from '@/lib/errors';

vi.mock('@/services/auth/auth.service', () => ({
  AuthService: vi.fn().mockImplementation(() => ({
    createSession: vi.fn().mockResolvedValue({
      access_token: 'access-token',
      refresh_token: 'refresh-token',
      expires_at: '2026-07-28T12:34:56Z',
      user: {
        id: 'user_xxx',
        phone: '138****8000',
        display_name: null,
        avatar_url: null,
        role: 'viewer',
        is_new_user: true,
      },
    }),
  })),
}));

const { POST } = await import('../auth/sessions/route');
const { AuthService } = await import('@/services/auth/auth.service');

describe('POST /api/auth/sessions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return mapped auth session payload', async () => {
    const request = new NextRequest('https://localhost:3001/api/auth/sessions', {
      method: 'POST',
      body: JSON.stringify({
        countryCode: '+86',
        phone: '13800138000',
        code: '123456',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      code: 0,
      data: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        expiresAt: '2026-07-28T12:34:56Z',
        user: {
          id: 'user_xxx',
          phone: '138****8000',
          displayName: null,
          avatarUrl: null,
          role: 'viewer',
          isNewUser: true,
        },
      },
      message: 'ok',
    });
    expect(vi.mocked(AuthService).mock.results[0]?.value.createSession).toHaveBeenCalledWith({
      countryCode: '+86',
      phone: '13800138000',
      code: '123456',
    });
  });

  it('should reject invalid request body', async () => {
    const request = new NextRequest('https://localhost:3001/api/auth/sessions', {
      method: 'POST',
      body: JSON.stringify({
        countryCode: '+86',
        phone: '13800138000',
        code: '12345',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should propagate invalid code errors', async () => {
    vi.mocked(AuthService).mockImplementationOnce(() => ({
      createSession: vi.fn().mockRejectedValue(Errors.authInvalidCode('验证码错误，请重新输入')),
    }) as unknown as InstanceType<typeof AuthService>);

    const request = new NextRequest('https://localhost:3001/api/auth/sessions', {
      method: 'POST',
      body: JSON.stringify({
        countryCode: '+86',
        phone: '13800138000',
        code: '123456',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('AUTH_INVALID_CODE');
  });
});
