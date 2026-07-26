import { describe, it, expect, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';
import { POST } from '../player/stop/route';
import {
  getPlaybackHistoryRepository,
  resetRepositoryRegistry,
} from '@/repositories/repository-registry';

const PLAYBACK_SESSION_ID = '770e8400-e29b-41d4-a716-446655440000';
const DRAMA_ID = '550e8400-e29b-41d4-a716-446655440001';
const EPISODE_ID = '660e8400-e29b-41d4-a716-446655440001';

describe('POST /api/player/stop', () => {
  beforeEach(() => {
    resetRepositoryRegistry();
  });

  it('should save playback progress', async () => {
    const request = new NextRequest('https://example.com/api/player/stop', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: EPISODE_ID,
        progress: 120,
        duration: 180,
      }),
    });

    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.code).toBe(0);
    expect(body.data.saved_progress).toBe(120);
  });

  it('should clamp progress to duration', async () => {
    const request = new NextRequest('https://example.com/api/player/stop', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: EPISODE_ID,
        progress: 999,
        duration: 180,
      }),
    });

    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data.saved_progress).toBe(180);
  });

  it('should overwrite latest history for repeated stop calls', async () => {
    const firstRequest = new NextRequest('https://example.com/api/player/stop', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: EPISODE_ID,
        progress: 60,
        duration: 180,
      }),
    });
    await POST(firstRequest);

    const secondRequest = new NextRequest('https://example.com/api/player/stop', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: EPISODE_ID,
        progress: 90,
        duration: 180,
      }),
    });
    const response = await POST(secondRequest);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data.saved_progress).toBe(90);

    const saved = await getPlaybackHistoryRepository().findLatest(PLAYBACK_SESSION_ID, DRAMA_ID);
    expect(saved?.progress).toBe(90);
  });

  it('should return 400 when playback session header is missing', async () => {
    const request = new NextRequest('https://example.com/api/player/stop', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: EPISODE_ID,
        progress: 120,
        duration: 180,
      }),
    });

    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('INVALID_PLAYBACK_SESSION');
  });

  it('should return 404 when episode does not belong to drama', async () => {
    const request = new NextRequest('https://example.com/api/player/stop', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Playback-Session-Id': PLAYBACK_SESSION_ID,
      },
      body: JSON.stringify({
        drama_id: DRAMA_ID,
        episode_id: '660e8400-e29b-41d4-a716-446655440011',
        progress: 120,
        duration: 180,
      }),
    });

    const response = await POST(request);
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error.code).toBe('EPISODE_NOT_FOUND');
  });
});
