import { Button, Card } from '@/components/ui';
import type { MallShortcut } from '@/lib/schemas';
import styles from './MallShortcutGrid.module.css';

interface MallShortcutGridProps {
  shortcuts: MallShortcut[];
  onShortcutClick: (shortcut: MallShortcut) => void;
}

function ShortcutIcon({ type }: { type: MallShortcut['icon'] }) {
  switch (type) {
    case 'orders':
      return (
        <svg viewBox="0 0 28 28" aria-hidden="true" className={styles.iconSvg}>
          <rect x="6" y="4.5" width="16" height="19" rx="3.8" />
          <path d="M10.5 3.5h7" />
          <path d="M10 11h8" />
          <path d="M10 15h8" />
        </svg>
      );
    case 'coupon':
      return (
        <svg viewBox="0 0 28 28" aria-hidden="true" className={styles.iconSvg}>
          <path d="M7 8.5h14a2.5 2.5 0 0 1 2.5 2.5 2.4 2.4 0 0 0 0 6A2.5 2.5 0 0 1 21 19.5H7a2.5 2.5 0 0 1-2.5-2.5 2.4 2.4 0 0 0 0-6A2.5 2.5 0 0 1 7 8.5Z" />
          <path d="M14 9.5v9" />
        </svg>
      );
    case 'wallet':
      return (
        <svg viewBox="0 0 28 28" aria-hidden="true" className={styles.iconSvg}>
          <path d="M5 9.5h18v11a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3z" />
          <path d="M8.5 9.5V7.8A2.8 2.8 0 0 1 11.3 5h8.2" />
          <circle cx="19.6" cy="16.4" r="1.2" />
        </svg>
      );
    case 'same-style':
      return (
        <svg viewBox="0 0 28 28" aria-hidden="true" className={styles.iconSvg}>
          <path d="M8.5 9.5h11a3.5 3.5 0 0 1 3.5 3.5v7.5H9.5A3.5 3.5 0 0 1 6 17V12a2.5 2.5 0 0 1 2.5-2.5Z" />
          <path d="M10 9.5V8.1A3.1 3.1 0 0 1 13.1 5h1.8A3.1 3.1 0 0 1 18 8.1v1.4" />
          <circle cx="14" cy="15.2" r="2.2" />
        </svg>
      );
    default:
      return (
        <svg viewBox="0 0 28 28" aria-hidden="true" className={styles.iconSvg}>
          <circle cx="14" cy="14" r="9.2" />
          <path d="M12 9.6h4.8v8.8H12z" />
          <path d="M10 13.9h8" />
        </svg>
      );
  }
}

export function MallShortcutGrid({ shortcuts, onShortcutClick }: MallShortcutGridProps) {
  return (
    <section aria-labelledby="mall-shortcuts-heading" className={styles.section}>
      <h2 id="mall-shortcuts-heading" className={styles.title}>
        快捷入口
      </h2>
      <div className={styles.panel}>
        <div className={styles.grid}>
          {shortcuts.map((shortcut) => (
            <Card key={shortcut.key} className={styles.card}>
              <Button
                type="button"
                variant="ghost"
                className={styles.button}
                onClick={() => onShortcutClick(shortcut)}
                aria-label={shortcut.title}
              >
                <span className={styles.icon} aria-hidden="true">
                  <ShortcutIcon type={shortcut.icon} />
                </span>
                <span className={styles.label}>{shortcut.title}</span>
              </Button>
            </Card>
          ))}
        </div>
        <div className={styles.pagination} aria-hidden="true">
          <span className={`${styles.dot} ${styles.dotActive}`} />
          <span className={styles.dot} />
        </div>
      </div>
    </section>
  );
}
