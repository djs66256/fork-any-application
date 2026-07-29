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
  earn: {
    route: '/earn',
    bridgeEnabled: getBooleanEnv(process.env.NEXT_PUBLIC_EARN_BRIDGE_ENABLED, true),
    browserFeedback: {
      loginUnavailable: '暂时无法打开登录，请稍后再试',
      taskRequiresApp: '请在 App 内完成该任务',
      taskUnavailable: '当前任务暂不可用，请稍后重试',
      taskInDevelopment: '该任务开发中，敬请期待',
      reloginRequired: '请先登录后再领取奖励',
      completionFailed: '奖励领取失败，请稍后重试',
    },
  },
} as const;
