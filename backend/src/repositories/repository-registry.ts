import { config } from '@/lib/config';
import { CommentRepositoryInterface } from '@/repositories/interfaces/comment.repository.interface';
import { DramaRepositoryInterface } from '@/repositories/interfaces/drama.repository.interface';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';
import { PlaybackHistoryRepositoryInterface } from '@/repositories/interfaces/playback-history.repository.interface';
import { CommentMockRepository } from '@/repositories/mock/comment.mock.repository';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { EpisodeMockRepository } from '@/repositories/mock/episode.mock.repository';
import { PlaybackHistoryMockRepository } from '@/repositories/mock/playback-history.mock.repository';
import { CommentSupabaseRepository } from '@/repositories/supabase/comment.supabase.repository';
import { PlaybackHistorySupabaseRepository } from '@/repositories/supabase/playback-history.supabase.repository';

export function createDefaultDramaRepository(): DramaRepositoryInterface {
  return new DramaMockRepository();
}

export function createDefaultEpisodeRepository(): EpisodeRepositoryInterface {
  return new EpisodeMockRepository();
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

let dramaRepository: DramaRepositoryInterface = createDefaultDramaRepository();
let episodeRepository: EpisodeRepositoryInterface = createDefaultEpisodeRepository();
let playbackHistoryRepository: PlaybackHistoryRepositoryInterface = createDefaultPlaybackHistoryRepository();
let commentRepository: CommentRepositoryInterface = createDefaultCommentRepository();

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

export function resetRepositoryRegistry(): void {
  dramaRepository = createDefaultDramaRepository();
  episodeRepository = createDefaultEpisodeRepository();
  playbackHistoryRepository = createDefaultPlaybackHistoryRepository();
  commentRepository = createDefaultCommentRepository();
}
