import { describe, it, expect, beforeEach } from 'vitest';
import { DramaService } from './drama.service';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import {
  BookDramaResponse,
  Drama,
  HotSearchListResponse,
  RankingDrama,
} from '@/lib/schemas';
import {
  DramaRepositoryInterface,
  PaginatedResult,
} from '@/repositories/interfaces/drama.repository.interface';

function makeDramaInput(overrides: Partial<Omit<Drama, 'id' | 'created_at' | 'updated_at'>> = {}): Omit<Drama, 'id' | 'created_at' | 'updated_at'> {
  return {
    title: overrides.title ?? 'Test Drama',
    description: overrides.description ?? '',
    cover_url: overrides.cover_url ?? null,
    category: overrides.category ?? 'Test Category',
    episode_count: overrides.episode_count ?? 12,
    tags: overrides.tags ?? [],
    rating: overrides.rating ?? null,
  };
}

class InvalidSearchRepository implements DramaRepositoryInterface {
  async findMany(): Promise<PaginatedResult<Drama>> {
    throw new Error('Method not implemented.');
  }

  async search(): Promise<PaginatedResult<Drama>> {
    return {
      data: [
        {
          id: 'not-a-uuid',
          title: 'Broken',
          description: '',
          cover_url: null,
          category: '都市',
          episode_count: 12,
          tags: [],
          rating: 8,
          created_at: '2026-07-24T00:00:00Z',
          updated_at: '2026-07-24T00:00:00Z',
        } as unknown as Drama,
      ],
      pagination: {
        page: 1,
        page_size: 10,
        total: 1,
        total_pages: 1,
      },
    };
  }

  async listRankings(): Promise<PaginatedResult<RankingDrama>> {
    throw new Error('Method not implemented.');
  }

  async listHotSearches(): Promise<HotSearchListResponse> {
    throw new Error('Method not implemented.');
  }

  async bookDrama(): Promise<BookDramaResponse> {
    throw new Error('Method not implemented.');
  }

  async findById(): Promise<Drama | null> {
    throw new Error('Method not implemented.');
  }

  async create(): Promise<Drama> {
    throw new Error('Method not implemented.');
  }

  async update(): Promise<Drama | null> {
    throw new Error('Method not implemented.');
  }

  async delete(): Promise<boolean> {
    throw new Error('Method not implemented.');
  }
}

class InvalidHotSearchRepository implements DramaRepositoryInterface {
  async findMany(): Promise<PaginatedResult<Drama>> {
    throw new Error('Method not implemented.');
  }

  async search(): Promise<PaginatedResult<Drama>> {
    throw new Error('Method not implemented.');
  }

  async listRankings(): Promise<PaginatedResult<RankingDrama>> {
    throw new Error('Method not implemented.');
  }

  async listHotSearches(): Promise<HotSearchListResponse> {
    return {
      data: [
        {
          rank: 0,
          keyword: '非法热搜',
          score: 1,
        },
      ],
    } as HotSearchListResponse;
  }

  async bookDrama(): Promise<BookDramaResponse> {
    throw new Error('Method not implemented.');
  }

  async findById(): Promise<Drama | null> {
    throw new Error('Method not implemented.');
  }

  async create(): Promise<Drama> {
    throw new Error('Method not implemented.');
  }

  async update(): Promise<Drama | null> {
    throw new Error('Method not implemented.');
  }

  async delete(): Promise<boolean> {
    throw new Error('Method not implemented.');
  }
}

class InvalidRankingsRepository implements DramaRepositoryInterface {
  async findMany(): Promise<PaginatedResult<Drama>> {
    throw new Error('Method not implemented.');
  }

  async search(): Promise<PaginatedResult<Drama>> {
    throw new Error('Method not implemented.');
  }

  async listRankings(): Promise<PaginatedResult<RankingDrama>> {
    return {
      data: [
        {
          id: '123e4567-e89b-12d3-a456-426614174000',
          title: 'Broken Ranking',
          description: '',
          cover_url: null,
          category: '都市',
          episode_count: 12,
          tags: [],
          rating: 8,
          created_at: '2026-07-24T00:00:00Z',
          updated_at: '2026-07-24T00:00:00Z',
          content_type: 'all',
          play_count: 100,
          booking_count: 10,
          recommendation_score: 90,
          is_booked: false,
        } as unknown as RankingDrama,
      ],
      pagination: {
        page: 1,
        page_size: 10,
        total: 1,
        total_pages: 1,
      },
    };
  }

  async listHotSearches(): Promise<HotSearchListResponse> {
    throw new Error('Method not implemented.');
  }

  async bookDrama(): Promise<BookDramaResponse> {
    throw new Error('Method not implemented.');
  }

  async findById(): Promise<Drama | null> {
    throw new Error('Method not implemented.');
  }

  async create(): Promise<Drama> {
    throw new Error('Method not implemented.');
  }

  async update(): Promise<Drama | null> {
    throw new Error('Method not implemented.');
  }

  async delete(): Promise<boolean> {
    throw new Error('Method not implemented.');
  }
}

class InvalidBookingRepository implements DramaRepositoryInterface {
  async findMany(): Promise<PaginatedResult<Drama>> {
    throw new Error('Method not implemented.');
  }

  async search(): Promise<PaginatedResult<Drama>> {
    throw new Error('Method not implemented.');
  }

  async listRankings(): Promise<PaginatedResult<RankingDrama>> {
    throw new Error('Method not implemented.');
  }

  async listHotSearches(): Promise<HotSearchListResponse> {
    throw new Error('Method not implemented.');
  }

  async bookDrama(): Promise<BookDramaResponse> {
    return {
      drama_id: 'not-a-uuid',
      booked: true,
      booking_count: 1,
    } as unknown as BookDramaResponse;
  }

  async findById(): Promise<Drama | null> {
    throw new Error('Method not implemented.');
  }

  async create(): Promise<Drama> {
    throw new Error('Method not implemented.');
  }

  async update(): Promise<Drama | null> {
    throw new Error('Method not implemented.');
  }

  async delete(): Promise<boolean> {
    throw new Error('Method not implemented.');
  }
}

describe('DramaService', () => {
  let service: DramaService;
  let repo: DramaMockRepository;

  beforeEach(() => {
    repo = new DramaMockRepository();
    service = new DramaService(repo);
  });

  it('should list seeded homepage dramas by default', async () => {
    const result = await service.listDramas({ page: 1, pageSize: 10 });
    expect(result.data).toHaveLength(10);
    expect(result.pagination.total).toBe(12);
    expect(result.pagination.total_pages).toBe(2);
  });

  it('should return correct second page slice', async () => {
    const result = await service.listDramas({ page: 2, pageSize: 10 });
    expect(result.data).toHaveLength(2);
    expect(result.data[0]?.id).toBe('550e8400-e29b-41d4-a716-446655440011');
  });

  it('should return empty data for oversized page without failing', async () => {
    const result = await service.listDramas({ page: 999, pageSize: 10 });
    expect(result.data).toEqual([]);
    expect(result.pagination.total).toBe(12);
    expect(result.pagination.total_pages).toBe(2);
  });

  it('should validate repository output against canonical schema', async () => {
    const emptyRepo = new DramaMockRepository([]);
    const emptyService = new DramaService(emptyRepo);
    const created = await emptyRepo.create(makeDramaInput({ title: 'Schema Check', episode_count: 9, tags: ['测试'] }));

    const result = await emptyService.listDramas({ page: 1, pageSize: 10 });
    expect(result.data).toHaveLength(1);
    expect(result.data[0]).toMatchObject({
      id: created.id,
      title: 'Schema Check',
      episode_count: 9,
      tags: ['测试'],
    });
  });

  it('should search dramas and validate response contract', async () => {
    const result = await service.searchDramas({ q: '后', page: 1, pageSize: 10 });

    expect(result.data.map((item) => item.title)).toEqual([
      '逆袭归来后我成了豪门团宠',
      '离婚后前夫跪求复合',
      '我在八零年代当后妈',
      '重生后我把渣男送进火葬场',
      '误撩禁欲教授后她红了',
      '替嫁后她成了京圈白月光',
    ]);
    expect(result.pagination.total).toBe(6);
  });

  it('should return empty search results for large pages without failing', async () => {
    const result = await service.searchDramas({ q: '后', page: 999, pageSize: 10 });

    expect(result.data).toEqual([]);
    expect(result.pagination).toEqual({
      page: 999,
      page_size: 10,
      total: 6,
      total_pages: 1,
    });
  });

  it('should list hot searches and validate response contract', async () => {
    const result = await service.listHotSearches();

    expect(result.data.length).toBeGreaterThan(0);
    expect(result.data.length).toBeLessThanOrEqual(10);
    expect(result.data[0]).toEqual({ rank: 1, keyword: '逆袭', score: 9821 });
  });

  it('should list rankings and keep booking state for the authenticated user', async () => {
    await repo.bookDrama({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      userId: 'user-1',
    });

    const result = await service.listRankings(
      {
        contentType: 'all',
        type: 'booking',
        page: 1,
        pageSize: 10,
      },
      { userId: 'user-1' },
    );

    expect(result.data.length).toBe(10);
    expect(result.data[0]?.booking_count).toBeGreaterThanOrEqual(result.data[1]?.booking_count ?? 0);
    expect(result.data.find((item) => item.id === '550e8400-e29b-41d4-a716-446655440001')?.is_booked).toBe(true);
  });

  it('should book dramas idempotently through the service', async () => {
    const first = await service.bookDrama({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      userId: 'user-1',
    });
    const second = await service.bookDrama({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      userId: 'user-1',
    });

    expect(first.booked).toBe(true);
    expect(second.booked).toBe(true);
    expect(second.booking_count).toBe(first.booking_count);
  });

  it('should return not found when booking a missing drama', async () => {
    await expect(
      service.bookDrama({
        dramaId: '123e4567-e89b-12d3-a456-426614174999',
        userId: 'user-1',
      }),
    ).rejects.toMatchObject({
      code: 'NOT_FOUND',
    });
  });

  it('should wrap invalid search output as internal error', async () => {
    const invalidService = new DramaService(new InvalidSearchRepository());

    await expect(invalidService.searchDramas({ q: '逆袭', page: 1, pageSize: 10 })).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
    });
  });

  it('should wrap invalid rankings output as internal error', async () => {
    const invalidService = new DramaService(new InvalidRankingsRepository());

    await expect(
      invalidService.listRankings({
        contentType: 'all',
        type: 'hot',
        page: 1,
        pageSize: 10,
      }),
    ).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
    });
  });

  it('should wrap invalid hot search output as internal error', async () => {
    const invalidService = new DramaService(new InvalidHotSearchRepository());

    await expect(invalidService.listHotSearches()).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
    });
  });

  it('should wrap invalid booking output as internal error', async () => {
    const invalidService = new DramaService(new InvalidBookingRepository());

    await expect(
      invalidService.bookDrama({
        dramaId: '550e8400-e29b-41d4-a716-446655440001',
        userId: 'user-1',
      }),
    ).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
    });
  });

  it('should throw notImplemented for getDramaById', async () => {
    await expect(service.getDramaById('some-id')).rejects.toThrow(/not implemented/i);
  });

  it('should throw notImplemented for createDrama', async () => {
    await expect(service.createDrama(makeDramaInput())).rejects.toThrow(/not implemented/i);
  });
});
