import { Episode } from '@/lib/schemas';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';
import { Errors } from '@/lib/errors';

export class EpisodeService {
  constructor(private episodeRepository: EpisodeRepositoryInterface) {}

  async getEpisodeById(_id: string): Promise<Episode> {
    throw Errors.notImplemented('getEpisodeById not implemented');
  }
}
