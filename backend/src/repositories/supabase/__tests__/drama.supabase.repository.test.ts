import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: vi.fn(),
}));

describe('DramaSupabaseRepository', () => {
  let DramaSupabaseRepository: typeof import('../../../repositories/supabase/drama.supabase.repository').DramaSupabaseRepository;
  let getSupabaseAdmin: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    vi.resetModules();

    const mockClient = {
      from: vi.fn(),
    };
    getSupabaseAdmin = vi.fn().mockReturnValue(mockClient);

    vi.doMock('@/infrastructure/supabase', () => ({
      getSupabaseAdmin,
    }));

    const mod = await import('../drama.supabase.repository');
    DramaSupabaseRepository = mod.DramaSupabaseRepository;
  });

  it('findMany should map legacy supabase rows to canonical paginated results', async () => {
    const mockData = [
      {
        id: '550e8400-e29b-41d4-a716-446655440001',
        title: 'Drama 1',
        description: null,
        cover_url: null,
        category: '都市',
        total_episodes: 12,
        rating: 8.1,
        created_at: '2026-07-25T00:00:00Z',
        updated_at: '2026-07-25T00:00:00Z',
      },
      {
        id: '550e8400-e29b-41d4-a716-446655440002',
        title: 'Drama 2',
        description: 'desc',
        cover_url: 'https://example.com/drama-2.jpg',
        category: null,
        total_episodes: 24,
        rating: null,
        created_at: '2026-07-24T00:00:00Z',
        updated_at: '2026-07-24T00:00:00Z',
      },
    ];

    const mockResult = { data: mockData, error: null, count: 25 };

    const builderAfterRange = {
      order: vi.fn().mockResolvedValue(mockResult),
    };
    const builderAfterSelect = {
      range: vi.fn().mockReturnValue(builderAfterRange),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn(),
      insert: vi.fn().mockReturnThis(),
      update: vi.fn().mockReturnThis(),
      delete: vi.fn().mockReturnThis(),
    };
    const builder = {
      select: vi.fn().mockReturnValue(builderAfterSelect),
    };
    getSupabaseAdmin().from.mockReturnValue(builder);

    const repo = new DramaSupabaseRepository();
    const result = await repo.findMany({ page: 2, pageSize: 10 });

    expect(builder.select).toHaveBeenCalledWith(
      'id,title,description,cover_url,category,total_episodes,rating,created_at,updated_at',
      { count: 'exact', head: false },
    );
    expect(result.pagination.page).toBe(2);
    expect(result.pagination.page_size).toBe(10);
    expect(result.pagination.total).toBe(25);
    expect(result.pagination.total_pages).toBe(3);
    expect(result.data).toHaveLength(2);
    expect(result.data[0]).toMatchObject({
      episode_count: 12,
      description: '',
      tags: [],
    });
    expect(result.data[1]).toMatchObject({
      episode_count: 24,
      category: '',
      rating: null,
      tags: [],
    });
  });

  it('search should query title and category with case-insensitive matching', async () => {
    const mockData = [
      {
        id: '550e8400-e29b-41d4-a716-446655440001',
        title: '逆袭归来后我成了豪门团宠',
        description: null,
        cover_url: null,
        category: '都市',
        total_episodes: 68,
        rating: 8.9,
        created_at: '2026-07-25T00:00:00Z',
        updated_at: '2026-07-25T00:00:00Z',
      },
    ];

    const builderAfterRange = {
      order: vi.fn().mockResolvedValue({ data: mockData, error: null, count: 1 }),
    };
    const builderAfterOr = {
      range: vi.fn().mockReturnValue(builderAfterRange),
    };
    const builderAfterSelect = {
      or: vi.fn().mockReturnValue(builderAfterOr),
    };
    const builder = {
      select: vi.fn().mockReturnValue(builderAfterSelect),
    };
    getSupabaseAdmin().from.mockReturnValue(builder);

    const repo = new DramaSupabaseRepository();
    const result = await repo.search({ q: '后', page: 1, pageSize: 10 });

    expect(builderAfterSelect.or).toHaveBeenCalledWith('title.ilike.%后%,category.ilike.%后%');
    expect(result.data).toHaveLength(1);
    expect(result.pagination).toEqual({
      page: 1,
      page_size: 10,
      total: 1,
      total_pages: 1,
    });
  });

  it('search should return empty data for oversized pages while preserving pagination', async () => {
    const builderAfterRange = {
      order: vi.fn().mockResolvedValue({ data: [], error: null, count: 4 }),
    };
    const builderAfterOr = {
      range: vi.fn().mockReturnValue(builderAfterRange),
    };
    const builderAfterSelect = {
      or: vi.fn().mockReturnValue(builderAfterOr),
    };
    const builder = {
      select: vi.fn().mockReturnValue(builderAfterSelect),
    };
    getSupabaseAdmin().from.mockReturnValue(builder);

    const repo = new DramaSupabaseRepository();
    const result = await repo.search({ q: '后', page: 999, pageSize: 10 });

    expect(result.data).toEqual([]);
    expect(result.pagination).toEqual({
      page: 999,
      page_size: 10,
      total: 4,
      total_pages: 1,
    });
  });

  it('listRankings should map ranking fields and booking state', async () => {
    const rankingRows = [
      {
        id: '550e8400-e29b-41d4-a716-446655440001',
        title: 'Drama 1',
        description: null,
        cover_url: null,
        category: '都市',
        total_episodes: 12,
        rating: 8.1,
        created_at: '2026-07-25T00:00:00Z',
        updated_at: '2026-07-25T00:00:00Z',
        content_type: 'ai',
        play_count: 200,
        booking_count: 20,
        recommendation_score: '88.4',
      },
    ];

    const bookingRows = [{ drama_id: '550e8400-e29b-41d4-a716-446655440001' }];

    const rankingBuilderAfterOrderOne = {
      order: vi.fn().mockResolvedValue({ data: rankingRows, error: null, count: 1 }),
    };
    const rankingBuilderAfterRange = {
      order: vi.fn().mockReturnValue(rankingBuilderAfterOrderOne),
    };
    const rankingBuilderAfterEq = {
      range: vi.fn().mockReturnValue(rankingBuilderAfterRange),
    };
    const rankingBuilderAfterSelect = {
      eq: vi.fn().mockReturnValue(rankingBuilderAfterEq),
      range: vi.fn().mockReturnValue(rankingBuilderAfterRange),
    };
    const rankingBuilder = {
      select: vi.fn().mockReturnValue(rankingBuilderAfterSelect),
    };

    const bookingsBuilderAfterEq = {
      in: vi.fn().mockResolvedValue({ data: bookingRows, error: null }),
    };
    const bookingsBuilder = {
      select: vi.fn().mockReturnValue({
        eq: vi.fn().mockReturnValue(bookingsBuilderAfterEq),
      }),
    };

    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'dramas') return rankingBuilder;
      if (table === 'bookings') return bookingsBuilder;
      throw new Error(`Unexpected table ${table}`);
    });

    const repo = new DramaSupabaseRepository();
    const result = await repo.listRankings(
      { contentType: 'ai', type: 'recommend', page: 1, pageSize: 10 },
      { userId: 'user-1' },
    );

    expect(rankingBuilder.select).toHaveBeenCalledWith(
      'id,title,description,cover_url,category,total_episodes,rating,created_at,updated_at,content_type,play_count,booking_count,recommendation_score',
      { count: 'exact', head: false },
    );
    expect(rankingBuilderAfterSelect.eq).toHaveBeenCalledWith('content_type', 'ai');
    expect(result.data[0]).toMatchObject({
      content_type: 'ai',
      play_count: 200,
      booking_count: 20,
      recommendation_score: 88.4,
      is_booked: true,
    });
  });

  it('listHotSearches should return stable static items', async () => {
    const repo = new DramaSupabaseRepository();
    const result = await repo.listHotSearches();

    expect(result.data.length).toBeGreaterThan(0);
    expect(result.data.length).toBeLessThanOrEqual(10);
    expect(result.data[0]).toEqual({ rank: 1, keyword: '逆袭', score: 9821 });
  });

  it('findById should return canonical drama data', async () => {
    const row = {
      id: '550e8400-e29b-41d4-a716-446655440003',
      title: 'Drama 3',
      description: null,
      cover_url: null,
      category: '情感',
      total_episodes: 30,
      rating: 7.5,
      created_at: '2026-07-23T00:00:00Z',
      updated_at: '2026-07-23T00:00:00Z',
    };

    const mockChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: row,
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(mockChain);

    const repo = new DramaSupabaseRepository();
    const result = await repo.findById(row.id);

    expect(result).toMatchObject({
      id: row.id,
      episode_count: 30,
      tags: [],
      description: '',
    });
  });

  it('findById should return null for PGRST116', async () => {
    const mockChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: null,
        error: { code: 'PGRST116', message: 'No rows returned' },
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(mockChain);

    const repo = new DramaSupabaseRepository();
    const result = await repo.findById('non-existent');
    expect(result).toBeNull();
  });

  it('findById should throw on other errors', async () => {
    const mockChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: null,
        error: { code: 'OTHER', message: 'DB error' },
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(mockChain);

    const repo = new DramaSupabaseRepository();
    await expect(repo.findById('some-id')).rejects.toThrow();
  });

  it('create should map canonical input to legacy supabase columns', async () => {
    const insertedRow = {
      id: '550e8400-e29b-41d4-a716-446655440004',
      title: 'Created drama',
      description: '',
      cover_url: null,
      category: '都市',
      total_episodes: 12,
      rating: null,
      created_at: '2026-07-22T00:00:00Z',
      updated_at: '2026-07-22T00:00:00Z',
    };

    const mockChain = {
      insert: vi.fn().mockReturnThis(),
      select: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: insertedRow,
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(mockChain);

    const repo = new DramaSupabaseRepository();
    const result = await repo.create({
      title: 'Created drama',
      description: '',
      cover_url: null,
      category: '都市',
      episode_count: 12,
      tags: [],
      rating: null,
    });

    expect(mockChain.insert).toHaveBeenCalledWith({
      title: 'Created drama',
      description: '',
      cover_url: null,
      category: '都市',
      total_episodes: 12,
      rating: null,
      content_type: undefined,
      play_count: undefined,
      booking_count: undefined,
      recommendation_score: undefined,
    });
    expect(result).toMatchObject({
      id: insertedRow.id,
      episode_count: 12,
      tags: [],
    });
  });

  it('create should reject non-empty tags until storage supports them', async () => {
    const repo = new DramaSupabaseRepository();

    await expect(
      repo.create({
        title: 'Created drama',
        description: '',
        cover_url: null,
        category: '都市',
        episode_count: 12,
        tags: ['标签'],
        rating: null,
      }),
    ).rejects.toThrow(/does not support non-empty tags/i);
  });

  it('update should reject non-empty tags until storage supports them', async () => {
    const repo = new DramaSupabaseRepository();

    await expect(
      repo.update('550e8400-e29b-41d4-a716-446655440004', {
        tags: ['标签'],
      }),
    ).rejects.toThrow(/does not support non-empty tags/i);
  });

  it('create should handle conflict error', async () => {
    const mockChain = {
      insert: vi.fn().mockReturnThis(),
      select: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: null,
        error: { code: '23505', message: 'Duplicate key' },
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(mockChain);

    const repo = new DramaSupabaseRepository();
    await expect(
      repo.create({
        title: 'test',
        description: '',
        cover_url: null,
        category: '都市',
        episode_count: 12,
        tags: [],
        rating: null,
      }),
    ).rejects.toThrow();
  });

  it('bookDrama should create booking and increment count', async () => {
    const bookingReadChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: { id: '550e8400-e29b-41d4-a716-446655440001', booking_count: 20 },
        error: null,
      }),
      update: vi.fn().mockReturnThis(),
    };
    const bookingsInsertChain = {
      insert: vi.fn().mockResolvedValue({ error: null }),
    };
    const dramasUpdateChain = {
      update: vi.fn().mockReturnThis(),
      eq: vi.fn().mockResolvedValue({ error: null }),
      select: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: { id: '550e8400-e29b-41d4-a716-446655440001', booking_count: 20 },
        error: null,
      }),
    };

    let dramaCallCount = 0;
    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'dramas') {
        dramaCallCount += 1;
        return dramaCallCount === 1 ? bookingReadChain : dramasUpdateChain;
      }
      if (table === 'bookings') {
        return bookingsInsertChain;
      }
      throw new Error(`Unexpected table ${table}`);
    });

    const repo = new DramaSupabaseRepository();
    const result = await repo.bookDrama({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      userId: 'user-1',
    });

    expect(bookingsInsertChain.insert).toHaveBeenCalledWith({
      user_id: 'user-1',
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
    });
    expect(dramasUpdateChain.update).toHaveBeenCalledWith({ booking_count: 21 });
    expect(result).toEqual({
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
      booked: true,
      booking_count: 21,
    });
  });

  it('bookDrama should stay idempotent on duplicate booking', async () => {
    const bookingReadChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: { id: '550e8400-e29b-41d4-a716-446655440001', booking_count: 20 },
        error: null,
      }),
    };
    const bookingsInsertChain = {
      insert: vi.fn().mockResolvedValue({ error: { code: '23505', message: 'duplicate key' } }),
    };

    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'dramas') return bookingReadChain;
      if (table === 'bookings') return bookingsInsertChain;
      throw new Error(`Unexpected table ${table}`);
    });

    const repo = new DramaSupabaseRepository();
    const result = await repo.bookDrama({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      userId: 'user-1',
    });

    expect(result.booking_count).toBe(20);
    expect(result.booked).toBe(true);
  });

  it('bookDrama should surface missing drama as not found', async () => {
    const bookingReadChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: null,
        error: { code: 'PGRST116', message: 'No rows returned' },
      }),
    };
    getSupabaseAdmin().from.mockImplementation(() => bookingReadChain);

    const repo = new DramaSupabaseRepository();
    await expect(
      repo.bookDrama({
        dramaId: '550e8400-e29b-41d4-a716-446655440001',
        userId: 'user-1',
      }),
    ).rejects.toMatchObject({
      code: 'NOT_FOUND',
    });
  });

  it('delete should return true on success', async () => {
    const mockChain = {
      delete: vi.fn().mockReturnThis(),
      eq: vi.fn().mockResolvedValue({
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(mockChain);

    const repo = new DramaSupabaseRepository();
    const result = await repo.delete('some-id');
    expect(result).toBe(true);
  });
});
