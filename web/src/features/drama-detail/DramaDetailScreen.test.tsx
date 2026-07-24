import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { DramaDetailScreen } from '@/features/drama-detail/DramaDetailScreen';

describe('DramaDetailScreen', () => {
  it('should display the dramaId', () => {
    render(<DramaDetailScreen dramaId="test456" />);
    expect(screen.getByText(/Drama ID: test456/)).toBeInTheDocument();
  });

  it('should display "详情页" title', () => {
    render(<DramaDetailScreen dramaId="abc" />);
    expect(screen.getByText('详情页')).toBeInTheDocument();
  });

  it('should display "待实现" placeholder', () => {
    render(<DramaDetailScreen dramaId="abc" />);
    expect(screen.getByText('待实现')).toBeInTheDocument();
  });
});
