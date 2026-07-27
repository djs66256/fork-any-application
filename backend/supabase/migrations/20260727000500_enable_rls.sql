-- enable_rls: Replace development RLS policies with role-based policies

-- ============================================================
-- Drop existing development policies
-- ============================================================
DROP POLICY IF EXISTS "Allow all for authenticated" ON dramas;
DROP POLICY IF EXISTS "Allow all for authenticated" ON episodes;
DROP POLICY IF EXISTS "Allow all for authenticated" ON profiles;

-- ============================================================
-- dramas: admin/editor can write, all authenticated can read
-- ============================================================
CREATE POLICY "admin_editor_can_write_dramas" ON dramas
  FOR INSERT TO authenticated
  WITH CHECK (auth.jwt()->'app_metadata'->>'role' IN ('admin', 'editor'));

CREATE POLICY "admin_editor_can_update_dramas" ON dramas
  FOR UPDATE TO authenticated
  USING (auth.jwt()->'app_metadata'->>'role' IN ('admin', 'editor'))
  WITH CHECK (auth.jwt()->'app_metadata'->>'role' IN ('admin', 'editor'));

CREATE POLICY "admin_editor_can_delete_dramas" ON dramas
  FOR DELETE TO authenticated
  USING (auth.jwt()->'app_metadata'->>'role' IN ('admin', 'editor'));

CREATE POLICY "all_roles_can_read_dramas" ON dramas
  FOR SELECT TO authenticated
  USING (true);

-- ============================================================
-- episodes: admin/editor can write, all authenticated can read
-- ============================================================
CREATE POLICY "admin_editor_can_write_episodes" ON episodes
  FOR INSERT TO authenticated
  WITH CHECK (auth.jwt()->'app_metadata'->>'role' IN ('admin', 'editor'));

CREATE POLICY "admin_editor_can_update_episodes" ON episodes
  FOR UPDATE TO authenticated
  USING (auth.jwt()->'app_metadata'->>'role' IN ('admin', 'editor'))
  WITH CHECK (auth.jwt()->'app_metadata'->>'role' IN ('admin', 'editor'));

CREATE POLICY "admin_editor_can_delete_episodes" ON episodes
  FOR DELETE TO authenticated
  USING (auth.jwt()->'app_metadata'->>'role' IN ('admin', 'editor'));

CREATE POLICY "all_roles_can_read_episodes" ON episodes
  FOR SELECT TO authenticated
  USING (true);

-- ============================================================
-- profiles: admin can read all and update roles
-- ============================================================
CREATE POLICY "admin_can_read_profiles" ON profiles
  FOR SELECT TO authenticated
  USING (auth.jwt()->'app_metadata'->>'role' = 'admin');

CREATE POLICY "admin_can_update_roles" ON profiles
  FOR UPDATE TO authenticated
  USING (auth.jwt()->'app_metadata'->>'role' = 'admin')
  WITH CHECK (auth.jwt()->'app_metadata'->>'role' = 'admin');

-- ============================================================
-- Allow users to read their own profile
-- ============================================================
CREATE POLICY "users_can_read_own_profile" ON profiles
  FOR SELECT TO authenticated
  USING (auth.uid() = id);