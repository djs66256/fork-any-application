import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

function readTokens(): string {
  return readFileSync(
    resolve(__dirname, 'tokens.css'),
    'utf-8',
  );
}

describe('tokens.css', () => {
  it('should define --color-primary', () => {
    const content = readTokens();
    expect(content).toContain('--color-primary');
  });

  it('should define --spacing-md', () => {
    const content = readTokens();
    expect(content).toContain('--spacing-md');
  });

  it('should define --radius-md', () => {
    const content = readTokens();
    expect(content).toContain('--radius-md');
  });

  it('should define --font-size-md', () => {
    const content = readTokens();
    expect(content).toContain('--font-size-md');
  });

  it('should include dark mode media query', () => {
    const content = readTokens();
    expect(content).toContain('prefers-color-scheme: dark');
  });

  it('should define all required color tokens', () => {
    const content = readTokens();
    const colorTokens = [
      '--color-primary',
      '--color-primary-hover',
      '--color-secondary',
      '--color-background',
      '--color-surface',
      '--color-text-primary',
      '--color-text-secondary',
      '--color-border',
      '--color-error',
    ];
    for (const token of colorTokens) {
      expect(content).toContain(token);
    }
  });

  it('should define all spacing tokens', () => {
    const content = readTokens();
    const spacingTokens = [
      '--spacing-xs',
      '--spacing-sm',
      '--spacing-md',
      '--spacing-lg',
      '--spacing-xl',
      '--spacing-2xl',
    ];
    for (const token of spacingTokens) {
      expect(content).toContain(token);
    }
  });
});
