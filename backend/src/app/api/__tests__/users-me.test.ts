import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { Errors } from '@/lib/errors';

const mockGetUser = vi.fn();

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: () => ({
    auth: {
      getUser: mockGetUser,
    },
  }),
}));

vi.mock('@/services/auth/auth.service', () => ({
  AuthService: vi.fn().mockImplementation(() => ({
    getCurrentUser: vi.fn().mockResolvedValue({
      id: '550e8400-e29b-41d4-a716-446655440001',
      phone: '138****8000',
      display_name: null,
      avatar_url: null,
      role: 'viewer',
      is_new_user: false,
    }),
  })),
}));

const { GET } = await import('../users/me/route');
const { AuthService } = await import('@/services/auth/auth.service');

describe('GET /api/users/me', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return current user summary when bearer token is valid', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: '550e8400-e29b-41d4-a716-446655440001',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    const request = new NextRequest('https://localhost:3001/api/users/me', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      code: 0,
      data: {
        id: '550e8400-e29b-41d4-a716-446655440001',
        phone: '138****8000',
        displayName: null,
        avatarUrl: null,
        role: 'viewer',
        isNewUser: false,
      },
      message: 'ok',
    });
    expect(vi.mocked(AuthService).mock.results[0]?.value.getCurrentUser).toHaveBeenCalledWith(
      '550e8400-e29b-41d4-a716-446655440001',
    );
  });

  it('should reject requests without authorization', async () => {
    const request = new NextRequest('https://localhost:3001/api/users/me');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(401);
    expect(body.error.code).toBe('AUTH_UNAUTHORIZED');
  });

  it('should propagate not-found profile errors', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: '550e8400-e29b-41d4-a716-446655440001',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    vi.mocked(AuthService).mockImplementationOnce(() => ({
      getCurrentUser: vi.fn().mockRejectedValue(Errors.notFound('User', '550e8400-e29b-41d4-a716-446655440001')),
    }) as unknown as InstanceType<typeof AuthService>);

    const request = new NextRequest('https://localhost:3001/api/users/me', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error.code).toBe('NOT_FOUND');
  });
});
