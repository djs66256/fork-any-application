import { describe, it, expect } from 'vitest';
import { NextRequest } from 'next/server';

const { GET, POST } = await import('../dramas/route');

describe('GET /api/dramas', () => {
  it('should return canonical contract with default pagination', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas');
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toHaveProperty('data');
    expect(body).toHaveProperty('pagination');
    expect(body.pagination.page).toBe(1);
    expect(body.pagination.page_size).toBe(10);
    expect(body.pagination.total).toBe(12);
    expect(body.pagination.total_pages).toBe(2);
    expect(body.data).toHaveLength(10);
  });

  it('should return homepage feed fields on first page', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas?page=1&pageSize=10');
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data[0]).toMatchObject({
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
    });
  });

  it('should paginate second page with stable slice', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas?page=2&pageSize=10');
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toHaveLength(2);
    expect(body.data.map((item: { id: string }) => item.id)).toEqual([
      '550e8400-e29b-41d4-a716-446655440011',
      '550e8400-e29b-41d4-a716-446655440012',
    ]);
    expect(body.pagination).toEqual({
      page: 2,
      page_size: 10,
      total: 12,
      total_pages: 2,
    });
  });

  it('should return empty data for oversized pages while preserving pagination metadata', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas?page=999&pageSize=10');
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toEqual([]);
    expect(body.pagination).toEqual({
      page: 999,
      page_size: 10,
      total: 12,
      total_pages: 2,
    });
  });

  it('should reject invalid page params', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas?page=0&pageSize=10');
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should reject invalid pageSize params', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas?page=1&pageSize=101');
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });
});

describe('POST /api/dramas', () => {
  it('should return 501 not implemented', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas', {
      method: 'POST',
    });
    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(501);
    expect(body.error.code).toBe('NOT_IMPLEMENTED');
  });
});
