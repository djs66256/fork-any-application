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
  auth: {
    get allowTestOtpBypass() {
      return (process.env.AUTH_ALLOW_TEST_OTP_BYPASS ?? 'false') === 'true';
    },
    get testOtpCode() {
      return process.env.AUTH_TEST_OTP_CODE ?? '123456';
    },
    get testPhone() {
      return process.env.AUTH_TEST_PHONE ?? '13800138000';
    },
    get accessTokenTtlSeconds() {
      return Number(process.env.AUTH_ACCESS_TOKEN_TTL_SECONDS ?? 3600);
    },
    get refreshTokenTtlSeconds() {
      return Number(process.env.AUTH_REFRESH_TOKEN_TTL_SECONDS ?? 2592000);
    },
  },
  player: {
    historyRepository: process.env.PLAYER_HISTORY_REPOSITORY ?? 'mock',
  },
} as const;
