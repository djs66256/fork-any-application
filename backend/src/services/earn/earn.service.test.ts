import { describe, expect, it } from 'vitest';
import { Errors } from '@/lib/errors';
import {
  CompleteEarnTaskResponse,
  EarnOverviewResponse,
} from '@/lib/schemas';
import {
  CompleteEarnTaskParams,
  EarnRepositoryInterface,
  GetEarnOverviewParams,
} from '@/repositories/interfaces/earn.repository.interface';
import { EarnService } from './earn.service';

const validOverview: EarnOverviewResponse = {
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
};

const validCompleteTaskResponse: CompleteEarnTaskResponse = {
  success: true,
  task_id: '22222222-2222-4222-8222-222222222222',
  coins_earned: 500,
  total_coins: 1700,
  task_status: 'completed',
};

class StubEarnRepository implements EarnRepositoryInterface {
  constructor(
    private readonly overviewResult: EarnOverviewResponse,
    private readonly completeTaskResult: CompleteEarnTaskResponse,
  ) {}

  async getOverview(params: GetEarnOverviewParams): Promise<EarnOverviewResponse> {
    void params;
    return this.overviewResult;
  }

  async completeTask(params: CompleteEarnTaskParams): Promise<CompleteEarnTaskResponse> {
    void params;
    return this.completeTaskResult;
  }
}

class InvalidOverviewRepository implements EarnRepositoryInterface {
  async getOverview(): Promise<EarnOverviewResponse> {
    return {
      ...validOverview,
      daily_rewards: validOverview.daily_rewards.slice(0, 6),
    } as EarnOverviewResponse;
  }

  async completeTask(): Promise<CompleteEarnTaskResponse> {
    return validCompleteTaskResponse;
  }
}

class InvalidCompleteTaskRepository implements EarnRepositoryInterface {
  async getOverview(): Promise<EarnOverviewResponse> {
    return validOverview;
  }

  async completeTask(): Promise<CompleteEarnTaskResponse> {
    return {
      ...validCompleteTaskResponse,
      task_status: 'claimed',
    } as CompleteEarnTaskResponse;
  }
}

class AppErrorEarnRepository implements EarnRepositoryInterface {
  async getOverview(): Promise<EarnOverviewResponse> {
    throw Errors.serviceUnavailable('earn-overview');
  }

  async completeTask(): Promise<CompleteEarnTaskResponse> {
    throw Errors.notFound('Earn task', '22222222-2222-4222-8222-222222222222');
  }
}

describe('EarnService', () => {
  it('should return validated overview for valid repository output', async () => {
    const service = new EarnService(new StubEarnRepository(validOverview, validCompleteTaskResponse));

    const result = await service.getOverview({ auth: undefined });

    expect(result).toEqual(validOverview);
  });

  it('should wrap invalid overview output as internal error', async () => {
    const service = new EarnService(new InvalidOverviewRepository());

    await expect(service.getOverview({ auth: undefined })).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
      message: 'Invalid earn overview result',
    });
  });

  it('should rethrow AppError from overview repository', async () => {
    const service = new EarnService(new AppErrorEarnRepository());

    await expect(service.getOverview({ auth: undefined })).rejects.toMatchObject({
      code: 'SERVICE_UNAVAILABLE',
    });
  });

  it('should return validated complete-task response for valid repository output', async () => {
    const service = new EarnService(new StubEarnRepository(validOverview, validCompleteTaskResponse));

    const result = await service.completeTask({
      auth: { userId: '550e8400-e29b-41d4-a716-446655440001', role: 'viewer' },
      taskId: '22222222-2222-4222-8222-222222222222',
    });

    expect(result).toEqual(validCompleteTaskResponse);
  });

  it('should wrap invalid complete-task output as internal error', async () => {
    const service = new EarnService(new InvalidCompleteTaskRepository());

    await expect(
      service.completeTask({
        auth: { userId: '550e8400-e29b-41d4-a716-446655440001', role: 'viewer' },
        taskId: '22222222-2222-4222-8222-222222222222',
      }),
    ).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
      message: 'Invalid earn complete-task result',
    });
  });

  it('should rethrow AppError from complete-task repository', async () => {
    const service = new EarnService(new AppErrorEarnRepository());

    await expect(
      service.completeTask({
        auth: { userId: '550e8400-e29b-41d4-a716-446655440001', role: 'viewer' },
        taskId: '22222222-2222-4222-8222-222222222222',
      }),
    ).rejects.toMatchObject({
      code: 'NOT_FOUND',
    });
  });
});
