import { describe, it, expect, beforeEach } from 'vitest';
import { CommentService } from './comment.service';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { CommentMockRepository } from '@/repositories/mock/comment.mock.repository';
import { Errors } from '@/lib/errors';
import type {
  CommentRepositoryInterface,
  ToggleCommentLikeParams,
} from '@/repositories/interfaces/comment.repository.interface';
import type { Comment, CommentListResponse, ToggleCommentLikeResponse } from '@/lib/schemas';

const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';
const MISSING_DRAMA_ID = '550e8400-e29b-41d4-a716-446655449999';
const COMMENT_ID = '880e8400-e29b-41d4-a716-446655440001';
const USER_ID = '770e8400-e29b-41d4-a716-446655440001';

class InvalidListCommentRepository implements CommentRepositoryInterface {
  async listByDrama(): Promise<CommentListResponse> {
    return {
      data: [
        {
          id: 'not-a-uuid',
          drama_id: DRAMA_ID,
          content: 'invalid',
          like_count: 0,
          liked: false,
          created_at: '2026-07-29T09:30:00.000Z',
          updated_at: '2026-07-29T09:30:00.000Z',
          user: {
            id: USER_ID,
            display_name: '测试用户',
            avatar_url: null,
          },
        } as unknown as Comment,
      ],
      pagination: {
        page: 1,
        page_size: 20,
        total: 1,
        total_pages: 1,
      },
    };
  }

  async create(): Promise<Comment> {
    throw new Error('Method not implemented.');
  }

  async toggleLike(): Promise<ToggleCommentLikeResponse> {
    throw new Error('Method not implemented.');
  }
}

class InvalidCreateCommentRepository implements CommentRepositoryInterface {
  async listByDrama(): Promise<CommentListResponse> {
    throw new Error('Method not implemented.');
  }

  async create(): Promise<Comment> {
    return {
      id: 'not-a-uuid',
      drama_id: DRAMA_ID,
      content: 'hello',
      like_count: 0,
      liked: false,
      created_at: '2026-07-29T09:30:00.000Z',
      updated_at: '2026-07-29T09:30:00.000Z',
      user: {
        id: USER_ID,
        display_name: '测试用户',
        avatar_url: null,
      },
    } as unknown as Comment;
  }

  async toggleLike(): Promise<ToggleCommentLikeResponse> {
    throw new Error('Method not implemented.');
  }
}

class InvalidToggleCommentRepository implements CommentRepositoryInterface {
  async listByDrama(): Promise<CommentListResponse> {
    throw new Error('Method not implemented.');
  }

  async create(): Promise<Comment> {
    throw new Error('Method not implemented.');
  }

  async toggleLike(): Promise<ToggleCommentLikeResponse> {
    return {
      comment_id: 'not-a-uuid',
      liked: true,
      like_count: 1,
    } as unknown as ToggleCommentLikeResponse;
  }
}

class CommentNotFoundRepository implements CommentRepositoryInterface {
  async listByDrama(): Promise<CommentListResponse> {
    throw new Error('Method not implemented.');
  }

  async create(): Promise<Comment> {
    throw new Error('Method not implemented.');
  }

  async toggleLike(params: ToggleCommentLikeParams): Promise<ToggleCommentLikeResponse> {
    throw Errors.commentNotFound(params.commentId);
  }
}

describe('CommentService', () => {
  let service: CommentService;

  beforeEach(() => {
    service = new CommentService(new DramaMockRepository(), new CommentMockRepository());
  });

  it('should list comments with canonical pagination', async () => {
    const result = await service.listByDrama({
      dramaId: DRAMA_ID,
      page: 1,
      pageSize: 20,
      sort: 'latest',
      userId: USER_ID,
    });

    expect(result.data.length).toBeGreaterThan(0);
    expect(result.pagination).toEqual({
      page: 1,
      page_size: 20,
      total: 3,
      total_pages: 1,
    });
    expect(result.data[0]?.liked).toBe(false);
  });

  it('should create comment and return canonical comment object', async () => {
    const result = await service.createComment({
      dramaId: DRAMA_ID,
      userId: USER_ID,
      content: '  新评论  ',
    });

    expect(result).toMatchObject({
      drama_id: DRAMA_ID,
      content: '新评论',
      like_count: 0,
      liked: false,
      user: {
        id: USER_ID,
      },
    });
  });

  it('should toggle like and return latest like state', async () => {
    const first = await service.toggleLike({
      dramaId: DRAMA_ID,
      commentId: COMMENT_ID,
      userId: USER_ID,
    });
    const second = await service.toggleLike({
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

  it('should throw DRAMA_NOT_FOUND when drama does not exist', async () => {
    await expect(
      service.listByDrama({
        dramaId: MISSING_DRAMA_ID,
        page: 1,
        pageSize: 20,
        sort: 'latest',
      }),
    ).rejects.toMatchObject({ code: 'DRAMA_NOT_FOUND' });
  });

  it('should propagate COMMENT_NOT_FOUND from repository', async () => {
    const missingCommentService = new CommentService(
      new DramaMockRepository(),
      new CommentNotFoundRepository(),
    );

    await expect(
      missingCommentService.toggleLike({
        dramaId: DRAMA_ID,
        commentId: '880e8400-e29b-41d4-a716-446655449999',
        userId: USER_ID,
      }),
    ).rejects.toMatchObject({ code: 'COMMENT_NOT_FOUND' });
  });

  it('should wrap invalid list results as INTERNAL_ERROR', async () => {
    const invalidService = new CommentService(
      new DramaMockRepository(),
      new InvalidListCommentRepository(),
    );

    await expect(
      invalidService.listByDrama({
        dramaId: DRAMA_ID,
        page: 1,
        pageSize: 20,
        sort: 'latest',
      }),
    ).rejects.toMatchObject({ code: 'INTERNAL_ERROR' });
  });

  it('should wrap invalid create results as INTERNAL_ERROR', async () => {
    const invalidService = new CommentService(
      new DramaMockRepository(),
      new InvalidCreateCommentRepository(),
    );

    await expect(
      invalidService.createComment({
        dramaId: DRAMA_ID,
        userId: USER_ID,
        content: 'hello',
      }),
    ).rejects.toMatchObject({ code: 'INTERNAL_ERROR' });
  });

  it('should wrap invalid toggle results as INTERNAL_ERROR', async () => {
    const invalidService = new CommentService(
      new DramaMockRepository(),
      new InvalidToggleCommentRepository(),
    );

    await expect(
      invalidService.toggleLike({
        dramaId: DRAMA_ID,
        commentId: COMMENT_ID,
        userId: USER_ID,
      }),
    ).rejects.toMatchObject({ code: 'INTERNAL_ERROR' });
  });
});
