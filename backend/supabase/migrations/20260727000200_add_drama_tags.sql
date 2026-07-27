-- add_drama_tags: add tags storage for drama search and classification reuse

ALTER TABLE dramas
  ADD COLUMN IF NOT EXISTS tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

CREATE INDEX IF NOT EXISTS idx_dramas_tags_gin ON dramas USING GIN (tags);
