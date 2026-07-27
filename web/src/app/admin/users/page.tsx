import type { Metadata } from 'next';
import { UserList } from '@/features/admin/components/UserList';

export const metadata: Metadata = {
  title: '用户管理',
};

export default function UsersPage() {
  return <UserList />;
}