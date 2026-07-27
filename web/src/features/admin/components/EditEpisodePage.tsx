'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { EpisodeForm } from '@/features/admin/components/EpisodeForm';
import { adminApi } from '@/features/admin/api/client';
import type { AdminEpisode } from '@/features/admin/api/types';

export function EditEpisodePage() {
  const params = useParams<{ id: string; episodeId: string }>();
  const dramaId = params.id;
  const episodeId = params.episodeId;

  const [episode, setEpisode] = useState<AdminEpisode | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    adminApi
      .listEpisodes(dramaId)
      .then((episodes) => {
        const found = episodes.find((ep) => ep.id === episodeId);
        if (found) {
          setEpisode(found);
        } else {
          setError('剧集不存在');
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : '加载失败'))
      .finally(() => setIsLoading(false));
  }, [dramaId, episodeId]);

  if (isLoading) {
    return (
      <div style={{ padding: 'var(--spacing-lg)', color: 'var(--color-text-secondary)' }}>
        加载中...
      </div>
    );
  }

  if (error || !episode) {
    return (
      <div style={{ padding: 'var(--spacing-lg)', color: 'var(--color-text-secondary)' }}>
        <p>{error || '剧集不存在'}</p>
        <Link
          href={`/admin/dramas/${dramaId}/episodes`}
          style={{ color: '#2563EB', fontSize: 'var(--font-size-sm)' }}
        >
          返回剧集列表
        </Link>
      </div>
    );
  }

  return (
    <div>
      <Link
        href={`/admin/dramas/${dramaId}/episodes`}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          fontSize: 'var(--font-size-sm)',
          color: '#2563EB',
          textDecoration: 'none',
          marginBottom: 'var(--spacing-md)',
        }}
      >
        &larr; 返回剧集列表
      </Link>
      <h1
        style={{
          fontSize: 'var(--font-size-lg)',
          fontWeight: 600,
          color: 'var(--color-text-primary)',
          marginBottom: 'var(--spacing-lg)',
        }}
      >
        编辑剧集
      </h1>
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
          padding: 'var(--spacing-lg)',
        }}
      >
        <EpisodeForm dramaId={dramaId} initialData={episode} isEdit />
      </div>
    </div>
  );
}