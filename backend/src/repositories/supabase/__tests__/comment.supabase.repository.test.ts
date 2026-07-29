import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: vi.fn(),
}));

describe('CommentSupabaseRepository', () => {
  let CommentSupabaseRepository: typeof import('../comment.supabase.repository').CommentSupabaseRepository;
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

    const mod = await import('../comment.supabase.repository');
    CommentSupabaseRepository = mod.CommentSupabaseRepository;
  });

  it('should map listByDrama rows and liked state to canonical contract', async () => {
    const commentsRows = [
      {
        id: '880e8400-e29b-41d4-a716-446655440001',
        drama_id: '550e8400-e29b-41d4-a716-446655440001',
        user_id: '770e8400-e29b-41d4-a716-446655440001',
        content: '评论正文',
        like_count: 3,
        created_at: '2026-07-29T09:30:00.000Z',
        updated_at: '2026-07-29T09:30:00.000Z',
        profiles: {
          id: '770e8400-e29b-41d4-a716-446655440001',
          display_name: '测试用户',
          avatar_url: null,
        },
      },
    ];

    const listQuery = {
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      range: vi.fn().mockReturnThis(),
    };
    listQuery.order.mockImplementation((column: string) => {
      if (column === 'created_at') {
        return Promise.resolve({
          data: commentsRows,
          error: null,
          count: 1,
        });
      }
      return listQuery;
    });
    const likesQuery = {
      eq: vi.fn().mockReturnThis(),
      in: vi.fn().mockResolvedValue({
        data: [{ comment_id: '880e8400-e29b-41d4-a716-446655440001' }],
        error: null,
      }),
    };

    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'comments') {
        return { select: vi.fn().mockReturnValue(listQuery) };
      }
      if (table === 'comment_likes') {
        return { select: vi.fn().mockReturnValue(likesQuery) };
      }
      throw new Error(`unexpected table ${table}`);
    });

    const repo = new CommentSupabaseRepository();
    const result = await repo.listByDrama({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      page: 1,
      pageSize: 20,
      sort: 'latest',
      userId: '770e8400-e29b-41d4-a716-446655440099',
    });

    expect(result).toEqual({
      data: [
        {
          id: '880e8400-e29b-41d4-a716-446655440001',
          drama_id: '550e8400-e29b-41d4-a716-446655440001',
          content: '评论正文',
          like_count: 3,
          liked: true,
          created_at: '2026-07-29T09:30:00.000Z',
          updated_at: '2026-07-29T09:30:00.000Z',
          user: {
            id: '770e8400-e29b-41d4-a716-446655440001',
            display_name: '测试用户',
            avatar_url: null,
          },
        },
      ],
      pagination: {
        page: 1,
        page_size: 20,
        total: 1,
        total_pages: 1,
      },
    });
  });

  it('should create comment and map canonical response', async () => {
    const insertQuery = {
      select: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: {
          id: '880e8400-e29b-41d4-a716-446655440010',
          drama_id: '550e8400-e29b-41d4-a716-446655440001',
          user_id: '770e8400-e29b-41d4-a716-446655440001',
          content: 'hello',
          like_count: 0,
          created_at: '2026-07-29T09:30:00.000Z',
          updated_at: '2026-07-29T09:30:00.000Z',
          profiles: {
            id: '770e8400-e29b-41d4-a716-446655440001',
            display_name: null,
            avatar_url: null,
          },
        },
        error: null,
      }),
    };

    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'comments') {
        return { insert: vi.fn().mockReturnValue(insertQuery) };
      }
      throw new Error(`unexpected table ${table}`);
    });

    const repo = new CommentSupabaseRepository();
    const result = await repo.create({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      userId: '770e8400-e29b-41d4-a716-446655440001',
      content: '  hello  ',
    });

    expect(result).toMatchObject({
      id: '880e8400-e29b-41d4-a716-446655440010',
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
      content: 'hello',
      like_count: 0,
      liked: false,
      user: {
        id: '770e8400-e29b-41d4-a716-446655440001',
        display_name: '用户',
        avatar_url: null,
      },
    });
  });

  it('should toggle likes and return updated count', async () => {
    const commentQuery = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: {
          id: '880e8400-e29b-41d4-a716-446655440001',
          drama_id: '550e8400-e29b-41d4-a716-446655440001',
          like_count: 3,
        },
        error: null,
      }),
    };
    const existingLikeQuery = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      maybeSingle: vi.fn().mockResolvedValue({
        data: null,
        error: null,
      }),
    };
    const insertLikeQuery = {
      insert: vi.fn().mockResolvedValue({ error: null }),
    };
    const updateCommentQuery = {
      eq: vi.fn().mockResolvedValue({ error: null }),
    };
    const commentsTable = {
      select: vi.fn().mockReturnValue(commentQuery),
      update: vi.fn().mockReturnValue(updateCommentQuery),
    };

    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'comments') {
        return commentsTable;
      }
      if (table === 'comment_likes') {
        return {
          select: vi.fn().mockReturnValue(existingLikeQuery),
          insert: insertLikeQuery.insert,
        };
      }
      throw new Error(`unexpected table ${table}`);
    });

    const repo = new CommentSupabaseRepository();
    const result = await repo.toggleLike({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      commentId: '880e8400-e29b-41d4-a716-446655440001',
      userId: '770e8400-e29b-41d4-a716-446655440099',
    });

    expect(result).toEqual({
      comment_id: '880e8400-e29b-41d4-a716-446655440001',
      liked: true,
      like_count: 4,
    });
  });

  it('should return COMMENT_NOT_FOUND when comment does not exist or mismatches drama', async () => {
    const missingQuery = {
      select: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({
        data: null,
        error: { message: 'row not found', code: 'PGRST116' },
      }),
    };
    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'comments') {
        return { select: vi.fn().mockReturnValue(missingQuery) };
      }
      throw new Error(`unexpected table ${table}`);
    });

    const repo = new CommentSupabaseRepository();
    await expect(
      repo.toggleLike({
        dramaId: '550e8400-e29b-41d4-a716-446655440001',
        commentId: '880e8400-e29b-41d4-a716-446655449999',
        userId: '770e8400-e29b-41d4-a716-446655440099',
      }),
    ).rejects.toMatchObject({ code: 'COMMENT_NOT_FOUND' });
  });

  it('should map availability failures to SERVICE_UNAVAILABLE', async () => {
    const listQuery = {
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      range: vi.fn().mockReturnThis(),
    };
    listQuery.order.mockImplementation((column: string) => {
      if (column === 'created_at') {
        return Promise.resolve({
          data: null,
          count: null,
          error: { message: 'network timeout while connecting', code: '08006' },
        });
      }
      return listQuery;
    });
    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'comments') {
        return { select: vi.fn().mockReturnValue(listQuery) };
      }
      throw new Error(`unexpected table ${table}`);
    });

    const repo = new CommentSupabaseRepository();
    await expect(
      repo.listByDrama({
        dramaId: '550e8400-e29b-41d4-a716-446655440001',
        page: 1,
        pageSize: 20,
        sort: 'latest',
      }),
    ).rejects.toMatchObject({ code: 'SERVICE_UNAVAILABLE' });
  });

  it('should wrap invalid rows as INTERNAL_ERROR', async () => {
    const listQuery = {
      eq: vi.fn().mockReturnThis(),
      order: vi.fn().mockReturnThis(),
      range: vi.fn().mockReturnThis(),
    };
    listQuery.order.mockImplementation((column: string) => {
      if (column === 'created_at') {
        return Promise.resolve({
          data: [
            {
              id: 'not-a-uuid',
              drama_id: '550e8400-e29b-41d4-a716-446655440001',
              user_id: '770e8400-e29b-41d4-a716-446655440001',
              content: 'bad row',
              like_count: 0,
              created_at: '2026-07-29T09:30:00.000Z',
              updated_at: '2026-07-29T09:30:00.000Z',
              profiles: null,
            },
          ],
          error: null,
          count: 1,
        });
      }
      return listQuery;
    });
    getSupabaseAdmin().from.mockImplementation((table: string) => {
      if (table === 'comments') {
        return { select: vi.fn().mockReturnValue(listQuery) };
      }
      throw new Error(`unexpected table ${table}`);
    });

    const repo = new CommentSupabaseRepository();
    await expect(
      repo.listByDrama({
        dramaId: '550e8400-e29b-41d4-a716-446655440001',
        page: 1,
        pageSize: 20,
        sort: 'latest',
      }),
    ).rejects.toMatchObject({ code: 'INTERNAL_ERROR' });
  });
});
