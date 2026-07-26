import { PlaybackHistory } from '@/lib/schemas';

export interface UpsertPlaybackHistoryInput {
  playback_session_id: string;
  drama_id: string;
  episode_id: string;
  progress: number;
  duration: number | null;
  updated_at?: string;
}

export interface PlaybackHistoryRepositoryInterface {
  findLatest(playbackSessionId: string, dramaId: string): Promise<PlaybackHistory | null>;
  upsert(input: UpsertPlaybackHistoryInput): Promise<PlaybackHistory>;
}
