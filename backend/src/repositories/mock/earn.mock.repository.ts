import {
  CompleteEarnTaskResponse,
  CompleteEarnTaskResponseSchema,
  EarnDailyReward,
  EarnDailyRewardSchema,
  EarnOverviewResponse,
  EarnOverviewResponseSchema,
  EarnTask,
  EarnTaskSchema,
} from '@/lib/schemas';
import { Errors } from '@/lib/errors';
import {
  CompleteEarnTaskParams,
  EarnRepositoryInterface,
  GetEarnOverviewParams,
} from '@/repositories/interfaces/earn.repository.interface';

const NEW_USER_TASK_ID = '11111111-1111-4111-8111-111111111111';
const REPRESENTATIVE_TASK_ID = '22222222-2222-4222-8222-222222222222';
const LOCKED_TASK_ID = '33333333-3333-4333-8333-333333333333';
const LOGGED_IN_BASE_COINS = 1200;

const BASE_NEW_USER_TASK: EarnTask = EarnTaskSchema.parse({
  id: NEW_USER_TASK_ID,
  title: '新人7天保底6元',
  description: '完成首次看剧任务即可领取金币奖励',
  reward_coins: 600,
  status: 'available',
  action: {
    type: 'play',
    video_id: 'drama-001-episode-01',
  },
});

const BASE_CASH_TASKS: EarnTask[] = [
  EarnTaskSchema.parse({
    id: REPRESENTATIVE_TASK_ID,
    title: '看剧领现金',
    description: '完整观看指定短剧可获得金币',
    reward_coins: 500,
    status: 'available',
    action: {
      type: 'play',
      video_id: 'drama-001-episode-01',
    },
    is_representative: true,
  }),
  EarnTaskSchema.parse({
    id: LOCKED_TASK_ID,
    title: '周末福利任务',
    description: '活动即将开启，敬请期待',
    reward_coins: 300,
    status: 'locked',
    action: {
      type: 'placeholder',
      feedback: '活动暂未开放',
    },
  }),
];

const BASE_DAILY_REWARDS: EarnDailyReward[] = [
  { day: 1, coins: 10, status: 'claimable' },
  { day: 2, coins: 20, status: 'locked' },
  { day: 3, coins: 30, status: 'locked' },
  { day: 4, coins: 40, status: 'locked' },
  { day: 5, coins: 50, status: 'locked' },
  { day: 6, coins: 60, status: 'locked' },
  { day: 7, coins: 70, status: 'locked' },
].map((reward) => EarnDailyRewardSchema.parse(reward));

function cloneTask(task: EarnTask): EarnTask {
  return {
    ...task,
    action: { ...task.action },
  };
}

function cloneReward(reward: EarnDailyReward): EarnDailyReward {
  return {
    ...reward,
  };
}

function cloneOverview(overview: EarnOverviewResponse): EarnOverviewResponse {
  return {
    ...overview,
    new_user_task: cloneTask(overview.new_user_task),
    daily_rewards: overview.daily_rewards.map(cloneReward),
    cash_tasks: overview.cash_tasks.map(cloneTask),
  };
}

export class EarnMockRepository implements EarnRepositoryInterface {
  private readonly newUserTask: EarnTask;
  private readonly dailyRewards: EarnDailyReward[];
  private readonly cashTasks: EarnTask[];
  private readonly userCompletedTaskIds = new Map<string, Set<string>>();

  constructor(input?: {
    newUserTask?: EarnTask;
    dailyRewards?: EarnDailyReward[];
    cashTasks?: EarnTask[];
  }) {
    this.newUserTask = cloneTask(input?.newUserTask ?? BASE_NEW_USER_TASK);
    this.dailyRewards = (input?.dailyRewards ?? BASE_DAILY_REWARDS).map(cloneReward);
    this.cashTasks = (input?.cashTasks ?? BASE_CASH_TASKS).map(cloneTask);
  }

  async getOverview({ userId }: GetEarnOverviewParams): Promise<EarnOverviewResponse> {
    const completedTaskIds = userId ? this.userCompletedTaskIds.get(userId) ?? new Set<string>() : new Set<string>();
    const isLoggedIn = Boolean(userId);
    const completedCoins = this.calculateCompletedCoins(completedTaskIds);

    const overview = EarnOverviewResponseSchema.parse({
      coins: isLoggedIn ? LOGGED_IN_BASE_COINS + completedCoins : 0,
      is_logged_in: isLoggedIn,
      new_user_task: this.newUserTask,
      daily_rewards: this.dailyRewards,
      cash_tasks: this.cashTasks.map((task) => {
        if (!userId) {
          return task;
        }

        if (completedTaskIds.has(task.id)) {
          return {
            ...task,
            status: 'completed' as const,
          };
        }

        return task;
      }),
    });

    return cloneOverview(overview);
  }

  async completeTask({ userId, taskId }: CompleteEarnTaskParams): Promise<CompleteEarnTaskResponse> {
    const task = this.cashTasks.find((item) => item.id === taskId) ?? (this.newUserTask.id === taskId ? this.newUserTask : null);

    if (!task) {
      throw Errors.notFound('Earn task', taskId);
    }

    if (!task.is_representative) {
      throw Errors.conflict('Earn task cannot be completed');
    }

    const completedTaskIds = this.getOrCreateCompletedTaskIds(userId);
    const alreadyCompleted = completedTaskIds.has(taskId);
    if (!alreadyCompleted) {
      completedTaskIds.add(taskId);
    }

    const totalCoins = LOGGED_IN_BASE_COINS + this.calculateCompletedCoins(completedTaskIds);

    return CompleteEarnTaskResponseSchema.parse({
      success: true,
      task_id: task.id,
      coins_earned: alreadyCompleted ? 0 : task.reward_coins,
      total_coins: totalCoins,
      task_status: 'completed',
    });
  }

  private getOrCreateCompletedTaskIds(userId: string): Set<string> {
    const existing = this.userCompletedTaskIds.get(userId);
    if (existing) {
      return existing;
    }

    const created = new Set<string>();
    this.userCompletedTaskIds.set(userId, created);
    return created;
  }

  private calculateCompletedCoins(taskIds: Set<string>): number {
    let coins = 0;

    for (const taskId of taskIds) {
      const task = this.cashTasks.find((item) => item.id === taskId) ?? (this.newUserTask.id === taskId ? this.newUserTask : null);
      if (task?.is_representative) {
        coins += task.reward_coins;
      }
    }

    return coins;
  }
}

export const EARN_MOCK_IDS = {
  newUserTaskId: NEW_USER_TASK_ID,
  representativeTaskId: REPRESENTATIVE_TASK_ID,
  lockedTaskId: LOCKED_TASK_ID,
} as const;
