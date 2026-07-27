import { describe, it, expect, beforeEach } from 'vitest';
import { PlayerService } from './player.service';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';
import { PlaybackHistoryMockRepository } from '@/repositories/mock/playback-history.mock.repository';

const PLAYBACK_SESSION_ID = '770e8400-e29b-41d4-a716-446655440000';
const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';
const EPISODE_ID = '660e8400-e29b-41d4-a716-446655440001';
const UNPLAYABLE_EPISODE_ID = '660e8400-e29b-41d4-a716-446655440003';

describe('PlayerService', () => {
  let service: PlayerService;
  let historyRepository: PlaybackHistoryMockRepository;
  let episodeRepository: EpisodeMockRepository;

  beforeEach(() => {
    historyRepository = new PlaybackHistoryMockRepository();
    episodeRepository = new EpisodeMockRepository();
    service = new PlayerService(
      new DramaMockRepository(),
      episodeRepository,
      historyRepository,
    );
  });

  it('should return has_history=false when playback history does not exist', async () => {
    const result = await service.getPlaybackProgress(PLAYBACK_SESSION_ID, DRAMA_ID);

    expect(result.data.has_history).toBe(false);
    expect(result.data.episode_id).toBeNull();
    expect(result.data.start_time).toBe(0);
  });

  it('should return playback history when history exists', async () => {
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: DRAMA_ID,
      episode_id: EPISODE_ID,
      progress: 120,
      duration: 180,
      updated_at: '2026-07-26T00:00:00Z',
    });

    const result = await service.getPlaybackProgress(PLAYBACK_SESSION_ID, DRAMA_ID);
    expect(result.data.has_history).toBe(true);
    expect(result.data.episode_id).toBe(EPISODE_ID);
    expect(result.data.start_time).toBe(120);
  });

  it('should return has_history=false when referenced episode becomes invalid', async () => {
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: DRAMA_ID,
      episode_id: EPISODE_ID,
      progress: 120,
      duration: 180,
      updated_at: '2026-07-26T00:00:00Z',
    });
    episodeRepository.remove(EPISODE_ID);

    const result = await service.getPlaybackProgress(PLAYBACK_SESSION_ID, DRAMA_ID);
    expect(result.data.has_history).toBe(false);
    expect(result.data.episode_id).toBeNull();
  });

  it('should start playback for playable episode', async () => {
    const result = await service.startPlayback(PLAYBACK_SESSION_ID, DRAMA_ID, EPISODE_ID, 30);

    expect(result.data.drama_id).toBe(DRAMA_ID);
    expect(result.data.episode_id).toBe(EPISODE_ID);
    expect(result.data.accepted_progress).toBe(30);
    expect(result.data.playback_session_id).toBe(PLAYBACK_SESSION_ID);
  });

  it('should return recently viewed empty response when no history exists', async () => {
    const result = await service.getRecentlyViewed(PLAYBACK_SESSION_ID);

    expect(result).toEqual({
      code: 0,
      data: { items: [] },
      message: 'ok',
    });
  });

  it('should filter invalid histories and return up to three valid recently viewed items', async () => {
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
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440003',
      episode_id: '660e8400-e29b-41d4-a716-446655440099',
      progress: 60,
      duration: 180,
      updated_at: '2026-07-27T15:18:00.000Z',
    });
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440004',
      episode_id: '660e8400-e29b-41d4-a716-446655440021',
      progress: 40,
      duration: 180,
      updated_at: '2026-07-27T15:17:00.000Z',
    });
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440005',
      episode_id: '660e8400-e29b-41d4-a716-446655440022',
      progress: 20,
      duration: 180,
      updated_at: '2026-07-27T15:16:00.000Z',
    });

    episodeRepository.addSeed({
      id: '660e8400-e29b-41d4-a716-446655440021',
      drama_id: '550e8400-e29b-41d4-a716-446655440004',
      title: '第 1 集',
      episode_number: 1,
      duration: 200,
      video_url: 'https://example.com/dramas/004/episode-1.mp4',
      thumbnail_url: 'https://example.com/dramas/004/episode-1.jpg',
      description: '第一集简介',
      created_at: '2026-07-27T14:00:00.000Z',
      updated_at: '2026-07-27T14:00:00.000Z',
    });
    episodeRepository.addSeed({
      id: '660e8400-e29b-41d4-a716-446655440022',
      drama_id: '550e8400-e29b-41d4-a716-446655440005',
      title: '第 1 集',
      episode_number: 1,
      duration: 200,
      video_url: 'https://example.com/dramas/005/episode-1.mp4',
      thumbnail_url: 'https://example.com/dramas/005/episode-1.jpg',
      description: '第一集简介',
      created_at: '2026-07-27T14:00:00.000Z',
      updated_at: '2026-07-27T14:00:00.000Z',
    });

    const result = await service.getRecentlyViewed(PLAYBACK_SESSION_ID);

    expect(result.data.items).toHaveLength(3);
    expect(result.data.items.map((item) => item.drama_id)).toEqual([
      '550e8400-e29b-41d4-a716-446655440001',
      '550e8400-e29b-41d4-a716-446655440002',
      '550e8400-e29b-41d4-a716-446655440004',
    ]);
    expect(result.data.items[0]).toMatchObject({
      title: '逆袭归来后我成了豪门团宠',
      episode_number: 1,
      progress: 120,
    });
    expect(result.data.items[1]).toMatchObject({
      title: '离婚后前夫跪求复合',
      episode_number: 1,
      progress: 80,
    });
  });

  it('should throw EPISODE_NOT_FOUND when episode does not belong to drama', async () => {
    await expect(
      service.startPlayback(
        PLAYBACK_SESSION_ID,
        DRAMA_ID,
        '660e8400-e29b-41d4-a716-446655440011',
        0,
      ),
    ).rejects.toMatchObject({ code: 'EPISODE_NOT_FOUND' });
  });

  it('should throw EPISODE_NOT_PLAYABLE when episode has no resource', async () => {
    await expect(
      service.startPlayback(PLAYBACK_SESSION_ID, DRAMA_ID, UNPLAYABLE_EPISODE_ID, 0),
    ).rejects.toMatchObject({ code: 'EPISODE_NOT_PLAYABLE' });
  });

  it('should save stop progress and clamp to duration', async () => {
    const result = await service.stopPlayback(PLAYBACK_SESSION_ID, DRAMA_ID, EPISODE_ID, 500, 180);

    expect(result.data.saved_progress).toBe(180);
    expect(result.data.duration).toBe(180);

    const saved = await historyRepository.findLatest(PLAYBACK_SESSION_ID, DRAMA_ID);
    expect(saved?.progress).toBe(180);
  });

  it('should overwrite previous stop progress for same session and drama', async () => {
    await service.stopPlayback(PLAYBACK_SESSION_ID, DRAMA_ID, EPISODE_ID, 30, 180);
    const result = await service.stopPlayback(PLAYBACK_SESSION_ID, DRAMA_ID, EPISODE_ID, 80, 180);

    expect(result.data.saved_progress).toBe(80);

    const saved = await historyRepository.findLatest(PLAYBACK_SESSION_ID, DRAMA_ID);
    expect(saved?.progress).toBe(80);
  });

  it('should throw INTERNAL_ERROR when recently viewed response mapping fails', async () => {
    await historyRepository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
      episode_id: '660e8400-e29b-41d4-a716-446655440001',
      progress: 120,
      duration: 180,
      updated_at: 'not-an-iso-timestamp',
    });

    await expect(service.getRecentlyViewed(PLAYBACK_SESSION_ID)).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
      message: 'Failed to map recently viewed response',
    });
  });

  it('should throw DRAMA_NOT_FOUND when drama does not exist', async () => {
    await expect(
      service.getPlaybackProgress(PLAYBACK_SESSION_ID, '550e8400-e29b-41d4-a716-446655449999'),
    ).rejects.toMatchObject({ code: 'DRAMA_NOT_FOUND' });
  });
});
