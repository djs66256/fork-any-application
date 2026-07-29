import { beforeEach, describe, expect, it, vi } from 'vitest';
import { resetRepositoryRegistry, setSystemMessageRepository } from '@/repositories/repository-registry';
import { SystemMessageMockRepository } from '@/repositories/mock/system-message.mock.repository';

const { GET } = await import('../messages/preview/route');

describe('GET /api/messages/preview', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-29T10:00:00.000Z'));
    resetRepositoryRegistry();
  });

  it('should return latest preview when system messages exist', async () => {
    const response = await GET(undefined as never, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      title: '系统通知',
      summary: '你收藏的专题上新了 3 部短剧。',
      relative_time: '1分钟前',
    });
  });

  it('should return 204 when system messages are empty', async () => {
    setSystemMessageRepository(new SystemMessageMockRepository([]));

    const response = await GET(undefined as never, undefined);

    expect(response.status).toBe(204);
    expect(await response.text()).toBe('');
  });
});
