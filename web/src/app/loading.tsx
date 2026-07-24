import { Container } from '@/components/ui';

export default function Loading() {
  return (
    <Container>
      <main
        style={{
          paddingBlock: 'var(--spacing-2xl)',
          textAlign: 'center',
          color: 'var(--color-text-secondary)',
        }}
      >
        <p>加载中...</p>
      </main>
    </Container>
  );
}
