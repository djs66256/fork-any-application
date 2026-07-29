import { Card } from '@/components/ui';
import type { EarnTask } from '@/lib/schemas';
import { EarnTaskCard } from './EarnTaskCard';
import styles from './EarnCashTaskSection.module.css';

interface EarnCashTaskSectionProps {
  tasks: EarnTask[];
  onTaskClick: (task: EarnTask) => void;
  disabled?: boolean;
}

export function EarnCashTaskSection({
  tasks,
  onTaskClick,
  disabled = false,
}: EarnCashTaskSectionProps) {
  return (
    <Card as="section" className={styles.section}>
      <div className={styles.header}>
        <h2 className={styles.title}>现金任务列表</h2>
        <p className={styles.description}>仅一个代表任务接入真实播放闭环，其余任务保留受控反馈</p>
      </div>
      {tasks.length ? (
        <div className={styles.list}>
          {tasks.map((task) => (
            <EarnTaskCard key={task.id} task={task} onClick={onTaskClick} disabled={disabled} />
          ))}
        </div>
      ) : (
        <div className={styles.empty}>
          <p>暂无现金任务，去看看上方福利和新手任务吧。</p>
        </div>
      )}
    </Card>
  );
}
