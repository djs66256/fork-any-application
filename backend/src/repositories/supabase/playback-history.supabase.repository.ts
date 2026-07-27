import { PlaybackHistory, PlaybackHistorySchema } from '@/lib/schemas';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';
import {
  PlaybackHistoryRepositoryInterface,
  UpsertPlaybackHistoryInput,
} from '@/repositories/interfaces/playback-history.repository.interface';

const PLAYBACK_HISTORY_SELECT_COLUMNS = 'playback_session_id,drama_id,episode_id,progress,duration,updated_at';

function mapRowToPlaybackHistory(row: unknown): PlaybackHistory {
  const parsed = PlaybackHistorySchema.safeParse(row);
  if (!parsed.success) {
    throw Errors.internal('Invalid playback history row returned from Supabase');
  }

  return parsed.data;
}

export class PlaybackHistorySupabaseRepository implements PlaybackHistoryRepositoryInterface {
  async findLatest(playbackSessionId: string, dramaId: string): Promise<PlaybackHistory | null> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('playback_history')
      .select(PLAYBACK_HISTORY_SELECT_COLUMNS)
      .eq('playback_session_id', playbackSessionId)
      .eq('drama_id', dramaId)
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }

      throw Errors.internal(`Failed to fetch playback history: ${error.message}`);
    }

    return mapRowToPlaybackHistory(data);
  }

  async listRecentBySession(playbackSessionId: string, limit: number): Promise<PlaybackHistory[]> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('playback_history')
      .select(PLAYBACK_HISTORY_SELECT_COLUMNS)
      .eq('playback_session_id', playbackSessionId)
      .order('updated_at', { ascending: false })
      .limit(limit);

    if (error) {
      const message = error.message.toLowerCase();
      const code = typeof error.code === 'string' ? error.code.toLowerCase() : '';
      const isAvailabilityError = message.includes('failed to fetch')
        || message.includes('network')
        || message.includes('timeout')
        || message.includes('unavailable')
        || message.includes('connection')
        || code === '08000'
        || code === '08003'
        || code === '08006'
        || code === '57p01';

      if (isAvailabilityError) {
        throw Errors.serviceUnavailable('playback_history');
      }

      throw Errors.internal(`Failed to fetch recent playback history: ${error.message}`);
    }

    return (data ?? []).map(mapRowToPlaybackHistory);
  }

  async upsert(input: UpsertPlaybackHistoryInput): Promise<PlaybackHistory> {
    const payload = {
      ...input,
      updated_at: input.updated_at ?? new Date().toISOString(),
    };

    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('playback_history')
      .upsert(payload, { onConflict: 'playback_session_id,drama_id' })
      .select(PLAYBACK_HISTORY_SELECT_COLUMNS)
      .single();

    if (error) {
      throw Errors.internal(`Failed to upsert playback history: ${error.message}`);
    }

    return mapRowToPlaybackHistory(data);
  }
}
