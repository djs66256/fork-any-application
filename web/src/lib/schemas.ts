import { z } from 'zod';

export const DramaSchema = z.object({
  id: z.string(),
  title: z.string().min(1),
  description: z.string(),
  coverUrl: z.string().url(),
  category: z.string(),
  episodeCount: z.number().int().positive(),
});

export type Drama = z.infer<typeof DramaSchema>;

export const EpisodeSchema = z.object({
  id: z.string(),
  dramaId: z.string(),
  title: z.string().min(1),
  episodeNumber: z.number().int().positive(),
  duration: z.number().int().positive(),
  videoUrl: z.string().url(),
});

export type Episode = z.infer<typeof EpisodeSchema>;

export const HealthResponseSchema = z.object({
  status: z.enum(['ok', 'degraded', 'error']),
  version: z.string(),
  services: z.object({
    database: z.enum(['connected', 'disconnected', 'degraded']),
    redis: z.enum(['connected', 'disconnected', 'degraded']),
  }),
});

export type HealthResponse = z.infer<typeof HealthResponseSchema>;
