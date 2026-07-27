-- add_ranking_fields_and_bookings: extend dramas ranking data and create bookings table

ALTER TABLE dramas
  ADD COLUMN IF NOT EXISTS content_type TEXT NOT NULL DEFAULT 'live_action' CHECK (content_type IN ('live_action', 'ai')),
  ADD COLUMN IF NOT EXISTS booking_count INTEGER NOT NULL DEFAULT 0 CHECK (booking_count >= 0),
  ADD COLUMN IF NOT EXISTS recommendation_score NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (recommendation_score >= 0);

CREATE INDEX IF NOT EXISTS idx_dramas_content_type ON dramas(content_type);
CREATE INDEX IF NOT EXISTS idx_dramas_play_count_desc ON dramas(play_count DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dramas_booking_count_desc ON dramas(booking_count DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dramas_recommendation_score_desc ON dramas(recommendation_score DESC, created_at DESC);

CREATE TABLE IF NOT EXISTS bookings (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  drama_id    UUID NOT NULL REFERENCES dramas(id) ON DELETE CASCADE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(user_id, drama_id)
);

CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_drama_id ON bookings(drama_id);
CREATE INDEX IF NOT EXISTS idx_bookings_created_at ON bookings(created_at DESC);

ALTER TABLE bookings ENABLE ROW LEVEL SECURITY;

DROP TRIGGER IF EXISTS trg_bookings_updated_at ON bookings;
CREATE TRIGGER trg_bookings_updated_at
  BEFORE UPDATE ON bookings
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

DO $$
BEGIN
  DROP POLICY IF EXISTS "Allow all for authenticated" ON bookings;
  CREATE POLICY "Allow all for authenticated" ON bookings FOR ALL TO authenticated USING (true) WITH CHECK (true);
END
$$;
