import type { ButtonHTMLAttributes, ReactNode } from 'react';
import styles from './Button.module.css';

export interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  /** Visual variant of the button */
  variant?: 'primary' | 'secondary' | 'ghost';
  /** Size of the button */
  size?: 'sm' | 'md' | 'lg';
  /** Whether the button is disabled */
  disabled?: boolean;
  /** Additional CSS class names */
  className?: string;
  /** Button content */
  children: ReactNode;
}

export function Button({
  variant = 'primary',
  size = 'md',
  disabled = false,
  className,
  children,
  ...rest
}: ButtonProps) {
  const classNames = [
    styles.button,
    styles[`variant-${variant}`],
    styles[`size-${size}`],
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button
      className={classNames}
      data-variant={variant}
      data-size={size}
      disabled={disabled}
      {...rest}
    >
      {children}
    </button>
  );
}
