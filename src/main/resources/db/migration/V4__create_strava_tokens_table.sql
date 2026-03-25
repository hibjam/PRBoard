CREATE TABLE strava_sessions (
    id              BIGSERIAL NOT NULL PRIMARY KEY,
    user_id         UUID NOT NULL UNIQUE REFERENCES users(user_id),
    access_token    VARCHAR(255) NOT NULL,
    refresh_token   VARCHAR(255) NOT NULL,
    expires_at      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);