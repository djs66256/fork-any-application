import { describe, it, expect, beforeEach } from 'vitest';
import { EpisodeService } from './episode.service';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';

const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';

describe('EpisodeService', () => {
  let service: EpisodeService;
  let episodeRepository: EpisodeMockRepository;
  let dramaRepository: DramaMockRepository;

  beforeEach(() => {
    episodeRepository = new EpisodeMockRepository([]);
    dramaRepository = new DramaMockRepository();
    service = new EpisodeService(episodeRepository, dramaRepository);
  });

  it('should return episodes sorted by episode_number', async () => {
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

    const result = await service.listEpisodesByDramaId(DRAMA_ID);
    expect(result.data.items.map((item) => item.episode_number)).toEqual([1, 2, 3]);
    expect(result.data.drama_id).toBe(DRAMA_ID);
  });

  it('should return empty items for existing drama without episodes', async () => {
    const result = await service.listEpisodesByDramaId(DRAMA_ID);
    expect(result.data.items).toEqual([]);
    expect(result.data.drama_id).toBe(DRAMA_ID);
  });

  it('should throw DRAMA_NOT_FOUND for unknown drama', async () => {
    await expect(
      service.listEpisodesByDramaId('550e8400-e29b-41d4-a716-446655449999'),
    ).rejects.toMatchObject({ code: 'DRAMA_NOT_FOUND' });
  });

  it('should return episode by id', async () => {
    episodeRepository.addSeed({
      id: '660e8400-e29b-41d4-a716-446655440010',
      drama_id: DRAMA_ID,
      title: '特别篇',
      episode_number: 10,
      duration: 260,
      video_url: 'https://example.com/special.mp4',
      thumbnail_url: null,
      description: null,
      created_at: '2026-07-26T00:10:00Z',
      updated_at: '2026-07-26T00:10:00Z',
    });

    const result = await service.getEpisodeById('660e8400-e29b-41d4-a716-446655440010');
    expect(result.title).toBe('特别篇');
  });

  it('should throw EPISODE_NOT_FOUND for unknown episode', async () => {
    await expect(service.getEpisodeById('660e8400-e29b-41d4-a716-446655449999')).rejects.toMatchObject({
      code: 'EPISODE_NOT_FOUND',
    });
  });
});
