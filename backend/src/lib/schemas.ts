import { z } from 'zod';
import { RECENTLY_VIEWED_LIMIT } from '@/lib/player';

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

export const TheaterChannelSchema = z.enum([
  'all',
  'real',
  'anime',
  'movie',
  'audio',
  'novel',
  'comic',
  'bigscreen',
]);

export type TheaterChannel = z.infer<typeof TheaterChannelSchema>;

export const TheaterFeedQuerySchema = z.object({
  channel: TheaterChannelSchema.default('all'),
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(20),
});

export type TheaterFeedQuery = z.infer<typeof TheaterFeedQuerySchema>;

export const TheaterDramaSchema = DramaSchema.extend({
  heat: z.number().int().min(0),
});

export type TheaterDrama = z.infer<typeof TheaterDramaSchema>;

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

export const InstallationIdHeaderSchema = z.string().uuid();
export type InstallationIdHeader = z.infer<typeof InstallationIdHeaderSchema>;

export const MessageListQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(20).default(20),
});
export type MessageListQuery = z.infer<typeof MessageListQuerySchema>;

export const SignInDaySchema = z.object({
  day: z.number().int().min(1).max(7),
  title: z.string().min(1),
  reward_label: z.string().min(1),
  status: z.enum(['signed', 'today', 'locked']),
});
export type SignInDay = z.infer<typeof SignInDaySchema>;

export const SignInStatusSchema = z.object({
  server_date: z.string().min(1),
  should_show_popup: z.boolean(),
  today_signed: z.boolean(),
  current_streak: z.number().int().min(0).max(7),
  reward_copy: z.string().min(1),
  days: z.array(SignInDaySchema).length(7),
});
export type SignInStatus = z.infer<typeof SignInStatusSchema>;

export const MessagePreviewSchema = z.object({
  title: z.string().min(1),
  summary: z.string().min(1),
  relative_time: z.string().min(1),
});
export type MessagePreview = z.infer<typeof MessagePreviewSchema>;

export const SystemMessageSchema = z.object({
  id: z.string().uuid(),
  title: z.string().min(1),
  summary: z.string().min(1),
  sent_at: z.string(),
});
export type SystemMessage = z.infer<typeof SystemMessageSchema>;

export const InteractionMessageSchema = z.object({
  id: z.string().uuid(),
  type: z.enum(['comment_reply', 'comment_like', 'system_hint']).default('system_hint'),
  title: z.string().min(1),
  summary: z.string().min(1),
  sent_at: z.string(),
});
export type InteractionMessage = z.infer<typeof InteractionMessageSchema>;

export const SystemMessageListResponseSchema = z.object({
  data: z.array(SystemMessageSchema),
  pagination: PaginationSchema,
});
export type SystemMessageListResponse = z.infer<typeof SystemMessageListResponseSchema>;

export const InteractionMessageListResponseSchema = z.object({
  data: z.array(InteractionMessageSchema),
  pagination: PaginationSchema,
});
export type InteractionMessageListResponse = z.infer<typeof InteractionMessageListResponseSchema>;

export const MallProductSchema = z.object({
  id: z.string().uuid(),
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

export const EarnTaskStatusSchema = z.enum(['available', 'in_progress', 'completed', 'claimed', 'locked']);
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
  id: z.string().uuid(),
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
  task_id: z.string().uuid(),
});
export type CompleteEarnTaskRequest = z.infer<typeof CompleteEarnTaskRequestSchema>;

export const CompleteEarnTaskResponseSchema = z.object({
  success: z.literal(true),
  task_id: z.string().uuid(),
  coins_earned: z.number().int().nonnegative(),
  total_coins: z.number().int().nonnegative(),
  task_status: z.literal('completed'),
});
export type CompleteEarnTaskResponse = z.infer<typeof CompleteEarnTaskResponseSchema>;

export const DramaListResponseSchema = z.object({
  data: z.array(DramaSchema),
  pagination: PaginationSchema,
});

export type DramaListResponse = z.infer<typeof DramaListResponseSchema>;

export const TheaterFeedResponseSchema = z.object({
  data: z.array(TheaterDramaSchema),
  pagination: PaginationSchema,
});

export type TheaterFeedResponse = z.infer<typeof TheaterFeedResponseSchema>;

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

export const CLASSIFICATION_DIMENSION_KEYS = ['era_background', 'theme_plot', 'character_setting'] as const;

export const ClassificationGenderSchema = z.enum(['all', 'male', 'female']);
export type ClassificationGender = z.infer<typeof ClassificationGenderSchema>;

export const ClassificationDimensionKeySchema = z.enum(CLASSIFICATION_DIMENSION_KEYS);
export type ClassificationDimensionKey = z.infer<typeof ClassificationDimensionKeySchema>;

export const ClassificationTagsQuerySchema = z.object({
  gender: ClassificationGenderSchema.default('all'),
});

export type ClassificationTagsQuery = z.infer<typeof ClassificationTagsQuerySchema>;

export const ClassificationDimensionSchema = z.object({
  key: ClassificationDimensionKeySchema,
  name: z.string().trim().min(1),
  tags: z.array(z.string().trim().min(1)).default([]),
});

export type ClassificationDimension = z.infer<typeof ClassificationDimensionSchema>;

export const ClassificationDimensionsSchema = z
  .array(ClassificationDimensionSchema)
  .length(CLASSIFICATION_DIMENSION_KEYS.length)
  .superRefine((dimensions, context) => {
    CLASSIFICATION_DIMENSION_KEYS.forEach((expectedKey, index) => {
      const dimension = dimensions[index];
      if (!dimension) {
        return;
      }

      if (dimension.key !== expectedKey) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Expected classification dimension key ${expectedKey}`,
          path: [index, 'key'],
        });
      }
    });
  });

export const ClassificationTagsResultSchema = z.object({
  gender: ClassificationGenderSchema,
  dimensions: ClassificationDimensionsSchema,
});

export type ClassificationTagsResult = z.infer<typeof ClassificationTagsResultSchema>;

export const ClassificationTagsResponseSchema = z.object({
  data: ClassificationTagsResultSchema,
});

export type ClassificationTagsResponse = z.infer<typeof ClassificationTagsResponseSchema>;

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

export const DramaIdPathSchema = z.object({
  id: z.string().uuid(),
});

export type DramaIdPath = z.infer<typeof DramaIdPathSchema>;

export const CommentIdPathSchema = z.object({
  commentId: z.string().uuid(),
});

export type CommentIdPath = z.infer<typeof CommentIdPathSchema>;

export const DramaCommentPathSchema = DramaIdPathSchema;

export type DramaCommentPath = z.infer<typeof DramaCommentPathSchema>;

export const DramaCommentLikePathSchema = DramaIdPathSchema.extend({
  commentId: z.string().uuid(),
});

export type DramaCommentLikePath = z.infer<typeof DramaCommentLikePathSchema>;

export const CommentSortSchema = z.enum(['latest', 'hot']);
export type CommentSort = z.infer<typeof CommentSortSchema>;

export const CommentUserSummarySchema = z.object({
  id: z.string().uuid(),
  display_name: z.string().trim().min(1),
  avatar_url: z.string().url().nullable().default(null),
});

export type CommentUserSummary = z.infer<typeof CommentUserSummarySchema>;

export const CommentSchema = z.object({
  id: z.string().uuid(),
  drama_id: z.string().uuid(),
  content: z.string().trim().min(1).max(500),
  like_count: z.number().int().min(0),
  liked: z.boolean(),
  created_at: z.string(),
  updated_at: z.string(),
  user: CommentUserSummarySchema,
});

export type Comment = z.infer<typeof CommentSchema>;

export const CommentListQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(50).default(20),
  sort: CommentSortSchema.default('latest'),
});

export type CommentListQuery = z.infer<typeof CommentListQuerySchema>;

export const CreateCommentRequestSchema = z.object({
  content: z.string().trim().min(1).max(500),
});

export type CreateCommentRequest = z.infer<typeof CreateCommentRequestSchema>;

export const CommentListResponseSchema = z.object({
  data: z.array(CommentSchema),
  pagination: PaginationSchema,
});

export type CommentListResponse = z.infer<typeof CommentListResponseSchema>;

export const ToggleCommentLikeResponseSchema = z.object({
  comment_id: z.string().uuid(),
  liked: z.boolean(),
  like_count: z.number().int().min(0),
});

export type ToggleCommentLikeResponse = z.infer<typeof ToggleCommentLikeResponseSchema>;

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

export const RecentlyViewedItemSchema = z.object({
  drama_id: z.string().uuid(),
  title: z.string().min(1),
  cover_url: z.string().url().nullable().default(null),
  episode_number: z.number().int().min(1),
  progress: z.number().min(0),
  updated_at: z.string().datetime(),
});

export type RecentlyViewedItem = z.infer<typeof RecentlyViewedItemSchema>;

export const RecentlyViewedResponseSchema = z.object({
  code: z.literal(0),
  data: z.object({
    items: z.array(RecentlyViewedItemSchema).max(RECENTLY_VIEWED_LIMIT),
  }),
  message: z.string(),
});

export type RecentlyViewedResponse = z.infer<typeof RecentlyViewedResponseSchema>;

export const UserProfileSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email().optional().nullable(),
  display_name: z.string().min(1).optional().nullable(),
  avatar_url: z.string().url().optional().nullable(),
  created_at: z.string(),
  updated_at: z.string(),
});

export type UserProfile = z.infer<typeof UserProfileSchema>;

export const CountryCodeSchema = z.string().trim().regex(/^\+[1-9]\d{0,3}$/);
export type CountryCode = z.infer<typeof CountryCodeSchema>;

export const PhoneSchema = z.string().trim().regex(/^1\d{10}$/);
export type Phone = z.infer<typeof PhoneSchema>;

export const OtpCodeSchema = z.string().trim().regex(/^\d{6}$/);
export type OtpCode = z.infer<typeof OtpCodeSchema>;

export const AuthSceneSchema = z.enum(['login']);
export type AuthScene = z.infer<typeof AuthSceneSchema>;

export const SendOtpRequestSchema = z.object({
  countryCode: CountryCodeSchema.default('+86'),
  phone: PhoneSchema,
  scene: AuthSceneSchema.default('login'),
});
export type SendOtpRequest = z.infer<typeof SendOtpRequestSchema>;

export const SendOtpResponseSchema = z.object({
  code: z.literal(0),
  data: z.object({
    requestId: z.string().trim().min(1),
    cooldownSeconds: z.number().int().min(0),
    expiresInSeconds: z.number().int().min(0),
  }),
  message: z.string(),
});
export type SendOtpResponse = z.infer<typeof SendOtpResponseSchema>;

export const CreateAuthSessionRequestSchema = z.object({
  countryCode: CountryCodeSchema.default('+86'),
  phone: PhoneSchema,
  code: OtpCodeSchema,
});
export type CreateAuthSessionRequest = z.infer<typeof CreateAuthSessionRequestSchema>;

export const RefreshAuthSessionRequestSchema = z.object({
  refreshToken: z.string().trim().min(1),
});
export type RefreshAuthSessionRequest = z.infer<typeof RefreshAuthSessionRequestSchema>;

export const AuthUserSchema = z.object({
  id: z.string().uuid(),
  phone: z.string().trim().min(1),
  display_name: z.string().trim().min(1).nullable().optional(),
  avatar_url: z.string().url().nullable().optional(),
  role: z.enum(['admin', 'editor', 'viewer']).default('viewer'),
  is_new_user: z.boolean(),
});
export type AuthUser = z.infer<typeof AuthUserSchema>;

export const AuthSessionSchema = z.object({
  access_token: z.string().trim().min(1),
  refresh_token: z.string().trim().min(1),
  expires_at: z.string(),
  user: AuthUserSchema,
});
export type AuthSession = z.infer<typeof AuthSessionSchema>;

export const AuthSessionPayloadSchema = z.object({
  accessToken: z.string().trim().min(1),
  refreshToken: z.string().trim().min(1),
  expiresAt: z.string(),
  user: z.object({
    id: z.string().uuid(),
    phone: z.string().trim().min(1),
    displayName: z.string().trim().min(1).nullable().optional(),
    avatarUrl: z.string().url().nullable().optional(),
    role: z.enum(['admin', 'editor', 'viewer']).default('viewer'),
    isNewUser: z.boolean(),
  }),
});
export type AuthSessionPayload = z.infer<typeof AuthSessionPayloadSchema>;

export const AuthSessionResponseSchema = z.object({
  code: z.literal(0),
  data: AuthSessionPayloadSchema,
  message: z.string(),
});
export type AuthSessionResponse = z.infer<typeof AuthSessionResponseSchema>;

export const CurrentUserPayloadSchema = z.object({
  id: z.string().uuid(),
  phone: z.string().trim().min(1),
  displayName: z.string().trim().min(1).nullable().optional(),
  avatarUrl: z.string().url().nullable().optional(),
  role: z.enum(['admin', 'editor', 'viewer']).default('viewer'),
  isNewUser: z.boolean(),
});
export type CurrentUserPayload = z.infer<typeof CurrentUserPayloadSchema>;

export const CurrentUserResponseSchema = z.object({
  code: z.literal(0),
  data: CurrentUserPayloadSchema,
  message: z.string(),
});
export type CurrentUserResponse = z.infer<typeof CurrentUserResponseSchema>;

export const EmptySuccessResponseSchema = z.object({
  code: z.literal(0),
  data: z.null(),
  message: z.string(),
});
export type EmptySuccessResponse = z.infer<typeof EmptySuccessResponseSchema>;

// ============================================================
// Admin Panel Schemas
// ============================================================

export const AdminLoginRequestSchema = z.object({
  email: z.string().email(),
  password: z.string().min(6).max(72),
});

export type AdminLoginRequest = z.infer<typeof AdminLoginRequestSchema>;

export const AdminStatsResponseSchema = z.object({
  total_dramas: z.number().int().min(0),
  total_episodes: z.number().int().min(0),
  total_users: z.number().int().min(0),
});

export type AdminStatsResponse = z.infer<typeof AdminStatsResponseSchema>;

export const AdminDramaCreateSchema = z.object({
  title: z.string().min(1).max(200),
  description: z.string().default(''),
  cover_url: z.string().url().nullable().default(null),
  category: z.string().default(''),
  episode_count: z.number().int().min(0).default(0),
  tags: z.array(z.string()).default([]),
  rating: z.number().min(0).max(10).nullable().default(null),
});

export type AdminDramaCreate = z.infer<typeof AdminDramaCreateSchema>;

export const AdminDramaUpdateSchema = AdminDramaCreateSchema.partial();

export type AdminDramaUpdate = z.infer<typeof AdminDramaUpdateSchema>;

export const AdminEpisodeCreateSchema = z.object({
  title: z.string().min(1).max(200),
  episode_number: z.number().int().min(1),
  duration: z.number().int().min(0).optional().nullable(),
  video_url: z.string().url().optional().nullable(),
  thumbnail_url: z.string().url().optional().nullable(),
  description: z.string().optional().nullable(),
});

export type AdminEpisodeCreate = z.infer<typeof AdminEpisodeCreateSchema>;

export const AdminEpisodeUpdateSchema = AdminEpisodeCreateSchema.partial();

export type AdminEpisodeUpdate = z.infer<typeof AdminEpisodeUpdateSchema>;

export const AdminRoleUpdateSchema = z.object({
  role: z.enum(['admin', 'editor', 'viewer']),
});

export type AdminRoleUpdate = z.infer<typeof AdminRoleUpdateSchema>;

export const AdminUserProfileSchema = UserProfileSchema.extend({
  role: z.enum(['admin', 'editor', 'viewer']),
});

export type AdminUserProfile = z.infer<typeof AdminUserProfileSchema>;

export const AdminUserListResponseSchema = z.object({
  data: z.array(AdminUserProfileSchema),
  pagination: PaginationSchema,
});

export type AdminUserListResponse = z.infer<typeof AdminUserListResponseSchema>;
