-- ============================================================
-- 本地开发数据库初始化
-- 启动时自动执行：创建角色、schema 与扩展
-- ============================================================

-- 扩展
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── 角色 ──────────────────────────────────────────────────
-- 标准 Supabase 角色体系，确保 Supabase Studio 正常工作
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'supabase_admin') THEN
    CREATE ROLE supabase_admin LOGIN SUPERUSER PASSWORD 'postgres';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'anon') THEN
    CREATE ROLE anon NOLOGIN NOINHERIT;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'authenticated') THEN
    CREATE ROLE authenticated NOLOGIN NOINHERIT;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'authenticator') THEN
    CREATE ROLE authenticator LOGIN NOINHERIT PASSWORD 'postgres';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'service_role') THEN
    CREATE ROLE service_role NOLOGIN NOINHERIT BYPASSRLS;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'supabase_auth_admin') THEN
    CREATE ROLE supabase_auth_admin LOGIN NOINHERIT CREATEROLE PASSWORD 'postgres';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'supabase_storage_admin') THEN
    CREATE ROLE supabase_storage_admin LOGIN NOINHERIT PASSWORD 'postgres';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'supabase_realtime_admin') THEN
    CREATE ROLE supabase_realtime_admin LOGIN NOINHERIT PASSWORD 'postgres' BYPASSRLS REPLICATION;
  END IF;
END
$$;

-- 角色继承链
GRANT anon TO authenticator;
GRANT authenticated TO authenticator;
GRANT service_role TO authenticator;

-- ── Schema ────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS storage;
CREATE SCHEMA IF NOT EXISTS extensions;

-- ── Auth 枚举类型 ─────────────────────────────────────────
DO $$ BEGIN CREATE TYPE auth.aal_level           AS ENUM ('aal1', 'aal2', 'aal3');                              EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN CREATE TYPE auth.code_challenge_method AS ENUM ('s256', 'plain');                                    EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN CREATE TYPE auth.factor_status        AS ENUM ('unverified', 'verified');                            EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN CREATE TYPE auth.factor_type          AS ENUM ('totp', 'webauthn', 'phone');                         EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN CREATE TYPE auth.one_time_token_type  AS ENUM ('confirmation_token','reauthentication_token','recovery_token','email_change_token_new','email_change_token_current','phone_change_token'); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ── Auth 核心表 ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS auth.instances (id uuid PRIMARY KEY, uuid uuid, raw_base_config text);
CREATE TABLE IF NOT EXISTS auth.users (
    instance_id uuid, id uuid PRIMARY KEY, aud varchar(255), role varchar(255),
    email varchar(255), encrypted_password varchar(255), email_confirmed_at timestamptz,
    invited_at timestamptz, confirmation_token varchar(255), confirmation_sent_at timestamptz,
    recovery_token varchar(255), recovery_sent_at timestamptz, email_change_token_new varchar(255),
    email_change varchar(255), email_change_sent_at timestamptz, last_sign_in_at timestamptz,
    raw_app_meta_data jsonb, raw_user_meta_data jsonb, is_super_admin boolean,
    created_at timestamptz, updated_at timestamptz, phone varchar(15) DEFAULT NULL,
    phone_confirmed_at timestamptz, phone_change varchar(15) DEFAULT '', phone_change_token varchar(255) DEFAULT '',
    phone_change_sent_at timestamptz, confirmed_at timestamptz GENERATED ALWAYS AS (LEAST(email_confirmed_at, phone_confirmed_at)) STORED,
    email_change_confirm_status smallint DEFAULT 0, banned_until timestamptz, reauthentication_token varchar(255) DEFAULT '',
    reauthentication_sent_at timestamptz, is_sso_user boolean NOT NULL DEFAULT FALSE, deleted_at timestamptz,
    is_anonymous boolean NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS auth.sessions (
    id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at timestamptz, updated_at timestamptz, factor_id uuid, aal auth.aal_level,
    not_after timestamptz, refreshed_at timestamp, user_agent text, ip inet, tag text
);
CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
    instance_id uuid, id bigserial PRIMARY KEY, token varchar(255), user_id varchar(255),
    revoked boolean, created_at timestamptz, updated_at timestamptz, parent varchar(255),
    session_id uuid REFERENCES auth.sessions(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS auth.audit_log_entries (instance_id uuid, id uuid PRIMARY KEY, payload json, created_at timestamptz, ip_address varchar(64) NOT NULL DEFAULT '');
CREATE TABLE IF NOT EXISTS auth.identities (
    id text PRIMARY KEY, user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    identity_data jsonb NOT NULL, provider text NOT NULL, last_sign_in_at timestamptz,
    created_at timestamptz, updated_at timestamptz,
    email text GENERATED ALWAYS AS (lower(identity_data->>'email')) STORED
);
CREATE TABLE IF NOT EXISTS auth.mfa_factors (
    id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    friendly_name text, factor_type auth.factor_type, status auth.factor_status,
    created_at timestamptz, updated_at timestamptz, secret text
);
CREATE TABLE IF NOT EXISTS auth.flow_state (
    id uuid PRIMARY KEY, user_id uuid, auth_code text NOT NULL, code_challenge_method auth.code_challenge_method NOT NULL,
    code_challenge text NOT NULL, provider_type text NOT NULL, provider_access_token text,
    provider_refresh_token text, created_at timestamptz, updated_at timestamptz,
    authentication_method text NOT NULL, auth_code_issued_at timestamptz
);
CREATE TABLE IF NOT EXISTS auth.schema_migrations (version varchar(255) PRIMARY KEY);
CREATE TABLE IF NOT EXISTS auth.one_time_tokens (
    id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_type auth.one_time_token_type, token_hash text, relates_to text,
    created_at timestamp NOT NULL DEFAULT now(), updated_at timestamp NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS auth.mfa_challenges (
    id uuid PRIMARY KEY, factor_id uuid NOT NULL REFERENCES auth.mfa_factors(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL, verified_at timestamptz, ip_address inet NOT NULL
);
CREATE TABLE IF NOT EXISTS auth.mfa_amr_claims (
    session_id uuid NOT NULL REFERENCES auth.sessions(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, authentication_method text NOT NULL,
    id uuid PRIMARY KEY
);
CREATE TABLE IF NOT EXISTS auth.sso_providers (id uuid PRIMARY KEY, resource_id text, created_at timestamptz, updated_at timestamptz);
CREATE TABLE IF NOT EXISTS auth.sso_domains (
    id uuid PRIMARY KEY, sso_provider_id uuid NOT NULL REFERENCES auth.sso_providers(id) ON DELETE CASCADE,
    domain text NOT NULL, created_at timestamptz, updated_at timestamptz
);
CREATE UNIQUE INDEX IF NOT EXISTS identities_user_id_provider_unique ON auth.identities (user_id, provider);

GRANT ALL ON ALL TABLES IN SCHEMA auth TO supabase_admin;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA auth TO supabase_admin;
GRANT ALL ON ALL SEQUENCES IN SCHEMA auth TO supabase_admin;

-- ── 权限 ──────────────────────────────────────────────────
GRANT USAGE ON SCHEMA public     TO PUBLIC;
GRANT USAGE ON SCHEMA auth       TO PUBLIC;
GRANT USAGE ON SCHEMA storage    TO PUBLIC;
GRANT USAGE ON SCHEMA extensions TO PUBLIC;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT ALL ON TABLES    TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT ALL ON FUNCTIONS TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT ALL ON SEQUENCES TO PUBLIC;
