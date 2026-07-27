import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { requireRole } from '@/middleware/auth';
import { AdminEpisodeCreateSchema } from '@/lib/schemas';
import { AdminService } from '@/services/admin/admin.service';

export const GET = requireRole(
  ['admin', 'editor', 'viewer'],
  withErrorHandler(async (request: NextRequest, context: unknown) => {
    const { id } = await (context as { params: Promise<{ id: string }> }).params;
    const service = new AdminService();
    const episodes = await service.listEpisodes(id);

    return NextResponse.json({
      code: 0,
      data: {
        drama_id: id,
        items: episodes,
      },
      message: 'ok',
    });
  }),
);

export const POST = requireRole(
  ['admin', 'editor'],
  withErrorHandler(async (request: NextRequest, context: unknown) => {
    const { id } = await (context as { params: Promise<{ id: string }> }).params;
    const body = await request.json();
    const data = AdminEpisodeCreateSchema.parse(body);

    const service = new AdminService();
    const episode = await service.createEpisode(id, data);

    return NextResponse.json(
      {
        code: 0,
        data: episode,
        message: 'ok',
      },
      { status: 201 },
    );
  }),
);