import { Episode } from '@/lib/schemas';

export interface EpisodeRepositoryInterface {
  findByDramaId(dramaId: string): Promise<Episode[]>;
  findById(id: string): Promise<Episode | null>;
}
