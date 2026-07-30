import {
  MallBannerSchema,
  MallProductSchema,
  MallShortcutSchema,
  type MallBanner,
  type MallProduct,
  type MallShortcut,
} from '@/lib/schemas';

export interface MallProductVisualPreset {
  badge?: string;
  coupon?: string;
  originalPrice?: string;
  viewers?: string;
  shopName?: string;
  isLive?: boolean;
  imageAspectRatio?: string;
}

export interface MallBannerVisualPreset {
  eyebrow: string;
  title: string;
  subtitle: string;
  cta?: string;
}

function createSvgDataUrl(svg: string): string {
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function createProductArtwork(kind: string): string {
  if (kind === 'garlic-press') {
    return createSvgDataUrl(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 780">
        <defs>
          <linearGradient id="bg" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stop-color="#e0b384" />
            <stop offset="100%" stop-color="#c68c57" />
          </linearGradient>
          <linearGradient id="metal" x1="0" x2="1" y1="0" y2="1">
            <stop offset="0%" stop-color="#373a3f" />
            <stop offset="100%" stop-color="#0d1013" />
          </linearGradient>
        </defs>
        <rect width="640" height="780" fill="url(#bg)" />
        <rect y="528" width="640" height="252" fill="#ab6f3d" opacity="0.95" />
        <rect x="88" y="194" width="468" height="170" rx="84" transform="rotate(18 322 279)" fill="url(#metal)" />
        <path d="M246 332c36 14 72 24 132 36 27 5 49 26 56 53l14 52c7 26-12 51-40 51H266c-26 0-49-17-57-41l-25-77c-9-30 18-53 62-38z" fill="#eaedf1" />
        <g fill="#f7f1d9">
          <circle cx="238" cy="480" r="10" />
          <circle cx="258" cy="495" r="9" />
          <circle cx="286" cy="491" r="10" />
          <circle cx="307" cy="508" r="9" />
          <circle cx="330" cy="493" r="11" />
          <circle cx="352" cy="510" r="9" />
          <circle cx="374" cy="489" r="10" />
        </g>
        <rect x="30" y="26" width="156" height="54" rx="27" fill="#ff154f" />
        <text x="108" y="61" text-anchor="middle" fill="#ffffff" font-size="28" font-family="Arial" font-weight="700">直播中</text>
        <text x="40" y="118" fill="#fff2cb" font-size="46" font-family="Arial" font-weight="700">切蒜不用刀</text>
        <text x="40" y="162" fill="#fff8e6" font-size="24" font-family="Arial">厨房爆款好物</text>
      </svg>
    `);
  }

  if (kind === 'ootd') {
    return createSvgDataUrl(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 780">
        <rect width="640" height="780" fill="#e5c7cb" />
        <circle cx="520" cy="148" r="112" fill="#f1d9de" opacity="0.72" />
        <circle cx="166" cy="162" r="84" fill="#f8dde0" opacity="0.82" />
        <path d="M270 162c40 0 72 32 72 72s-32 72-72 72-72-32-72-72 32-72 72-72z" fill="#f2cec0" />
        <rect x="226" y="294" width="88" height="112" rx="34" fill="#191d25" />
        <path d="M174 320h188l31 96c7 22-10 44-33 44H204c-23 0-40-22-33-44l3-10-42 58c-12 17-35 20-51 7-16-13-19-37-6-53l76-98c10-13 24-20 40-20z" fill="#181c24" />
        <path d="M128 566l118-126 88 46-91 197h-40l-35-119-27 119h-43z" fill="#0d0f14" />
        <path d="M164 320h106l-22 118-120-7z" fill="#f1c2cb" />
        <path d="M270 320h100l76 119-128 2z" fill="#f1c2cb" />
        <path d="M214 438h174l-26 82H198z" fill="#2d2235" />
        <path d="M450 456c30 0 54 24 54 54v112h-97l-18-115c-4-24 14-47 38-50l23-1z" fill="#15171d" />
        <text x="414" y="114" text-anchor="middle" fill="#5d4a60" font-size="58" font-family="Georgia" font-style="italic">NEEDARNA</text>
        <text x="468" y="148" text-anchor="middle" fill="#5d4a60" font-size="20" font-family="Arial">心动穿搭</text>
      </svg>
    `);
  }

  if (kind === 'ring') {
    return createSvgDataUrl(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 780">
        <rect width="640" height="780" fill="#faf9f8" />
        <ellipse cx="320" cy="630" rx="170" ry="34" fill="#e7e2dd" />
        <path d="M216 296c20-73 86-124 164-124 74 0 140 44 166 111 31 79 5 167-65 220-71 54-190 66-273 20-77-42-112-131-89-210 21-72 83-136 154-155" fill="none" stroke="#b7bdc4" stroke-width="58" />
        <path d="M218 296c19-67 81-114 154-114 68 0 129 40 153 102 27 69 5 147-55 194-65 50-172 60-248 18-69-39-100-118-80-191 19-67 77-127 143-145z" fill="none" stroke="#f7f7f9" stroke-width="24" />
        <g fill="#ffffff" stroke="#b8bfc6" stroke-width="8">
          <polygon points="314,214 334,246 372,252 344,278 350,316 314,298 279,316 284,278 256,252 294,246" />
          <circle cx="227" cy="340" r="20" />
          <circle cx="276" cy="300" r="18" />
          <circle cx="381" cy="298" r="18" />
          <circle cx="431" cy="338" r="20" />
        </g>
        <text x="320" y="712" text-anchor="middle" fill="#282b31" font-size="26" font-family="Arial" font-weight="700">LONG&amp;SHE</text>
      </svg>
    `);
  }

  if (kind === 'cheongsam') {
    return createSvgDataUrl(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 780">
        <defs>
          <linearGradient id="bg" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stop-color="#f0e7dd" />
            <stop offset="100%" stop-color="#f7efe8" />
          </linearGradient>
          <linearGradient id="dress" x1="0" x2="1" y1="0" y2="1">
            <stop offset="0%" stop-color="#f2b56f" />
            <stop offset="100%" stop-color="#d55f42" />
          </linearGradient>
        </defs>
        <rect width="640" height="780" fill="url(#bg)" />
        <rect x="12" y="22" width="164" height="56" rx="28" fill="#ff124f" />
        <text x="94" y="59" text-anchor="middle" fill="#ffffff" font-size="30" font-family="Arial" font-weight="700">直播中</text>
        <ellipse cx="320" cy="744" rx="164" ry="28" fill="#e8dbd1" />
        <circle cx="320" cy="184" r="72" fill="#f5d8c2" />
        <path d="M254 240h132l86 66-35 51-42-25v299H245V332l-42 27-34-53z" fill="url(#dress)" />
        <path d="M240 268c34 40 56 58 144 70v34c-66 4-120-21-164-74z" fill="#f7d4ad" opacity="0.65" />
        <path d="M370 304c28 28 48 42 96 51l-16 26c-36-11-69-31-104-64z" fill="#f7d4ad" opacity="0.65" />
        <path d="M287 410c22 8 42 12 64 12 20 0 42-4 60-12" fill="none" stroke="#d6483d" stroke-width="10" stroke-linecap="round" />
        <rect x="270" y="636" width="26" height="88" rx="13" fill="#f5d8c2" />
        <rect x="346" y="636" width="26" height="88" rx="13" fill="#f5d8c2" />
      </svg>
    `);
  }

  if (kind === 'necklace') {
    return createSvgDataUrl(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 780">
        <rect width="640" height="780" fill="#f7f7f5" />
        <path d="M202 142c36 128 56 200 118 300 64-100 86-176 118-300" fill="none" stroke="#c1c6cb" stroke-width="12" stroke-linecap="round" />
        <g fill="#dde1e6" stroke="#adb3ba" stroke-width="8">
          <path d="M320 332l36 34-12 50 44 28-12 44-56 2-24 52-24-52-56-2-12-44 44-28-12-50z" />
          <circle cx="320" cy="312" r="22" fill="#ffffff" />
        </g>
        <text x="320" y="688" text-anchor="middle" fill="#2b2f34" font-size="42" font-family="Arial">925银复古十字项链</text>
      </svg>
    `);
  }

  return createSvgDataUrl(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 780">
      <defs>
        <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
          <stop offset="0%" stop-color="#1f4557" />
          <stop offset="100%" stop-color="#648c9e" />
        </linearGradient>
      </defs>
      <rect width="640" height="780" fill="url(#bg)" />
      <rect x="12" y="22" width="164" height="56" rx="28" fill="#ff124f" />
      <text x="94" y="59" text-anchor="middle" fill="#ffffff" font-size="30" font-family="Arial" font-weight="700">直播中</text>
      <rect x="432" y="112" width="98" height="246" rx="22" transform="rotate(-17 432 112)" fill="#ffffff" />
      <rect x="446" y="168" width="70" height="92" rx="14" transform="rotate(-17 446 168)" fill="#f24a3d" />
      <path d="M456 316l70-22 22 63c6 18-4 38-22 44l-4 1c-18 6-38-4-44-22z" fill="#e8edf1" />
      <path d="M418 392c18 20 28 42 28 68 0 36-24 66-60 66s-60-30-60-66c0-29 12-51 38-70z" fill="#ecf2f6" opacity="0.92" />
      <path d="M370 466c0 26 22 48 48 48s48-22 48-48c0-20-8-36-21-52-3 20-15 39-40 39-13 0-25 6-35 13z" fill="#b9d6e6" />
      <text x="320" y="690" text-anchor="middle" fill="#f4f7fa" font-size="42" font-family="Arial">强力粘胶防水修补</text>
    </svg>
  `);
}

function createBannerArtwork(kind: string, eyebrow: string, title: string, subtitle: string, cta?: string): string {
  if (kind === 'summer') {
    return createSvgDataUrl(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 380 160">
        <defs>
          <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
            <stop offset="0%" stop-color="#66dbe7" />
            <stop offset="100%" stop-color="#12a9c0" />
          </linearGradient>
        </defs>
        <rect width="380" height="160" rx="24" fill="url(#bg)" />
        <g fill="#ffffff" opacity="0.24">
          <circle cx="48" cy="36" r="4" /><circle cx="92" cy="28" r="4" /><circle cx="126" cy="54" r="4" />
          <circle cx="314" cy="24" r="5" /><circle cx="336" cy="66" r="4" /><circle cx="272" cy="46" r="4" />
        </g>
        <text x="30" y="58" fill="#ffffff" font-size="20" font-family="Arial" font-weight="600">${eyebrow}</text>
        <text x="30" y="98" fill="#ffffff" font-size="42" font-family="Arial" font-weight="700">${title}</text>
        <text x="30" y="138" fill="#dcfff6" font-size="28" font-family="Arial">${subtitle}</text>
      </svg>
    `);
  }

  if (kind === 'travel') {
    return createSvgDataUrl(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 380 160">
        <rect width="380" height="160" rx="24" fill="#fff6ef" />
        <path d="M134 0h76c34 46 50 80 50 160h-86c6-16 10-42 10-80 0-32-16-58-50-80z" fill="#ffe4d4" />
        <text x="34" y="68" fill="#ff5e2f" font-size="40" font-family="Arial" font-weight="700">${title}</text>
        <text x="34" y="114" fill="#ff5e2f" font-size="36" font-family="Arial" font-weight="700">${subtitle}</text>
        <rect x="220" y="86" width="114" height="44" rx="16" fill="#ff6c2e" />
        <text x="277" y="116" text-anchor="middle" fill="#ffffff" font-size="28" font-family="Arial" font-weight="700">${cta ?? ''}</text>
        <text x="278" y="64" text-anchor="middle" fill="#ff6c2e" font-size="30" font-family="Arial" font-weight="700">一站囤齐</text>
      </svg>
    `);
  }

  return createSvgDataUrl(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 380 160">
      <rect width="380" height="160" rx="24" fill="#74dced" />
      <rect x="240" y="24" width="96" height="96" rx="20" fill="#effbff" opacity="0.92" />
      <rect x="262" y="40" width="32" height="56" rx="8" transform="rotate(12 262 40)" fill="#ff8447" />
      <path d="M296 44h48c10 0 18 8 18 18v44c0 10-8 18-18 18h-52l4-80z" fill="#9fdcf1" />
      <path d="M308 28c18 0 32 14 32 32" fill="none" stroke="#ffd768" stroke-width="6" stroke-linecap="round" />
      <text x="30" y="116" fill="#ff5f2f" font-size="32" font-family="Arial" font-weight="700">${title}</text>
      <text x="212" y="116" fill="#ff5f2f" font-size="32" font-family="Arial" font-weight="700">${subtitle}</text>
      <text x="30" y="52" fill="#ffffff" font-size="20" font-family="Arial" font-weight="600">${eyebrow}</text>
    </svg>
  `);
}

const bannerSeed = [
  {
    id: 'mall-banner-summer',
    image_url: createBannerArtwork('summer', '红果', '夏日市集', '爆款低价'),
    target_type: 'search',
    target_value: '',
    sort_order: 0,
  },
  {
    id: 'mall-banner-travel',
    image_url: createBannerArtwork('travel', '旅途', '旅途', '超省心', '去下单'),
    target_type: 'product',
    target_value: '550e8400-e29b-41d4-a716-446655440106',
    sort_order: 1,
  },
  {
    id: 'mall-banner-holiday',
    image_url: createBannerArtwork('holiday', '暑期精选', '精选好物', '暑假出游'),
    target_type: 'product',
    target_value: '550e8400-e29b-41d4-a716-446655440102',
    sort_order: 2,
  },
] as const;

const shortcutSeed = [
  {
    key: 'orders',
    title: '我的订单',
    icon: 'orders',
    behavior: 'placeholder-feedback',
  },
  {
    key: 'coupon',
    title: '卡券/红包',
    icon: 'coupon',
    behavior: 'placeholder-feedback',
  },
  {
    key: 'wallet',
    title: '我的钱包',
    icon: 'wallet',
    behavior: 'placeholder-feedback',
  },
  {
    key: 'same-style',
    title: '短剧同款',
    icon: 'same-style',
    behavior: 'placeholder-feedback',
  },
  {
    key: 'subsidy',
    title: '国家补贴',
    icon: 'subsidy',
    behavior: 'placeholder-feedback',
  },
] as const;

const productSeed = [
  {
    id: '550e8400-e29b-41d4-a716-446655440101',
    title: '多功能不锈钢压蒜器 厨房省力切蒜神器',
    image_url: createProductArtwork('garlic-press'),
    price: 39.9,
    tags: ['直播抢购'],
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440102',
    title: '学院风辣妹穿搭套装 秋冬显瘦一整套',
    image_url: createProductArtwork('ootd'),
    price: 29.9,
    tags: ['学院风'],
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440103',
    title: '龙蛇925银戒指 S925银 轻奢礼盒款',
    image_url: createProductArtwork('ring'),
    price: 37.9,
    tags: ['礼盒装'],
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440104',
    title: '新中式提花上衣 显白亮肤薄款通勤装',
    image_url: createProductArtwork('cheongsam'),
    price: 59.9,
    tags: ['新中式'],
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440105',
    title: '龙蛇坠链均925银 复古十字项链',
    image_url: createProductArtwork('necklace'),
    price: 19.9,
    tags: ['复古银饰'],
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440106',
    title: '强力防水修补胶 家居缝隙速干粘接',
    image_url: createProductArtwork('glue'),
    price: 23.9,
    tags: ['居家修补'],
  },
] as const;

export const mallBanners: MallBanner[] = bannerSeed
  .map((banner) => MallBannerSchema.parse(banner))
  .sort((left, right) => left.sort_order - right.sort_order);

export const mallShortcuts: MallShortcut[] = shortcutSeed.map((shortcut) =>
  MallShortcutSchema.parse(shortcut),
);

export const mallStableProducts: MallProduct[] = productSeed.map((product) =>
  MallProductSchema.parse(product),
);

const stableProductIdSet = new Set(mallStableProducts.map((product) => product.id));

const productVisualSeed: Record<string, MallProductVisualPreset> = {
  '550e8400-e29b-41d4-a716-446655440101': {
    badge: '直播中',
    viewers: '511人逛过',
    shopName: '鑫鑫',
    isLive: true,
    imageAspectRatio: '0.8',
  },
  '550e8400-e29b-41d4-a716-446655440102': {
    coupon: '店铺新人减10元',
    originalPrice: '3698人逛过',
    imageAspectRatio: '0.98',
  },
  '550e8400-e29b-41d4-a716-446655440103': {
    coupon: '立减价',
    originalPrice: '5138人逛过',
    imageAspectRatio: '1.16',
  },
  '550e8400-e29b-41d4-a716-446655440104': {
    badge: '直播中',
    viewers: '616人逛过',
    shopName: '锦绣工厂店',
    isLive: true,
    imageAspectRatio: '1.28',
  },
  '550e8400-e29b-41d4-a716-446655440105': {
    coupon: '立减价',
    originalPrice: '1万人逛过',
    imageAspectRatio: '1.08',
  },
  '550e8400-e29b-41d4-a716-446655440106': {
    badge: '直播中',
    viewers: '892人逛过',
    shopName: '妙妙好货',
    isLive: true,
    imageAspectRatio: '1.22',
  },
};

const bannerVisualSeed: Record<string, MallBannerVisualPreset> = {
  'mall-banner-summer': {
    eyebrow: '红果',
    title: '夏日市集',
    subtitle: '爆款低价',
  },
  'mall-banner-travel': {
    eyebrow: '旅途',
    title: '旅途',
    subtitle: '超省心',
    cta: '去下单',
  },
  'mall-banner-holiday': {
    eyebrow: '暑期精选',
    title: '精选好物',
    subtitle: '暑假出游',
  },
};

function createFallbackProductVisual(product: MallProduct): MallProductVisualPreset {
  return {
    coupon: product.tags[0] ? `${product.tags[0]}好价` : '商城精选',
    originalPrice: `${Math.max(1200, Math.round(product.price * 118))}人逛过`,
    imageAspectRatio: '1.04',
  };
}

export function isStableMallProduct(productId: string): boolean {
  return stableProductIdSet.has(productId);
}

export function buildMallStableFeed(remoteProducts: MallProduct[]): MallProduct[] {
  const filteredRemoteProducts = remoteProducts.filter((product) => !stableProductIdSet.has(product.id));
  return [...mallStableProducts, ...filteredRemoteProducts];
}

export function mergeMallProducts(currentItems: MallProduct[], nextItems: MallProduct[]): MallProduct[] {
  const seenIds = new Set(currentItems.map((item) => item.id));
  const mergedItems = [...currentItems];

  nextItems.forEach((item) => {
    if (!seenIds.has(item.id)) {
      seenIds.add(item.id);
      mergedItems.push(item);
    }
  });

  return mergedItems;
}

export function getMallProductVisual(product: MallProduct): MallProductVisualPreset {
  return productVisualSeed[product.id] ?? createFallbackProductVisual(product);
}

export function getMallBannerVisual(bannerId: string): MallBannerVisualPreset | null {
  return bannerVisualSeed[bannerId] ?? null;
}
