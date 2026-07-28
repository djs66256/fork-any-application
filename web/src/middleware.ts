import { NextResponse, type NextRequest } from 'next/server';
import { createServerClient } from '@supabase/ssr';

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Only handle /admin routes
  if (!pathname.startsWith('/admin')) {
    return NextResponse.next();
  }

  // Start with a next() response so supabase cookie changes are always
  // attached to the final response — even when we redirect later.
  let response = NextResponse.next({ request });

  // Create a Supabase client for server-side auth check
  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
    {
      cookies: {
        getAll() {
          return request.cookies.getAll();
        },
        setAll(cookiesToSet) {
          cookiesToSet.forEach(({ name, value, options }) => {
            response.cookies.set(name, value, options);
          });
        },
      },
    },
  );

  const { data: { session } } = await supabase.auth.getSession();

  const isLoginPage = pathname === '/admin/login';

  // Not logged in and trying to access admin pages (not login)
  if (!session && !isLoginPage) {
    const loginUrl = new URL('/admin/login', request.url);
    response = NextResponse.redirect(loginUrl);
    return response;
  }

  // Already logged in and trying to access login page
  if (session && isLoginPage) {
    const adminUrl = new URL('/admin', request.url);
    response = NextResponse.redirect(adminUrl);
    return response;
  }

  return response;
}

export const config = {
  matcher: ['/admin/:path*'],
};