CREATE TYPE discipline AS ENUM ('SWIM', 'BIKE', 'RUN');
CREATE TYPE activity_source AS ENUM ('STRAVA', 'GARMIN');

CREATE TABLE activities (
    id                  BIGSERIAL NOT NULL PRIMARY KEY,
    external_id         VARCHAR(255) NOT NULL,
    source              activity_source NOT NULL,
    discipline          discipline NOT NULL,
    distance_metres     DECIMAL(10, 2),
    duration_seconds    INTEGER,
    start_time          TIMESTAMP NOT NULL,
    user_id             UUID NOT NULL REFERENCES users(user_id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (external_id, source)
);