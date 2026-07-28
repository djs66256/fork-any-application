'use client';

import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from 'react';
import { getSupabaseBrowserClient } from '@/lib/supabase';
import { useRouter } from 'next/navigation';

// ── Types ────────────────────────────────────────────────────

export interface AdminUser {
  id: string;
  email: string;
  role: string;
  displayName?: string;
}

interface AuthContextValue {
  user: AdminUser | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  error: string | null;
  role: string;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

// ── Context ───────────────────────────────────────────────────

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

// ── Helper ────────────────────────────────────────────────────

function sessionToUser(session: {
  user: { id: string; email?: string; app_metadata: Record<string, unknown>; user_metadata?: Record<string, unknown> };
} | null): AdminUser | null {
  if (!session) return null;
  return {
    id: session.user.id,
    email: session.user.email ?? '',
    role: (session.user.app_metadata?.role as string) ?? 'viewer',
    displayName: (session.user.user_metadata?.display_name as string) ?? undefined,
  };
}

// ── Provider ──────────────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AdminUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  // Bootstrap: check existing session on mount
  useEffect(() => {
    const supabase = getSupabaseBrowserClient();

    supabase.auth.getSession().then(({ data: { session } }) => {
      setUser(sessionToUser(session));
      setIsLoading(false);
    });

    // Subscribe to future auth changes
    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      (_event, session) => {
        setUser(sessionToUser(session));
      },
    );

    return () => {
      subscription.unsubscribe();
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    setError(null);
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
      throw signInError;
    }
    // onAuthStateChange will update user state; LoginForm watches isAuthenticated to redirect
  }, []);

  const logout = useCallback(async () => {
    const supabase = getSupabaseBrowserClient();
    await supabase.auth.signOut();
    setUser(null);
    router.push('/admin/login');
  }, [router]);

  const value: AuthContextValue = {
    user,
    isLoading,
    isAuthenticated: user !== null,
    error,
    role: user?.role ?? 'viewer',
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// ── Hook ──────────────────────────────────────────────────────

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
