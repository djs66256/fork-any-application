import { NextResponse } from 'next/server';
import type { AuthSession, AuthUser } from '@/lib/schemas';

export function success<T>(data: T) {
  return NextResponse.json({
    code: 0,
    data,
    message: 'ok',
  });
}

export function mapAuthUserPayload(user: AuthUser) {
  return {
    id: user.id,
    phone: user.phone,
    displayName: user.display_name ?? null,
    avatarUrl: user.avatar_url ?? null,
    role: user.role,
    isNewUser: user.is_new_user,
  };
}

export function mapAuthSessionPayload(session: AuthSession) {
  return {
    accessToken: session.access_token,
    refreshToken: session.refresh_token,
    expiresAt: session.expires_at,
    user: mapAuthUserPayload(session.user),
  };
}

export function extractAccessToken(authorizationHeader: string | null): string | undefined {
  if (!authorizationHeader?.startsWith('Bearer ')) {
    return undefined;
  }

  const token = authorizationHeader.slice('Bearer '.length).trim();
  return token || undefined;
}
