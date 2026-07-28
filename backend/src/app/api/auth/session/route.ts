import { NextRequest } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { AuthService } from '@/services/auth/auth.service';
import { extractAccessToken, success } from '../_helpers';

export const DELETE = withErrorHandler(async (request: NextRequest) => {
  const service = new AuthService();
  const accessToken = extractAccessToken(request.headers.get('Authorization'));

  await service.logout(accessToken);

  return success(null);
});
