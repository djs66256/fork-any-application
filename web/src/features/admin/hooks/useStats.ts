'use client';

import { useState, useEffect, useCallback } from 'react';
import { adminApi } from '@/features/admin/api/client';
import type { AdminStats } from '@/features/admin/api/types';

export function useStats() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await adminApi.getStats();
      setStats(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void fetchStats();
    }, 0);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [fetchStats]);

  return { stats, isLoading, error, refetch: fetchStats };
}