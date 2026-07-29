import { Errors } from '@/lib/errors';
import {
  InteractionMessageListResponseSchema,
  MessagePreviewSchema,
  SystemMessageListResponseSchema,
  type InteractionMessageListResponse,
  type MessagePreview,
  type SystemMessageListResponse,
} from '@/lib/schemas';
import type { InteractionMessageRepositoryInterface } from '@/repositories/interfaces/interaction-message.repository.interface';
import type { SystemMessageRepositoryInterface } from '@/repositories/interfaces/system-message.repository.interface';

function isAppError(error: unknown): error is Error & { code: string } {
  return error instanceof Error && 'code' in error;
}

function formatRelativeTime(sentAt: string, nowIso: string): string {
  const diff = Math.max(0, Date.parse(nowIso) - Date.parse(sentAt));
  const hour = 60 * 60 * 1000;
  const minute = 60 * 1000;

  if (diff >= hour) {
    return `${Math.max(1, Math.floor(diff / hour))}小时前`;
  }

  return `${Math.max(1, Math.floor(diff / minute))}分钟前`;
}

export interface MessageServiceOptions {
  nowProvider?: () => string;
}

export class MessageService {
  constructor(
    private readonly systemMessageRepository: SystemMessageRepositoryInterface,
    private readonly interactionMessageRepository: InteractionMessageRepositoryInterface,
    private readonly options: MessageServiceOptions = {},
  ) {}

  async getPreview(): Promise<MessagePreview | null> {
    try {
      const latest = await this.systemMessageRepository.getLatest();
      if (!latest) {
        return null;
      }

      return MessagePreviewSchema.parse({
        title: latest.title,
        summary: latest.summary,
        relative_time: formatRelativeTime(
          latest.sent_at,
          this.options.nowProvider?.() ?? new Date().toISOString(),
        ),
      });
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.serviceUnavailable('messages_preview');
    }
  }

  async listSystemMessages(input: { page: number; pageSize: number }): Promise<SystemMessageListResponse> {
    try {
      return SystemMessageListResponseSchema.parse(await this.systemMessageRepository.list(input));
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.serviceUnavailable('system_messages');
    }
  }

  async listInteractionMessages(input: {
    userId?: string;
    page: number;
    pageSize: number;
  }): Promise<InteractionMessageListResponse> {
    if (!input.userId) {
      throw Errors.authUnauthorized('请先登录后查看互动消息');
    }

    try {
      return InteractionMessageListResponseSchema.parse(
        await this.interactionMessageRepository.listByUser({
          userId: input.userId,
          page: input.page,
          pageSize: input.pageSize,
        }),
      );
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.serviceUnavailable('interaction_messages');
    }
  }
}
