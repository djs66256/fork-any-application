import { describe, it, expect, vi, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';

vi.mock('@/services/drama/drama.service', () => ({
  DramaService: vi.fn().mockImplementation(() => ({
    listClassificationTags: vi.fn().mockResolvedValue({
      gender: 'all',
      dimensions: [
        { key: 'era_background', name: '时代背景', tags: ['都市', '古风'] },
        { key: 'theme_plot', name: '主题情节', tags: ['逆袭', '系统'] },
        { key: 'character_setting', name: '角色设定', tags: ['总裁', '萌宝'] },
      ],
    }),
  })),
}));

const { GET } = await import('../dramas/tags/route');
const { DramaService } = await import('@/services/drama/drama.service');

describe('GET /api/dramas/tags', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return canonical response and default gender to all', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/tags');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      data: {
        gender: 'all',
        dimensions: [
          { key: 'era_background', name: '时代背景', tags: ['都市', '古风'] },
          { key: 'theme_plot', name: '主题情节', tags: ['逆袭', '系统'] },
          { key: 'character_setting', name: '角色设定', tags: ['总裁', '萌宝'] },
        ],
      },
    });
    expect(vi.mocked(DramaService).mock.results[0]?.value.listClassificationTags).toHaveBeenCalledWith({
      gender: 'all',
    });
  });

  it('should forward explicit gender values', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/tags?gender=male');
    const response = await GET(request, undefined);

    expect(response.status).toBe(200);
    expect(vi.mocked(DramaService).mock.results[0]?.value.listClassificationTags).toHaveBeenCalledWith({
      gender: 'male',
    });
  });

  it('should preserve empty dimensions instead of omitting them', async () => {
    vi.mocked(DramaService).mockImplementationOnce(() => ({
      listClassificationTags: vi.fn().mockResolvedValue({
        gender: 'female',
        dimensions: [
          { key: 'era_background', name: '时代背景', tags: ['都市'] },
          { key: 'theme_plot', name: '主题情节', tags: ['甜宠'] },
          { key: 'character_setting', name: '角色设定', tags: [] },
        ],
      }),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/dramas/tags?gender=female');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data.dimensions[2]).toEqual({
      key: 'character_setting',
      name: '角色设定',
      tags: [],
    });
  });

  it('should reject invalid gender with validation error', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/tags?gender=unknown');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return internal error when service throws unexpectedly', async () => {
    vi.mocked(DramaService).mockImplementationOnce(() => ({
      listClassificationTags: vi.fn().mockRejectedValue(new Error('boom')),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/dramas/tags');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
