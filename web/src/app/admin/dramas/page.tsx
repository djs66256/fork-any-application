import type { Metadata } from 'next';
import { DramaList } from '@/features/admin/components/DramaList';

export const metadata: Metadata = {
  title: '短剧管理',
};

export default function DramaListPage() {
  return <DramaList />;
}