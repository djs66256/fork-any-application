import type { Metadata } from 'next';
import { EditEpisodePage } from '@/features/admin/components/EditEpisodePage';

export const metadata: Metadata = {
  title: '编辑剧集',
};

export default function EditEpisodeRoute() {
  return <EditEpisodePage />;
}