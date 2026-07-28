import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { Errors } from '@/lib/errors';

vi.mock('@/services/auth/auth.service', () => ({
  AuthService: vi.fn().mockImplementation(() => ({
    sendOtp: vi.fn().mockResolvedValue({
      requestId: 'otp_req_xxx',
      cooldownSeconds: 60,
      expiresInSeconds: 300,
    }),
  })),
}));

const { POST } = await import('../auth/otp-requests/route');
const { AuthService } = await import('@/services/auth/auth.service');

describe('POST /api/auth/otp-requests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return canonical success envelope', async () => {
    const request = new NextRequest('https://localhost:3001/api/auth/otp-requests', {
      method: 'POST',
      body: JSON.stringify({
        countryCode: '+86',
        phone: '13800138000',
        scene: 'login',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      code: 0,
      data: {
        requestId: 'otp_req_xxx',
        cooldownSeconds: 60,
        expiresInSeconds: 300,
      },
      message: 'ok',
    });
    expect(vi.mocked(AuthService).mock.results[0]?.value.sendOtp).toHaveBeenCalledWith({
      countryCode: '+86',
      phone: '13800138000',
      scene: 'login',
    });
  });

  it('should reject invalid request body', async () => {
    const request = new NextRequest('https://localhost:3001/api/auth/otp-requests', {
      method: 'POST',
      body: JSON.stringify({
        countryCode: '+86',
        phone: '123',
        scene: 'login',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should propagate service app errors', async () => {
    vi.mocked(AuthService).mockImplementationOnce(() => ({
      sendOtp: vi.fn().mockRejectedValue(Errors.authRateLimited('验证码请求过于频繁')),
    }) as unknown as InstanceType<typeof AuthService>);

    const request = new NextRequest('https://localhost:3001/api/auth/otp-requests', {
      method: 'POST',
      body: JSON.stringify({
        countryCode: '+86',
        phone: '13800138000',
        scene: 'login',
      }),
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(429);
    expect(body.error.code).toBe('AUTH_RATE_LIMITED');
  });
});
