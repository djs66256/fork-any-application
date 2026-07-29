import { beforeEach, describe, expect, it } from 'vitest';
import { InteractionMessageMockRepository } from '@/repositories/mock/interaction-message.mock.repository';

const USER_ID = '00000000-0000-4000-8000-13800138000';

describe('InteractionMessageMockRepository', () => {
  let repository: InteractionMessageMockRepository;

  beforeEach(() => {
    repository = new InteractionMessageMockRepository();
  });

  it('should return seeded messages for logged-in users', async () => {
    const result = await repository.listByUser({
      userId: USER_ID,
      page: 1,
      pageSize: 2,
    });

    expect(result.data).toHaveLength(2);
    expect(result.data[0]).toMatchObject({
      id: '660e8400-e29b-41d4-a716-446655440010',
      type: 'comment_reply',
    });
    expect(result.pagination).toEqual({
      page: 1,
      page_size: 2,
      total: 3,
      total_pages: 2,
    });
  });

  it('should return empty lists for users without seeded fixtures', async () => {
    const result = await repository.listByUser({
      userId: '00000000-0000-4000-8000-13900139000',
      page: 1,
      pageSize: 20,
    });

    expect(result).toEqual({
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
