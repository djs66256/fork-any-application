import { describe, it, expect, beforeEach, vi } from 'vitest';
import { NextRequest, NextResponse } from 'next/server';
import { clearLocalAuthSessions, createLocalAuthSession } from '@/services/auth/local-auth-session.store';

const mockGetUser = vi.fn();
const mockGetSupabaseAdmin = vi.fn(() => ({
  auth: {
    getUser: mockGetUser,
  },
}));

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: () => mockGetSupabaseAdmin(),
}));

const {
  verifyJwt,
  resolveOptionalAuthContext,
  resolveRequiredAuthContext,
  requireAuth,
  requireAuthContext,
  requireRole,
  getAuth,
} = await import('../auth');

describe('auth middleware helpers', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearLocalAuthSessions();
  });

  it('verifyJwt should return null when bearer token is missing', async () => {
    const request = new NextRequest('https://localhost:3001/api/test');

    await expect(verifyJwt(request)).resolves.toBeNull();
    expect(mockGetUser).not.toHaveBeenCalled();
  });

  it('verifyJwt should return auth context for valid token', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: '550e8400-e29b-41d4-a716-446655440001',
          app_metadata: { role: 'admin' },
        },
      },
      error: null,
    });

    const request = new NextRequest('https://localhost:3001/api/test', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });

    await expect(verifyJwt(request)).resolves.toEqual({
      userId: '550e8400-e29b-41d4-a716-446655440001',
      role: 'admin',
    });
  });

  it('verifyJwt should resolve local auth context for local access token', async () => {
    const session = createLocalAuthSession({
      userId: '550e8400-e29b-41d4-a716-446655440099',
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 2592000,
    });

    const request = new NextRequest('https://localhost:3001/api/test', {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
    });

    await expect(verifyJwt(request)).resolves.toEqual({
      userId: '550e8400-e29b-41d4-a716-446655440099',
      role: 'viewer',
    });
    expect(mockGetUser).not.toHaveBeenCalled();
  });

  it('verifyJwt should fallback unknown role to viewer', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: '550e8400-e29b-41d4-a716-446655440001',
          app_metadata: { role: 'super-admin' },
        },
      },
      error: null,
    });

    const request = new NextRequest('https://localhost:3001/api/test', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });

    await expect(verifyJwt(request)).resolves.toEqual({
      userId: '550e8400-e29b-41d4-a716-446655440001',
      role: 'viewer',
    });
  });

  it('verifyJwt should return null when supabase rejects token', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: { user: null },
      error: { message: 'invalid token' },
    });

    const request = new NextRequest('https://localhost:3001/api/test', {
      headers: {
        Authorization: 'Bearer invalid-token',
      },
    });

    await expect(verifyJwt(request)).resolves.toBeNull();
  });

  it('resolveOptionalAuthContext should return null for invalid token', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: { user: null },
      error: { message: 'invalid token' },
    });

    const request = new NextRequest('https://localhost:3001/api/test', {
      headers: {
        Authorization: 'Bearer invalid-token',
      },
    });

    await expect(resolveOptionalAuthContext(request)).resolves.toBeNull();
  });

  it('resolveRequiredAuthContext should throw auth unauthorized when token missing', async () => {
    const request = new NextRequest('https://localhost:3001/api/test');

    await expect(resolveRequiredAuthContext(request)).rejects.toMatchObject({
      code: 'AUTH_UNAUTHORIZED',
      statusCode: 401,
    });
  });

  it('requireAuth should reject unauthorized request', async () => {
    const handler = vi.fn(async () => NextResponse.json({ ok: true }));
    const wrapped = requireAuth(handler);
    const request = new NextRequest('https://localhost:3001/api/test');
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(401);
    expect(body.error.code).toBe('AUTH_UNAUTHORIZED');
    expect(body.error.message).toBe('请先登录');
    expect(handler).not.toHaveBeenCalled();
  });

  it('requireAuthContext should inject auth into request', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: '550e8400-e29b-41d4-a716-446655440001',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    const wrapped = requireAuthContext(async (request) =>
      NextResponse.json({ auth: getAuth(request) }),
    );
    const request = new NextRequest('https://localhost:3001/api/test', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.auth).toEqual({
      userId: '550e8400-e29b-41d4-a716-446655440001',
      role: 'viewer',
    });
  });

  it('requireRole should reject missing auth with formatted error response', async () => {
    const wrapped = requireRole(['admin'], async () => NextResponse.json({ ok: true }));
    const request = new NextRequest('https://localhost:3001/api/test');
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(401);
    expect(body.error.code).toBe('AUTH_UNAUTHORIZED');
  });

  it('requireRole should reject role mismatch', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: '550e8400-e29b-41d4-a716-446655440001',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    const wrapped = requireRole(['admin'], async () => NextResponse.json({ ok: true }));
    const request = new NextRequest('https://localhost:3001/api/test', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(403);
    expect(body.error.code).toBe('FORBIDDEN');
  });

  it('requireRole should allow matching role and expose auth context', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: '550e8400-e29b-41d4-a716-446655440001',
          app_metadata: { role: 'editor' },
        },
      },
      error: null,
    });

    const wrapped = requireRole(['admin', 'editor'], async (request) =>
      NextResponse.json({ auth: getAuth(request) }),
    );
    const request = new NextRequest('https://localhost:3001/api/test', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.auth).toEqual({
      userId: '550e8400-e29b-41d4-a716-446655440001',
      role: 'editor',
    });
  });
});
