import { describe, expect, it } from 'vitest';
import { NextRequest } from 'next/server';

const { GET } = await import('../messages/system/route');

describe('GET /api/messages/system', () => {
  it('should return paginated system messages', async () => {
    const request = new NextRequest('https://example.com/api/messages/system?page=1&pageSize=2');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toHaveLength(2);
    expect(body.pagination).toEqual({
      page: 1,
      page_size: 2,
      total: 3,
      total_pages: 2,
    });
  });

  it('should return validation error for invalid pagination', async () => {
    const request = new NextRequest('https://example.com/api/messages/system?page=0&pageSize=21');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });
});
