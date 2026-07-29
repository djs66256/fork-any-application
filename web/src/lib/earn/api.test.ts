import { describe, expect, it, vi, beforeEach } from 'vitest';
import { completeEarnTask, getEarnOverview } from '@/lib/earn/api';
import { api } from '@/lib/api-client';

vi.mock('@/lib/api-client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('earn api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('gets overview through core api client', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      coins: 0,
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
      },
      daily_rewards: Array.from({ length: 7 }, (_, index) => ({
        day: index + 1,
        coins: (index + 1) * 10,
        status: index === 0 ? 'claimable' : 'locked',
      })),
      cash_tasks: [],
    });

    const result = await getEarnOverview();

    expect(api.get).toHaveBeenCalledWith('/api/earn/overview');
    expect(result.coins).toBe(0);
  });

  it('posts complete-task with explicit bearer auth header', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      success: true,
      task_id: '22222222-2222-4222-8222-222222222222',
      coins_earned: 500,
      total_coins: 1200,
      task_status: 'completed',
    });

    const result = await completeEarnTask(
      '22222222-2222-4222-8222-222222222222',
      'token-123',
    );

    expect(api.post).toHaveBeenCalledWith(
      '/api/earn/complete-task',
      {
        task_id: '22222222-2222-4222-8222-222222222222',
      },
      {
        headers: {
          Authorization: 'Bearer token-123',
        },
      },
    );
    expect(result.total_coins).toBe(1200);
  });

  it('rejects invalid complete-task ids before sending request', async () => {
    await expect(completeEarnTask('invalid-task-id', 'token-123')).rejects.toThrow();
    expect(api.post).not.toHaveBeenCalled();
  });

  it('rejects invalid overview payloads', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      coins: 0,
      is_logged_in: false,
      new_user_task: null,
      daily_rewards: [],
      cash_tasks: [],
    });

    await expect(getEarnOverview()).rejects.toThrow();
  });
});
