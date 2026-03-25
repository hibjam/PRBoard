CREATE TABLE activity_weightlifting_session (
    activity_id      BIGINT       PRIMARY KEY REFERENCES activities(id) ON DELETE CASCADE,
    duration_seconds INTEGER,
    total_volume_kg  DECIMAL(10,2),
    total_sets       INTEGER,
    total_reps       INTEGER,
    bodyweight_kg  DECIMAL(5,2),   -- required for Wilks/DOTS; nullable (not always known)
    is_competition BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE exercises (
    id           SERIAL      PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    muscle_group VARCHAR(50),
    equipment    VARCHAR(50),
    is_active    BOOLEAN     NOT NULL DEFAULT true,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    lift_category  VARCHAR(30)
);

CREATE TABLE activity_weightlifting_sets (
    id               BIGSERIAL    PRIMARY KEY,
    activity_id      BIGINT       NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    exercise_id      INTEGER      NOT NULL REFERENCES exercises(id),
    set_number       SMALLINT     NOT NULL,
    reps             SMALLINT,
    weight_kg        DECIMAL(6,2),
    duration_seconds INTEGER,
    rpe              DECIMAL(3,1),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_weightlifting_sets_activity ON activity_weightlifting_sets(activity_id);
CREATE INDEX idx_weightlifting_sets_exercise ON activity_weightlifting_sets(exercise_id);