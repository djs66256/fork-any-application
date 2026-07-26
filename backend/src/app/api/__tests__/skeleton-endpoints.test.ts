import { describe, it, expect } from 'vitest';
import { NextRequest } from 'next/server';

const { GET: getDramaById } = await import('../dramas/[id]/route');
const { GET: getEpisodeById } = await import('../episodes/[id]/route');

describe('remaining skeleton endpoints should return 501', () => {
  it('GET /api/dramas/[id] should return 501', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/123');
    const response = await getDramaById(request, { id: '123' });
    const body = await response.json();

    expect(response.status).toBe(501);
    expect(body.error.code).toBe('NOT_IMPLEMENTED');
  });

  it('GET /api/episodes/[id] should return 501', async () => {
    const request = new NextRequest('https://localhost:3001/api/episodes/123');
    const response = await getEpisodeById(request, { id: '123' });
    const body = await response.json();

    expect(response.status).toBe(501);
    expect(body.error.code).toBe('NOT_IMPLEMENTED');
  });

});
