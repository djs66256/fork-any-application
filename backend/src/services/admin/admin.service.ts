import {
  AdminStatsResponse,
  AdminStatsResponseSchema,
  AdminDramaCreate,
  AdminDramaUpdate,
  AdminEpisodeCreate,
  AdminEpisodeUpdate,
  AdminUserProfile,
  AdminUserListResponseSchema,
  Drama,
  DramaSchema,
  DramaListResponseSchema,
  Episode,
  EpisodeSchema,
} from '@/lib/schemas';
import { DramaSupabaseRepository } from '@/repositories/supabase/drama.supabase.repository';
import { EpisodeSupabaseRepository } from '@/repositories/supabase/episode.supabase.repository';
import { UserSupabaseRepository } from '@/repositories/supabase/user.supabase.repository';
import { Errors } from '@/lib/errors';

function computeTotalPages(total: number, pageSize: number): number {
  return total === 0 ? 0 : Math.ceil(total / pageSize);
}

export class AdminService {
  constructor(
    private dramaRepo = new DramaSupabaseRepository(),
    private episodeRepo = new EpisodeSupabaseRepository(),
    private userRepo = new UserSupabaseRepository(),
  ) {}

  async getStats(): Promise<AdminStatsResponse> {
    const [totalDramas, totalEpisodes, totalUsers] = await Promise.all([
      this.dramaRepo.count(),
      this.episodeRepo.count(),
      this.userRepo.count(),
    ]);

    return AdminStatsResponseSchema.parse({
      total_dramas: totalDramas,
      total_episodes: totalEpisodes,
      total_users: totalUsers,
    });
  }

  async listDramas(page: number, pageSize: number): Promise<{ data: Drama[]; pagination: { page: number; page_size: number; total: number; total_pages: number } }> {
    const result = await this.dramaRepo.findMany({ page, pageSize });
    return DramaListResponseSchema.parse(result);
  }

  async createDrama(data: AdminDramaCreate): Promise<Drama> {
    const drama = await this.dramaRepo.create({
      title: data.title,
      description: data.description,
      cover_url: data.cover_url,
      category: data.category,
      episode_count: data.episode_count,
      tags: data.tags,
      rating: data.rating,
    });
    return DramaSchema.parse(drama);
  }

  async getDrama(id: string): Promise<Drama> {
    const drama = await this.dramaRepo.findById(id);
    if (!drama) {
      throw Errors.notFound('Drama', id);
    }
    return DramaSchema.parse(drama);
  }

  async updateDrama(id: string, data: AdminDramaUpdate): Promise<Drama> {
    const existing = await this.dramaRepo.findById(id);
    if (!existing) {
      throw Errors.notFound('Drama', id);
    }

    const updated = await this.dramaRepo.update(id, data);
    if (!updated) {
      throw Errors.notFound('Drama', id);
    }
    return DramaSchema.parse(updated);
  }

  async deleteDrama(id: string): Promise<void> {
    const existing = await this.dramaRepo.findById(id);
    if (!existing) {
      throw Errors.notFound('Drama', id);
    }

    // Episodes are deleted via DB-level ON DELETE CASCADE (foreign key),
    // but we still call delete on the drama which will cascade at DB level.
    await this.dramaRepo.delete(id);
  }

  async listEpisodes(dramaId: string): Promise<Episode[]> {
    const drama = await this.dramaRepo.findById(dramaId);
    if (!drama) {
      throw Errors.notFound('Drama', dramaId);
    }

    const episodes = await this.episodeRepo.findByDramaId(dramaId);
    return episodes.map((ep) => EpisodeSchema.parse(ep));
  }

  async createEpisode(dramaId: string, data: AdminEpisodeCreate): Promise<Episode> {
    const drama = await this.dramaRepo.findById(dramaId);
    if (!drama) {
      throw Errors.notFound('Drama', dramaId);
    }

    const episode = await this.episodeRepo.create({
      drama_id: dramaId,
      title: data.title,
      episode_number: data.episode_number,
      duration: data.duration ?? null,
      video_url: data.video_url ?? null,
      thumbnail_url: data.thumbnail_url ?? null,
      description: data.description ?? null,
    });

    // Update drama episode_count
    const count = await this.episodeRepo.countByDramaId(dramaId);
    await this.dramaRepo.update(dramaId, { episode_count: count });

    return EpisodeSchema.parse(episode);
  }

  async updateEpisode(id: string, data: AdminEpisodeUpdate): Promise<Episode> {
    const existing = await this.episodeRepo.findById(id);
    if (!existing) {
      throw Errors.notFound('Episode', id);
    }

    const updated = await this.episodeRepo.update(id, data);
    if (!updated) {
      throw Errors.notFound('Episode', id);
    }

    // Update parent drama episode_count
    const count = await this.episodeRepo.countByDramaId(updated.drama_id);
    await this.dramaRepo.update(updated.drama_id, { episode_count: count });

    return EpisodeSchema.parse(updated);
  }

  async deleteEpisode(id: string): Promise<void> {
    const existing = await this.episodeRepo.findById(id);
    if (!existing) {
      throw Errors.notFound('Episode', id);
    }

    const dramaId = existing.drama_id;
    await this.episodeRepo.delete(id);

    // Update parent drama episode_count
    const count = await this.episodeRepo.countByDramaId(dramaId);
    await this.dramaRepo.update(dramaId, { episode_count: count });
  }

  async listUsers(page: number, pageSize: number): Promise<{ data: AdminUserProfile[]; pagination: { page: number; page_size: number; total: number; total_pages: number } }> {
    const { data, total } = await this.userRepo.list(page, pageSize);

    return AdminUserListResponseSchema.parse({
      data,
      pagination: {
        page,
        page_size: pageSize,
        total,
        total_pages: computeTotalPages(total, pageSize),
      },
    });
  }

  async updateUserRole(userId: string, role: string, currentUserId: string): Promise<AdminUserProfile> {
    if (userId === currentUserId) {
      throw Errors.cannotModifySelf();
    }

    const user = await this.userRepo.findById(userId);
    if (!user) {
      throw Errors.notFound('User', userId);
    }

    const updated = await this.userRepo.updateRole(userId, role);
    if (!updated) {
      throw Errors.notFound('User', userId);
    }

    return updated;
  }
}