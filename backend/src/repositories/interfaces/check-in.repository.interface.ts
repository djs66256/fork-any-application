import { z } from 'zod';

export const CheckInSubjectTypeSchema = z.enum(['user', 'installation']);
export type CheckInSubjectType = z.infer<typeof CheckInSubjectTypeSchema>;

export const CheckInSubjectSchema = z.object({
  type: CheckInSubjectTypeSchema,
  id: z.string().min(1),
});
export type CheckInSubject = z.infer<typeof CheckInSubjectSchema>;

export const CheckInRecordSchema = z.object({
  id: z.string().uuid(),
  subject_type: CheckInSubjectTypeSchema,
  subject_id: z.string().min(1),
  business_date: z.string().min(1),
  streak_day: z.number().int().min(1).max(7),
  created_at: z.string().min(1),
});
export type CheckInRecord = z.infer<typeof CheckInRecordSchema>;

export const CreateCheckInRecordInputSchema = CheckInRecordSchema.omit({
  id: true,
  created_at: true,
});
export type CreateCheckInRecordInput = z.infer<typeof CreateCheckInRecordInputSchema>;

export interface CheckInRepositoryInterface {
  listRecentBySubject(subject: CheckInSubject, limit?: number): Promise<CheckInRecord[]>;
  createIfAbsent(input: CreateCheckInRecordInput): Promise<CheckInRecord>;
}
