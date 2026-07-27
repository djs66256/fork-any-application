import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { DramaService } from '@/services/drama/drama.service';
import { withErrorHandler } from '@/middleware/error-handler';
import { getAuthenticatedUserId } from '@/middleware/auth';

const BookDramaParamsSchema = z.object({
  id: z.string().uuid(),
});

type BookDramaRouteContext = {
  params: Promise<{ id: string }> | { id: string };
};

export const POST = withErrorHandler(async (request: NextRequest, context: unknown) => {
  const { id } = BookDramaParamsSchema.parse(await Promise.resolve((context as BookDramaRouteContext).params));
  const userId = getAuthenticatedUserId(request);

  const repository = new DramaMockRepository();
  const service = new DramaService(repository);
  const result = await service.bookDrama({
    dramaId: id,
    userId,
  });

  return NextResponse.json(result);
});
