import { withErrorHandler } from '@/middleware/error-handler';
import { Errors } from '@/lib/errors';

export const POST = withErrorHandler(async () => {
  throw Errors.notImplemented('POST /api/player/stop not implemented');
});
