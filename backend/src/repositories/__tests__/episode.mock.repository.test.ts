import { describe, it, expect, beforeEach } from 'vitest';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';
import { Episode } from '@/lib/schemas';

function makeEpisode(overrides: Partial<Episode> = {}): Episode {
  return {
    id: overrides.id ?? '550e8400-e29b-41d4-a716-446655440001',
    drama_id: overrides.drama_id ?? '550e8400-e29b-41d4-a716-446655440000',
    title: overrides.title ?? 'Episode 1',
    episode_number: overrides.episode_number ?? 1,
    duration: overrides.duration ?? null,
    video_url: overrides.video_url ?? null,
    thumbnail_url: overrides.thumbnail_url ?? null,
    description: overrides.description ?? null,
    created_at: overrides.created_at ?? '2026-07-24T00:00:00.000Z',
    updated_at: overrides.updated_at ?? '2026-07-24T00:00:00.000Z',
  };
}

describe('EpisodeMockRepository', () => {
  let repo: EpisodeMockRepository;

  beforeEach(() => {
    repo = new EpisodeMockRepository();
  });

  it('should return empty array for drama with no episodes', async () => {
    const result = await repo.findByDramaId('drama-no-episodes');
    expect(result).toEqual([]);
  });

  it('should return null for non-existent episode id', async () => {
    const result = await repo.findById('non-existent');
    expect(result).toBeNull();
  });

  it('should find seeded episode by id', async () => {
    const episode = makeEpisode({ id: 'ep-001', title: 'Pilot' });
    repo.addSeed(episode);
    const found = await repo.findById('ep-001');
    expect(found).not.toBeNull();
    expect(found!.title).toBe('Pilot');
  });

  it('should find episodes by drama id', async () => {
    const dramaId = '550e8400-e29b-41d4-a716-446655440000';
    repo.addSeed(makeEpisode({ id: 'ep-1', drama_id: dramaId, title: 'Ep 1', episode_number: 1 }));
    repo.addSeed(makeEpisode({ id: 'ep-2', drama_id: dramaId, title: 'Ep 2', episode_number: 2 }));
    repo.addSeed(makeEpisode({ id: 'ep-3', drama_id: 'other-drama', title: 'Other' }));

    const results = await repo.findByDramaId(dramaId);
    expect(results).toHaveLength(2);
    expect(results.map((e) => e.title)).toContain('Ep 1');
    expect(results.map((e) => e.title)).toContain('Ep 2');
  });

  it('should return empty array for drama id with no episodes', async () => {
    repo.addSeed(makeEpisode({ id: 'ep-1', drama_id: 'drama-a' }));
    const results = await repo.findByDramaId('drama-b');
    expect(results).toEqual([]);
  });
});
