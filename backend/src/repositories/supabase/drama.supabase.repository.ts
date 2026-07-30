import { z } from 'zod';
import {
  BookDramaResponse,
  BookDramaResponseSchema,
  BookingAsset,
  BookingAssetAvailabilityStatus,
  BookingAssetListResponse,
  BookingAssetSchema,
  BookingAssetSummary,
  ClassificationDimension,
  CLASSIFICATION_DIMENSION_KEYS,
  ClassificationGender,
  Drama,
  DramaSchema,
  HotSearchListResponse,
  HotSearchListResponseSchema,
  RankingDrama,
  RankingDramaSchema,
  TheaterDrama,
} from '@/lib/schemas';
import {
  AuthContext,
  BookDramaParams,
  ClassificationTagsQuery,
  ClassificationTagsResult,
  DramaRepositoryInterface,
  ListUserBookingsParams,
  PaginatedResult,
  PaginationParams,
  RankingParams,
  SearchDramasParams,
  TheaterFeedParams,
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
  tags: z.array(z.string()).nullable().optional(),
});

const BookingCountRowSchema = z.object({
  id: z.string().uuid(),
  booking_count: z.number().int().min(0).nullable().optional(),
});

const BookingAssetDramaRowSchema = z.object({
  id: z.string().uuid(),
  title: z.string().trim().min(1),
  cover_url: z.string().url().nullable().optional(),
  episode_count: z.number().int().min(0),
  status: z.string().trim().min(1),
});

const BookingAssetRowSchema = z.object({
  drama_id: z.string().uuid(),
  created_at: z.string(),
  dramas: BookingAssetDramaRowSchema.nullable().optional(),
});

const BOOKING_ASSET_SELECT_COLUMNS = 'drama_id,created_at,dramas!inner(id,title,cover_url,episode_count,status)';

type SupabaseDramaRow = z.infer<typeof SupabaseDramaRowSchema>;

const DRAMA_SELECT_COLUMNS = 'id,title,description,cover_url,category,episode_count,rating,created_at,updated_at,tags';
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

const CLASSIFICATION_DIMENSION_NAMES: Record<(typeof CLASSIFICATION_DIMENSION_KEYS)[number], string> = {
  era_background: '时代背景',
  theme_plot: '主题情节',
  character_setting: '角色设定',
};

const CLASSIFICATION_TAG_SEEDS: Record<Exclude<ClassificationGender, 'all'>, ClassificationDimension[]> = {
  male: [
    {
      key: 'era_background',
      name: CLASSIFICATION_DIMENSION_NAMES.era_background,
      tags: ['都市', '古风', '年代'],
    },
    {
      key: 'theme_plot',
      name: CLASSIFICATION_DIMENSION_NAMES.theme_plot,
      tags: ['逆袭', '系统', '复仇'],
    },
    {
      key: 'character_setting',
      name: CLASSIFICATION_DIMENSION_NAMES.character_setting,
      tags: ['总裁', '萌宝'],
    },
  ],
  female: [
    {
      key: 'era_background',
      name: CLASSIFICATION_DIMENSION_NAMES.era_background,
      tags: ['都市', '校园', '豪门'],
    },
    {
      key: 'theme_plot',
      name: CLASSIFICATION_DIMENSION_NAMES.theme_plot,
      tags: ['甜宠', '穿书', '重生'],
    },
    {
      key: 'character_setting',
      name: CLASSIFICATION_DIMENSION_NAMES.character_setting,
      tags: [],
    },
  ],
};

function cloneDimension(dimension: ClassificationDimension): ClassificationDimension {
  return {
    ...dimension,
    tags: [...dimension.tags],
  };
}

function mergeUniqueTags(primary: string[], secondary: string[]): string[] {
  const seen = new Set<string>();
  const merged: string[] = [];

  for (const tag of [...primary, ...secondary]) {
    if (seen.has(tag)) {
      continue;
    }

    seen.add(tag);
    merged.push(tag);
  }

  return merged;
}

function getClassificationDimensions(gender: ClassificationGender): ClassificationDimension[] {
  if (gender === 'all') {
    return CLASSIFICATION_DIMENSION_KEYS.map((key) => {
      const maleDimension = CLASSIFICATION_TAG_SEEDS.male.find((dimension) => dimension.key === key);
      const femaleDimension = CLASSIFICATION_TAG_SEEDS.female.find((dimension) => dimension.key === key);

      return {
        key,
        name: CLASSIFICATION_DIMENSION_NAMES[key],
        tags: mergeUniqueTags(maleDimension?.tags ?? [], femaleDimension?.tags ?? []),
      };
    });
  }

  return CLASSIFICATION_DIMENSION_KEYS.map((key) => {
    const dimension = CLASSIFICATION_TAG_SEEDS[gender].find((item) => item.key === key);

    return {
      key,
      name: CLASSIFICATION_DIMENSION_NAMES[key],
      tags: [...(dimension?.tags ?? [])],
    };
  });
}

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
    tags: parsed.data.tags ?? [],
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
    tags: parsed.data.tags ?? [],
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
  return {
    title: data.title,
    description: data.description,
    cover_url: data.cover_url,
    category: data.category,
    episode_count: data.episode_count,
    rating: data.rating,
    tags: data.tags,
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

function escapeIlikeQuery(value: string): string {
  return value.trim().replace(/[%_]/g, (char) => `\\${char}`);
}

function buildSearchExpression(queryPattern: string): string {
  return `title.ilike.${queryPattern},category.ilike.${queryPattern},tags.cs.{"${queryPattern.slice(1, -1)}"}`;
}

function mapDramaStatusToAvailabilityStatus(status: string): BookingAssetAvailabilityStatus | null {
  if (status === 'announced') {
    return 'upcoming';
  }

  if (status === 'ongoing' || status === 'completed') {
    return 'online';
  }

  return null;
}

function toServiceUnavailableError(error: { message: string } | null) {
  const message = error?.message?.trim();
  if (message) {
    return Errors.serviceUnavailable(`Supabase: ${message}`);
  }

  return Errors.serviceUnavailable('Supabase');
}

function parseBookingAssetRows(rows: unknown[]): z.infer<typeof BookingAssetRowSchema>[] {
  return rows.flatMap((row) => {
    const parsed = BookingAssetRowSchema.safeParse(row);
    if (!parsed.success) {
      return [];
    }

    if (!parsed.data.dramas) {
      return [];
    }

    return [parsed.data];
  });
}

function sortBookingAssetRows(rows: z.infer<typeof BookingAssetRowSchema>[]): z.infer<typeof BookingAssetRowSchema>[] {
  return [...rows].sort((left, right) => {
    const bookedAtDifference = right.created_at.localeCompare(left.created_at);
    if (bookedAtDifference !== 0) {
      return bookedAtDifference;
    }

    return right.drama_id.localeCompare(left.drama_id);
  });
}

function summarizeBookingAssets(rows: z.infer<typeof BookingAssetRowSchema>[]): BookingAssetSummary {
  return rows.reduce<BookingAssetSummary>((summary, row) => {
    const availabilityStatus = mapDramaStatusToAvailabilityStatus(row.dramas?.status ?? '');
    if (!availabilityStatus) {
      console.warn('[BookingAssets] Unknown drama status', {
        dramaId: row.drama_id,
        status: row.dramas?.status,
      });
      return summary;
    }

    if (availabilityStatus === 'online') {
      summary.online_count += 1;
    } else {
      summary.upcoming_count += 1;
    }

    return summary;
  }, {
    online_count: 0,
    upcoming_count: 0,
  });
}

function mapBookingRowToAsset(row: z.infer<typeof BookingAssetRowSchema>): BookingAsset | null {
  const availabilityStatus = mapDramaStatusToAvailabilityStatus(row.dramas?.status ?? '');
  if (!availabilityStatus || !row.dramas) {
    if (row.dramas) {
      console.warn('[BookingAssets] Unknown drama status', {
        dramaId: row.drama_id,
        status: row.dramas.status,
      });
    }
    return null;
  }

  return BookingAssetSchema.parse({
    drama_id: row.drama_id,
    title: row.dramas.title,
    cover_url: row.dramas.cover_url ?? null,
    episode_count: row.dramas.episode_count,
    booked_at: row.created_at,
    availability_status: availabilityStatus,
  });
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
    const escapedQuery = escapeIlikeQuery(params.q);
    const queryPattern = `%${escapedQuery}%`;

    const { data, error, count } = await supabase
      .from('dramas')
      .select(DRAMA_SELECT_COLUMNS, { count: 'exact', head: false })
      .or(buildSearchExpression(queryPattern))
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

  async listTheaterFeed(params: TheaterFeedParams): Promise<PaginatedResult<TheaterDrama>> {
    void params;
    throw Errors.serviceUnavailable('theater-feed');
  }

  async listClassificationTags(params: ClassificationTagsQuery): Promise<ClassificationTagsResult> {
    return {
      gender: params.gender,
      dimensions: getClassificationDimensions(params.gender).map(cloneDimension),
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

  async listUserBookings(params: ListUserBookingsParams): Promise<BookingAssetListResponse> {
    const supabase = getSupabaseAdmin();
    const from = (params.page - 1) * params.pageSize;
    const to = from + params.pageSize - 1;

    const { data: summaryRows, error: summaryError } = await supabase
      .from('bookings')
      .select(BOOKING_ASSET_SELECT_COLUMNS)
      .eq('user_id', params.userId);

    if (summaryError) {
      throw toServiceUnavailableError(summaryError);
    }

    const validSummaryRows = sortBookingAssetRows(parseBookingAssetRows(summaryRows ?? []));
    const summary = summarizeBookingAssets(validSummaryRows);

    let query = supabase
      .from('bookings')
      .select(BOOKING_ASSET_SELECT_COLUMNS, { count: 'exact', head: false })
      .eq('user_id', params.userId);

    if (params.status === 'online') {
      query = query.in('dramas.status', ['ongoing', 'completed']);
    } else {
      query = query.eq('dramas.status', 'announced');
    }

    const { data, error, count } = await query
      .order('created_at', { ascending: false })
      .order('drama_id', { ascending: false })
      .range(from, to);

    if (error) {
      throw toServiceUnavailableError(error);
    }

    const assets = sortBookingAssetRows(parseBookingAssetRows(data ?? []))
      .map(mapBookingRowToAsset)
      .filter((item): item is BookingAsset => item !== null);

    const total = count ?? 0;

    return {
      data: assets,
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: computeTotalPages(total, params.pageSize),
      },
      summary,
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

    if (data.title !== undefined) rowUpdate.title = data.title;
    if (data.description !== undefined) rowUpdate.description = data.description;
    if (data.cover_url !== undefined) rowUpdate.cover_url = data.cover_url;
    if (data.category !== undefined) rowUpdate.category = data.category;
    if (data.episode_count !== undefined) rowUpdate.episode_count = data.episode_count;
    if (data.rating !== undefined) rowUpdate.rating = data.rating;
    if (data.tags !== undefined) rowUpdate.tags = data.tags;

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
