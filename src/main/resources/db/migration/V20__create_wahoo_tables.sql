CREATE TABLE wahoo_oauth_states (
    id             BIGSERIAL    PRIMARY KEY,
    state_token    VARCHAR(64)  NOT NULL UNIQUE,
    code_verifier  VARCHAR(128) NOT NULL,
    user_id        UUID         NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wahoo_oauth_states_state_token ON wahoo_oauth_states(state_token);
CREATE INDEX idx_wahoo_oauth_states_expires_at  ON wahoo_oauth_states(expires_at);

CREATE TABLE wahoo_sessions (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        UUID         NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    access_token   VARCHAR(255) NOT NULL,
    refresh_token  VARCHAR(255) NOT NULL,
    expires_at     BIGINT       NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);