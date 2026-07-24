import type { ReactNode } from 'react';
import styles from './Card.module.css';

export interface CardProps {
  children?: ReactNode;
  className?: string;
  /** Semantic HTML tag to render (default: 'div') */
  as?: 'div' | 'article' | 'section';
}

export function Card({ children, className, as: Tag = 'div' }: CardProps) {
  const classNames = [styles.card, className].filter(Boolean).join(' ');
  return <Tag className={classNames}>{children}</Tag>;
}
