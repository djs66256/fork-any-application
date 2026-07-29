import { describe, expect, it } from 'vitest';
import { EarnMockRepository, EARN_MOCK_IDS } from '@/repositories/mock/earn.mock.repository';

describe('EarnMockRepository', () => {
  it('should return anonymous overview with zero coins and stable sections', async () => {
    const repository = new EarnMockRepository();

    const result = await repository.getOverview({ userId: undefined });

    expect(result.coins).toBe(0);
    expect(result.is_logged_in).toBe(false);
    expect(result.daily_rewards).toHaveLength(7);
    expect(result.cash_tasks[0]?.id).toBe(EARN_MOCK_IDS.representativeTaskId);
    expect(result.cash_tasks[0]?.status).toBe('available');
  });

  it('should return logged-in overview with base coins and completed representative task status', async () => {
    const repository = new EarnMockRepository();
    const userId = '550e8400-e29b-41d4-a716-446655440001';

    await repository.completeTask({ userId, taskId: EARN_MOCK_IDS.representativeTaskId });
    const result = await repository.getOverview({ userId });

    expect(result.is_logged_in).toBe(true);
    expect(result.coins).toBe(1700);
    expect(result.cash_tasks.find((task) => task.id === EARN_MOCK_IDS.representativeTaskId)?.status).toBe('completed');
  });

  it('should complete representative task idempotently without double-counting coins', async () => {
    const repository = new EarnMockRepository();
    const userId = '550e8400-e29b-41d4-a716-446655440002';

    const first = await repository.completeTask({ userId, taskId: EARN_MOCK_IDS.representativeTaskId });
    const second = await repository.completeTask({ userId, taskId: EARN_MOCK_IDS.representativeTaskId });

    expect(first).toEqual({
      success: true,
      task_id: EARN_MOCK_IDS.representativeTaskId,
      coins_earned: 500,
      total_coins: 1700,
      task_status: 'completed',
    });
    expect(second).toEqual({
      success: true,
      task_id: EARN_MOCK_IDS.representativeTaskId,
      coins_earned: 0,
      total_coins: 1700,
      task_status: 'completed',
    });
  });

  it('should reject unknown tasks with not found error', async () => {
    const repository = new EarnMockRepository();

    await expect(
      repository.completeTask({
        userId: '550e8400-e29b-41d4-a716-446655440003',
        taskId: '44444444-4444-4444-8444-444444444444',
      }),
    ).rejects.toMatchObject({
      code: 'NOT_FOUND',
    });
  });

  it('should reject non-representative tasks with conflict error', async () => {
    const repository = new EarnMockRepository();

    await expect(
      repository.completeTask({
        userId: '550e8400-e29b-41d4-a716-446655440004',
        taskId: EARN_MOCK_IDS.lockedTaskId,
      }),
    ).rejects.toMatchObject({
      code: 'CONFLICT',
      message: 'Earn task cannot be completed',
    });
  });

  it('should clone overview payloads to avoid leaking mutable state', async () => {
    const repository = new EarnMockRepository();

    const first = await repository.getOverview({ userId: undefined });
    first.cash_tasks[0]!.status = 'completed';

    const second = await repository.getOverview({ userId: undefined });
    expect(second.cash_tasks[0]?.status).toBe('available');
  });
});
