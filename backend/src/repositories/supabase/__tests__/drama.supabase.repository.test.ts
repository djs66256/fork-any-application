import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseClient: vi.fn(),
}));

describe('DramaSupabaseRepository', () => {
  let DramaSupabaseRepository: typeof import('../../../repositories/supabase/drama.supabase.repository').DramaSupabaseRepository;
  let getSupabaseClient: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    vi.resetModules();

    const mockClient = {
      from: vi.fn(),
    };
    getSupabaseClient = vi.fn().mockReturnValue(mockClient);

    vi.doMock('@/infrastructure/supabase', () => ({
      getSupabaseClient,
    }));

    const mod = await import('../drama.supabase.repository');
    DramaSupabaseRepository = mod.DramaSupabaseRepository;
  });

  it('findMany should return paginated results', async () => {
    const mockData = [
      { id: '1', title: 'Drama 1', total_episodes: 12, status: 'ongoing', play_count: 0 },
      { id: '2', title: 'Drama 2', total_episodes: 24, status: 'completed', play_count: 100 },
    ];

    const mockResult = { data: mockData, error: null, count: 25 };

    // The chain: from() -> select() -> range() -> order() which resolves
    // select() returns a new builder, range() returns a new builder, order() resolves
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
    getSupabaseClient().from.mockReturnValue(builder);

    const repo = new DramaSupabaseRepository();
    const result = await repo.findMany({ page: 2, pageSize: 10 });

    expect(result.pagination.page).toBe(2);
    expect(result.pagination.page_size).toBe(10);
    expect(result.pagination.total).toBe(25);
    expect(result.pagination.total_pages).toBe(3);
    expect(result.data).toHaveLength(2);
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
    getSupabaseClient().from.mockReturnValue(mockChain);

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
    getSupabaseClient().from.mockReturnValue(mockChain);

    const repo = new DramaSupabaseRepository();
    await expect(repo.findById('some-id')).rejects.toThrow();
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
    getSupabaseClient().from.mockReturnValue(mockChain);

    const repo = new DramaSupabaseRepository();
    await expect(repo.create({ title: 'test' } as unknown as Parameters<typeof repo.create>[0])).rejects.toThrow();
  });

  it('delete should return true on success', async () => {
    const mockChain = {
      delete: vi.fn().mockReturnThis(),
      eq: vi.fn().mockResolvedValue({
        error: null,
      }),
    };
    getSupabaseClient().from.mockReturnValue(mockChain);

    const repo = new DramaSupabaseRepository();
    const result = await repo.delete('some-id');
    expect(result).toBe(true);
  });
});
