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

export const RankingTypeSchema = z.enum(['hot', 'recommend', 'booking']);
export type RankingType = z.infer<typeof RankingTypeSchema>;

export const RankingContentTypeSchema = z.enum(['all', 'live_action', 'ai']);
export type RankingContentType = z.infer<typeof RankingContentTypeSchema>;

export const RankingDramaSchema = DramaSchema.extend({
  content_type: z.enum(['live_action', 'ai']),
  play_count: z.number().int().min(0),
  booking_count: z.number().int().min(0),
  recommendation_score: z.number().min(0),
  is_booked: z.boolean().default(false),
});

export type RankingDrama = z.infer<typeof RankingDramaSchema>;

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

export const PaginationSchema = z.object({
  page: z.number().int().min(1),
  page_size: z.number().int().min(1),
  total: z.number().int().min(0),
  total_pages: z.number().int().min(0),
});

export const DramaListResponseSchema = z.object({
  data: z.array(DramaSchema),
  pagination: PaginationSchema,
});

export type DramaListResponse = z.infer<typeof DramaListResponseSchema>;

export const RankingListResponseSchema = z.object({
  data: z.array(RankingDramaSchema),
  pagination: PaginationSchema,
});

export type RankingListResponse = z.infer<typeof RankingListResponseSchema>;

export const SearchDramaQuerySchema = z.object({
  q: z.string().trim().min(1).max(50),
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(10),
});

export type SearchDramaQuery = z.infer<typeof SearchDramaQuerySchema>;

export const RankingQuerySchema = z.object({
  type: RankingTypeSchema.default('hot'),
  contentType: RankingContentTypeSchema.default('all'),
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(10),
});

export type RankingQuery = z.infer<typeof RankingQuerySchema>;

export const BookDramaResponseSchema = z.object({
  drama_id: z.string().uuid(),
  booked: z.literal(true),
  booking_count: z.number().int().min(0),
});

export type BookDramaResponse = z.infer<typeof BookDramaResponseSchema>;

export const HotSearchItemSchema = z.object({
  rank: z.number().int().min(1),
  keyword: z.string().trim().min(1).max(50),
  score: z.number().int().min(0),
});

export type HotSearchItem = z.infer<typeof HotSearchItemSchema>;

export const HotSearchListResponseSchema = z.object({
  data: z.array(HotSearchItemSchema).max(10),
});

export type HotSearchListResponse = z.infer<typeof HotSearchListResponseSchema>;

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
