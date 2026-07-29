import { api } from '@/lib/api-client';
import {
  CompleteEarnTaskRequestSchema,
  CompleteEarnTaskResponseSchema,
  EarnOverviewResponseSchema,
  type CompleteEarnTaskResponse,
  type EarnOverviewResponse,
} from '@/lib/schemas';

export async function getEarnOverview(): Promise<EarnOverviewResponse> {
  const response = await api.get('/api/earn/overview');
  return EarnOverviewResponseSchema.parse(response);
}

export async function completeEarnTask(
  taskId: string,
  accessToken: string,
): Promise<CompleteEarnTaskResponse> {
  const normalizedRequest = CompleteEarnTaskRequestSchema.parse({
    task_id: taskId,
  });

  const response = await api.post('/api/earn/complete-task', normalizedRequest, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  return CompleteEarnTaskResponseSchema.parse(response);
}
