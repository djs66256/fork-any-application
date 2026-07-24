import { withErrorHandler } from '@/middleware/error-handler';
import { Errors } from '@/lib/errors';

export const GET = withErrorHandler(async () => {
  throw Errors.notImplemented('GET /api/dramas/[id] not implemented');
});
