import { Button } from '@/components/ui';
import type { MallProduct } from '@/lib/schemas';
import { MallProductCard } from './MallProductCard';
import styles from './MallProductGrid.module.css';

interface MallProductGridProps {
  items: MallProduct[];
  hasNextPage: boolean;
  isLoading: boolean;
  isAppending: boolean;
  appendError: string | null;
  errorMessage: string | null;
  onProductClick: (product: MallProduct) => void;
  onRetryInitial: () => void;
  onLoadMore: () => void;
  onRetryAppend: () => void;
}

export function MallProductGrid({
  items,
  hasNextPage,
  isLoading,
  isAppending,
  appendError,
  errorMessage,
  onProductClick,
  onRetryInitial,
  onLoadMore,
  onRetryAppend,
}: MallProductGridProps) {
  return (
    <section aria-labelledby="mall-products-heading" className={styles.section}>
      <div className={styles.sectionHeader}>
        <h2 id="mall-products-heading" className={styles.title}>
          热门商品
        </h2>
      </div>

      {isLoading ? <p className={styles.status}>商品加载中...</p> : null}

      {!isLoading && errorMessage ? (
        <div className={styles.stateCard} role="alert">
          <p className={styles.status}>{errorMessage}</p>
          <Button type="button" variant="secondary" onClick={onRetryInitial}>
            重试
          </Button>
        </div>
      ) : null}

      {!isLoading && !errorMessage && items.length === 0 ? (
        <div className={styles.stateCard}>
          <p className={styles.status}>暂无商品，去看看其他活动吧。</p>
        </div>
      ) : null}

      {!errorMessage && items.length > 0 ? (
        <>
          <div className={styles.grid}>
            {items.map((product) => (
              <MallProductCard key={product.id} product={product} onClick={onProductClick} />
            ))}
          </div>
          <div className={styles.footer}>
            {appendError ? (
              <div className={styles.footerState} role="alert">
                <span>{appendError}</span>
                <Button type="button" variant="secondary" onClick={onRetryAppend}>
                  重试加载更多
                </Button>
              </div>
            ) : null}
            {isAppending ? <p className={styles.status}>正在加载更多...</p> : null}
            {!isAppending && !appendError && hasNextPage ? (
              <Button type="button" variant="secondary" onClick={onLoadMore}>
                加载更多
              </Button>
            ) : null}
            {!isAppending && !appendError && !hasNextPage ? (
              <p className={styles.status}>已经到底啦，去看看其他活动吧。</p>
            ) : null}
          </div>
        </>
      ) : null}
    </section>
  );
}
