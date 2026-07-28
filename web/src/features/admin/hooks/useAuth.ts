// Re-export from AuthContext for backward compatibility.
// All existing imports of useAuth / AdminUser / AuthState continue to work.
export { useAuth } from '@/features/admin/contexts/AuthContext';
export type { AdminUser } from '@/features/admin/contexts/AuthContext';

import type { AdminUser } from '@/features/admin/contexts/AuthContext';

/** @deprecated Use AdminUser from AuthContext instead */
export interface AuthState {
  user: AdminUser | null;
  isLoading: boolean;
  error: string | null;
}
