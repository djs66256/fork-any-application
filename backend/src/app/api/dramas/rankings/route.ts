import { NextRequest, NextResponse } from 'next/server';
import { RankingQuerySchema } from '@/lib/schemas';
import { DramaService } from '@/services/drama/drama.service';
import { DramaSupabaseRepository } from '@/repositories/supabase/drama.supabase.repository';
import { withErrorHandler } from '@/middleware/error-handler';
import { resolveOptionalAuthContext } from '@/middleware/auth';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const query = RankingQuerySchema.parse({
    type: searchParams.get('type') ?? undefined,
    contentType: searchParams.get('contentType') ?? undefined,
    page: searchParams.get('page') ?? undefined,
    pageSize: searchParams.get('pageSize') ?? undefined,
  });

  const service = new DramaService(new DramaSupabaseRepository());
  const authContext = await resolveOptionalAuthContext(request) ?? undefined;
  const result = await service.listRankings(query, authContext ? { userId: authContext.userId } : undefined);

  return NextResponse.json(result);
});
