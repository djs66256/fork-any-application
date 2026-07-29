import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  isEarnBridgeAvailable,
  openEarnTaskPlayer,
  requestEarnLogin,
  type EarnNativeBridge,
} from '@/features/earn/bridge/earn-bridge';

describe('earn-bridge', () => {
  beforeEach(() => {
    delete (window as typeof window & { __EARN_NATIVE_BRIDGE__?: EarnNativeBridge }).__EARN_NATIVE_BRIDGE__;
    vi.restoreAllMocks();
  });

  it('uses native bridge when available for login', () => {
    const postMessage = vi.fn();
    (window as typeof window & { __EARN_NATIVE_BRIDGE__?: EarnNativeBridge }).__EARN_NATIVE_BRIDGE__ = {
      postMessage,
    };

    const result = requestEarnLogin({
      source: 'earn',
      returnTarget: '/earn',
    });

    expect(result).toBe('bridge');
    expect(postMessage).toHaveBeenCalledWith({
      type: 'earn.requestLogin',
      payload: {
        source: 'earn',
        returnTarget: '/earn',
      },
    });
  });

  it('uses native bridge when available for task player', () => {
    const postMessage = vi.fn();
    (window as typeof window & { __EARN_NATIVE_BRIDGE__?: EarnNativeBridge }).__EARN_NATIVE_BRIDGE__ = {
      postMessage,
    };

    const result = openEarnTaskPlayer({
      taskId: '22222222-2222-4222-8222-222222222222',
      source: 'earn',
      returnTarget: '/earn',
      videoId: 'drama-001-episode-01',
    });

    expect(result).toBe('bridge');
    expect(postMessage).toHaveBeenCalledWith({
      type: 'earn.openTaskPlayer',
      payload: {
        taskId: '22222222-2222-4222-8222-222222222222',
        source: 'earn',
        returnTarget: '/earn',
        videoId: 'drama-001-episode-01',
      },
    });
  });

  it('returns browser fallback when login bridge is unavailable', () => {
    expect(
      requestEarnLogin({
        source: 'earn',
        returnTarget: '/earn',
      }),
    ).toBe('browser-fallback');
  });

  it('returns browser fallback when task player bridge is unavailable', () => {
    expect(
      openEarnTaskPlayer({
        taskId: '22222222-2222-4222-8222-222222222222',
        source: 'earn',
        returnTarget: '/earn',
        videoId: 'drama-001-episode-01',
      }),
    ).toBe('browser-fallback');
  });

  it('reports bridge availability', () => {
    expect(isEarnBridgeAvailable()).toBe(false);

    (window as typeof window & { __EARN_NATIVE_BRIDGE__?: EarnNativeBridge }).__EARN_NATIVE_BRIDGE__ = {
      postMessage: vi.fn(),
    };

    expect(isEarnBridgeAvailable()).toBe(true);
  });

  it('throws when native bridge payload is invalid', () => {
    const postMessage = vi.fn();
    (window as typeof window & { __EARN_NATIVE_BRIDGE__?: EarnNativeBridge }).__EARN_NATIVE_BRIDGE__ = {
      postMessage,
    };

    expect(() =>
      openEarnTaskPlayer({
        taskId: 'invalid-task-id',
        source: 'earn',
        returnTarget: '/earn',
        videoId: 'drama-001-episode-01',
      } as never),
    ).toThrow();
  });
});
