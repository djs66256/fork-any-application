import { NextRequest, NextResponse } from 'next/server';
import { Errors, formatErrorResponse } from '@/lib/errors';
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
