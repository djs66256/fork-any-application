import { describe, it, expect, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';
import { GET } from '../dramas/[id]/episodes/route';
import {
  resetRepositoryRegistry,
  setDramaRepository,
  setEpisodeRepository,
} from '@/repositories/repository-registry';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';

const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';

describe('GET /api/dramas/[id]/episodes', () => {
  beforeEach(() => {
    resetRepositoryRegistry();
  });

  it('should return canonical episode list sorted by episode_number', async () => {
    const episodeRepository = new EpisodeMockRepository([]);
    episodeRepository.addSeed({
      id: '660e8400-e29b-41d4-a716-446655440003',
      drama_id: DRAMA_ID,
      title: '第 3 集',
      episode_number: 3,
      duration: 200,
      video_url: 'https://example.com/3.mp4',
      thumbnail_url: null,
      description: null,
      created_at: '2026-07-26T00:02:00Z',
      updated_at: '2026-07-26T00:02:00Z',
    });
    episodeRepository.addSeed({
      id: '660e8400-e29b-41d4-a716-446655440001',
      drama_id: DRAMA_ID,
      title: '第 1 集',
      episode_number: 1,
      duration: 180,
      video_url: 'https://example.com/1.mp4',
      thumbnail_url: null,
      description: null,
      created_at: '2026-07-26T00:00:00Z',
      updated_at: '2026-07-26T00:00:00Z',
    });
    episodeRepository.addSeed({
      id: '660e8400-e29b-41d4-a716-446655440002',
      drama_id: DRAMA_ID,
      title: '第 2 集',
      episode_number: 2,
      duration: 190,
      video_url: 'https://example.com/2.mp4',
      thumbnail_url: null,
      description: null,
      created_at: '2026-07-26T00:01:00Z',
      updated_at: '2026-07-26T00:01:00Z',
    });
    setEpisodeRepository(episodeRepository);

    const request = new NextRequest(`https://example.com/api/dramas/${DRAMA_ID}/episodes`);
    const response = await GET(request, { params: Promise.resolve({ id: DRAMA_ID }) });
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.code).toBe(0);
    expect(body.message).toBe('ok');
    expect(body.data.drama_id).toBe(DRAMA_ID);
    expect(body.data.items.map((item: { episode_number: number }) => item.episode_number)).toEqual([1, 2, 3]);
  });

  it('should return empty items when drama exists but has no episodes', async () => {
    setEpisodeRepository(new EpisodeMockRepository([]));

    const request = new NextRequest(`https://example.com/api/dramas/${DRAMA_ID}/episodes`);
    const response = await GET(request, { params: Promise.resolve({ id: DRAMA_ID }) });
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data.items).toEqual([]);
  });

  it('should return 404 when drama does not exist', async () => {
    const dramaRepository = new DramaMockRepository([]);
    setDramaRepository(dramaRepository);
    setEpisodeRepository(new EpisodeMockRepository([]));

    const request = new NextRequest('https://example.com/api/dramas/550e8400-e29b-41d4-a716-446655449999/episodes');
    const response = await GET(request, {
      params: Promise.resolve({ id: '550e8400-e29b-41d4-a716-446655449999' }),
    });
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error.code).toBe('DRAMA_NOT_FOUND');
  });

  it('should return 400 for invalid drama id', async () => {
    const request = new NextRequest('https://example.com/api/dramas/invalid/episodes');
    const response = await GET(request, { params: Promise.resolve({ id: 'invalid' }) });
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });
});
