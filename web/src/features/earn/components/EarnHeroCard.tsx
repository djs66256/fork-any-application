import { Button, Card } from '@/components/ui';
import styles from './EarnHeroCard.module.css';

interface EarnHeroCardProps {
  coins: number;
  isLoggedIn: boolean;
  onLoginClick: () => void;
}

export function EarnHeroCard({ coins, isLoggedIn, onLoginClick }: EarnHeroCardProps) {
  return (
    <Card as="section" className={styles.card}>
      <div className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>赚钱中心</p>
          <h1 className={styles.title}>边追剧边赚金币</h1>
          <p className={styles.subtitle}>代表任务完成后即可回到当前页查看收益变化</p>
        </div>
        <div className={styles.summary} aria-label={`当前累计 ${coins} 金币`}>
          <span className={styles.summaryLabel}>累计金币</span>
          <strong className={styles.summaryValue}>{coins}</strong>
        </div>
      </div>
      {!isLoggedIn ? (
        <div className={styles.loginHint}>
          <span>登录后可同步任务奖励与收益记录</span>
          <Button type="button" onClick={onLoginClick} size="sm">
            去登录
          </Button>
        </div>
      ) : null}
    </Card>
  );
}
