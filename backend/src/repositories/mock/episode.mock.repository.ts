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

  async create(data: Omit<Episode, 'id' | 'created_at' | 'updated_at'>): Promise<Episode> {
    const now = new Date().toISOString();
    const id = crypto.randomUUID();
    const episode: Episode = {
      ...data,
      id,
      created_at: now,
      updated_at: now,
    } as Episode;
    this.data.set(id, episode);
    return episode;
  }

  async update(
    id: string,
    data: Partial<Omit<Episode, 'id' | 'created_at' | 'updated_at'>>,
  ): Promise<Episode | null> {
    const existing = this.data.get(id);
    if (!existing) return null;

    const updated: Episode = {
      ...existing,
      ...data,
      updated_at: new Date().toISOString(),
    } as Episode;
    this.data.set(id, updated);
    return updated;
  }

  async delete(id: string): Promise<boolean> {
    return this.data.delete(id);
  }

  async count(): Promise<number> {
    return this.data.size;
  }

  async countByDramaId(dramaId: string): Promise<number> {
    return Array.from(this.data.values()).filter((ep) => ep.drama_id === dramaId).length;
  }

  // Helper for tests: add seed data
  addSeed(episode: Episode): void {
    this.data.set(episode.id, episode);
  }

  clear(): void {
    this.data.clear();
  }
}