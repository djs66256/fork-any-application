import { Button } from '@/components/ui';
import styles from './MallHeader.module.css';

interface MallHeaderProps {
  onSearch: () => void;
  onCart: () => void;
}

export function MallHeader({ onSearch, onCart }: MallHeaderProps) {
  return (
    <header className={styles.header}>
      <Button
        type="button"
        variant="secondary"
        className={styles.searchButton}
        onClick={onSearch}
        aria-label="打开商城搜索"
      >
        搜索商品
      </Button>
      <Button
        type="button"
        variant="ghost"
        className={styles.cartButton}
        onClick={onCart}
        aria-label="打开购物车入口"
      >
        购物车
      </Button>
    </header>
  );
}
