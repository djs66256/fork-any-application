import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { config } from '@/lib/config';
import { Errors, AppError } from '@/lib/errors';
import type {
  AuthSession,
  AuthUser,
  CreateAuthSessionRequest,
  RefreshAuthSessionRequest,
  SendOtpRequest,
} from '@/lib/schemas';
import { AuthProfileSupabaseRepository } from '@/repositories/supabase/auth-profile.supabase.repository';
import type { AuthProfileRepositoryInterface } from '@/repositories/interfaces/auth-profile.repository.interface';
import {
  createLocalAuthSession,
  getLocalAuthUser,
  isLocalAccessToken,
  isLocalRefreshToken,
  refreshLocalAuthSession,
  revokeLocalAuthSessionByAccessToken,
  upsertLocalAuthUser,
} from './local-auth-session.store';

interface SendOtpResult {
  requestId: string;
  cooldownSeconds: number;
  expiresInSeconds: number;
}

type VerifyOtpParams = {
  phone: string;
  token: string;
  type: 'sms';
};

type SupabaseSessionUser = {
  id: string;
  phone?: string | null;
  created_at?: string;
  last_sign_in_at?: string;
  app_metadata?: {
    role?: string;
    is_new_user?: boolean;
  };
};

type SupabaseSession = {
  access_token: string;
  refresh_token: string;
  expires_at?: number | null;
  user: SupabaseSessionUser;
};

type SessionUserMeta = {
  session: SupabaseSession;
  isNewUser: boolean;
};

function isTestOtpBypassEnabled(): boolean {
  return config.auth.allowTestOtpBypass;
}

function isTestPhoneRequest(countryCode: string, phone: string): boolean {
  return `${countryCode}${phone}` === `+86${config.auth.testPhone}`;
}

function isTestOtpCode(code: string): boolean {
  return code === config.auth.testOtpCode;
}

function normalizePhone(countryCode: string, phone: string): string {
  return `${countryCode}${phone}`;
}

function maskPhone(phone: string | null | undefined): string {
  const normalized = phone?.trim() ?? '';
  const digits = normalized.replace(/^\+86/, '');
  if (!/^1\d{10}$/.test(digits)) {
    return normalized;
  }

  return `${digits.slice(0, 3)}****${digits.slice(-4)}`;
}

function toIsoString(expiresAt?: number | null): string {
  if (!expiresAt) {
    throw Errors.internal('Missing session expiry');
  }

  return new Date(expiresAt * 1000).toISOString();
}

function normalizeRole(role: unknown): 'admin' | 'editor' | 'viewer' {
  return role === 'admin' || role === 'editor' || role === 'viewer' ? role : 'viewer';
}

function normalizeAppMetadata(appMetadata: unknown): SupabaseSessionUser['app_metadata'] {
  if (!appMetadata || typeof appMetadata !== 'object') {
    return undefined;
  }

  return {
    role: (appMetadata as { role?: unknown }).role as string | undefined,
    is_new_user: (appMetadata as { is_new_user?: unknown }).is_new_user as boolean | undefined,
  };
}

function isNewAuthUser(user: SupabaseSessionUser): boolean {
  if (typeof user.app_metadata?.is_new_user === 'boolean') {
    return user.app_metadata.is_new_user;
  }

  if (!user.created_at || !user.last_sign_in_at) {
    return false;
  }

  return user.created_at === user.last_sign_in_at;
}

function isAppError(error: unknown): error is AppError {
  return error instanceof AppError;
}

function isAuthApiError(error: unknown): error is { message?: string; status?: number; code?: string; name?: string } {
  return typeof error === 'object' && error !== null;
}

function isPhoneProviderDisabled(message: string, code?: string): boolean {
  return code === 'phone_provider_disabled'
    || message.includes('unsupported phone provider')
    || message.includes('phone provider disabled')
    || message.includes('phone_provider_disabled');
}

function isInvalidPhoneMessage(message: string): boolean {
  return message.includes('invalid phone')
    || message.includes('phone number is invalid')
    || message.includes('phone format')
    || message.includes('sms phone number is invalid');
}

function mapOtpError(error: unknown): never {
  if (isAuthApiError(error)) {
    const message = error.message ?? 'Auth service unavailable';
    const lowered = message.toLowerCase();

    if (error.status === 429 || lowered.includes('rate limit')) {
      throw Errors.authRateLimited('验证码请求过于频繁');
    }

    if (error.status === 409 || lowered.includes('cooldown')) {
      throw Errors.authCodeCooldown('请稍后再获取验证码');
    }

    if (isPhoneProviderDisabled(lowered, error.code)) {
      throw Errors.serviceUnavailable('Supabase Auth');
    }

    if (error.status === 400 && isInvalidPhoneMessage(lowered)) {
      throw Errors.authInvalidPhone('手机号格式不正确');
    }
  }

  throw Errors.serviceUnavailable('Supabase Auth');
}

function mapVerifyOtpError(error: unknown): never {
  if (isAuthApiError(error)) {
    const message = error.message ?? 'Auth service unavailable';
    const lowered = message.toLowerCase();

    if (isPhoneProviderDisabled(lowered, error.code)) {
      throw Errors.serviceUnavailable('Supabase Auth');
    }

    if (error.status === 400) {
      if (lowered.includes('expired')) {
        throw Errors.authCodeExpired('验证码已过期');
      }
      if (lowered.includes('token') || lowered.includes('otp') || lowered.includes('code')) {
        throw Errors.authInvalidCode('验证码错误，请重新输入');
      }
    }

    if (error.status === 429 || lowered.includes('rate limit')) {
      throw Errors.authRateLimited('验证过于频繁，请稍后重试');
    }
  }

  throw Errors.serviceUnavailable('Supabase Auth');
}

function mapRefreshError(error: unknown): never {
  if (isAuthApiError(error)) {
    const message = error.message ?? 'Refresh token expired or invalid';
    const lowered = message.toLowerCase();

    if (error.status === 400 || error.status === 401 || lowered.includes('refresh') || lowered.includes('jwt')) {
      throw Errors.authRefreshExpired('登录态已失效，请重新登录');
    }
  }

  throw Errors.serviceUnavailable('Supabase Auth');
}

export class AuthService {
  constructor(
    private authProfileRepository: AuthProfileRepositoryInterface = new AuthProfileSupabaseRepository(),
  ) {}

  async sendOtp(input: SendOtpRequest): Promise<SendOtpResult> {
    if (isTestOtpBypassEnabled() && isTestPhoneRequest(input.countryCode, input.phone)) {
      return {
        requestId: `test_otp_${input.phone}`,
        cooldownSeconds: 60,
        expiresInSeconds: 300,
      };
    }

    const supabase = getSupabaseAdmin();

    try {
      const { data, error } = await supabase.auth.signInWithOtp({
        phone: normalizePhone(input.countryCode, input.phone),
        options: {
          shouldCreateUser: true,
        },
      });

      if (error) {
        mapOtpError(error);
      }

      const requestId = (
        data as { user?: { id?: string | null } | null } | null
      )?.user?.id ?? `otp_${input.phone}`;

      return {
        requestId,
        cooldownSeconds: 60,
        expiresInSeconds: 300,
      };
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      mapOtpError(error);
    }
  }

  async createSession(input: CreateAuthSessionRequest): Promise<AuthSession> {
    if (isTestOtpBypassEnabled() && isTestPhoneRequest(input.countryCode, input.phone)) {
      if (!isTestOtpCode(input.code)) {
        throw Errors.authInvalidCode('验证码错误，请重新输入');
      }

      return this.createLocalTestSession(input.phone);
    }

    const supabase = getSupabaseAdmin();
    const e164Phone = normalizePhone(input.countryCode, input.phone);

    try {
      const { data, error } = await supabase.auth.verifyOtp({
        phone: e164Phone,
        token: input.code,
        type: 'sms',
      } as VerifyOtpParams);

      if (error || !data.session || !data.user) {
        mapVerifyOtpError(error ?? new Error('Missing session or user after verifyOtp'));
      }

      return this.buildSession({
        session: {
          access_token: data.session.access_token,
          refresh_token: data.session.refresh_token,
          expires_at: data.session.expires_at,
          user: {
            id: data.user.id,
            phone: data.user.phone,
            created_at: data.user.created_at,
            last_sign_in_at: data.user.last_sign_in_at,
            app_metadata: normalizeAppMetadata(data.user.app_metadata),
          },
        },
        isNewUser: isNewAuthUser({
          id: data.user.id,
          phone: data.user.phone,
          created_at: data.user.created_at,
          last_sign_in_at: data.user.last_sign_in_at,
          app_metadata: normalizeAppMetadata(data.user.app_metadata),
        }),
      });
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      mapVerifyOtpError(error);
    }
  }

  async refreshSession(input: RefreshAuthSessionRequest): Promise<AuthSession> {
    if (isLocalRefreshToken(input.refreshToken)) {
      const localSession = refreshLocalAuthSession({
        refreshToken: input.refreshToken,
        accessTokenTtlSeconds: config.auth.accessTokenTtlSeconds,
        refreshTokenTtlSeconds: config.auth.refreshTokenTtlSeconds,
      });

      if (!localSession) {
        throw Errors.authRefreshExpired('登录态已失效，请重新登录');
      }

      return this.buildLocalSession({
        accessToken: localSession.accessToken,
        refreshToken: localSession.refreshToken,
        expiresAt: localSession.expiresAt,
        userId: localSession.userId,
        phone: localSession.phone,
        role: localSession.role,
        isNewUser: false,
      });
    }

    const supabase = getSupabaseAdmin();

    try {
      const { data, error } = await supabase.auth.refreshSession({
        refresh_token: input.refreshToken,
      });

      if (error || !data.session || !data.user) {
        mapRefreshError(error ?? new Error('Missing session or user after refreshSession'));
      }

      return this.buildSession({
        session: {
          access_token: data.session.access_token,
          refresh_token: data.session.refresh_token,
          expires_at: data.session.expires_at,
          user: {
            id: data.user.id,
            phone: data.user.phone,
            created_at: data.user.created_at,
            last_sign_in_at: data.user.last_sign_in_at,
            app_metadata: normalizeAppMetadata(data.user.app_metadata),
          },
        },
        isNewUser: false,
      });
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      mapRefreshError(error);
    }
  }

  async getCurrentUser(authUserId: string): Promise<AuthUser> {
    const localUser = getLocalAuthUser(authUserId);
    if (localUser) {
      return {
        id: localUser.id,
        phone: maskPhone(`+86${localUser.phone}`) || localUser.phone,
        display_name: null,
        avatar_url: null,
        role: localUser.role,
        is_new_user: false,
      };
    }

    const user = await this.authProfileRepository.findAuthUserById(authUserId);
    if (!user) {
      throw Errors.notFound('User', authUserId);
    }
    return user;
  }

  async logout(accessToken?: string | null): Promise<void> {
    if (!accessToken) {
      return;
    }

    if (isLocalAccessToken(accessToken)) {
      revokeLocalAuthSessionByAccessToken(accessToken);
      return;
    }

    const supabase = getSupabaseAdmin();

    try {
      const { error } = await supabase.auth.admin.signOut(accessToken);
      if (error) {
        const lowered = error.message.toLowerCase();
        if (
          error.status === 400
          || error.status === 401
          || error.status === 403
          || error.status === 404
          || lowered.includes('session')
          || lowered.includes('not found')
          || lowered.includes('invalid jwt')
          || lowered.includes('invalid token')
          || lowered.includes('jwt')
        ) {
          return;
        }
        throw Errors.serviceUnavailable('Supabase Auth');
      }
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      return;
    }
  }

  private async createLocalTestSession(phone: string): Promise<AuthSession> {
    const userId = `00000000-0000-4000-8000-${phone.slice(-12).padStart(12, '0')}`;
    const existingLocalUser = getLocalAuthUser(userId);
    const isNewUser = !existingLocalUser;

    upsertLocalAuthUser({
      id: userId,
      phone,
      role: 'viewer',
    });

    const localSession = createLocalAuthSession({
      userId,
      phone,
      role: 'viewer',
      accessTokenTtlSeconds: config.auth.accessTokenTtlSeconds,
      refreshTokenTtlSeconds: config.auth.refreshTokenTtlSeconds,
    });

    return this.buildLocalSession({
      accessToken: localSession.accessToken,
      refreshToken: localSession.refreshToken,
      expiresAt: localSession.expiresAt,
      userId,
      phone,
      role: 'viewer',
      isNewUser,
    });
  }

  private async buildLocalSession(input: {
    accessToken: string;
    refreshToken: string;
    expiresAt: number;
    userId: string;
    phone: string;
    role: 'admin' | 'editor' | 'viewer';
    isNewUser: boolean;
  }): Promise<AuthSession> {
    const localUser = getLocalAuthUser(input.userId);
    const resolvedPhone = localUser?.phone ?? input.phone;

    return {
      access_token: input.accessToken,
      refresh_token: input.refreshToken,
      expires_at: toIsoString(input.expiresAt),
      user: {
        id: input.userId,
        phone: maskPhone(`+86${resolvedPhone}`) || resolvedPhone,
        display_name: null,
        avatar_url: null,
        role: input.role,
        is_new_user: input.isNewUser,
      },
    };
  }

  private async buildSession(input: SessionUserMeta): Promise<AuthSession> {
    let profile = await this.authProfileRepository.findAuthUserById(input.session.user.id);
    if (!profile) {
      await this.authProfileRepository.ensureAuthUserProfile(input.session.user.id);
      profile = await this.authProfileRepository.findAuthUserById(input.session.user.id);
    }

    if (!profile) {
      throw Errors.notFound('User', input.session.user.id);
    }

    return {
      access_token: input.session.access_token,
      refresh_token: input.session.refresh_token,
      expires_at: toIsoString(input.session.expires_at),
      user: {
        ...profile,
        phone: maskPhone(input.session.user.phone) || profile.phone,
        role: normalizeRole(input.session.user.app_metadata?.role),
        is_new_user: input.isNewUser,
      },
    };
  }
}
