-- rename_episode_count: Rename total_episodes to episode_count in dramas table

ALTER TABLE dramas RENAME COLUMN total_episodes TO episode_count;