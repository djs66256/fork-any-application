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
          paddingBlock: 'var(--spacing-2xl)',
          textAlign: 'center',
        }}
      >
        <h1 style={{ fontSize: 'var(--font-size-xl)', marginBottom: 'var(--spacing-md)' }}>
          页面出错了
        </h1>
        <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-lg)' }}>
          {error.message}
        </p>
        <Button variant="primary" onClick={reset}>
          重试
        </Button>
      </main>
    </Container>
  );
}
