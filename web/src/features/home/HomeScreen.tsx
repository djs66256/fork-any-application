import { config } from '@/lib/config';
import { Container, Card } from '@/components/ui';
import Link from 'next/link';

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

          <nav style={{ display: 'flex', gap: 'var(--spacing-md)', justifyContent: 'center', marginTop: 'var(--spacing-lg)' }}>
            <Link
              href="/play/sample"
              style={{
                color: 'var(--color-primary)',
                fontWeight: 600,
                textDecoration: 'underline',
              }}
            >
              Play Sample
            </Link>
            <Link
              href="/detail/sample"
              style={{
                color: 'var(--color-primary)',
                fontWeight: 600,
                textDecoration: 'underline',
              }}
            >
              Detail Sample
            </Link>
          </nav>
        </Card>
      </main>
    </Container>
  );
}
