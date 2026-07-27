'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { DramaForm } from '@/features/admin/components/DramaForm';
import { adminApi } from '@/features/admin/api/client';
import type { AdminDrama } from '@/features/admin/api/types';

export function EditDramaPage() {
  const params = useParams<{ id: string }>();
  const [drama, setDrama] = useState<AdminDrama | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    adminApi
      .getDrama(params.id)
      .then(setDrama)
      .catch((err) => setError(err instanceof Error ? err.message : '加载失败'))
      .finally(() => setIsLoading(false));
  }, [params.id]);

  if (isLoading) {
    return (
      <div style={{ padding: 'var(--spacing-lg)', color: 'var(--color-text-secondary)' }}>
        加载中...
      </div>
    );
  }

  if (error || !drama) {
    return (
      <div style={{ padding: 'var(--spacing-lg)', color: 'var(--color-text-secondary)' }}>
        <p>{error || '短剧不存在'}</p>
        <Link
          href="/admin/dramas"
          style={{ color: '#2563EB', fontSize: 'var(--font-size-sm)' }}
        >
          返回短剧列表
        </Link>
      </div>
    );
  }

  return (
    <div>
      <Link
        href="/admin/dramas"
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          fontSize: 'var(--font-size-sm)',
          color: '#2563EB',
          textDecoration: 'none',
          marginBottom: 'var(--spacing-md)',
        }}
      >
        &larr; 返回短剧列表
      </Link>
      <h1
        style={{
          fontSize: 'var(--font-size-lg)',
          fontWeight: 600,
          color: 'var(--color-text-primary)',
          marginBottom: 'var(--spacing-lg)',
        }}
      >
        编辑短剧
      </h1>
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
          padding: 'var(--spacing-lg)',
        }}
      >
        <DramaForm initialData={drama} isEdit />
      </div>
    </div>
  );
}