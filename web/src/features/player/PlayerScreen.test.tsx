import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PlayerScreen } from '@/features/player/PlayerScreen';

describe('PlayerScreen', () => {
  it('renders the video id', () => {
    render(<PlayerScreen videoId="test123" />);
    expect(screen.getByText(/Video ID: test123/)).toBeInTheDocument();
  });

  it('renders placeholder content', () => {
    render(<PlayerScreen videoId="abc" />);
    expect(screen.getByText('播放页')).toBeInTheDocument();
    expect(screen.getByText('待实现')).toBeInTheDocument();
  });
});
