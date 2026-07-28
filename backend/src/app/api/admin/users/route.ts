import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { withCors } from '@/middleware/cors';
import { requireRole } from '@/middleware/auth';
import { AdminService } from '@/services/admin/admin.service';

export const GET = withCors(requireRole(
  ['admin'],
  withErrorHandler(async (request: NextRequest) => {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get('page') || '1', 10);
    const pageSize = parseInt(searchParams.get('pageSize') || '20', 10);

    const service = new AdminService();
    const result = await service.listUsers(page, pageSize);

    return NextResponse.json({
      code: 0,
      data: result,
      message: 'ok',
    });
  }),
));