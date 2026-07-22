export const config = {
  app: {
    name: process.env.NEXT_PUBLIC_APP_NAME ?? 'ShortDrama',
    version: process.env.NEXT_PUBLIC_APP_VERSION ?? '0.1.0',
    env: process.env.NODE_ENV ?? 'development',
  },
} as const;
