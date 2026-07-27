import { Episode, EpisodeSchema } from '@/lib/schemas';
import { EpisodeRepositoryInterface } from '@/repositories/interfaces/episode.repository.interface';

const DEFAULT_EPISODES: Episode[] = [
  {
    id: '660e8400-e29b-41d4-a716-446655440001',
    drama_id: '550e8400-e29b-41d4-a716-446655440001',
    title: '第 1 集',
    episode_number: 1,
    duration: 180,
    video_url: 'https://example.com/dramas/001/episode-1.mp4',
    thumbnail_url: 'https://example.com/dramas/001/episode-1.jpg',
    description: '第一集简介',
    created_at: '2026-07-26T00:00:00Z',
    updated_at: '2026-07-26T00:00:00Z',
  },
  {
    id: '660e8400-e29b-41d4-a716-446655440002',
    drama_id: '550e8400-e29b-41d4-a716-446655440001',
    title: '第 2 集',
    episode_number: 2,
    duration: 190,
    video_url: 'https://example.com/dramas/001/episode-2.mp4',
    thumbnail_url: 'https://example.com/dramas/001/episode-2.jpg',
    description: '第二集简介',
    created_at: '2026-07-26T00:01:00Z',
    updated_at: '2026-07-26T00:01:00Z',
  },
  {
    id: '660e8400-e29b-41d4-a716-446655440003',
    drama_id: '550e8400-e29b-41d4-a716-446655440001',
    title: '第 3 集',
    episode_number: 3,
    duration: 200,
    video_url: null,
    thumbnail_url: 'https://example.com/dramas/001/episode-3.jpg',
    description: '第三集简介',
    created_at: '2026-07-26T00:02:00Z',
    updated_at: '2026-07-26T00:02:00Z',
  },
  {
    id: '660e8400-e29b-41d4-a716-446655440011',
    drama_id: '550e8400-e29b-41d4-a716-446655440002',
    title: '第 1 集',
    episode_number: 1,
    duration: 210,
    video_url: 'https://example.com/dramas/002/episode-1.mp4',
    thumbnail_url: 'https://example.com/dramas/002/episode-1.jpg',
    description: '第一集简介',
    created_at: '2026-07-26T00:03:00Z',
    updated_at: '2026-07-26T00:03:00Z',
  },
  {
    id: '660e8400-e29b-41d4-a716-446655440012',
    drama_id: '550e8400-e29b-41d4-a716-446655440002',
    title: '第 2 集',
    episode_number: 2,
    duration: 220,
    video_url: 'https://example.com/dramas/002/episode-2.mp4',
    thumbnail_url: 'https://example.com/dramas/002/episode-2.jpg',
    description: '第二集简介',
    created_at: '2026-07-26T00:04:00Z',
    updated_at: '2026-07-26T00:04:00Z',
  },
].map((episode) => EpisodeSchema.parse(episode));

function cloneEpisode(episode: Episode): Episode {
  return {
    ...episode,
    duration: episode.duration ?? null,
    video_url: episode.video_url ?? null,
    thumbnail_url: episode.thumbnail_url ?? null,
    description: episode.description ?? null,
  };
}

export class EpisodeMockRepository implements EpisodeRepositoryInterface {
  private data: Map<string, Episode>;

  constructor(initialData: Episode[] = DEFAULT_EPISODES) {
    this.data = new Map(initialData.map((episode) => [episode.id, cloneEpisode(episode)]));
  }

  async findByDramaId(dramaId: string): Promise<Episode[]> {
    return Array.from(this.data.values())
      .filter((ep) => ep.drama_id === dramaId)
      .map(cloneEpisode);
  }

  async findById(id: string): Promise<Episode | null> {
    const episode = this.data.get(id);
    return episode ? cloneEpisode(episode) : null;
  }

async create(data: Omit<Episode, 'id' | 'created_at' | 'updated_at'>): Promise<Episode> {
    const now = new Date().toISOString();
    const id = crypto.randomUUID();
    const episode: Episode = {
      ...data,
      id,
      created_at: now,
      updated_at: now,
    } as Episode;
    this.data.set(id, episode);
    return episode;
  }

  async update(
    id: string,
    data: Partial<Omit<Episode, 'id' | 'created_at' | 'updated_at'>>,
  ): Promise<Episode | null> {
    const existing = this.data.get(id);
    if (!existing) return null;

    const updated: Episode = {
      ...existing,
      ...data,
      updated_at: new Date().toISOString(),
    } as Episode;
    this.data.set(id, updated);
    return updated;
  }

  async delete(id: string): Promise<boolean> {
    return this.data.delete(id);
  }

  async count(): Promise<number> {
    return this.data.size;
  }

  async countByDramaId(dramaId: string): Promise<number> {
    return Array.from(this.data.values()).filter((ep) => ep.drama_id === dramaId).length;
  }

  // Helper for tests: add seed data
  addSeed(episode: Episode): void {
    this.data.set(episode.id, cloneEpisode(EpisodeSchema.parse(episode)));
  }

  remove(id: string): void {
    this.data.delete(id);
  }

  clear(): void {
    this.data.clear();
  }
}