import { config } from '@/lib/config';
import { CheckInRepositoryInterface } from '@/repositories/interfaces/check-in.repository.interface';
import { CommentRepositoryInterface } from '@/repositories/interfaces/comment.repository.interface';
import { DramaRepositoryInterface } from '@/repositories/interfaces/drama.repository.interface';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';
import { EarnRepositoryInterface } from '@/repositories/interfaces/earn.repository.interface';
import { InteractionMessageRepositoryInterface } from '@/repositories/interfaces/interaction-message.repository.interface';
import { MallRepositoryInterface } from '@/repositories/interfaces/mall.repository.interface';
import { PlaybackHistoryRepositoryInterface } from '@/repositories/interfaces/playback-history.repository.interface';
import { SystemMessageRepositoryInterface } from '@/repositories/interfaces/system-message.repository.interface';
import { CheckInMockRepository } from '@/repositories/mock/check-in.mock.repository';
import { CommentMockRepository } from '@/repositories/mock/comment.mock.repository';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { EarnMockRepository } from '@/repositories/mock/earn.mock.repository';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';
import { InteractionMessageMockRepository } from '@/repositories/mock/interaction-message.mock.repository';
import { MallMockRepository } from '@/repositories/mock/mall.mock.repository';
import { PlaybackHistoryMockRepository } from '@/repositories/mock/playback-history.mock.repository';
import { SystemMessageMockRepository } from '@/repositories/mock/system-message.mock.repository';
import { CheckInSupabaseRepository } from '@/repositories/supabase/check-in.supabase.repository';
import { CommentSupabaseRepository } from '@/repositories/supabase/comment.supabase.repository';
import { PlaybackHistorySupabaseRepository } from '@/repositories/supabase/playback-history.supabase.repository';
import { SystemMessageSupabaseRepository } from '@/repositories/supabase/system-message.supabase.repository';

export function createDefaultDramaRepository(): DramaRepositoryInterface {
  return new DramaMockRepository();
}

export function createDefaultEpisodeRepository(): EpisodeRepositoryInterface {
  return new EpisodeMockRepository();
}

export function createDefaultMallRepository(): MallRepositoryInterface {
  return new MallMockRepository();
}

export function createDefaultEarnRepository(): EarnRepositoryInterface {
  return new EarnMockRepository();
}

export function createDefaultPlaybackHistoryRepository(): PlaybackHistoryRepositoryInterface {
  if (config.player.historyRepository === 'supabase') {
    return new PlaybackHistorySupabaseRepository();
  }

  return new PlaybackHistoryMockRepository();
}

export function createDefaultCommentRepository(): CommentRepositoryInterface {
  if (config.comments.repository === 'supabase') {
    return new CommentSupabaseRepository();
  }

  return new CommentMockRepository();
}

export function createDefaultCheckInRepository(): CheckInRepositoryInterface {
  if (config.checkIns.repository === 'supabase') {
    return new CheckInSupabaseRepository();
  }

  return new CheckInMockRepository();
}

export function createDefaultSystemMessageRepository(): SystemMessageRepositoryInterface {
  if (config.systemMessages.repository === 'supabase') {
    return new SystemMessageSupabaseRepository();
  }

  return new SystemMessageMockRepository();
}

export function createDefaultInteractionMessageRepository(): InteractionMessageRepositoryInterface {
  return new InteractionMessageMockRepository();
}

let dramaRepository: DramaRepositoryInterface = createDefaultDramaRepository();
let episodeRepository: EpisodeRepositoryInterface = createDefaultEpisodeRepository();
let mallRepository: MallRepositoryInterface = createDefaultMallRepository();
let earnRepository: EarnRepositoryInterface = createDefaultEarnRepository();
let playbackHistoryRepository: PlaybackHistoryRepositoryInterface = createDefaultPlaybackHistoryRepository();
let commentRepository: CommentRepositoryInterface = createDefaultCommentRepository();
let checkInRepository: CheckInRepositoryInterface = createDefaultCheckInRepository();
let systemMessageRepository: SystemMessageRepositoryInterface = createDefaultSystemMessageRepository();
let interactionMessageRepository: InteractionMessageRepositoryInterface = createDefaultInteractionMessageRepository();

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

export function getEarnRepository(): EarnRepositoryInterface {
  return earnRepository;
}

export function setEarnRepository(repository: EarnRepositoryInterface): void {
  earnRepository = repository;
}

export function getPlaybackHistoryRepository(): PlaybackHistoryRepositoryInterface {
  return playbackHistoryRepository;
}

export function setPlaybackHistoryRepository(repository: PlaybackHistoryRepositoryInterface): void {
  playbackHistoryRepository = repository;
}

export function getCommentRepository(): CommentRepositoryInterface {
  return commentRepository;
}

export function setCommentRepository(repository: CommentRepositoryInterface): void {
  commentRepository = repository;
}

export function getCheckInRepository(): CheckInRepositoryInterface {
  return checkInRepository;
}

export function setCheckInRepository(repository: CheckInRepositoryInterface): void {
  checkInRepository = repository;
}

export function getSystemMessageRepository(): SystemMessageRepositoryInterface {
  return systemMessageRepository;
}

export function setSystemMessageRepository(repository: SystemMessageRepositoryInterface): void {
  systemMessageRepository = repository;
}

export function getInteractionMessageRepository(): InteractionMessageRepositoryInterface {
  return interactionMessageRepository;
}

export function setInteractionMessageRepository(repository: InteractionMessageRepositoryInterface): void {
  interactionMessageRepository = repository;
}

export function resetRepositoryRegistry(): void {
  dramaRepository = createDefaultDramaRepository();
  episodeRepository = createDefaultEpisodeRepository();
  mallRepository = createDefaultMallRepository();
  earnRepository = createDefaultEarnRepository();
  playbackHistoryRepository = createDefaultPlaybackHistoryRepository();
  commentRepository = createDefaultCommentRepository();
  checkInRepository = createDefaultCheckInRepository();
  systemMessageRepository = createDefaultSystemMessageRepository();
  interactionMessageRepository = createDefaultInteractionMessageRepository();
}
