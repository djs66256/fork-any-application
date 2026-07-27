'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import styles from './AdminSidebar.module.css';

interface NavItem {
  label: string;
  href: string;
  roles: string[];
}

const navItems: NavItem[] = [
  { label: '仪表盘', href: '/admin', roles: ['admin', 'editor', 'viewer'] },
  { label: '短剧管理', href: '/admin/dramas', roles: ['admin', 'editor', 'viewer'] },
  { label: '用户管理', href: '/admin/users', roles: ['admin'] },
];

interface AdminSidebarProps {
  role: string;
}

export function AdminSidebar({ role }: AdminSidebarProps) {
  const pathname = usePathname();

  const visibleItems = navItems.filter((item) => item.roles.includes(role));

  return (
    <aside className={styles.sidebar}>
      <nav>
        <ul className={styles.navList}>
          {visibleItems.map((item) => {
            const isActive = pathname === item.href || pathname.startsWith(item.href + '/');
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={`${styles.navItem} ${isActive ? styles.active : ''}`}
                >
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </aside>
  );
}