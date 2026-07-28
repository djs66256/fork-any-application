import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { clearLocalAuthSessions } from './local-auth-session.store';
import type { AuthProfileRepositoryInterface } from '@/repositories/interfaces/auth-profile.repository.interface';

const mockSignInWithOtp = vi.fn();
const mockVerifyOtp = vi.fn();
const mockRefreshSession = vi.fn();
const mockAdminSignOut = vi.fn();

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: () => ({
    auth: {
      signInWithOtp: mockSignInWithOtp,
      verifyOtp: mockVerifyOtp,
      refreshSession: mockRefreshSession,
      admin: {
        signOut: mockAdminSignOut,
      },
    },
  }),
}));

class MockAuthProfileRepository implements AuthProfileRepositoryInterface {
  public ensureAuthUserProfileCallCount = 0;

  constructor(private users = new Map<string, { id: string; phone: string; display_name?: string | null; avatar_url?: string | null; is_new_user: boolean; role?: string }>()) {}

  async findAuthUserById(userId: string) {
    return this.users.get(userId) ?? null;
  }

  async ensureAuthUserProfile(userId: string) {
    this.ensureAuthUserProfileCallCount += 1;
    if (!this.users.has(userId)) {
      this.users.set(userId, {
        id: userId,
        phone: '138****8000',
        display_name: null,
        avatar_url: null,
        is_new_user: false,
        role: 'viewer',
      });
    }
  }
}

describe('AuthService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearLocalAuthSessions();
    vi.unstubAllEnvs();
    vi.useRealTimers();
  });

  it('sendOtp should return canonical cooldown payload', async () => {
    mockSignInWithOtp.mockResolvedValueOnce({
      data: { user: { id: 'otp-user-id' } },
      error: null,
    });

    const service = new AuthService(new MockAuthProfileRepository());
    const result = await service.sendOtp({
      countryCode: '+86',
      phone: '13800138000',
      scene: 'login',
    });

    expect(mockSignInWithOtp).toHaveBeenCalledWith({
      phone: '+8613800138000',
      options: { shouldCreateUser: true },
    });
    expect(result).toEqual({
      requestId: 'otp-user-id',
      cooldownSeconds: 60,
      expiresInSeconds: 300,
    });
  });

  it('sendOtp should map rate limit errors', async () => {
    mockSignInWithOtp.mockResolvedValueOnce({
      data: { user: null },
      error: { status: 429, message: 'rate limit exceeded' },
    });

    const service = new AuthService(new MockAuthProfileRepository());

    await expect(service.sendOtp({
      countryCode: '+86',
      phone: '13800138000',
      scene: 'login',
    })).rejects.toMatchObject({ code: 'AUTH_RATE_LIMITED' });
  });

  it('sendOtp should map disabled phone provider to service unavailable', async () => {
    mockSignInWithOtp.mockResolvedValueOnce({
      data: { user: null },
      error: { status: 400, code: 'phone_provider_disabled', message: 'Unsupported phone provider' },
    });

    const service = new AuthService(new MockAuthProfileRepository());

    await expect(service.sendOtp({
      countryCode: '+86',
      phone: '13800138000',
      scene: 'login',
    })).rejects.toMatchObject({ code: 'SERVICE_UNAVAILABLE' });
  });

  it('sendOtp should bypass provider for configured local test phone', async () => {
    vi.stubEnv('AUTH_ALLOW_TEST_OTP_BYPASS', 'true');

    const service = new AuthService(new MockAuthProfileRepository());
    const result = await service.sendOtp({
      countryCode: '+86',
      phone: '13800138000',
      scene: 'login',
    });

    expect(result).toEqual({
      requestId: 'test_otp_13800138000',
      cooldownSeconds: 60,
      expiresInSeconds: 300,
    });
    expect(mockSignInWithOtp).not.toHaveBeenCalled();
  });

  it('createSession should map new user session', async () => {
    mockVerifyOtp.mockResolvedValueOnce({
      data: {
        session: {
          access_token: 'access-token',
          refresh_token: 'refresh-token',
          expires_at: 1785242096,
        },
        user: {
          id: 'user-1',
          phone: '+8613800138000',
          created_at: '2026-07-28T00:00:00Z',
          last_sign_in_at: '2026-07-28T00:00:00Z',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    const repo = new MockAuthProfileRepository(new Map([
      ['user-1', {
        id: 'user-1',
        phone: '138****8000',
        display_name: null,
        avatar_url: null,
        is_new_user: false,
        role: 'viewer',
      }],
    ]));

    const service = new AuthService(repo);
    const result = await service.createSession({
      countryCode: '+86',
      phone: '13800138000',
      code: '123456',
    });

    expect(result.access_token).toBe('access-token');
    expect(result.refresh_token).toBe('refresh-token');
    expect(result.user.phone).toBe('138****8000');
    expect(result.user.is_new_user).toBe(true);
  });

  it('createSession should map invalid code error', async () => {
    mockVerifyOtp.mockResolvedValueOnce({
      data: { session: null, user: null },
      error: { status: 400, message: 'Token has invalid format' },
    });

    const service = new AuthService(new MockAuthProfileRepository());
    await expect(service.createSession({
      countryCode: '+86',
      phone: '13800138000',
      code: '123456',
    })).rejects.toMatchObject({ code: 'AUTH_INVALID_CODE' });
  });

  it('createSession should map disabled phone provider to service unavailable', async () => {
    mockVerifyOtp.mockResolvedValueOnce({
      data: { session: null, user: null },
      error: { status: 400, code: 'phone_provider_disabled', message: 'Unsupported phone provider' },
    });

    const service = new AuthService(new MockAuthProfileRepository());
    await expect(service.createSession({
      countryCode: '+86',
      phone: '13800138000',
      code: '123456',
    })).rejects.toMatchObject({ code: 'SERVICE_UNAVAILABLE' });
  });

  it('createSession should map expired code error', async () => {
    mockVerifyOtp.mockResolvedValueOnce({
      data: { session: null, user: null },
      error: { status: 400, message: 'OTP expired' },
    });

    const service = new AuthService(new MockAuthProfileRepository());
    await expect(service.createSession({
      countryCode: '+86',
      phone: '13800138000',
      code: '123456',
    })).rejects.toMatchObject({ code: 'AUTH_CODE_EXPIRED' });
  });

  it('createSession should create local session for configured test phone', async () => {
    vi.stubEnv('AUTH_ALLOW_TEST_OTP_BYPASS', 'true');

    const service = new AuthService(new MockAuthProfileRepository());
    const result = await service.createSession({
      countryCode: '+86',
      phone: '13800138000',
      code: '123456',
    });

    expect(result.access_token.startsWith('local_at_')).toBe(true);
    expect(result.refresh_token.startsWith('local_rt_')).toBe(true);
    expect(result.user.phone).toBe('138****8000');
    expect(result.user.role).toBe('viewer');
    expect(result.user.is_new_user).toBe(true);
    expect(mockVerifyOtp).not.toHaveBeenCalled();
  });

  it('createSession should reject invalid local bypass code', async () => {
    vi.stubEnv('AUTH_ALLOW_TEST_OTP_BYPASS', 'true');

    const service = new AuthService(new MockAuthProfileRepository());

    await expect(service.createSession({
      countryCode: '+86',
      phone: '13800138000',
      code: '000000',
    })).rejects.toMatchObject({ code: 'AUTH_INVALID_CODE' });
    expect(mockVerifyOtp).not.toHaveBeenCalled();
  });

  it('refreshSession should return refreshed session', async () => {
    mockRefreshSession.mockResolvedValueOnce({
      data: {
        session: {
          access_token: 'new-access-token',
          refresh_token: 'new-refresh-token',
          expires_at: 1785242096,
        },
        user: {
          id: 'user-1',
          phone: '+8613800138000',
          created_at: '2026-07-27T00:00:00Z',
          last_sign_in_at: '2026-07-28T00:00:00Z',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    const repo = new MockAuthProfileRepository(new Map([
      ['user-1', {
        id: 'user-1',
        phone: '138****8000',
        display_name: null,
        avatar_url: null,
        is_new_user: false,
        role: 'viewer',
      }],
    ]));

    const service = new AuthService(repo);
    const result = await service.refreshSession({ refreshToken: 'refresh-token' });

    expect(mockRefreshSession).toHaveBeenCalledWith({ refresh_token: 'refresh-token' });
    expect(result.access_token).toBe('new-access-token');
    expect(result.user.is_new_user).toBe(false);
  });

  it('refreshSession should ensure profile before building session when missing', async () => {
    mockRefreshSession.mockResolvedValueOnce({
      data: {
        session: {
          access_token: 'new-access-token',
          refresh_token: 'new-refresh-token',
          expires_at: 1785242096,
        },
        user: {
          id: 'user-missing-profile',
          phone: '+8613800138000',
          created_at: '2026-07-27T00:00:00Z',
          last_sign_in_at: '2026-07-28T00:00:00Z',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    const repo = new MockAuthProfileRepository();
    const service = new AuthService(repo);
    const result = await service.refreshSession({ refreshToken: 'refresh-token' });

    expect(repo.ensureAuthUserProfileCallCount).toBe(1);
    expect(result.user.id).toBe('user-missing-profile');
    expect(result.user.phone).toBe('138****8000');
  });

  it('refreshSession should map expired refresh token', async () => {
    mockRefreshSession.mockResolvedValueOnce({
      data: { session: null, user: null },
      error: { status: 401, message: 'refresh token expired' },
    });

    const service = new AuthService(new MockAuthProfileRepository());
    await expect(service.refreshSession({ refreshToken: 'refresh-token' })).rejects.toMatchObject({
      code: 'AUTH_REFRESH_EXPIRED',
    });
  });

  it('refreshSession should rotate local refresh token', async () => {
    vi.stubEnv('AUTH_ALLOW_TEST_OTP_BYPASS', 'true');

    const service = new AuthService(new MockAuthProfileRepository());
    const session = await service.createSession({
      countryCode: '+86',
      phone: '13800138000',
      code: '123456',
    });
    const refreshed = await service.refreshSession({ refreshToken: session.refresh_token });

    expect(refreshed.access_token.startsWith('local_at_')).toBe(true);
    expect(refreshed.refresh_token.startsWith('local_rt_')).toBe(true);
    expect(refreshed.access_token).not.toBe(session.access_token);
    expect(refreshed.refresh_token).not.toBe(session.refresh_token);
    expect(refreshed.user.id).toBe(session.user.id);
    expect(refreshed.user.is_new_user).toBe(false);
    expect(mockRefreshSession).not.toHaveBeenCalled();
  });

  it('refreshSession should reject expired local refresh token', async () => {
    vi.stubEnv('AUTH_ALLOW_TEST_OTP_BYPASS', 'true');
    vi.stubEnv('AUTH_ACCESS_TOKEN_TTL_SECONDS', '1');
    vi.stubEnv('AUTH_REFRESH_TOKEN_TTL_SECONDS', '1');
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-28T00:00:00Z'));

    const service = new AuthService(new MockAuthProfileRepository());
    const session = await service.createSession({
      countryCode: '+86',
      phone: '13800138000',
      code: '123456',
    });

    vi.setSystemTime(new Date('2026-07-28T00:00:03Z'));

    await expect(service.refreshSession({ refreshToken: session.refresh_token })).rejects.toMatchObject({
      code: 'AUTH_REFRESH_EXPIRED',
    });

    vi.useRealTimers();
  });

  it('getCurrentUser should return profile summary', async () => {
    const repo = new MockAuthProfileRepository(new Map([
      ['user-1', {
        id: 'user-1',
        phone: '138****8000',
        display_name: '测试用户',
        avatar_url: null,
        is_new_user: false,
        role: 'viewer',
      }],
    ]));

    const service = new AuthService(repo);
    await expect(service.getCurrentUser('user-1')).resolves.toMatchObject({
      id: 'user-1',
      phone: '138****8000',
    });
  });

  it('getCurrentUser should return local user summary for bypass session', async () => {
    vi.stubEnv('AUTH_ALLOW_TEST_OTP_BYPASS', 'true');

    const service = new AuthService(new MockAuthProfileRepository());
    const session = await service.createSession({
      countryCode: '+86',
      phone: '13800138000',
      code: '123456',
    });

    await expect(service.getCurrentUser(session.user.id)).resolves.toMatchObject({
      id: session.user.id,
      phone: '138****8000',
      role: 'viewer',
      is_new_user: false,
    });
  });

  it('getCurrentUser should throw not found when profile missing', async () => {
    const service = new AuthService(new MockAuthProfileRepository());
    await expect(service.getCurrentUser('missing-user')).rejects.toMatchObject({ code: 'NOT_FOUND' });
  });

  it('logout should ignore missing token', async () => {
    const service = new AuthService(new MockAuthProfileRepository());
    await expect(service.logout()).resolves.toBeUndefined();
    expect(mockAdminSignOut).not.toHaveBeenCalled();
  });

  it('logout should swallow invalid session errors', async () => {
    mockAdminSignOut.mockResolvedValueOnce({
      error: { status: 401, message: 'session not found' },
    });

    const service = new AuthService(new MockAuthProfileRepository());
    await expect(service.logout('access-token')).resolves.toBeUndefined();
  });

  it('logout should remain idempotent for invalid jwt token', async () => {
    mockAdminSignOut.mockResolvedValueOnce({
      error: { status: 400, message: 'invalid JWT: token contains an invalid number of segments' },
    });

    const service = new AuthService(new MockAuthProfileRepository());
    await expect(service.logout('fake-access-token')).resolves.toBeUndefined();
  });

  it('logout should throw service unavailable for unexpected upstream failure', async () => {
    mockAdminSignOut.mockResolvedValueOnce({
      error: { status: 503, message: 'upstream unavailable' },
    });

    const service = new AuthService(new MockAuthProfileRepository());
    await expect(service.logout('access-token')).rejects.toMatchObject({ code: 'SERVICE_UNAVAILABLE' });
  });
});
