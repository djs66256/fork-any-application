import { NextResponse } from 'next/server';
import { HealthService } from '@/services/health/health.service';
import { withErrorHandler } from '@/middleware/error-handler';

export const GET = withErrorHandler(async () => {
  const service = new HealthService();
  const data = await service.check();
  return NextResponse.json(data);
});
