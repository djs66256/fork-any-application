import { InteractionMessageSchema, type InteractionMessage } from '@/lib/schemas';
import {
  InteractionMessageListResult,
  InteractionMessageRepositoryInterface,
  ListInteractionMessagesParams,
} from '@/repositories/interfaces/interaction-message.repository.interface';

const DEFAULT_USER_ID = '00000000-0000-4000-8000-13800138000';

const INTERACTION_MESSAGE_SEEDS = new Map<string, InteractionMessage[]>([
  [
    DEFAULT_USER_ID,
    [
      {
        id: '660e8400-e29b-41d4-a716-446655440010',
        type: 'comment_reply',
        title: '有人回复了你的评论',
        summary: '“这集反转真不错” 收到一条新回复。',
        sent_at: '2026-07-29T09:00:00.000Z',
      },
      {
        id: '660e8400-e29b-41d4-a716-446655440011',
        type: 'comment_like',
        title: '你的评论收到了点赞',
        summary: '“女主这波打脸太爽了。” 获得了新的点赞。',
        sent_at: '2026-07-29T07:00:00.000Z',
      },
      {
        id: '660e8400-e29b-41d4-a716-446655440012',
        type: 'system_hint',
        title: '互动提醒',
        summary: '登录后可持续查看最近的互动动态。',
        sent_at: '2026-07-28T07:00:00.000Z',
      },
    ].map((item) => InteractionMessageSchema.parse(item)),
  ],
]);

function cloneMessage(message: InteractionMessage): InteractionMessage {
  return InteractionMessageSchema.parse({ ...message });
}

function computeTotalPages(total: number, pageSize: number): number {
  return total === 0 ? 0 : Math.ceil(total / pageSize);
}

export class InteractionMessageMockRepository implements InteractionMessageRepositoryInterface {
  constructor(private readonly messagesByUser = INTERACTION_MESSAGE_SEEDS) {}

  async listByUser(params: ListInteractionMessagesParams): Promise<InteractionMessageListResult> {
    const messages = this.messagesByUser.get(params.userId) ?? [];
    const total = messages.length;
    const start = (params.page - 1) * params.pageSize;
    const data = messages.slice(start, start + params.pageSize).map(cloneMessage);

    return {
      data,
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: computeTotalPages(total, params.pageSize),
      },
    };
  }
}
