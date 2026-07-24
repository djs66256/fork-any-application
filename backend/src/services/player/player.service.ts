import { Errors } from '@/lib/errors';

export class PlayerService {
  async startPlayback(_dramaId: string, _episodeId: string, _progress: number): Promise<void> {
    throw Errors.notImplemented('startPlayback not implemented');
  }

  async stopPlayback(_dramaId: string, _episodeId: string, _progress: number, _duration: number): Promise<void> {
    throw Errors.notImplemented('stopPlayback not implemented');
  }
}
