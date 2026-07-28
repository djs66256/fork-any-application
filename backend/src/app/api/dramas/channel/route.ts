import { NextRequest, NextResponse } from 'next/server';
import { TheaterFeedQuerySchema } from '@/lib/schemas';
import { DramaService } from '@/services/drama/drama.service';
import { getDramaRepository } from '@/repositories/repository-registry';
import { withErrorHandler } from '@/middleware/error-handler';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const query = TheaterFeedQuerySchema.parse({
    channel: searchParams.get('channel') ?? undefined,
    page: searchParams.get('page') ?? undefined,
    pageSize: searchParams.get('pageSize') ?? undefined,
  });

  const service = new DramaService(getDramaRepository());
  const result = await service.listTheaterFeed(query);

  return NextResponse.json(result);
});
