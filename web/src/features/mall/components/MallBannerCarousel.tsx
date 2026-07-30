import { Button, Card } from '@/components/ui';
import type { MallBanner } from '@/lib/schemas';
import { getMallBannerVisual } from '@/features/mall/config/mall-seed';
import styles from './MallBannerCarousel.module.css';

interface MallBannerCarouselProps {
  banners: MallBanner[];
  onBannerClick: (banner: MallBanner) => void;
}

export function MallBannerCarousel({ banners, onBannerClick }: MallBannerCarouselProps) {
  if (banners.length === 0) {
    return null;
  }

  return (
    <section aria-labelledby="mall-banners-heading" className={styles.section}>
      <h2 id="mall-banners-heading" className={styles.title}>
        活动横幅
      </h2>
      <div className={styles.list}>
        {banners.map((banner) => {
          const visual = getMallBannerVisual(banner.id);

          return (
            <Card key={banner.id} className={styles.card}>
              <Button
                type="button"
                variant="ghost"
                className={styles.button}
                onClick={() => onBannerClick(banner)}
                aria-label={`活动横幅 ${banner.id}`}
              >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={banner.image_url}
                  alt={`${visual?.title ?? '商城'}活动横幅`}
                  className={styles.image}
                  loading="lazy"
                  decoding="async"
                />
              </Button>
            </Card>
          );
        })}
      </div>
    </section>
  );
}
