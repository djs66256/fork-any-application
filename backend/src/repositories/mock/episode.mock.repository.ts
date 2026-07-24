import { Episode } from '@/lib/schemas';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';

export class EpisodeMockRepository implements EpisodeRepositoryInterface {
  private data: Map<string, Episode> = new Map();

  async findByDramaId(dramaId: string): Promise<Episode[]> {
    return Array.from(this.data.values()).filter((ep) => ep.drama_id === dramaId);
  }

  async findById(id: string): Promise<Episode | null> {
    return this.data.get(id) ?? null;
  }

  // Helper for tests: add seed data
  addSeed(episode: Episode): void {
    this.data.set(episode.id, episode);
  }

  clear(): void {
    this.data.clear();
  }
}
