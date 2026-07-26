import { describe, it, expect } from 'vitest';
import {
  HealthResponseSchema,
  DramaSchema,
  EpisodeSchema,
  DramaListResponseSchema,
  DramaIdPathSchema,
  EpisodeListResponseSchema,
  PlaybackHistorySchema,
  PlaybackSessionIdHeaderSchema,
  PlayerProgressQuerySchema,
  PlayerProgressResponseSchema,
  PlayerStartRequestSchema,
  PlayerStartResponseSchema,
  PlayerStopRequestSchema,
  PlayerStopResponseSchema,
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

describe('player schemas', () => {
  it('should parse valid drama path params', () => {
    const result = DramaIdPathSchema.parse({
      id: '550e8400-e29b-41d4-a716-446655440001',
    });

    expect(result.id).toBe('550e8400-e29b-41d4-a716-446655440001');
  });

  it('should reject invalid drama path params', () => {
    expect(() => DramaIdPathSchema.parse({ id: 'not-a-uuid' })).toThrow();
  });

  it('should parse playback session header', () => {
    const result = PlaybackSessionIdHeaderSchema.parse('770e8400-e29b-41d4-a716-446655440000');
    expect(result).toBe('770e8400-e29b-41d4-a716-446655440000');
  });

  it('should reject invalid playback session header', () => {
    expect(() => PlaybackSessionIdHeaderSchema.parse('missing')).toThrow();
  });

  it('should parse progress query', () => {
    const result = PlayerProgressQuerySchema.parse({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
    });

    expect(result.dramaId).toBe('550e8400-e29b-41d4-a716-446655440001');
  });

  it('should reject invalid progress query', () => {
    expect(() => PlayerProgressQuerySchema.parse({ dramaId: 'bad-id' })).toThrow();
  });

  it('should parse playback history row', () => {
    const result = PlaybackHistorySchema.parse({
      playback_session_id: '770e8400-e29b-41d4-a716-446655440000',
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
      episode_id: '660e8400-e29b-41d4-a716-446655440001',
      progress: 120,
      duration: 180,
      updated_at: '2026-07-26T00:00:00Z',
    });

    expect(result.duration).toBe(180);
  });

  it('should parse episode list response', () => {
    const result = EpisodeListResponseSchema.parse({
      code: 0,
      data: {
        drama_id: '550e8400-e29b-41d4-a716-446655440001',
        series_status: 'completed',
        items: [
          {
            id: '660e8400-e29b-41d4-a716-446655440001',
            drama_id: '550e8400-e29b-41d4-a716-446655440001',
            title: '第 1 集',
            episode_number: 1,
            duration: 180,
            video_url: 'https://example.com/video-1.mp4',
            thumbnail_url: 'https://example.com/thumb-1.jpg',
            description: '第一集简介',
            created_at: '2026-07-26T00:00:00Z',
            updated_at: '2026-07-26T00:00:00Z',
          },
        ],
      },
      message: 'ok',
    });

    expect(result.data.items).toHaveLength(1);
  });

  it('should parse progress response without history', () => {
    const result = PlayerProgressResponseSchema.parse({
      code: 0,
      data: {
        drama_id: '550e8400-e29b-41d4-a716-446655440001',
        has_history: false,
        episode_id: null,
        start_time: 0,
        updated_at: null,
      },
      message: 'ok',
    });

    expect(result.data.has_history).toBe(false);
    expect(result.data.start_time).toBe(0);
  });

  it('should reject invalid progress response shape', () => {
    expect(() =>
      PlayerProgressResponseSchema.parse({
        code: 0,
        data: {
          drama_id: '550e8400-e29b-41d4-a716-446655440001',
          has_history: true,
          episode_id: null,
          start_time: -1,
          updated_at: null,
        },
        message: 'ok',
      }),
    ).toThrow();
  });

  it('should parse valid start request', () => {
    const data = {
      drama_id: '123e4567-e89b-12d3-a456-426614174000',
      episode_id: '123e4567-e89b-12d3-a456-426614174001',
      progress: 0,
    };
    const result = PlayerStartRequestSchema.parse(data);
    expect(result.progress).toBe(0);
  });

  it('should default start progress to 0', () => {
    const data = {
      drama_id: '123e4567-e89b-12d3-a456-426614174000',
      episode_id: '123e4567-e89b-12d3-a456-426614174001',
    };
    const result = PlayerStartRequestSchema.parse(data);
    expect(result.progress).toBe(0);
  });

  it('should parse start response', () => {
    const result = PlayerStartResponseSchema.parse({
      code: 0,
      data: {
        drama_id: '550e8400-e29b-41d4-a716-446655440001',
        episode_id: '660e8400-e29b-41d4-a716-446655440001',
        accepted_progress: 330,
        playback_session_id: '770e8400-e29b-41d4-a716-446655440000',
        started_at: '2026-07-26T00:00:00Z',
      },
      message: 'ok',
    });

    expect(result.data.accepted_progress).toBe(330);
  });

  it('should reject invalid accepted_progress in start response', () => {
    expect(() =>
      PlayerStartResponseSchema.parse({
        code: 0,
        data: {
          drama_id: '550e8400-e29b-41d4-a716-446655440001',
          episode_id: '660e8400-e29b-41d4-a716-446655440001',
          accepted_progress: -1,
          playback_session_id: '770e8400-e29b-41d4-a716-446655440000',
          started_at: '2026-07-26T00:00:00Z',
        },
        message: 'ok',
      }),
    ).toThrow();
  });

  it('should parse valid stop request', () => {
    const result = PlayerStopRequestSchema.parse({
      drama_id: '123e4567-e89b-12d3-a456-426614174000',
      episode_id: '123e4567-e89b-12d3-a456-426614174001',
      progress: 120,
      duration: 180,
    });

    expect(result.duration).toBe(180);
  });

  it('should parse stop response', () => {
    const result = PlayerStopResponseSchema.parse({
      code: 0,
      data: {
        drama_id: '550e8400-e29b-41d4-a716-446655440001',
        episode_id: '660e8400-e29b-41d4-a716-446655440001',
        saved_progress: 180,
        duration: 180,
        updated_at: '2026-07-26T00:00:00Z',
      },
      message: 'ok',
    });

    expect(result.data.saved_progress).toBe(180);
  });

  it('should reject invalid saved_progress in stop response', () => {
    expect(() =>
      PlayerStopResponseSchema.parse({
        code: 0,
        data: {
          drama_id: '550e8400-e29b-41d4-a716-446655440001',
          episode_id: '660e8400-e29b-41d4-a716-446655440001',
          saved_progress: -1,
          duration: 180,
          updated_at: '2026-07-26T00:00:00Z',
        },
        message: 'ok',
      }),
    ).toThrow();
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
