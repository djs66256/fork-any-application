import { describe, it, expect } from 'vitest';
import { MallMockRepository } from '@/repositories/mock/mall.mock.repository';
import { MallProduct } from '@/lib/schemas';

const sampleProducts: MallProduct[] = [
  {
    id: '650e8400-e29b-41d4-a716-446655449001',
    title: '样例商品 1',
    image_url: 'https://example.com/mall/products/sample-1.jpg',
    price: 19.9,
    tags: ['热卖'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655449002',
    title: '样例商品 2',
    image_url: 'https://example.com/mall/products/sample-2.jpg',
    price: 29.9,
    tags: [],
  },
];

describe('MallMockRepository', () => {
  it('should return the first page in stable seed order', async () => {
    const repository = new MallMockRepository();

    const result = await repository.listProducts({ page: 1, pageSize: 20 });

    expect(result.data).toHaveLength(20);
    expect(result.data[0]?.id).toBe('650e8400-e29b-41d4-a716-446655440001');
    expect(result.data[19]?.id).toBe('650e8400-e29b-41d4-a716-446655440020');
    expect(result.pagination).toEqual({
      page: 1,
      page_size: 20,
      total: 25,
      total_pages: 2,
    });
  });

  it('should return subsequent pages without duplicates', async () => {
    const repository = new MallMockRepository();

    const page1 = await repository.listProducts({ page: 1, pageSize: 20 });
    const page2 = await repository.listProducts({ page: 2, pageSize: 20 });

    expect(page2.data).toHaveLength(5);
    expect(page2.data[0]?.id).toBe('650e8400-e29b-41d4-a716-446655440021');

    const ids = new Set([...page1.data, ...page2.data].map((item) => item.id));
    expect(ids.size).toBe(25);
  });

  it('should return empty data for oversized pages while preserving pagination metadata', async () => {
    const repository = new MallMockRepository();

    const result = await repository.listProducts({ page: 999, pageSize: 20 });

    expect(result).toEqual({
      data: [],
      pagination: {
        page: 999,
        page_size: 20,
        total: 25,
        total_pages: 2,
      },
    });
  });

  it('should support empty repositories', async () => {
    const repository = new MallMockRepository([]);

    const result = await repository.listProducts({ page: 1, pageSize: 20 });

    expect(result).toEqual({
      data: [],
      pagination: {
        page: 1,
        page_size: 20,
        total: 0,
        total_pages: 0,
      },
    });
  });

  it('should clone returned products to avoid leaking mutable state', async () => {
    const repository = new MallMockRepository(sampleProducts);

    const result = await repository.listProducts({ page: 1, pageSize: 20 });
    result.data[0]?.tags.push('被污染');

    const secondRead = await repository.listProducts({ page: 1, pageSize: 20 });
    expect(secondRead.data[0]?.tags).toEqual(['热卖']);
  });
});
