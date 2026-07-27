'use client';

import { useEffect, useState, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { AdminSidebar } from './AdminSidebar';
import { AdminHeader } from './AdminHeader';
import { ToastProvider } from './Toast';
import { getSupabaseBrowserClient } from '@/lib/supabase';
import styles from './AdminLayout.module.css';

interface AdminLayoutClientProps {
  children: ReactNode;
}

export function AdminLayoutClient({ children }: AdminLayoutClientProps) {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<{
    email: string;
    role: string;
  } | null>(null);

  useEffect(() => {
    const supabase = getSupabaseBrowserClient();

    supabase.auth.getSession().then(({ data: { session } }) => {
      if (!session) {
        router.push('/admin/login');
        return;
      }

      const email = session.user.email ?? '';
      const role = session.user.app_metadata?.role ?? 'viewer';

      setUser({ email, role });
      setIsLoading(false);
    });
  }, [router]);

  const handleLogout = async () => {
    const supabase = getSupabaseBrowserClient();
    await supabase.auth.signOut();
    router.push('/admin/login');
  };

  if (isLoading || !user) {
    return (
      <div className={styles.loadingContainer}>
        <span>加载中...</span>
      </div>
    );
  }

  return (
    <ToastProvider>
      <div className={styles.layoutWrapper}>
        <AdminHeader email={user.email} role={user.role} onLogout={handleLogout} />
        <div className={styles.mainArea}>
          <AdminSidebar role={user.role} />
          <main className={styles.content}>{children}</main>
        </div>
      </div>
    </ToastProvider>
  );
}