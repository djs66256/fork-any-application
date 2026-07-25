import { Errors } from '@/lib/errors';

export class PlayerService {
  async startPlayback(dramaId: string, episodeId: string, progress: number): Promise<void> {
    void dramaId;
    void episodeId;
    void progress;
    throw Errors.notImplemented('startPlayback not implemented');
  }

  async stopPlayback(dramaId: string, episodeId: string, progress: number, duration: number): Promise<void> {
    void dramaId;
    void episodeId;
    void progress;
    void duration;
    throw Errors.notImplemented('stopPlayback not implemented');
  }
}
