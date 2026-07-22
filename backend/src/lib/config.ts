export const config = {
  app: {
    name: process.env.APP_NAME ?? 'ShortDrama Backend',
    version: process.env.APP_VERSION ?? '0.1.0',
    env: process.env.NODE_ENV ?? 'development',
  },
} as const;
