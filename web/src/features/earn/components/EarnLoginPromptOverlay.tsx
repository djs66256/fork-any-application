import { Button, Card } from '@/components/ui';
import type { EarnTask } from '@/lib/schemas';
import styles from './EarnLoginPromptOverlay.module.css';

interface EarnLoginPromptOverlayProps {
  visible: boolean;
  task: EarnTask | null;
  onContinue: () => void;
  onCancel: () => void;
}

export function EarnLoginPromptOverlay({
  visible,
  task,
  onContinue,
  onCancel,
}: EarnLoginPromptOverlayProps) {
  if (!visible) {
    return null;
  }

  return (
    <div className={styles.backdrop} role="presentation">
      <Card as="section" className={styles.dialog}>
        <h2 className={styles.title}>登录后可继续领取任务奖励</h2>
        <p className={styles.description}>
          {task ? `当前任务：${task.title}` : '登录后可同步任务奖励与收益记录。'}
        </p>
        <div className={styles.actions}>
          <Button type="button" variant="secondary" onClick={onCancel}>
            先看看
          </Button>
          <Button type="button" onClick={onContinue}>
            继续登录
          </Button>
        </div>
      </Card>
    </div>
  );
}
