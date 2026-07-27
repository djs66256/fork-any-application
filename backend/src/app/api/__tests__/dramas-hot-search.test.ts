import { describe, it, expect, vi } from 'vitest';
import { NextRequest } from 'next/server';

vi.mock('@/services/drama/drama.service', () => ({
  DramaService: vi.fn().mockImplementation(() => ({
    listHotSearches: vi.fn().mockResolvedValue({
      data: [
        { rank: 1, keyword: '逆袭', score: 9821 },
        { rank: 2, keyword: '豪门', score: 9540 },
      ],
    }),
  })),
}));

const { GET } = await import('../dramas/hot-search/route');
const { DramaService } = await import('@/services/drama/drama.service');

describe('GET /api/dramas/hot-search', () => {
  it('should return hot search items', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/hot-search');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toEqual([
      { rank: 1, keyword: '逆袭', score: 9821 },
      { rank: 2, keyword: '豪门', score: 9540 },
    ]);
    expect(vi.mocked(DramaService).mock.results[0]?.value.listHotSearches).toHaveBeenCalledTimes(1);
  });

  it('should return internal error when service throws', async () => {
    vi.mocked(DramaService).mockImplementationOnce(() => ({
      listHotSearches: vi.fn().mockRejectedValue(new Error('boom')),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/dramas/hot-search');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
