import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { withCors } from '@/middleware/cors';
import { getSupabaseAdmin } from '@/infrastructure/supabase';

export const POST = withCors(withErrorHandler(async (request: NextRequest) => {
  const supabase = getSupabaseAdmin();
  await supabase.auth.signOut();

  return NextResponse.json({
    code: 0,
    data: null,
    message: 'ok',
  });
}));