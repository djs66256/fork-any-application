import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: vi.fn(),
}));

describe('CheckInSupabaseRepository', () => {
  let CheckInSupabaseRepository: typeof import('../check-in.supabase.repository').CheckInSupabaseRepository;
  let getSupabaseAdmin: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    vi.resetModules();

    const mockClient = { from: vi.fn() };
    getSupabaseAdmin = vi.fn().mockReturnValue(mockClient);

    vi.doMock('@/infrastructure/supabase', () => ({
      getSupabaseAdmin,
    }));

    const mod = await import('../check-in.supabase.repository');
    CheckInSupabaseRepository = mod.CheckInSupabaseRepository;
  });

  it('should list recent subject records', async () => {
    const chain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      limit: vi.fn().mockResolvedValue({
        data: [
          {
            id: '550e8400-e29b-41d4-a716-446655440001',
            subject_type: 'user',
            subject_id: '00000000-0000-4000-8000-13800138000',
            business_date: '2026-07-29',
            streak_day: 3,
            created_at: '2026-07-29T08:00:00.000Z',
          },
        ],
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(chain);

    const repo = new CheckInSupabaseRepository();
    const result = await repo.listRecentBySubject({
      type: 'user',
      id: '00000000-0000-4000-8000-13800138000',
    }, 10);

    expect(result).toHaveLength(1);
    expect(chain.limit).toHaveBeenCalledWith(10);
  });

  it('should create records and map rows', async () => {
    const insertChain = {
      select: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: {
          id: '550e8400-e29b-41d4-a716-446655440001',
          subject_type: 'user',
          subject_id: '00000000-0000-4000-8000-13800138000',
          business_date: '2026-07-29',
          streak_day: 3,
          created_at: '2026-07-29T08:00:00.000Z',
        },
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue({ insert: vi.fn().mockReturnValue(insertChain) });

    const repo = new CheckInSupabaseRepository();
    const result = await repo.createIfAbsent({
      subject_type: 'user',
      subject_id: '00000000-0000-4000-8000-13800138000',
      business_date: '2026-07-29',
      streak_day: 3,
    });

    expect(result.streak_day).toBe(3);
  });

  it('should treat unique conflicts as idempotent success', async () => {
    const selectChain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: {
          id: '550e8400-e29b-41d4-a716-446655440002',
          subject_type: 'user',
          subject_id: '00000000-0000-4000-8000-13800138000',
          business_date: '2026-07-29',
          streak_day: 3,
          created_at: '2026-07-29T08:00:00.000Z',
        },
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'check_in_records') {
        return {
          insert: vi.fn().mockReturnValue({
            select: vi.fn().mockReturnThis(),
            single: vi.fn().mockResolvedValue({
              data: null,
              error: { message: 'duplicate key value violates unique constraint', code: '23505' },
            }),
          }),
          select: vi.fn().mockReturnValue(selectChain),
        };
      }
      throw new Error(`unexpected table ${table}`);
    });

    const repo = new CheckInSupabaseRepository();
    const result = await repo.createIfAbsent({
      subject_type: 'user',
      subject_id: '00000000-0000-4000-8000-13800138000',
      business_date: '2026-07-29',
      streak_day: 3,
    });

    expect(result.id).toBe('550e8400-e29b-41d4-a716-446655440002');
  });

  it('should surface datasource failures as service unavailable', async () => {
    const chain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      limit: vi.fn().mockResolvedValue({
        data: null,
        error: { message: 'network timeout while connecting', code: '08006' },
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(chain);

    const repo = new CheckInSupabaseRepository();

    await expect(repo.listRecentBySubject({ type: 'user', id: 'u1' })).rejects.toMatchObject({
      code: 'SERVICE_UNAVAILABLE',
    });
  });

  it('should treat invalid rows as service unavailable', async () => {
    const chain = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      limit: vi.fn().mockResolvedValue({
        data: [{ id: 'not-a-uuid' }],
        error: null,
      }),
    };
    getSupabaseAdmin().from.mockReturnValue(chain);

    const repo = new CheckInSupabaseRepository();

    await expect(repo.listRecentBySubject({ type: 'user', id: 'u1' })).rejects.toMatchObject({
      code: 'SERVICE_UNAVAILABLE',
    });
  });
});
