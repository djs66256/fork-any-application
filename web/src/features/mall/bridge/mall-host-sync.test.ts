import { describe, expect, it, vi } from 'vitest';
import { parseMallHostMessage, subscribeMallHostMessages } from '@/features/mall/bridge/mall-host-sync';

describe('mall-host-sync', () => {
  it('parses auth sync messages', () => {
    const result = parseMallHostMessage({
      type: 'mall.syncAuthState',
      payload: {
        source: 'mall',
        isLoggedIn: true,
        reason: 'initial-load',
        returnTarget: '/mall',
      },
    });

    expect(result).toEqual({
      type: 'mall.syncAuthState',
      payload: {
        source: 'mall',
        isLoggedIn: true,
        reason: 'initial-load',
        returnTarget: '/mall',
      },
    });
  });

  it('parses restore context messages', () => {
    const result = parseMallHostMessage({
      type: 'mall.restoreContext',
      payload: {
        source: 'mall',
        reason: 'login-return',
        returnTarget: '/mall',
        preserveScroll: true,
      },
    });

    expect(result).toEqual({
      type: 'mall.restoreContext',
      payload: {
        source: 'mall',
        reason: 'login-return',
        returnTarget: '/mall',
        preserveScroll: true,
      },
    });
  });

  it('filters invalid payloads', () => {
    expect(
      parseMallHostMessage({
        type: 'mall.syncAuthState',
        payload: {
          source: 'mall',
          isLoggedIn: 'yes',
          reason: 'initial-load',
          returnTarget: '/mall',
        },
      }),
    ).toBeNull();
  });

  it('subscribes to window message events', () => {
    const handler = vi.fn();
    const unsubscribe = subscribeMallHostMessages(handler);

    window.dispatchEvent(
      new MessageEvent('message', {
        data: {
          type: 'mall.restoreContext',
          payload: {
            source: 'mall',
            reason: 'search-return',
            returnTarget: '/mall',
          },
        },
      }),
    );

    expect(handler).toHaveBeenCalledWith({
      type: 'mall.restoreContext',
      payload: {
        source: 'mall',
        reason: 'search-return',
        returnTarget: '/mall',
        preserveScroll: false,
      },
    });

    unsubscribe();
  });
});
