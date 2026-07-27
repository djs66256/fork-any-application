import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { PlayerService } from '@/services/player/player.service';
import { parsePlaybackSessionId } from '@/app/api/player/parse-playback-session-id';
import {
  getDramaRepository,
  getEpisodeRepository,
  getPlaybackHistoryRepository,
} from '@/repositories/repository-registry';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const playbackSessionId = parsePlaybackSessionId(request);
  const service = new PlayerService(
    getDramaRepository(),
    getEpisodeRepository(),
    getPlaybackHistoryRepository(),
  );

  const result = await service.getRecentlyViewed(playbackSessionId);
  return NextResponse.json(result);
});
