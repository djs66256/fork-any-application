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
  it('should define --color-accent', () => {
    const content = readTokens();
    expect(content).toContain('--color-accent');
  });

  it('should define --color-bg-default', () => {
    const content = readTokens();
    expect(content).toContain('--color-bg-default');
  });

  it('should define --color-fg-default', () => {
    const content = readTokens();
    expect(content).toContain('--color-fg-default');
  });

  it('should define --radius', () => {
    const content = readTokens();
    expect(content).toContain('--radius');
  });

  it('should define --font-size-body', () => {
    const content = readTokens();
    expect(content).toContain('--font-size-body');
  });

  it('should include dark mode media query', () => {
    const content = readTokens();
    expect(content).toContain('prefers-color-scheme: dark');
  });

  it('should define all core color tokens', () => {
    const content = readTokens();
    const tokens = [
      '--color-accent',
      '--color-accent-hover',
      '--color-bg-default',
      '--color-bg-subtle',
      '--color-fg-default',
      '--color-fg-muted',
      '--color-border-default',
      '--color-danger',
    ];
    for (const token of tokens) {
      expect(content).toContain(token);
    }
  });

  it('should define spacing tokens', () => {
    const content = readTokens();
    const tokens = [
      '--space-1',
      '--space-2',
      '--space-3',
      '--space-4',
      '--space-5',
      '--space-6',
    ];
    for (const token of tokens) {
      expect(content).toContain(token);
    }
  });
});
