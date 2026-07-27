'use client';

import styles from './AdminHeader.module.css';

interface AdminHeaderProps {
  email: string;
  role: string;
  onLogout: () => void;
}

export function AdminHeader({ email, role, onLogout }: AdminHeaderProps) {
  const roleLabel: Record<string, string> = {
    admin: '超级管理员',
    editor: '内容编辑',
    viewer: '查看者',
  };

  return (
    <header className={styles.header}>
      <span className={styles.logo}>ShortDrama Admin</span>
      <div className={styles.userSection}>
        <span className={styles.roleBadge}>{roleLabel[role] ?? role}</span>
        <span className={styles.userEmail}>{email}</span>
        <button
          className={styles.logoutButton}
          onClick={onLogout}
          type="button"
        >
          退出登录
        </button>
      </div>
    </header>
  );
}