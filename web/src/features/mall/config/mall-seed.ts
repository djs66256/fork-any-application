import { MallBannerSchema, MallShortcutSchema, type MallBanner, type MallShortcut } from '@/lib/schemas';

const bannerSeed = [
  {
    id: 'mall-banner-summer',
    image_url: 'https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=1200&q=80',
    target_type: 'search',
    target_value: '',
    sort_order: 0,
  },
  {
    id: 'mall-banner-newcomer',
    image_url: 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1200&q=80',
    target_type: 'product',
    target_value: '550e8400-e29b-41d4-a716-446655440101',
    sort_order: 1,
  },
] as const;

const shortcutSeed = [
  {
    key: 'orders',
    title: '我的订单',
    icon: '📦',
    behavior: 'placeholder-feedback',
  },
  {
    key: 'coupon',
    title: '优惠券红包',
    icon: '🎁',
    behavior: 'placeholder-feedback',
  },
  {
    key: 'wallet',
    title: '我的钱包',
    icon: '💳',
    behavior: 'placeholder-feedback',
  },
  {
    key: 'same-style',
    title: '短剧同款',
    icon: '🎬',
    behavior: 'placeholder-feedback',
  },
  {
    key: 'subsidy',
    title: '国补专区',
    icon: '⭐',
    behavior: 'placeholder-feedback',
  },
] as const;

export const mallBanners: MallBanner[] = bannerSeed
  .map((banner) => MallBannerSchema.parse(banner))
  .sort((left, right) => left.sort_order - right.sort_order);

export const mallShortcuts: MallShortcut[] = shortcutSeed.map((shortcut) =>
  MallShortcutSchema.parse(shortcut),
);
