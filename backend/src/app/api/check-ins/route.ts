import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { resolveOptionalAuthContext } from '@/middleware/auth';
import { parseInstallationId } from '@/app/api/check-ins/parse-installation-id';
import { CheckInService } from '@/services/check-in/check-in.service';
import { getCheckInRepository } from '@/repositories/repository-registry';

export const POST = withErrorHandler(async (request: NextRequest) => {
  const auth = await resolveOptionalAuthContext(request);
  const installationId = parseInstallationId(request) ?? undefined;
  const service = new CheckInService(getCheckInRepository());
  const result = await service.checkIn({
    userId: auth?.userId,
    installationId,
  });

  return NextResponse.json(result);
});
