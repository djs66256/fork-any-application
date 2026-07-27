'use client';

import type { ReactNode } from 'react';
import styles from './DataTable.module.css';

interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  width?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
  emptyText?: string;
  page?: number;
  totalPages?: number;
  total?: number;
  onPageChange?: (page: number) => void;
  skeletonRowCount?: number;
}

export function DataTable<T extends { id: string }>({
  columns,
  data,
  isLoading,
  error,
  onRetry,
  emptyText = '暂无数据',
  page,
  totalPages,
  total,
  onPageChange,
  skeletonRowCount = 5,
}: DataTableProps<T>) {
  if (error) {
    return (
      <div className={styles.errorContainer}>
        <span>{error}</span>
        <button className={styles.retryButton} onClick={onRetry} type="button">
          重试
        </button>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className={styles.loadingSkeleton}>
        {Array.from({ length: skeletonRowCount }).map((_, i) => (
          <div key={i}>
            <div className={styles.skeletonRow} />
            <div className={`${styles.skeletonRow} ${styles.skeletonRowShort}`} />
          </div>
        ))}
      </div>
    );
  }

  if (data.length === 0) {
    return <div className={styles.emptyState}>{emptyText}</div>;
  }

  return (
    <div>
      <div className={styles.tableWrapper}>
        <table className={styles.table}>
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={col.key} style={col.width ? { width: col.width } : undefined}>
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((row) => (
              <tr key={row.id}>
                {columns.map((col) => (
                  <td key={col.key}>{col.render(row)}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {page !== undefined && totalPages !== undefined && totalPages > 1 && (
        <div className={styles.pagination}>
          <button
            className={styles.pageButton}
            disabled={page <= 1}
            onClick={() => onPageChange?.(page - 1)}
            type="button"
          >
            上一页
          </button>
          <span className={styles.pageInfo}>
            第 {page} / {totalPages} 页 (共 {total} 条)
          </span>
          <button
            className={styles.pageButton}
            disabled={page >= totalPages}
            onClick={() => onPageChange?.(page + 1)}
            type="button"
          >
            下一页
          </button>
        </div>
      )}
    </div>
  );
}