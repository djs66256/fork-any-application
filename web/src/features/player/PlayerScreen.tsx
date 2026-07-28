import { Container, Card } from '@/components/ui';

export interface PlayerScreenProps {
  videoId: string;
}

export function PlayerScreen({ videoId }: PlayerScreenProps) {
  return (
    <Container>
      <main style={{ paddingBlock: 'var(--space-6)' }}>
        <Card>
          <h1 style={{ fontSize: 'var(--font-size-large)', marginBottom: 'var(--space-3)' }}>
            播放页
          </h1>
          <p style={{ color: 'var(--color-fg-muted)', marginBottom: 'var(--space-2)' }}>
            Video ID: {videoId}
          </p>
          <p style={{ color: 'var(--color-fg-muted)' }}>
            待实现
          </p>
        </Card>
      </main>
    </Container>
  );
}
