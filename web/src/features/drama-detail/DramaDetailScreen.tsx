import { Container, Card } from '@/components/ui';

export interface DramaDetailScreenProps {
  dramaId: string;
}

export function DramaDetailScreen({ dramaId }: DramaDetailScreenProps) {
  return (
    <Container>
      <main style={{ paddingBlock: 'var(--space-6)' }}>
        <Card>
          <h1 style={{ fontSize: 'var(--font-size-large)', marginBottom: 'var(--space-3)' }}>
            详情页
          </h1>
          <p style={{ color: 'var(--color-fg-muted)', marginBottom: 'var(--space-2)' }}>
            Drama ID: {dramaId}
          </p>
          <p style={{ color: 'var(--color-fg-muted)' }}>
            待实现
          </p>
        </Card>
      </main>
    </Container>
  );
}
