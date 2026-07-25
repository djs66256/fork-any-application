import { Episode } from '@/lib/schemas';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';
import { Errors } from '@/lib/errors';

export class EpisodeService {
  constructor(private episodeRepository: EpisodeRepositoryInterface) {}

  async getEpisodeById(id: string): Promise<Episode> {
    void id;
    throw Errors.notImplemented('getEpisodeById not implemented');
  }
}
