import { beforeEach, describe, expect, it } from 'vitest';
import { SystemMessageMockRepository } from '@/repositories/mock/system-message.mock.repository';

describe('SystemMessageMockRepository', () => {
  let repository: SystemMessageMockRepository;

  beforeEach(() => {
    repository = new SystemMessageMockRepository();
  });

  it('should return latest preview from seeded fixture', async () => {
    const result = await repository.getLatest();

    expect(result).toMatchObject({
      id: '550e8400-e29b-41d4-a716-446655440003',
      title: '系统通知',
    });
  });

  it('should paginate system messages with canonical pagination fields', async () => {
    const result = await repository.list({ page: 1, pageSize: 2 });

    expect(result.data).toHaveLength(2);
    expect(result.pagination).toEqual({
      page: 1,
      page_size: 2,
      total: 3,
      total_pages: 2,
    });
  });

  it('should support empty fixtures', async () => {
    const emptyRepository = new SystemMessageMockRepository([]);

    expect(await emptyRepository.getLatest()).toBeNull();
    await expect(emptyRepository.list({ page: 1, pageSize: 20 })).resolves.toEqual({
      data: [],
      pagination: {
        page: 1,
        page_size: 20,
        total: 0,
        total_pages: 0,
      },
    });
  });
});
