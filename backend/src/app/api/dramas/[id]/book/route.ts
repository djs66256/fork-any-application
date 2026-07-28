import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';
import { DramaService } from '@/services/drama/drama.service';
import { withErrorHandler } from '@/middleware/error-handler';
import { getAuth, requireAuthContext } from '@/middleware/auth';
import { DramaSupabaseRepository } from '@/repositories/supabase/drama.supabase.repository';

const BookDramaParamsSchema = z.object({
  id: z.string().uuid(),
});

type BookDramaRouteContext = {
  params: Promise<{ id: string }> | { id: string };
};

export const POST = withErrorHandler(requireAuthContext(async (request: NextRequest, context: unknown) => {
  const { id } = BookDramaParamsSchema.parse(await Promise.resolve((context as BookDramaRouteContext).params));
  const auth = getAuth(request);

  const repository = new DramaSupabaseRepository();
  const service = new DramaService(repository);
  const result = await service.bookDrama({
    dramaId: id,
    userId: auth.userId,
  });

  return NextResponse.json(result);
}));
