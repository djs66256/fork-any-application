import { NextRequest, NextResponse } from 'next/server';
import { resolveOptionalAuthContext } from '@/middleware/auth';
import { withErrorHandler } from '@/middleware/error-handler';
import { getEarnRepository } from '@/repositories/repository-registry';
import { EarnService } from '@/services/earn/earn.service';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const auth = await resolveOptionalAuthContext(request);
  const service = new EarnService(getEarnRepository());
  const result = await service.getOverview({ auth });

  return NextResponse.json(result);
});
