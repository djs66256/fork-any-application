import { describe, it, expect, beforeEach, vi } from 'vitest';

const mockCreateClient = vi.fn();
const mockRpc = vi.fn();

mockCreateClient.mockReturnValue({
  rpc: mockRpc,
});

vi.mock('@supabase/supabase-js', () => ({
  createClient: (...args: unknown[]) => mockCreateClient(...args),
}));

vi.mock('@/lib/config', () => ({
  config: {
    supabase: {
      url: 'https://test.supabase.co',
      anonKey: 'test-anon-key',
      serviceRoleKey: 'test-service-role-key',
    },
  },
}));

describe('supabase', () => {
  beforeEach(() => {
    vi.resetModules();
    mockCreateClient.mockClear();
    mockRpc.mockClear();
    // Reset singletons by re-importing
  });

  it('should create supabase client with anon key', async () => {
    const { getSupabaseClient } = await import('../supabase');
    const client = getSupabaseClient();
    expect(mockCreateClient).toHaveBeenCalledWith(
      'https://test.supabase.co',
      'test-anon-key',
    );
    expect(client).toBeDefined();
  });

  it('should create supabase admin with service role key', async () => {
    const { getSupabaseAdmin } = await import('../supabase');
    const admin = getSupabaseAdmin();
    expect(mockCreateClient).toHaveBeenCalledWith(
      'https://test.supabase.co',
      'test-service-role-key',
      expect.objectContaining({
        auth: expect.objectContaining({
          autoRefreshToken: false,
          persistSession: false,
        }),
      }),
    );
    expect(admin).toBeDefined();
  });

  it('should return same instance on repeated getSupabaseClient calls', async () => {
    const { getSupabaseClient } = await import('../supabase');
    const c1 = getSupabaseClient();
    const c2 = getSupabaseClient();
    expect(mockCreateClient).toHaveBeenCalledTimes(1);
    expect(c1).toBe(c2);
  });

  it('checkSupabaseHealth should return true when rpc succeeds', async () => {
    mockRpc.mockResolvedValueOnce({ data: {}, error: null });
    const { checkSupabaseHealth } = await import('../supabase');
    const result = await checkSupabaseHealth();
    expect(result).toBe(true);
  });

  it('checkSupabaseHealth should return false when rpc fails', async () => {
    mockRpc.mockResolvedValueOnce({ data: null, error: { message: 'error' } });
    const { checkSupabaseHealth } = await import('../supabase');
    const result = await checkSupabaseHealth();
    expect(result).toBe(false);
  });

  it('checkSupabaseHealth should return false when throws', async () => {
    mockRpc.mockRejectedValueOnce(new Error('Connection refused'));
    const { checkSupabaseHealth } = await import('../supabase');
    const result = await checkSupabaseHealth();
    expect(result).toBe(false);
  });

  it('closeSupabase should reset client singletons', async () => {
    const { getSupabaseClient, closeSupabase } = await import('../supabase');
    getSupabaseClient();
    expect(mockCreateClient).toHaveBeenCalledTimes(1);
    closeSupabase();
    getSupabaseClient();
    expect(mockCreateClient).toHaveBeenCalledTimes(2);
  });
});
