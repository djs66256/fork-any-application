import { AppError, Errors } from '@/lib/errors';
import {
  CompleteEarnTaskResponse,
  CompleteEarnTaskResponseSchema,
  EarnOverviewResponse,
  EarnOverviewResponseSchema,
} from '@/lib/schemas';
import { AuthContext } from '@/middleware/auth';
import { EarnRepositoryInterface } from '@/repositories/interfaces/earn.repository.interface';

function isAppError(error: unknown): error is AppError {
  return error instanceof AppError;
}

export class EarnService {
  constructor(private readonly earnRepository: EarnRepositoryInterface) {}

  async getOverview(params: { auth?: AuthContext | null }): Promise<EarnOverviewResponse> {
    try {
      return EarnOverviewResponseSchema.parse(
        await this.earnRepository.getOverview({ userId: params.auth?.userId }),
      );
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }

      throw Errors.internal('Invalid earn overview result');
    }
  }

  async completeTask(params: { auth: AuthContext; taskId: string }): Promise<CompleteEarnTaskResponse> {
    try {
      return CompleteEarnTaskResponseSchema.parse(
        await this.earnRepository.completeTask({
          userId: params.auth.userId,
          taskId: params.taskId,
        }),
      );
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }

      throw Errors.internal('Invalid earn complete-task result');
    }
  }
}
