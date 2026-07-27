import { NextRequest, NextResponse } from 'next/server';
import { RankingQuerySchema } from '@/lib/schemas';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { DramaService } from '@/services/drama/drama.service';
import { withErrorHandler } from '@/middleware/error-handler';
import { getOptionalUserId } from '@/middleware/auth';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const query = RankingQuerySchema.parse({
    type: searchParams.get('type') ?? undefined,
    contentType: searchParams.get('contentType') ?? undefined,
    page: searchParams.get('page') ?? undefined,
    pageSize: searchParams.get('pageSize') ?? undefined,
  });

  const repository = new DramaMockRepository();
  const service = new DramaService(repository);
  const userId = getOptionalUserId(request);
  const authContext = userId ? { userId } : undefined;
  const result = await service.listRankings(query, authContext);

  return NextResponse.json(result);
});
