import { Container, Card } from '@/components/ui';

export interface PlayerScreenProps {
  dramaId: string;
}

export function PlayerScreen({ dramaId }: PlayerScreenProps) {
  return (
    <Container>
      <main style={{ paddingBlock: 'var(--spacing-2xl)' }}>
        <Card>
          <h1 style={{ fontSize: 'var(--font-size-xl)', marginBottom: 'var(--spacing-md)' }}>
            播放页
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
