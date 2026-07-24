import { describe, it, expect, beforeEach } from 'vitest';
import { PlayerService } from './player.service';

describe('PlayerService', () => {
  let service: PlayerService;

  beforeEach(() => {
    service = new PlayerService();
  });

  it('should throw notImplemented for startPlayback', async () => {
    await expect(
      service.startPlayback('drama-id', 'episode-id', 0),
    ).rejects.toThrow(/not implemented/i);
  });

  it('should throw notImplemented for stopPlayback', async () => {
    await expect(
      service.stopPlayback('drama-id', 'episode-id', 120, 3600),
    ).rejects.toThrow(/not implemented/i);
  });
});
