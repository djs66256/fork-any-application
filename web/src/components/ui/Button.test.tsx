import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Button } from '@/components/ui/Button';

describe('Button', () => {
  it('should render with default variant primary', () => {
    render(<Button>Click me</Button>);
    const btn = screen.getByRole('button', { name: /click me/i });
    expect(btn).toBeInTheDocument();
    expect(btn).toHaveAttribute('data-variant', 'primary');
  });

  it('should render with secondary variant', () => {
    render(<Button variant="secondary">Secondary</Button>);
    const btn = screen.getByRole('button', { name: /secondary/i });
    expect(btn).toHaveAttribute('data-variant', 'secondary');
  });

  it('should render with ghost variant', () => {
    render(<Button variant="ghost">Ghost</Button>);
    const btn = screen.getByRole('button', { name: /ghost/i });
    expect(btn).toHaveAttribute('data-variant', 'ghost');
  });

  it('should render with small size', () => {
    render(<Button size="sm">Small</Button>);
    const btn = screen.getByRole('button', { name: /small/i });
    expect(btn).toHaveAttribute('data-size', 'sm');
  });

  it('should render as disabled', () => {
    render(<Button disabled>Disabled</Button>);
    const btn = screen.getByRole('button', { name: /disabled/i });
    expect(btn).toBeDisabled();
  });

  it('should apply custom className', () => {
    render(<Button className="custom-class">Custom</Button>);
    const btn = screen.getByRole('button', { name: /custom/i });
    expect(btn).toHaveClass('custom-class');
  });

  it('should forward onClick handler', async () => {
    let clicked = false;
    render(<Button onClick={() => { clicked = true; }}>Click</Button>);
    screen.getByRole('button', { name: /click/i }).click();
    expect(clicked).toBe(true);
  });
});
