import { Card } from '@/components/ui';
import type { EarnTask } from '@/lib/schemas';
import { EarnTaskCard } from './EarnTaskCard';
import styles from './EarnNewUserTaskCard.module.css';

interface EarnNewUserTaskCardProps {
  task: EarnTask;
  onActionClick: (task: EarnTask) => void;
  disabled?: boolean;
}

export function EarnNewUserTaskCard({
  task,
  onActionClick,
  disabled = false,
}: EarnNewUserTaskCardProps) {
  return (
    <Card as="section" className={styles.section}>
      <div className={styles.header}>
        <h2 className={styles.title}>新手任务</h2>
        <p className={styles.description}>首屏代表任务，优先打通登录与播放闭环</p>
      </div>
      <EarnTaskCard task={task} onClick={onActionClick} disabled={disabled} />
    </Card>
  );
}
