import { Episode } from '@/lib/schemas';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';
import { getSupabaseClient } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';

export class EpisodeSupabaseRepository implements EpisodeRepositoryInterface {
  async findByDramaId(dramaId: string): Promise<Episode[]> {
    const supabase = getSupabaseClient();
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
    const supabase = getSupabaseClient();
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
}
