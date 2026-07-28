import type { Metadata } from 'next';
import Link from 'next/link';
import { DramaForm } from '@/features/admin/components/DramaForm';

export const metadata: Metadata = {
  title: '新建短剧',
};

export default function NewDramaPage() {
  return (
    <div>
      <Link
        href="/admin/dramas"
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          fontSize: 'var(--font-size-small)',
          color: 'var(--color-accent)',
          textDecoration: 'none',
          marginBottom: 'var(--space-3)',
        }}
      >
        &larr; 返回短剧列表
      </Link>
      <h1
        style={{
          fontSize: 'var(--font-size-body)',
          fontWeight: 600,
          color: 'var(--color-fg-default)',
          marginBottom: 'var(--space-4)',
        }}
      >
        新建短剧
      </h1>
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
          padding: 'var(--space-4)',
        }}
      >
        <DramaForm />
      </div>
    </div>
  );
}