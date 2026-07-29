import { beforeEach, describe, expect, it } from 'vitest';
import { NextRequest } from 'next/server';
import { clearLocalAuthSessions, createLocalAuthSession, upsertLocalAuthUser } from '@/services/auth/local-auth-session.store';

const { GET } = await import('../messages/interactions/route');

const USER_ID = '00000000-0000-4000-8000-13800138000';

describe('GET /api/messages/interactions', () => {
  beforeEach(() => {
    clearLocalAuthSessions();
  });

  it('should reject anonymous access', async () => {
    const request = new NextRequest('https://example.com/api/messages/interactions?page=1&pageSize=20');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(401);
    expect(body.error.code).toBe('AUTH_UNAUTHORIZED');
  });

  it('should return interaction messages for authenticated users', async () => {
    upsertLocalAuthUser({ id: USER_ID, phone: '13800138000', role: 'viewer' });
    const session = createLocalAuthSession({
      userId: USER_ID,
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 3600,
    });

    const request = new NextRequest('https://example.com/api/messages/interactions?page=1&pageSize=2', {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
    });
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toHaveLength(2);
    expect(body.pagination).toEqual({
      page: 1,
      page_size: 2,
      total: 3,
      total_pages: 2,
    });
  });

  it('should return validation error for invalid pagination', async () => {
    upsertLocalAuthUser({ id: USER_ID, phone: '13800138000', role: 'viewer' });
    const session = createLocalAuthSession({
      userId: USER_ID,
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 3600,
    });

    const request = new NextRequest('https://example.com/api/messages/interactions?page=0&pageSize=21', {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
      },
    });
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });
});
