import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { config } from '@/lib/config';

let _supabaseClient: SupabaseClient | null = null;
let _supabaseAdmin: SupabaseClient | null = null;

export function getSupabaseClient(): SupabaseClient {
  if (!_supabaseClient) {
    const url = config.supabase.url;
    const anonKey = config.supabase.anonKey;
    if (!url || !anonKey) {
      throw new Error('Supabase URL and anon key are required');
    }
    _supabaseClient = createClient(url, anonKey);
  }
  return _supabaseClient;
}

export function getSupabaseAdmin(): SupabaseClient {
  if (!_supabaseAdmin) {
    const url = config.supabase.url;
    const serviceRoleKey = config.supabase.serviceRoleKey;
    if (!url || !serviceRoleKey) {
      throw new Error('Supabase URL and service role key are required');
    }
    _supabaseAdmin = createClient(url, serviceRoleKey, {
      auth: {
        autoRefreshToken: false,
        persistSession: false,
      },
    });
  }
  return _supabaseAdmin;
}

export async function checkSupabaseHealth(): Promise<boolean> {
  try {
    const client = getSupabaseAdmin();
    const { error } = await client.rpc('version');
    return !error;
  } catch {
    return false;
  }
}

export function closeSupabase(): void {
  _supabaseClient = null;
  _supabaseAdmin = null;
}
