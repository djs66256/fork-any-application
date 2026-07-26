import { NextRequest, NextResponse } from 'next/server';
import { DramaIdPathSchema } from '@/lib/schemas';
import { withErrorHandler } from '@/middleware/error-handler';
import { EpisodeService } from '@/services/episode/episode.service';
import { getDramaRepository, getEpisodeRepository } from '@/repositories/repository-registry';

type RouteContext = {
  params: Promise<{
    id: string;
  }>;
};

export const GET = withErrorHandler(async (_request: NextRequest, context: unknown) => {
  const { id } = DramaIdPathSchema.parse(await (context as RouteContext).params);

  const service = new EpisodeService(getEpisodeRepository(), getDramaRepository());
  const result = await service.listEpisodesByDramaId(id);

  return NextResponse.json(result);
});
