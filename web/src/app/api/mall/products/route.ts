import { NextResponse } from 'next/server';
import { MallProductsQuerySchema } from '@/lib/schemas';
import { buildMallStableFeed } from '@/features/mall/config/mall-seed';

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const query = MallProductsQuerySchema.parse({
    page: searchParams.get('page') ?? 1,
    pageSize: searchParams.get('pageSize') ?? 20,
  });

  const data = buildMallStableFeed([]);

  return NextResponse.json({
    data,
    pagination: {
      page: query.page,
      page_size: query.pageSize,
      total: data.length,
      total_pages: 1,
    },
  });
}
