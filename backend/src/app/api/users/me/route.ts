import { NextRequest } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { AuthService } from '@/services/auth/auth.service';
import { getAuth, requireAuthContext } from '@/middleware/auth';
import { mapAuthUserPayload, success } from '@/app/api/auth/_helpers';

export const GET = withErrorHandler(requireAuthContext(async (request: NextRequest) => {
  const service = new AuthService();
  const auth = getAuth(request);
  const result = await service.getCurrentUser(auth.userId);

  return success(mapAuthUserPayload(result));
}));
