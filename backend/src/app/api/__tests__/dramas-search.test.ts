import { describe, it, expect, vi, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';

vi.mock('@/services/drama/drama.service', () => ({
  DramaService: vi.fn().mockImplementation(() => ({
    searchDramas: vi.fn().mockResolvedValue({
      data: [
        {
          id: '550e8400-e29b-41d4-a716-446655440012',
          title: '天降萌宝总裁爹地别太宠',
          description: '萌宝助攻下，破镜重圆的爱情再次启动。',
          cover_url: 'https://example.com/dramas/012.jpg',
          category: '家庭',
          episode_count: 66,
          tags: ['萌宝', '破镜重圆'],
          rating: 8,
          created_at: '2026-07-24T13:00:00Z',
          updated_at: '2026-07-24T13:00:00Z',
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
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return canonical drama list response for valid queries', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/search?q=%20%E8%90%8C%E5%AE%9D%20&page=1&pageSize=10');
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
      q: '萌宝',
      page: 1,
      pageSize: 10,
    });
  });

  it('should keep route semantics for category queries', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/search?q=%E9%83%BD%E5%B8%82&page=1&pageSize=10');
    const response = await GET(request, undefined);

    expect(response.status).toBe(200);
    expect(vi.mocked(DramaService).mock.results[0]?.value.searchDramas).toHaveBeenCalledWith({
      q: '都市',
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
