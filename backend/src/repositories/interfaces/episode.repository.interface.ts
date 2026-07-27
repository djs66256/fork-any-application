import { Episode } from '@/lib/schemas';

export interface EpisodeRepositoryInterface {
  findByDramaId(dramaId: string): Promise<Episode[]>;
  findById(id: string): Promise<Episode | null>;
  create(data: Omit<Episode, 'id' | 'created_at' | 'updated_at'>): Promise<Episode>;
  update(id: string, data: Partial<Omit<Episode, 'id' | 'created_at' | 'updated_at'>>): Promise<Episode | null>;
  delete(id: string): Promise<boolean>;
  count(): Promise<number>;
  countByDramaId(dramaId: string): Promise<number>;
}