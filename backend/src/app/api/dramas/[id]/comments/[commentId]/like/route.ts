import { NextRequest, NextResponse } from 'next/server';
import { DramaCommentLikePathSchema } from '@/lib/schemas';
import { getAuthenticatedUserId } from '@/middleware/auth';
import { withErrorHandler } from '@/middleware/error-handler';
import { getCommentRepository, getDramaRepository } from '@/repositories/repository-registry';
import { CommentService } from '@/services/comment/comment.service';

type DramaCommentLikeRouteContext = {
  params: Promise<{ id: string; commentId: string }> | { id: string; commentId: string };
};

export const POST = withErrorHandler(async (request: NextRequest, context: unknown) => {
  const { id, commentId } = DramaCommentLikePathSchema.parse(
    await Promise.resolve((context as DramaCommentLikeRouteContext).params),
  );

  const service = new CommentService(getDramaRepository(), getCommentRepository());
  const result = await service.toggleLike({
    dramaId: id,
    commentId,
    userId: getAuthenticatedUserId(request),
  });

  return NextResponse.json(result);
});
