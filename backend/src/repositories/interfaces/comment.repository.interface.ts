import {
  Comment,
  CommentListResponse,
  CommentSort,
  ToggleCommentLikeResponse,
} from '@/lib/schemas';

export interface CommentPaginationParams {
  page: number;
  pageSize: number;
}

export interface ListDramaCommentsParams extends CommentPaginationParams {
  dramaId: string;
  sort: CommentSort;
  userId?: string;
}

export interface CreateCommentParams {
  dramaId: string;
  userId: string;
  content: string;
}

export interface ToggleCommentLikeParams {
  dramaId: string;
  commentId: string;
  userId: string;
}

export interface CommentRepositoryInterface {
  listByDrama(params: ListDramaCommentsParams): Promise<CommentListResponse>;
  create(params: CreateCommentParams): Promise<Comment>;
  toggleLike(params: ToggleCommentLikeParams): Promise<ToggleCommentLikeResponse>;
}
