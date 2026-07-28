import { describe, it, expect } from 'vitest';
import {
  AuthSessionResponseSchema,
  AuthSessionSchema,
  AuthUserSchema,
  BookDramaResponseSchema,
  CLASSIFICATION_DIMENSION_KEYS,
  ClassificationTagsQuerySchema,
  ClassificationTagsResponseSchema,
  CreateAuthSessionRequestSchema,
  CountryCodeSchema,
  CurrentUserResponseSchema,
  DramaIdPathSchema,
  DramaListResponseSchema,
  DramaSchema,
  EmptySuccessResponseSchema,
  EpisodeListResponseSchema,
  EpisodeSchema,
  HealthResponseSchema,
  HotSearchItemSchema,
  HotSearchListResponseSchema,
  OtpCodeSchema,
  PhoneSchema,
  PlaybackHistorySchema,
  PlaybackSessionIdHeaderSchema,
  PlayerProgressQuerySchema,
  PlayerProgressResponseSchema,
  PlayerStartRequestSchema,
  PlayerStartResponseSchema,
  PlayerStopRequestSchema,
  PlayerStopResponseSchema,
  RecentlyViewedResponseSchema,
  RankingDramaSchema,
  RankingListResponseSchema,
  RankingQuerySchema,
  RefreshAuthSessionRequestSchema,
  SearchDramaQuerySchema,
  SendOtpRequestSchema,
  SendOtpResponseSchema,
  TheaterDramaSchema,
  TheaterFeedQuerySchema,
  TheaterFeedResponseSchema,
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

describe('TheaterFeedQuerySchema', () => {
  it('should apply theater feed defaults', () => {
    expect(TheaterFeedQuerySchema.parse({})).toEqual({
      channel: 'all',
      page: 1,
      pageSize: 20,
    });
  });

  it('should coerce and parse valid theater feed params', () => {
    expect(TheaterFeedQuerySchema.parse({ channel: 'all', page: '2', pageSize: '20' })).toEqual({
      channel: 'all',
      page: 2,
      pageSize: 20,
    });
  });

  it('should reject invalid theater feed params', () => {
    expect(() => TheaterFeedQuerySchema.parse({ channel: 'foo' })).toThrow();
    expect(() => TheaterFeedQuerySchema.parse({ page: 0 })).toThrow();
    expect(() => TheaterFeedQuerySchema.parse({ pageSize: 101 })).toThrow();
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

describe('TheaterDramaSchema', () => {
  it('should parse valid theater drama payloads with heat', () => {
    const result = TheaterDramaSchema.parse({
      id: '123e4567-e89b-12d3-a456-426614174000',
      title: 'Theater Drama',
      description: '',
      cover_url: null,
      category: 'Romance',
      episode_count: 24,
      tags: [],
      rating: null,
      created_at: '2026-07-24T00:00:00.000Z',
      updated_at: '2026-07-24T00:00:00.000Z',
      heat: 98210,
    });

    expect(result.heat).toBe(98210);
  });

  it('should reject negative or missing heat', () => {
    expect(() =>
      TheaterDramaSchema.parse({
        id: '123e4567-e89b-12d3-a456-426614174000',
        title: 'Theater Drama',
        description: '',
        cover_url: null,
        category: 'Romance',
        episode_count: 24,
        tags: [],
        rating: null,
        created_at: '2026-07-24T00:00:00.000Z',
        updated_at: '2026-07-24T00:00:00.000Z',
        heat: -1,
      }),
    ).toThrow();

    expect(() =>
      TheaterDramaSchema.parse({
        id: '123e4567-e89b-12d3-a456-426614174000',
        title: 'Theater Drama',
        description: '',
        cover_url: null,
        category: 'Romance',
        episode_count: 24,
        tags: [],
        rating: null,
        created_at: '2026-07-24T00:00:00.000Z',
        updated_at: '2026-07-24T00:00:00.000Z',
      }),
    ).toThrow();
  });
});

describe('TheaterFeedResponseSchema', () => {
  it('should parse canonical theater feed response', () => {
    const result = TheaterFeedResponseSchema.parse({
      data: [
        {
          id: '123e4567-e89b-12d3-a456-426614174000',
          title: 'Theater Drama',
          description: '',
          cover_url: null,
          category: 'Romance',
          episode_count: 24,
          tags: [],
          rating: null,
          created_at: '2026-07-24T00:00:00.000Z',
          updated_at: '2026-07-24T00:00:00.000Z',
          heat: 1,
        },
      ],
      pagination: { page: 1, page_size: 20, total: 1, total_pages: 1 },
    });

    expect(result.data).toHaveLength(1);
    expect(result.pagination.page_size).toBe(20);
  });
});

describe('ClassificationTagsQuerySchema', () => {
  it('should default gender to all and parse valid values', () => {
    expect(ClassificationTagsQuerySchema.parse({})).toEqual({ gender: 'all' });
    expect(ClassificationTagsQuerySchema.parse({ gender: 'male' })).toEqual({ gender: 'male' });
    expect(ClassificationTagsQuerySchema.parse({ gender: 'female' })).toEqual({ gender: 'female' });
  });

  it('should reject invalid gender values', () => {
    expect(() => ClassificationTagsQuerySchema.parse({ gender: 'unknown' })).toThrow();
    expect(() => ClassificationTagsQuerySchema.parse({ gender: 1 })).toThrow();
  });
});

describe('ClassificationTagsResponseSchema', () => {
  it('should parse canonical classification response with fixed dimensions', () => {
    const result = ClassificationTagsResponseSchema.parse({
      data: {
        gender: 'all',
        dimensions: [
          { key: 'era_background', name: '时代背景', tags: ['都市'] },
          { key: 'theme_plot', name: '主题情节', tags: ['逆袭'] },
          { key: 'character_setting', name: '角色设定', tags: [] },
        ],
      },
    });

    expect(result.data.gender).toBe('all');
    expect(result.data.dimensions).toHaveLength(3);
    expect(result.data.dimensions.map((item) => item.key)).toEqual([...CLASSIFICATION_DIMENSION_KEYS]);
  });

  it('should reject missing dimensions or incorrect order', () => {
    expect(() =>
      ClassificationTagsResponseSchema.parse({
        data: {
          gender: 'male',
          dimensions: [
            { key: 'theme_plot', name: '主题情节', tags: [] },
            { key: 'era_background', name: '时代背景', tags: [] },
            { key: 'character_setting', name: '角色设定', tags: [] },
          ],
        },
      }),
    ).toThrow();

    expect(() =>
      ClassificationTagsResponseSchema.parse({
        data: {
          gender: 'female',
          dimensions: [
            { key: 'era_background', name: '时代背景', tags: [] },
            { key: 'theme_plot', name: '主题情节', tags: [] },
          ],
        },
      }),
    ).toThrow();
  });
});

describe('Auth schemas', () => {
  it('should parse valid auth primitives', () => {
    expect(CountryCodeSchema.parse('+86')).toBe('+86');
    expect(PhoneSchema.parse('13800138000')).toBe('13800138000');
    expect(OtpCodeSchema.parse('123456')).toBe('123456');
  });

  it('should reject invalid auth primitives', () => {
    expect(() => CountryCodeSchema.parse('86')).toThrow();
    expect(() => PhoneSchema.parse('123')).toThrow();
    expect(() => OtpCodeSchema.parse('12345a')).toThrow();
  });

  it('should parse send otp request with defaults', () => {
    const result = SendOtpRequestSchema.parse({
      phone: '13800138000',
    });

    expect(result).toEqual({
      countryCode: '+86',
      phone: '13800138000',
      scene: 'login',
    });
  });

  it('should reject invalid send otp request', () => {
    expect(() =>
      SendOtpRequestSchema.parse({
        countryCode: '+86',
        phone: '123',
        scene: 'login',
      }),
    ).toThrow();
  });

  it('should parse create auth session request', () => {
    const result = CreateAuthSessionRequestSchema.parse({
      phone: '13800138000',
      code: '654321',
    });

    expect(result.countryCode).toBe('+86');
    expect(result.phone).toBe('13800138000');
    expect(result.code).toBe('654321');
  });

  it('should reject invalid create auth session request', () => {
    expect(() =>
      CreateAuthSessionRequestSchema.parse({
        countryCode: '+86',
        phone: '13800138000',
        code: '12345',
      }),
    ).toThrow();
  });

  it('should parse refresh auth session request', () => {
    const result = RefreshAuthSessionRequestSchema.parse({
      refreshToken: 'refresh-token',
    });

    expect(result.refreshToken).toBe('refresh-token');
  });

  it('should reject blank refresh token', () => {
    expect(() =>
      RefreshAuthSessionRequestSchema.parse({
        refreshToken: '   ',
      }),
    ).toThrow();
  });

  it('should parse auth user and session responses', () => {
    const user = AuthUserSchema.parse({
      id: '123e4567-e89b-12d3-a456-426614174000',
      phone: '13800138000',
      display_name: '测试用户',
      avatar_url: 'https://example.com/avatar.jpg',
      is_new_user: true,
    });

    const session = AuthSessionSchema.parse({
      access_token: 'access-token',
      refresh_token: 'refresh-token',
      expires_at: '2026-07-28T12:00:00Z',
      user,
    });

    const sendOtpResponse = SendOtpResponseSchema.parse({
      code: 0,
      data: {
        requestId: 'otp_req_xxx',
        cooldownSeconds: 60,
        expiresInSeconds: 300,
      },
      message: 'ok',
    });

    const sessionResponse = AuthSessionResponseSchema.parse({
      code: 0,
      data: {
        accessToken: session.access_token,
        refreshToken: session.refresh_token,
        expiresAt: session.expires_at,
        user: {
          id: user.id,
          phone: user.phone,
          displayName: user.display_name,
          avatarUrl: user.avatar_url,
          role: user.role,
          isNewUser: user.is_new_user,
        },
      },
      message: 'ok',
    });

    const currentUserResponse = CurrentUserResponseSchema.parse({
      code: 0,
      data: {
        id: user.id,
        phone: user.phone,
        displayName: user.display_name,
        avatarUrl: user.avatar_url,
        role: user.role,
        isNewUser: user.is_new_user,
      },
      message: 'ok',
    });

    const emptySuccess = EmptySuccessResponseSchema.parse({
      code: 0,
      data: null,
      message: 'ok',
    });

    expect(sendOtpResponse.data.cooldownSeconds).toBe(60);
    expect(sendOtpResponse.data.expiresInSeconds).toBe(300);
    expect(sessionResponse.data.user.isNewUser).toBe(true);
    expect(currentUserResponse.data.phone).toBe('13800138000');
    expect(emptySuccess.data).toBeNull();
  });

  it('should reject malformed auth session response payload', () => {
    expect(() =>
      AuthSessionResponseSchema.parse({
        code: 0,
        data: {
          accessToken: '',
          refreshToken: 'refresh-token',
          expiresAt: '2026-07-28T12:00:00Z',
          user: {
            id: '123e4567-e89b-12d3-a456-426614174000',
            phone: '13800138000',
            isNewUser: false,
          },
        },
        message: 'ok',
      }),
    ).toThrow();
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

  it('should parse recently viewed response with nullable cover_url', () => {
    const result = RecentlyViewedResponseSchema.parse({
      code: 0,
      data: {
        items: [
          {
            drama_id: '550e8400-e29b-41d4-a716-446655440001',
            title: '逆袭归来后我成了豪门团宠',
            cover_url: null,
            episode_number: 12,
            progress: 128.5,
            updated_at: '2026-07-27T15:20:00.000Z',
          },
        ],
      },
      message: 'ok',
    });

    expect(result.data.items).toHaveLength(1);
    expect(result.data.items[0]?.cover_url).toBeNull();
  });

  it('should allow recently viewed response with empty items', () => {
    const result = RecentlyViewedResponseSchema.parse({
      code: 0,
      data: { items: [] },
      message: 'ok',
    });

    expect(result.data.items).toEqual([]);
  });

  it('should reject invalid recently viewed response shape', () => {
    expect(() =>
      RecentlyViewedResponseSchema.parse({
        code: 0,
        data: {
          items: [
            {
              drama_id: '550e8400-e29b-41d4-a716-446655440001',
              title: '有效数据',
              cover_url: null,
              episode_number: 1,
              progress: 10,
              updated_at: '2026-07-27T15:20:00.000Z',
            },
            {
              drama_id: '550e8400-e29b-41d4-a716-446655440002',
              title: '有效数据',
              cover_url: null,
              episode_number: 2,
              progress: 20,
              updated_at: '2026-07-27T15:19:00.000Z',
            },
            {
              drama_id: '550e8400-e29b-41d4-a716-446655440003',
              title: '有效数据',
              cover_url: null,
              episode_number: 3,
              progress: 30,
              updated_at: '2026-07-27T15:18:00.000Z',
            },
            {
              drama_id: '550e8400-e29b-41d4-a716-446655440004',
              title: '越界数据',
              cover_url: null,
              episode_number: 4,
              progress: 40,
              updated_at: '2026-07-27T15:17:00.000Z',
            },
          ],
        },
        message: 'ok',
      }),
    ).toThrow();

    expect(() =>
      RecentlyViewedResponseSchema.parse({
        code: 0,
        data: {
          items: [
            {
              drama_id: 'invalid-uuid',
              title: '非法 UUID',
              cover_url: null,
              episode_number: 1,
              progress: 10,
              updated_at: '2026-07-27T15:20:00.000Z',
            },
          ],
        },
        message: 'ok',
      }),
    ).toThrow();

    expect(() =>
      RecentlyViewedResponseSchema.parse({
        code: 0,
        data: {
          items: [
            {
              drama_id: '550e8400-e29b-41d4-a716-446655440001',
              title: '负进度',
              cover_url: null,
              episode_number: 1,
              progress: -1,
              updated_at: '2026-07-27T15:20:00.000Z',
            },
          ],
        },
        message: 'ok',
      }),
    ).toThrow();

    expect(() =>
      RecentlyViewedResponseSchema.parse({
        code: 0,
        data: {
          items: [
            {
              drama_id: '550e8400-e29b-41d4-a716-446655440001',
              title: '非法集数',
              cover_url: null,
              episode_number: 0,
              progress: 10,
              updated_at: '2026-07-27T15:20:00.000Z',
            },
          ],
        },
        message: 'ok',
      }),
    ).toThrow();
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
