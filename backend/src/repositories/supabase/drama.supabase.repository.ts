import { Drama } from '@/lib/schemas';
import { DramaRepositoryInterface, PaginationParams, PaginatedResult } from '@/repositories/interfaces/drama.repository.interface';
import { getSupabaseClient } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';

export class DramaSupabaseRepository implements DramaRepositoryInterface {
  async findMany(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    const supabase = getSupabaseClient();
    const from = (params.page - 1) * params.pageSize;
    const to = from + params.pageSize - 1;

    const { data, error, count } = await supabase
      .from('dramas')
      .select('*', { count: 'exact', head: false })
      .range(from, to)
      .order('created_at', { ascending: false });

    if (error) {
      throw Errors.internal(`Failed to fetch dramas: ${error.message}`);
    }

    const total = count ?? 0;
    const totalPages = Math.ceil(total / params.pageSize);

    return {
      data: (data ?? []) as Drama[],
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: totalPages,
      },
    };
  }

  async findById(id: string): Promise<Drama | null> {
    const supabase = getSupabaseClient();
    const { data, error } = await supabase
      .from('dramas')
      .select('*')
      .eq('id', id)
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }
      throw Errors.internal(`Failed to fetch drama: ${error.message}`);
    }

    return data as Drama;
  }

  async create(data: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama> {
    const supabase = getSupabaseClient();
    const { data: created, error } = await supabase
      .from('dramas')
      .insert(data)
      .select()
      .single();

    if (error) {
      if (error.code === '23505') {
        throw Errors.conflict('Drama already exists');
      }
      throw Errors.internal(`Failed to create drama: ${error.message}`);
    }

    return created as Drama;
  }

  async update(
    id: string,
    data: Partial<Omit<Drama, 'id' | 'created_at' | 'updated_at'>>,
  ): Promise<Drama | null> {
    const supabase = getSupabaseClient();
    const { data: updated, error } = await supabase
      .from('dramas')
      .update(data)
      .eq('id', id)
      .select()
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }
      throw Errors.internal(`Failed to update drama: ${error.message}`);
    }

    return updated as Drama;
  }

  async delete(id: string): Promise<boolean> {
    const supabase = getSupabaseClient();
    const { error } = await supabase
      .from('dramas')
      .delete()
      .eq('id', id);

    if (error) {
      throw Errors.internal(`Failed to delete drama: ${error.message}`);
    }

    return true;
  }
}
