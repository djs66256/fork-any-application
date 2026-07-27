import type { Metadata } from 'next';
import { LoginForm } from '@/features/admin/components/LoginForm';

export const metadata: Metadata = {
  title: '登录',
};

export default function LoginPage() {
  return <LoginForm />;
}