'use client';

import { type ReactNode } from 'react';
import { usePathname } from 'next/navigation';
import { AdminSidebar } from './AdminSidebar';
import { AdminHeader } from './AdminHeader';
import { ToastProvider } from './Toast';
import { useAuth } from '@/features/admin/contexts/AuthContext';
import styles from './AdminLayout.module.css';

interface AdminLayoutClientProps {
  children: ReactNode;
}

export function AdminLayoutClient({ children }: AdminLayoutClientProps) {
  const pathname = usePathname();
  const { isLoading, isAuthenticated, role } = useAuth();

  const isLoginPage = pathname === '/admin/login';

  // Login page renders without the admin shell
  if (isLoginPage) {
    return <>{children}</>;
  }

  if (isLoading) {
    return (
      <div className={styles.loadingContainer}>
        <span>加载中...</span>
      </div>
    );
  }

  // Middleware handles redirect when not authenticated; show nothing here
  if (!isAuthenticated) {
    return (
      <div className={styles.loadingContainer}>
        <span>加载中...</span>
      </div>
    );
  }

  return (
    <ToastProvider>
      <div className={styles.layoutWrapper}>
        <AdminHeader />
        <div className={styles.mainArea}>
          <AdminSidebar role={role} />
          <main className={styles.content}>{children}</main>
        </div>
      </div>
    </ToastProvider>
  );
}