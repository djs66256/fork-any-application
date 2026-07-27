'use client';

import { useState, useEffect, useCallback } from 'react';
import { getSupabaseBrowserClient } from '@/lib/supabase';

export interface AdminUser {
  id: string;
  email: string;
  role: string;
}

export interface AuthState {
  user: AdminUser | null;
  isLoading: boolean;
  error: string | null;
}

export function useAuth(): AuthState & {
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
} {
  const [user, setUser] = useState<AdminUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const supabase = getSupabaseBrowserClient();

    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session) {
        setUser({
          id: session.user.id,
          email: session.user.email ?? '',
          role: session.user.app_metadata?.role ?? 'viewer',
        });
      }
      setIsLoading(false);
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      (_event, session) => {
        if (session) {
          setUser({
            id: session.user.id,
            email: session.user.email ?? '',
            role: session.user.app_metadata?.role ?? 'viewer',
          });
        } else {
          setUser(null);
        }
      },
    );

    return () => {
      subscription.unsubscribe();
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    setError(null);
    setIsLoading(true);

    try {
      const supabase = getSupabaseBrowserClient();
      const { error: signInError } = await supabase.auth.signInWithPassword({
        email,
        password,
      });

      if (signInError) {
        if (signInError.message.includes('Invalid login credentials')) {
          setError('邮箱或密码错误');
        } else if (signInError.message.includes('Email')) {
          setError('邮箱格式不正确');
        } else {
          setError(signInError.message || '登录失败，请重试');
        }
      }
    } catch {
      setError('网络错误，请重试');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    const supabase = getSupabaseBrowserClient();
    await supabase.auth.signOut();
    setUser(null);
  }, []);

  return { user, isLoading, error, login, logout };
}