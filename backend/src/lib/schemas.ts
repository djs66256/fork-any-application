import { z } from 'zod';

export const HealthResponseSchema = z.object({
  status: z.enum(['ok', 'degraded', 'error']),
  timestamp: z.string(),
  version: z.string(),
  services: z.object({
    database: z.enum(['connected', 'disconnected', 'unknown']),
    redis: z.enum(['connected', 'disconnected', 'unknown']),
  }),
});

export type HealthResponse = z.infer<typeof HealthResponseSchema>;

export const DramaSchema = z.object({
  id: z.string().uuid(),
  title: z.string().min(1),
  description: z.string().optional().nullable(),
  cover_url: z.string().url().optional().nullable(),
  category: z.string().optional().nullable(),
  total_episodes: z.number().int().min(0),
  release_year: z.number().int().optional().nullable(),
  rating: z.number().min(0).max(10).optional().nullable(),
  status: z.enum(['ongoing', 'completed', 'announced']).default('ongoing'),
  created_at: z.string(),
  updated_at: z.string(),
  play_count: z.number().int().min(0).default(0),
});

export type Drama = z.infer<typeof DramaSchema>;

export const EpisodeSchema = z.object({
  id: z.string().uuid(),
  drama_id: z.string().uuid(),
  title: z.string().min(1),
  episode_number: z.number().int().min(1),
  duration: z.number().int().min(0).optional().nullable(),
  video_url: z.string().url().optional().nullable(),
  thumbnail_url: z.string().url().optional().nullable(),
  description: z.string().optional().nullable(),
  created_at: z.string(),
  updated_at: z.string(),
});

export type Episode = z.infer<typeof EpisodeSchema>;

export const DramaListResponseSchema = z.object({
  data: z.array(DramaSchema),
  pagination: z.object({
    page: z.number().int().min(1),
    page_size: z.number().int().min(1),
    total: z.number().int().min(0),
    total_pages: z.number().int().min(0),
  }),
});

export type DramaListResponse = z.infer<typeof DramaListResponseSchema>;

export const PlayerStartRequestSchema = z.object({
  drama_id: z.string().uuid(),
  episode_id: z.string().uuid(),
  progress: z.number().min(0).default(0),
});

export type PlayerStartRequest = z.infer<typeof PlayerStartRequestSchema>;

export const PlayerStopRequestSchema = z.object({
  drama_id: z.string().uuid(),
  episode_id: z.string().uuid(),
  progress: z.number().min(0),
  duration: z.number().min(1),
});

export type PlayerStopRequest = z.infer<typeof PlayerStopRequestSchema>;

export const UserProfileSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email().optional().nullable(),
  display_name: z.string().min(1).optional().nullable(),
  avatar_url: z.string().url().optional().nullable(),
  created_at: z.string(),
  updated_at: z.string(),
});

export type UserProfile = z.infer<typeof UserProfileSchema>;
