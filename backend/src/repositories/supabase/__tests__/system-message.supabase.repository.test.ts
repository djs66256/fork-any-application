import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: vi.fn(),
}));

describe('SystemMessageSupabaseRepository', () => {
  let SystemMessageSupabaseRepository: typeof import('../system-message.supabase.repository').SystemMessageSupabaseRepository;
  let getSupabaseAdmin: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    vi.resetModules();

    const mockClient = { from: vi.fn() };
    getSupabaseAdmin = vi.fn().mockReturnValue(mockClient);

    vi.doMock('@/infrastructure/supabase', () => ({
      getSupabaseAdmin,
    }));

    const mod = await import('../system-message.supabase.repository');
    SystemMessageSupabaseRepository = mod.SystemMessageSupabaseRepository;
  });

  it('should return latest preview row', async () => {
    const chain = {
      select: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      limit: vi.fn().mockReturnThis(),
      maybeSingle: vi.fn().mockResolvedValue({
        data: {
          id: '550e8400-e29b-41d4-a716-446655440001',
          title: '系统通知',
          summary: '你关注的剧集已更新第 12 集。',
          sent_at: '2026-07-29T08:00:00.000Z',
        },
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(chain);

    const repo = new SystemMessageSupabaseRepository();
    const result = await repo.getLatest();

    expect(result?.id).toBe('550e8400-e29b-41d4-a716-446655440001');
  });

  it('should return null for empty previews', async () => {
    const chain = {
      select: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      limit: vi.fn().mockReturnThis(),
      maybeSingle: vi.fn().mockResolvedValue({
        data: null,
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(chain);

    const repo = new SystemMessageSupabaseRepository();
    await expect(repo.getLatest()).resolves.toBeNull();
  });

  it('should paginate system messages', async () => {
    const chain = {
      select: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      range: vi.fn().mockResolvedValue({
        data: [
          {
            id: '550e8400-e29b-41d4-a716-446655440001',
            title: '系统通知',
            summary: '你关注的剧集已更新第 12 集。',
            sent_at: '2026-07-29T08:00:00.000Z',
          },
        ],
        error: null,
        count: 1,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(chain);

    const repo = new SystemMessageSupabaseRepository();
    const result = await repo.list({ page: 1, pageSize: 20 });

    expect(result.pagination).toEqual({
      page: 1,
      page_size: 20,
      total: 1,
      total_pages: 1,
    });
  });

  it('should surface datasource failures as service unavailable', async () => {
    const chain = {
      select: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      range: vi.fn().mockResolvedValue({
        data: null,
        count: null,
        error: { message: 'network timeout while connecting', code: '08006' },
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(chain);

    const repo = new SystemMessageSupabaseRepository();

    await expect(repo.list({ page: 1, pageSize: 20 })).rejects.toMatchObject({
      code: 'SERVICE_UNAVAILABLE',
    });
  });

  it('should treat invalid rows as service unavailable', async () => {
    const chain = {
      select: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      range: vi.fn().mockResolvedValue({
        data: [{ id: 'bad-id', title: 'x', summary: 'y', sent_at: '2026-07-29T08:00:00.000Z' }],
        error: null,
        count: 1,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(chain);

    const repo = new SystemMessageSupabaseRepository();

    await expect(repo.list({ page: 1, pageSize: 20 })).rejects.toMatchObject({
      code: 'SERVICE_UNAVAILABLE',
    });
  });
});
