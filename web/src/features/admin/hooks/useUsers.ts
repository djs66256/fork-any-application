'use client';

import { useState, useEffect, useCallback } from 'react';
import { adminApi } from '@/features/admin/api/client';
import type { AdminUser, Pagination } from '@/features/admin/api/types';

export function useUsers(page = 1, pageSize = 20) {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [pagination, setPagination] = useState<Pagination | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await adminApi.listUsers(page, pageSize);
      setUsers(result.data);
      setPagination(result.pagination);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void fetchUsers();
    }, 0);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [fetchUsers]);

  return { users, pagination, isLoading, error, refetch: fetchUsers };
}