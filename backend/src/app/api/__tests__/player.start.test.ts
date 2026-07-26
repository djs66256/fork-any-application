import { describe, it, expect, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';
import { POST } from '../player/start/route';
import { resetRepositoryRegistry } from '@/repositories/repository-registry';

const PLAYBACK_SESSION_ID = '770e8400-e29b-41d4-a716-446655440000';
const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';
const EPISODE_ID = '660e8400-e29b-41d4-a716-446655440001';
const UNPLAYABLE_EPISODE_ID = '660e8400-e29b-41d4-a716-446655440003';

describe('POST /api/player/start', () => {
  beforeEach(() => {
    resetRepositoryRegistry();
  });

  it('should confirm playback start', async () => {
    const request = new NextRequest('https://example.com/api/player/start', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: EPISODE_ID,
        progress: 30,
      }),
    });

    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.code).toBe(0);
    expect(body.data.accepted_progress).toBe(30);
    expect(body.data.playback_session_id).toBe(PLAYBACK_SESSION_ID);
  });

  it('should return 404 when episode does not belong to drama', async () => {
    const request = new NextRequest('https://example.com/api/player/start', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: '660e8400-e29b-41d4-a716-446655440011',
        progress: 0,
      }),
    });

    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error.code).toBe('EPISODE_NOT_FOUND');
  });

  it('should return 409 when episode has no playable resource', async () => {
    const request = new NextRequest('https://example.com/api/player/start', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: UNPLAYABLE_EPISODE_ID,
        progress: 0,
      }),
    });

    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(409);
    expect(body.error.code).toBe('EPISODE_NOT_PLAYABLE');
  });

  it('should return 400 when playback session header is missing', async () => {
    const request = new NextRequest('https://example.com/api/player/start', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: EPISODE_ID,
        progress: 0,
      }),
    });

    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('INVALID_PLAYBACK_SESSION');
  });

  it('should return 400 when request body is invalid', async () => {
    const request = new NextRequest('https://example.com/api/player/start', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: EPISODE_ID,
        progress: -1,
      }),
    });

    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('INVALID_PARAMS');
  });
});
