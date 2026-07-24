import { describe, it, expect, beforeEach } from 'vitest';
import { EpisodeService } from './episode.service';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';

describe('EpisodeService', () => {
  let service: EpisodeService;

  beforeEach(() => {
    const repo = new EpisodeMockRepository();
    service = new EpisodeService(repo);
  });

  it('should throw notImplemented for getEpisodeById', async () => {
    await expect(service.getEpisodeById('some-id')).rejects.toThrow(/not implemented/i);
  });
});
