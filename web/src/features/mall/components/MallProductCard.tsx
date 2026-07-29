import { Button, Card } from '@/components/ui';
import type { MallProduct } from '@/lib/schemas';
import styles from './MallProductCard.module.css';

interface MallProductCardProps {
  product: MallProduct;
  onClick: (product: MallProduct) => void;
}

function formatPrice(price: number): string {
  return `¥${price.toFixed(2)}`;
}

export function MallProductCard({ product, onClick }: MallProductCardProps) {
  return (
    <Card as="article" className={styles.card}>
      <Button
        type="button"
        variant="ghost"
        className={styles.button}
        onClick={() => onClick(product)}
        aria-label={`查看商品 ${product.title}`}
      >
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={product.image_url}
          alt={product.title}
          className={styles.image}
          loading="lazy"
          decoding="async"
        />
        <span className={styles.title}>{product.title}</span>
        <span className={styles.price}>{formatPrice(product.price)}</span>
        {product.tags.length > 0 ? (
          <span className={styles.tags} aria-label={`商品标签 ${product.tags.join('、')}`}>
            {product.tags.map((tag) => (
              <span key={tag} className={styles.tag}>
                {tag}
              </span>
            ))}
          </span>
        ) : null}
      </Button>
    </Card>
  );
}
