import { checkSupabaseHealth } from '@/infrastructure/supabase';
import { checkRedisHealth } from '@/infrastructure/redis';
import { config } from '@/lib/config';

export interface HealthStatus {
  status: 'ok' | 'degraded' | 'error';
  version: string;
  timestamp: string;
  services: {
    database: 'connected' | 'disconnected' | 'unknown';
    redis: 'connected' | 'disconnected' | 'unknown';
  };
}

export class HealthService {
  async check(): Promise<HealthStatus> {
    const [dbHealthy, redisHealthy] = await Promise.all([
      checkSupabaseHealth(),
      checkRedisHealth(),
    ]);

    const database = dbHealthy ? 'connected' as const : 'disconnected' as const;
    const redis = redisHealthy ? 'connected' as const : 'disconnected' as const;

    let status: 'ok' | 'degraded' | 'error' = 'ok';
    if (!dbHealthy && !redisHealthy) {
      status = 'error';
    } else if (!dbHealthy || !redisHealthy) {
      status = 'degraded';
    }

    return {
      status,
      version: config.app.version,
      timestamp: new Date().toISOString(),
      services: {
        database,
        redis,
      },
    };
  }
}
