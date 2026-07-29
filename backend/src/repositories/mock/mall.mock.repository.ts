import { MallProduct, MallProductSchema, MallProductsQuery } from '@/lib/schemas';
import { MallPaginatedResult, MallRepositoryInterface } from '@/repositories/interfaces/mall.repository.interface';

const DEFAULT_MALL_PRODUCTS: MallProduct[] = [
  {
    id: '650e8400-e29b-41d4-a716-446655440001',
    title: '轻奢真丝睡衣礼盒',
    image_url: 'https://example.com/mall/products/pajama-gift-box.jpg',
    price: 199,
    tags: ['热卖', '包邮'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440002',
    title: '云感记忆枕双只装',
    image_url: 'https://example.com/mall/products/memory-pillows.jpg',
    price: 159,
    tags: ['自营'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440003',
    title: '北欧极简小茶几',
    image_url: 'https://example.com/mall/products/side-table.jpg',
    price: 299,
    tags: ['新品'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440004',
    title: '家用便携投影仪',
    image_url: 'https://example.com/mall/products/projector.jpg',
    price: 899,
    tags: ['热卖', '补贴'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440005',
    title: '无线降噪蓝牙耳机',
    image_url: 'https://example.com/mall/products/headphones.jpg',
    price: 329,
    tags: ['爆款'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440006',
    title: '高硼硅冷水壶套装',
    image_url: 'https://example.com/mall/products/glass-pitcher.jpg',
    price: 79,
    tags: ['包邮'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440007',
    title: '轻量通勤双肩包',
    image_url: 'https://example.com/mall/products/backpack.jpg',
    price: 129,
    tags: ['新品'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440008',
    title: 'ins风香薰蜡烛礼盒',
    image_url: 'https://example.com/mall/products/candle-gift-box.jpg',
    price: 69,
    tags: ['礼盒'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440009',
    title: '家居全棉四件套',
    image_url: 'https://example.com/mall/products/bedding-set.jpg',
    price: 239,
    tags: ['热卖'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440010',
    title: '复古手冲咖啡壶',
    image_url: 'https://example.com/mall/products/coffee-pot.jpg',
    price: 119,
    tags: ['自营'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440011',
    title: '便携榨汁杯',
    image_url: 'https://example.com/mall/products/juicer-cup.jpg',
    price: 99,
    tags: ['包邮'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440012',
    title: '珐琅汤锅 24cm',
    image_url: 'https://example.com/mall/products/casserole.jpg',
    price: 269,
    tags: ['新品'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440013',
    title: '智能恒温杯垫',
    image_url: 'https://example.com/mall/products/mug-warmer.jpg',
    price: 89,
    tags: ['爆款'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440014',
    title: '厨房刀具五件套',
    image_url: 'https://example.com/mall/products/knife-set.jpg',
    price: 179,
    tags: ['自营'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440015',
    title: '儿童积木收纳桌',
    image_url: 'https://example.com/mall/products/storage-table.jpg',
    price: 359,
    tags: ['补贴'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440016',
    title: '户外折叠露营椅',
    image_url: 'https://example.com/mall/products/camping-chair.jpg',
    price: 149,
    tags: ['热卖'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440017',
    title: '家用筋膜枪',
    image_url: 'https://example.com/mall/products/massage-gun.jpg',
    price: 259,
    tags: ['爆款', '补贴'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440018',
    title: '桌面护眼台灯',
    image_url: 'https://example.com/mall/products/desk-lamp.jpg',
    price: 139,
    tags: ['包邮'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440019',
    title: '女士羊绒围巾',
    image_url: 'https://example.com/mall/products/scarf.jpg',
    price: 219,
    tags: ['礼盒'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440020',
    title: '空气循环扇',
    image_url: 'https://example.com/mall/products/circulation-fan.jpg',
    price: 309,
    tags: ['新品', '补贴'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440021',
    title: '宠物自动喂食器',
    image_url: 'https://example.com/mall/products/pet-feeder.jpg',
    price: 399,
    tags: ['自营'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440022',
    title: '可折叠收纳箱三件套',
    image_url: 'https://example.com/mall/products/storage-boxes.jpg',
    price: 109,
    tags: ['包邮'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440023',
    title: '电动牙刷礼盒',
    image_url: 'https://example.com/mall/products/toothbrush-set.jpg',
    price: 279,
    tags: ['热卖'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440024',
    title: '香氛洗护旅行装',
    image_url: 'https://example.com/mall/products/travel-kit.jpg',
    price: 59,
    tags: ['新品'],
  },
  {
    id: '650e8400-e29b-41d4-a716-446655440025',
    title: '人体工学脚踏板',
    image_url: 'https://example.com/mall/products/footrest.jpg',
    price: 169,
    tags: ['自营'],
  },
].map((product) => MallProductSchema.parse(product));

function cloneMallProduct(product: MallProduct): MallProduct {
  return {
    ...product,
    tags: [...product.tags],
  };
}

function paginateProducts(
  items: MallProduct[],
  params: MallProductsQuery,
): MallPaginatedResult<MallProduct> {
  const total = items.length;
  const totalPages = total === 0 ? 0 : Math.ceil(total / params.pageSize);
  const start = (params.page - 1) * params.pageSize;

  return {
    data: items.slice(start, start + params.pageSize).map(cloneMallProduct),
    pagination: {
      page: params.page,
      page_size: params.pageSize,
      total,
      total_pages: totalPages,
    },
  };
}

export class MallMockRepository implements MallRepositoryInterface {
  private readonly products: MallProduct[];

  constructor(initialProducts: MallProduct[] = DEFAULT_MALL_PRODUCTS) {
    this.products = initialProducts.map(cloneMallProduct);
  }

  async listProducts(params: MallProductsQuery): Promise<MallPaginatedResult<MallProduct>> {
    return paginateProducts(this.products, params);
  }
}
