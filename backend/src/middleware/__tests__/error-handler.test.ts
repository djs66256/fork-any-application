import { describe, it, expect, vi } from 'vitest';
import { NextRequest, NextResponse } from 'next/server';
import { ZodError } from 'zod';
import { withErrorHandler } from '../error-handler';
import { Errors } from '@/lib/errors';

function createMockRequest(method = 'GET'): NextRequest {
  return new NextRequest('https://localhost:3001/api/test', {
    method,
  });
}

describe('withErrorHandler', () => {
  it('should pass through successful handler response', async () => {
    const handler = vi.fn().mockResolvedValue(
      NextResponse.json({ ok: true }),
    );
    const wrapped = withErrorHandler(handler);
    const request = createMockRequest();
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({ ok: true });
  });

  it('should catch AppError and return formatted error response', async () => {
    const handler = vi.fn().mockRejectedValue(
      Errors.notFound('Drama', 'x'),
    );
    const wrapped = withErrorHandler(handler);
    const request = createMockRequest();
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error).toBeDefined();
    expect(body.error.code).toBe('NOT_FOUND');
    expect(body.error.message).toBe('Drama (x) not found');
  });

  it('should catch ZodError and return 400 with validation error', async () => {
    const handler = vi.fn().mockRejectedValue(
      new ZodError([
        {
          code: 'too_small',
          minimum: 1,
          type: 'string',
          inclusive: true,
          exact: false,
          message: 'String must contain at least 1 character(s)',
          path: ['title'],
        },
      ]),
    );
    const wrapped = withErrorHandler(handler);
    const request = createMockRequest();
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
    expect(body.error.details).toBeDefined();
  });

  it('should catch unknown errors and return 500', async () => {
    const handler = vi.fn().mockRejectedValue(new Error('Something broke'));
    const wrapped = withErrorHandler(handler);
    const request = createMockRequest();
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });

  it('should handle AppError with validationError code', async () => {
    const handler = vi.fn().mockRejectedValue(
      Errors.validationError('Bad input'),
    );
    const wrapped = withErrorHandler(handler);
    const request = createMockRequest();
    const response = await wrapped(request, {});
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });
});
