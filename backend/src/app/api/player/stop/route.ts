import { NextRequest, NextResponse } from 'next/server';
import { PlaybackSessionIdHeaderSchema, PlayerStopRequestSchema } from '@/lib/schemas';
import { withErrorHandler } from '@/middleware/error-handler';
import { Errors } from '@/lib/errors';
import { PlayerService } from '@/services/player/player.service';
import {
  getDramaRepository,
  getEpisodeRepository,
  getPlaybackHistoryRepository,
} from '@/repositories/repository-registry';

function parsePlaybackSessionId(request: NextRequest): string {
  const playbackSessionId = request.headers.get('X-Playback-Session-Id');
  if (!playbackSessionId) {
    throw Errors.invalidPlaybackSession('Missing X-Playback-Session-Id');
  }

  const parsed = PlaybackSessionIdHeaderSchema.safeParse(playbackSessionId);
  if (!parsed.success) {
    throw Errors.invalidPlaybackSession('Invalid X-Playback-Session-Id');
  }

  return parsed.data;
}

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
