import { z } from 'zod';

const SeriesStatusSchema = z.enum(['completed', 'ongoing']);

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
  description: z.string().default(''),
  cover_url: z.string().url().nullable().default(null),
  category: z.string().default(''),
  episode_count: z.number().int().min(0),
  tags: z.array(z.string()).default([]),
  rating: z.number().min(0).max(10).nullable().default(null),
  created_at: z.string(),
  updated_at: z.string(),
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

export const DramaIdPathSchema = z.object({
  id: z.string().uuid(),
});

export type DramaIdPath = z.infer<typeof DramaIdPathSchema>;

export const PlaybackSessionIdHeaderSchema = z.string().uuid();

export type PlaybackSessionIdHeader = z.infer<typeof PlaybackSessionIdHeaderSchema>;

export const PlayerProgressQuerySchema = z.object({
  dramaId: z.string().uuid(),
});

export type PlayerProgressQuery = z.infer<typeof PlayerProgressQuerySchema>;

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

export const PlaybackHistorySchema = z.object({
  playback_session_id: z.string().uuid(),
  drama_id: z.string().uuid(),
  episode_id: z.string().uuid(),
  progress: z.number().min(0),
  duration: z.number().min(1).nullable().optional(),
  updated_at: z.string(),
});

export type PlaybackHistory = z.infer<typeof PlaybackHistorySchema>;

export const EpisodeListResponseSchema = z.object({
  code: z.literal(0),
  data: z.object({
    drama_id: z.string().uuid(),
    series_status: SeriesStatusSchema.default('completed'),
    items: z.array(EpisodeSchema),
  }),
  message: z.string(),
});

export type EpisodeListResponse = z.infer<typeof EpisodeListResponseSchema>;

export const PlayerProgressResponseSchema = z.object({
  code: z.literal(0),
  data: z.object({
    drama_id: z.string().uuid(),
    has_history: z.boolean(),
    episode_id: z.string().uuid().nullable(),
    start_time: z.number().min(0),
    updated_at: z.string().nullable(),
  }),
  message: z.string(),
});

export type PlayerProgressResponse = z.infer<typeof PlayerProgressResponseSchema>;

export const PlayerStartResponseSchema = z.object({
  code: z.literal(0),
  data: z.object({
    drama_id: z.string().uuid(),
    episode_id: z.string().uuid(),
    accepted_progress: z.number().min(0),
    playback_session_id: z.string().uuid(),
    started_at: z.string(),
  }),
  message: z.string(),
});

export type PlayerStartResponse = z.infer<typeof PlayerStartResponseSchema>;

export const PlayerStopResponseSchema = z.object({
  code: z.literal(0),
  data: z.object({
    drama_id: z.string().uuid(),
    episode_id: z.string().uuid(),
    saved_progress: z.number().min(0),
    duration: z.number().min(1),
    updated_at: z.string(),
  }),
  message: z.string(),
});

export type PlayerStopResponse = z.infer<typeof PlayerStopResponseSchema>;

export const UserProfileSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email().optional().nullable(),
  display_name: z.string().min(1).optional().nullable(),
  avatar_url: z.string().url().optional().nullable(),
  created_at: z.string(),
  updated_at: z.string(),
});

export type UserProfile = z.infer<typeof UserProfileSchema>;
