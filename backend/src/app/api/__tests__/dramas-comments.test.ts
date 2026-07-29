import { beforeEach, describe, expect, it } from 'vitest';
import { NextRequest } from 'next/server';
import {
  resetRepositoryRegistry,
  setCommentRepository,
  setDramaRepository,
} from '@/repositories/repository-registry';
import { CommentMockRepository } from '@/repositories/mock/comment.mock.repository';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { Errors } from '@/lib/errors';
import type {
  CommentRepositoryInterface,
  ToggleCommentLikeParams,
} from '@/repositories/interfaces/comment.repository.interface';
import type { Comment, CommentListResponse, ToggleCommentLikeResponse } from '@/lib/schemas';

const { GET, POST } = await import('../dramas/[id]/comments/route');
const { POST: POST_LIKE } = await import('../dramas/[id]/comments/[commentId]/like/route');

const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';
const COMMENT_ID = '880e8400-e29b-41d4-a716-446655440001';
const USER_ID = '770e8400-e29b-41d4-a716-446655440001';

class MissingCommentRepository implements CommentRepositoryInterface {
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

describe('comments routes', () => {
  beforeEach(() => {
    resetRepositoryRegistry();
    setDramaRepository(new DramaMockRepository());
    setCommentRepository(new CommentMockRepository());
  });

  it('should return canonical comments list with default query params', async () => {
    const request = new NextRequest(`https://example.com/api/dramas/${DRAMA_ID}/comments`);
    const response = await GET(request, {
      params: Promise.resolve({ id: DRAMA_ID }),
    });
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toHaveLength(3);
    expect(body.pagination).toEqual({
      page: 1,
      page_size: 20,
      total: 3,
      total_pages: 1,
    });
    expect(body.data[0]).toMatchObject({
      id: COMMENT_ID,
      drama_id: DRAMA_ID,
      liked: false,
    });
  });

  it('should support authenticated liked state and sort=hot', async () => {
    const request = new NextRequest(
      `https://example.com/api/dramas/${DRAMA_ID}/comments?sort=hot&page=1&pageSize=2`,
      {
        headers: {
          'x-user-id': USER_ID,
        },
      },
    );
    const response = await GET(request, {
      params: Promise.resolve({ id: DRAMA_ID }),
    });
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toHaveLength(2);
    expect(body.data[0].id).toBe('880e8400-e29b-41d4-a716-446655440002');
    expect(body.data[0].liked).toBe(true);
    expect(body.data[1].id).toBe('880e8400-e29b-41d4-a716-446655440001');
  });

  it('should return 400 for invalid query or path params', async () => {
    const invalidQueryRequest = new NextRequest(
      `https://example.com/api/dramas/${DRAMA_ID}/comments?page=0&pageSize=99&sort=foo`,
    );
    const invalidQueryResponse = await GET(invalidQueryRequest, {
      params: Promise.resolve({ id: DRAMA_ID }),
    });
    const invalidQueryBody = await invalidQueryResponse.json();

    expect(invalidQueryResponse.status).toBe(400);
    expect(invalidQueryBody.error.code).toBe('VALIDATION_ERROR');

    const invalidPathRequest = new NextRequest('https://example.com/api/dramas/not-a-uuid/comments');
    const invalidPathResponse = await GET(invalidPathRequest, {
      params: Promise.resolve({ id: 'not-a-uuid' }),
    });
    const invalidPathBody = await invalidPathResponse.json();

    expect(invalidPathResponse.status).toBe(400);
    expect(invalidPathBody.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return 404 when drama does not exist', async () => {
    const request = new NextRequest('https://example.com/api/dramas/550e8400-e29b-41d4-a716-446655449999/comments');
    const response = await GET(request, {
      params: Promise.resolve({ id: '550e8400-e29b-41d4-a716-446655449999' }),
    });
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error.code).toBe('DRAMA_NOT_FOUND');
  });

  it('should create comments for authenticated users', async () => {
    const request = new NextRequest(`https://example.com/api/dramas/${DRAMA_ID}/comments`, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-user-id': USER_ID,
      },
      body: JSON.stringify({ content: '  新评论  ' }),
    });

    const response = await POST(request, {
      params: Promise.resolve({ id: DRAMA_ID }),
    });
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toMatchObject({
      drama_id: DRAMA_ID,
      content: '新评论',
      like_count: 0,
      liked: false,
    });
  });

  it('should return 401 for anonymous create and like requests', async () => {
    const createRequest = new NextRequest(`https://example.com/api/dramas/${DRAMA_ID}/comments`, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
      },
      body: JSON.stringify({ content: '新评论' }),
    });
    const createResponse = await POST(createRequest, {
      params: Promise.resolve({ id: DRAMA_ID }),
    });
    const createBody = await createResponse.json();

    expect(createResponse.status).toBe(401);
    expect(createBody.error.code).toBe('UNAUTHORIZED');

    const likeRequest = new NextRequest(
      `https://example.com/api/dramas/${DRAMA_ID}/comments/${COMMENT_ID}/like`,
      { method: 'POST' },
    );
    const likeResponse = await POST_LIKE(likeRequest, {
      params: Promise.resolve({ id: DRAMA_ID, commentId: COMMENT_ID }),
    });
    const likeBody = await likeResponse.json();

    expect(likeResponse.status).toBe(401);
    expect(likeBody.error.code).toBe('UNAUTHORIZED');
  });

  it('should return 400 for invalid create body and like path', async () => {
    const createRequest = new NextRequest(`https://example.com/api/dramas/${DRAMA_ID}/comments`, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-user-id': USER_ID,
      },
      body: JSON.stringify({ content: '   ' }),
    });
    const createResponse = await POST(createRequest, {
      params: Promise.resolve({ id: DRAMA_ID }),
    });
    const createBody = await createResponse.json();

    expect(createResponse.status).toBe(400);
    expect(createBody.error.code).toBe('VALIDATION_ERROR');

    const likeRequest = new NextRequest('https://example.com/api/dramas/bad/comments/bad/like', {
      method: 'POST',
      headers: {
        'x-user-id': USER_ID,
      },
    });
    const likeResponse = await POST_LIKE(likeRequest, {
      params: Promise.resolve({ id: 'bad', commentId: 'bad' }),
    });
    const likeBody = await likeResponse.json();

    expect(likeResponse.status).toBe(400);
    expect(likeBody.error.code).toBe('VALIDATION_ERROR');
  });

  it('should toggle likes and return canonical response', async () => {
    const request = new NextRequest(
      `https://example.com/api/dramas/${DRAMA_ID}/comments/${COMMENT_ID}/like`,
      {
        method: 'POST',
        headers: {
          'x-user-id': USER_ID,
        },
      },
    );

    const response = await POST_LIKE(request, {
      params: Promise.resolve({ id: DRAMA_ID, commentId: COMMENT_ID }),
    });
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      comment_id: COMMENT_ID,
      liked: true,
      like_count: 6,
    });
  });

  it('should return 404 when target comment does not exist', async () => {
    setCommentRepository(new MissingCommentRepository());

    const request = new NextRequest(
      `https://example.com/api/dramas/${DRAMA_ID}/comments/880e8400-e29b-41d4-a716-446655449999/like`,
      {
        method: 'POST',
        headers: {
          'x-user-id': USER_ID,
        },
      },
    );

    const response = await POST_LIKE(request, {
      params: Promise.resolve({
        id: DRAMA_ID,
        commentId: '880e8400-e29b-41d4-a716-446655449999',
      }),
    });
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error.code).toBe('COMMENT_NOT_FOUND');
  });
});
