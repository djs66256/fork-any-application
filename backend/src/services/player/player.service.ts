import {
  PlayerProgressResponse,
  PlayerProgressResponseSchema,
  PlayerStartResponse,
  PlayerStartResponseSchema,
  PlayerStopResponse,
  PlayerStopResponseSchema,
} from '@/lib/schemas';
import { Errors } from '@/lib/errors';
import { DramaRepositoryInterface } from '@/repositories/interfaces/drama.repository.interface';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';
import { PlaybackHistoryRepositoryInterface } from '@/repositories/interfaces/playback-history.repository.interface';

function clampProgress(progress: number, duration: number): number {
  return Math.min(Math.max(progress, 0), duration);
}

export class PlayerService {
  constructor(
    private readonly dramaRepository: DramaRepositoryInterface,
    private readonly episodeRepository: EpisodeRepositoryInterface,
    private readonly playbackHistoryRepository: PlaybackHistoryRepositoryInterface,
  ) {}

  async getPlaybackProgress(
    playbackSessionId: string,
    dramaId: string,
  ): Promise<PlayerProgressResponse> {
    await this.ensureDramaExists(dramaId);

    const history = await this.playbackHistoryRepository.findLatest(playbackSessionId, dramaId);
    if (!history) {
      return PlayerProgressResponseSchema.parse({
        code: 0,
        data: {
          drama_id: dramaId,
          has_history: false,
          episode_id: null,
          start_time: 0,
          updated_at: null,
        },
        message: 'ok',
      });
    }

    const episode = await this.episodeRepository.findById(history.episode_id);
    if (!episode || episode.drama_id !== dramaId) {
      return PlayerProgressResponseSchema.parse({
        code: 0,
        data: {
          drama_id: dramaId,
          has_history: false,
          episode_id: null,
          start_time: 0,
          updated_at: null,
        },
        message: 'ok',
      });
    }

    return PlayerProgressResponseSchema.parse({
      code: 0,
      data: {
        drama_id: dramaId,
        has_history: true,
        episode_id: history.episode_id,
        start_time: history.progress,
        updated_at: history.updated_at,
      },
      message: 'ok',
    });
  }

  async startPlayback(
    playbackSessionId: string,
    dramaId: string,
    episodeId: string,
    progress: number,
  ): Promise<PlayerStartResponse> {
    await this.ensureDramaExists(dramaId);
    const episode = await this.ensureEpisodeBelongsToDrama(dramaId, episodeId);
    this.ensureEpisodePlayable(episode.id, episode.video_url);

    return PlayerStartResponseSchema.parse({
      code: 0,
      data: {
        drama_id: dramaId,
        episode_id: episodeId,
        accepted_progress: Math.max(progress, 0),
        playback_session_id: playbackSessionId,
        started_at: new Date().toISOString(),
      },
      message: 'ok',
    });
  }

  async stopPlayback(
    playbackSessionId: string,
    dramaId: string,
    episodeId: string,
    progress: number,
    duration: number,
  ): Promise<PlayerStopResponse> {
    await this.ensureDramaExists(dramaId);
    await this.ensureEpisodeBelongsToDrama(dramaId, episodeId);

    const savedProgress = clampProgress(progress, duration);
    const history = await this.playbackHistoryRepository.upsert({
      playback_session_id: playbackSessionId,
      drama_id: dramaId,
      episode_id: episodeId,
      progress: savedProgress,
      duration,
    });

    return PlayerStopResponseSchema.parse({
      code: 0,
      data: {
        drama_id: dramaId,
        episode_id: episodeId,
        saved_progress: history.progress,
        duration,
        updated_at: history.updated_at,
      },
      message: 'ok',
    });
  }

  private async ensureDramaExists(dramaId: string): Promise<void> {
    const drama = await this.dramaRepository.findById(dramaId);
    if (!drama) {
      throw Errors.dramaNotFound(dramaId);
    }
  }

  private async ensureEpisodeBelongsToDrama(dramaId: string, episodeId: string) {
    const episode = await this.episodeRepository.findById(episodeId);
    if (!episode || episode.drama_id !== dramaId) {
      throw Errors.episodeNotFound(episodeId);
    }

    return episode;
  }

  private ensureEpisodePlayable(episodeId: string, videoUrl: string | null | undefined): void {
    if (!videoUrl) {
      throw Errors.episodeNotPlayable(episodeId);
    }
  }
}
