import { SystemMessageSchema, type SystemMessage } from '@/lib/schemas';
import {
  ListSystemMessagesParams,
  SystemMessageListResult,
  SystemMessageRepositoryInterface,
} from '@/repositories/interfaces/system-message.repository.interface';

const SYSTEM_MESSAGE_SEEDS: SystemMessage[] = [
  {
    id: '550e8400-e29b-41d4-a716-446655440003',
    title: '系统通知',
    summary: '你收藏的专题上新了 3 部短剧。',
    sent_at: '2026-07-29T10:00:00.000Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440002',
    title: '系统通知',
    summary: '你关注的剧集已更新第 12 集。',
    sent_at: '2026-07-29T08:00:00.000Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440001',
    title: '活动提醒',
    summary: '本周签到达到 7 天即可解锁额外奖励展示。',
    sent_at: '2026-07-28T08:00:00.000Z',
  },
].map((item) => SystemMessageSchema.parse(item));

function cloneMessage(message: SystemMessage): SystemMessage {
  return SystemMessageSchema.parse({ ...message });
}

function computeTotalPages(total: number, pageSize: number): number {
  return total === 0 ? 0 : Math.ceil(total / pageSize);
}

export class SystemMessageMockRepository implements SystemMessageRepositoryInterface {
  constructor(private readonly messages: SystemMessage[] = SYSTEM_MESSAGE_SEEDS) {}

  async getLatest(): Promise<SystemMessage | null> {
    return this.messages[0] ? cloneMessage(this.messages[0]) : null;
  }

  async list(params: ListSystemMessagesParams): Promise<SystemMessageListResult> {
    const total = this.messages.length;
    const start = (params.page - 1) * params.pageSize;
    const data = this.messages.slice(start, start + params.pageSize).map(cloneMessage);

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
