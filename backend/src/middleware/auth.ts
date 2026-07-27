import { NextRequest, NextResponse } from 'next/server';
import { Errors, formatErrorResponse } from '@/lib/errors';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import type { RouteHandler } from './error-handler';

function extractBearerToken(request: NextRequest): string | null {
  const authHeader = request.headers.get('Authorization');

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null;
  }

  const token = authHeader.slice('Bearer '.length).trim();
  return token.length > 0 ? token : null;
}

export function getOptionalUserId(request: NextRequest): string | undefined {
  const explicitUserId = request.headers.get('x-user-id')?.trim();
  if (explicitUserId) {
    return explicitUserId;
  }

  return extractBearerToken(request) ?? undefined;
}

export function getAuthenticatedUserId(request: NextRequest): string {
  const userId = getOptionalUserId(request);
  if (!userId) {
    throw Errors.unauthorized();
  }

  return userId;
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

  try {
    const supabase = getSupabaseAdmin();
    const { data: { user }, error } = await supabase.auth.getUser(token);

    if (error || !user) {
      return null;
    }

    const role = user.app_metadata?.role as string || 'viewer';

    // Log warning for unknown role values
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

/**
 * Skeleton auth wrapper. In the skeleton phase, it validates the
 * Authorization header format but does not enforce authentication.
 * Future implementation will verify the JWT via Supabase.
 */
export function requireAuth(handler: RouteHandler): RouteHandler {
  return async (request: NextRequest, context: unknown) => {
    if (!extractBearerToken(request)) {
      const err = Errors.unauthorized();
      return NextResponse.json(formatErrorResponse(err), { status: err.statusCode });
    }

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
      const err = Errors.unauthorized('请先登录');
      return NextResponse.json(
        { code: err.statusCode, data: null, message: err.message },
        { status: err.statusCode },
      );
    }

    if (!roles.includes(auth.role)) {
      const err = Errors.forbidden('无权访问');
      return NextResponse.json(
        { code: err.statusCode, data: null, message: err.message },
        { status: err.statusCode },
      );
    }

    // Inject auth context into request
    (request as Record<string, unknown>).auth = auth;

    return handler(request, context);
  };
}

/**
 * Extract AuthContext from request (set by requireRole middleware).
 */
export function getAuth(request: NextRequest): AuthContext {
  const auth = (request as Record<string, unknown>).auth as AuthContext | undefined;
  if (!auth) {
    throw Errors.unauthorized('Auth context not found');
  }
  return auth;
}