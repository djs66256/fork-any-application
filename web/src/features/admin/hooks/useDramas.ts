'use client';

import { useState, useEffect, useCallback } from 'react';
import { adminApi } from '@/features/admin/api/client';
import type { AdminDrama, Pagination } from '@/features/admin/api/types';

export function useDramas(page = 1, pageSize = 20) {
  const [dramas, setDramas] = useState<AdminDrama[]>([]);
  const [pagination, setPagination] = useState<Pagination | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDramas = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await adminApi.listDramas(page, pageSize);
      setDramas(result.data);
      setPagination(result.pagination);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    fetchDramas();
  }, [fetchDramas]);

  return { dramas, pagination, isLoading, error, refetch: fetchDramas };
}