CREATE TABLE activity_endurance_data (
    activity_id           BIGINT      PRIMARY KEY REFERENCES activities(id) ON DELETE CASCADE,
    distance_metres       DECIMAL(10,2),
    duration_seconds      INTEGER,
    elevation_gain_metres DECIMAL(8,2),
    avg_heart_rate        SMALLINT,
    max_heart_rate        SMALLINT,
    avg_watts             SMALLINT,
    normalized_power      SMALLINT,
    avg_cadence           SMALLINT
);

-- Migrate existing distance/duration — all current activities are endurance disciplines
INSERT INTO activity_endurance_data (activity_id, distance_metres, duration_seconds)
SELECT id, distance_metres, duration_seconds FROM activities;

-- Now safe to drop from base table
ALTER TABLE activities DROP COLUMN distance_metres;
ALTER TABLE activities DROP COLUMN duration_seconds;