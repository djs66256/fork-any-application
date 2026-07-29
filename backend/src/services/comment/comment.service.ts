import {
  Comment,
  CommentListQuery,
  CommentListResponse,
  CommentListResponseSchema,
  CommentSchema,
  ToggleCommentLikeResponse,
  ToggleCommentLikeResponseSchema,
} from '@/lib/schemas';
import { Errors } from '@/lib/errors';
import { DramaRepositoryInterface } from '@/repositories/interfaces/drama.repository.interface';
import {
  CommentRepositoryInterface,
  CreateCommentParams,
  ToggleCommentLikeParams,
} from '@/repositories/interfaces/comment.repository.interface';

function isAppError(error: unknown): error is Error & { code: string } {
  return error instanceof Error && 'code' in error;
}

export class CommentService {
  constructor(
    private readonly dramaRepository: DramaRepositoryInterface,
    private readonly commentRepository: CommentRepositoryInterface,
  ) {}

  async listByDrama(input: {
    dramaId: string;
    page: CommentListQuery['page'];
    pageSize: CommentListQuery['pageSize'];
    sort: CommentListQuery['sort'];
    userId?: string;
  }): Promise<CommentListResponse> {
    await this.ensureDramaExists(input.dramaId);

    try {
      return CommentListResponseSchema.parse(await this.commentRepository.listByDrama(input));
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.internal('Invalid comment list result');
    }
  }

  async createComment(input: CreateCommentParams): Promise<Comment> {
    await this.ensureDramaExists(input.dramaId);

    try {
      return CommentSchema.parse(await this.commentRepository.create(input));
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.internal('Invalid comment creation result');
    }
  }

  async toggleLike(input: ToggleCommentLikeParams): Promise<ToggleCommentLikeResponse> {
    await this.ensureDramaExists(input.dramaId);

    try {
      return ToggleCommentLikeResponseSchema.parse(await this.commentRepository.toggleLike(input));
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.internal('Invalid comment like toggle result');
    }
  }

  private async ensureDramaExists(dramaId: string): Promise<void> {
    const drama = await this.dramaRepository.findById(dramaId);
    if (!drama) {
      throw Errors.dramaNotFound(dramaId);
    }
  }
}
