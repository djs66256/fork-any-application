import { NextRequest } from 'next/server';
import { PlaybackSessionIdHeaderSchema } from '@/lib/schemas';
import { Errors } from '@/lib/errors';

export function parsePlaybackSessionId(request: NextRequest): string {
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
