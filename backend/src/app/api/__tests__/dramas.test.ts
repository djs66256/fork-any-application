import { describe, it, expect } from 'vitest';
import { NextRequest } from 'next/server';

const { GET, POST } = await import('../dramas/route');

describe('GET /api/dramas', () => {
  it('should return empty list with pagination metadata', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas?page=1&pageSize=10');
    const response = await GET(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toEqual([]);
    expect(body.pagination).toBeDefined();
    expect(body.pagination.page).toBe(1);
    expect(body.pagination.page_size).toBe(10);
    expect(body.pagination.total).toBe(0);
    expect(body.pagination.total_pages).toBe(0);
  });

  it('should use default page and pageSize when not provided', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas');
    const response = await GET(request);
    const body = await response.json();

    expect(body.pagination.page).toBe(1);
    expect(body.pagination.page_size).toBe(10);
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
