import { beforeEach, describe, expect, it } from 'vitest';
import { CheckInMockRepository } from '@/repositories/mock/check-in.mock.repository';
import type { CheckInSubject } from '@/repositories/interfaces/check-in.repository.interface';

const USER_SUBJECT: CheckInSubject = {
  type: 'user',
  id: '00000000-0000-4000-8000-13800138000',
};

const INSTALLATION_SUBJECT: CheckInSubject = {
  type: 'installation',
  id: '770e8400-e29b-41d4-a716-446655440000',
};

describe('CheckInMockRepository', () => {
  let repository: CheckInMockRepository;

  beforeEach(() => {
    repository = new CheckInMockRepository();
  });

  it('should create records idempotently per subject and business day', async () => {
    const first = await repository.createIfAbsent({
      subject_type: USER_SUBJECT.type,
      subject_id: USER_SUBJECT.id,
      business_date: '2026-07-29',
      streak_day: 1,
    });

    const second = await repository.createIfAbsent({
      subject_type: USER_SUBJECT.type,
      subject_id: USER_SUBJECT.id,
      business_date: '2026-07-29',
      streak_day: 1,
    });

    expect(second.id).toBe(first.id);
  });

  it('should isolate anonymous and logged-in subjects', async () => {
    await repository.createIfAbsent({
      subject_type: USER_SUBJECT.type,
      subject_id: USER_SUBJECT.id,
      business_date: '2026-07-29',
      streak_day: 1,
    });
    await repository.createIfAbsent({
      subject_type: INSTALLATION_SUBJECT.type,
      subject_id: INSTALLATION_SUBJECT.id,
      business_date: '2026-07-29',
      streak_day: 1,
    });

    const userRecords = await repository.listRecentBySubject(USER_SUBJECT);
    const installationRecords = await repository.listRecentBySubject(INSTALLATION_SUBJECT);

    expect(userRecords).toHaveLength(1);
    expect(installationRecords).toHaveLength(1);
    expect(userRecords[0]?.subject_type).toBe('user');
    expect(installationRecords[0]?.subject_type).toBe('installation');
  });

  it('should return most recent records first', async () => {
    await repository.createIfAbsent({
      subject_type: USER_SUBJECT.type,
      subject_id: USER_SUBJECT.id,
      business_date: '2026-07-27',
      streak_day: 1,
    });
    await repository.createIfAbsent({
      subject_type: USER_SUBJECT.type,
      subject_id: USER_SUBJECT.id,
      business_date: '2026-07-29',
      streak_day: 3,
    });
    await repository.createIfAbsent({
      subject_type: USER_SUBJECT.type,
      subject_id: USER_SUBJECT.id,
      business_date: '2026-07-28',
      streak_day: 2,
    });

    const result = await repository.listRecentBySubject(USER_SUBJECT, 2);

    expect(result).toHaveLength(2);
    expect(result.map((item) => item.business_date)).toEqual(['2026-07-29', '2026-07-28']);
  });
});
