import { describe, it, expect, beforeEach } from 'vitest';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { Drama } from '@/lib/schemas';

function makeDrama(overrides: Partial<Drama> = {}): Omit<Drama, 'id' | 'created_at' | 'updated_at'> {
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

describe('DramaMockRepository', () => {
  let repo: DramaMockRepository;

  beforeEach(() => {
    repo = new DramaMockRepository();
  });

  it('should return empty list with correct pagination for empty repo', async () => {
    const result = await repo.findMany({ page: 1, pageSize: 10 });
    expect(result.data).toEqual([]);
    expect(result.pagination.total).toBe(0);
    expect(result.pagination.total_pages).toBe(0);
    expect(result.pagination.page).toBe(1);
    expect(result.pagination.page_size).toBe(10);
  });

  it('should create a drama and return it', async () => {
    const drama = await repo.create(makeDrama());
    expect(drama.id).toBeDefined();
    expect(drama.title).toBe('Test Drama');
    expect(drama.created_at).toBeDefined();
    expect(drama.updated_at).toBeDefined();
  });

  it('should find a created drama by id', async () => {
    const created = await repo.create(makeDrama({ title: 'Found Me' }));
    const found = await repo.findById(created.id);
    expect(found).not.toBeNull();
    expect(found!.title).toBe('Found Me');
  });

  it('should return null for non-existent id', async () => {
    const found = await repo.findById('non-existent-id');
    expect(found).toBeNull();
  });

  it('should update a drama', async () => {
    const created = await repo.create(makeDrama({ title: 'Original' }));
    const updated = await repo.update(created.id, { title: 'Updated' });
    expect(updated).not.toBeNull();
    expect(updated!.title).toBe('Updated');
    // updated_at should be >= created_at (same ms possible in fast test)
    expect(new Date(updated!.updated_at).getTime()).toBeGreaterThanOrEqual(
      new Date(created.updated_at).getTime(),
    );
  });

  it('should return null when updating non-existent drama', async () => {
    const result = await repo.update('nonexistent', { title: 'Nope' });
    expect(result).toBeNull();
  });

  it('should delete a drama and not find it afterwards', async () => {
    const created = await repo.create(makeDrama());
    const deleted = await repo.delete(created.id);
    expect(deleted).toBe(true);
    const found = await repo.findById(created.id);
    expect(found).toBeNull();
  });

  it('should return false when deleting non-existent drama', async () => {
    const result = await repo.delete('nonexistent');
    expect(result).toBe(false);
  });

  it('should paginate multiple dramas correctly', async () => {
    // Create 25 dramas
    for (let i = 0; i < 25; i++) {
      await repo.create(makeDrama({ title: `Drama ${i}` }));
    }

    // Page 1, size 10
    const page1 = await repo.findMany({ page: 1, pageSize: 10 });
    expect(page1.data).toHaveLength(10);
    expect(page1.pagination.total).toBe(25);
    expect(page1.pagination.total_pages).toBe(3);

    // Page 2, size 10
    const page2 = await repo.findMany({ page: 2, pageSize: 10 });
    expect(page2.data).toHaveLength(10);
    expect(page2.pagination.page).toBe(2);

    // Page 3, size 10 (should have 5 items)
    const page3 = await repo.findMany({ page: 3, pageSize: 10 });
    expect(page3.data).toHaveLength(5);
  });
});
