import { describe, it, expect } from 'vitest';
import {
  HealthResponseSchema,
  MallBannerSchema,
  MallBridgeMessageSchema,
  MallHostMessageSchema,
  MallLoginContextSchema,
  MallProductSchema,
  MallProductsQuerySchema,
  MallProductsResponseSchema,
  MallShortcutSchema,
} from '@/lib/schemas';

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

describe('mall schemas', () => {
  it('parses a valid mall product response', () => {
    const result = MallProductsResponseSchema.parse({
      data: [
        {
          id: '550e8400-e29b-41d4-a716-446655440101',
          title: '轻奢真丝睡衣礼盒',
          image_url: 'https://example.com/mall/products/pajama-gift-box.jpg',
          price: 199,
          tags: ['热卖', '包邮'],
        },
      ],
      pagination: {
        page: 1,
        page_size: 20,
        total: 42,
        total_pages: 3,
      },
    });

    expect(result.data).toHaveLength(1);
    expect(result.pagination.total_pages).toBe(3);
  });

  it('rejects invalid mall product ids', () => {
    expect(() =>
      MallProductSchema.parse({
        id: 'not-a-uuid',
        title: '商品',
        image_url: 'https://example.com/image.jpg',
        price: 99,
        tags: [],
      }),
    ).toThrow();
  });

  it('rejects invalid image urls', () => {
    expect(() =>
      MallProductSchema.parse({
        id: '550e8400-e29b-41d4-a716-446655440101',
        title: '商品',
        image_url: 'invalid-url',
        price: 99,
        tags: [],
      }),
    ).toThrow();
  });

  it('coerces product query defaults', () => {
    const result = MallProductsQuerySchema.parse({});
    expect(result).toEqual({ page: 1, pageSize: 20 });
  });

  it('rejects invalid product query values', () => {
    expect(() => MallProductsQuerySchema.parse({ page: 0, pageSize: 101 })).toThrow();
  });

  it('parses banner config', () => {
    const result = MallBannerSchema.parse({
      id: 'hero-1',
      image_url: 'https://example.com/banner.jpg',
      target_type: 'search',
      target_value: '',
      sort_order: 0,
    });

    expect(result.target_type).toBe('search');
  });

  it('parses shortcut config', () => {
    const result = MallShortcutSchema.parse({
      key: 'orders',
      title: '我的订单',
      icon: '📦',
      behavior: 'placeholder-feedback',
    });

    expect(result.key).toBe('orders');
  });

  it('parses login bridge payload', () => {
    const result = MallLoginContextSchema.parse({
      source: 'mall',
      productId: '550e8400-e29b-41d4-a716-446655440101',
      returnTarget: '/mall',
    });

    expect(result.returnTarget).toBe('/mall');
  });

  it('rejects invalid login bridge payload', () => {
    expect(() =>
      MallLoginContextSchema.parse({
        source: 'home',
        productId: '550e8400-e29b-41d4-a716-446655440101',
        returnTarget: '/mall',
      }),
    ).toThrow();
  });

  it('parses bridge messages', () => {
    const result = MallBridgeMessageSchema.parse({
      type: 'mall.openSearch',
      payload: {
        source: 'mall',
        returnTarget: '/mall',
      },
    });

    expect(result.type).toBe('mall.openSearch');
  });

  it('parses host restore messages with default preserveScroll', () => {
    const result = MallHostMessageSchema.parse({
      type: 'mall.restoreContext',
      payload: {
        source: 'mall',
        reason: 'login-return',
        returnTarget: '/mall',
      },
    });

    expect(result.payload.preserveScroll).toBe(false);
  });

  it('rejects invalid host message payloads', () => {
    expect(() =>
      MallHostMessageSchema.parse({
        type: 'mall.syncAuthState',
        payload: {
          source: 'mall',
          isLoggedIn: 'yes',
          reason: 'initial-load',
          returnTarget: '/mall',
        },
      }),
    ).toThrow();
  });
});
