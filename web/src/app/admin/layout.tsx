import type { Metadata } from 'next';
import { AdminLayoutClient } from '@/features/admin/components/AdminLayoutClient';

export const metadata: Metadata = {
  title: {
    default: '管理平台',
    template: '%s — 管理平台',
  },
};

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <AdminLayoutClient>{children}</AdminLayoutClient>;
}