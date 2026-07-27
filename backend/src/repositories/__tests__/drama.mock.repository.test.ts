import { describe, it, expect, beforeEach } from 'vitest';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { Drama } from '@/lib/schemas';

function makeDrama(overrides: Partial<Drama> = {}): Omit<Drama, 'id' | 'created_at' | 'updated_at'> {
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

describe('DramaMockRepository', () => {
  let repo: DramaMockRepository;

  beforeEach(() => {
    repo = new DramaMockRepository();
  });

  it('should seed homepage dramas by default', async () => {
    const result = await repo.findMany({ page: 1, pageSize: 10 });
    expect(result.data).toHaveLength(10);
    expect(result.pagination.total).toBe(12);
    expect(result.pagination.total_pages).toBe(2);
    expect(result.data[0]).toMatchObject({
      title: '逆袭归来后我成了豪门团宠',
      episode_count: 68,
      tags: ['逆袭', '豪门'],
    });
  });

  it('should support empty repositories when initialized with no data', async () => {
    const emptyRepo = new DramaMockRepository([]);
    const result = await emptyRepo.findMany({ page: 1, pageSize: 10 });
    expect(result.data).toEqual([]);
    expect(result.pagination.total).toBe(0);
    expect(result.pagination.total_pages).toBe(0);
  });

  it('should create a drama and return it', async () => {
    const emptyRepo = new DramaMockRepository([]);
    const drama = await emptyRepo.create(makeDrama());
    expect(drama.id).toBeDefined();
    expect(drama.title).toBe('Test Drama');
    expect(drama.created_at).toBeDefined();
    expect(drama.updated_at).toBeDefined();
  });

  it('should find a created drama by id', async () => {
    const emptyRepo = new DramaMockRepository([]);
    const created = await emptyRepo.create(makeDrama({ title: 'Found Me' }));
    const found = await emptyRepo.findById(created.id);
    expect(found).not.toBeNull();
    expect(found!.title).toBe('Found Me');
  });

  it('should return null for non-existent id', async () => {
    const found = await repo.findById('non-existent-id');
    expect(found).toBeNull();
  });

  it('should update a drama', async () => {
    const emptyRepo = new DramaMockRepository([]);
    const created = await emptyRepo.create(makeDrama({ title: 'Original' }));
    const updated = await emptyRepo.update(created.id, { title: 'Updated', tags: ['已更新'] });
    expect(updated).not.toBeNull();
    expect(updated!.title).toBe('Updated');
    expect(updated!.tags).toEqual(['已更新']);
    expect(new Date(updated!.updated_at).getTime()).toBeGreaterThanOrEqual(
      new Date(created.updated_at).getTime(),
    );
  });

  it('should return null when updating non-existent drama', async () => {
    const result = await repo.update('nonexistent', { title: 'Nope' });
    expect(result).toBeNull();
  });

  it('should delete a drama and not find it afterwards', async () => {
    const emptyRepo = new DramaMockRepository([]);
    const created = await emptyRepo.create(makeDrama());
    const deleted = await emptyRepo.delete(created.id);
    expect(deleted).toBe(true);
    const found = await emptyRepo.findById(created.id);
    expect(found).toBeNull();
  });

  it('should return false when deleting non-existent drama', async () => {
    const result = await repo.delete('nonexistent');
    expect(result).toBe(false);
  });

  it('should paginate seeded dramas correctly across multiple pages', async () => {
    const page1 = await repo.findMany({ page: 1, pageSize: 10 });
    const page2 = await repo.findMany({ page: 2, pageSize: 10 });
    const page999 = await repo.findMany({ page: 999, pageSize: 10 });

    expect(page1.data).toHaveLength(10);
    expect(page2.data).toHaveLength(2);
    expect(page2.pagination.page).toBe(2);
    expect(page2.pagination.total).toBe(12);
    expect(page2.pagination.total_pages).toBe(2);
    expect(page2.data[0].id).toBe('550e8400-e29b-41d4-a716-446655440011');

    expect(page999.data).toEqual([]);
    expect(page999.pagination.total).toBe(12);
    expect(page999.pagination.total_pages).toBe(2);
  });

  it('should search dramas by title with case-insensitive contains matching', async () => {
    const result = await repo.search({ q: '后', page: 1, pageSize: 10 });

    expect(result.data.map((item) => item.title)).toEqual([
      '逆袭归来后我成了豪门团宠',
      '离婚后前夫跪求复合',
      '我在八零年代当后妈',
      '重生后我把渣男送进火葬场',
      '误撩禁欲教授后她红了',
      '替嫁后她成了京圈白月光',
    ]);
    expect(result.pagination.total).toBe(6);
    expect(result.pagination.total_pages).toBe(1);
  });

  it('should search dramas by category with case-insensitive contains matching', async () => {
    const result = await repo.search({ q: '都市', page: 1, pageSize: 10 });

    expect(result.data).toHaveLength(2);
    expect(result.data.every((item) => item.category === '都市')).toBe(true);
  });

  it('should return empty search results for oversized pages while preserving pagination', async () => {
    const result = await repo.search({ q: '后', page: 999, pageSize: 10 });

    expect(result.data).toEqual([]);
    expect(result.pagination).toEqual({
      page: 999,
      page_size: 10,
      total: 6,
      total_pages: 1,
    });
  });

  it('should return hot search items with stable rank and size constraints', async () => {
    const result = await repo.listHotSearches();

    expect(result.data.length).toBeGreaterThan(0);
    expect(result.data.length).toBeLessThanOrEqual(10);
    expect(result.data[0]).toEqual({
      rank: 1,
      keyword: '逆袭',
      score: 9821,
    });
    expect(result.data.every((item, index) => item.rank === index + 1)).toBe(true);
  });
});
