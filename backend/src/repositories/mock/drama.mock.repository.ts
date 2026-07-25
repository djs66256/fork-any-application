import { Drama, DramaSchema } from '@/lib/schemas';
import { DramaRepositoryInterface, PaginationParams, PaginatedResult } from '@/repositories/interfaces/drama.repository.interface';

const HOMEPAGE_DRAMAS: Drama[] = [
  {
    id: '550e8400-e29b-41d4-a716-446655440001',
    title: '逆袭归来后我成了豪门团宠',
    description: '落魄千金重回豪门，在误会与守护中逆风翻盘。',
    cover_url: 'https://example.com/dramas/001.jpg',
    category: '都市',
    episode_count: 68,
    tags: ['逆袭', '豪门'],
    rating: 8.9,
    created_at: '2026-07-25T00:00:00Z',
    updated_at: '2026-07-25T00:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440002',
    title: '离婚后前夫跪求复合',
    description: '一纸离婚协议后，前任开始漫长追妻之路。',
    cover_url: 'https://example.com/dramas/002.jpg',
    category: '情感',
    episode_count: 56,
    tags: ['追妻', '甜宠'],
    rating: 8.4,
    created_at: '2026-07-24T23:00:00Z',
    updated_at: '2026-07-24T23:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440003',
    title: '开局签到神级系统',
    description: '普通打工人觉醒系统，从此一路开挂。',
    cover_url: null,
    category: '都市',
    episode_count: 72,
    tags: ['系统', '爽文'],
    rating: 8.7,
    created_at: '2026-07-24T22:00:00Z',
    updated_at: '2026-07-24T22:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440004',
    title: '王妃今天也想和离',
    description: '先婚后爱设定下，王府内外暗潮涌动。',
    cover_url: 'https://example.com/dramas/004.jpg',
    category: '古风',
    episode_count: 48,
    tags: ['古风', '权谋'],
    rating: 8.2,
    created_at: '2026-07-24T21:00:00Z',
    updated_at: '2026-07-24T21:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440005',
    title: '十八线女配觉醒了',
    description: '',
    cover_url: 'https://example.com/dramas/005.jpg',
    category: '穿书',
    episode_count: 40,
    tags: [],
    rating: 7.8,
    created_at: '2026-07-24T20:00:00Z',
    updated_at: '2026-07-24T20:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440006',
    title: '我在八零年代当后妈',
    description: '意外穿越后，她在年代剧本里经营起全新人生。',
    cover_url: 'https://example.com/dramas/006.jpg',
    category: '年代',
    episode_count: 36,
    tags: ['年代', '养娃'],
    rating: 8.1,
    created_at: '2026-07-24T19:00:00Z',
    updated_at: '2026-07-24T19:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440007',
    title: '隐婚老公太会宠',
    description: '契约婚姻曝光后，冷面总裁开启高能护妻模式。',
    cover_url: 'https://example.com/dramas/007.jpg',
    category: '甜宠',
    episode_count: 60,
    tags: ['契约婚姻', '总裁'],
    rating: 8.6,
    created_at: '2026-07-24T18:00:00Z',
    updated_at: '2026-07-24T18:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440008',
    title: '重生后我把渣男送进火葬场',
    description: '重来一次，她亲手改写命运与复仇结局。',
    cover_url: 'https://example.com/dramas/008.jpg',
    category: '复仇',
    episode_count: 52,
    tags: ['重生', '复仇'],
    rating: 8.8,
    created_at: '2026-07-24T17:00:00Z',
    updated_at: '2026-07-24T17:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440009',
    title: '误撩禁欲教授后她红了',
    description: '校园与娱乐圈双线展开，情感拉扯持续升温。',
    cover_url: null,
    category: '校园',
    episode_count: 44,
    tags: ['校园', '娱乐圈'],
    rating: 7.9,
    created_at: '2026-07-24T16:00:00Z',
    updated_at: '2026-07-24T16:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440010',
    title: '替嫁后她成了京圈白月光',
    description: '替嫁进入豪门后，真假身份与旧案同时浮现。',
    cover_url: 'https://example.com/dramas/010.jpg',
    category: '豪门',
    episode_count: 58,
    tags: ['替嫁', '马甲'],
    rating: null,
    created_at: '2026-07-24T15:00:00Z',
    updated_at: '2026-07-24T15:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440011',
    title: '全家都能听见我的心声',
    description: '穿书少女靠吐槽改变全家命运走向。',
    cover_url: 'https://example.com/dramas/011.jpg',
    category: '轻喜',
    episode_count: 30,
    tags: ['穿书', '读心'],
    rating: 8.3,
    created_at: '2026-07-24T14:00:00Z',
    updated_at: '2026-07-24T14:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440012',
    title: '天降萌宝总裁爹地别太宠',
    description: '萌宝助攻下，破镜重圆的爱情再次启动。',
    cover_url: 'https://example.com/dramas/012.jpg',
    category: '家庭',
    episode_count: 66,
    tags: ['萌宝', '破镜重圆'],
    rating: 8,
    created_at: '2026-07-24T13:00:00Z',
    updated_at: '2026-07-24T13:00:00Z',
  },
].map((drama) => DramaSchema.parse(drama));

function cloneDrama(drama: Drama): Drama {
  return {
    ...drama,
    tags: [...drama.tags],
  };
}

export class DramaMockRepository implements DramaRepositoryInterface {
  private data: Map<string, Drama>;

  constructor(initialData: Drama[] = HOMEPAGE_DRAMAS) {
    this.data = new Map(initialData.map((drama) => [drama.id, cloneDrama(drama)]));
  }

  async findMany(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    const all = Array.from(this.data.values());
    const total = all.length;
    const totalPages = total === 0 ? 0 : Math.ceil(total / params.pageSize);
    const start = (params.page - 1) * params.pageSize;
    const data = all.slice(start, start + params.pageSize).map(cloneDrama);

    return {
      data,
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: totalPages,
      },
    };
  }

  async findById(id: string): Promise<Drama | null> {
    const drama = this.data.get(id);
    return drama ? cloneDrama(drama) : null;
  }

  async create(data: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama> {
    const now = new Date().toISOString();
    const id = crypto.randomUUID();
    const drama = DramaSchema.parse({
      ...data,
      id,
      created_at: now,
      updated_at: now,
    });

    this.data.set(id, drama);
    return cloneDrama(drama);
  }

  async update(
    id: string,
    data: Partial<Omit<Drama, 'id' | 'created_at' | 'updated_at'>>,
  ): Promise<Drama | null> {
    const existing = this.data.get(id);
    if (!existing) return null;

    const updated = DramaSchema.parse({
      ...existing,
      ...data,
      updated_at: new Date().toISOString(),
    });

    this.data.set(id, updated);
    return cloneDrama(updated);
  }

  async delete(id: string): Promise<boolean> {
    return this.data.delete(id);
  }
}
