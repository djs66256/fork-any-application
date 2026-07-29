import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { getCheckInRepository, resetRepositoryRegistry } from '@/repositories/repository-registry';
import { clearLocalAuthSessions, createLocalAuthSession, upsertLocalAuthUser } from '@/services/auth/local-auth-session.store';

const { POST } = await import('../check-ins/route');

const USER_ID = '00000000-0000-4000-8000-13800138000';
const INSTALLATION_ID = '770e8400-e29b-41d4-a716-446655440000';

describe('POST /api/check-ins', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-29T08:00:00.000Z'));
    resetRepositoryRegistry();
    clearLocalAuthSessions();
  });

  it('should create anonymous sign-in successfully', async () => {
    const request = new NextRequest('https://example.com/api/check-ins', {
      method: 'POST',
      headers: {
        'X-Installation-Id': INSTALLATION_ID,
      },
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.today_signed).toBe(true);
    expect(body.current_streak).toBe(1);
  });

  it('should remain idempotent for repeated same-day sign-ins', async () => {
    const request = new NextRequest('https://example.com/api/check-ins', {
      method: 'POST',
      headers: {
        'X-Installation-Id': INSTALLATION_ID,
      },
    });

    const first = await POST(request, undefined);
    const second = await POST(request, undefined);
    const body = await second.json();

    expect(first.status).toBe(200);
    expect(second.status).toBe(200);
    expect(body.today_signed).toBe(true);

    const records = await getCheckInRepository().listRecentBySubject({
      type: 'installation',
      id: INSTALLATION_ID,
    });
    expect(records).toHaveLength(1);
  });

  it('should prioritize logged-in user over installation id on sign-in', async () => {
    upsertLocalAuthUser({ id: USER_ID, phone: '13800138000', role: 'viewer' });
    const session = createLocalAuthSession({
      userId: USER_ID,
      phone: '13800138000',
      role: 'viewer',
      accessTokenTtlSeconds: 3600,
      refreshTokenTtlSeconds: 3600,
    });

    const request = new NextRequest('https://example.com/api/check-ins', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
        'X-Installation-Id': INSTALLATION_ID,
      },
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.today_signed).toBe(true);

    const userRecords = await getCheckInRepository().listRecentBySubject({ type: 'user', id: USER_ID });
    const installationRecords = await getCheckInRepository().listRecentBySubject({
      type: 'installation',
      id: INSTALLATION_ID,
    });
    expect(userRecords).toHaveLength(1);
    expect(installationRecords).toHaveLength(0);
  });

  it('should return validation error for invalid installation header', async () => {
    const request = new NextRequest('https://example.com/api/check-ins', {
      method: 'POST',
      headers: {
        'X-Installation-Id': 'bad-id',
      },
    });

    const response = await POST(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });
});
