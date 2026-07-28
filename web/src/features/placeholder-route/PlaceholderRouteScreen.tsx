import { Card, Container } from '@/components/ui';

export interface PlaceholderRouteScreenProps {
  title: string;
  description: string;
}

export function PlaceholderRouteScreen({ title, description }: PlaceholderRouteScreenProps) {
  return (
    <Container>
      <main style={{ paddingBlock: 'var(--space-6)' }}>
        <Card>
          <h1 style={{ fontSize: 'var(--font-size-large)', marginBottom: 'var(--space-3)' }}>
            {title}
          </h1>
          <p style={{ color: 'var(--color-fg-muted)' }}>{description}</p>
        </Card>
      </main>
    </Container>
  );
}
