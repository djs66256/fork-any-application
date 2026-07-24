import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseClient: vi.fn(),
}));

describe('EpisodeSupabaseRepository', () => {
  let EpisodeSupabaseRepository: typeof import('../../../repositories/supabase/episode.supabase.repository').EpisodeSupabaseRepository;
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

    const mod = await import('../episode.supabase.repository');
    EpisodeSupabaseRepository = mod.EpisodeSupabaseRepository;
  });

  it('findByDramaId should return episodes ordered by episode_number', async () => {
    const mockData = [
      { id: 'ep-1', drama_id: 'drama-1', title: 'Ep 1', episode_number: 1 },
      { id: 'ep-2', drama_id: 'drama-1', title: 'Ep 2', episode_number: 2 },
    ];

    const mockChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockResolvedValue({
        data: mockData,
        error: null,
      }),
    };
    getSupabaseClient().from.mockReturnValue(mockChain);

    const repo = new EpisodeSupabaseRepository();
    const result = await repo.findByDramaId('drama-1');
    expect(result).toHaveLength(2);
    expect(result[0].episode_number).toBe(1);
    expect(result[1].episode_number).toBe(2);
  });

  it('findById should return episode', async () => {
    const mockData = { id: 'ep-1', drama_id: 'drama-1', title: 'Pilot', episode_number: 1 };

    const mockChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: mockData,
        error: null,
      }),
    };
    getSupabaseClient().from.mockReturnValue(mockChain);

    const repo = new EpisodeSupabaseRepository();
    const result = await repo.findById('ep-1');
    expect(result).not.toBeNull();
    expect(result!.title).toBe('Pilot');
  });

  it('findById should return null for PGRST116', async () => {
    const mockChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: null,
        error: { code: 'PGRST116', message: 'No rows' },
      }),
    };
    getSupabaseClient().from.mockReturnValue(mockChain);

    const repo = new EpisodeSupabaseRepository();
    const result = await repo.findById('non-existent');
    expect(result).toBeNull();
  });
});
