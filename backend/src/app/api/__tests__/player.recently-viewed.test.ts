import { describe, it, expect, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';
import { GET } from '../player/recently-viewed/route';
import {
  resetRepositoryRegistry,
  setEpisodeRepository,
  setPlaybackHistoryRepository,
} from '@/repositories/repository-registry';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';
import { PlaybackHistoryMockRepository } from '@/repositories/mock/playback-history.mock.repository';

const PLAYBACK_SESSION_ID = '770e8400-e29b-41d4-a716-446655440000';
const OTHER_SESSION_ID = '770e8400-e29b-41d4-a716-446655440999';

describe('GET /api/player/recently-viewed', () => {
  beforeEach(() => {
    resetRepositoryRegistry();
  });

  it('should return recently viewed items for valid header', async () => {
    const historyRepository = new PlaybackHistoryMockRepository();
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
      episode_id: '660e8400-e29b-41d4-a716-446655440001',
      progress: 120,
      duration: 180,
      updated_at: '2026-07-27T15:20:00.000Z',
    });
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440002',
      episode_id: '660e8400-e29b-41d4-a716-446655440011',
      progress: 80,
      duration: 180,
      updated_at: '2026-07-27T15:19:00.000Z',
    });
    await historyRepository.upsert({
      playback_session_id: OTHER_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440004',
      episode_id: '660e8400-e29b-41d4-a716-446655440021',
      progress: 50,
      duration: 180,
      updated_at: '2026-07-27T15:18:00.000Z',
    });
    setPlaybackHistoryRepository(historyRepository);

    const request = new NextRequest('https://example.com/api/player/recently-viewed', {
      headers: {
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
    });

    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      code: 0,
      data: {
        items: [
          {
            drama_id: '550e8400-e29b-41d4-a716-446655440001',
            title: '逆袭归来后我成了豪门团宠',
            cover_url: 'https://example.com/dramas/001.jpg',
            episode_number: 1,
            progress: 120,
            updated_at: '2026-07-27T15:20:00.000Z',
          },
          {
            drama_id: '550e8400-e29b-41d4-a716-446655440002',
            title: '离婚后前夫跪求复合',
            cover_url: 'https://example.com/dramas/002.jpg',
            episode_number: 1,
            progress: 80,
            updated_at: '2026-07-27T15:19:00.000Z',
          },
        ],
      },
      message: 'ok',
    });
  });

  it('should return empty items when no history exists', async () => {
    const request = new NextRequest('https://example.com/api/player/recently-viewed', {
      headers: {
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
    });

    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      code: 0,
      data: { items: [] },
      message: 'ok',
    });
  });

  it('should return empty items when candidate histories are invalid after filtering', async () => {
    const historyRepository = new PlaybackHistoryMockRepository();
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
      episode_id: '660e8400-e29b-41d4-a716-446655440001',
      progress: 120,
      duration: 180,
      updated_at: '2026-07-27T15:20:00.000Z',
    });
    setPlaybackHistoryRepository(historyRepository);

    const episodeRepository = new EpisodeMockRepository();
    episodeRepository.remove('660e8400-e29b-41d4-a716-446655440001');
    setEpisodeRepository(episodeRepository);

    const request = new NextRequest('https://example.com/api/player/recently-viewed', {
      headers: {
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
    });

    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      code: 0,
      data: { items: [] },
      message: 'ok',
    });
  });

  it('should return 400 when header is missing', async () => {
    const request = new NextRequest('https://example.com/api/player/recently-viewed');
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('INVALID_PLAYBACK_SESSION');
  });

  it('should return 400 when header is invalid', async () => {
    const request = new NextRequest('https://example.com/api/player/recently-viewed', {
      headers: {
        'X-Playback-Session-Id': 'invalid',
      },
    });
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('INVALID_PLAYBACK_SESSION');
  });

  it('should return 500 when recently viewed response mapping fails', async () => {
    const historyRepository = new PlaybackHistoryMockRepository();
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
      episode_id: '660e8400-e29b-41d4-a716-446655440001',
      progress: 120,
      duration: 180,
      updated_at: 'not-an-iso-timestamp',
    });
    setPlaybackHistoryRepository(historyRepository);

    const request = new NextRequest('https://example.com/api/player/recently-viewed', {
      headers: {
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
    });

    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
