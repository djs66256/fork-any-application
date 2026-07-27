-- ============================================================
-- 本地开发数据库初始化
-- 启动时自动执行：创建角色、schema、扩展与权限
--
-- 注意：auth schema 的表由 GoTrue 容器自动创建和管理，
-- 请勿在此文件中创建任何 auth 表。
-- ============================================================

-- 扩展
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── 角色 ──────────────────────────────────────────────────
-- 标准 Supabase 角色体系
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
    CREATE ROLE authenticator LOGIN INHERIT PASSWORD 'postgres';
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'service_role') THEN
    CREATE ROLE service_role NOLOGIN NOINHERIT BYPASSRLS;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'supabase_auth_admin') THEN
    CREATE ROLE supabase_auth_admin LOGIN INHERIT CREATEROLE PASSWORD 'postgres';
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

-- ── Auth schema 所有权交给 GoTrue ──────────────────────────
-- GoTrue 容器启动时会自动在 auth schema 中创建/迁移自己的表
ALTER SCHEMA auth OWNER TO supabase_auth_admin;

-- ── Auth 枚举类型 (GoTrue 依赖) ────────────────────────────
-- 创建后将所有权转给 supabase_auth_admin，GoTrue 的 migration 需要修改它们
DO $$ BEGIN CREATE TYPE auth.aal_level           AS ENUM ('aal1', 'aal2', 'aal3');                              EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN CREATE TYPE auth.code_challenge_method AS ENUM ('s256', 'plain');                                    EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN CREATE TYPE auth.factor_status        AS ENUM ('unverified', 'verified');                            EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN CREATE TYPE auth.factor_type          AS ENUM ('totp', 'webauthn', 'phone');                         EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN CREATE TYPE auth.one_time_token_type  AS ENUM ('confirmation_token','reauthentication_token','recovery_token','email_change_token_new','email_change_token_current','phone_change_token'); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TYPE auth.aal_level           OWNER TO supabase_auth_admin;
ALTER TYPE auth.code_challenge_method OWNER TO supabase_auth_admin;
ALTER TYPE auth.factor_status        OWNER TO supabase_auth_admin;
ALTER TYPE auth.factor_type          OWNER TO supabase_auth_admin;
ALTER TYPE auth.one_time_token_type  OWNER TO supabase_auth_admin;

-- ── Public schema 权限 (PostgREST 需要) ─────────────────────
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role, authenticator;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;

ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public
  GRANT ALL ON TABLES TO anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public
  GRANT ALL ON SEQUENCES TO anon, authenticated, service_role;

-- ── 其他 schema 权限 ────────────────────────────────────────
GRANT USAGE ON SCHEMA auth       TO PUBLIC;
GRANT USAGE ON SCHEMA storage    TO PUBLIC;
GRANT USAGE ON SCHEMA extensions TO PUBLIC;

-- ── supabase_auth_admin schema 权限 ─────────────────────────
GRANT USAGE ON SCHEMA public TO supabase_auth_admin;
GRANT ALL ON ALL TABLES IN SCHEMA public TO supabase_auth_admin;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO supabase_auth_admin;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public
  GRANT ALL ON TABLES TO supabase_auth_admin;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public
  GRANT ALL ON SEQUENCES TO supabase_auth_admin;

-- ── supabase_auth_admin search_path ─────────────────────────
ALTER ROLE supabase_auth_admin SET search_path TO auth, public, extensions;

-- ── supabase_storage_admin 权限 ─────────────────────────────
GRANT USAGE ON SCHEMA storage TO supabase_storage_admin;
GRANT ALL ON ALL TABLES IN SCHEMA storage TO supabase_storage_admin;
ALTER SCHEMA storage OWNER TO supabase_storage_admin;

-- ── supabase_realtime_admin 权限 ────────────────────────────
GRANT USAGE ON SCHEMA public TO supabase_realtime_admin;
GRANT ALL ON ALL TABLES IN SCHEMA public TO supabase_realtime_admin;
