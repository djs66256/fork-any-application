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

  it('should throw DRAMA_NOT_FOUND when drama does not exist', async () => {
    await expect(
      service.getPlaybackProgress(PLAYBACK_SESSION_ID, '550e8400-e29b-41d4-a716-446655449999'),
    ).rejects.toMatchObject({ code: 'DRAMA_NOT_FOUND' });
  });
});
