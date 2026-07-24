import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { HomeScreen } from '@/features/home/HomeScreen';

describe('HomeScreen', () => {
  it('should display the app name', () => {
    render(<HomeScreen />);
    expect(screen.getByText('ShortDrama')).toBeInTheDocument();
  });

  it('should display the version number', () => {
    render(<HomeScreen />);
    expect(screen.getByText(/Version: 0\.1\.0/)).toBeInTheDocument();
  });

  it('should display the environment', () => {
    render(<HomeScreen />);
    expect(screen.getByText(/Environment:/)).toBeInTheDocument();
  });

  it('should contain a link to /play/sample', () => {
    render(<HomeScreen />);
    const link = screen.getByRole('link', { name: /play sample/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/play/sample');
  });

  it('should contain a link to /detail/sample', () => {
    render(<HomeScreen />);
    const link = screen.getByRole('link', { name: /detail sample/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/detail/sample');
  });
});
