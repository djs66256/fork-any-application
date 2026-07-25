import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { DramaDetailScreen } from '@/features/drama-detail';

interface DetailPageProps {
  params: Promise<{ id: string }>;
}

function normalizeId(id: string): string | null {
  const normalizedId = id.trim();
  return normalizedId.length > 0 ? normalizedId : null;
}

export async function generateMetadata({ params }: DetailPageProps): Promise<Metadata> {
  const { id } = await params;
  const normalizedId = normalizeId(id);

  if (!normalizedId) {
    return {
      title: '详情',
    };
  }

  return {
    title: '详情',
    description: `详情页：${normalizedId}`,
  };
}

export default async function DetailPage({ params }: DetailPageProps) {
  const { id } = await params;
  const normalizedId = normalizeId(id);

  if (!normalizedId) {
    notFound();
  }

  return <DramaDetailScreen dramaId={normalizedId} />;
}
