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
        新建短剧
      </h1>
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
          padding: 'var(--spacing-lg)',
        }}
      >
        <DramaForm />
      </div>
    </div>
  );
}