import { z } from 'zod';
import { Drama, DramaSchema } from '@/lib/schemas';
import { DramaRepositoryInterface, PaginationParams, PaginatedResult } from '@/repositories/interfaces/drama.repository.interface';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';

const SupabaseDramaRowSchema = z.object({
  id: z.string().uuid(),
  title: z.string().min(1),
  description: z.string().nullable().optional(),
  cover_url: z.string().url().nullable().optional(),
  category: z.string().nullable().optional(),
  total_episodes: z.number().int().min(0),
  rating: z.number().min(0).max(10).nullable().optional(),
  created_at: z.string(),
  updated_at: z.string(),
});

type SupabaseDramaRow = z.infer<typeof SupabaseDramaRowSchema>;

function mapRowToDrama(row: unknown): Drama {
  const parsed = SupabaseDramaRowSchema.safeParse(row);
  if (!parsed.success) {
    throw Errors.internal('Invalid drama row returned from Supabase');
  }

  const drama = DramaSchema.safeParse({
    id: parsed.data.id,
    title: parsed.data.title,
    description: parsed.data.description ?? '',
    cover_url: parsed.data.cover_url ?? null,
    category: parsed.data.category ?? '',
    episode_count: parsed.data.total_episodes,
    tags: [],
    rating: parsed.data.rating ?? null,
    created_at: parsed.data.created_at,
    updated_at: parsed.data.updated_at,
  });

  if (!drama.success) {
    throw Errors.internal('Failed to map drama row to canonical contract');
  }

  return drama.data;
}

function mapDramaToRow(data: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Omit<SupabaseDramaRow, 'id' | 'created_at' | 'updated_at'> {
  if (data.tags.length > 0) {
    throw Errors.validationError('Supabase drama storage does not support non-empty tags yet');
  }

  return {
    title: data.title,
    description: data.description,
    cover_url: data.cover_url,
    category: data.category,
    total_episodes: data.episode_count,
    rating: data.rating,
  };
}

const DRAMA_SELECT_COLUMNS = 'id,title,description,cover_url,category,total_episodes,rating,created_at,updated_at';

export class DramaSupabaseRepository implements DramaRepositoryInterface {
  async findMany(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    const supabase = getSupabaseAdmin();
    const from = (params.page - 1) * params.pageSize;
    const to = from + params.pageSize - 1;

    const { data, error, count } = await supabase
      .from('dramas')
      .select(DRAMA_SELECT_COLUMNS, { count: 'exact', head: false })
      .range(from, to)
      .order('created_at', { ascending: false });

    if (error) {
      throw Errors.internal(`Failed to fetch dramas: ${error.message}`);
    }

    const total = count ?? 0;
    const totalPages = total === 0 ? 0 : Math.ceil(total / params.pageSize);

    return {
      data: (data ?? []).map(mapRowToDrama),
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: totalPages,
      },
    };
  }

  async findById(id: string): Promise<Drama | null> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('dramas')
      .select(DRAMA_SELECT_COLUMNS)
      .eq('id', id)
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }
      throw Errors.internal(`Failed to fetch drama: ${error.message}`);
    }

    return mapRowToDrama(data);
  }

  async create(data: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama> {
    const row = mapDramaToRow(data);
    const supabase = getSupabaseAdmin();
    const { data: created, error } = await supabase
      .from('dramas')
      .insert(row)
      .select(DRAMA_SELECT_COLUMNS)
      .single();

    if (error) {
      if (error.code === '23505') {
        throw Errors.conflict('Drama already exists');
      }
      throw Errors.internal(`Failed to create drama: ${error.message}`);
    }

    return mapRowToDrama(created);
  }

  async update(
    id: string,
    data: Partial<Omit<Drama, 'id' | 'created_at' | 'updated_at'>>,
  ): Promise<Drama | null> {
    const supabase = getSupabaseAdmin();
    const rowUpdate: Partial<Omit<SupabaseDramaRow, 'id' | 'created_at' | 'updated_at'>> = {};

    if (data.tags !== undefined && data.tags.length > 0) {
      throw Errors.validationError('Supabase drama storage does not support non-empty tags yet');
    }

    if (data.title !== undefined) rowUpdate.title = data.title;
    if (data.description !== undefined) rowUpdate.description = data.description;
    if (data.cover_url !== undefined) rowUpdate.cover_url = data.cover_url;
    if (data.category !== undefined) rowUpdate.category = data.category;
    if (data.episode_count !== undefined) rowUpdate.total_episodes = data.episode_count;
    if (data.rating !== undefined) rowUpdate.rating = data.rating;

    const { data: updated, error } = await supabase
      .from('dramas')
      .update(rowUpdate)
      .eq('id', id)
      .select(DRAMA_SELECT_COLUMNS)
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }
      throw Errors.internal(`Failed to update drama: ${error.message}`);
    }

    return mapRowToDrama(updated);
  }

  async delete(id: string): Promise<boolean> {
    const supabase = getSupabaseAdmin();
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
