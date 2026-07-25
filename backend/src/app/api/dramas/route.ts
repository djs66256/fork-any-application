import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';
import { DramaService } from '@/services/drama/drama.service';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
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

  const repository = new DramaMockRepository();
  const service = new DramaService(repository);
  const result = await service.listDramas({ page, pageSize });

  return NextResponse.json(result);
});

export const POST = withErrorHandler(async () => {
  throw Errors.notImplemented('POST /api/dramas not implemented');
});
