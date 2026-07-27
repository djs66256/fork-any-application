import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { requireRole } from '@/middleware/auth';
import { AdminDramaCreateSchema } from '@/lib/schemas';
import { AdminService } from '@/services/admin/admin.service';

export const GET = requireRole(
  ['admin', 'editor', 'viewer'],
  withErrorHandler(async (request: NextRequest) => {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get('page') || '1', 10);
    const pageSize = parseInt(searchParams.get('pageSize') || '20', 10);

    const service = new AdminService();
    const result = await service.listDramas(page, pageSize);

    return NextResponse.json({
      code: 0,
      data: result,
      message: 'ok',
    });
  }),
);

export const POST = requireRole(
  ['admin', 'editor'],
  withErrorHandler(async (request: NextRequest) => {
    const body = await request.json();
    const data = AdminDramaCreateSchema.parse(body);

    const service = new AdminService();
    const drama = await service.createDrama(data);

    return NextResponse.json(
      {
        code: 0,
        data: drama,
        message: 'ok',
      },
      { status: 201 },
    );
  }),
);