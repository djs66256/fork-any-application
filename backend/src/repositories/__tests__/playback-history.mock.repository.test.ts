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

  it('should list recent histories for a playback session ordered by updated_at desc', async () => {
    await repository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440010',
      episode_id: '660e8400-e29b-41d4-a716-446655440010',
      progress: 30,
      duration: 180,
      updated_at: '2026-07-26T00:03:00Z',
    });
    await repository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440011',
      episode_id: '660e8400-e29b-41d4-a716-446655440011',
      progress: 60,
      duration: 180,
      updated_at: '2026-07-26T00:05:00Z',
    });
    await repository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: '550e8400-e29b-41d4-a716-446655440012',
      episode_id: '660e8400-e29b-41d4-a716-446655440012',
      progress: 90,
      duration: 180,
      updated_at: '2026-07-26T00:01:00Z',
    });
    await repository.upsert({
      playback_session_id: '770e8400-e29b-41d4-a716-446655440999',
      drama_id: '550e8400-e29b-41d4-a716-446655440013',
      episode_id: '660e8400-e29b-41d4-a716-446655440013',
      progress: 120,
      duration: 180,
      updated_at: '2026-07-26T00:09:00Z',
    });

    const result = await repository.listRecentBySession(PLAYBACK_SESSION_ID, 2);

    expect(result).toHaveLength(2);
    expect(result.map((item) => item.drama_id)).toEqual([
      '550e8400-e29b-41d4-a716-446655440011',
      '550e8400-e29b-41d4-a716-446655440010',
    ]);
  });

  it('should return cloned results from listRecentBySession', async () => {
    await repository.upsert({
      playback_session_id: PLAYBACK_SESSION_ID,
      drama_id: DRAMA_ID,
      episode_id: EPISODE_ID,
      progress: 120,
      duration: 180,
      updated_at: '2026-07-26T00:00:00Z',
    });

    const result = await repository.listRecentBySession(PLAYBACK_SESSION_ID, 1);
    result[0]!.progress = 0;

    const latest = await repository.findLatest(PLAYBACK_SESSION_ID, DRAMA_ID);
    expect(latest?.progress).toBe(120);
  });
});
