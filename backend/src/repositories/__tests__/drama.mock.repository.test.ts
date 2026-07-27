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
    expect(found?.title).toBe('Found Me');
  });

  it('should return null for non-existent id', async () => {
    const found = await repo.findById('123e4567-e89b-12d3-a456-426614174999');
    expect(found).toBeNull();
  });

  it('should update a drama', async () => {
    const emptyRepo = new DramaMockRepository([]);
    const created = await emptyRepo.create(makeDrama({ title: 'Original' }));
    const updated = await emptyRepo.update(created.id, { title: 'Updated', tags: ['已更新'] });
    expect(updated).not.toBeNull();
    expect(updated?.title).toBe('Updated');
    expect(updated?.tags).toEqual(['已更新']);
    expect(new Date(updated?.updated_at ?? 0).getTime()).toBeGreaterThanOrEqual(
      new Date(created.updated_at).getTime(),
    );
  });

  it('should return null when updating non-existent drama', async () => {
    const result = await repo.update('123e4567-e89b-12d3-a456-426614174999', { title: 'Nope' });
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
    const result = await repo.delete('123e4567-e89b-12d3-a456-426614174999');
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
    expect(page2.data[0]?.id).toBe('550e8400-e29b-41d4-a716-446655440011');

    expect(page999.data).toEqual([]);
    expect(page999.pagination.total).toBe(12);
    expect(page999.pagination.total_pages).toBe(2);
  });

  it('should return fixed dimensions for male classification tags', async () => {
    const result = await repo.listClassificationTags({ gender: 'male' });

    expect(result).toEqual({
      gender: 'male',
      dimensions: [
        { key: 'era_background', name: '时代背景', tags: ['都市', '古风', '年代'] },
        { key: 'theme_plot', name: '主题情节', tags: ['逆袭', '系统', '复仇'] },
        { key: 'character_setting', name: '角色设定', tags: ['总裁', '萌宝'] },
      ],
    });
  });

  it('should preserve empty dimensions for female classification tags', async () => {
    const result = await repo.listClassificationTags({ gender: 'female' });

    expect(result.dimensions).toHaveLength(3);
    expect(result.dimensions[2]).toEqual({
      key: 'character_setting',
      name: '角色设定',
      tags: [],
    });
  });

  it('should merge all classification tags with stable dedupe order', async () => {
    const result = await repo.listClassificationTags({ gender: 'all' });

    expect(result.dimensions).toEqual([
      { key: 'era_background', name: '时代背景', tags: ['都市', '古风', '年代', '校园', '豪门'] },
      { key: 'theme_plot', name: '主题情节', tags: ['逆袭', '系统', '复仇', '甜宠', '穿书', '重生'] },
      { key: 'character_setting', name: '角色设定', tags: ['总裁', '萌宝'] },
    ]);
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

  it('should search dramas by tags in default mock chain', async () => {
    const result = await repo.search({ q: '萌宝', page: 1, pageSize: 10 });

    expect(result.data.map((item) => item.title)).toEqual(['天降萌宝总裁爹地别太宠']);
    expect(result.data[0]?.tags).toContain('萌宝');
    expect(result.pagination.total).toBe(1);
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

  it('should filter live action dramas and sort hot rankings by play_count desc', async () => {
    const result = await repo.listRankings({
      contentType: 'live_action',
      type: 'hot',
      page: 1,
      pageSize: 10,
    });

    expect(result.data.length).toBeGreaterThan(0);
    expect(result.data.every((item) => item.content_type === 'live_action')).toBe(true);
    expect(result.data[0]?.play_count).toBeGreaterThanOrEqual(result.data[1]?.play_count ?? 0);
  });

  it('should support recommendation and booking rankings with oversized pages', async () => {
    const recommendResult = await repo.listRankings({
      contentType: 'all',
      type: 'recommend',
      page: 1,
      pageSize: 10,
    });
    const bookingResult = await repo.listRankings({
      contentType: 'all',
      type: 'booking',
      page: 1,
      pageSize: 10,
    });
    const oversized = await repo.listRankings({
      contentType: 'all',
      type: 'booking',
      page: 999,
      pageSize: 10,
    });

    expect(recommendResult.data[0]?.recommendation_score).toBeGreaterThanOrEqual(
      recommendResult.data[1]?.recommendation_score ?? 0,
    );
    expect(bookingResult.data[0]?.booking_count).toBeGreaterThanOrEqual(
      bookingResult.data[1]?.booking_count ?? 0,
    );
    expect(oversized.data).toEqual([]);
    expect(oversized.pagination.total).toBe(12);
  });

  it('should expose is_booked only for the requesting user context', async () => {
    const firstDramaId = '550e8400-e29b-41d4-a716-446655440001';
    await repo.bookDrama({ dramaId: firstDramaId, userId: 'user-1' });

    const anonymous = await repo.listRankings({
      contentType: 'all',
      type: 'booking',
      page: 1,
      pageSize: 10,
    });
    const userOne = await repo.listRankings(
      { contentType: 'all', type: 'booking', page: 1, pageSize: 10 },
      { userId: 'user-1' },
    );
    const userTwo = await repo.listRankings(
      { contentType: 'all', type: 'booking', page: 1, pageSize: 10 },
      { userId: 'user-2' },
    );

    expect(anonymous.data.find((item) => item.id === firstDramaId)?.is_booked).toBe(false);
    expect(userOne.data.find((item) => item.id === firstDramaId)?.is_booked).toBe(true);
    expect(userTwo.data.find((item) => item.id === firstDramaId)?.is_booked).toBe(false);
  });

  it('should book a drama idempotently and increment booking_count only once', async () => {
    const dramaId = '550e8400-e29b-41d4-a716-446655440001';

    const before = await repo.listRankings({ contentType: 'all', type: 'booking', page: 1, pageSize: 20 });
    const originalCount = before.data.find((item) => item.id === dramaId)?.booking_count ?? 0;

    const first = await repo.bookDrama({ dramaId, userId: 'user-1' });
    const second = await repo.bookDrama({ dramaId, userId: 'user-1' });

    expect(first.booked).toBe(true);
    expect(second.booked).toBe(true);
    expect(first.booking_count).toBe(originalCount + 1);
    expect(second.booking_count).toBe(originalCount + 1);
  });

  it('should throw not found when booking a missing drama', async () => {
    await expect(
      repo.bookDrama({
        dramaId: '123e4567-e89b-12d3-a456-426614174999',
        userId: 'user-1',
      }),
    ).rejects.toMatchObject({
      code: 'NOT_FOUND',
    });
  });
});
