'use client';

import { useState, useEffect, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/features/admin/hooks/useAuth';
import styles from './LoginForm.module.css';

export function LoginForm() {
  const router = useRouter();
  const { login, isLoading: authLoading, isAuthenticated, error } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});

  // Redirect when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      router.push('/admin');
    }
  }, [isAuthenticated, router]);

  const validate = (): boolean => {
    const errors: { email?: string; password?: string } = {};
    if (!email.trim()) {
      errors.email = '请输入邮箱';
    }
    if (!password) {
      errors.password = '请输入密码';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await login(email, password);
      // onAuthStateChange in AuthProvider will flip isAuthenticated → useEffect redirects
    } catch {
      // Error is already set in AuthContext
    } finally {
      setIsSubmitting(false);
    }
  };

  const isLoading = authLoading || isSubmitting;
  const isFormDisabled = isLoading || !email.trim() || !password;

  return (
    <div className={styles.loginPage}>
      <div className={styles.loginCard}>
        <h1 className={styles.title}>管理平台</h1>
        <p className={styles.subtitle}>ShortDrama Admin Panel</p>
        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="email">
              邮箱
            </label>
            <input
              id="email"
              className={styles.input}
              type="email"
              placeholder="admin@example.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setFieldErrors((prev) => ({ ...prev, email: undefined }));
              }}
              autoComplete="email"
            />
            {fieldErrors.email && (
              <span style={{ fontSize: 12, color: 'var(--color-danger)' }}>
                {fieldErrors.email}
              </span>
            )}
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="password">
              密码
            </label>
            <input
              id="password"
              className={styles.input}
              type="password"
              placeholder="输入密码"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setFieldErrors((prev) => ({ ...prev, password: undefined }));
              }}
              autoComplete="current-password"
            />
            {fieldErrors.password && (
              <span style={{ fontSize: 12, color: 'var(--color-danger)' }}>
                {fieldErrors.password}
              </span>
            )}
          </div>

          {error && <div className={styles.errorMessage}>{error}</div>}

          <button
            className={styles.submitButton}
            type="submit"
            disabled={isFormDisabled}
          >
            {isLoading ? '登录中...' : '登录'}
          </button>
        </form>
      </div>
    </div>
  );
}