'use client';

import { StatCard } from './StatCard';
import { useStats } from '@/features/admin/hooks/useStats';
import styles from './Dashboard.module.css';

export function Dashboard() {
  const { stats, isLoading, error, refetch } = useStats();

  if (error) {
    return (
      <div className={styles.errorContainer}>
        <span className={styles.errorText}>{error}</span>
        <button className={styles.retryButton} onClick={refetch} type="button">
          重试
        </button>
      </div>
    );
  }

  return (
    <div className={styles.dashboard}>
      <h1 className={styles.pageTitle}>仪表盘</h1>
      <div className={styles.statsRow}>
        <StatCard
          label="总短剧数"
          value={stats?.total_dramas ?? 0}
          isLoading={isLoading}
        />
        <StatCard
          label="总剧集数"
          value={stats?.total_episodes ?? 0}
          isLoading={isLoading}
        />
        <StatCard
          label="用户数"
          value={stats?.total_users ?? 0}
          isLoading={isLoading}
        />
      </div>
    </div>
  );
}