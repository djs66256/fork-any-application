import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';
import { DramaService } from '@/services/drama/drama.service';
import { getDramaRepository } from '@/repositories/repository-registry';
import { withErrorHandler } from '@/middleware/error-handler';
import { Errors } from '@/lib/errors';

const PaginationQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(10),
});

export const GET = withErrorHandler(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const { page, pageSize } = PaginationQuerySchema.parse({
    page: searchParams.get('page') ?? undefined,
    pageSize: searchParams.get('pageSize') ?? undefined,
  });

  const service = new DramaService(getDramaRepository());
  const result = await service.listDramas({ page, pageSize });

  return NextResponse.json(result);
});

export const POST = withErrorHandler(async () => {
  throw Errors.notImplemented('POST /api/dramas not implemented');
});
