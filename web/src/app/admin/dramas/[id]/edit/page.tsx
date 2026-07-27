import type { Metadata } from 'next';
import { EditDramaPage } from '@/features/admin/components/EditDramaPage';

export const metadata: Metadata = {
  title: '编辑短剧',
};

export default function EditDramaRoute() {
  return <EditDramaPage />;
}