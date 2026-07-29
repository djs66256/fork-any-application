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
  CompleteEarnTaskRequestSchema,
  EarnBridgeMessageSchema,
  EarnHostMessageSchema,
  EarnHostTransportSchema,
  EarnLoginContextSchema,
  EarnOverviewResponseSchema,
  EarnTaskContextSchema,
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

describe('earn schemas', () => {
  const validOverview = {
    coins: 1200,
    is_logged_in: false,
    new_user_task: {
      id: '11111111-1111-4111-8111-111111111111',
      title: '新人7天保底6元',
      description: '完成首次看剧任务即可领取金币奖励',
      reward_coins: 600,
      status: 'available' as const,
      action: {
        type: 'play' as const,
        video_id: 'drama-001-episode-01',
      },
      is_representative: true,
    },
    daily_rewards: Array.from({ length: 7 }, (_, index) => ({
      day: index + 1,
      coins: (index + 1) * 10,
      status: index === 0 ? ('claimable' as const) : ('locked' as const),
    })),
    cash_tasks: [
      {
        id: '22222222-2222-4222-8222-222222222222',
        title: '看剧领现金',
        description: '完整观看指定短剧可获得金币',
        reward_coins: 500,
        status: 'available' as const,
        action: {
          type: 'play' as const,
          video_id: 'drama-001-episode-01',
        },
        is_representative: true,
      },
      {
        id: '33333333-3333-4333-8333-333333333333',
        title: '每日逛逛赚钱页',
        description: '开发中的展示任务',
        reward_coins: 50,
        status: 'locked' as const,
        action: {
          type: 'placeholder' as const,
          feedback: '该任务开发中，敬请期待',
        },
      },
    ],
  };

  it('parses valid earn overview data', () => {
    const result = EarnOverviewResponseSchema.parse(validOverview);
    expect(result.cash_tasks).toHaveLength(2);
    expect(result.daily_rewards).toHaveLength(7);
  });

  it('rejects invalid earn task id', () => {
    expect(() =>
      EarnOverviewResponseSchema.parse({
        ...validOverview,
        cash_tasks: [
          {
            ...validOverview.cash_tasks[0],
            id: 'invalid-id',
          },
        ],
      }),
    ).toThrow();
  });

  it('rejects invalid return target in task context', () => {
    expect(() =>
      EarnTaskContextSchema.parse({
        taskId: '22222222-2222-4222-8222-222222222222',
        source: 'earn',
        returnTarget: '/mall',
        videoId: 'drama-001-episode-01',
      }),
    ).toThrow();
  });

  it('parses login context', () => {
    const result = EarnLoginContextSchema.parse({
      source: 'earn',
      returnTarget: '/earn',
    });

    expect(result.returnTarget).toBe('/earn');
  });

  it('parses earn bridge messages', () => {
    const result = EarnBridgeMessageSchema.parse({
      type: 'earn.openTaskPlayer',
      payload: {
        taskId: '22222222-2222-4222-8222-222222222222',
        source: 'earn',
        returnTarget: '/earn',
        videoId: 'drama-001-episode-01',
      },
    });

    expect(result.type).toBe('earn.openTaskPlayer');
  });

  it('parses host transport contract', () => {
    const result = EarnHostTransportSchema.parse({
      type: 'custom-event',
      eventName: 'earn.hostMessage',
    });

    expect(result.eventName).toBe('earn.hostMessage');
  });

  it('rejects invalid host event name', () => {
    expect(() =>
      EarnHostTransportSchema.parse({
        type: 'custom-event',
        eventName: 'message',
      }),
    ).toThrow();
  });

  it('parses auth sync host messages with nullable token', () => {
    const result = EarnHostMessageSchema.parse({
      type: 'earn.syncAuthState',
      payload: {
        source: 'earn',
        isLoggedIn: true,
        reason: 'login-success',
        returnTarget: '/earn',
        apiAccessToken: 'token-123',
        expiresAt: null,
      },
    });

    expect(result.payload.apiAccessToken).toBe('token-123');
  });

  it('parses restore context messages with default preserveScroll', () => {
    const result = EarnHostMessageSchema.parse({
      type: 'earn.restoreContext',
      payload: {
        source: 'earn',
        reason: 'task-return',
        returnTarget: '/earn',
      },
    });

    expect(result.payload.preserveScroll).toBe(false);
  });

  it('parses task completion messages', () => {
    const result = EarnHostMessageSchema.parse({
      type: 'earn.completeTask',
      payload: {
        source: 'earn',
        taskId: '22222222-2222-4222-8222-222222222222',
        videoId: 'drama-001-episode-01',
        completed: true,
        reason: 'playback-ended',
      },
    });

    expect(result.type).toBe('earn.completeTask');
    expect(result.payload.completed).toBe(true);
  });

  it('rejects invalid earn host payloads', () => {
    expect(() =>
      EarnHostMessageSchema.parse({
        type: 'earn.syncAuthState',
        payload: {
          source: 'earn',
          isLoggedIn: 'yes',
          reason: 'initial-load',
          returnTarget: '/earn',
        },
      }),
    ).toThrow();
  });

  it('rejects invalid complete-task request payload', () => {
    expect(() =>
      CompleteEarnTaskRequestSchema.parse({
        task_id: 'not-a-uuid',
      }),
    ).toThrow();
  });
});
