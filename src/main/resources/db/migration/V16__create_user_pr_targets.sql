CREATE TABLE user_pr_targets (
    id               SERIAL       PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    discipline_id    SMALLINT     NOT NULL REFERENCES disciplines(id),
    distance_metres  DECIMAL(10,2) NOT NULL,
    label            VARCHAR(50),  -- e.g. "5K", "Parkrun", "100m", custom label
    is_preset        BOOLEAN      NOT NULL DEFAULT false,  -- true = standard distance, false = user-defined
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, discipline_id, distance_metres)
);

CREATE INDEX idx_pr_targets_user ON user_pr_targets(user_id);
CREATE INDEX idx_pr_targets_user_discipline ON user_pr_targets(user_id, discipline_id);

-- Function to seed default PR targets when a user selects a discipline.
-- Called by the application layer after profile update, not here directly.
-- Default presets per discipline are managed in application code (PrTargetDefaults.java).
-- This comment documents the intended presets:
--
-- RUN / TRAIL_RUN:
--   400m, 1K, 1 mile (1609.34m), 5K, 10K, half marathon (21097.5m), marathon (42195m)
--
-- SWIM:
--   50m, 100m, 200m, 400m, 800m, 1500m, 1 mile open water (1609.34m)
--
-- BIKE:
--   10K, 25K, 50K, 100K, 100 miles (160934m)
--
-- ROW:
--   500m, 1K, 2K, 5K, 10K
--
-- HIKE:
--   5K, 10K, 20K