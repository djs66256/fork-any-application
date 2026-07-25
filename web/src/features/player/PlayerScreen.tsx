import { Container, Card } from '@/components/ui';

export interface PlayerScreenProps {
  videoId: string;
}

export function PlayerScreen({ videoId }: PlayerScreenProps) {
  return (
    <Container>
      <main style={{ paddingBlock: 'var(--spacing-2xl)' }}>
        <Card>
          <h1 style={{ fontSize: 'var(--font-size-xl)', marginBottom: 'var(--spacing-md)' }}>
            播放页
          </h1>
          <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-sm)' }}>
            Video ID: {videoId}
          </p>
          <p style={{ color: 'var(--color-text-secondary)' }}>
            待实现
          </p>
        </Card>
      </main>
    </Container>
  );
}
