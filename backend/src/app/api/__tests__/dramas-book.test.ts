import { describe, it, expect, vi, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';
import { Errors } from '@/lib/errors';

vi.mock('@/services/drama/drama.service', () => ({
  DramaService: vi.fn().mockImplementation(() => ({
    bookDrama: vi.fn().mockResolvedValue({
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
      booked: true,
      booking_count: 21,
    }),
  })),
}));

const { POST } = await import('../dramas/[id]/book/route');
const { DramaService } = await import('@/services/drama/drama.service');

describe('POST /api/dramas/[id]/book', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should book a drama with bearer auth', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/550e8400-e29b-41d4-a716-446655440001/book', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer user-1',
      },
    });
    const response = await POST(request, {
      params: Promise.resolve({ id: '550e8400-e29b-41d4-a716-446655440001' }),
    });
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toEqual({
      drama_id: '550e8400-e29b-41d4-a716-446655440001',
      booked: true,
      booking_count: 21,
    });
    expect(vi.mocked(DramaService).mock.results[0]?.value.bookDrama).toHaveBeenCalledWith({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      userId: 'user-1',
    });
  });

  it('should prefer x-user-id when present', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/550e8400-e29b-41d4-a716-446655440001/book', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer ignored-token',
        'x-user-id': 'user-2',
      },
    });
    const response = await POST(request, {
      params: Promise.resolve({ id: '550e8400-e29b-41d4-a716-446655440001' }),
    });

    expect(response.status).toBe(200);
    expect(vi.mocked(DramaService).mock.results[0]?.value.bookDrama).toHaveBeenCalledWith({
      dramaId: '550e8400-e29b-41d4-a716-446655440001',
      userId: 'user-2',
    });
  });

  it('should reject requests without authentication', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/550e8400-e29b-41d4-a716-446655440001/book', {
      method: 'POST',
    });
    const response = await POST(request, {
      params: Promise.resolve({ id: '550e8400-e29b-41d4-a716-446655440001' }),
    });
    const body = await response.json();

    expect(response.status).toBe(401);
    expect(body.error.code).toBe('UNAUTHORIZED');
  });

  it('should reject invalid drama ids with validation error', async () => {
    const request = new NextRequest('https://localhost:3001/api/dramas/not-a-uuid/book', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer user-1',
      },
    });
    const response = await POST(request, {
      params: Promise.resolve({ id: 'not-a-uuid' }),
    });
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should return not found when service raises domain not found', async () => {
    vi.mocked(DramaService).mockImplementationOnce(() => ({
      bookDrama: vi.fn().mockRejectedValue(Errors.notFound('Drama', '550e8400-e29b-41d4-a716-446655440001')),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/dramas/550e8400-e29b-41d4-a716-446655440001/book', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer user-1',
      },
    });
    const response = await POST(request, {
      params: Promise.resolve({ id: '550e8400-e29b-41d4-a716-446655440001' }),
    });
    const body = await response.json();

    expect(response.status).toBe(404);
    expect(body.error.code).toBe('NOT_FOUND');
  });

  it('should return internal error when service throws unexpectedly', async () => {
    vi.mocked(DramaService).mockImplementationOnce(() => ({
      bookDrama: vi.fn().mockRejectedValue(new Error('boom')),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/dramas/550e8400-e29b-41d4-a716-446655440001/book', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer user-1',
      },
    });
    const response = await POST(request, {
      params: Promise.resolve({ id: '550e8400-e29b-41d4-a716-446655440001' }),
    });
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
