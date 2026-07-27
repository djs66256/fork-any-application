import { NextRequest, NextResponse } from 'next/server';
import { PlayerStopRequestSchema } from '@/lib/schemas';
import { withErrorHandler } from '@/middleware/error-handler';
import { Errors } from '@/lib/errors';
import { PlayerService } from '@/services/player/player.service';
import { parsePlaybackSessionId } from '@/app/api/player/parse-playback-session-id';
import {
  getDramaRepository,
  getEpisodeRepository,
  getPlaybackHistoryRepository,
} from '@/repositories/repository-registry';

export const POST = withErrorHandler(async (request: NextRequest) => {
  const payload = PlayerStopRequestSchema.safeParse(await request.json());
  if (!payload.success) {
    throw Errors.invalidParams('Invalid player stop request', payload.error.flatten());
  }

  const playbackSessionId = parsePlaybackSessionId(request);
  const service = new PlayerService(
    getDramaRepository(),
    getEpisodeRepository(),
    getPlaybackHistoryRepository(),
  );

  const result = await service.stopPlayback(
    playbackSessionId,
    payload.data.drama_id,
    payload.data.episode_id,
    payload.data.progress,
    payload.data.duration,
  );

  return NextResponse.json(result);
});
