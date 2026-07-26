import { describe, it, expect, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';
import { GET } from '../player/progress/route';
import {
  resetRepositoryRegistry,
  setEpisodeRepository,
  setPlaybackHistoryRepository,
} from '@/repositories/repository-registry';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';
import { PlaybackHistoryMockRepository } from '@/repositories/mock/playback-history.mock.repository';

const PLAYBACK_SESSION_ID = '770e8400-e29b-41d4-a716-446655440000';
const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';
const EPISODE_ID = '660e8400-e29b-41d4-a716-446655440001';

describe('GET /api/player/progress', () => {
  beforeEach(() => {
    resetRepositoryRegistry();
  });

  it('should return has_history=false when no history exists', async () => {
    const request = new NextRequest(`https://example.com/api/player/progress?dramaId=${DRAMA_ID}`, {
      headers: {
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
    });

    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.code).toBe(0);
    expect(body.data.has_history).toBe(false);
    expect(body.data.episode_id).toBeNull();
  });

  it('should return history when latest history exists', async () => {
    const historyRepository = new PlaybackHistoryMockRepository();
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: DRAMA_ID,
      episode_id: EPISODE_ID,
      progress: 120,
      duration: 180,
      updated_at: '2026-07-26T00:00:00Z',
    });
    setPlaybackHistoryRepository(historyRepository);

    const request = new NextRequest(`https://example.com/api/player/progress?dramaId=${DRAMA_ID}`, {
      headers: {
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
    });

    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data.has_history).toBe(true);
    expect(body.data.episode_id).toBe(EPISODE_ID);
    expect(body.data.start_time).toBe(120);
  });

  it('should return has_history=false when history references invalid episode', async () => {
    const historyRepository = new PlaybackHistoryMockRepository();
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: DRAMA_ID,
      episode_id: EPISODE_ID,
      progress: 120,
      duration: 180,
      updated_at: '2026-07-26T00:00:00Z',
    });

    const episodeRepository = new EpisodeMockRepository();
    episodeRepository.remove(EPISODE_ID);

    setPlaybackHistoryRepository(historyRepository);
    setEpisodeRepository(episodeRepository);

    const request = new NextRequest(`https://example.com/api/player/progress?dramaId=${DRAMA_ID}`, {
      headers: {
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
    });

    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data.has_history).toBe(false);
    expect(body.data.episode_id).toBeNull();
  });

  it('should return 400 when header is missing', async () => {
    const request = new NextRequest(`https://example.com/api/player/progress?dramaId=${DRAMA_ID}`);
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('INVALID_PLAYBACK_SESSION');
  });

  it('should return 400 when header is invalid', async () => {
    const request = new NextRequest(`https://example.com/api/player/progress?dramaId=${DRAMA_ID}`, {
      headers: {
        'X-Playback-Session-Id': 'invalid',
      },
    });
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('INVALID_PLAYBACK_SESSION');
  });
});
