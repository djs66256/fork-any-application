import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PlayerScreen } from '@/features/player/PlayerScreen';

describe('PlayerScreen', () => {
  it('should display the dramaId', () => {
    render(<PlayerScreen dramaId="test123" />);
    expect(screen.getByText(/Drama ID: test123/)).toBeInTheDocument();
  });

  it('should display "播放页" title', () => {
    render(<PlayerScreen dramaId="abc" />);
    expect(screen.getByText('播放页')).toBeInTheDocument();
  });

  it('should display "待实现" placeholder', () => {
    render(<PlayerScreen dramaId="abc" />);
    expect(screen.getByText('待实现')).toBeInTheDocument();
  });
});
