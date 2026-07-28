import type { Metadata } from 'next';
import { AdminLayoutClient } from '@/features/admin/components/AdminLayoutClient';
import { AuthProvider } from '@/features/admin/contexts/AuthContext';
import { ThemeProvider } from '@/lib/theme';

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
  return (
    <ThemeProvider>
      <AuthProvider>
        <AdminLayoutClient>{children}</AdminLayoutClient>
      </AuthProvider>
    </ThemeProvider>
  );
}