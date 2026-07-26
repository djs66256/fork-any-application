import { describe, it, expect, beforeEach } from 'vitest';
import { PlaybackHistoryMockRepository } from '@/repositories/mock/playback-history.mock.repository';

const PLAYBACK_SESSION_ID = '770e8400-e29b-41d4-a716-446655440000';
const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';
const EPISODE_ID = '660e8400-e29b-41d4-a716-446655440001';

describe('PlaybackHistoryMockRepository', () => {
  let repository: PlaybackHistoryMockRepository;

  beforeEach(() => {
    repository = new PlaybackHistoryMockRepository();
  });

  it('should return null when history does not exist', async () => {
    const result = await repository.findLatest(PLAYBACK_SESSION_ID, DRAMA_ID);
    expect(result).toBeNull();
  });

  it('should upsert and return latest history', async () => {
    await repository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: DRAMA_ID,
      episode_id: EPISODE_ID,
      progress: 120,
      duration: 180,
      updated_at: '2026-07-26T00:00:00Z',
    });

    const result = await repository.findLatest(PLAYBACK_SESSION_ID, DRAMA_ID);
    expect(result).not.toBeNull();
    expect(result?.progress).toBe(120);
    expect(result?.duration).toBe(180);
  });

  it('should overwrite existing history for same playback session and drama', async () => {
    await repository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: DRAMA_ID,
      episode_id: EPISODE_ID,
      progress: 120,
      duration: 180,
      updated_at: '2026-07-26T00:00:00Z',
    });

    const updated = await repository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: DRAMA_ID,
      episode_id: '660e8400-e29b-41d4-a716-446655440002',
      progress: 150,
      duration: 200,
      updated_at: '2026-07-26T00:10:00Z',
    });

    expect(updated.episode_id).toBe('660e8400-e29b-41d4-a716-446655440002');
    expect(updated.progress).toBe(150);

    const latest = await repository.findLatest(PLAYBACK_SESSION_ID, DRAMA_ID);
    expect(latest?.episode_id).toBe('660e8400-e29b-41d4-a716-446655440002');
    expect(latest?.updated_at).toBe('2026-07-26T00:10:00Z');
  });
});
