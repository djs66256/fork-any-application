import { MallProduct, MallProductsQuery } from '@/lib/schemas';

export interface MallPaginatedResult<T> {
  data: T[];
  pagination: {
    page: number;
    page_size: number;
    total: number;
    total_pages: number;
  };
}

export interface MallRepositoryInterface {
  listProducts(params: MallProductsQuery): Promise<MallPaginatedResult<MallProduct>>;
}
