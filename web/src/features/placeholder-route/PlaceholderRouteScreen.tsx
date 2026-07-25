import { Card, Container } from '@/components/ui';

export interface PlaceholderRouteScreenProps {
  title: string;
  description: string;
}

export function PlaceholderRouteScreen({ title, description }: PlaceholderRouteScreenProps) {
  return (
    <Container>
      <main style={{ paddingBlock: 'var(--spacing-2xl)' }}>
        <Card>
          <h1 style={{ fontSize: 'var(--font-size-xl)', marginBottom: 'var(--spacing-md)' }}>
            {title}
          </h1>
          <p style={{ color: 'var(--color-text-secondary)' }}>{description}</p>
        </Card>
      </main>
    </Container>
  );
}
