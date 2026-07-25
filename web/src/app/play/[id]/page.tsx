import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { PlayerScreen } from '@/features/player';

interface PlayPageProps {
  params: Promise<{ id: string }>;
}

function normalizeId(id: string): string | null {
  const normalizedId = id.trim();
  return normalizedId.length > 0 ? normalizedId : null;
}

export async function generateMetadata({ params }: PlayPageProps): Promise<Metadata> {
  const { id } = await params;
  const normalizedId = normalizeId(id);

  if (!normalizedId) {
    return {
      title: '播放',
    };
  }

  return {
    title: '播放',
    description: `播放页：${normalizedId}`,
  };
}

export default async function PlayPage({ params }: PlayPageProps) {
  const { id } = await params;
  const normalizedId = normalizeId(id);

  if (!normalizedId) {
    notFound();
  }

  return <PlayerScreen videoId={normalizedId} />;
}
