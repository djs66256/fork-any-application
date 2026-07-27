import { NextRequest, NextResponse } from 'next/server';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { SearchDramaQuerySchema } from '@/lib/schemas';
import { DramaService } from '@/services/drama/drama.service';
import { withErrorHandler } from '@/middleware/error-handler';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const query = SearchDramaQuerySchema.parse({
    q: searchParams.get('q') ?? undefined,
    page: searchParams.get('page') ?? undefined,
    pageSize: searchParams.get('pageSize') ?? undefined,
  });

  const repository = new DramaMockRepository();
  const service = new DramaService(repository);
  const result = await service.searchDramas(query);

  return NextResponse.json(result);
});
