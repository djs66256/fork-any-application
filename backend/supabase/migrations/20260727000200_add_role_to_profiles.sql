-- add_role_to_profiles: Add role enum and column to profiles table

CREATE TYPE user_role AS ENUM ('admin', 'editor', 'viewer');

ALTER TABLE profiles ADD COLUMN IF NOT EXISTS role user_role NOT NULL DEFAULT 'viewer';

CREATE INDEX IF NOT EXISTS idx_profiles_role ON profiles(role);