import { NextRequest, NextResponse } from 'next/server';
import { PlayerProgressQuerySchema } from '@/lib/schemas';
import { withErrorHandler } from '@/middleware/error-handler';
import { Errors } from '@/lib/errors';
import { PlayerService } from '@/services/player/player.service';
import { parsePlaybackSessionId } from '@/app/api/player/parse-playback-session-id';
import {
  getDramaRepository,
  getEpisodeRepository,
  getPlaybackHistoryRepository,
} from '@/repositories/repository-registry';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const query = PlayerProgressQuerySchema.safeParse({
    dramaId: searchParams.get('dramaId') ?? undefined,
  });

  if (!query.success) {
    throw Errors.invalidParams('Invalid dramaId', query.error.flatten());
  }

  const playbackSessionId = parsePlaybackSessionId(request);
  const service = new PlayerService(
    getDramaRepository(),
    getEpisodeRepository(),
    getPlaybackHistoryRepository(),
  );

  const result = await service.getPlaybackProgress(playbackSessionId, query.data.dramaId);
  return NextResponse.json(result);
});
