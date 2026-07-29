import { describe, it, expect, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';
import { GET } from '../mall/products/route';
import { resetRepositoryRegistry, setMallRepository } from '@/repositories/repository-registry';
import { MallProduct } from '@/lib/schemas';
import { MallPaginatedResult, MallRepositoryInterface } from '@/repositories/interfaces/mall.repository.interface';

const validMallProduct: MallProduct = {
  id: '650e8400-e29b-41d4-a716-446655440001',
  title: '轻奢真丝睡衣礼盒',
  image_url: 'https://example.com/mall/products/pajama-gift-box.jpg',
  price: 199,
  tags: ['热卖', '包邮'],
};

class ThrowingMallRepository implements MallRepositoryInterface {
  async listProducts(): Promise<MallPaginatedResult<MallProduct>> {
    throw new Error('boom');
  }
}

class EmptyMallRepository implements MallRepositoryInterface {
  async listProducts(params: { page: number; pageSize: number }): Promise<MallPaginatedResult<MallProduct>> {
    return {
      data: [],
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total: 0,
        total_pages: 0,
      },
    };
  }
}

class SingleProductMallRepository implements MallRepositoryInterface {
  async listProducts(params: { page: number; pageSize: number }): Promise<MallPaginatedResult<MallProduct>> {
    return {
      data: [validMallProduct],
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total: 1,
        total_pages: 1,
      },
    };
  }
}

describe('GET /api/mall/products', () => {
  beforeEach(() => {
    resetRepositoryRegistry();
  });

  it('should return default pagination results', async () => {
    const request = new NextRequest('https://example.com/api/mall/products');

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toHaveLength(20);
    expect(body.pagination).toEqual({
      page: 1,
      page_size: 20,
      total: 25,
      total_pages: 2,
    });
  });

  it('should return repository data with canonical response contract', async () => {
    setMallRepository(new SingleProductMallRepository());
    const request = new NextRequest('https://example.com/api/mall/products?page=1&pageSize=20');

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      data: [validMallProduct],
      pagination: {
        page: 1,
        page_size: 20,
        total: 1,
        total_pages: 1,
      },
    });
  });

  it('should reject invalid pagination params with validation error', async () => {
    const request = new NextRequest('https://example.com/api/mall/products?page=0&pageSize=101');

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return empty data for oversized pages while preserving pagination', async () => {
    const request = new NextRequest('https://example.com/api/mall/products?page=999&pageSize=20');

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      data: [],
      pagination: {
        page: 999,
        page_size: 20,
        total: 25,
        total_pages: 2,
      },
    });
  });

  it('should support empty state responses', async () => {
    setMallRepository(new EmptyMallRepository());
    const request = new NextRequest('https://example.com/api/mall/products');

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      data: [],
      pagination: {
        page: 1,
        page_size: 20,
        total: 0,
        total_pages: 0,
      },
    });
  });

  it('should return internal error when service throws unexpectedly', async () => {
    setMallRepository(new ThrowingMallRepository());
    const request = new NextRequest('https://example.com/api/mall/products');

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
