import type { Metadata } from 'next';
import { EpisodeList } from '@/features/admin/components/EpisodeList';

export const metadata: Metadata = {
  title: '剧集管理',
};

export default function EpisodeListPage() {
  return <EpisodeList />;
}