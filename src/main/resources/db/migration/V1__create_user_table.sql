CREATE TABLE users (
    id          BIGSERIAL NOT NULL PRIMARY KEY,
    user_id     UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);