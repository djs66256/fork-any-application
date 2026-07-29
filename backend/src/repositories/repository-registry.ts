import { config } from '@/lib/config';
import { DramaRepositoryInterface } from '@/repositories/interfaces/drama.repository.interface';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';
import { MallRepositoryInterface } from '@/repositories/interfaces/mall.repository.interface';
import { PlaybackHistoryRepositoryInterface } from '@/repositories/interfaces/playback-history.repository.interface';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';
import { MallMockRepository } from '@/repositories/mock/mall.mock.repository';
import { PlaybackHistoryMockRepository } from '@/repositories/mock/playback-history.mock.repository';
import { PlaybackHistorySupabaseRepository } from '@/repositories/supabase/playback-history.supabase.repository';

export function createDefaultDramaRepository(): DramaRepositoryInterface {
  return new DramaMockRepository();
}

export function createDefaultEpisodeRepository(): EpisodeRepositoryInterface {
  return new EpisodeMockRepository();
}

export function createDefaultMallRepository(): MallRepositoryInterface {
  return new MallMockRepository();
}

export function createDefaultPlaybackHistoryRepository(): PlaybackHistoryRepositoryInterface {
  if (config.player.historyRepository === 'supabase') {
    return new PlaybackHistorySupabaseRepository();
  }

  return new PlaybackHistoryMockRepository();
}

let dramaRepository: DramaRepositoryInterface = createDefaultDramaRepository();
let episodeRepository: EpisodeRepositoryInterface = createDefaultEpisodeRepository();
let mallRepository: MallRepositoryInterface = createDefaultMallRepository();
let playbackHistoryRepository: PlaybackHistoryRepositoryInterface = createDefaultPlaybackHistoryRepository();

export function getDramaRepository(): DramaRepositoryInterface {
  return dramaRepository;
}

export function setDramaRepository(repository: DramaRepositoryInterface): void {
  dramaRepository = repository;
}

export function getEpisodeRepository(): EpisodeRepositoryInterface {
  return episodeRepository;
}

export function setEpisodeRepository(repository: EpisodeRepositoryInterface): void {
  episodeRepository = repository;
}

export function getMallRepository(): MallRepositoryInterface {
  return mallRepository;
}

export function setMallRepository(repository: MallRepositoryInterface): void {
  mallRepository = repository;
}

export function getPlaybackHistoryRepository(): PlaybackHistoryRepositoryInterface {
  return playbackHistoryRepository;
}

export function setPlaybackHistoryRepository(repository: PlaybackHistoryRepositoryInterface): void {
  playbackHistoryRepository = repository;
}

export function resetRepositoryRegistry(): void {
  dramaRepository = createDefaultDramaRepository();
  episodeRepository = createDefaultEpisodeRepository();
  mallRepository = createDefaultMallRepository();
  playbackHistoryRepository = createDefaultPlaybackHistoryRepository();
}
