import { Episode, EpisodeListResponse, EpisodeListResponseSchema } from '@/lib/schemas';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';
import { DramaRepositoryInterface } from '@/repositories/interfaces/drama.repository.interface';
import { Errors } from '@/lib/errors';

function toSeriesStatus(episodeCount: number): 'completed' | 'ongoing' {
  return episodeCount > 0 ? 'completed' : 'ongoing';
}

export class EpisodeService {
  constructor(
    private readonly episodeRepository: EpisodeRepositoryInterface,
    private readonly dramaRepository?: DramaRepositoryInterface,
  ) {}

  async listEpisodesByDramaId(dramaId: string): Promise<EpisodeListResponse> {
    const drama = this.dramaRepository ? await this.dramaRepository.findById(dramaId) : null;
    if (this.dramaRepository && !drama) {
      throw Errors.dramaNotFound(dramaId);
    }

    const items = (await this.episodeRepository.findByDramaId(dramaId))
      .slice()
      .sort((left, right) => left.episode_number - right.episode_number);

    return EpisodeListResponseSchema.parse({
      code: 0,
      data: {
        drama_id: dramaId,
        series_status: toSeriesStatus(drama?.episode_count ?? items.length),
        items,
      },
      message: 'ok',
    });
  }

  async getEpisodeById(id: string): Promise<Episode> {
    const episode = await this.episodeRepository.findById(id);
    if (!episode) {
      throw Errors.episodeNotFound(id);
    }

    return episode;
  }
}
