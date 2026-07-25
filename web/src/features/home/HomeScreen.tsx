import type { CSSProperties } from 'react';
import Link from 'next/link';
import { Card, Container } from '@/components/ui';
import { config } from '@/lib/config';

const linkStyle = {
  color: 'var(--color-primary)',
  fontWeight: 600,
  textDecoration: 'underline',
} satisfies CSSProperties;

export function HomeScreen() {
  return (
    <Container>
      <main style={{ paddingBlock: 'var(--spacing-2xl)', textAlign: 'center' }}>
        <Card>
          <h1 style={{ fontSize: 'var(--font-size-2xl)', marginBottom: 'var(--spacing-md)' }}>
            {config.app.name}
          </h1>
          <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-sm)' }}>
            Version: {config.app.version}
          </p>
          <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-lg)' }}>
            Environment: {config.app.env}
          </p>

          <nav
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              gap: 'var(--spacing-md)',
              justifyContent: 'center',
              marginTop: 'var(--spacing-lg)',
            }}
          >
            <Link href="/play/sample" style={linkStyle}>
              播放页示例
            </Link>
            <Link href="/detail/sample" style={linkStyle}>
              详情页示例
            </Link>
            <Link href="/search" style={linkStyle}>
              搜索
            </Link>
            <Link href="/rankings" style={linkStyle}>
              榜单
            </Link>
            <Link href="/mall" style={linkStyle}>
              商城
            </Link>
          </nav>
        </Card>
      </main>
    </Container>
  );
}
