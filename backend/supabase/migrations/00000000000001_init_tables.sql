-- init_tables: Create dramas, episodes, and profiles tables

-- ============================================================
-- Helper: update_updated_at_column trigger function
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- dramas
-- ============================================================
CREATE TABLE IF NOT EXISTS dramas (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title         TEXT NOT NULL CHECK (char_length(title) > 0),
  description   TEXT,
  cover_url     TEXT,
  category      TEXT,
  total_episodes INTEGER NOT NULL DEFAULT 0 CHECK (total_episodes >= 0),
  release_year  INTEGER,
  rating        NUMERIC(3,1) CHECK (rating >= 0 AND rating <= 10),
  status        TEXT NOT NULL DEFAULT 'ongoing' CHECK (status IN ('ongoing', 'completed', 'announced')),
  play_count    INTEGER NOT NULL DEFAULT 0 CHECK (play_count >= 0),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for dramas
CREATE INDEX IF NOT EXISTS idx_dramas_status ON dramas(status);
CREATE INDEX IF NOT EXISTS idx_dramas_category ON dramas(category);
CREATE INDEX IF NOT EXISTS idx_dramas_created_at ON dramas(created_at DESC);

-- Update trigger for dramas
DROP TRIGGER IF EXISTS trg_dramas_updated_at ON dramas;
CREATE TRIGGER trg_dramas_updated_at
  BEFORE UPDATE ON dramas
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- episodes
-- ============================================================
CREATE TABLE IF NOT EXISTS episodes (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drama_id       UUID NOT NULL REFERENCES dramas(id) ON DELETE CASCADE,
  title          TEXT NOT NULL CHECK (char_length(title) > 0),
  episode_number INTEGER NOT NULL CHECK (episode_number >= 1),
  duration       INTEGER CHECK (duration >= 0),
  video_url      TEXT,
  thumbnail_url  TEXT,
  description    TEXT,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(drama_id, episode_number)
);

-- Indexes for episodes
CREATE INDEX IF NOT EXISTS idx_episodes_drama_id ON episodes(drama_id);
CREATE INDEX IF NOT EXISTS idx_episodes_episode_number ON episodes(drama_id, episode_number);

-- Update trigger for episodes
DROP TRIGGER IF EXISTS trg_episodes_updated_at ON episodes;
CREATE TRIGGER trg_episodes_updated_at
  BEFORE UPDATE ON episodes
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- profiles (linked to auth.users)
-- ============================================================
CREATE TABLE IF NOT EXISTS profiles (
  id           UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email        TEXT,
  display_name TEXT,
  avatar_url   TEXT,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for profiles
CREATE INDEX IF NOT EXISTS idx_profiles_email ON profiles(email);

-- Update trigger for profiles
DROP TRIGGER IF EXISTS trg_profiles_updated_at ON profiles;
CREATE TRIGGER trg_profiles_updated_at
  BEFORE UPDATE ON profiles
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- RLS policies (skeleton: allow all for development)
-- ============================================================
ALTER TABLE dramas ENABLE ROW LEVEL SECURITY;
ALTER TABLE episodes ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- Allow all operations for authenticated users (development default)
DO $$
BEGIN
  -- DROP existing policies silently
  DROP POLICY IF EXISTS "Allow all for authenticated" ON dramas;
  DROP POLICY IF EXISTS "Allow all for authenticated" ON episodes;
  DROP POLICY IF EXISTS "Allow all for authenticated" ON profiles;

  -- Create permissive policies for development
  CREATE POLICY "Allow all for authenticated" ON dramas FOR ALL TO authenticated USING (true) WITH CHECK (true);
  CREATE POLICY "Allow all for authenticated" ON episodes FOR ALL TO authenticated USING (true) WITH CHECK (true);
  CREATE POLICY "Allow all for authenticated" ON profiles FOR ALL TO authenticated USING (true) WITH CHECK (true);
END
$$;
