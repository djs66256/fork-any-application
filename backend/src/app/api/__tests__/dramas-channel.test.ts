import { beforeEach, describe, expect, it } from 'vitest';
import { NextRequest } from 'next/server';
import { GET } from '../dramas/channel/route';
import {
  resetRepositoryRegistry,
  setDramaRepository,
} from '@/repositories/repository-registry';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';

class StubTheaterRepository extends DramaMockRepository {
  override async listTheaterFeed() {
    return {
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
          heat: 98210,
        },
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

class InvalidTheaterRepository extends DramaMockRepository {
  override async listTheaterFeed() {
    return {
      data: [
        {
          id: '550e8400-e29b-41d4-a716-446655440001',
          title: 'broken',
          description: '',
          cover_url: null,
          category: '都市',
          episode_count: 1,
          tags: [],
          rating: null,
          created_at: '2026-07-25T00:00:00Z',
          updated_at: '2026-07-25T00:00:00Z',
        },
      ],
      pagination: {
        page: 1,
        page_size: 20,
        total: 1,
        total_pages: 1,
      },
    } as never;
  }
}

describe('GET /api/dramas/channel', () => {
  beforeEach(() => {
    resetRepositoryRegistry();
  });

  it('should return canonical theater feed and use repository registry injection', async () => {
    setDramaRepository(new StubTheaterRepository());

    const request = new NextRequest('https://localhost:3001/api/dramas/channel?channel=all&page=1&pageSize=20');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
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
          heat: 98210,
        },
      ],
      pagination: {
        page: 1,
        page_size: 20,
        total: 1,
        total_pages: 1,
      },
    });
  });

  it('should return empty data with 200 for non-all channels', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/channel?channel=real&page=1&pageSize=20');
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

  it('should reject invalid theater params with validation error', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/channel?channel=foo&page=0&pageSize=101');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return internal error when repository data is invalid', async () => {
    setDramaRepository(new InvalidTheaterRepository());

    const request = new NextRequest('https://localhost:3001/api/dramas/channel?channel=all&page=1&pageSize=20');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
