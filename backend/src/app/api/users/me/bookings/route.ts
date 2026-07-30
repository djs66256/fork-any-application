import { NextRequest, NextResponse } from 'next/server';
import { BookingAssetQuerySchema } from '@/lib/schemas';
import { withErrorHandler } from '@/middleware/error-handler';
import { getAuth, requireAuthContext } from '@/middleware/auth';
import { DramaSupabaseRepository } from '@/repositories/supabase/drama.supabase.repository';
import { DramaService } from '@/services/drama/drama.service';

// 429 remains a contract reserve and is not actively implemented in this route.

export const GET = withErrorHandler(requireAuthContext(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const query = BookingAssetQuerySchema.parse({
    status: searchParams.get('status') ?? undefined,
    page: searchParams.get('page') ?? undefined,
    pageSize: searchParams.get('pageSize') ?? undefined,
  });

  const auth = getAuth(request);
  const repository = new DramaSupabaseRepository();
  const service = new DramaService(repository);
  const result = await service.listUserBookings({
    userId: auth.userId,
    status: query.status,
    page: query.page,
    pageSize: query.pageSize,
  });

  return NextResponse.json(result);
}));
