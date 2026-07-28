import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { withCors } from '@/middleware/cors';
import { requireRole } from '@/middleware/auth';
import { AdminDramaUpdateSchema } from '@/lib/schemas';
import { AdminService } from '@/services/admin/admin.service';

export const GET = withCors(requireRole(
  ['admin', 'editor', 'viewer'],
  withErrorHandler(async (request: NextRequest, context: unknown) => {
    const { id } = await (context as { params: Promise<{ id: string }> }).params;
    const service = new AdminService();
    const drama = await service.getDrama(id);

    return NextResponse.json({
      code: 0,
      data: drama,
      message: 'ok',
    });
  }),
));

export const PUT = withCors(requireRole(
  ['admin', 'editor'],
  withErrorHandler(async (request: NextRequest, context: unknown) => {
    const { id } = await (context as { params: Promise<{ id: string }> }).params;
    const body = await request.json();
    const data = AdminDramaUpdateSchema.parse(body);

    const service = new AdminService();
    const drama = await service.updateDrama(id, data);

    return NextResponse.json({
      code: 0,
      data: drama,
      message: 'ok',
    });
  }),
));

export const DELETE = withCors(requireRole(
  ['admin', 'editor'],
  withErrorHandler(async (request: NextRequest, context: unknown) => {
    const { id } = await (context as { params: Promise<{ id: string }> }).params;
    const service = new AdminService();
    await service.deleteDrama(id);

    return NextResponse.json({
      code: 0,
      data: { deleted: true },
      message: 'ok',
    });
  }),
));