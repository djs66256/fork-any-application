export const config = {
  app: {
    name: process.env.APP_NAME ?? 'Backend',
    version: process.env.APP_VERSION ?? '0.1.0',
    env: process.env.NODE_ENV ?? 'development',
  },
  supabase: {
    url: process.env.SUPABASE_URL ?? '',
    anonKey: process.env.SUPABASE_ANON_KEY ?? '',
    serviceRoleKey: process.env.SUPABASE_SERVICE_ROLE_KEY ?? '',
  },
  redis: {
    url: process.env.REDIS_URL ?? 'redis://localhost:6379',
  },
} as const;
