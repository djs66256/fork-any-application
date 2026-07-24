import Redis from 'ioredis';
import { config } from '@/lib/config';

let _redis: Redis | null = null;

export function getRedis(): Redis {
  if (!_redis) {
    _redis = new Redis(config.redis.url, {
      lazyConnect: true,
      retryStrategy(times: number) {
        if (times > 3) return null; // Stop retrying
        return Math.min(times * 200, 2000);
      },
    });
  }
  return _redis;
}

export async function checkRedisHealth(): Promise<boolean> {
  try {
    const redis = getRedis();
    const result = await redis.ping();
    return result === 'PONG';
  } catch {
    return false;
  }
}

export function closeRedis(): void {
  if (_redis) {
    _redis.disconnect();
    _redis = null;
  }
}
