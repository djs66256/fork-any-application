import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { EarnPageScreen } from '@/features/earn/EarnPageScreen';

const { useEarnPage } = vi.hoisted(() => ({
  useEarnPage: vi.fn(),
}));

vi.mock('@/features/earn/hooks/useEarnPage', () => ({
  useEarnPage,
}));

function createHookResult(overrides: Partial<ReturnType<typeof useEarnPage>> = {}) {
  return {
    state: {
      overview: {
        coins: 1200,
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
        cash_tasks: [
          {
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
          },
        ],
      },
      isLoading: false,
      errorMessage: null,
      isLoggedIn: false,
      apiAccessToken: null,
      loginPromptVisible: false,
      activeTask: null,
      pendingCompletionTaskId: null,
      feedbackMessage: null,
      pendingRestoreReason: null,
      isCompletingTask: false,
    },
    retryInitialLoad: vi.fn(),
    handleTaskClick: vi.fn(),
    continueLogin: vi.fn(),
    cancelLoginPrompt: vi.fn(),
    dismissFeedback: vi.fn(),
    handleHeroLoginClick: vi.fn(),
    ...overrides,
  };
}

describe('EarnPageScreen', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders earn page sections on success', () => {
    useEarnPage.mockReturnValue(createHookResult());

    render(<EarnPageScreen />);

    expect(screen.getByText('赚钱中心')).toBeInTheDocument();
    expect(screen.getByText('新手任务')).toBeInTheDocument();
    expect(screen.getByText('连续看剧福利')).toBeInTheDocument();
    expect(screen.getByText('现金任务列表')).toBeInTheDocument();
    expect(screen.getByText('新人7天保底6元')).toBeInTheDocument();
    expect(screen.getByText('看剧领现金')).toBeInTheDocument();
  });

  it('renders empty cash task state without hiding static sections', () => {
    useEarnPage.mockReturnValue(
      createHookResult({
        state: {
          ...createHookResult().state,
          overview: {
            ...createHookResult().state.overview,
            cash_tasks: [],
          },
        },
      }),
    );

    render(<EarnPageScreen />);

    expect(screen.getByText('暂无现金任务，去看看上方福利和新手任务吧。')).toBeInTheDocument();
    expect(screen.getByText('连续看剧福利')).toBeInTheDocument();
  });

  it('renders error state and retries', async () => {
    const retryInitialLoad = vi.fn();
    useEarnPage.mockReturnValue(
      createHookResult({
        state: {
          ...createHookResult().state,
          overview: null,
          errorMessage: '服务开小差了，请稍后重试',
        },
        retryInitialLoad,
      }),
    );

    render(<EarnPageScreen />);

    expect(screen.getByText('赚钱首页加载失败')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '重试' }));

    await waitFor(() => {
      expect(retryInitialLoad).toHaveBeenCalledTimes(1);
    });
  });

  it('shows login prompt and continues login when anonymous users click representative tasks', () => {
    const continueLogin = vi.fn();
    const cancelLoginPrompt = vi.fn();
    useEarnPage.mockReturnValue(
      createHookResult({
        state: {
          ...createHookResult().state,
          loginPromptVisible: true,
          activeTask: createHookResult().state.overview.new_user_task,
        },
        continueLogin,
        cancelLoginPrompt,
      }),
    );

    render(<EarnPageScreen />);

    expect(screen.getByText('登录后可继续领取任务奖励')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '继续登录' }));
    fireEvent.click(screen.getByRole('button', { name: '先看看' }));

    expect(continueLogin).toHaveBeenCalledTimes(1);
    expect(cancelLoginPrompt).toHaveBeenCalledTimes(1);
  });

  it('shows feedback toast and dismisses it', () => {
    const dismissFeedback = vi.fn();
    useEarnPage.mockReturnValue(
      createHookResult({
        state: {
          ...createHookResult().state,
          feedbackMessage: '请在 App 内完成该任务',
        },
        dismissFeedback,
      }),
    );

    render(<EarnPageScreen />);

    expect(screen.getByText('请在 App 内完成该任务')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '知道了' }));

    expect(dismissFeedback).toHaveBeenCalledTimes(1);
  });
});
