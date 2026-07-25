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
