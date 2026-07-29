import { Button, Card } from '@/components/ui';
import type { EarnTask } from '@/lib/schemas';
import styles from './EarnTaskCard.module.css';

interface EarnTaskCardProps {
  task: EarnTask;
  onClick: (task: EarnTask) => void;
  disabled?: boolean;
}

function getButtonLabel(task: EarnTask): string {
  if (task.status === 'completed' || task.status === 'claimed') {
    return '已完成';
  }

  if (task.status === 'locked') {
    return task.action.type === 'placeholder' ? '开发中' : '暂未开放';
  }

  return '立即领取';
}

export function EarnTaskCard({ task, onClick, disabled = false }: EarnTaskCardProps) {
  const actionDisabled =
    disabled || task.status === 'completed' || task.status === 'claimed';

  return (
    <Card as="article" className={styles.card}>
      <div className={styles.content}>
        <div>
          <h3 className={styles.title}>{task.title}</h3>
          <p className={styles.description}>{task.description}</p>
        </div>
        <div className={styles.meta}>
          <span className={styles.reward} aria-label={`奖励 ${task.reward_coins} 金币`}>
            +{task.reward_coins} 金币
          </span>
          {task.is_representative ? <span className={styles.badge}>代表任务</span> : null}
        </div>
      </div>
      <Button
        type="button"
        onClick={() => onClick(task)}
        disabled={actionDisabled}
        className={styles.action}
      >
        {getButtonLabel(task)}
      </Button>
    </Card>
  );
}
