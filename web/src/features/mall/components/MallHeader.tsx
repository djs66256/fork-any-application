import { Button } from '@/components/ui';
import styles from './MallHeader.module.css';

interface MallHeaderProps {
  onSearch: () => void;
  onCart: () => void;
}

function SearchIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className={styles.searchIconSvg}>
      <circle cx="11" cy="11" r="7.2" />
      <path d="M16.6 16.6 21 21" />
    </svg>
  );
}

function CartIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className={styles.cartIconSvg}>
      <path d="M2.5 3.5h2.4l1.5 10.6a1.4 1.4 0 0 0 1.4 1.2h9.3a1.4 1.4 0 0 0 1.3-1l2-7H6.6" />
      <circle cx="9.3" cy="19.2" r="1.4" />
      <circle cx="17.4" cy="19.2" r="1.4" />
    </svg>
  );
}

export function MallHeader({ onSearch, onCart }: MallHeaderProps) {
  return (
    <header className={styles.header}>
      <Button
        type="button"
        variant="ghost"
        className={styles.searchButton}
        onClick={onSearch}
        aria-label="打开商城搜索"
      >
        <span className={styles.searchIcon} aria-hidden="true">
          <SearchIcon />
        </span>
        <span className={styles.searchPlaceholder}>高级感纯欲套装辣妹风</span>
        <span className={styles.searchAction}>搜索</span>
      </Button>
      <Button
        type="button"
        variant="ghost"
        className={styles.cartButton}
        onClick={onCart}
        aria-label="打开购物车入口"
      >
        <CartIcon />
      </Button>
    </header>
  );
}
