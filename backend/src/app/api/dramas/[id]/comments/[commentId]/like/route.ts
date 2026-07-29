import { NextRequest, NextResponse } from 'next/server';
import { DramaCommentLikePathSchema } from '@/lib/schemas';
import { getAuth, requireAuthContext } from '@/middleware/auth';
import { withErrorHandler } from '@/middleware/error-handler';
import { getCommentRepository, getDramaRepository } from '@/repositories/repository-registry';
import { CommentService } from '@/services/comment/comment.service';

type DramaCommentLikeRouteContext = {
  params: Promise<{ id: string; commentId: string }> | { id: string; commentId: string };
};

export const POST = withErrorHandler(requireAuthContext(async (request: NextRequest, context: unknown) => {
  const { id, commentId } = DramaCommentLikePathSchema.parse(
    await Promise.resolve((context as DramaCommentLikeRouteContext).params),
  );
  const auth = getAuth(request);

  const service = new CommentService(getDramaRepository(), getCommentRepository());
  const result = await service.toggleLike({
    dramaId: id,
    commentId,
    userId: auth.userId,
  });

  return NextResponse.json(result);
}));
