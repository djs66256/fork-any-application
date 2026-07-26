import { PlaybackHistory, PlaybackHistorySchema } from '@/lib/schemas';
import {
  PlaybackHistoryRepositoryInterface,
  UpsertPlaybackHistoryInput,
} from '@/repositories/interfaces/playback-history.repository.interface';

function clonePlaybackHistory(record: PlaybackHistory): PlaybackHistory {
  return {
    ...record,
    duration: record.duration ?? null,
  };
}

export class PlaybackHistoryMockRepository implements PlaybackHistoryRepositoryInterface {
  private data: Map<string, PlaybackHistory>;

  constructor(initialData: PlaybackHistory[] = []) {
    this.data = new Map(
      initialData.map((item) => [
        this.getKey(item.playback_session_id, item.drama_id),
        clonePlaybackHistory(PlaybackHistorySchema.parse(item)),
      ]),
    );
  }

  async findLatest(playbackSessionId: string, dramaId: string): Promise<PlaybackHistory | null> {
    const item = this.data.get(this.getKey(playbackSessionId, dramaId));
    return item ? clonePlaybackHistory(item) : null;
  }

  async upsert(input: UpsertPlaybackHistoryInput): Promise<PlaybackHistory> {
    const record = PlaybackHistorySchema.parse({
      ...input,
      duration: input.duration,
      updated_at: input.updated_at ?? new Date().toISOString(),
    });

    this.data.set(this.getKey(record.playback_session_id, record.drama_id), record);
    return clonePlaybackHistory(record);
  }

  clear(): void {
    this.data.clear();
  }

  private getKey(playbackSessionId: string, dramaId: string): string {
    return `${playbackSessionId}:${dramaId}`;
  }
}
