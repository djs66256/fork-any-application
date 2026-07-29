import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { MallProductPlaceholderScreen } from '@/features/mall';
import { MallProductIdSchema } from '@/lib/schemas';

interface MallProductPageProps {
  params: Promise<{ id: string }>;
}

function normalizeId(id: string): string {
  return id.trim();
}

function parseProductId(id: string): string | null {
  const normalizedId = normalizeId(id);
  const validation = MallProductIdSchema.safeParse(normalizedId);
  return validation.success ? validation.data : null;
}

export async function generateMetadata({ params }: MallProductPageProps): Promise<Metadata> {
  const { id } = await params;
  const productId = parseProductId(id);

  return {
    title: '商城商品详情',
    description: productId ? `商城商品详情占位页：${productId}` : '商城商品详情占位页',
  };
}

export default async function MallProductPage({ params }: MallProductPageProps) {
  const { id } = await params;
  const productId = parseProductId(id);

  if (!productId) {
    notFound();
  }

  return <MallProductPlaceholderScreen productId={productId} />;
}
