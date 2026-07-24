import type { Metadata } from 'next';
import { PlayerScreen } from '@/features/player';

interface PlayPageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata(): Promise<Metadata> {
  return {
    title: `播放 — ShortDrama`,
  };
}

export default async function PlayPage({ params }: PlayPageProps) {
  const { id } = await params;
  return <PlayerScreen dramaId={id} />;
}
