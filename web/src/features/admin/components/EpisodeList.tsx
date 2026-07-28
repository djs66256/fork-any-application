'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { DataTable } from '@/features/admin/components/DataTable';
import { ConfirmModal } from '@/features/admin/components/ConfirmModal';
import { useEpisodes } from '@/features/admin/hooks/useEpisodes';
import { adminApi } from '@/features/admin/api/client';
import { useAuth } from '@/features/admin/contexts/AuthContext';
import type { AdminEpisode } from '@/features/admin/api/types';
import styles from './DramaList.module.css';

export function EpisodeList() {
  const params = useParams<{ id: string }>();
  const dramaId = params.id;
  const { episodes, isLoading, error, refetch } = useEpisodes(dramaId);
  const [deleteTarget, setDeleteTarget] = useState<AdminEpisode | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const { role } = useAuth();
  const [dramaTitle, setDramaTitle] = useState<string>('');

  useEffect(() => {
    // Fetch drama title
    adminApi
      .getDrama(dramaId)
      .then((d) => setDramaTitle(d.title))
      .catch(() => setDramaTitle('未知短剧'));
  }, [dramaId]);

  const isViewer = role === 'viewer';

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    try {
      await adminApi.deleteEpisode(deleteTarget.id);
      setDeleteTarget(null);
      refetch();
    } catch {
      // Error handled by toast
    } finally {
      setIsDeleting(false);
    }
  };

  const columns = [
    {
      key: 'episode_number',
      header: '序号',
      width: '80px',
      render: (row: AdminEpisode) => row.episode_number,
    },
    { key: 'title', header: '标题', render: (row: AdminEpisode) => row.title },
    {
      key: 'duration',
      header: '时长',
      render: (row: AdminEpisode) =>
        row.duration != null ? `${Math.floor(row.duration / 60)}:${String(row.duration % 60).padStart(2, '0')}` : '-',
    },
    {
      key: 'video_url',
      header: '视频 URL',
      render: (row: AdminEpisode) =>
        row.video_url ? (
          <span style={{ fontSize: '12px', color: 'var(--color-fg-muted)', maxWidth: '200px', display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {row.video_url}
          </span>
        ) : (
          '-'
        ),
    },
    {
      key: 'actions',
      header: '操作',
      width: '160px',
      render: (row: AdminEpisode) => (
        <div className={styles.actions}>
          {!isViewer && (
            <>
              <Link
                href={`/admin/dramas/${dramaId}/episodes/${row.id}/edit`}
                className={styles.actionLink}
              >
                编辑
              </Link>
              <button
                className={styles.actionLinkDanger}
                onClick={() => setDeleteTarget(row)}
                type="button"
              >
                删除
              </button>
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <Link
            href="/admin/dramas"
            className={styles.backLink}
          >
            &larr; 返回短剧列表
          </Link>
          <h1 className={styles.dramaTitle}>
            {dramaTitle || '加载中...'} — 剧集管理
          </h1>
        </div>
        {!isViewer && (
          <Link
            href={`/admin/dramas/${dramaId}/episodes/new`}
            className={styles.createButton}
          >
            新建剧集
          </Link>
        )}
      </div>

      <div className={styles.card}>
        <DataTable
          columns={columns}
          data={episodes}
          isLoading={isLoading}
          error={error}
          onRetry={refetch}
          emptyText="暂无剧集"
        />
      </div>

      {deleteTarget && (
        <ConfirmModal
          title="删除剧集"
          message={`确认删除剧集「${deleteTarget.title}」？此操作不可恢复。`}
          confirmLabel="删除"
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
          isLoading={isDeleting}
        />
      )}
    </div>
  );
}