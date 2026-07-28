'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { EpisodeForm } from '@/features/admin/components/EpisodeForm';

export function NewEpisodePage() {
  const params = useParams<{ id: string }>();
  const dramaId = params.id;

  return (
    <div>
      <Link
        href={`/admin/dramas/${dramaId}/episodes`}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          fontSize: 'var(--font-size-small)',
          color: 'var(--color-accent)',
          textDecoration: 'none',
          marginBottom: 'var(--space-3)',
        }}
      >
        &larr; 返回剧集列表
      </Link>
      <h1
        style={{
          fontSize: 'var(--font-size-body)',
          fontWeight: 600,
          color: 'var(--color-fg-default)',
          marginBottom: 'var(--space-4)',
        }}
      >
        新建剧集
      </h1>
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
          padding: 'var(--space-4)',
        }}
      >
        <EpisodeForm dramaId={dramaId} />
      </div>
    </div>
  );
}