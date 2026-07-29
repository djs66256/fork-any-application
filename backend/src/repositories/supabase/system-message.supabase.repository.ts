import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';
import { SystemMessageListResponseSchema, SystemMessageSchema } from '@/lib/schemas';
import {
  ListSystemMessagesParams,
  SystemMessageListResult,
  SystemMessageRepositoryInterface,
} from '@/repositories/interfaces/system-message.repository.interface';

const SYSTEM_MESSAGE_SELECT_COLUMNS = 'id,title,summary,sent_at';

function isAvailabilityError(error: { message?: string | null; code?: string | null }): boolean {
  const message = (error.message ?? '').toLowerCase();
  const code = (error.code ?? '').toLowerCase();

  return message.includes('failed to fetch')
    || message.includes('network')
    || message.includes('timeout')
    || message.includes('unavailable')
    || message.includes('connection')
    || code === '08000'
    || code === '08003'
    || code === '08006'
    || code === '57p01';
}

function computeTotalPages(total: number, pageSize: number): number {
  return total === 0 ? 0 : Math.ceil(total / pageSize);
}

export class SystemMessageSupabaseRepository implements SystemMessageRepositoryInterface {
  async getLatest() {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('system_messages')
      .select(SYSTEM_MESSAGE_SELECT_COLUMNS)
      .order('sent_at', { ascending: false })
      .limit(1)
      .maybeSingle();

    if (error) {
      if (isAvailabilityError(error)) {
        throw Errors.serviceUnavailable('system_messages');
      }
      throw Errors.serviceUnavailable('system_messages');
    }

    if (!data) {
      return null;
    }

    const parsed = SystemMessageSchema.safeParse(data);
    if (!parsed.success) {
      throw Errors.serviceUnavailable('system_messages');
    }

    return parsed.data;
  }

  async list(params: ListSystemMessagesParams): Promise<SystemMessageListResult> {
    const supabase = getSupabaseAdmin();
    const from = (params.page - 1) * params.pageSize;
    const to = from + params.pageSize - 1;

    const { data, error, count } = await supabase
      .from('system_messages')
      .select(SYSTEM_MESSAGE_SELECT_COLUMNS, { count: 'exact', head: false })
      .order('sent_at', { ascending: false })
      .range(from, to);

    if (error) {
      if (isAvailabilityError(error)) {
        throw Errors.serviceUnavailable('system_messages');
      }
      throw Errors.serviceUnavailable('system_messages');
    }

    const parsedData = (data ?? []).map((item) => {
      const parsed = SystemMessageSchema.safeParse(item);
      if (!parsed.success) {
        throw Errors.serviceUnavailable('system_messages');
      }
      return parsed.data;
    });

    return SystemMessageListResponseSchema.parse({
      data: parsedData,
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total: count ?? 0,
        total_pages: computeTotalPages(count ?? 0, params.pageSize),
      },
    });
  }
}
