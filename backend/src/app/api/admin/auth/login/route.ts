import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { AdminLoginRequestSchema } from '@/lib/schemas';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';

export const POST = withErrorHandler(async (request: NextRequest) => {
  const body = await request.json();
  const { email, password } = AdminLoginRequestSchema.parse(body);

  const supabase = getSupabaseAdmin();
  const { data, error } = await supabase.auth.signInWithPassword({
    email,
    password,
  });

  if (error || !data.user || !data.session) {
    throw Errors.invalidCredentials();
  }

  const role = data.user.app_metadata?.role || 'viewer';

  return NextResponse.json({
    code: 0,
    data: {
      token: data.session.access_token,
      user: {
        id: data.user.id,
        email: data.user.email,
        role,
      },
    },
    message: 'ok',
  });
});