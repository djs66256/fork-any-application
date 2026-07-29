import { api } from '@/lib/api-client';
import { MallProductsQuerySchema, MallProductsResponseSchema, type MallProductsQuery, type MallProductsResponse } from '@/lib/schemas';

export async function fetchMallProducts(query: MallProductsQuery): Promise<MallProductsResponse> {
  const normalizedQuery = MallProductsQuerySchema.parse(query);
  const response = await api.get('/api/mall/products', {
    params: {
      page: normalizedQuery.page,
      pageSize: normalizedQuery.pageSize,
    },
  });

  return MallProductsResponseSchema.parse(response);
}
