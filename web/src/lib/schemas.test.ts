import { describe, it, expect } from 'vitest';
import { HealthResponseSchema } from '@/lib/schemas';

describe('HealthResponseSchema', () => {
  const validHealthResponse = {
    status: 'ok' as const,
    version: '0.1.0',
    services: {
      database: 'connected' as const,
      redis: 'connected' as const,
    },
  };

  it('should parse valid health response data', () => {
    const result = HealthResponseSchema.parse(validHealthResponse);
    expect(result).toEqual(validHealthResponse);
  });

  it('should accept "degraded" status', () => {
    const result = HealthResponseSchema.parse({
      ...validHealthResponse,
      status: 'degraded',
      services: {
        database: 'degraded',
        redis: 'connected',
      },
    });
    expect(result.status).toBe('degraded');
    expect(result.services.database).toBe('degraded');
  });

  it('should accept "error" status', () => {
    const result = HealthResponseSchema.parse({
      ...validHealthResponse,
      status: 'error',
      services: {
        database: 'disconnected',
        redis: 'disconnected',
      },
    });
    expect(result.status).toBe('error');
  });

  it('should reject data missing required fields', () => {
    expect(() => HealthResponseSchema.parse({ status: 'error' })).toThrow();
  });

  it('should reject invalid status value', () => {
    expect(() =>
      HealthResponseSchema.parse({
        status: 'invalid',
        version: '0.1.0',
        services: { database: 'connected', redis: 'connected' },
      }),
    ).toThrow();
  });

  it('should reject invalid services values', () => {
    expect(() =>
      HealthResponseSchema.parse({
        ...validHealthResponse,
        services: { database: 'unknown', redis: 'connected' },
      }),
    ).toThrow();
  });
});
