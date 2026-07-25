import { describe, it, expect, beforeEach } from 'vitest';
import { DramaService } from './drama.service';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { Drama } from '@/lib/schemas';

function makeDramaInput(overrides: Partial<Omit<Drama, 'id' | 'created_at' | 'updated_at'>> = {}): Omit<Drama, 'id' | 'created_at' | 'updated_at'> {
  return {
    title: overrides.title ?? 'Test Drama',
    description: overrides.description ?? '',
    cover_url: overrides.cover_url ?? null,
    category: overrides.category ?? 'Test Category',
    episode_count: overrides.episode_count ?? 12,
    tags: overrides.tags ?? [],
    rating: overrides.rating ?? null,
  };
}

describe('DramaService', () => {
  let service: DramaService;
  let repo: DramaMockRepository;

  beforeEach(() => {
    repo = new DramaMockRepository();
    service = new DramaService(repo);
  });

  it('should list seeded homepage dramas by default', async () => {
    const result = await service.listDramas({ page: 1, pageSize: 10 });
    expect(result.data).toHaveLength(10);
    expect(result.pagination.total).toBe(12);
    expect(result.pagination.total_pages).toBe(2);
  });

  it('should return correct second page slice', async () => {
    const result = await service.listDramas({ page: 2, pageSize: 10 });
    expect(result.data).toHaveLength(2);
    expect(result.data[0].id).toBe('550e8400-e29b-41d4-a716-446655440011');
  });

  it('should return empty data for oversized page without failing', async () => {
    const result = await service.listDramas({ page: 999, pageSize: 10 });
    expect(result.data).toEqual([]);
    expect(result.pagination.total).toBe(12);
    expect(result.pagination.total_pages).toBe(2);
  });

  it('should validate repository output against canonical schema', async () => {
    const emptyRepo = new DramaMockRepository([]);
    const emptyService = new DramaService(emptyRepo);
    const created = await emptyRepo.create(makeDramaInput({ title: 'Schema Check', episode_count: 9, tags: ['测试'] }));

    const result = await emptyService.listDramas({ page: 1, pageSize: 10 });
    expect(result.data).toHaveLength(1);
    expect(result.data[0]).toMatchObject({
      id: created.id,
      title: 'Schema Check',
      episode_count: 9,
      tags: ['测试'],
    });
  });

  it('should throw notImplemented for getDramaById', async () => {
    await expect(service.getDramaById('some-id')).rejects.toThrow(/not implemented/i);
  });

  it('should throw notImplemented for createDrama', async () => {
    await expect(service.createDrama(makeDramaInput())).rejects.toThrow(/not implemented/i);
  });
});
