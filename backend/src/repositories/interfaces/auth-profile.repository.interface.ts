import { AuthUser } from '@/lib/schemas';

export interface AuthProfileRepositoryInterface {
  findAuthUserById(userId: string): Promise<AuthUser | null>;
  ensureAuthUserProfile(userId: string): Promise<void>;
}
