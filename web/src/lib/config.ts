function getBooleanEnv(value: string | undefined, fallback: boolean): boolean {
  if (value === undefined) {
    return fallback;
  }

  return value.toLowerCase() === 'true';
}

export const config = {
  app: {
    name: process.env.NEXT_PUBLIC_APP_NAME ?? 'ShortDrama',
    version: process.env.NEXT_PUBLIC_APP_VERSION ?? '0.1.0',
    env: process.env.NODE_ENV ?? 'development',
  },
  mall: {
    route: '/mall',
    searchFallbackRoute: '/search',
    pageSize: 20,
    bridgeEnabled: getBooleanEnv(process.env.NEXT_PUBLIC_MALL_BRIDGE_ENABLED, true),
  },
} as const;
