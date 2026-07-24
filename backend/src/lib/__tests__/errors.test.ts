import { describe, it, expect } from 'vitest';
import {
  AppError,
  ErrorCode,
  Errors,
  formatErrorResponse,
} from '../errors';

describe('Errors factory', () => {
  it('should create notFound error with correct properties', () => {
    const err = Errors.notFound('Drama', 'abc');
    expect(err).toBeInstanceOf(AppError);
    expect(err.code).toBe(ErrorCode.NOT_FOUND);
    expect(err.message).toBe('Drama (abc) not found');
    expect(err.statusCode).toBe(404);
  });

  it('should create validationError with correct properties', () => {
    const err = Errors.validationError('Bad input');
    expect(err).toBeInstanceOf(AppError);
    expect(err.code).toBe(ErrorCode.VALIDATION_ERROR);
    expect(err.message).toBe('Bad input');
    expect(err.statusCode).toBe(400);
  });

  it('should create unauthorized with default message', () => {
    const err = Errors.unauthorized();
    expect(err.code).toBe(ErrorCode.UNAUTHORIZED);
    expect(err.statusCode).toBe(401);
    expect(err.message).toBe('Authentication required');
  });

  it('should create unauthorized with custom message', () => {
    const err = Errors.unauthorized('Invalid token');
    expect(err.message).toBe('Invalid token');
  });

  it('should create forbidden with default message', () => {
    const err = Errors.forbidden();
    expect(err.code).toBe(ErrorCode.FORBIDDEN);
    expect(err.statusCode).toBe(403);
  });

  it('should create conflict with message', () => {
    const err = Errors.conflict('Drama already exists');
    expect(err.code).toBe(ErrorCode.CONFLICT);
    expect(err.statusCode).toBe(409);
  });

  it('should create tooManyRequests with default message', () => {
    const err = Errors.tooManyRequests();
    expect(err.code).toBe(ErrorCode.TOO_MANY_REQUESTS);
    expect(err.statusCode).toBe(429);
  });

  it('should create internal with default message', () => {
    const err = Errors.internal();
    expect(err.code).toBe(ErrorCode.INTERNAL_ERROR);
    expect(err.statusCode).toBe(500);
  });

  it('should create notImplemented with default message', () => {
    const err = Errors.notImplemented();
    expect(err.code).toBe(ErrorCode.NOT_IMPLEMENTED);
    expect(err.statusCode).toBe(501);
  });

  it('should create serviceUnavailable with service name', () => {
    const err = Errors.serviceUnavailable('Redis');
    expect(err.code).toBe(ErrorCode.SERVICE_UNAVAILABLE);
    expect(err.statusCode).toBe(503);
    expect(err.message).toBe('Service unavailable: Redis');
  });
});

describe('formatErrorResponse', () => {
  it('should serialize AppError to error response body', () => {
    const err = Errors.validationError('Bad input');
    const body = formatErrorResponse(err);
    expect(body).toEqual({
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Bad input',
      },
    });
  });

  it('should serialize notImplemented error correctly', () => {
    const err = Errors.notImplemented();
    const body = formatErrorResponse(err);
    expect(body.error.code).toBe('NOT_IMPLEMENTED');
    expect(body.error.message).toBe('Not implemented');
  });
});
