import Link from 'next/link';
import { Container } from '@/components/ui';

export default function NotFound() {
  return (
    <Container>
      <main
        style={{
          paddingBlock: 'var(--spacing-2xl)',
          textAlign: 'center',
        }}
      >
        <h1 style={{ fontSize: 'var(--font-size-2xl)', marginBottom: 'var(--spacing-md)' }}>
          页面不存在
        </h1>
        <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-lg)' }}>
          您访问的页面不存在或已移除。
        </p>
        <Link
          href="/"
          style={{
            color: 'var(--color-primary)',
            fontWeight: 600,
            textDecoration: 'underline',
          }}
        >
          返回首页
        </Link>
      </main>
    </Container>
  );
}
