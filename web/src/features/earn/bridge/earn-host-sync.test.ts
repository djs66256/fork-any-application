import { describe, expect, it, vi } from 'vitest';
import { parseEarnHostMessage, subscribeEarnHostMessages } from '@/features/earn/bridge/earn-host-sync';

describe('earn-host-sync', () => {
  it('parses auth sync messages', () => {
    const result = parseEarnHostMessage({
      type: 'earn.syncAuthState',
      payload: {
        source: 'earn',
        isLoggedIn: true,
        reason: 'initial-load',
        returnTarget: '/earn',
        apiAccessToken: 'token-123',
      },
    });

    expect(result).toEqual({
      type: 'earn.syncAuthState',
      payload: {
        source: 'earn',
        isLoggedIn: true,
        reason: 'initial-load',
        returnTarget: '/earn',
        apiAccessToken: 'token-123',
      },
    });
  });

  it('parses restore context messages', () => {
    const result = parseEarnHostMessage({
      type: 'earn.restoreContext',
      payload: {
        source: 'earn',
        reason: 'login-return',
        returnTarget: '/earn',
        preserveScroll: true,
      },
    });

    expect(result).toEqual({
      type: 'earn.restoreContext',
      payload: {
        source: 'earn',
        reason: 'login-return',
        returnTarget: '/earn',
        preserveScroll: true,
      },
    });
  });

  it('parses complete-task messages', () => {
    const result = parseEarnHostMessage({
      type: 'earn.completeTask',
      payload: {
        source: 'earn',
        taskId: '22222222-2222-4222-8222-222222222222',
        videoId: 'drama-001-episode-01',
        completed: true,
        reason: 'playback-ended',
      },
    });

    expect(result).toEqual({
      type: 'earn.completeTask',
      payload: {
        source: 'earn',
        taskId: '22222222-2222-4222-8222-222222222222',
        videoId: 'drama-001-episode-01',
        completed: true,
        reason: 'playback-ended',
      },
    });
  });

  it('filters invalid payloads', () => {
    expect(
      parseEarnHostMessage({
        type: 'earn.syncAuthState',
        payload: {
          source: 'earn',
          isLoggedIn: 'yes',
          reason: 'initial-load',
          returnTarget: '/earn',
        },
      }),
    ).toBeNull();
  });

  it('subscribes only to earn.hostMessage custom events', () => {
    const handler = vi.fn();
    const unsubscribe = subscribeEarnHostMessages(handler);

    window.dispatchEvent(
      new CustomEvent('earn.hostMessage', {
        detail: {
          type: 'earn.restoreContext',
          payload: {
            source: 'earn',
            reason: 'task-return',
            returnTarget: '/earn',
          },
        },
      }),
    );

    window.dispatchEvent(
      new MessageEvent('message', {
        data: {
          type: 'earn.restoreContext',
          payload: {
            source: 'earn',
            reason: 'task-return',
            returnTarget: '/earn',
          },
        },
      }),
    );

    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler).toHaveBeenCalledWith({
      type: 'earn.restoreContext',
      payload: {
        source: 'earn',
        reason: 'task-return',
        returnTarget: '/earn',
        preserveScroll: false,
      },
    });

    unsubscribe();
  });

  it('ignores invalid custom event detail payloads', () => {
    const handler = vi.fn();
    const unsubscribe = subscribeEarnHostMessages(handler);

    window.dispatchEvent(
      new CustomEvent('earn.hostMessage', {
        detail: {
          type: 'earn.completeTask',
          payload: {
            source: 'earn',
            taskId: 'bad-id',
            videoId: 'drama-001-episode-01',
            completed: true,
            reason: 'playback-ended',
          },
        },
      }),
    );

    expect(handler).not.toHaveBeenCalled();
    unsubscribe();
  });
});
