import type { Metadata } from 'next';
import { DramaDetailScreen } from '@/features/drama-detail';

interface DetailPageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata(): Promise<Metadata> {
  return {
    title: `详情 — ShortDrama`,
  };
}

export default async function DetailPage({ params }: DetailPageProps) {
  const { id } = await params;
  return <DramaDetailScreen dramaId={id} />;
}
