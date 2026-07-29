import {
  CompleteEarnTaskResponse,
  EarnOverviewResponse,
} from '@/lib/schemas';

export interface GetEarnOverviewParams {
  userId?: string;
}

export interface CompleteEarnTaskParams {
  userId: string;
  taskId: string;
}

export interface EarnRepositoryInterface {
  getOverview(params: GetEarnOverviewParams): Promise<EarnOverviewResponse>;
  completeTask(params: CompleteEarnTaskParams): Promise<CompleteEarnTaskResponse>;
}
