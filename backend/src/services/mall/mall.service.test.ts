import { describe, it, expect } from 'vitest';
import { Errors } from '@/lib/errors';
import { MallProduct } from '@/lib/schemas';
import { MallPaginatedResult, MallRepositoryInterface } from '@/repositories/interfaces/mall.repository.interface';
import { MallService } from './mall.service';

const validMallProduct: MallProduct = {
  id: '650e8400-e29b-41d4-a716-446655440001',
  title: '轻奢真丝睡衣礼盒',
  image_url: 'https://example.com/mall/products/pajama-gift-box.jpg',
  price: 199,
  tags: ['热卖', '包邮'],
};

class StubMallRepository implements MallRepositoryInterface {
  constructor(private readonly result: MallPaginatedResult<MallProduct>) {}

  async listProducts(): Promise<MallPaginatedResult<MallProduct>> {
    return this.result;
  }
}

class InvalidMallRepository implements MallRepositoryInterface {
  async listProducts(): Promise<MallPaginatedResult<MallProduct>> {
    return {
      data: [
        {
          ...validMallProduct,
          id: 'invalid-uuid',
        } as unknown as MallProduct,
      ],
      pagination: {
        page: 1,
        page_size: 20,
        total: 1,
        total_pages: 1,
      },
    };
  }
}

class AppErrorMallRepository implements MallRepositoryInterface {
  async listProducts(): Promise<MallPaginatedResult<MallProduct>> {
    throw Errors.serviceUnavailable('mall-products');
  }
}

describe('MallService', () => {
  it('should return validated mall products for valid repository output', async () => {
    const service = new MallService(
      new StubMallRepository({
        data: [validMallProduct],
        pagination: {
          page: 1,
          page_size: 20,
          total: 1,
          total_pages: 1,
        },
      }),
    );

    const result = await service.listProducts({ page: 1, pageSize: 20 });

    expect(result).toEqual({
      data: [validMallProduct],
      pagination: {
        page: 1,
        page_size: 20,
        total: 1,
        total_pages: 1,
      },
    });
  });

  it('should wrap invalid repository output as internal error', async () => {
    const service = new MallService(new InvalidMallRepository());

    await expect(service.listProducts({ page: 1, pageSize: 20 })).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
      message: 'Invalid mall products result',
    });
  });

  it('should rethrow AppError instances from repository', async () => {
    const service = new MallService(new AppErrorMallRepository());

    await expect(service.listProducts({ page: 1, pageSize: 20 })).rejects.toMatchObject({
      code: 'SERVICE_UNAVAILABLE',
    });
  });
});
