import { NextRequest, NextResponse } from 'next/server';
import { resolveRequiredAuthContext } from '@/middleware/auth';
import { withErrorHandler } from '@/middleware/error-handler';
import { CompleteEarnTaskRequestSchema } from '@/lib/schemas';
import { getEarnRepository } from '@/repositories/repository-registry';
import { EarnService } from '@/services/earn/earn.service';

export const POST = withErrorHandler(async (request: NextRequest) => {
  const body = await request.json();
  const input = CompleteEarnTaskRequestSchema.parse(body);
  const auth = await resolveRequiredAuthContext(request);
  const service = new EarnService(getEarnRepository());
  const result = await service.completeTask({ auth, taskId: input.task_id });

  return NextResponse.json(result);
});
