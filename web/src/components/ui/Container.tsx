import type { ReactNode, CSSProperties } from 'react';
import styles from './Container.module.css';

export interface ContainerProps {
  children?: ReactNode;
  className?: string;
  /** Max width of the container (default: 960px) */
  maxWidth?: string;
}

export function Container({ children, className, maxWidth = '960px' }: ContainerProps) {
  const classNames = [styles.container, className].filter(Boolean).join(' ');
  const style: CSSProperties = { maxWidth };

  return (
    <div className={classNames} style={style}>
      {children}
    </div>
  );
}
