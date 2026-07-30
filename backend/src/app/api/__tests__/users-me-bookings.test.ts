import { describe, it, expect, vi, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';
import { Errors } from '@/lib/errors';

const mockGetUser = vi.fn();

vi.mock('@/infrastructure/supabase', () => ({
  getSupabaseAdmin: () => ({
    auth: {
      getUser: mockGetUser,
    },
    from: vi.fn(),
  }),
}));

vi.mock('@/repositories/supabase/drama.supabase.repository', () => ({
  DramaSupabaseRepository: vi.fn(),
}));

vi.mock('@/services/drama/drama.service', () => ({
  DramaService: vi.fn().mockImplementation(() => ({
    listUserBookings: vi.fn().mockResolvedValue({
      data: [
        {
          drama_id: '550e8400-e29b-41d4-a716-446655440001',
          title: '逆袭归来后我成了豪门团宠',
          cover_url: 'https://example.com/dramas/001.jpg',
          episode_count: 68,
          booked_at: '2026-07-30T03:25:00.000Z',
          availability_status: 'online',
        },
      ],
      pagination: {
        page: 1,
        page_size: 20,
        total: 1,
        total_pages: 1,
      },
      summary: {
        online_count: 1,
        upcoming_count: 2,
      },
    }),
  })),
}));

const { GET } = await import('../users/me/bookings/route');
const { DramaService } = await import('@/services/drama/drama.service');

describe('GET /api/users/me/bookings', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return booking assets with default query values for authenticated users', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: 'user-1',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    const request = new NextRequest('https://localhost:3001/api/users/me/bookings', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.summary).toEqual({ online_count: 1, upcoming_count: 2 });
    expect(vi.mocked(DramaService).mock.results[0]?.value.listUserBookings).toHaveBeenCalledWith({
      userId: 'user-1',
      status: 'online',
      page: 1,
      pageSize: 20,
    });
  });

  it('should forward explicit query params to the service', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: 'user-1',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    const request = new NextRequest(
      'https://localhost:3001/api/users/me/bookings?status=upcoming&page=2&pageSize=5',
      {
        headers: {
          Authorization: 'Bearer valid-token',
        },
      },
    );

    const response = await GET(request, undefined);

    expect(response.status).toBe(200);
    expect(vi.mocked(DramaService).mock.results[0]?.value.listUserBookings).toHaveBeenCalledWith({
      userId: 'user-1',
      status: 'upcoming',
      page: 2,
      pageSize: 5,
    });
  });

  it('should reject unauthenticated requests', async () => {
    const request = new NextRequest('https://localhost:3001/api/users/me/bookings');
    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(401);
    expect(body.error.code).toBe('AUTH_UNAUTHORIZED');
  });

  it('should reject invalid query params with validation error', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: 'user-1',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    const request = new NextRequest(
      'https://localhost:3001/api/users/me/bookings?status=invalid&page=0&pageSize=21',
      {
        headers: {
          Authorization: 'Bearer valid-token',
        },
      },
    );

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(400);
    expect(body.error.code).toBe('VALIDATION_ERROR');
  });

  it('should keep empty list contract stable for oversized pages', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: 'user-1',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    vi.mocked(DramaService).mockImplementationOnce(() => ({
      listUserBookings: vi.fn().mockResolvedValue({
        data: [],
        pagination: {
          page: 999,
          page_size: 20,
          total: 4,
          total_pages: 1,
        },
        summary: {
          online_count: 1,
          upcoming_count: 3,
        },
      }),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/users/me/bookings?page=999', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body.data).toEqual([]);
    expect(body.pagination).toEqual({
      page: 999,
      page_size: 20,
      total: 4,
      total_pages: 1,
    });
  });

  it('should propagate service unavailable errors', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: 'user-1',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    vi.mocked(DramaService).mockImplementationOnce(() => ({
      listUserBookings: vi.fn().mockRejectedValue(Errors.serviceUnavailable('Supabase')),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/users/me/bookings', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(503);
    expect(body.error.code).toBe('SERVICE_UNAVAILABLE');
  });

  it('should return internal error when service throws unexpectedly', async () => {
    mockGetUser.mockResolvedValueOnce({
      data: {
        user: {
          id: 'user-1',
          app_metadata: { role: 'viewer' },
        },
      },
      error: null,
    });

    vi.mocked(DramaService).mockImplementationOnce(() => ({
      listUserBookings: vi.fn().mockRejectedValue(new Error('boom')),
    }) as unknown as InstanceType<typeof DramaService>);

    const request = new NextRequest('https://localhost:3001/api/users/me/bookings', {
      headers: {
        Authorization: 'Bearer valid-token',
      },
    });

    const response = await GET(request, undefined);
    const body = await response.json();

    expect(response.status).toBe(500);
    expect(body.error.code).toBe('INTERNAL_ERROR');
  });
});
