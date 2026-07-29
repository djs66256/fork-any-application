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

export const PaginationSchema = z.object({
  page: z.number().int().min(1),
  page_size: z.number().int().min(1),
  total: z.number().int().min(0),
  total_pages: z.number().int().min(0),
});

export type Pagination = z.infer<typeof PaginationSchema>;

export const MallProductIdSchema = z.string().uuid();

export const MallProductSchema = z.object({
  id: MallProductIdSchema,
  title: z.string().trim().min(1).max(200),
  image_url: z.string().url(),
  price: z.number().nonnegative(),
  tags: z.array(z.string().trim().min(1).max(20)).max(3).default([]),
});

export type MallProduct = z.infer<typeof MallProductSchema>;

export const MallProductsQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(20),
});

export type MallProductsQuery = z.infer<typeof MallProductsQuerySchema>;

export const MallProductsResponseSchema = z.object({
  data: z.array(MallProductSchema),
  pagination: PaginationSchema,
});

export type MallProductsResponse = z.infer<typeof MallProductsResponseSchema>;

export const MallBannerSchema = z.object({
  id: z.string().trim().min(1),
  image_url: z.string().url(),
  target_type: z.enum(['none', 'product', 'search', 'web']),
  target_value: z.string().default(''),
  sort_order: z.number().int().min(0),
});

export type MallBanner = z.infer<typeof MallBannerSchema>;

export const MallShortcutSchema = z.object({
  key: z.enum(['orders', 'coupon', 'wallet', 'same-style', 'subsidy']),
  title: z.string().trim().min(1),
  icon: z.string().trim().min(1),
  behavior: z.enum(['placeholder-feedback']),
});

export type MallShortcut = z.infer<typeof MallShortcutSchema>;

export const MallSearchContextSchema = z.object({
  source: z.literal('mall'),
  returnTarget: z.literal('/mall'),
});

export type MallSearchContext = z.infer<typeof MallSearchContextSchema>;

export const MallLoginContextSchema = z.object({
  source: z.literal('mall'),
  productId: MallProductIdSchema,
  returnTarget: z.literal('/mall'),
});

export type MallLoginContext = z.infer<typeof MallLoginContextSchema>;

export const MallBridgeMessageSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('mall.openSearch'),
    payload: MallSearchContextSchema,
  }),
  z.object({
    type: z.literal('mall.requestLogin'),
    payload: MallLoginContextSchema,
  }),
]);

export type MallBridgeMessage = z.infer<typeof MallBridgeMessageSchema>;

export const MallHostAuthStateSchema = z.object({
  source: z.literal('mall'),
  isLoggedIn: z.boolean(),
  reason: z.enum(['initial-load', 'login-success', 'login-cancel', 'app-resume']),
  returnTarget: z.literal('/mall'),
});

export type MallHostAuthState = z.infer<typeof MallHostAuthStateSchema>;

export const MallRestoreContextSchema = z.object({
  source: z.literal('mall'),
  reason: z.enum(['search-return', 'login-return', 'container-recreated']),
  returnTarget: z.literal('/mall'),
  preserveScroll: z.boolean().default(false),
});

export type MallRestoreContext = z.infer<typeof MallRestoreContextSchema>;

export const MallHostMessageSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('mall.syncAuthState'),
    payload: MallHostAuthStateSchema,
  }),
  z.object({
    type: z.literal('mall.restoreContext'),
    payload: MallRestoreContextSchema,
  }),
]);

export type MallHostMessage = z.infer<typeof MallHostMessageSchema>;

export const EarnTaskIdSchema = z.string().uuid();

export const EarnTaskStatusSchema = z.enum([
  'available',
  'in_progress',
  'completed',
  'claimed',
  'locked',
]);

export type EarnTaskStatus = z.infer<typeof EarnTaskStatusSchema>;

export const EarnTaskActionSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('play'),
    video_id: z.string().trim().min(1),
  }),
  z.object({
    type: z.literal('placeholder'),
    feedback: z.string().trim().min(1),
  }),
  z.object({
    type: z.literal('login'),
  }),
]);

export type EarnTaskAction = z.infer<typeof EarnTaskActionSchema>;

export const EarnTaskSchema = z.object({
  id: EarnTaskIdSchema,
  title: z.string().trim().min(1).max(100),
  description: z.string().trim().min(1).max(200),
  reward_coins: z.number().int().nonnegative(),
  status: EarnTaskStatusSchema,
  action: EarnTaskActionSchema,
  is_representative: z.boolean().optional(),
});

export type EarnTask = z.infer<typeof EarnTaskSchema>;

export const EarnDailyRewardStatusSchema = z.enum(['claimable', 'claimed', 'locked']);

export type EarnDailyRewardStatus = z.infer<typeof EarnDailyRewardStatusSchema>;

export const EarnDailyRewardSchema = z.object({
  day: z.number().int().min(1).max(7),
  coins: z.number().int().nonnegative(),
  status: EarnDailyRewardStatusSchema,
});

export type EarnDailyReward = z.infer<typeof EarnDailyRewardSchema>;

export const EarnOverviewResponseSchema = z.object({
  coins: z.number().int().nonnegative(),
  is_logged_in: z.boolean(),
  new_user_task: EarnTaskSchema,
  daily_rewards: z.array(EarnDailyRewardSchema).length(7),
  cash_tasks: z.array(EarnTaskSchema),
});

export type EarnOverviewResponse = z.infer<typeof EarnOverviewResponseSchema>;

export const CompleteEarnTaskRequestSchema = z.object({
  task_id: EarnTaskIdSchema,
});

export type CompleteEarnTaskRequest = z.infer<typeof CompleteEarnTaskRequestSchema>;

export const CompleteEarnTaskResponseSchema = z.object({
  success: z.literal(true),
  task_id: EarnTaskIdSchema,
  coins_earned: z.number().int().nonnegative(),
  total_coins: z.number().int().nonnegative(),
  task_status: z.literal('completed'),
});

export type CompleteEarnTaskResponse = z.infer<typeof CompleteEarnTaskResponseSchema>;

export const EarnTaskContextSchema = z.object({
  taskId: EarnTaskIdSchema,
  source: z.literal('earn'),
  returnTarget: z.literal('/earn'),
  videoId: z.string().trim().min(1),
});

export type EarnTaskContext = z.infer<typeof EarnTaskContextSchema>;

export const EarnLoginContextSchema = z.object({
  source: z.literal('earn'),
  returnTarget: z.literal('/earn'),
});

export type EarnLoginContext = z.infer<typeof EarnLoginContextSchema>;

export const EarnBridgeMessageSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('earn.requestLogin'),
    payload: EarnLoginContextSchema,
  }),
  z.object({
    type: z.literal('earn.openTaskPlayer'),
    payload: EarnTaskContextSchema,
  }),
]);

export type EarnBridgeMessage = z.infer<typeof EarnBridgeMessageSchema>;

export const EarnHostTransportSchema = z.object({
  type: z.literal('custom-event'),
  eventName: z.literal('earn.hostMessage'),
});

export type EarnHostTransport = z.infer<typeof EarnHostTransportSchema>;

export const EarnHostAuthStateSchema = z.object({
  source: z.literal('earn'),
  isLoggedIn: z.boolean(),
  reason: z.enum(['initial-load', 'login-success', 'login-cancel', 'app-resume']),
  returnTarget: z.literal('/earn'),
  apiAccessToken: z.string().trim().min(1).nullable().optional(),
  expiresAt: z.string().trim().min(1).nullable().optional(),
});

export type EarnHostAuthState = z.infer<typeof EarnHostAuthStateSchema>;

export const EarnRestoreContextSchema = z.object({
  source: z.literal('earn'),
  reason: z.enum(['login-return', 'task-return', 'container-recreated']),
  returnTarget: z.literal('/earn'),
  preserveScroll: z.boolean().default(false),
});

export type EarnRestoreContext = z.infer<typeof EarnRestoreContextSchema>;

export const EarnTaskPlayerResultSchema = z.object({
  source: z.literal('earn'),
  taskId: EarnTaskIdSchema,
  videoId: z.string().trim().min(1),
  completed: z.boolean(),
  reason: z.enum([
    'playback-ended',
    'user-exit',
    'backgrounded',
    'error',
    'container-recreated',
  ]),
});

export type EarnTaskPlayerResult = z.infer<typeof EarnTaskPlayerResultSchema>;

export const EarnHostMessageSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('earn.syncAuthState'),
    payload: EarnHostAuthStateSchema,
  }),
  z.object({
    type: z.literal('earn.restoreContext'),
    payload: EarnRestoreContextSchema,
  }),
  z.object({
    type: z.literal('earn.completeTask'),
    payload: EarnTaskPlayerResultSchema,
  }),
]);

export type EarnHostMessage = z.infer<typeof EarnHostMessageSchema>;
