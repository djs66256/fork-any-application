import { beforeEach, describe, expect, it } from 'vitest';
import { MessageService } from './message.service';
import { InteractionMessageMockRepository } from '@/repositories/mock/interaction-message.mock.repository';
import { SystemMessageMockRepository } from '@/repositories/mock/system-message.mock.repository';
import type { InteractionMessageRepositoryInterface } from '@/repositories/interfaces/interaction-message.repository.interface';

const USER_ID = '00000000-0000-4000-8000-13800138000';

class InvalidInteractionRepository implements InteractionMessageRepositoryInterface {
  async listByUser() {
    return {
      data: [{ id: 'bad-id' }],
      pagination: {
        page: 1,
        page_size: 20,
        total: 1,
        total_pages: 1,
      },
    } as never;
  }
}

describe('MessageService', () => {
  let service: MessageService;

  beforeEach(() => {
    service = new MessageService(
      new SystemMessageMockRepository(),
      new InteractionMessageMockRepository(),
      { nowProvider: () => '2026-07-29T10:00:00.000Z' },
    );
  });

  it('should return preview with derived relative time', async () => {
    const result = await service.getPreview();

    expect(result).toEqual({
      title: '系统通知',
      summary: '你收藏的专题上新了 3 部短剧。',
      relative_time: '1分钟前',
    });
  });

  it('should return null preview when no system messages exist', async () => {
    const emptyService = new MessageService(
      new SystemMessageMockRepository([]),
      new InteractionMessageMockRepository(),
      { nowProvider: () => '2026-07-29T10:00:00.000Z' },
    );

    await expect(emptyService.getPreview()).resolves.toBeNull();
  });

  it('should paginate system messages', async () => {
    const result = await service.listSystemMessages({ page: 1, pageSize: 2 });

    expect(result.data).toHaveLength(2);
    expect(result.pagination).toEqual({
      page: 1,
      page_size: 2,
      total: 3,
      total_pages: 2,
    });
  });

  it('should reject anonymous interaction message access', async () => {
    await expect(service.listInteractionMessages({ page: 1, pageSize: 20 })).rejects.toMatchObject({
      code: 'AUTH_UNAUTHORIZED',
    });
  });

  it('should return interaction message lists for logged-in users', async () => {
    const result = await service.listInteractionMessages({
      userId: USER_ID,
      page: 1,
      pageSize: 2,
    });

    expect(result.data).toHaveLength(2);
    expect(result.pagination.total).toBe(3);
  });

  it('should wrap invalid interaction repository payloads as service unavailable', async () => {
    const invalidService = new MessageService(
      new SystemMessageMockRepository(),
      new InvalidInteractionRepository(),
    );

    await expect(
      invalidService.listInteractionMessages({
        userId: USER_ID,
        page: 1,
        pageSize: 20,
      }),
    ).rejects.toMatchObject({ code: 'SERVICE_UNAVAILABLE' });
  });
});
