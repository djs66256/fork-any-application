import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: vi.fn(),
}));

describe('PlaybackHistorySupabaseRepository', () => {
  let PlaybackHistorySupabaseRepository: typeof import('../playback-history.supabase.repository').PlaybackHistorySupabaseRepository;
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

    const mod = await import('../playback-history.supabase.repository');
    PlaybackHistorySupabaseRepository = mod.PlaybackHistorySupabaseRepository;
  });

  it('listRecentBySession should query recent playback histories by session and limit', async () => {
    const rows = [
      {
        playback_session_id: '770e8400-e29b-41d4-a716-446655440000',
        drama_id: '550e8400-e29b-41d4-a716-446655440001',
        episode_id: '660e8400-e29b-41d4-a716-446655440001',
        progress: 120,
        duration: 180,
        updated_at: '2026-07-26T00:00:00Z',
      },
    ];

    const mockChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      limit: vi.fn().mockResolvedValue({
        data: rows,
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(mockChain);

    const repo = new PlaybackHistorySupabaseRepository();
    const result = await repo.listRecentBySession('770e8400-e29b-41d4-a716-446655440000', 10);

    expect(getSupabaseAdmin().from).toHaveBeenCalledWith('playback_history');
    expect(mockChain.select).toHaveBeenCalledWith(
      'playback_session_id,drama_id,episode_id,progress,duration,updated_at',
    );
    expect(mockChain.eq).toHaveBeenCalledWith(
      'playback_session_id',
      '770e8400-e29b-41d4-a716-446655440000',
    );
    expect(mockChain.order).toHaveBeenCalledWith('updated_at', { ascending: false });
    expect(mockChain.limit).toHaveBeenCalledWith(10);
    expect(result).toEqual(rows);
  });

  it('listRecentBySession should wrap supabase errors', async () => {
    const mockChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      limit: vi.fn().mockResolvedValue({
        data: null,
        error: { message: 'query rejected', code: 'XX000' },
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(mockChain);

    const repo = new PlaybackHistorySupabaseRepository();

    await expect(repo.listRecentBySession('770e8400-e29b-41d4-a716-446655440000', 10)).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
      message: 'Failed to fetch recent playback history: query rejected',
    });
  });

  it('listRecentBySession should surface datasource availability errors as service unavailable', async () => {
    const mockChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      limit: vi.fn().mockResolvedValue({
        data: null,
        error: { message: 'network timeout while connecting', code: '08006' },
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(mockChain);

    const repo = new PlaybackHistorySupabaseRepository();

    await expect(repo.listRecentBySession('770e8400-e29b-41d4-a716-446655440000', 10)).rejects.toMatchObject({
      code: 'SERVICE_UNAVAILABLE',
      message: 'Service unavailable: playback_history',
    });
  });
});
