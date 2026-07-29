import { Button, Card } from '@/components/ui';
import type { MallShortcut } from '@/lib/schemas';
import styles from './MallShortcutGrid.module.css';

interface MallShortcutGridProps {
  shortcuts: MallShortcut[];
  onShortcutClick: (shortcut: MallShortcut) => void;
}

export function MallShortcutGrid({ shortcuts, onShortcutClick }: MallShortcutGridProps) {
  return (
    <section aria-labelledby="mall-shortcuts-heading" className={styles.section}>
      <div className={styles.sectionHeader}>
        <h2 id="mall-shortcuts-heading" className={styles.title}>
          快捷入口
        </h2>
      </div>
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
                {shortcut.icon}
              </span>
              <span className={styles.label}>{shortcut.title}</span>
            </Button>
          </Card>
        ))}
      </div>
    </section>
  );
}
