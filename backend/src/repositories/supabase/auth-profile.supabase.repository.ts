import { AuthUser, AuthUserSchema } from '@/lib/schemas';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';
import type { AuthProfileRepositoryInterface } from '@/repositories/interfaces/auth-profile.repository.interface';

function maskPhone(phone: string | null | undefined): string | null {
  const normalized = phone?.trim();
  if (!normalized) {
    return null;
  }

  const digits = normalized.replace(/^\+86/, '');
  if (!/^1\d{10}$/.test(digits)) {
    return normalized;
  }

  return `${digits.slice(0, 3)}****${digits.slice(-4)}`;
}

export class AuthProfileSupabaseRepository implements AuthProfileRepositoryInterface {
  async findAuthUserById(userId: string): Promise<AuthUser | null> {
    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase
      .from('profiles')
      .select('id,display_name,avatar_url,role')
      .eq('id', userId)
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return null;
      }
      throw Errors.internal(`Failed to fetch auth profile: ${error.message}`);
    }

    const { data: authData, error: authError } = await supabase.auth.admin.getUserById(userId);
    if (authError || !authData.user) {
      throw Errors.internal(`Failed to fetch auth user: ${authError?.message ?? 'missing user'}`);
    }

    const phone = maskPhone(authData.user.phone);
    if (!phone) {
      throw Errors.notFound('Auth user phone', userId);
    }

    const role = typeof authData.user.app_metadata?.role === 'string'
      ? authData.user.app_metadata.role
      : (data.role ?? 'viewer');

    return AuthUserSchema.parse({
      id: data.id,
      phone,
      display_name: data.display_name ?? null,
      avatar_url: data.avatar_url ?? null,
      is_new_user: false,
      role,
    });
  }

  async ensureAuthUserProfile(userId: string): Promise<void> {
    const supabase = getSupabaseAdmin();
    const { data: authData, error: authError } = await supabase.auth.admin.getUserById(userId);
    if (authError || !authData.user) {
      throw Errors.internal(`Failed to fetch auth user: ${authError?.message ?? 'missing user'}`);
    }

    const metadataRole = authData.user.app_metadata?.role;
    const role = metadataRole === 'admin' || metadataRole === 'editor' || metadataRole === 'viewer'
      ? metadataRole
      : 'viewer';

    const { error } = await supabase
      .from('profiles')
      .upsert({
        id: userId,
        email: authData.user.email ?? null,
        role,
      }, {
        onConflict: 'id',
        ignoreDuplicates: false,
      });

    if (error) {
      throw Errors.internal(`Failed to ensure auth profile: ${error.message}`);
    }
  }
}
