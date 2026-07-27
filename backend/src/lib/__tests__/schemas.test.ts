import { describe, it, expect } from 'vitest';
import {
  BookDramaResponseSchema,
  DramaListResponseSchema,
  DramaSchema,
  EpisodeSchema,
  HealthResponseSchema,
  HotSearchItemSchema,
  HotSearchListResponseSchema,
  PlayerStartRequestSchema,
  RankingDramaSchema,
  RankingListResponseSchema,
  RankingQuerySchema,
  SearchDramaQuerySchema,
  UserProfileSchema,
} from '../schemas';

describe('HealthResponseSchema', () => {
  it('should parse valid health response data', () => {
    const data = {
      status: 'ok' as const,
      version: '0.1.0',
      timestamp: '2026-07-24T00:00:00.000Z',
      services: {
        database: 'connected' as const,
        redis: 'connected' as const,
      },
    };
    const result = HealthResponseSchema.parse(data);
    expect(result.status).toBe('ok');
    expect(result.services.database).toBe('connected');
    expect(result.services.redis).toBe('connected');
  });

  it('should accept degraded status with disconnected services', () => {
    const data = {
      status: 'degraded' as const,
      version: '0.1.0',
      timestamp: '2026-07-24T00:00:00.000Z',
      services: {
        database: 'disconnected' as const,
        redis: 'connected' as const,
      },
    };
    const result = HealthResponseSchema.parse(data);
    expect(result.status).toBe('degraded');
    expect(result.services.database).toBe('disconnected');
  });
});

describe('DramaSchema', () => {
  const validDrama = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    title: 'Test Drama',
    description: 'A test drama series',
    cover_url: 'https://example.com/cover.jpg',
    category: 'Romance',
    episode_count: 24,
    tags: ['甜宠', '逆袭'],
    rating: 8.5,
    created_at: '2026-07-24T00:00:00.000Z',
    updated_at: '2026-07-24T00:00:00.000Z',
  };

  it('should parse valid drama data with homepage feed fields', () => {
    const result = DramaSchema.parse(validDrama);
    expect(result.title).toBe('Test Drama');
    expect(result.episode_count).toBe(24);
    expect(result.tags).toEqual(['甜宠', '逆袭']);
  });

  it('should default nullable and collection fields', () => {
    const result = DramaSchema.parse({
      id: validDrama.id,
      title: validDrama.title,
      episode_count: 12,
      created_at: validDrama.created_at,
      updated_at: validDrama.updated_at,
    });

    expect(result.description).toBe('');
    expect(result.cover_url).toBeNull();
    expect(result.category).toBe('');
    expect(result.tags).toEqual([]);
    expect(result.rating).toBeNull();
  });

  it('should reject empty title', () => {
    expect(() => DramaSchema.parse({ ...validDrama, title: '' })).toThrow();
  });

  it('should reject invalid uuid', () => {
    expect(() => DramaSchema.parse({ ...validDrama, id: 'not-a-uuid' })).toThrow();
  });

  it('should reject negative episode_count', () => {
    expect(() => DramaSchema.parse({ ...validDrama, episode_count: -1 })).toThrow();
  });

  it('should reject rating outside 0-10 range', () => {
    expect(() => DramaSchema.parse({ ...validDrama, rating: 11 })).toThrow();
  });

  it('should reject legacy total_episodes-only payloads', () => {
    expect(() =>
      DramaSchema.parse({
        id: validDrama.id,
        title: validDrama.title,
        total_episodes: 24,
        created_at: validDrama.created_at,
        updated_at: validDrama.updated_at,
      }),
    ).toThrow();
  });
});

describe('RankingQuerySchema', () => {
  it('should apply ranking defaults', () => {
    const result = RankingQuerySchema.parse({});
    expect(result).toEqual({
      type: 'hot',
      contentType: 'all',
      page: 1,
      pageSize: 10,
    });
  });

  it('should coerce and parse valid ranking params', () => {
    const result = RankingQuerySchema.parse({
      type: 'booking',
      contentType: 'ai',
      page: '2',
      pageSize: '20',
    });

    expect(result).toEqual({
      type: 'booking',
      contentType: 'ai',
      page: 2,
      pageSize: 20,
    });
  });

  it('should reject invalid ranking params', () => {
    expect(() => RankingQuerySchema.parse({ type: 'foo' })).toThrow();
    expect(() => RankingQuerySchema.parse({ contentType: 'bar' })).toThrow();
    expect(() => RankingQuerySchema.parse({ page: 0 })).toThrow();
    expect(() => RankingQuerySchema.parse({ pageSize: 101 })).toThrow();
  });
});

describe('RankingDramaSchema', () => {
  const validRankingDrama = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    title: 'Ranked Drama',
    description: 'Top ranked drama',
    cover_url: 'https://example.com/cover.jpg',
    category: 'Romance',
    episode_count: 24,
    tags: ['甜宠'],
    rating: 8.8,
    created_at: '2026-07-24T00:00:00.000Z',
    updated_at: '2026-07-24T00:00:00.000Z',
    content_type: 'live_action' as const,
    play_count: 100,
    booking_count: 10,
    recommendation_score: 80.5,
    is_booked: true,
  };

  it('should parse valid ranking drama payloads', () => {
    const result = RankingDramaSchema.parse(validRankingDrama);
    expect(result.content_type).toBe('live_action');
    expect(result.play_count).toBe(100);
    expect(result.booking_count).toBe(10);
    expect(result.recommendation_score).toBe(80.5);
    expect(result.is_booked).toBe(true);
  });

  it('should reject invalid ranking-specific fields', () => {
    expect(() => RankingDramaSchema.parse({ ...validRankingDrama, content_type: 'all' })).toThrow();
    expect(() => RankingDramaSchema.parse({ ...validRankingDrama, play_count: -1 })).toThrow();
    expect(() => RankingDramaSchema.parse({ ...validRankingDrama, booking_count: -1 })).toThrow();
  });
});

describe('RankingListResponseSchema', () => {
  it('should parse canonical rankings response', () => {
    const result = RankingListResponseSchema.parse({
      data: [
        {
          id: '123e4567-e89b-12d3-a456-426614174000',
          title: 'Ranked Drama',
          description: '',
          cover_url: null,
          category: 'Romance',
          episode_count: 24,
          tags: [],
          rating: null,
          created_at: '2026-07-24T00:00:00.000Z',
          updated_at: '2026-07-24T00:00:00.000Z',
          content_type: 'ai',
          play_count: 100,
          booking_count: 10,
          recommendation_score: 80,
          is_booked: false,
        },
      ],
      pagination: { page: 1, page_size: 10, total: 1, total_pages: 1 },
    });

    expect(result.data).toHaveLength(1);
    expect(result.pagination.total_pages).toBe(1);
  });
});

describe('BookDramaResponseSchema', () => {
  it('should parse a successful booking response', () => {
    const result = BookDramaResponseSchema.parse({
      drama_id: '123e4567-e89b-12d3-a456-426614174000',
      booked: true,
      booking_count: 11,
    });

    expect(result.booked).toBe(true);
    expect(result.booking_count).toBe(11);
  });

  it('should reject non-true booked flags', () => {
    expect(() =>
      BookDramaResponseSchema.parse({
        drama_id: '123e4567-e89b-12d3-a456-426614174000',
        booked: false,
        booking_count: 11,
      }),
    ).toThrow();
  });
});

describe('EpisodeSchema', () => {
  const validEpisode = {
    id: '123e4567-e89b-12d3-a456-426614174001',
    drama_id: '123e4567-e89b-12d3-a456-426614174000',
    title: 'Episode 1',
    episode_number: 1,
    duration: 3600,
    video_url: 'https://example.com/video.mp4',
    thumbnail_url: 'https://example.com/thumb.jpg',
    description: 'First episode',
    created_at: '2026-07-24T00:00:00.000Z',
    updated_at: '2026-07-24T00:00:00.000Z',
  };

  it('should parse valid episode data', () => {
    const result = EpisodeSchema.parse(validEpisode);
    expect(result.title).toBe('Episode 1');
    expect(result.episode_number).toBe(1);
  });

  it('should reject episode_number less than 1', () => {
    expect(() => EpisodeSchema.parse({ ...validEpisode, episode_number: 0 })).toThrow();
  });

  it('should allow nullable optional fields', () => {
    const result = EpisodeSchema.parse({
      ...validEpisode,
      duration: null,
      video_url: null,
    });
    expect(result.duration).toBeNull();
  });
});

describe('DramaListResponseSchema', () => {
  it('should parse list response with canonical homepage payload', () => {
    const data = {
      data: [
        {
          id: '123e4567-e89b-12d3-a456-426614174000',
          title: 'Test Drama',
          description: '',
          cover_url: null,
          category: 'Romance',
          episode_count: 24,
          tags: [],
          rating: null,
          created_at: '2026-07-24T00:00:00.000Z',
          updated_at: '2026-07-24T00:00:00.000Z',
        },
      ],
      pagination: { page: 1, page_size: 10, total: 1, total_pages: 1 },
    };
    const result = DramaListResponseSchema.parse(data);
    expect(result.data).toHaveLength(1);
    expect(result.pagination.total_pages).toBe(1);
  });
});

describe('SearchDramaQuerySchema', () => {
  it('should trim q and coerce pagination params', () => {
    const result = SearchDramaQuerySchema.parse({
      q: '  逆袭  ',
      page: '1',
      pageSize: '10',
    });

    expect(result).toEqual({
      q: '逆袭',
      page: 1,
      pageSize: 10,
    });
  });

  it('should apply default pagination values', () => {
    const result = SearchDramaQuerySchema.parse({
      q: '豪门',
    });

    expect(result).toEqual({
      q: '豪门',
      page: 1,
      pageSize: 10,
    });
  });

  it('should reject blank or oversized q', () => {
    expect(() => SearchDramaQuerySchema.parse({ q: '   ' })).toThrow();
    expect(() => SearchDramaQuerySchema.parse({ q: 'a'.repeat(51) })).toThrow();
  });

  it('should reject invalid page or pageSize', () => {
    expect(() => SearchDramaQuerySchema.parse({ q: '逆袭', page: 0 })).toThrow();
    expect(() => SearchDramaQuerySchema.parse({ q: '逆袭', pageSize: 101 })).toThrow();
  });
});

describe('HotSearchItemSchema', () => {
  it('should parse valid hot search item', () => {
    const result = HotSearchItemSchema.parse({
      rank: 1,
      keyword: '逆袭',
      score: 9821,
    });

    expect(result.rank).toBe(1);
    expect(result.keyword).toBe('逆袭');
    expect(result.score).toBe(9821);
  });

  it('should reject invalid hot search item fields', () => {
    expect(() => HotSearchItemSchema.parse({ rank: 0, keyword: '逆袭', score: 1 })).toThrow();
    expect(() => HotSearchItemSchema.parse({ rank: 1, keyword: '   ', score: 1 })).toThrow();
    expect(() => HotSearchItemSchema.parse({ rank: 1, keyword: '逆袭', score: -1 })).toThrow();
  });
});

describe('HotSearchListResponseSchema', () => {
  it('should parse valid hot search response', () => {
    const result = HotSearchListResponseSchema.parse({
      data: [
        { rank: 1, keyword: '逆袭', score: 9821 },
        { rank: 2, keyword: '豪门', score: 9540 },
      ],
    });

    expect(result.data).toHaveLength(2);
    expect(result.data[0].keyword).toBe('逆袭');
  });

  it('should reject more than 10 hot search items', () => {
    expect(() =>
      HotSearchListResponseSchema.parse({
        data: Array.from({ length: 11 }, (_, index) => ({
          rank: index + 1,
          keyword: `关键词${index + 1}`,
          score: 1000 - index,
        })),
      }),
    ).toThrow();
  });
});

describe('PlayerStartRequestSchema', () => {
  it('should parse valid start request', () => {
    const data = {
      drama_id: '123e4567-e89b-12d3-a456-426614174000',
      episode_id: '123e4567-e89b-12d3-a456-426614174001',
      progress: 0,
    };
    const result = PlayerStartRequestSchema.parse(data);
    expect(result.progress).toBe(0);
  });

  it('should default progress to 0', () => {
    const data = {
      drama_id: '123e4567-e89b-12d3-a456-426614174000',
      episode_id: '123e4567-e89b-12d3-a456-426614174001',
    };
    const result = PlayerStartRequestSchema.parse(data);
    expect(result.progress).toBe(0);
  });
});

describe('UserProfileSchema', () => {
  it('should parse valid user profile', () => {
    const data = {
      id: '123e4567-e89b-12d3-a456-426614174002',
      email: 'test@example.com',
      display_name: 'Test User',
      avatar_url: 'https://example.com/avatar.jpg',
      created_at: '2026-07-24T00:00:00.000Z',
      updated_at: '2026-07-24T00:00:00.000Z',
    };
    const result = UserProfileSchema.parse(data);
    expect(result.display_name).toBe('Test User');
  });

  it('should allow nullable email and display_name', () => {
    const data = {
      id: '123e4567-e89b-12d3-a456-426614174002',
      email: null,
      display_name: null,
      created_at: '2026-07-24T00:00:00.000Z',
      updated_at: '2026-07-24T00:00:00.000Z',
    };
    const result = UserProfileSchema.parse(data);
    expect(result.email).toBeNull();
    expect(result.display_name).toBeNull();
  });
});
