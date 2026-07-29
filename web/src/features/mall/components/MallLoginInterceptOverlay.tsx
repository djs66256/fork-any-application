import { useEffect } from 'react';
import { Button, Card } from '@/components/ui';
import type { MallProduct } from '@/lib/schemas';
import styles from './MallLoginInterceptOverlay.module.css';

interface MallLoginInterceptOverlayProps {
  visible: boolean;
  product: MallProduct | null;
  onContinueLogin: () => void;
  onCancel: () => void;
}

export function MallLoginInterceptOverlay({
  visible,
  product,
  onContinueLogin,
  onCancel,
}: MallLoginInterceptOverlayProps) {
  useEffect(() => {
    if (!visible) {
      return undefined;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onCancel();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [onCancel, visible]);

  if (!visible || !product) {
    return null;
  }

  return (
    <div className={styles.backdrop} role="presentation">
      <section
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="mall-login-intercept-title"
      >
        <Card as="div">
          <h2 id="mall-login-intercept-title" className={styles.title}>
            登录后可继续查看商品
          </h2>
          <p className={styles.description}>
            当前商品“{product.title}”需要登录后继续查看或购买。
          </p>
          <div className={styles.actions}>
            <Button type="button" variant="secondary" onClick={onCancel}>
              取消
            </Button>
            <Button type="button" onClick={onContinueLogin}>
              继续登录
            </Button>
          </div>
        </Card>
      </section>
    </div>
  );
}
