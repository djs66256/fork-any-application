'use client';

import { Container, Button } from '@/components/ui';

interface ErrorPageProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function ErrorPage({ error, reset }: ErrorPageProps) {
  return (
    <Container>
      <main
        role="alert"
        style={{
          paddingBlock: 'var(--space-6)',
          textAlign: 'center',
        }}
      >
        <h1 style={{ fontSize: 'var(--font-size-large)', marginBottom: 'var(--space-3)' }}>
          页面出错了
        </h1>
        <p style={{ color: 'var(--color-fg-muted)', marginBottom: 'var(--space-4)' }}>
          {error.message}
        </p>
        <Button variant="primary" onClick={reset}>
          重试
        </Button>
      </main>
    </Container>
  );
}
