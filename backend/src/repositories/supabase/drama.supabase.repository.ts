import { z } from 'zod';
import {
  BookDramaResponse,
  BookDramaResponseSchema,
  Drama,
  DramaSchema,
  HotSearchListResponse,
  HotSearchListResponseSchema,
  RankingDrama,
  RankingDramaSchema,
} from '@/lib/schemas';
import {
  AuthContext,
  BookDramaParams,
  DramaRepositoryInterface,
  PaginatedResult,
  PaginationParams,
  RankingParams,
  SearchDramasParams,
} from '@/repositories/interfaces/drama.repository.interface';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';

const SupabaseDramaRowSchema = z.object({
  id: z.string().uuid(),
  title: z.string().min(1),
  description: z.string().nullable().optional(),
  cover_url: z.string().url().nullable().optional(),
  category: z.string().nullable().optional(),
  episode_count: z.number().int().min(0),
  rating: z.number().min(0).max(10).nullable().optional(),
  created_at: z.string(),
  updated_at: z.string(),
  content_type: z.enum(['live_action', 'ai']).nullable().optional(),
  play_count: z.number().int().min(0).nullable().optional(),
  booking_count: z.number().int().min(0).nullable().optional(),
  recommendation_score: z.union([z.number().min(0), z.string()]).nullable().optional(),
});

const BookingCountRowSchema = z.object({
  id: z.string().uuid(),
  booking_count: z.number().int().min(0).nullable().optional(),
});

type SupabaseDramaRow = z.infer<typeof SupabaseDramaRowSchema>;

const DRAMA_SELECT_COLUMNS = 'id,title,description,cover_url,category,episode_count,rating,created_at,updated_at';
const RANKING_SELECT_COLUMNS = `${DRAMA_SELECT_COLUMNS},content_type,play_count,booking_count,recommendation_score`;

const HOT_SEARCH_ITEMS: HotSearchListResponse = HotSearchListResponseSchema.parse({
  data: [
    { rank: 1, keyword: '逆袭', score: 9821 },
    { rank: 2, keyword: '豪门', score: 9540 },
    { rank: 3, keyword: '总裁', score: 9300 },
    { rank: 4, keyword: '甜宠', score: 9088 },
    { rank: 5, keyword: '重生', score: 8890 },
    { rank: 6, keyword: '穿书', score: 8605 },
    { rank: 7, keyword: '都市', score: 8411 },
    { rank: 8, keyword: '校园', score: 8204 },
    { rank: 9, keyword: '复仇', score: 7988 },
    { rank: 10, keyword: '萌宝', score: 7802 },
  ],
});

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
    episode_count: parsed.data.episode_count,
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

function mapRowToRankingDrama(row: unknown, isBooked = false): RankingDrama {
  const parsed = SupabaseDramaRowSchema.safeParse(row);
  if (!parsed.success) {
    throw Errors.internal('Invalid ranking drama row returned from Supabase');
  }

  const recommendationScore = parsed.data.recommendation_score;
  const numericRecommendationScore = typeof recommendationScore === 'string'
    ? Number.parseFloat(recommendationScore)
    : recommendationScore ?? 0;

  const rankingDrama = RankingDramaSchema.safeParse({
    id: parsed.data.id,
    title: parsed.data.title,
    description: parsed.data.description ?? '',
    cover_url: parsed.data.cover_url ?? null,
    category: parsed.data.category ?? '',
    episode_count: parsed.data.episode_count,
    tags: [],
    rating: parsed.data.rating ?? null,
    created_at: parsed.data.created_at,
    updated_at: parsed.data.updated_at,
    content_type: parsed.data.content_type ?? 'live_action',
    play_count: parsed.data.play_count ?? 0,
    booking_count: parsed.data.booking_count ?? 0,
    recommendation_score: numericRecommendationScore,
    is_booked: isBooked,
  });

  if (!rankingDrama.success) {
    throw Errors.internal('Failed to map ranking drama row to canonical contract');
  }

  return rankingDrama.data;
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
    episode_count: data.episode_count,
    rating: data.rating,
    content_type: undefined,
    play_count: undefined,
    booking_count: undefined,
    recommendation_score: undefined,
  };
}

function computeTotalPages(total: number, pageSize: number): number {
  return total === 0 ? 0 : Math.ceil(total / pageSize);
}

function rankingSortColumn(type: RankingParams['type']): 'play_count' | 'recommendation_score' | 'booking_count' {
  switch (type) {
    case 'hot':
      return 'play_count';
    case 'recommend':
      return 'recommendation_score';
    case 'booking':
      return 'booking_count';
  }
}

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

    return {
      data: (data ?? []).map(mapRowToDrama),
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: computeTotalPages(total, params.pageSize),
      },
    };
  }

  async search(params: SearchDramasParams): Promise<PaginatedResult<Drama>> {
    const supabase = getSupabaseAdmin();
    const from = (params.page - 1) * params.pageSize;
    const to = from + params.pageSize - 1;
    const escapedQuery = params.q.trim().replace(/[%_]/g, (char) => `\\${char}`);
    const queryPattern = `%${escapedQuery}%`;

    const { data, error, count } = await supabase
      .from('dramas')
      .select(DRAMA_SELECT_COLUMNS, { count: 'exact', head: false })
      .or(`title.ilike.${queryPattern},category.ilike.${queryPattern}`)
      .range(from, to)
      .order('created_at', { ascending: false });

    if (error) {
      throw Errors.internal(`Failed to search dramas: ${error.message}`);
    }

    const total = count ?? 0;

    return {
      data: (data ?? []).map(mapRowToDrama),
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: computeTotalPages(total, params.pageSize),
      },
    };
  }

  async listRankings(
    params: RankingParams,
    authContext?: AuthContext,
  ): Promise<PaginatedResult<RankingDrama>> {
    const supabase = getSupabaseAdmin();
    const from = (params.page - 1) * params.pageSize;
    const to = from + params.pageSize - 1;

    let query = supabase
      .from('dramas')
      .select(RANKING_SELECT_COLUMNS, { count: 'exact', head: false });

    if (params.contentType !== 'all') {
      query = query.eq('content_type', params.contentType);
    }

    const { data, error, count } = await query
      .range(from, to)
      .order(rankingSortColumn(params.type), { ascending: false })
      .order('created_at', { ascending: false });

    if (error) {
      throw Errors.internal(`Failed to fetch rankings: ${error.message}`);
    }

    const dramaRows = data ?? [];
    const dramaIds = dramaRows
      .map((row) => SupabaseDramaRowSchema.safeParse(row))
      .filter((row): row is { success: true; data: SupabaseDramaRow } => row.success)
      .map((row) => row.data.id);

    const bookedIds = new Set<string>();
    if (authContext && dramaIds.length > 0) {
      const { data: bookingRows, error: bookingsError } = await supabase
        .from('bookings')
        .select('drama_id')
        .eq('user_id', authContext.userId)
        .in('drama_id', dramaIds);

      if (bookingsError) {
        throw Errors.internal(`Failed to fetch booking state: ${bookingsError.message}`);
      }

      for (const row of bookingRows ?? []) {
        const dramaId = typeof row.drama_id === 'string' ? row.drama_id : null;
        if (dramaId) {
          bookedIds.add(dramaId);
        }
      }
    }

    const total = count ?? 0;

    return {
      data: dramaRows.map((row) => {
        const parsed = SupabaseDramaRowSchema.safeParse(row);
        const isBooked = parsed.success ? bookedIds.has(parsed.data.id) : false;
        return mapRowToRankingDrama(row, isBooked);
      }),
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: computeTotalPages(total, params.pageSize),
      },
    };
  }

  async listHotSearches(): Promise<HotSearchListResponse> {
    return {
      data: HOT_SEARCH_ITEMS.data.map((item) => ({ ...item })),
    };
  }

  async bookDrama(params: BookDramaParams): Promise<BookDramaResponse> {
    const supabase = getSupabaseAdmin();
    const { data: dramaRow, error: dramaError } = await supabase
      .from('dramas')
      .select('id,booking_count')
      .eq('id', params.dramaId)
      .single();

    if (dramaError) {
      if (dramaError.code === 'PGRST116') {
        throw Errors.notFound('Drama', params.dramaId);
      }

      throw Errors.internal(`Failed to load drama booking state: ${dramaError.message}`);
    }

    const parsedDramaRow = BookingCountRowSchema.safeParse(dramaRow);
    if (!parsedDramaRow.success) {
      throw Errors.internal('Invalid booking counter row returned from Supabase');
    }

    const currentBookingCount = parsedDramaRow.data.booking_count ?? 0;
    const { error: insertError } = await supabase
      .from('bookings')
      .insert({
        user_id: params.userId,
        drama_id: params.dramaId,
      });

    if (insertError?.code === '23505') {
      return BookDramaResponseSchema.parse({
        drama_id: params.dramaId,
        booked: true,
        booking_count: currentBookingCount,
      });
    }

    if (insertError) {
      throw Errors.internal(`Failed to create booking: ${insertError.message}`);
    }

    const nextBookingCount = currentBookingCount + 1;
    const { error: updateError } = await supabase
      .from('dramas')
      .update({ booking_count: nextBookingCount })
      .eq('id', params.dramaId);

    if (updateError) {
      throw Errors.internal(`Failed to update booking count: ${updateError.message}`);
    }

    return BookDramaResponseSchema.parse({
      drama_id: params.dramaId,
      booked: true,
      booking_count: nextBookingCount,
    });
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
    if (data.episode_count !== undefined) rowUpdate.episode_count = data.episode_count;
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

  async count(): Promise<number> {
    const supabase = getSupabaseAdmin();
    const { count, error } = await supabase
      .from('dramas')
      .select('*', { count: 'exact', head: true });

    if (error) {
      throw Errors.internal(`Failed to count dramas: ${error.message}`);
    }

    return count ?? 0;
  }
}
