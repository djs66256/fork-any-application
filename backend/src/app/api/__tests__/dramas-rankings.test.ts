import { describe, it, expect, vi, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';

vi.mock('@/services/drama/drama.service', () => ({
  DramaService: vi.fn().mockImplementation(() => ({
    listRankings: vi.fn().mockResolvedValue({
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
          content_type: 'live_action',
          play_count: 9999,
          booking_count: 888,
          recommendation_score: 95.5,
          is_booked: false,
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

const { GET } = await import('../dramas/rankings/route');
const { DramaService } = await import('@/services/drama/drama.service');

describe('GET /api/dramas/rankings', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return canonical rankings response for valid queries', async () => {
    const request = new NextRequest(
      'https://localhost:3001/api/dramas/rankings?type=booking&contentType=ai&page=2&pageSize=20',
    );
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
    expect(vi.mocked(DramaService).mock.results[0]?.value.listRankings).toHaveBeenCalledWith(
      {
        type: 'booking',
        contentType: 'ai',
        page: 2,
        pageSize: 20,
      },
      undefined,
    );
  });

  it('should forward optional authenticated context to the service', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/rankings', {
      headers: {
        'x-user-id': 'user-1',
      },
    });
    const response = await GET(request, undefined);

    expect(response.status).toBe(200);
    expect(vi.mocked(DramaService).mock.results[0]?.value.listRankings).toHaveBeenCalledWith(
      {
        type: 'hot',
        contentType: 'all',
        page: 1,
        pageSize: 10,
      },
      { userId: 'user-1' },
    );
  });

  it('should return empty data with 200 for oversized pages', async () => {
    vi.mocked(DramaService).mockImplementationOnce(() => ({
      listRankings: vi.fn().mockResolvedValue({
        data: [],
        pagination: {
          page: 999,
          page_size: 10,
          total: 4,
          total_pages: 1,
        },
      }),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/dramas/rankings?page=999&pageSize=10');
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

  it('should reject invalid ranking params with validation error', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/rankings?type=foo&page=0&pageSize=101');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return internal error when service throws unexpectedly', async () => {
    vi.mocked(DramaService).mockImplementationOnce(() => ({
      listRankings: vi.fn().mockRejectedValue(new Error('boom')),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/dramas/rankings');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
