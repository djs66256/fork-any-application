import { beforeEach, describe, expect, it, vi } from 'vitest';
import { config } from '@/lib/config';
import {
  isMallBridgeAvailable,
  openMallSearch,
  requestMallLogin,
  type MallNativeBridge,
} from '@/features/mall/bridge/mall-bridge';

describe('mall-bridge', () => {
  beforeEach(() => {
    delete (window as typeof window & { __MALL_NATIVE_BRIDGE__?: MallNativeBridge }).__MALL_NATIVE_BRIDGE__;
    vi.restoreAllMocks();
  });

  it('uses native bridge when available for search', () => {
    const postMessage = vi.fn();
    (window as typeof window & { __MALL_NATIVE_BRIDGE__?: MallNativeBridge }).__MALL_NATIVE_BRIDGE__ = {
      postMessage,
    };

    const mode = openMallSearch({
      source: 'mall',
      returnTarget: '/mall',
    });

    expect(mode).toBe('bridge');
    expect(postMessage).toHaveBeenCalledWith({
      type: 'mall.openSearch',
      payload: {
        source: 'mall',
        returnTarget: '/mall',
      },
    });
  });

  it('falls back to browser navigation when bridge is unavailable', () => {
    const assign = vi.fn();
    Object.defineProperty(window, 'location', {
      value: { assign },
      configurable: true,
    });

    const mode = openMallSearch({
      source: 'mall',
      returnTarget: '/mall',
    });

    expect(mode).toBe('browser-fallback');
    expect(assign).toHaveBeenCalledWith(config.mall.searchFallbackRoute);
  });

  it('reports bridge availability', () => {
    expect(isMallBridgeAvailable()).toBe(false);

    (window as typeof window & { __MALL_NATIVE_BRIDGE__?: MallNativeBridge }).__MALL_NATIVE_BRIDGE__ = {
      postMessage: vi.fn(),
    };

    expect(isMallBridgeAvailable()).toBe(true);
  });

  it('sends login requests through the native bridge', () => {
    const postMessage = vi.fn();
    (window as typeof window & { __MALL_NATIVE_BRIDGE__?: MallNativeBridge }).__MALL_NATIVE_BRIDGE__ = {
      postMessage,
    };

    requestMallLogin({
      source: 'mall',
      productId: '550e8400-e29b-41d4-a716-446655440101',
      returnTarget: '/mall',
    });

    expect(postMessage).toHaveBeenCalledWith({
      type: 'mall.requestLogin',
      payload: {
        source: 'mall',
        productId: '550e8400-e29b-41d4-a716-446655440101',
        returnTarget: '/mall',
      },
    });
  });

  it('throws when login bridge is unavailable', () => {
    expect(() =>
      requestMallLogin({
        source: 'mall',
        productId: '550e8400-e29b-41d4-a716-446655440101',
        returnTarget: '/mall',
      }),
    ).toThrow('Mall bridge is unavailable');
  });
});
