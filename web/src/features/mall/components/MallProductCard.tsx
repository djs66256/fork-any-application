import { Button, Card } from '@/components/ui';
import type { MallProduct } from '@/lib/schemas';
import { getMallProductVisual } from '@/features/mall/config/mall-seed';
import styles from './MallProductCard.module.css';

interface MallProductCardProps {
  product: MallProduct;
  onClick: (product: MallProduct) => void;
}

function formatPrice(price: number): string {
  const normalizedPrice = price.toFixed(1).replace(/\.0$/, '');
  return `¥${normalizedPrice}`;
}

export function MallProductCard({ product, onClick }: MallProductCardProps) {
  const visual = getMallProductVisual(product);

  return (
    <Card as="article" className={styles.card}>
      <Button
        type="button"
        variant="ghost"
        className={styles.button}
        onClick={() => onClick(product)}
        aria-label={`查看商品 ${product.title}`}
      >
        <div className={styles.imageWrap} style={{ ['--mall-image-ratio' as string]: visual.imageAspectRatio ?? '1' }}>
          {visual.isLive ? <span className={styles.liveBadge}>{visual.badge ?? '直播中'}</span> : null}
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={product.image_url}
            alt={product.title}
            className={styles.image}
            loading="lazy"
            decoding="async"
          />
        </div>
        <div className={styles.content}>
          <span className={styles.title}>{product.title}</span>
          {!visual.isLive && visual.coupon ? <span className={styles.coupon}>{visual.coupon}</span> : null}
          <div className={styles.priceRow}>
            <span className={styles.price}>{formatPrice(product.price)}</span>
            <span className={styles.priceSuffix}>起减价</span>
            {visual.originalPrice ? <span className={styles.originalPrice}>{visual.originalPrice}</span> : null}
          </div>
          {visual.isLive ? (
            <div className={styles.metaRow}>
              <span className={styles.shopName}>{visual.shopName}</span>
              <span className={styles.viewers}>{visual.viewers}</span>
            </div>
          ) : null}
          {!visual.isLive && product.tags.length > 0 ? (
            <span className={styles.tags} aria-label={`商品标签 ${product.tags.join('、')}`}>
              {product.tags.map((tag) => (
                <span key={tag} className={styles.tag}>
                  {tag}
                </span>
              ))}
            </span>
          ) : null}
        </div>
      </Button>
    </Card>
  );
}
