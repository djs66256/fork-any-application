import { beforeEach, describe, expect, it } from 'vitest';
import { CommentMockRepository } from '@/repositories/mock/comment.mock.repository';

const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';
const EMPTY_DRAMA_ID = '550e8400-e29b-41d4-a716-446655449999';
const USER_ID = '770e8400-e29b-41d4-a716-446655440001';
const COMMENT_ID = '880e8400-e29b-41d4-a716-446655440001';

describe('CommentMockRepository', () => {
  let repository: CommentMockRepository;

  beforeEach(() => {
    repository = new CommentMockRepository();
  });

  it('should return latest-sorted paginated comments', async () => {
    const result = await repository.listByDrama({
      dramaId: DRAMA_ID,
      page: 1,
      pageSize: 2,
      sort: 'latest',
      userId: USER_ID,
    });

    expect(result.data).toHaveLength(2);
    expect(result.data.map((item) => item.id)).toEqual([
      '880e8400-e29b-41d4-a716-446655440001',
      '880e8400-e29b-41d4-a716-446655440002',
    ]);
    expect(result.pagination).toEqual({
      page: 1,
      page_size: 2,
      total: 3,
      total_pages: 2,
    });
  });

  it('should support hot sorting and preserve contract for oversized pages', async () => {
    const hot = await repository.listByDrama({
      dramaId: DRAMA_ID,
      page: 1,
      pageSize: 20,
      sort: 'hot',
      userId: USER_ID,
    });
    const oversized = await repository.listByDrama({
      dramaId: DRAMA_ID,
      page: 999,
      pageSize: 20,
      sort: 'hot',
      userId: USER_ID,
    });

    expect(hot.data.map((item) => item.id)).toEqual([
      '880e8400-e29b-41d4-a716-446655440002',
      '880e8400-e29b-41d4-a716-446655440001',
      '880e8400-e29b-41d4-a716-446655440003',
    ]);
    expect(oversized.data).toEqual([]);
    expect(oversized.pagination).toEqual({
      page: 999,
      page_size: 20,
      total: 3,
      total_pages: 1,
    });
  });

  it('should return empty list and pagination metadata for dramas without comments', async () => {
    const result = await repository.listByDrama({
      dramaId: EMPTY_DRAMA_ID,
      page: 1,
      pageSize: 20,
      sort: 'latest',
    });

    expect(result).toEqual({
      data: [],
      pagination: {
        page: 1,
        page_size: 20,
        total: 0,
        total_pages: 0,
      },
    });
  });

  it('should create comment with canonical defaults', async () => {
    const result = await repository.create({
      dramaId: DRAMA_ID,
      userId: USER_ID,
      content: '  hello world  ',
    });

    expect(result).toMatchObject({
      drama_id: DRAMA_ID,
      content: 'hello world',
      like_count: 0,
      liked: false,
      user: {
        id: USER_ID,
        display_name: '追剧达人',
      },
    });

    const list = await repository.listByDrama({
      dramaId: DRAMA_ID,
      page: 1,
      pageSize: 20,
      sort: 'latest',
      userId: USER_ID,
    });

    expect(list.data.some((item) => item.id === result.id)).toBe(true);
  });

  it('should toggle like idempotently', async () => {
    const first = await repository.toggleLike({
      dramaId: DRAMA_ID,
      commentId: COMMENT_ID,
      userId: USER_ID,
    });
    const second = await repository.toggleLike({
      dramaId: DRAMA_ID,
      commentId: COMMENT_ID,
      userId: USER_ID,
    });

    expect(first).toEqual({
      comment_id: COMMENT_ID,
      liked: true,
      like_count: 6,
    });
    expect(second).toEqual({
      comment_id: COMMENT_ID,
      liked: false,
      like_count: 5,
    });
  });

  it('should reject like toggles for missing comments or mismatched drama ids', async () => {
    await expect(
      repository.toggleLike({
        dramaId: DRAMA_ID,
        commentId: '880e8400-e29b-41d4-a716-446655449999',
        userId: USER_ID,
      }),
    ).rejects.toMatchObject({ code: 'COMMENT_NOT_FOUND' });

    await expect(
      repository.toggleLike({
        dramaId: '550e8400-e29b-41d4-a716-446655440002',
        commentId: COMMENT_ID,
        userId: USER_ID,
      }),
    ).rejects.toMatchObject({ code: 'COMMENT_NOT_FOUND' });
  });
});
