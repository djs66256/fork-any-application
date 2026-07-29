import { Button } from '@/components/ui';
import styles from './EarnFeedbackToast.module.css';

interface EarnFeedbackToastProps {
  message: string | null;
  onDismiss: () => void;
}

export function EarnFeedbackToast({ message, onDismiss }: EarnFeedbackToastProps) {
  if (!message) {
    return null;
  }

  return (
    <div className={styles.toast} role="status" aria-live="polite">
      <span>{message}</span>
      <Button type="button" variant="ghost" size="sm" onClick={onDismiss}>
        知道了
      </Button>
    </div>
  );
}
