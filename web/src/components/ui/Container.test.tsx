import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { Container } from '@/components/ui/Container';

describe('Container', () => {
  it('should render children', () => {
    const { container } = render(<Container>Content</Container>);
    expect(container.textContent).toBe('Content');
  });

  it('should apply default maxWidth', () => {
    const { container } = render(<Container>Content</Container>);
    const div = container.firstChild as HTMLElement;
    expect(div.style.maxWidth).toBe('960px');
  });

  it('should apply custom maxWidth', () => {
    const { container } = render(<Container maxWidth="768px">Content</Container>);
    const div = container.firstChild as HTMLElement;
    expect(div.style.maxWidth).toBe('768px');
  });

  it('should apply custom className', () => {
    const { container } = render(<Container className="my-class">Content</Container>);
    const div = container.firstChild as HTMLElement;
    expect(div).toHaveClass('my-class');
  });
});
