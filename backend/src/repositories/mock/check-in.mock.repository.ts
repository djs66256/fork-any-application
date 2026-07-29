import { randomUUID } from 'node:crypto';
import {
  CheckInRecord,
  CheckInRecordSchema,
  CheckInRepositoryInterface,
  CheckInSubject,
  CreateCheckInRecordInput,
} from '@/repositories/interfaces/check-in.repository.interface';

function cloneRecord(record: CheckInRecord): CheckInRecord {
  return CheckInRecordSchema.parse({ ...record });
}

function buildKey(subject: CheckInSubject, businessDate: string): string {
  return `${subject.type}:${subject.id}:${businessDate}`;
}

export class CheckInMockRepository implements CheckInRepositoryInterface {
  private readonly records = new Map<string, CheckInRecord>();

  constructor(initialRecords: CheckInRecord[] = []) {
    initialRecords.forEach((record) => {
      const parsed = CheckInRecordSchema.parse(record);
      this.records.set(buildKey({ type: parsed.subject_type, id: parsed.subject_id }, parsed.business_date), parsed);
    });
  }

  async listRecentBySubject(subject: CheckInSubject, limit = 30): Promise<CheckInRecord[]> {
    return Array.from(this.records.values())
      .filter((record) => record.subject_type === subject.type && record.subject_id === subject.id)
      .sort((left, right) => {
        const businessDateCompare = right.business_date.localeCompare(left.business_date);
        if (businessDateCompare !== 0) {
          return businessDateCompare;
        }
        return right.created_at.localeCompare(left.created_at);
      })
      .slice(0, limit)
      .map(cloneRecord);
  }

  async createIfAbsent(input: CreateCheckInRecordInput): Promise<CheckInRecord> {
    const key = buildKey({ type: input.subject_type, id: input.subject_id }, input.business_date);
    const existing = this.records.get(key);
    if (existing) {
      return cloneRecord(existing);
    }

    const record = CheckInRecordSchema.parse({
      id: randomUUID(),
      subject_type: input.subject_type,
      subject_id: input.subject_id,
      business_date: input.business_date,
      streak_day: input.streak_day,
      created_at: new Date().toISOString(),
    });

    this.records.set(key, record);
    return cloneRecord(record);
  }
}
