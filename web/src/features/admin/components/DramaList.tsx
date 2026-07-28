'use client';

import { useState } from 'react';
import Link from 'next/link';
import { DataTable } from '@/features/admin/components/DataTable';
import { ConfirmModal } from '@/features/admin/components/ConfirmModal';
import { useDramas } from '@/features/admin/hooks/useDramas';
import { adminApi } from '@/features/admin/api/client';
import { useAuth } from '@/features/admin/contexts/AuthContext';
import type { AdminDrama } from '@/features/admin/api/types';
import styles from './DramaList.module.css';

export function DramaList() {
  const [page, setPage] = useState(1);
  const { dramas, pagination, isLoading, error, refetch } = useDramas(page, 20);
  const [deleteTarget, setDeleteTarget] = useState<AdminDrama | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const { role } = useAuth();

  const isViewer = role === 'viewer';

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    try {
      await adminApi.deleteDrama(deleteTarget.id);
      setDeleteTarget(null);
      refetch();
    } catch {
      // Error handled by toast in parent
    } finally {
      setIsDeleting(false);
    }
  };

  const columns = [
    {
      key: 'cover',
      header: '封面',
      width: '60px',
      render: (row: AdminDrama) =>
        row.cover_url ? (
          <img
            className={styles.coverThumb}
            src={row.cover_url}
            alt={row.title}
            onError={(e) => {
              (e.target as HTMLImageElement).style.display = 'none';
            }}
          />
        ) : (
          <div className={styles.coverPlaceholder}>无图</div>
        ),
    },
    { key: 'title', header: '标题', render: (row: AdminDrama) => row.title },
    { key: 'category', header: '分类', render: (row: AdminDrama) => row.category || '-' },
    {
      key: 'episode_count',
      header: '集数',
      render: (row: AdminDrama) => row.episode_count,
    },
    {
      key: 'rating',
      header: '评分',
      render: (row: AdminDrama) => (row.rating != null ? row.rating : '-'),
    },
    {
      key: 'actions',
      header: '操作',
      width: '200px',
      render: (row: AdminDrama) => (
        <div className={styles.actions}>
          <Link
            href={`/admin/dramas/${row.id}/episodes`}
            className={styles.actionLink}
          >
            剧集
          </Link>
          {!isViewer && (
            <>
              <Link
                href={`/admin/dramas/${row.id}/edit`}
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
        <h1 className={styles.pageTitle}>短剧管理</h1>
        {!isViewer && (
          <Link href="/admin/dramas/new" className={styles.createButton}>
            新建短剧
          </Link>
        )}
      </div>

      <div className={styles.card}>
        <DataTable
          columns={columns}
          data={dramas}
          isLoading={isLoading}
          error={error}
          onRetry={refetch}
          emptyText="暂无短剧"
          page={pagination?.page}
          totalPages={pagination?.total_pages}
          total={pagination?.total}
          onPageChange={setPage}
        />
      </div>

      {deleteTarget && (
        <ConfirmModal
          title="删除短剧"
          message="删除短剧将同时删除所有关联剧集，不可恢复。确认删除？"
          confirmLabel="删除"
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
          isLoading={isDeleting}
        />
      )}
    </div>
  );
}