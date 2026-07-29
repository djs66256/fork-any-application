import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { resetRepositoryRegistry } from '@/repositories/repository-registry';
import { clearLocalAuthSessions, createLocalAuthSession, upsertLocalAuthUser } from '@/services/auth/local-auth-session.store';

const { GET } = await import('../check-ins/status/route');

const USER_ID = '00000000-0000-4000-8000-13800138000';
const INSTALLATION_ID = '770e8400-e29b-41d4-a716-446655440000';

describe('GET /api/check-ins/status', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-29T08:00:00.000Z'));
    resetRepositoryRegistry();
    clearLocalAuthSessions();
  });

  it('should return anonymous sign-in status with installation id', async () => {
    const request = new NextRequest('https://example.com/api/check-ins/status', {
      headers: {
        'X-Installation-Id': INSTALLATION_ID,
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.server_date).toBe('2026-07-29');
    expect(body.should_show_popup).toBe(true);
    expect(body.days).toHaveLength(7);
  });

  it('should prioritize logged-in user over installation id', async () => {
    upsertLocalAuthUser({ id: USER_ID, phone: '13800138000', role: 'viewer' });
    const session = createLocalAuthSession({
      userId: USER_ID,
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 3600,
    });

    const request = new NextRequest('https://example.com/api/check-ins/status', {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
        'X-Installation-Id': INSTALLATION_ID,
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.server_date).toBe('2026-07-29');
  });

  it('should return validation error when anonymous installation id is missing', async () => {
    const request = new NextRequest('https://example.com/api/check-ins/status');

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return validation error for invalid installation header', async () => {
    const request = new NextRequest('https://example.com/api/check-ins/status', {
      headers: {
        'X-Installation-Id': 'bad-id',
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });
});
