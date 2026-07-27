import type { Metadata } from 'next';
import { Dashboard } from '@/features/admin/components/Dashboard';

export const metadata: Metadata = {
  title: '仪表盘',
};

export default function DashboardPage() {
  return <Dashboard />;
}