import { Container, Card } from '@/components/ui';

export interface DramaDetailScreenProps {
  dramaId: string;
}

export function DramaDetailScreen({ dramaId }: DramaDetailScreenProps) {
  return (
    <Container>
      <main style={{ paddingBlock: 'var(--spacing-2xl)' }}>
        <Card>
          <h1 style={{ fontSize: 'var(--font-size-xl)', marginBottom: 'var(--spacing-md)' }}>
            详情页
          </h1>
          <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-sm)' }}>
            Drama ID: {dramaId}
          </p>
          <p style={{ color: 'var(--color-text-secondary)' }}>
            待实现
          </p>
        </Card>
      </main>
    </Container>
  );
}
