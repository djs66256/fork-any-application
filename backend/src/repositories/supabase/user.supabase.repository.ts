import { AdminUserProfile } from '@/lib/schemas';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';

export class UserSupabaseRepository {
  async list(page: number, pageSize: number): Promise<{ data: AdminUserProfile[]; total: number }> {
    const supabase = getSupabaseAdmin();
    const from = (page - 1) * pageSize;
    const to = from + pageSize - 1;

    const { data, error, count } = await supabase
      .from('profiles')
      .select('*', { count: 'exact', head: false })
      .range(from, to)
      .order('created_at', { ascending: false });

    if (error) {
      throw Errors.internal(`Failed to fetch users: ${error.message}`);
    }

    const users = (data ?? []).map((row) => ({
      id: row.id,
      email: row.email ?? null,
      display_name: row.display_name ?? null,
      avatar_url: row.avatar_url ?? null,
      role: row.role ?? 'viewer',
      created_at: row.created_at,
      updated_at: row.updated_at,
    })) as AdminUserProfile[];

    return { data: users, total: count ?? 0 };
  }

  async findById(id: string): Promise<AdminUserProfile | null> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('profiles')
      .select('*')
      .eq('id', id)
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }
      throw Errors.internal(`Failed to fetch user: ${error.message}`);
    }

    return {
      id: data.id,
      email: data.email ?? null,
      display_name: data.display_name ?? null,
      avatar_url: data.avatar_url ?? null,
      role: data.role ?? 'viewer',
      created_at: data.created_at,
      updated_at: data.updated_at,
    } as AdminUserProfile;
  }

  async updateRole(userId: string, role: string): Promise<AdminUserProfile | null> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('profiles')
      .update({ role })
      .eq('id', userId)
      .select('*')
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }
      throw Errors.internal(`Failed to update user role: ${error.message}`);
    }

    return {
      id: data.id,
      email: data.email ?? null,
      display_name: data.display_name ?? null,
      avatar_url: data.avatar_url ?? null,
      role: data.role ?? 'viewer',
      created_at: data.created_at,
      updated_at: data.updated_at,
    } as AdminUserProfile;
  }

  async count(): Promise<number> {
    const supabase = getSupabaseAdmin();
    const { count, error } = await supabase
      .from('profiles')
      .select('*', { count: 'exact', head: true });

    if (error) {
      throw Errors.internal(`Failed to count users: ${error.message}`);
    }

    return count ?? 0;
  }
}