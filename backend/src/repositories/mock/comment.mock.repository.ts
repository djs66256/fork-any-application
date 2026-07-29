import {
  Comment,
  CommentListResponse,
  CommentSchema,
  CommentSort,
  CommentUserSummary,
  CommentUserSummarySchema,
  ToggleCommentLikeResponse,
  ToggleCommentLikeResponseSchema,
} from '@/lib/schemas';
import { Errors } from '@/lib/errors';
import {
  CommentRepositoryInterface,
  CreateCommentParams,
  ListDramaCommentsParams,
  ToggleCommentLikeParams,
} from '@/repositories/interfaces/comment.repository.interface';

type CommentRecord = Omit<Comment, 'liked'>;

type UserSeed = {
  id: string;
  display_name: string;
  avatar_url: string | null;
};

const USER_SEEDS = new Map<string, UserSeed>([
  [
    '770e8400-e29b-41d4-a716-446655440001',
    {
      id: '770e8400-e29b-41d4-a716-446655440001',
      display_name: '追剧达人',
      avatar_url: 'https://example.com/avatars/user-1.png',
    },
  ],
  [
    '770e8400-e29b-41d4-a716-446655440002',
    {
      id: '770e8400-e29b-41d4-a716-446655440002',
      display_name: '吃瓜群众',
      avatar_url: null,
    },
  ],
  [
    '770e8400-e29b-41d4-a716-446655440003',
    {
      id: '770e8400-e29b-41d4-a716-446655440003',
      display_name: '沙发王',
      avatar_url: 'https://example.com/avatars/user-3.png',
    },
  ],
]);

const COMMENT_SEEDS: CommentRecord[] = [
  {
    id: '880e8400-e29b-41d4-a716-446655440001',
    drama_id: '550e8400-e29b-41d4-a716-446655440001',
    content: '这剧情反转太上头了。',
    like_count: 5,
    created_at: '2026-07-29T09:30:00.000Z',
    updated_at: '2026-07-29T09:30:00.000Z',
    user: USER_SEEDS.get('770e8400-e29b-41d4-a716-446655440001')!,
  },
  {
    id: '880e8400-e29b-41d4-a716-446655440002',
    drama_id: '550e8400-e29b-41d4-a716-446655440001',
    content: '女主这波打脸太爽了。',
    like_count: 12,
    created_at: '2026-07-29T09:20:00.000Z',
    updated_at: '2026-07-29T09:20:00.000Z',
    user: USER_SEEDS.get('770e8400-e29b-41d4-a716-446655440002')!,
  },
  {
    id: '880e8400-e29b-41d4-a716-446655440003',
    drama_id: '550e8400-e29b-41d4-a716-446655440001',
    content: '求快点更新下一集。',
    like_count: 1,
    created_at: '2026-07-29T09:10:00.000Z',
    updated_at: '2026-07-29T09:10:00.000Z',
    user: USER_SEEDS.get('770e8400-e29b-41d4-a716-446655440003')!,
  },
  {
    id: '880e8400-e29b-41d4-a716-446655440004',
    drama_id: '550e8400-e29b-41d4-a716-446655440002',
    content: '前夫哥终于开窍了。',
    like_count: 3,
    created_at: '2026-07-29T08:00:00.000Z',
    updated_at: '2026-07-29T08:00:00.000Z',
    user: USER_SEEDS.get('770e8400-e29b-41d4-a716-446655440001')!,
  },
];

const COMMENT_LIKE_SEEDS: Array<{ commentId: string; userId: string }> = [
  { commentId: '880e8400-e29b-41d4-a716-446655440001', userId: '770e8400-e29b-41d4-a716-446655440002' },
  { commentId: '880e8400-e29b-41d4-a716-446655440001', userId: '770e8400-e29b-41d4-a716-446655440003' },
  { commentId: '880e8400-e29b-41d4-a716-446655440002', userId: '770e8400-e29b-41d4-a716-446655440001' },
];

function cloneUser(user: CommentUserSummary): CommentUserSummary {
  return CommentUserSummarySchema.parse({ ...user });
}

function cloneCommentRecord(comment: CommentRecord): CommentRecord {
  return {
    ...comment,
    user: cloneUser(comment.user),
  };
}

function computeTotalPages(total: number, pageSize: number): number {
  return total === 0 ? 0 : Math.ceil(total / pageSize);
}

function sortComments(comments: CommentRecord[], sort: CommentSort): CommentRecord[] {
  return [...comments].sort((left, right) => {
    if (sort === 'hot') {
      const likeDifference = right.like_count - left.like_count;
      if (likeDifference !== 0) {
        return likeDifference;
      }
    }

    return right.created_at.localeCompare(left.created_at);
  });
}

export class CommentMockRepository implements CommentRepositoryInterface {
  private readonly comments = new Map<string, CommentRecord>();

  private readonly likes = new Set<string>();

  private readonly users = new Map<string, UserSeed>();

  constructor(
    initialComments: CommentRecord[] = COMMENT_SEEDS,
    initialLikes: Array<{ commentId: string; userId: string }> = COMMENT_LIKE_SEEDS,
  ) {
    initialComments.forEach((comment) => {
      this.comments.set(comment.id, cloneCommentRecord(comment));
    });
    initialLikes.forEach((like) => {
      this.likes.add(this.buildLikeKey(like.commentId, like.userId));
    });
    USER_SEEDS.forEach((user, id) => {
      this.users.set(id, { ...user });
    });
  }

  async listByDrama(params: ListDramaCommentsParams): Promise<CommentListResponse> {
    const filtered = Array.from(this.comments.values()).filter((comment) => comment.drama_id === params.dramaId);
    const sorted = sortComments(filtered, params.sort);
    const start = (params.page - 1) * params.pageSize;
    const total = sorted.length;
    const data = sorted.slice(start, start + params.pageSize).map((comment) => this.toComment(comment, params.userId));

    return {
      data,
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: computeTotalPages(total, params.pageSize),
      },
    };
  }

  async create(params: CreateCommentParams): Promise<Comment> {
    const now = new Date().toISOString();
    const user = this.getOrCreateUser(params.userId);
    const comment = cloneCommentRecord({
      id: crypto.randomUUID(),
      drama_id: params.dramaId,
      content: params.content.trim(),
      like_count: 0,
      created_at: now,
      updated_at: now,
      user,
    });

    this.comments.set(comment.id, comment);
    return this.toComment(comment, params.userId);
  }

  async toggleLike(params: ToggleCommentLikeParams): Promise<ToggleCommentLikeResponse> {
    const comment = this.comments.get(params.commentId);
    if (!comment || comment.drama_id !== params.dramaId) {
      throw Errors.commentNotFound(params.commentId);
    }

    const key = this.buildLikeKey(params.commentId, params.userId);
    const liked = !this.likes.has(key);

    if (liked) {
      this.likes.add(key);
      comment.like_count += 1;
    } else {
      this.likes.delete(key);
      comment.like_count = Math.max(0, comment.like_count - 1);
    }

    comment.updated_at = new Date().toISOString();

    return ToggleCommentLikeResponseSchema.parse({
      comment_id: params.commentId,
      liked,
      like_count: comment.like_count,
    });
  }

  private toComment(comment: CommentRecord, userId?: string): Comment {
    return CommentSchema.parse({
      ...cloneCommentRecord(comment),
      liked: userId ? this.likes.has(this.buildLikeKey(comment.id, userId)) : false,
    });
  }

  private buildLikeKey(commentId: string, userId: string): string {
    return `${commentId}:${userId}`;
  }

  private getOrCreateUser(userId: string): CommentUserSummary {
    const user = this.users.get(userId);
    if (user) {
      return cloneUser(user);
    }

    const createdUser: UserSeed = {
      id: userId,
      display_name: '用户',
      avatar_url: null,
    };
    this.users.set(userId, createdUser);
    return cloneUser(createdUser);
  }
}
