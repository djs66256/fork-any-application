import type { Metadata } from 'next';
import { NewEpisodePage } from '@/features/admin/components/NewEpisodePage';

export const metadata: Metadata = {
  title: '新建剧集',
};

export default function NewEpisodeRoute() {
  return <NewEpisodePage />;
}