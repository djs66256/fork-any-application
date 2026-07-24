import { NextRequest, NextResponse } from 'next/server';
import { Errors, formatErrorResponse } from '@/lib/errors';
import type { RouteHandler } from './error-handler';

/**
 * Skeleton auth wrapper. In the skeleton phase, it validates the
 * Authorization header format but does not enforce authentication.
 * Future implementation will verify the JWT via Supabase.
 */
export function requireAuth(handler: RouteHandler): RouteHandler {
  return async (request: NextRequest, context: unknown) => {
    const authHeader = request.headers.get('Authorization');

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      const err = Errors.unauthorized();
      return NextResponse.json(formatErrorResponse(err), { status: err.statusCode });
    }

    // Skeleton: accept any Bearer token, pass through
    return handler(request, context);
  };
}
