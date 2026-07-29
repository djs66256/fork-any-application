import { InteractionMessage } from '@/lib/schemas';

export interface ListInteractionMessagesParams {
  userId: string;
  page: number;
  pageSize: number;
}

export interface InteractionMessageListResult {
  data: InteractionMessage[];
  pagination: {
    page: number;
    page_size: number;
    total: number;
    total_pages: number;
  };
}

export interface InteractionMessageRepositoryInterface {
  listByUser(params: ListInteractionMessagesParams): Promise<InteractionMessageListResult>;
}
