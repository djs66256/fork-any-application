import { useEffect } from 'react';
import { Button } from '@/components/ui';
import type { MallProduct } from '@/lib/schemas';
import styles from './MallLoginInterceptOverlay.module.css';

interface MallLoginInterceptOverlayProps {
  visible: boolean;
  product: MallProduct | null;
  onContinueLogin: () => void;
  onCancel: () => void;
}

function DouyinIcon() {
  return (
    <svg viewBox="0 0 44 44" aria-hidden="true" className={styles.douyinIcon}>
      <circle cx="22" cy="22" r="21" fill="#ffffff" stroke="#ededf0" />
      <path d="M24.7 12.2c1.2 2.8 3.2 4.8 5.7 5.6v4.5a10.7 10.7 0 0 1-5.3-1.8v8.3a7.9 7.9 0 1 1-7.9-7.9c0.5 0 1 0 1.5 0.1v4.5a3.7 3.7 0 1 0 2.2 3.4V12.2Z" />
    </svg>
  );
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
        <div className={styles.panel}>
          <div className={styles.panelGlow} aria-hidden="true" />
          <div className={styles.sheetHandle} aria-hidden="true" />
          <h2 id="mall-login-intercept-title" className={styles.title}>
            完成抖音登录抢购超值好物
          </h2>
          <p className={styles.productHint}>当前商品“{product.title}”登录后可继续查看与购买。</p>
          <label className={styles.phoneField}>
            <span className={styles.srOnly}>手机号输入框</span>
            <input type="tel" placeholder="请输入您的手机号" readOnly value="" aria-label="请输入您的手机号" />
          </label>
          <Button type="button" className={styles.verifyButton} aria-label="获取验证码" disabled>
            获取验证码
          </Button>
          <label className={styles.agreementRow}>
            <input type="checkbox" aria-label="已阅读并同意协议" />
            <span>
              已阅读并同意 <button type="button">用户协议</button> 和 <button type="button">隐私政策</button> 以及 <button type="button">运营商服务协议</button>
            </span>
          </label>
          <Button type="button" className={styles.loginButton} onClick={onContinueLogin} aria-label="继续登录">
            继续登录
          </Button>
          <button type="button" className={styles.closeButton} onClick={onCancel} aria-label="关闭登录拦截">
            ×
          </button>
          <div className={styles.brandIcon} aria-hidden="true">
            <DouyinIcon />
          </div>
        </div>
      </section>
    </div>
  );
}
