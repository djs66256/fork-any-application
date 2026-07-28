import { Container } from '@/components/ui';

export default function Loading() {
  return (
    <Container>
      <main
        style={{
          paddingBlock: 'var(--space-6)',
          textAlign: 'center',
          color: 'var(--color-fg-muted)',
        }}
      >
        <p>加载中...</p>
      </main>
    </Container>
  );
}
