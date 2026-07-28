import { NextRequest, NextResponse } from 'next/server';
import { RankingQuerySchema } from '@/lib/schemas';
import { DramaService } from '@/services/drama/drama.service';
import { getDramaRepository } from '@/repositories/repository-registry';
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

  const service = new DramaService(getDramaRepository());
  const userId = getOptionalUserId(request);
  const authContext = userId ? { userId } : undefined;
  const result = await service.listRankings(query, authContext);

  return NextResponse.json(result);
});
