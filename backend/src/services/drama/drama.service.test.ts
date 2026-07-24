import { describe, it, expect, beforeEach } from 'vitest';
import { DramaService } from './drama.service';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { Drama } from '@/lib/schemas';

function makeDramaInput(overrides: Partial<Omit<Drama, 'id' | 'created_at' | 'updated_at'>> = {}): Omit<Drama, 'id' | 'created_at' | 'updated_at'> {
  return {
    title: overrides.title ?? 'Test Drama',
    description: overrides.description ?? null,
    cover_url: overrides.cover_url ?? null,
    category: overrides.category ?? null,
    total_episodes: overrides.total_episodes ?? 12,
    release_year: overrides.release_year ?? null,
    rating: overrides.rating ?? null,
    status: overrides.status ?? 'ongoing',
    play_count: overrides.play_count ?? 0,
  };
}

describe('DramaService', () => {
  let service: DramaService;
  let repo: DramaMockRepository;

  beforeEach(() => {
    repo = new DramaMockRepository();
    service = new DramaService(repo);
  });

  it('should list dramas (empty when no data)', async () => {
    const result = await service.listDramas({ page: 1, pageSize: 10 });
    expect(result.data).toEqual([]);
    expect(result.pagination.total).toBe(0);
  });

  it('should list dramas after creating some via repo', async () => {
    await repo.create(makeDramaInput({ title: 'Drama A' }));
    await repo.create(makeDramaInput({ title: 'Drama B' }));

    const result = await service.listDramas({ page: 1, pageSize: 10 });
    expect(result.data).toHaveLength(2);
    expect(result.pagination.total).toBe(2);
  });

  it('should throw notImplemented for getDramaById', async () => {
    await expect(service.getDramaById('some-id')).rejects.toThrow(/not implemented/i);
  });

  it('should throw notImplemented for createDrama', async () => {
    await expect(service.createDrama(makeDramaInput())).rejects.toThrow(/not implemented/i);
  });
});
