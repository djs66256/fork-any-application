import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';
import {
  CheckInRecordSchema,
  type CheckInRecord,
  type CheckInRepositoryInterface,
  type CheckInSubject,
  type CreateCheckInRecordInput,
} from '@/repositories/interfaces/check-in.repository.interface';

const CHECK_IN_SELECT_COLUMNS = 'id,subject_type,subject_id,business_date,streak_day,created_at';

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

function mapRow(row: unknown): CheckInRecord {
  const parsed = CheckInRecordSchema.safeParse(row);
  if (!parsed.success) {
    throw Errors.serviceUnavailable('check_in_records');
  }

  return parsed.data;
}

export class CheckInSupabaseRepository implements CheckInRepositoryInterface {
  async listRecentBySubject(subject: CheckInSubject, limit = 30): Promise<CheckInRecord[]> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('check_in_records')
      .select(CHECK_IN_SELECT_COLUMNS)
      .eq('subject_type', subject.type)
      .eq('subject_id', subject.id)
      .order('business_date', { ascending: false })
      .order('created_at', { ascending: false })
      .limit(limit);

    if (error) {
      if (isAvailabilityError(error)) {
        throw Errors.serviceUnavailable('check_in_records');
      }
      throw Errors.serviceUnavailable('check_in_records');
    }

    return (data ?? []).map(mapRow);
  }

  async createIfAbsent(input: CreateCheckInRecordInput): Promise<CheckInRecord> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('check_in_records')
      .insert(input)
      .select(CHECK_IN_SELECT_COLUMNS)
      .single();

    if (!error) {
      return mapRow(data);
    }

    if (error.code === '23505') {
      const { data: existing, error: existingError } = await supabase
        .from('check_in_records')
        .select(CHECK_IN_SELECT_COLUMNS)
        .eq('subject_type', input.subject_type)
        .eq('subject_id', input.subject_id)
        .eq('business_date', input.business_date)
        .single();

      if (existingError || !existing) {
        throw Errors.serviceUnavailable('check_in_records');
      }

      return mapRow(existing);
    }

    if (isAvailabilityError(error)) {
      throw Errors.serviceUnavailable('check_in_records');
    }

    throw Errors.serviceUnavailable('check_in_records');
  }
}
