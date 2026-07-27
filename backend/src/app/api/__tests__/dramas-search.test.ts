import { describe, it, expect, vi } from 'vitest';
import { NextRequest } from 'next/server';

vi.mock('@/services/drama/drama.service', () => ({
  DramaService: vi.fn().mockImplementation(() => ({
    searchDramas: vi.fn().mockResolvedValue({
      data: [
        {
          id: '550e8400-e29b-41d4-a716-446655440001',
          title: '逆袭归来后我成了豪门团宠',
          description: '落魄千金重回豪门，在误会与守护中逆风翻盘。',
          cover_url: 'https://example.com/dramas/001.jpg',
          category: '都市',
          episode_count: 68,
          tags: ['逆袭', '豪门'],
          rating: 8.9,
          created_at: '2026-07-25T00:00:00Z',
          updated_at: '2026-07-25T00:00:00Z',
        },
      ],
      pagination: {
        page: 1,
        page_size: 10,
        total: 1,
        total_pages: 1,
      },
    }),
  })),
}));

const { GET } = await import('../dramas/search/route');
const { DramaService } = await import('@/services/drama/drama.service');

describe('GET /api/dramas/search', () => {
  it('should return canonical drama list response for valid queries', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/search?q=%20%E9%80%86%E8%A2%AD%20&page=1&pageSize=10');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toHaveLength(1);
    expect(body.pagination).toEqual({
      page: 1,
      page_size: 10,
      total: 1,
      total_pages: 1,
    });
    expect(vi.mocked(DramaService).mock.results[0]?.value.searchDramas).toHaveBeenCalledWith({
      q: '逆袭',
      page: 1,
      pageSize: 10,
    });
  });

  it('should return empty data with 200 for oversized pages', async () => {
    vi.mocked(DramaService).mockImplementationOnce(() => ({
      searchDramas: vi.fn().mockResolvedValue({
        data: [],
        pagination: {
          page: 999,
          page_size: 10,
          total: 4,
          total_pages: 1,
        },
      }),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/dramas/search?q=%E5%90%8E&page=999&pageSize=10');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toEqual([]);
    expect(body.pagination).toEqual({
      page: 999,
      page_size: 10,
      total: 4,
      total_pages: 1,
    });
  });

  it('should reject blank q with validation error', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/search?q=%20%20%20');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should reject invalid pagination params with validation error', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/search?q=%E9%80%86%E8%A2%AD&page=0&pageSize=101');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return internal error when service throws', async () => {
    vi.mocked(DramaService).mockImplementationOnce(() => ({
      searchDramas: vi.fn().mockRejectedValue(new Error('boom')),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/dramas/search?q=%E9%80%86%E8%A2%AD');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
