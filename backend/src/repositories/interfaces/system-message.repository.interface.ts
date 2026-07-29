import { SystemMessage } from '@/lib/schemas';

export interface ListSystemMessagesParams {
  page: number;
  pageSize: number;
}

export interface SystemMessageListResult {
  data: SystemMessage[];
  pagination: {
    page: number;
    page_size: number;
    total: number;
    total_pages: number;
  };
}

export interface SystemMessageRepositoryInterface {
  getLatest(): Promise<SystemMessage | null>;
  list(params: ListSystemMessagesParams): Promise<SystemMessageListResult>;
}
