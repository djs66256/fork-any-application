import { Episode } from '@/lib/schemas';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';

export class EpisodeSupabaseRepository implements EpisodeRepositoryInterface {
  async findByDramaId(dramaId: string): Promise<Episode[]> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('episodes')
      .select('*')
      .eq('drama_id', dramaId)
      .order('episode_number', { ascending: true });

    if (error) {
      throw Errors.internal(`Failed to fetch episodes: ${error.message}`);
    }

    return (data ?? []) as Episode[];
  }

  async findById(id: string): Promise<Episode | null> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('episodes')
      .select('*')
      .eq('id', id)
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }
      throw Errors.internal(`Failed to fetch episode: ${error.message}`);
    }

    return data as Episode;
  }

  async create(data: Omit<Episode, 'id' | 'created_at' | 'updated_at'>): Promise<Episode> {
    const supabase = getSupabaseAdmin();
    const { data: created, error } = await supabase
      .from('episodes')
      .insert(data)
      .select('*')
      .single();

    if (error) {
      if (error.code === '23505') {
        throw Errors.conflict('剧集号已存在');
      }
      throw Errors.internal(`Failed to create episode: ${error.message}`);
    }

    return created as Episode;
  }

  async update(
    id: string,
    data: Partial<Omit<Episode, 'id' | 'created_at' | 'updated_at'>>,
  ): Promise<Episode | null> {
    const supabase = getSupabaseAdmin();
    const { data: updated, error } = await supabase
      .from('episodes')
      .update(data)
      .eq('id', id)
      .select('*')
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }
      if (error.code === '23505') {
        throw Errors.conflict('剧集号已存在');
      }
      throw Errors.internal(`Failed to update episode: ${error.message}`);
    }

    return updated as Episode;
  }

  async delete(id: string): Promise<boolean> {
    const supabase = getSupabaseAdmin();
    const { error } = await supabase
      .from('episodes')
      .delete()
      .eq('id', id);

    if (error) {
      throw Errors.internal(`Failed to delete episode: ${error.message}`);
    }

    return true;
  }

  async count(): Promise<number> {
    const supabase = getSupabaseAdmin();
    const { count, error } = await supabase
      .from('episodes')
      .select('*', { count: 'exact', head: true });

    if (error) {
      throw Errors.internal(`Failed to count episodes: ${error.message}`);
    }

    return count ?? 0;
  }

  async countByDramaId(dramaId: string): Promise<number> {
    const supabase = getSupabaseAdmin();
    const { count, error } = await supabase
      .from('episodes')
      .select('*', { count: 'exact', head: true })
      .eq('drama_id', dramaId);

    if (error) {
      throw Errors.internal(`Failed to count episodes for drama: ${error.message}`);
    }

    return count ?? 0;
  }
}