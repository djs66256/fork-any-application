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
