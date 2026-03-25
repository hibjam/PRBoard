CREATE TABLE strava_oauth_states (
    id          BIGSERIAL PRIMARY KEY,
    state_token VARCHAR(64)  NOT NULL UNIQUE,
    user_id     UUID         NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_strava_oauth_states_state_token ON strava_oauth_states(state_token);
CREATE INDEX idx_strava_oauth_states_expires_at  ON strava_oauth_states(expires_at);