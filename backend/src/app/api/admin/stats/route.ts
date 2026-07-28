import { NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { requireRole } from '@/middleware/auth';
import { AdminService } from '@/services/admin/admin.service';

export const GET = requireRole(
  ['admin', 'editor', 'viewer'],
  withErrorHandler(async () => {
    const service = new AdminService();
    const stats = await service.getStats();

    return NextResponse.json({
      code: 0,
      data: stats,
      message: 'ok',
    });
  }),
);