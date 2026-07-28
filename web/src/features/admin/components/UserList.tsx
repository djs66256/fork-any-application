'use client';

import { useState } from 'react';
import { DataTable } from '@/features/admin/components/DataTable';
import { useUsers } from '@/features/admin/hooks/useUsers';
import { adminApi } from '@/features/admin/api/client';
import { useAuth } from '@/features/admin/contexts/AuthContext';
import type { AdminUser } from '@/features/admin/api/types';
import styles from './DramaList.module.css';

export function UserList() {
  const [page, setPage] = useState(1);
  const { users, pagination, isLoading, error, refetch } = useUsers(page, 20);
  const { user, role } = useAuth();
  const currentUserId = user?.id ?? '';
  const [savingUserId, setSavingUserId] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  const isAdmin = role === 'admin';

  const handleRoleChange = async (userId: string, newRole: string) => {
    setSavingUserId(userId);
    setSaveError(null);
    try {
      await adminApi.updateUserRole(userId, newRole);
      refetch();
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : '保存失败');
    } finally {
      setSavingUserId(null);
    }
  };

  if (!isAdmin) {
    return <div className={styles.forbidden}>无权访问</div>;
  }

  const roleLabel: Record<string, string> = {
    admin: '超级管理员',
    editor: '内容编辑',
    viewer: '查看者',
  };

  const columns = [
    {
      key: 'email',
      header: '邮箱',
      render: (row: AdminUser) => row.email,
    },
    {
      key: 'display_name',
      header: '显示名',
      render: (row: AdminUser) => row.display_name || '-',
    },
    {
      key: 'role',
      header: '角色',
      render: (row: AdminUser) => (
        <span
          className={`${styles.roleBadge} ${
            row.role === 'admin'
              ? styles.roleAdmin
              : row.role === 'editor'
                ? styles.roleEditor
                : styles.roleViewer
          }`}
        >
          {roleLabel[row.role] ?? row.role}
        </span>
      ),
    },
    {
      key: 'created_at',
      header: '创建时间',
      render: (row: AdminUser) =>
        row.created_at ? new Date(row.created_at).toLocaleDateString('zh-CN') : '-',
    },
    {
      key: 'actions',
      header: '操作',
      width: '200px',
      render: (row: AdminUser) => {
        const isSelf = row.id === currentUserId;
        if (isSelf) {
          return (
            <span style={{ fontSize: '12px', color: 'var(--color-fg-muted)' }}>
              当前用户
            </span>
          );
        }
        return (
          <select
            className={styles.roleSelect}
            value={row.role}
            onChange={(e) => handleRoleChange(row.id, e.target.value)}
            disabled={savingUserId === row.id}
          >
            <option value="admin">超级管理员</option>
            <option value="editor">内容编辑</option>
            <option value="viewer">查看者</option>
          </select>
        );
      },
    },
  ];

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1 className={styles.pageTitle}>用户管理</h1>
      </div>

      {saveError && (
        <div
          style={{
            fontSize: 'var(--font-size-small)',
            color: 'var(--color-danger)',
            backgroundColor: 'var(--color-danger-subtle)',
            padding: 'var(--space-2)',
            borderRadius: 'var(--radius)',
          }}
        >
          {saveError}
        </div>
      )}

      <div className={styles.card}>
        <DataTable
          columns={columns}
          data={users}
          isLoading={isLoading}
          error={error}
          onRetry={refetch}
          emptyText="暂无用户"
          page={pagination?.page}
          totalPages={pagination?.total_pages}
          total={pagination?.total}
          onPageChange={setPage}
        />
      </div>
    </div>
  );
}