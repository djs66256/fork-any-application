-- create_playback_history: store latest anonymous playback progress per drama

CREATE TABLE IF NOT EXISTS playback_history (
  playback_session_id UUID NOT NULL,
  drama_id UUID NOT NULL,
  episode_id UUID NOT NULL,
  progress DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (progress >= 0),
  duration DOUBLE PRECISION CHECK (duration IS NULL OR duration >= 1),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (playback_session_id, drama_id)
);

CREATE INDEX IF NOT EXISTS idx_playback_history_updated_at
  ON playback_history(updated_at DESC);

ALTER TABLE playback_history ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  DROP POLICY IF EXISTS "Allow all for authenticated" ON playback_history;
  CREATE POLICY "Allow all for authenticated"
    ON playback_history
    FOR ALL
    TO authenticated
    USING (true)
    WITH CHECK (true);
END
$$;
