import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/types';
import { useEarnPage } from '@/features/earn/hooks/useEarnPage';
import type { EarnOverviewResponse, EarnTask } from '@/lib/schemas';

const {
  getEarnOverview,
  completeEarnTask,
  requestEarnLogin,
  openEarnTaskPlayer,
  subscribeEarnHostMessages,
} = vi.hoisted(() => ({
  getEarnOverview: vi.fn(),
  completeEarnTask: vi.fn(),
  requestEarnLogin: vi.fn(),
  openEarnTaskPlayer: vi.fn(),
  subscribeEarnHostMessages: vi.fn(),
}));

vi.mock('@/lib/earn/api', () => ({
  getEarnOverview,
  completeEarnTask,
}));

vi.mock('@/features/earn/bridge/earn-bridge', () => ({
  requestEarnLogin,
  openEarnTaskPlayer,
}));

vi.mock('@/features/earn/bridge/earn-host-sync', () => ({
  subscribeEarnHostMessages,
}));

const representativeTask: EarnTask = {
  id: '22222222-2222-4222-8222-222222222222',
  title: '看剧领现金',
  description: '完整观看指定短剧可获得金币',
  reward_coins: 500,
  status: 'available',
  action: {
    type: 'play',
    video_id: 'drama-001-episode-01',
  },
  is_representative: true,
};

const placeholderTask: EarnTask = {
  id: '33333333-3333-4333-8333-333333333333',
  title: '每日逛逛赚钱页',
  description: '开发中的展示任务',
  reward_coins: 50,
  status: 'locked',
  action: {
    type: 'placeholder',
    feedback: '该任务开发中，敬请期待',
  },
};

function createOverview(overrides: Partial<EarnOverviewResponse> = {}): EarnOverviewResponse {
  return {
    coins: 100,
    is_logged_in: false,
    new_user_task: {
      id: '11111111-1111-4111-8111-111111111111',
      title: '新人7天保底6元',
      description: '完成首次看剧任务即可领取金币奖励',
      reward_coins: 600,
      status: 'available',
      action: {
        type: 'play',
        video_id: 'drama-001-episode-01',
      },
      is_representative: true,
    },
    daily_rewards: Array.from({ length: 7 }, (_, index) => ({
      day: index + 1,
      coins: (index + 1) * 10,
      status: index === 0 ? 'claimable' : 'locked',
    })),
    cash_tasks: [representativeTask, placeholderTask],
    ...overrides,
  };
}

describe('useEarnPage', () => {
  let hostHandler: ((event: unknown) => void) | undefined;

  beforeEach(() => {
    vi.clearAllMocks();
    hostHandler = undefined;
    subscribeEarnHostMessages.mockImplementation((handler: (event: unknown) => void) => {
      hostHandler = handler;
      return () => undefined;
    });
    requestEarnLogin.mockReturnValue('bridge');
    openEarnTaskPlayer.mockReturnValue('bridge');
    getEarnOverview.mockResolvedValue(createOverview());
  });

  it('loads overview successfully on mount', async () => {
    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    expect(getEarnOverview).toHaveBeenCalledTimes(1);
    expect(result.current.state.errorMessage).toBeNull();
    expect(result.current.state.overview?.cash_tasks).toHaveLength(2);
  });

  it('keeps header and sets empty cash task state data when overview is empty', async () => {
    getEarnOverview.mockResolvedValueOnce(createOverview({ cash_tasks: [] }));

    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview?.cash_tasks).toEqual([]);
    });

    expect(result.current.state.overview?.new_user_task.title).toBe('新人7天保底6元');
  });

  it('stores initial load errors and supports retry', async () => {
    getEarnOverview.mockRejectedValueOnce(new Error('boom'));
    getEarnOverview.mockResolvedValueOnce(createOverview());

    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.errorMessage).toBe('服务开小差了，请稍后重试');
    });

    act(() => {
      result.current.retryInitialLoad();
    });

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });
  });

  it('shows login prompt for anonymous representative task clicks', async () => {
    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    act(() => {
      result.current.handleTaskClick(representativeTask);
    });

    expect(result.current.state.loginPromptVisible).toBe(true);
    expect(result.current.state.activeTask).toEqual(representativeTask);
    expect(openEarnTaskPlayer).not.toHaveBeenCalled();
  });

  it('shows placeholder feedback for not-yet-open tasks', async () => {
    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    act(() => {
      result.current.handleTaskClick(placeholderTask);
    });

    expect(result.current.state.feedbackMessage).toBe('该任务开发中，敬请期待');
  });

  it('continues login with browser fallback feedback when bridge is unavailable', async () => {
    requestEarnLogin.mockReturnValueOnce('browser-fallback');

    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    act(() => {
      result.current.handleTaskClick(representativeTask);
      result.current.continueLogin();
    });

    expect(requestEarnLogin).toHaveBeenCalledWith({
      source: 'earn',
      returnTarget: '/earn',
    });
    expect(result.current.state.feedbackMessage).toBe('暂时无法打开登录，请稍后再试');
  });

  it('opens player for logged-in users and falls back in browser mode', async () => {
    openEarnTaskPlayer.mockReturnValueOnce('browser-fallback');

    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    act(() => {
      hostHandler?.({
        type: 'earn.syncAuthState',
        payload: {
          source: 'earn',
          isLoggedIn: true,
          reason: 'login-success',
          returnTarget: '/earn',
          apiAccessToken: 'token-123',
        },
      });
    });

    act(() => {
      result.current.handleTaskClick(representativeTask);
    });

    expect(openEarnTaskPlayer).toHaveBeenCalledWith({
      taskId: representativeTask.id,
      source: 'earn',
      returnTarget: '/earn',
      videoId: 'drama-001-episode-01',
    });
    expect(result.current.state.feedbackMessage).toBe('请在 App 内完成该任务');
  });

  it('stores auth token only in memory state on host sync', async () => {
    const storageSpy = vi.spyOn(Storage.prototype, 'setItem');

    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    act(() => {
      hostHandler?.({
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

    expect(result.current.state.isLoggedIn).toBe(true);
    expect(result.current.state.apiAccessToken).toBe('token-123');
    expect(storageSpy).not.toHaveBeenCalled();
  });

  it('completes task and updates coins after host callback', async () => {
    completeEarnTask.mockResolvedValueOnce({
      success: true,
      task_id: representativeTask.id,
      coins_earned: 500,
      total_coins: 600,
      task_status: 'completed',
    });

    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    act(() => {
      hostHandler?.({
        type: 'earn.syncAuthState',
        payload: {
          source: 'earn',
          isLoggedIn: true,
          reason: 'login-success',
          returnTarget: '/earn',
          apiAccessToken: 'token-123',
        },
      });
    });

    act(() => {
      hostHandler?.({
        type: 'earn.completeTask',
        payload: {
          source: 'earn',
          taskId: representativeTask.id,
          videoId: 'drama-001-episode-01',
          completed: true,
          reason: 'playback-ended',
        },
      });
    });

    await waitFor(() => {
      expect(completeEarnTask).toHaveBeenCalledWith(representativeTask.id, 'token-123');
    });

    expect(result.current.state.overview?.coins).toBe(600);
    expect(result.current.state.overview?.cash_tasks[0].status).toBe('completed');
  });

  it('does not call complete-task api when playback callback is incomplete', async () => {
    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    act(() => {
      hostHandler?.({
        type: 'earn.completeTask',
        payload: {
          source: 'earn',
          taskId: representativeTask.id,
          videoId: 'drama-001-episode-01',
          completed: false,
          reason: 'user-exit',
        },
      });
    });

    expect(completeEarnTask).not.toHaveBeenCalled();
  });

  it('requires relogin when completion callback arrives without token', async () => {
    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    act(() => {
      hostHandler?.({
        type: 'earn.completeTask',
        payload: {
          source: 'earn',
          taskId: representativeTask.id,
          videoId: 'drama-001-episode-01',
          completed: true,
          reason: 'playback-ended',
        },
      });
    });

    expect(completeEarnTask).not.toHaveBeenCalled();
    expect(result.current.state.loginPromptVisible).toBe(true);
    expect(result.current.state.feedbackMessage).toBe('请先登录后再领取奖励');
  });

  it('requires relogin when complete-task returns 401', async () => {
    completeEarnTask.mockRejectedValueOnce(new ApiError(401, '请先登录'));

    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview).not.toBeNull();
    });

    act(() => {
      hostHandler?.({
        type: 'earn.syncAuthState',
        payload: {
          source: 'earn',
          isLoggedIn: true,
          reason: 'login-success',
          returnTarget: '/earn',
          apiAccessToken: 'token-123',
        },
      });
    });

    act(() => {
      hostHandler?.({
        type: 'earn.completeTask',
        payload: {
          source: 'earn',
          taskId: representativeTask.id,
          videoId: 'drama-001-episode-01',
          completed: true,
          reason: 'playback-ended',
        },
      });
    });

    await waitFor(() => {
      expect(result.current.state.isLoggedIn).toBe(false);
    });

    expect(result.current.state.apiAccessToken).toBeNull();
    expect(result.current.state.loginPromptVisible).toBe(true);
  });

  it('reloads overview when container is recreated', async () => {
    getEarnOverview.mockResolvedValueOnce(createOverview());
    getEarnOverview.mockResolvedValueOnce(createOverview({ coins: 999 }));

    const { result } = renderHook(() => useEarnPage());

    await waitFor(() => {
      expect(result.current.state.overview?.coins).toBe(100);
    });

    act(() => {
      hostHandler?.({
        type: 'earn.restoreContext',
        payload: {
          source: 'earn',
          reason: 'container-recreated',
          returnTarget: '/earn',
        },
      });
    });

    await waitFor(() => {
      expect(result.current.state.overview?.coins).toBe(999);
    });
  });
});
