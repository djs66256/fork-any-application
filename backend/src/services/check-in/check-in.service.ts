import { Errors } from '@/lib/errors';
import { SignInStatusSchema, type SignInDay, type SignInStatus } from '@/lib/schemas';
import type {
  CheckInRecord,
  CheckInRepositoryInterface,
  CheckInSubject,
} from '@/repositories/interfaces/check-in.repository.interface';

const SIGN_IN_REWARD_LABELS = [
  '金币 x10',
  '金币 x20',
  '金币 x30',
  '金币 x40',
  '金币 x50',
  '金币 x60',
  '金币 x70',
] as const;

export interface CheckInServiceOptions {
  businessDateProvider?: () => string;
}

function isAppError(error: unknown): error is Error & { code: string } {
  return error instanceof Error && 'code' in error;
}

function addDays(dateString: string, days: number): string {
  const date = new Date(`${dateString}T00:00:00.000Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function isConsecutive(previousDate: string, currentDate: string): boolean {
  return addDays(previousDate, 1) === currentDate;
}

export class CheckInService {
  constructor(
    private readonly repository: CheckInRepositoryInterface,
    private readonly options: CheckInServiceOptions = {},
  ) {}

  async getStatus(input: { userId?: string; installationId?: string }): Promise<SignInStatus> {
    const subject = this.resolveSubject(input);
    const serverDate = this.getBusinessDate();
    const records = await this.repository.listRecentBySubject(subject, 30);
    return this.buildStatus(records, serverDate);
  }

  async checkIn(input: { userId?: string; installationId?: string }): Promise<SignInStatus> {
    const subject = this.resolveSubject(input);
    const serverDate = this.getBusinessDate();
    const records = await this.repository.listRecentBySubject(subject, 30);
    const existingToday = records.find((record) => record.business_date === serverDate);

    if (!existingToday) {
      const streakDay = this.computeNextStreakDay(records, serverDate);
      await this.repository.createIfAbsent({
        subject_type: subject.type,
        subject_id: subject.id,
        business_date: serverDate,
        streak_day: streakDay,
      });
    }

    const latestRecords = await this.repository.listRecentBySubject(subject, 30);
    return this.buildStatus(latestRecords, serverDate);
  }

  private resolveSubject(input: { userId?: string; installationId?: string }): CheckInSubject {
    if (input.userId) {
      return {
        type: 'user',
        id: input.userId,
      };
    }

    if (input.installationId) {
      return {
        type: 'installation',
        id: input.installationId,
      };
    }

    throw Errors.validationError('Missing X-Installation-Id');
  }

  private getBusinessDate(): string {
    return this.options.businessDateProvider?.() ?? new Date().toISOString().slice(0, 10);
  }

  private computeNextStreakDay(records: CheckInRecord[], serverDate: string): number {
    const sorted = this.sortRecordsAscending(records);
    const lastRecord = sorted.at(-1);

    if (!lastRecord) {
      return 1;
    }

    if (!isConsecutive(lastRecord.business_date, serverDate)) {
      return 1;
    }

    if (lastRecord.streak_day === 7) {
      return 1;
    }

    return lastRecord.streak_day + 1;
  }

  private buildStatus(records: CheckInRecord[], serverDate: string): SignInStatus {
    try {
      const sorted = this.sortRecordsAscending(records);
      const currentCycle = this.buildCurrentCycle(sorted, serverDate);
      const todayRecord = currentCycle.find((record) => record.business_date === serverDate);
      const todaySigned = Boolean(todayRecord);
      const currentStreak = todaySigned
        ? (todayRecord?.streak_day ?? 0)
        : (currentCycle.at(-1)?.streak_day ?? 0);
      const nextDay = todaySigned ? currentStreak : Math.min(currentStreak + 1, 7);

      const days: SignInDay[] = SIGN_IN_REWARD_LABELS.map((rewardLabel, index) => {
        const day = index + 1;
        let status: SignInDay['status'] = 'locked';

        if (currentCycle.some((record) => record.streak_day === day)) {
          status = 'signed';
        } else if (!todaySigned && day === nextDay) {
          status = 'today';
        }

        return {
          day,
          title: `第 ${day} 天`,
          reward_label: rewardLabel,
          status,
        };
      });

      return SignInStatusSchema.parse({
        server_date: serverDate,
        should_show_popup: !todaySigned,
        today_signed: todaySigned,
        current_streak: currentStreak,
        reward_copy: todaySigned
          ? `已完成第 ${currentStreak || 1} 天签到`
          : `今日签到可领取第 ${nextDay} 天奖励`,
        days,
      });
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.serviceUnavailable('check_in_status');
    }
  }

  private sortRecordsAscending(records: CheckInRecord[]): CheckInRecord[] {
    return [...records].sort((left, right) => left.business_date.localeCompare(right.business_date));
  }

  private buildCurrentCycle(records: CheckInRecord[], serverDate: string): CheckInRecord[] {
    if (records.length === 0) {
      return [];
    }

    const lastRecord = records.at(-1);
    if (!lastRecord) {
      return [];
    }

    if (lastRecord.business_date !== serverDate && !isConsecutive(lastRecord.business_date, serverDate)) {
      return [];
    }

    if (
      lastRecord.business_date !== serverDate
      && isConsecutive(lastRecord.business_date, serverDate)
      && lastRecord.streak_day === 7
    ) {
      return [];
    }

    const cycle: CheckInRecord[] = [lastRecord];

    for (let index = records.length - 2; index >= 0; index -= 1) {
      const current = records[index];
      const next = cycle[0];
      if (!current || !next) {
        continue;
      }

      if (!isConsecutive(current.business_date, next.business_date)) {
        break;
      }

      cycle.unshift(current);
      if (cycle.length >= 7) {
        break;
      }
    }

    return cycle;
  }
}
