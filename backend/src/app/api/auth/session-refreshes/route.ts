import { NextRequest } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { RefreshAuthSessionRequestSchema } from '@/lib/schemas';
import { AuthService } from '@/services/auth/auth.service';
import { mapAuthSessionPayload, success } from '../_helpers';

export const POST = withErrorHandler(async (request: NextRequest) => {
  const body = await request.json();
  const input = RefreshAuthSessionRequestSchema.parse(body);
  const service = new AuthService();
  const result = await service.refreshSession(input);

  return success(mapAuthSessionPayload(result));
});
