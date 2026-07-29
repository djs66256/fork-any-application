import { Card } from '@/components/ui';
import type { EarnDailyReward } from '@/lib/schemas';
import styles from './EarnDailyRewardsGrid.module.css';

interface EarnDailyRewardsGridProps {
  rewards: EarnDailyReward[];
}

function getStatusLabel(status: EarnDailyReward['status']): string {
  switch (status) {
    case 'claimable':
      return '可领取';
    case 'claimed':
      return '已领取';
    case 'locked':
      return '未到达';
    default:
      return '未到达';
  }
}

export function EarnDailyRewardsGrid({ rewards }: EarnDailyRewardsGridProps) {
  return (
    <Card as="section" className={styles.section}>
      <div className={styles.header}>
        <h2 className={styles.title}>连续看剧福利</h2>
        <p className={styles.description}>首版仅展示 7 宫格状态，不触发真实领奖</p>
      </div>
      <div className={styles.grid}>
        {rewards.map((reward) => (
          <article key={reward.day} className={styles.cell} data-status={reward.status}>
            <span className={styles.day}>第 {reward.day} 天</span>
            <strong className={styles.coins}>+{reward.coins}</strong>
            <span className={styles.status}>{getStatusLabel(reward.status)}</span>
          </article>
        ))}
      </div>
    </Card>
  );
}
