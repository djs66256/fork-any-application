'use client';

import styles from './AdminHeader.module.css';
import { useTheme } from '@/lib/theme';
import { useAuth } from '@/features/admin/contexts/AuthContext';

const roleLabel: Record<string, string> = {
  admin: '超级管理员',
  editor: '内容编辑',
  viewer: '查看者',
};

export function AdminHeader() {
  const { theme, toggleTheme } = useTheme();
  const { user, role, logout } = useAuth();

  return (
    <header className={styles.header}>
      <span className={styles.logo}>ShortDrama Admin</span>
      <div className={styles.userSection}>
        <button
          className={styles.themeToggle}
          onClick={toggleTheme}
          type="button"
          aria-label={theme === 'light' ? '切换暗色模式' : '切换亮色模式'}
          title={theme === 'light' ? '切换暗色模式' : '切换亮色模式'}
        >
          {theme === 'light' ? '☀' : '☾'}
        </button>
        <span className={styles.roleBadge}>{roleLabel[role] ?? role}</span>
        <span className={styles.userEmail}>{user?.email ?? ''}</span>
        <button
          className={styles.logoutButton}
          onClick={logout}
          type="button"
        >
          退出登录
        </button>
      </div>
    </header>
  );
}