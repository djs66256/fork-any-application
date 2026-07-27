'use client';

import { useEffect } from 'react';

export default function AdminError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('Admin panel error:', error);
  }, [error]);

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '400px',
        gap: 'var(--spacing-md)',
        color: 'var(--color-text-secondary)',
      }}
    >
      <h2
        style={{
          fontSize: 'var(--font-size-xl)',
          fontWeight: 600,
          color: 'var(--color-text-primary)',
        }}
      >
        页面出错了
      </h2>
      <p style={{ fontSize: 'var(--font-size-sm)' }}>
        {error.message || '发生了未知错误'}
      </p>
      <button
        onClick={reset}
        style={{
          padding: 'var(--spacing-sm) var(--spacing-md)',
          backgroundColor: '#2563EB',
          color: '#ffffff',
          border: 'none',
          borderRadius: 'var(--radius-md)',
          fontSize: 'var(--font-size-sm)',
          cursor: 'pointer',
        }}
      >
        重试
      </button>
    </div>
  );
}