import Link from 'next/link';
import { Card, Container } from '@/components/ui';
import { MallProductIdSchema } from '@/lib/schemas';
import styles from './MallProductPlaceholderScreen.module.css';

interface MallProductPlaceholderScreenProps {
  productId: string;
}

export function MallProductPlaceholderScreen({ productId }: MallProductPlaceholderScreenProps) {
  const validation = MallProductIdSchema.safeParse(productId);

  if (!validation.success) {
    return (
      <Container maxWidth="720px">
        <main className={styles.page}>
          <Card as="section" className={styles.card}>
            <h1 className={styles.title}>商品详情</h1>
            <p className={styles.description}>商品信息无效，请返回商城首页重新选择。</p>
            <Link href="/mall" className={styles.link}>
              返回商城
            </Link>
          </Card>
        </main>
      </Container>
    );
  }

  return (
    <Container maxWidth="720px">
      <main className={styles.page}>
        <Card as="section" className={styles.card}>
          <h1 className={styles.title}>商品详情开发中</h1>
          <p className={styles.description}>商品 ID：{productId}</p>
          <p className={styles.description}>当前为首版占位承接页，真实交易能力将在后续版本开放。</p>
          <Link href="/mall" className={styles.link}>
            返回商城
          </Link>
        </Card>
      </main>
    </Container>
  );
}
