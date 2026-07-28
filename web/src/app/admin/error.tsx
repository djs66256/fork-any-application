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
        gap: 'var(--space-3)',
        color: 'var(--color-fg-muted)',
      }}
    >
      <h2
        style={{
          fontSize: 'var(--font-size-large)',
          fontWeight: 600,
          color: 'var(--color-fg-default)',
        }}
      >
        页面出错了
      </h2>
      <p style={{ fontSize: 'var(--font-size-small)' }}>
        {error.message || '发生了未知错误'}
      </p>
      <button
        onClick={reset}
        style={{
          padding: 'var(--space-2) var(--space-3)',
          backgroundColor: 'var(--color-accent)',
          color: '#ffffff',
          border: 'none',
          borderRadius: 'var(--radius)',
          fontSize: 'var(--font-size-small)',
          cursor: 'pointer',
        }}
      >
        重试
      </button>
    </div>
  );
}