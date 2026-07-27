import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { requireRole, getAuth } from '@/middleware/auth';
import { AdminRoleUpdateSchema } from '@/lib/schemas';
import { AdminService } from '@/services/admin/admin.service';

export const PUT = requireRole(
  ['admin'],
  withErrorHandler(async (request: NextRequest, context: unknown) => {
    const { id } = await (context as { params: Promise<{ id: string }> }).params;
    const body = await request.json();
    const { role } = AdminRoleUpdateSchema.parse(body);

    const auth = getAuth(request);
    const service = new AdminService();
    const user = await service.updateUserRole(id, role, auth.userId);

    return NextResponse.json({
      code: 0,
      data: user,
      message: 'ok',
    });
  }),
);