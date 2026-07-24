import { NextRequest, NextResponse } from 'next/server';
import { ZodError } from 'zod';
import { AppError, Errors, formatErrorResponse } from '@/lib/errors';

// Next.js 16 uses params: Promise<...> for dynamic route segments.
// Use unknown context type for maximum compatibility.
export type RouteHandler = (request: NextRequest, context: unknown) => Promise<NextResponse>;

export function withErrorHandler(handler: RouteHandler): RouteHandler {
  return async (request: NextRequest, context: unknown) => {
    try {
      return await handler(request, context);
    } catch (err) {
      if (err instanceof AppError) {
        const body = formatErrorResponse(err);
        return NextResponse.json(body, { status: err.statusCode });
      }

      if (err instanceof ZodError) {
        const issues = err.issues.map((issue) => ({
          path: issue.path,
          message: issue.message,
          code: issue.code,
        }));
        return NextResponse.json(
          {
            error: {
              code: 'VALIDATION_ERROR',
              message: 'Validation failed',
              details: issues,
            },
          },
          { status: 400 },
        );
      }

      console.error('Unhandled error:', err);
      const internalError = Errors.internal();
      return NextResponse.json(formatErrorResponse(internalError), { status: 500 });
    }
  };
}
