import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { MallProductsQuerySchema } from '@/lib/schemas';
import { getMallRepository } from '@/repositories/repository-registry';
import { MallService } from '@/services/mall/mall.service';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const query = MallProductsQuerySchema.parse({
    page: searchParams.get('page') ?? undefined,
    pageSize: searchParams.get('pageSize') ?? undefined,
  });

  const service = new MallService(getMallRepository());
  const result = await service.listProducts(query);

  return NextResponse.json(result);
});
