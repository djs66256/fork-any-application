import { NextRequest } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { SendOtpRequestSchema } from '@/lib/schemas';
import { AuthService } from '@/services/auth/auth.service';
import { success } from '../_helpers';

export const POST = withErrorHandler(async (request: NextRequest) => {
  const body = await request.json();
  const input = SendOtpRequestSchema.parse(body);
  const service = new AuthService();
  const result = await service.sendOtp(input);

  return success(result);
});
