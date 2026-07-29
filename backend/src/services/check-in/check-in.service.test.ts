import { beforeEach, describe, expect, it } from 'vitest';
import { CheckInService } from './check-in.service';
import { CheckInMockRepository } from '@/repositories/mock/check-in.mock.repository';

const USER_ID = '00000000-0000-4000-8000-13800138000';
const INSTALLATION_ID = '770e8400-e29b-41d4-a716-446655440000';

describe('CheckInService', () => {
  let repository: CheckInMockRepository;
  let service: CheckInService;

  beforeEach(() => {
    repository = new CheckInMockRepository();
    service = new CheckInService(repository, {
      businessDateProvider: () => '2026-07-29',
    });
  });

  it('should require installation id for anonymous users', async () => {
    await expect(service.getStatus({})).rejects.toMatchObject({ code: 'VALIDATION_ERROR' });
  });

  it('should prefer logged-in users over installation id', async () => {
    await repository.createIfAbsent({
      subject_type: 'user',
      subject_id: USER_ID,
      business_date: '2026-07-28',
      streak_day: 1,
    });
    await repository.createIfAbsent({
      subject_type: 'installation',
      subject_id: INSTALLATION_ID,
      business_date: '2026-07-28',
      streak_day: 7,
    });

    const result = await service.getStatus({
      userId: USER_ID,
      installationId: INSTALLATION_ID,
    });

    expect(result.current_streak).toBe(1);
    expect(result.days[0]?.status).toBe('signed');
    expect(result.days[1]?.status).toBe('today');
  });

  it('should create a new check-in and become idempotent on repeated calls', async () => {
    const first = await service.checkIn({ installationId: INSTALLATION_ID });
    const second = await service.checkIn({ installationId: INSTALLATION_ID });

    expect(first.today_signed).toBe(true);
    expect(second.today_signed).toBe(true);
    expect(second.current_streak).toBe(1);
    expect(second.should_show_popup).toBe(false);
  });

  it('should compute today slot for existing streaks', async () => {
    await repository.createIfAbsent({
      subject_type: 'installation',
      subject_id: INSTALLATION_ID,
      business_date: '2026-07-27',
      streak_day: 1,
    });
    await repository.createIfAbsent({
      subject_type: 'installation',
      subject_id: INSTALLATION_ID,
      business_date: '2026-07-28',
      streak_day: 2,
    });

    const result = await service.getStatus({ installationId: INSTALLATION_ID });

    expect(result.today_signed).toBe(false);
    expect(result.current_streak).toBe(2);
    expect(result.days[0]?.status).toBe('signed');
    expect(result.days[1]?.status).toBe('signed');
    expect(result.days[2]?.status).toBe('today');
    expect(result.should_show_popup).toBe(true);
  });

  it('should reset to day 1 after day 7 cycle completes and next day arrives', async () => {
    const cyclingRepository = new CheckInMockRepository();
    for (let index = 0; index < 7; index += 1) {
      await cyclingRepository.createIfAbsent({
        subject_type: 'user',
        subject_id: USER_ID,
        business_date: `2026-07-${String(index + 22).padStart(2, '0')}`,
        streak_day: index + 1,
      });
    }

    const nextCycleService = new CheckInService(cyclingRepository, {
      businessDateProvider: () => '2026-07-29',
    });
    const status = await nextCycleService.getStatus({ userId: USER_ID });

    expect(status.current_streak).toBe(0);
    expect(status.days[0]?.status).toBe('today');

    const checkedIn = await nextCycleService.checkIn({ userId: USER_ID });
    expect(checkedIn.current_streak).toBe(1);
    expect(checkedIn.days[0]?.status).toBe('signed');
  });
});
