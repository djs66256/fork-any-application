import Link from 'next/link';
import { Container } from '@/components/ui';

export default function NotFound() {
  return (
    <Container>
      <main
        style={{
          paddingBlock: 'var(--space-6)',
          textAlign: 'center',
        }}
      >
        <h1 style={{ fontSize: 'var(--font-size-title)', marginBottom: 'var(--space-3)' }}>
          页面不存在
        </h1>
        <p style={{ color: 'var(--color-fg-muted)', marginBottom: 'var(--space-4)' }}>
          您访问的页面不存在或已移除。
        </p>
        <Link
          href="/"
          style={{
            color: 'var(--color-accent)',
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
