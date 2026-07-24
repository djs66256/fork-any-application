import { describe, it, expect } from 'vitest';
import { NextRequest } from 'next/server';

const { GET: getDramaById } = await import('../dramas/[id]/route');
const { GET: getEpisodeById } = await import('../episodes/[id]/route');
const { POST: postPlayerStart } = await import('../player/start/route');
const { POST: postPlayerStop } = await import('../player/stop/route');

describe('skeleton endpoints should return 501', () => {
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

  it('POST /api/player/start should return 501', async () => {
    const request = new NextRequest('https://localhost:3001/api/player/start', {
      method: 'POST',
    });
    const response = await postPlayerStart(request);
    const body = await response.json();

    expect(response.status).toBe(501);
    expect(body.error.code).toBe('NOT_IMPLEMENTED');
  });

  it('POST /api/player/stop should return 501', async () => {
    const request = new NextRequest('https://localhost:3001/api/player/stop', {
      method: 'POST',
    });
    const response = await postPlayerStop(request);
    const body = await response.json();

    expect(response.status).toBe(501);
    expect(body.error.code).toBe('NOT_IMPLEMENTED');
  });
});
