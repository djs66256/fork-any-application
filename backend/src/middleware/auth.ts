import { NextRequest, NextResponse } from 'next/server';
import { AppError, Errors, formatErrorResponse } from '@/lib/errors';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { getLocalAuthSessionByAccessToken, isLocalAccessToken } from '@/services/auth/local-auth-session.store';
import type { RouteHandler } from './error-handler';

function extractBearerToken(request: NextRequest): string | null {
  const authHeader = request.headers.get('Authorization');

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null;
  }

  const token = authHeader.slice('Bearer '.length).trim();
  return token.length > 0 ? token : null;
}

export interface AuthContext {
  userId: string;
  role: string;
}

/**
 * Verify JWT token using Supabase Admin Client.
 * Returns AuthContext with userId and role, or null if invalid.
 */
export async function verifyJwt(request: NextRequest): Promise<AuthContext | null> {
  const token = extractBearerToken(request);
  if (!token) return null;

  if (isLocalAccessToken(token)) {
    const localSession = getLocalAuthSessionByAccessToken(token);
    if (!localSession) {
      return null;
    }

    return {
      userId: localSession.userId,
      role: localSession.role,
    };
  }

  try {
    const supabase = getSupabaseAdmin();
    const { data: { user }, error } = await supabase.auth.getUser(token);

    if (error || !user) {
      return null;
    }

    const role = user.app_metadata?.role as string || 'viewer';

    if (!['admin', 'editor', 'viewer'].includes(role)) {
      console.warn(`[Auth] Unknown role '${role}' for user ${user.id}, treating as viewer`);
      return { userId: user.id, role: 'viewer' };
    }

    return { userId: user.id, role };
  } catch (err) {
    console.error('[Auth] JWT verification failed:', err);
    return null;
  }
}

export async function resolveOptionalAuthContext(request: NextRequest): Promise<AuthContext | null> {
  return verifyJwt(request);
}

export async function resolveRequiredAuthContext(request: NextRequest): Promise<AuthContext> {
  const auth = await verifyJwt(request);
  if (!auth) {
    throw Errors.authUnauthorized('请先登录');
  }

  return auth;
}

/**
 * Skeleton auth wrapper retained for compatibility.
 * It now validates through the canonical auth contract.
 */
export function requireAuth(handler: RouteHandler): RouteHandler {
  return async (request: NextRequest, context: unknown) => {
    try {
      const auth = await resolveRequiredAuthContext(request);
      (request as unknown as Record<string, unknown>).auth = auth;
      return handler(request, context);
    } catch (err) {
      if (err instanceof AppError) {
        return NextResponse.json(formatErrorResponse(err), { status: err.statusCode });
      }
      throw err;
    }
  };
}

export function requireAuthContext(handler: RouteHandler): RouteHandler {
  return async (request: NextRequest, context: unknown) => {
    const auth = await resolveRequiredAuthContext(request);
    (request as unknown as Record<string, unknown>).auth = auth;
    return handler(request, context);
  };
}

/**
 * Middleware that verifies JWT and checks the user's role.
 * Injects AuthContext into request.auth for downstream handlers.
 */
export function requireRole(roles: string[], handler: RouteHandler): RouteHandler {
  return async (request: NextRequest, context: unknown) => {
    const auth = await verifyJwt(request);

    if (!auth) {
      const err = Errors.authUnauthorized('请先登录');
      return NextResponse.json(formatErrorResponse(err), { status: err.statusCode });
    }

    if (!roles.includes(auth.role)) {
      const err = Errors.forbidden('无权访问');
      return NextResponse.json(formatErrorResponse(err), { status: err.statusCode });
    }

    (request as unknown as Record<string, unknown>).auth = auth;

    return handler(request, context);
  };
}

/**
 * Extract AuthContext from request (set by middleware helpers).
 */
export function getAuth(request: NextRequest): AuthContext {
  const auth = (request as unknown as Record<string, unknown>).auth as AuthContext | undefined;
  if (!auth) {
    throw Errors.authUnauthorized('Auth context not found');
  }
  return auth;
}
