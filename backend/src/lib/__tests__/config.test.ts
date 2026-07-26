import { describe, it, expect } from 'vitest';
import { config as configModule } from '../config';

describe('config', () => {
  it('should have app config with default values when env vars are not set', () => {
    expect(configModule.app.name).toBeDefined();
    expect(configModule.app.version).toBeDefined();
    expect(configModule.app.env).toBeDefined();
  });

  it('should have supabase config with empty string defaults', () => {
    expect(configModule.supabase).toBeDefined();
    expect(typeof configModule.supabase.url).toBe('string');
    expect(typeof configModule.supabase.anonKey).toBe('string');
    expect(typeof configModule.supabase.serviceRoleKey).toBe('string');
  });

  it('should have redis config with default url', () => {
    expect(configModule.redis).toBeDefined();
    expect(configModule.redis.url).toBe('redis://localhost:6379');
  });

  it('should default player history repository to mock', () => {
    expect(configModule.player.historyRepository).toBe('mock');
  });

  it('should expose environment-backed supabase keys', () => {
    expect(configModule.supabase.url).toBeDefined();
    expect(configModule.supabase.anonKey).toBeDefined();
    expect(configModule.supabase.serviceRoleKey).toBeDefined();
  });
});
