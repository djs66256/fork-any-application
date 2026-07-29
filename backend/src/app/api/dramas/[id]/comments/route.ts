import { NextRequest, NextResponse } from 'next/server';
import {
  CommentListQuerySchema,
  CreateCommentRequestSchema,
  DramaCommentPathSchema,
} from '@/lib/schemas';
import { getAuth, requireAuthContext, resolveOptionalAuthContext } from '@/middleware/auth';
import { withErrorHandler } from '@/middleware/error-handler';
import { getCommentRepository, getDramaRepository } from '@/repositories/repository-registry';
import { CommentService } from '@/services/comment/comment.service';

type DramaCommentsRouteContext = {
  params: Promise<{ id: string }> | { id: string };
};

export const GET = withErrorHandler(async (request: NextRequest, context: unknown) => {
  const { id } = DramaCommentPathSchema.parse(
    await Promise.resolve((context as DramaCommentsRouteContext).params),
  );
  const { searchParams } = new URL(request.url);
  const query = CommentListQuerySchema.parse({
    page: searchParams.get('page') ?? undefined,
    pageSize: searchParams.get('pageSize') ?? undefined,
    sort: searchParams.get('sort') ?? undefined,
  });

  const service = new CommentService(getDramaRepository(), getCommentRepository());
  const authContext = await resolveOptionalAuthContext(request) ?? undefined;
  const result = await service.listByDrama({
    dramaId: id,
    ...query,
    userId: authContext?.userId,
  });

  return NextResponse.json(result);
});

export const POST = withErrorHandler(requireAuthContext(async (request: NextRequest, context: unknown) => {
  const { id } = DramaCommentPathSchema.parse(
    await Promise.resolve((context as DramaCommentsRouteContext).params),
  );
  const body = CreateCommentRequestSchema.parse(await request.json());
  const auth = getAuth(request);

  const service = new CommentService(getDramaRepository(), getCommentRepository());
  const result = await service.createComment({
    dramaId: id,
    userId: auth.userId,
    content: body.content,
  });

  return NextResponse.json(result);
}));
