import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { withCors } from '@/middleware/cors';
import { requireRole } from '@/middleware/auth';
import { AdminEpisodeUpdateSchema } from '@/lib/schemas';
import { AdminService } from '@/services/admin/admin.service';

export const PUT = withCors(requireRole(
  ['admin', 'editor'],
  withErrorHandler(async (request: NextRequest, context: unknown) => {
    const { id } = await (context as { params: Promise<{ id: string }> }).params;
    const body = await request.json();
    const data = AdminEpisodeUpdateSchema.parse(body);

    const service = new AdminService();
    const episode = await service.updateEpisode(id, data);

    return NextResponse.json({
      code: 0,
      data: episode,
      message: 'ok',
    });
  }),
));

export const DELETE = withCors(requireRole(
  ['admin', 'editor'],
  withErrorHandler(async (request: NextRequest, context: unknown) => {
    const { id } = await (context as { params: Promise<{ id: string }> }).params;
    const service = new AdminService();
    await service.deleteEpisode(id);

    return NextResponse.json({
      code: 0,
      data: { deleted: true },
      message: 'ok',
    });
  }),
));