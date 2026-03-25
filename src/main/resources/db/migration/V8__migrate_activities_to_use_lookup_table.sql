-- Add new FK column alongside old enum column
ALTER TABLE activities ADD COLUMN discipline_id SMALLINT REFERENCES disciplines(id);
ALTER TABLE activities ADD COLUMN source_v2 VARCHAR(20);
ALTER TABLE activities ADD COLUMN canonical_activity_id BIGINT REFERENCES activities(id);
ALTER TABLE activities ADD COLUMN updated_at TIMESTAMP;

-- Backfill from existing enum values
UPDATE activities a
SET discipline_id = d.id
FROM disciplines d
WHERE d.code = a.discipline::text;

UPDATE activities SET source_v2 = source::text;
UPDATE activities SET updated_at = created_at;

-- Enforce not-null now that backfill is done
ALTER TABLE activities ALTER COLUMN discipline_id SET NOT NULL;
ALTER TABLE activities ALTER COLUMN source_v2    SET NOT NULL;
ALTER TABLE activities ALTER COLUMN updated_at   SET NOT NULL;

-- Drop old columns
ALTER TABLE activities DROP COLUMN discipline;
ALTER TABLE activities DROP COLUMN source;
ALTER TABLE activities RENAME COLUMN source_v2 TO source;

-- Clean up old PG native enum (activity_source still referenced by strava_sessions — intentionally left)
DROP TYPE discipline;