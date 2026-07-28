import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';

vi.mock('@/services/auth/auth.service', () => ({
  AuthService: vi.fn().mockImplementation(() => ({
    logout: vi.fn().mockResolvedValue(undefined),
  })),
}));

const { DELETE } = await import('../auth/session/route');
const { AuthService } = await import('@/services/auth/auth.service');

describe('DELETE /api/auth/session', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should logout with bearer access token', async () => {
    const request = new NextRequest('https://localhost:3001/api/auth/session', {
      method: 'DELETE',
      headers: {
        Authorization: 'Bearer access-token',
      },
    });

    const response = await DELETE(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({ code: 0, data: null, message: 'ok' });
    expect(vi.mocked(AuthService).mock.results[0]?.value.logout).toHaveBeenCalledWith('access-token');
  });

  it('should remain idempotent when authorization is missing', async () => {
    const request = new NextRequest('https://localhost:3001/api/auth/session', {
      method: 'DELETE',
    });

    const response = await DELETE(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({ code: 0, data: null, message: 'ok' });
    expect(vi.mocked(AuthService).mock.results[0]?.value.logout).toHaveBeenCalledWith(undefined);
  });
});
