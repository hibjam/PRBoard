CREATE TABLE personal_records (
    id                     BIGSERIAL     PRIMARY KEY,
    user_id                UUID          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    discipline_id          SMALLINT      NOT NULL REFERENCES disciplines(id),

    -- PR type: FASTEST_DISTANCE | LONGEST_ACTIVITY | BEST_POWER | BEST_PACE | HEAVIEST_SESSION
    -- Stored as varchar — adding a new type requires no migration
    pr_type                VARCHAR(30)   NOT NULL,

    -- Populated for FASTEST_DISTANCE only — links back to the target that was set
    pr_target_id           INTEGER       REFERENCES user_pr_targets(id) ON DELETE SET NULL,

    -- The PR value. Units depend on pr_type:
    --   FASTEST_DISTANCE  → duration in seconds
    --   LONGEST_ACTIVITY  → distance in metres
    --   BEST_POWER        → watts (integer stored as decimal for consistency)
    --   HEAVIEST_SESSION  → total volume in kg
    value                  DECIMAL(12,4) NOT NULL,

    -- The activity that set this PR
    activity_id            BIGINT        NOT NULL REFERENCES activities(id),

    -- When the PR was achieved (copied from activity.start_time for easy querying)
    achieved_at            TIMESTAMP     NOT NULL,

    created_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- One current PR per user / discipline / type / target combination
    UNIQUE (user_id, discipline_id, pr_type, pr_target_id)
);

CREATE INDEX idx_prs_user_discipline ON personal_records(user_id, discipline_id);
CREATE INDEX idx_prs_activity ON personal_records(activity_id);