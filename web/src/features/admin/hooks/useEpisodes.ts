'use client';

import { useState, useEffect, useCallback } from 'react';
import { adminApi } from '@/features/admin/api/client';
import type { AdminEpisode } from '@/features/admin/api/types';

export function useEpisodes(dramaId: string) {
  const [episodes, setEpisodes] = useState<AdminEpisode[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchEpisodes = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await adminApi.listEpisodes(dramaId);
      setEpisodes(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setIsLoading(false);
    }
  }, [dramaId]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void fetchEpisodes();
    }, 0);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [fetchEpisodes]);

  return { episodes, isLoading, error, refetch: fetchEpisodes };
}